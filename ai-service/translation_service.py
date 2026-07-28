# Сервис перевода текста ru↔en через MarianMT
# Разбивает текст на предложения, переводит батчами по 16, удаляет HTML-теги
import logging
import re

import torch

logger = logging.getLogger(__name__)

# Регулярка для удаления HTML-тегов
_HTML_TAG = re.compile(r'<[^>]*>')
# Разделение на предложения по точке/восклицательному/вопросительному знаку
_SENTENCE_SPLIT = re.compile(r'(?<=[.!?])\s+(?=[A-ZА-ЯЁ0-9])')
# Размер батча для пакетного перевода
_BATCH_SIZE = 16


class TranslationService:
    def __init__(self, model_loader):
        self.model_loader = model_loader

    # Проверка доступности хотя бы одной модели перевода
    def is_available(self) -> bool:
        return bool(self.model_loader.translation_models)

    # Разбиение текста на предложения с очисткой от NBSP и лишних пробелов
    def _split_sentences(self, text: str) -> list[str]:
        text = text.replace('\u00a0', ' ')
        text = ' '.join(text.split())
        parts = _SENTENCE_SPLIT.split(text)
        parts = [p.strip() for p in parts if p.strip()]
        return parts if parts else [text]

    # Перевод батча предложений через модель (beam search, num_beams=2)
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

        # Генерация перевода без вычисления градиентов
        with torch.no_grad():
            output_ids = model.generate(
                **inputs,
                num_beams=2,
                max_length=512,
                early_stopping=True,
            )

        return [
            tokenizer.decode(ids, skip_special_tokens=True)
            for ids in output_ids
        ]

    # Полный перевод текста: очистка HTML, разбивка, пакетный перевод, сборка
    def translate(self, text: str, direction: str) -> str:
        tokenizer = self.model_loader.translation_tokenizers.get(direction)
        model = self.model_loader.translation_models.get(direction)

        if tokenizer is None or model is None:
            raise ValueError(f"No translation model for direction: {direction}")

        text = _HTML_TAG.sub('', text)
        sentences = self._split_sentences(text)

        # Если одно предложение — переводим напрямую
        if len(sentences) <= 1:
            return self._translate_batch(sentences, direction)[0]

        logger.info(
            "Translating %d sentences [%s], batch size %d",
            len(sentences), direction, _BATCH_SIZE,
        )

        # Перевод по батчам для экономии памяти
        translated_parts = []
        for i in range(0, len(sentences), _BATCH_SIZE):
            batch = sentences[i:i + _BATCH_SIZE]
            translated_parts.extend(self._translate_batch(batch, direction))

        return ' '.join(translated_parts)
