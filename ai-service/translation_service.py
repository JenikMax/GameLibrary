import logging

import torch

logger = logging.getLogger(__name__)


class TranslationService:
    def __init__(self, model_loader):
        self.model_loader = model_loader

    def is_available(self) -> bool:
        return bool(self.model_loader.translation_models)

    def translate(self, text: str, direction: str) -> str:
        tokenizer = self.model_loader.translation_tokenizers.get(direction)
        model = self.model_loader.translation_models.get(direction)

        if tokenizer is None or model is None:
            raise ValueError(f"No translation model for direction: {direction}")

        inputs = tokenizer(text, return_tensors="pt", padding=True, truncation=True, max_length=512)

        with torch.no_grad():
            output_ids = model.generate(
                **inputs,
                num_beams=4,
                max_length=512,
                early_stopping=True,
            )

        translated = tokenizer.decode(output_ids[0], skip_special_tokens=True)
        return translated
