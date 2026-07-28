# Сервис для получения векторных эмбеддингов текста через SentenceTransformer
# Использует модель intfloat/multilingual-e5-small (384-мерные вектора)
import logging
import numpy as np

logger = logging.getLogger(__name__)


class EmbeddingService:
    def __init__(self, model_loader):
        self.model_loader = model_loader

    # Проверка доступности модели эмбеддингов
    def is_available(self) -> bool:
        return self.model_loader.embedding_model is not None

    # Получение эмбеддинга для одного текста (нормализованный вектор)
    def embed(self, text: str) -> np.ndarray:
        embedding = self.model_loader.embedding_model.encode(text, normalize_embeddings=True)
        return embedding

    # Пакетное получение эмбеддингов для нескольких текстов
    def embed_batch(self, texts: list[str]) -> list[np.ndarray]:
        embeddings = self.model_loader.embedding_model.encode(
            texts,
            normalize_embeddings=True,
            show_progress_bar=False,
        )
        return list(embeddings)
