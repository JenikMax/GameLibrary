# Загрузчик моделей HuggingFace с кэшированием на диск
# Поддерживает 1 модель перевода (NLLB-200 ru-en), 1 модель эмбеддингов (multilingual-e5-small)
# и 1 модель компьютерного зрения CLIP для анализа скриншотов
import logging
import os

from transformers import AutoModelForSeq2SeqLM, AutoTokenizer, CLIPModel, CLIPProcessor
from sentence_transformers import SentenceTransformer

logger = logging.getLogger(__name__)


class ModelLoader:
    TRANSLATION_MODEL = "facebook/nllb-200-distilled-600M"
    EMBEDDING_MODEL = "intfloat/multilingual-e5-small"
    CLIP_MODEL = "openai/clip-vit-base-patch32"

    # ISO 639-3 + script коды для NLLB-200
    LANG_CODES = {
        "ru-en": ("rus_Cyrl", "eng_Latn"),
        "en-ru": ("eng_Latn", "rus_Cyrl"),
    }

    def __init__(self, models_dir: str):
        self.models_dir = models_dir
        self.translation_tokenizer = None
        self.translation_model = None
        self.embedding_model: SentenceTransformer = None
        self.clip_model: CLIPModel = None
        self.clip_processor: CLIPProcessor = None
        self._status: dict = {}

    # Загрузка всех моделей: создание директорий, загрузка переводчика и эмбеддинга
    def load_all(self):
        os.makedirs(self.models_dir, exist_ok=True)
        cache_dir = os.path.join(self.models_dir, "hf_cache")
        os.makedirs(cache_dir, exist_ok=True)

        self._load_translation_model(cache_dir)
        self._load_embedding_model(cache_dir)
        self._load_clip_model(cache_dir)

    # Загрузка модели перевода NLLB-200: проверка кэша, скачивание при необходимости
    def _load_translation_model(self, cache_dir: str):
        model_name = self.TRANSLATION_MODEL
        logger.info("Loading translation model: %s", model_name)
        try:
            local_dir = os.path.join(self.models_dir, "nllb-200-distilled-600M")
            if os.path.isdir(local_dir):
                logger.info("  Using cached model at: %s", local_dir)
                self.translation_tokenizer = AutoTokenizer.from_pretrained(
                    local_dir, local_files_only=True,
                    src_lang="eng_Latn",
                )
                self.translation_model = AutoModelForSeq2SeqLM.from_pretrained(
                    local_dir, local_files_only=True,
                    torch_dtype="float32",
                )
            else:
                logger.info("  Downloading from HuggingFace...")
                self.translation_tokenizer = AutoTokenizer.from_pretrained(
                    model_name, cache_dir=cache_dir,
                    src_lang="eng_Latn",
                )
                self.translation_model = AutoModelForSeq2SeqLM.from_pretrained(
                    model_name, cache_dir=cache_dir,
                    torch_dtype="float32",
                )
                logger.info("  Saving to: %s", local_dir)
                self.translation_tokenizer.save_pretrained(local_dir)
                self.translation_model.save_pretrained(local_dir)

            self.translation_model.eval()
            self._status["nllb"] = "loaded"
            logger.info("  [OK] Translation model loaded")
        except Exception as e:
            logger.error("  [FAIL] Translation model: %s", e)
            self._status["nllb"] = f"error: {e}"

    # Загрузка модели эмбеддингов SentenceTransformer
    def _load_embedding_model(self, cache_dir: str):
        logger.info("Loading embedding model: %s", self.EMBEDDING_MODEL)
        try:
            local_dir = os.path.join(self.models_dir, "multilingual-e5-small")
            if os.path.isdir(local_dir):
                logger.info("  Using cached model at: %s", local_dir)
                self.embedding_model = SentenceTransformer(local_dir, model_kwargs={"torch_dtype": "auto"})
            else:
                logger.info("  Downloading from HuggingFace...")
                self.embedding_model = SentenceTransformer(self.EMBEDDING_MODEL, cache_folder=cache_dir, model_kwargs={"torch_dtype": "auto"})
                logger.info("  Saving to: %s", local_dir)
                self.embedding_model.save(local_dir)

            self._status["embedding"] = "loaded"
            logger.info("  [OK] Embedding model loaded (dimension=%d)", self.embedding_model.get_sentence_embedding_dimension())
        except Exception as e:
            logger.error("  [FAIL] Embedding model: %s", e)
            self._status["embedding"] = f"error: {e}"

    # Загрузка CLIP модели для анализа изображений
    def _load_clip_model(self, cache_dir: str):
        logger.info("Loading CLIP model: %s", self.CLIP_MODEL)
        try:
            local_dir = os.path.join(self.models_dir, "clip-vit-base-patch32")
            if os.path.isdir(local_dir):
                logger.info("  Using cached model at: %s", local_dir)
                self.clip_processor = CLIPProcessor.from_pretrained(local_dir, local_files_only=True)
                self.clip_model = CLIPModel.from_pretrained(local_dir, local_files_only=True)
            else:
                logger.info("  Downloading from HuggingFace...")
                self.clip_processor = CLIPProcessor.from_pretrained(self.CLIP_MODEL, cache_dir=cache_dir)
                self.clip_model = CLIPModel.from_pretrained(self.CLIP_MODEL, cache_dir=cache_dir)
                logger.info("  Saving to: %s", local_dir)
                self.clip_processor.save_pretrained(local_dir)
                self.clip_model.save_pretrained(local_dir)

            self.clip_model.eval()
            self._status["clip"] = "loaded"
            logger.info("  [OK] CLIP model loaded")
        except Exception as e:
            logger.error("  [FAIL] CLIP model: %s", e)
            self._status["clip"] = f"error: {e}"

    # Возвращает копию словаря статусов загрузки моделей
    def get_status(self) -> dict:
        return dict(self._status)
