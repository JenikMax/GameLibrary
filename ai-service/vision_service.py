# Сервис анализа изображений через CLIP (Zero-shot классификация)
# Использует модель openai/clip-vit-base-patch32 для сопоставления
# скриншотов с текстовыми метками (жанры, теги)

import base64
import io
import logging

import numpy as np
from PIL import Image

logger = logging.getLogger(__name__)


class VisionService:

    def __init__(self, model_loader):
        self.model_loader = model_loader

    def is_available(self) -> bool:
        return (self.model_loader.clip_model is not None
                and self.model_loader.clip_processor is not None)

    def classify(self, image_base64: str, labels: list[str], top_k: int = 10) -> list[dict]:
        """
        Zero-shot классификация скриншота по текстовым меткам.
        Возвращает top_k меток с вероятностями.
        """
        import torch

        image_data = base64.b64decode(image_base64)
        image = Image.open(io.BytesIO(image_data)).convert("RGB")

        inputs = self.model_loader.clip_processor(
            text=labels,
            images=image,
            return_tensors="pt",
            padding=True,
            truncation=True,
        )

        with torch.inference_mode():
            outputs = self.model_loader.clip_model(**inputs)

        logits_per_image = outputs.logits_per_image
        probs = logits_per_image.softmax(dim=1).cpu().numpy()[0]

        results = []
        for idx in np.argsort(-probs)[:top_k]:
            score = float(probs[idx])
            if score >= 0.05:   # порог минимальной уверенности
                results.append({"label": labels[idx], "score": round(score, 4)})

        return results

    def classify_multi(self, images_base64: list[str], labels: list[str], top_k: int = 10) -> list[dict]:
        """
        Классификация нескольких скриншотов с агрегацией результатов.
        Усредняет вероятности по всем изображениям.
        """
        if not images_base64:
            return []

        all_probs = {}
        for img_b64 in images_base64:
            results = self.classify(img_b64, labels, top_k=len(labels))
            for r in results:
                label = r["label"]
                all_probs[label] = all_probs.get(label, 0.0) + r["score"]

        avg_results = [
            {"label": l, "score": round(s / len(images_base64), 4)}
            for l, s in all_probs.items()
        ]
        avg_results.sort(key=lambda x: -x["score"])
        return avg_results[:top_k]
