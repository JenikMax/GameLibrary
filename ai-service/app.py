import logging
import os
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from model_loader import ModelLoader
from translation_service import TranslationService
from embedding_service import EmbeddingService

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

model_loader: ModelLoader = None
translation_service: TranslationService = None
embedding_service: EmbeddingService = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global model_loader, translation_service, embedding_service

    models_dir = os.environ.get("MODELS_DIR", "/models")
    logger.info("Loading AI models from: %s", models_dir)

    start = time.time()
    model_loader = ModelLoader(models_dir)
    model_loader.load_all()
    elapsed = time.time() - start
    logger.info("All models loaded in %.1fs", elapsed)

    translation_service = TranslationService(model_loader)
    embedding_service = EmbeddingService(model_loader)

    yield

    logger.info("Shutting down AI service")


app = FastAPI(title="GameLibrary AI Service", lifespan=lifespan)


class TranslateRequest(BaseModel):
    text: str
    direction: str


class TranslateResponse(BaseModel):
    translated: str


class EmbedRequest(BaseModel):
    text: str


class EmbedResponse(BaseModel):
    embedding: list[float]


class EmbedBatchRequest(BaseModel):
    texts: list[str]


class EmbedBatchResponse(BaseModel):
    embeddings: list[list[float]]


class HealthResponse(BaseModel):
    status: str
    models: dict


@app.get("/health", response_model=HealthResponse)
async def health():
    return HealthResponse(
        status="ok",
        models=model_loader.get_status() if model_loader else {}
    )


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
