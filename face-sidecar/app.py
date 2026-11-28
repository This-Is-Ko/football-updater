"""Stateless face embedding and matching sidecar for football-updater.

It keeps no data: the Java app sends the image and, for matching, the player's reference embeddings,
which live in MySQL. Nothing is written to disk and images are not logged.
"""
import io
import json
import logging
import os
import threading
from typing import List

import numpy as np
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from PIL import Image, UnidentifiedImageError

from matching import DetectedFace, match_faces, sort_by_size

MODEL_PACK = os.environ.get("FACE_MODEL_PACK", "buffalo_l")
MODEL_ROOT = os.environ.get("FACE_MODEL_ROOT", "/models")
DET_SIZE = int(os.environ.get("FACE_DET_SIZE", "640"))
MIN_DET_SCORE = float(os.environ.get("FACE_MIN_DET_SCORE", "0.5"))
MAX_UPLOAD_BYTES = int(os.environ.get("FACE_MAX_UPLOAD_BYTES", str(10 * 1024 * 1024)))
MAX_PIXELS = int(os.environ.get("FACE_MAX_PIXELS", "40000000"))
MAX_REFERENCES = int(os.environ.get("FACE_MAX_REFERENCES", "20"))
MAX_EDGE = int(os.environ.get("FACE_MAX_EDGE", "1920"))

Image.MAX_IMAGE_PIXELS = MAX_PIXELS

log = logging.getLogger("face-sidecar")
logging.basicConfig(level=logging.INFO)

app = FastAPI(title="face-sidecar")

_analyzer = None
_analyzer_lock = threading.Lock()


def get_analyzer():
    """Load InsightFace once. Detection plus the ArcFace recognition model only."""
    global _analyzer
    with _analyzer_lock:
        if _analyzer is None:
            from insightface.app import FaceAnalysis

            analyzer = FaceAnalysis(name=MODEL_PACK, root=MODEL_ROOT,
                                    allowed_modules=["detection", "recognition"],
                                    providers=["CPUExecutionProvider"])
            analyzer.prepare(ctx_id=-1, det_size=(DET_SIZE, DET_SIZE))
            _analyzer = analyzer
        return _analyzer


def set_analyzer(analyzer):
    """For tests: swap in a fake analyzer."""
    global _analyzer
    _analyzer = analyzer


async def read_image(upload: UploadFile) -> np.ndarray:
    data = await upload.read(MAX_UPLOAD_BYTES + 1)
    if len(data) > MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="image too large")
    try:
        image = Image.open(io.BytesIO(data))
        width, height = image.size
        if width * height > MAX_PIXELS:
            raise HTTPException(status_code=413, detail="image has too many pixels")
        image = image.convert("RGB")
    except (UnidentifiedImageError, OSError, Image.DecompressionBombError):
        raise HTTPException(status_code=400, detail="not a readable image")
    if max(image.size) > MAX_EDGE:
        image.thumbnail((MAX_EDGE, MAX_EDGE))
    # InsightFace expects BGR like OpenCV
    return np.asarray(image)[:, :, ::-1].copy()


def detect(image: np.ndarray) -> List[DetectedFace]:
    analyzer = get_analyzer()
    with _analyzer_lock:
        raw_faces = analyzer.get(image)
    faces = []
    for face in raw_faces:
        embedding = getattr(face, "normed_embedding", None)
        if embedding is None:
            continue
        faces.append(DetectedFace(bbox=[float(v) for v in face.bbox], det_score=float(face.det_score),
                                  embedding=np.asarray(embedding, dtype=np.float32)))
    return faces


@app.get("/health")
def health():
    get_analyzer()
    return {"status": "ok", "model": MODEL_PACK}


@app.post("/embed")
async def embed(image: UploadFile = File(...)):
    """Detect faces and return each face's embedding, largest first. Used to enrol reference photos."""
    faces = sort_by_size([f for f in detect(await read_image(image)) if f.det_score >= MIN_DET_SCORE])
    return {
        "model": MODEL_PACK,
        "faces": [{"bbox": [round(v, 1) for v in f.bbox], "detScore": round(f.det_score, 4),
                   "embedding": [float(v) for v in f.embedding]} for f in faces],
    }


@app.post("/match")
async def match(image: UploadFile = File(...), references: str = Form(...)):
    """Compare every face in the image with the given reference embeddings (JSON list of 512-float lists)."""
    try:
        reference_list = json.loads(references)
    except json.JSONDecodeError:
        raise HTTPException(status_code=400, detail="references must be JSON")
    if not isinstance(reference_list, list) or not reference_list or len(reference_list) > MAX_REFERENCES:
        raise HTTPException(status_code=400, detail=f"send 1 to {MAX_REFERENCES} reference embeddings")
    faces = detect(await read_image(image))
    try:
        result = match_faces(faces, reference_list, MIN_DET_SCORE)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex))
    result["model"] = MODEL_PACK
    return result
