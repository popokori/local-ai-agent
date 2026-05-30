"""Configuration du worker (lue via variables d'environnement)."""

from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    host: str = os.environ.get("WORKER_HOST", "127.0.0.1")
    port: int = int(os.environ.get("WORKER_PORT", "9000"))
    internal_token: str = os.environ.get("WORKER_TOKEN", "")
    embedding_model: str = os.environ.get("EMBEDDING_MODEL", "BAAI/bge-m3")
    device: str = os.environ.get("EMBEDDING_DEVICE", "cpu")
    embedding_dim: int = int(os.environ.get("EMBEDDING_DIM", "1024"))


SETTINGS = Settings()
