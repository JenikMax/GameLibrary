import logging
import os

from transformers import AutoModelForSeq2SeqLM, AutoTokenizer
from sentence_transformers import SentenceTransformer

logger = logging.getLogger(__name__)


class ModelLoader:
    TRANSLATION_MODELS = {
        "ru-en": "Helsinki-NLP/opus-mt-ru-en",
        "en-ru": "Helsinki-NLP/opus-mt-en-ru",
    }
    EMBEDDING_MODEL = "intfloat/multilingual-e5-small"

    def __init__(self, models_dir: str):
        self.models_dir = models_dir
        self.translation_tokenizers: dict = {}
        self.translation_models: dict = {}
        self.embedding_model: SentenceTransformer = None
        self._status: dict = {}

    def load_all(self):
        os.makedirs(self.models_dir, exist_ok=True)
        cache_dir = os.path.join(self.models_dir, "hf_cache")
        os.makedirs(cache_dir, exist_ok=True)

        for direction, model_name in self.TRANSLATION_MODELS.items():
            self._load_translation_model(direction, model_name, cache_dir)

        self._load_embedding_model(cache_dir)

    def _load_translation_model(self, direction: str, model_name: str, cache_dir: str):
        logger.info("Loading translation model [%s]: %s", direction, model_name)
        try:
            local_dir = os.path.join(self.models_dir, direction.replace("-", "_"))
            if os.path.isdir(local_dir):
                logger.info("  Using cached model at: %s", local_dir)
                tokenizer = AutoTokenizer.from_pretrained(local_dir, local_files_only=True)
                model = AutoModelForSeq2SeqLM.from_pretrained(local_dir, local_files_only=True)
            else:
                logger.info("  Downloading from HuggingFace...")
                tokenizer = AutoTokenizer.from_pretrained(model_name, cache_dir=cache_dir)
                model = AutoModelForSeq2SeqLM.from_pretrained(model_name, cache_dir=cache_dir)
                logger.info("  Saving to: %s", local_dir)
                tokenizer.save_pretrained(local_dir)
                model.save_pretrained(local_dir)

            model.eval()
            self.translation_tokenizers[direction] = tokenizer
            self.translation_models[direction] = model
            self._status[direction] = "loaded"
            logger.info("  [OK] Translation model [%s] loaded", direction)
        except Exception as e:
            logger.error("  [FAIL] Translation model [%s]: %s", direction, e)
            self._status[direction] = f"error: {e}"

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

    def get_status(self) -> dict:
        return dict(self._status)
