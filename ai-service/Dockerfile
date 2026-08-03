# ============================================================
# ai-service/Dockerfile — Python FastAPI AI-сервис
# PyTorch + HuggingFace transformers для эмбеддингов
# и перевода. Модели автоматически загружаются при старте.
# ============================================================

FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir torch==2.5.1 --index-url https://download.pytorch.org/whl/cpu && \
    pip install --no-cache-dir -r requirements.txt

COPY . .

# Директория для кэша моделей HuggingFace
ENV MODELS_DIR=/models
ENV HF_HOME=/models/hf_cache

# Запуск FastAPI приложения (один worker для избежания дублирования моделей в памяти)
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
