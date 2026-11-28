FROM python:3.11-slim

# insightface builds a small C extension
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    && apt-get purge -y build-essential && apt-get autoremove -y

ENV FACE_MODEL_PACK=buffalo_l \
    FACE_MODEL_ROOT=/models

# Bake the model into the image so the container never downloads at runtime
RUN python -c "from insightface.app import FaceAnalysis; FaceAnalysis(name='buffalo_l', root='/models', allowed_modules=['detection','recognition'], providers=['CPUExecutionProvider']).prepare(ctx_id=-1)"

COPY app.py matching.py ./

RUN useradd --system --no-create-home sidecar && chown -R sidecar /models
USER sidecar

EXPOSE 8001
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8001/health', timeout=4)"
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8001", "--workers", "1"]
