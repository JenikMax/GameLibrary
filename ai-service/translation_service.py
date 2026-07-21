import logging
import re

import torch

logger = logging.getLogger(__name__)

_HTML_TAG = re.compile(r'<[^>]*>')
_SENTENCE_SPLIT = re.compile(r'(?<=[.!?])\s+(?=[A-ZА-ЯЁ0-9])')
_BATCH_SIZE = 16


class TranslationService:
    def __init__(self, model_loader):
        self.model_loader = model_loader

    def is_available(self) -> bool:
        return bool(self.model_loader.translation_models)

    def _split_sentences(self, text: str) -> list[str]:
        text = text.replace('\u00a0', ' ')
        text = ' '.join(text.split())
        parts = _SENTENCE_SPLIT.split(text)
        parts = [p.strip() for p in parts if p.strip()]
        return parts if parts else [text]

    def _translate_batch(self, sentences: list[str], direction: str) -> list[str]:
        tokenizer = self.model_loader.translation_tokenizers.get(direction)
        model = self.model_loader.translation_models.get(direction)
        if tokenizer is None or model is None:
            raise ValueError(f"No translation model for direction: {direction}")

        inputs = tokenizer(
            sentences,
            return_tensors="pt",
            padding=True,
            truncation=True,
            max_length=512,
        )

        with torch.no_grad():
            output_ids = model.generate(
                **inputs,
                num_beams=4,
                max_length=512,
                early_stopping=True,
            )

        return [
            tokenizer.decode(ids, skip_special_tokens=True)
            for ids in output_ids
        ]

    def translate(self, text: str, direction: str) -> str:
        tokenizer = self.model_loader.translation_tokenizers.get(direction)
        model = self.model_loader.translation_models.get(direction)

        if tokenizer is None or model is None:
            raise ValueError(f"No translation model for direction: {direction}")

        text = _HTML_TAG.sub('', text)
        sentences = self._split_sentences(text)

        if len(sentences) <= 1:
            return self._translate_batch(sentences, direction)[0]

        logger.info(
            "Translating %d sentences [%s], batch size %d",
            len(sentences), direction, _BATCH_SIZE,
        )

        translated_parts = []
        for i in range(0, len(sentences), _BATCH_SIZE):
            batch = sentences[i:i + _BATCH_SIZE]
            translated_parts.extend(self._translate_batch(batch, direction))

        return ' '.join(translated_parts)
