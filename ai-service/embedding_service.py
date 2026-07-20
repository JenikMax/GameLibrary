import logging
import numpy as np

logger = logging.getLogger(__name__)


class EmbeddingService:
    def __init__(self, model_loader):
        self.model_loader = model_loader

    def is_available(self) -> bool:
        return self.model_loader.embedding_model is not None

    def embed(self, text: str) -> np.ndarray:
        embedding = self.model_loader.embedding_model.encode(text, normalize_embeddings=True)
        return embedding

    def embed_batch(self, texts: list[str]) -> list[np.ndarray]:
        embeddings = self.model_loader.embedding_model.encode(
            texts,
            normalize_embeddings=True,
            show_progress_bar=False,
        )
        return list(embeddings)
