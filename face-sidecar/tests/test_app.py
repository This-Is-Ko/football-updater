import io
import json
import os
import sys
from types import SimpleNamespace

import numpy as np
import pytest
from fastapi.testclient import TestClient
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import app as sidecar  # noqa: E402
from matching import EMBEDDING_SIZE, DetectedFace, match_faces  # noqa: E402


def unit(seed: int) -> np.ndarray:
    vector = np.random.default_rng(seed).normal(size=EMBEDDING_SIZE).astype(np.float32)
    return vector / np.linalg.norm(vector)


def near(vector: np.ndarray, noise: float, seed: int) -> np.ndarray:
    jitter = np.random.default_rng(seed).normal(scale=noise, size=EMBEDDING_SIZE).astype(np.float32)
    mixed = vector + jitter
    return mixed / np.linalg.norm(mixed)


class FakeAnalyzer:
    def __init__(self, faces):
        self.faces = faces

    def get(self, image):
        return [SimpleNamespace(bbox=np.array(f.bbox), det_score=f.det_score, normed_embedding=f.embedding)
                for f in self.faces]


def jpeg_bytes(size=(64, 48)) -> bytes:
    buffer = io.BytesIO()
    Image.new("RGB", size, (10, 120, 40)).save(buffer, format="JPEG")
    return buffer.getvalue()


PLAYER = unit(1)
OTHER = unit(2)


@pytest.fixture
def client():
    return TestClient(sidecar.app)


def test_subject_is_largest_confident_face_and_scores_high_for_same_person():
    faces = [
        DetectedFace([0, 0, 10, 10], 0.9, OTHER),
        DetectedFace([0, 0, 100, 100], 0.9, near(PLAYER, 0.02, 3)),
        DetectedFace([0, 0, 200, 200], 0.2, OTHER),  # too unsure to count
    ]
    result = match_faces(faces, [near(PLAYER, 0.02, 4).tolist(), near(PLAYER, 0.02, 5).tolist()], 0.5)
    assert result["faceCount"] == 2
    assert result["subject"]["bbox"] == [0, 0, 100, 100]
    assert result["subject"]["maxSimilarity"] > 0.6
    assert result["bestMatch"]["index"] == result["subject"]["index"]


def test_different_person_scores_low():
    result = match_faces([DetectedFace([0, 0, 50, 50], 0.9, OTHER)], [PLAYER.tolist()], 0.5)
    assert result["subject"]["maxSimilarity"] < 0.2


def test_no_faces_gives_no_subject():
    result = match_faces([], [PLAYER.tolist()], 0.5)
    assert result["faceCount"] == 0 and result["subject"] is None


def test_bad_reference_size_is_rejected():
    with pytest.raises(ValueError):
        match_faces([], [[1.0, 2.0]], 0.5)


def test_embed_endpoint_returns_faces_largest_first(client):
    sidecar.set_analyzer(FakeAnalyzer([DetectedFace([0, 0, 5, 5], 0.9, OTHER), DetectedFace([0, 0, 40, 40], 0.95, PLAYER)]))
    response = client.post("/embed", files={"image": ("a.jpg", jpeg_bytes(), "image/jpeg")})
    assert response.status_code == 200
    faces = response.json()["faces"]
    assert len(faces) == 2
    assert faces[0]["bbox"] == [0, 0, 40, 40]
    assert len(faces[0]["embedding"]) == EMBEDDING_SIZE


def test_match_endpoint(client):
    sidecar.set_analyzer(FakeAnalyzer([DetectedFace([0, 0, 40, 40], 0.95, near(PLAYER, 0.02, 7))]))
    response = client.post("/match", files={"image": ("a.jpg", jpeg_bytes(), "image/jpeg")},
                           data={"references": json.dumps([PLAYER.tolist()])})
    assert response.status_code == 200
    assert response.json()["subject"]["maxSimilarity"] > 0.6


def test_match_rejects_missing_references(client):
    sidecar.set_analyzer(FakeAnalyzer([]))
    response = client.post("/match", files={"image": ("a.jpg", jpeg_bytes(), "image/jpeg")}, data={"references": "[]"})
    assert response.status_code == 400


def test_non_image_is_rejected(client):
    sidecar.set_analyzer(FakeAnalyzer([]))
    response = client.post("/embed", files={"image": ("a.jpg", b"not an image", "image/jpeg")})
    assert response.status_code == 400


def test_oversized_upload_is_rejected(client, monkeypatch):
    sidecar.set_analyzer(FakeAnalyzer([]))
    monkeypatch.setattr(sidecar, "MAX_UPLOAD_BYTES", 10)
    response = client.post("/embed", files={"image": ("a.jpg", jpeg_bytes(), "image/jpeg")})
    assert response.status_code == 413
