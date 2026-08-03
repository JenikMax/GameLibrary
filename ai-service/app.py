# FastAPI-приложение AI-сервиса GameLibrary
# Предоставляет эндпоинты: health, translate, embed, embed/batch, vision/classify
# Модели загружаются при старте приложения через lifespan-обработчик

import logging
import os
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from model_loader import ModelLoader
from translation_service import TranslationService
from embedding_service import EmbeddingService
from vision_service import VisionService

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

# Глобальные экземпляры сервисов, инициализируются при старте
model_loader: ModelLoader = None
translation_service: TranslationService = None
embedding_service: EmbeddingService = None
vision_service: VisionService = None


# Инициализация и завершение работы приложения
@asynccontextmanager
async def lifespan(app: FastAPI):
    global model_loader, translation_service, embedding_service, vision_service

    models_dir = os.environ.get("MODELS_DIR", "/models")
    logger.info("Loading AI models from: %s", models_dir)

    # Загрузка всех моделей HuggingFace при старте
    start = time.time()
    model_loader = ModelLoader(models_dir)
    model_loader.load_all()
    elapsed = time.time() - start
    logger.info("All models loaded in %.1fs", elapsed)

    # Инициализация сервисов перевода и эмбеддингов
    translation_service = TranslationService(model_loader)
    embedding_service = EmbeddingService(model_loader)
    vision_service = VisionService(model_loader)

    yield

    logger.info("Shutting down AI service")


app = FastAPI(title="GameLibrary AI Service", lifespan=lifespan)


# Pydantic-модели запросов и ответов

class TranslateRequest(BaseModel):
    text: str
    direction: str


class TranslateResponse(BaseModel):
    translated: str


class TranslateSentenceRequest(BaseModel):
    text: str
    direction: str


class TranslateSentenceResponse(BaseModel):
    translated: str


class EmbedRequest(BaseModel):
    text: str


class EmbedResponse(BaseModel):
    embedding: list[float]


class EmbedBatchRequest(BaseModel):
    texts: list[str]


class EmbedBatchResponse(BaseModel):
    embeddings: list[list[float]]


class VisionClassifyRequest(BaseModel):
    image_base64: str
    labels: list[str]
    top_k: int = 10


class VisionClassifyMultiRequest(BaseModel):
    images_base64: list[str]
    labels: list[str]
    top_k: int = 10


class VisionClassifyResponse(BaseModel):
    matches: list[dict]


class HealthResponse(BaseModel):
    status: str
    models: dict


# Проверка здоровья сервиса и статуса загруженных моделей
@app.get("/health", response_model=HealthResponse)
async def health():
    return HealthResponse(
        status="ok",
        models=model_loader.get_status() if model_loader else {}
    )


# Перевод текста (ru↔en) через NLLB-200
@app.post("/translate", response_model=TranslateResponse)
async def translate(req: TranslateRequest):
    if not translation_service or not translation_service.is_available():
        raise HTTPException(status_code=503, detail="Translation model not available")
    try:
        translated = translation_service.translate(req.text, req.direction)
        return TranslateResponse(translated=translated)
    except Exception as e:
        logger.error("Translation failed: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# Перевод одного предложения (ru↔en) без разбивки
@app.post("/translate/sentence", response_model=TranslateSentenceResponse)
async def translate_sentence(req: TranslateSentenceRequest):
    if not translation_service or not translation_service.is_available():
        raise HTTPException(status_code=503, detail="Translation model not available")
    try:
        translated = translation_service.translate_sentence(req.text, req.direction)
        return TranslateSentenceResponse(translated=translated)
    except Exception as e:
        logger.error("Sentence translation failed: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# Получение векторного эмбеддинга текста (multilingual-e5-small, 384 dim)
@app.post("/embed", response_model=EmbedResponse)
async def embed(req: EmbedRequest):
    if not embedding_service or not embedding_service.is_available():
        raise HTTPException(status_code=503, detail="Embedding model not available")
    try:
        embedding = embedding_service.embed(req.text)
        return EmbedResponse(embedding=embedding.tolist())
    except Exception as e:
        logger.error("Embedding failed: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# Пакетное получение эмбеддингов для нескольких текстов
@app.post("/embed/batch", response_model=EmbedBatchResponse)
async def embed_batch(req: EmbedBatchRequest):
    if not embedding_service or not embedding_service.is_available():
        raise HTTPException(status_code=503, detail="Embedding model not available")
    try:
        embeddings = embedding_service.embed_batch(req.texts)
        return EmbedBatchResponse(embeddings=[e.tolist() for e in embeddings])
    except Exception as e:
        logger.error("Batch embedding failed: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# Классификация скриншота по текстовым меткам через CLIP (zero-shot)
@app.post("/vision/classify", response_model=VisionClassifyResponse)
async def classify_image(req: VisionClassifyRequest):
    if not vision_service or not vision_service.is_available():
        raise HTTPException(status_code=503, detail="Vision model not available")
    try:
        matches = vision_service.classify(req.image_base64, req.labels, req.top_k)
        return VisionClassifyResponse(matches=matches)
    except Exception as e:
        logger.error("Vision classification failed: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# Классификация нескольких скриншотов с агрегацией результатов
@app.post("/vision/classify-multi", response_model=VisionClassifyResponse)
async def classify_images_multi(req: VisionClassifyMultiRequest):
    if not vision_service or not vision_service.is_available():
        raise HTTPException(status_code=503, detail="Vision model not available")
    try:
        matches = vision_service.classify_multi(req.images_base64, req.labels, req.top_k)
        return VisionClassifyResponse(matches=matches)
    except Exception as e:
        logger.error("Vision multi-classify failed: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
