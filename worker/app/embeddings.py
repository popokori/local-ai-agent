"""Encapsulation du modèle d'embeddings BGE-M3.

Le modèle est chargé une seule fois au démarrage (lazy lock dans `get_model`).
Sur CPU il prend ~2 Go RAM et tourne à quelques dizaines de tokens/s — suffisant
pour ingester quelques documents et répondre aux requêtes utilisateur en temps réel.
"""

from __future__ import annotations

import logging
import threading
from typing import List

from sentence_transformers import SentenceTransformer

from .settings import SETTINGS

log = logging.getLogger(__name__)

_model_lock = threading.Lock()
_model: SentenceTransformer | None = None


def get_model() -> SentenceTransformer:
    global _model
    if _model is None:
        with _model_lock:
            if _model is None:
                log.info("Loading embedding model %s on %s", SETTINGS.embedding_model, SETTINGS.device)
                _model = SentenceTransformer(SETTINGS.embedding_model, device=SETTINGS.device)
                log.info("Embedding model loaded: dim=%s", _model.get_sentence_embedding_dimension())
    return _model


def embed_texts(texts: List[str]) -> List[List[float]]:
    if not texts:
        return []
    model = get_model()
    # normalize=True → cosine ↔ dot product, simplifie la similarité côté Java
    vectors = model.encode(
        texts,
        normalize_embeddings=True,
        convert_to_numpy=True,
        show_progress_bar=False,
    )
    return vectors.tolist()


def embedding_dim() -> int:
    return int(get_model().get_sentence_embedding_dimension())
