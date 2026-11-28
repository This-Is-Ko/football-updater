"""Pure matching logic, kept free of InsightFace so it can be tested without the model."""
from dataclasses import dataclass
from typing import List, Optional, Sequence

import numpy as np

EMBEDDING_SIZE = 512


@dataclass
class DetectedFace:
    bbox: List[float]  # x1, y1, x2, y2 in pixels
    det_score: float
    embedding: np.ndarray  # L2-normalised, EMBEDDING_SIZE floats

    @property
    def area(self) -> float:
        x1, y1, x2, y2 = self.bbox
        return max(0.0, x2 - x1) * max(0.0, y2 - y1)


def normalise(vector: Sequence[float]) -> np.ndarray:
    array = np.asarray(vector, dtype=np.float32)
    if array.shape != (EMBEDDING_SIZE,):
        raise ValueError(f"embedding must have {EMBEDDING_SIZE} values, got {array.shape}")
    norm = float(np.linalg.norm(array))
    if not np.isfinite(norm) or norm == 0:
        raise ValueError("embedding must be finite and non-zero")
    return array / norm


def sort_by_size(faces: List[DetectedFace]) -> List[DetectedFace]:
    return sorted(faces, key=lambda face: face.area, reverse=True)


def similarity_to_references(face: DetectedFace, references: np.ndarray) -> dict:
    """Cosine similarity of one face against the reference matrix (n x 512, rows normalised)."""
    scores = references @ face.embedding
    return {"max": float(np.max(scores)), "mean": float(np.mean(scores))}


def match_faces(faces: List[DetectedFace], references: Sequence[Sequence[float]], min_det_score: float) -> dict:
    """Score every confident face against the player's references.

    The subject is the largest confident face, since a standout photo is about one player. The caller
    decides what to do when another face scores higher than the subject.
    """
    if not references:
        raise ValueError("at least one reference embedding is required")
    reference_matrix = np.stack([normalise(reference) for reference in references])
    confident = sort_by_size([face for face in faces if face.det_score >= min_det_score])
    scored = []
    for index, face in enumerate(confident):
        score = similarity_to_references(face, reference_matrix)
        scored.append({
            "index": index,
            "bbox": [round(v, 1) for v in face.bbox],
            "detScore": round(face.det_score, 4),
            "maxSimilarity": round(score["max"], 4),
            "meanSimilarity": round(score["mean"], 4),
        })
    subject: Optional[dict] = scored[0] if scored else None
    best: Optional[dict] = max(scored, key=lambda s: s["maxSimilarity"]) if scored else None
    return {
        "faceCount": len(scored),
        "subject": subject,
        "bestMatch": best,
        "faces": scored,
    }
