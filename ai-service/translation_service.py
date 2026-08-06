# Сервис перевода текста ru↔en через NLLB-200
# Разбивает текст на предложения, переводит батчами по 12, удаляет HTML-теги
import logging
import re

import torch

logger = logging.getLogger(__name__)

# Регулярка для удаления HTML-тегов
_HTML_TAG = re.compile(r'<[^>]*>')
# Разделение на предложения по точке/восклицательному/вопросительному знаку
_SENTENCE_SPLIT = re.compile(r'(?<=[.!?])\s+(?=[A-ZА-ЯЁ0-9])')
# Размер батча (NLLB-200 тяжелее MarianMT, меньший батч для экономии памяти)
_BATCH_SIZE = 12


class TranslationService:
    def __init__(self, model_loader):
        self.model_loader = model_loader

    # Проверка доступности модели перевода
    def is_available(self) -> bool:
        return (self.model_loader.translation_model is not None
                and self.model_loader.translation_tokenizer is not None)

    # Разбиение текста на предложения с очисткой от NBSP и лишних пробелов
    def _split_sentences(self, text: str) -> list[str]:
        text = text.replace('\u00a0', ' ')
        text = ' '.join(text.split())
        parts = _SENTENCE_SPLIT.split(text)
        parts = [p.strip() for p in parts if p.strip()]
        return parts if parts else [text]

    # Перевод батча предложений через NLLB-200 (beam search, num_beams=2)
    def _translate_batch(self, sentences: list[str], src_lang: str, tgt_lang: str) -> list[str]:
        tokenizer = self.model_loader.translation_tokenizer
        model = self.model_loader.translation_model

        tokenizer.src_lang = src_lang
        inputs = tokenizer(
            sentences,
            return_tensors="pt",
            padding=True,
            truncation=True,
            max_length=512,
        )

        forced_bos = tokenizer.convert_tokens_to_ids(tgt_lang)

        with torch.inference_mode():
            output_ids = model.generate(
                **inputs,
                forced_bos_token_id=forced_bos,
                num_beams=2,
                max_length=512,
                early_stopping=True,
            )

        return [
            tokenizer.decode(ids, skip_special_tokens=True)
            for ids in output_ids
        ]

    # Перевод одного предложения без разбивки (для прогресс-бара)
    def translate_sentence(self, text: str, direction: str) -> str:
        tokenizer = self.model_loader.translation_tokenizer
        model = self.model_loader.translation_model

        if tokenizer is None or model is None:
            raise ValueError("Translation model not loaded")

        lang_codes = self.model_loader.LANG_CODES.get(direction)
        if lang_codes is None:
            raise ValueError(f"Unknown direction: {direction}")

        src_lang, tgt_lang = lang_codes
        text = _HTML_TAG.sub('', text).strip()
        return self._translate_batch([text], src_lang, tgt_lang)[0]

    # Полный перевод текста: очистка HTML, разбивка, пакетный перевод, сборка
    def translate(self, text: str, direction: str) -> str:
        tokenizer = self.model_loader.translation_tokenizer
        model = self.model_loader.translation_model

        if tokenizer is None or model is None:
            raise ValueError("Translation model not loaded")

        lang_codes = self.model_loader.LANG_CODES.get(direction)
        if lang_codes is None:
            raise ValueError(f"Unknown direction: {direction}")

        src_lang, tgt_lang = lang_codes

        text = _HTML_TAG.sub('', text)
        sentences = self._split_sentences(text)

        if len(sentences) <= 1:
            return self._translate_batch(sentences, src_lang, tgt_lang)[0]

        logger.info(
            "Translating %d sentences [%s], batch size %d",
            len(sentences), direction, _BATCH_SIZE,
        )

        translated_parts = []
        for i in range(0, len(sentences), _BATCH_SIZE):
            batch = sentences[i:i + _BATCH_SIZE]
            translated_parts.extend(self._translate_batch(batch, src_lang, tgt_lang))

        return ' '.join(translated_parts)
