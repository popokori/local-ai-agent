"""FastAPI worker — endpoints /health, /embed, /parse, /run-code.

Auth interne : header `X-Internal-Token` qui doit matcher `WORKER_TOKEN`.
Si `WORKER_TOKEN` est vide, l'auth est désactivée (dev uniquement).
"""

from __future__ import annotations

import logging
import time
from typing import List, Optional

from fastapi import Depends, FastAPI, File, Header, HTTPException, UploadFile
from pydantic import BaseModel, Field

from .embeddings import embed_texts, embedding_dim
from .parsing import parse_file
from .sandbox import run_code
from .settings import SETTINGS

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
log = logging.getLogger("worker")

app = FastAPI(title="LocalAiAgent Python Worker", version="0.3.0")

START_TIME = time.time()


def require_internal_token(x_internal_token: Optional[str] = Header(default=None)) -> None:
    expected = SETTINGS.internal_token
    if not expected:
        return
    if x_internal_token != expected:
        raise HTTPException(status_code=401, detail="invalid internal token")


# ─────────────────────────────── DTOs ───────────────────────────────


class EmbedRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1)
    model: Optional[str] = None


class EmbedResponse(BaseModel):
    model: str
    dim: int
    embeddings: List[List[float]]


class ParsedPage(BaseModel):
    n: int
    text: str


class ParseResponse(BaseModel):
    fileName: str
    mimeType: Optional[str] = None
    pageCount: int
    pages: List[ParsedPage]


class WorkerHealth(BaseModel):
    status: str
    embeddingModel: str
    embeddingDim: Optional[int] = None
    uptimeSeconds: int


class RunCodeRequest(BaseModel):
    code: str = Field(..., min_length=1, max_length=20_000)
    timeoutSec: Optional[float] = Field(default=8.0, ge=0.5, le=30.0)


class RunCodeResponse(BaseModel):
    stdout: str
    stderr: str
    exitCode: int
    timedOut: bool
    truncated: bool


# ─────────────────────────────── Routes ───────────────────────────────


@app.get("/health", response_model=WorkerHealth)
def health() -> WorkerHealth:
    from .embeddings import _model  # type: ignore[attr-defined]
    dim = _model.get_sentence_embedding_dimension() if _model is not None else None
    return WorkerHealth(
        status="UP",
        embeddingModel=SETTINGS.embedding_model,
        embeddingDim=dim,
        uptimeSeconds=int(time.time() - START_TIME),
    )


@app.post("/embed", response_model=EmbedResponse, dependencies=[Depends(require_internal_token)])
def embed(req: EmbedRequest) -> EmbedResponse:
    start = time.time()
    vectors = embed_texts(req.texts)
    log.info("Embedded %d text(s) in %.2fs", len(req.texts), time.time() - start)
    return EmbedResponse(
        model=SETTINGS.embedding_model,
        dim=embedding_dim(),
        embeddings=vectors,
    )


@app.post("/parse", response_model=ParseResponse, dependencies=[Depends(require_internal_token)])
async def parse(file: UploadFile = File(...)) -> ParseResponse:
    content = await file.read()
    try:
        pages = parse_file(content, file.filename or "upload", file.content_type)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex))
    return ParseResponse(
        fileName=file.filename or "upload",
        mimeType=file.content_type,
        pageCount=len(pages),
        pages=[ParsedPage(n=n, text=t) for n, t in pages],
    )


@app.post("/run-code", response_model=RunCodeResponse, dependencies=[Depends(require_internal_token)])
def run_code_endpoint(req: RunCodeRequest) -> RunCodeResponse:
    start = time.time()
    result = run_code(req.code, timeout_sec=req.timeoutSec or 8.0)
    log.info("Ran code (%d chars) in %.2fs exit=%d timed_out=%s",
             len(req.code), time.time() - start, result.exit_code, result.timed_out)
    return RunCodeResponse(
        stdout=result.stdout,
        stderr=result.stderr,
        exitCode=result.exit_code,
        timedOut=result.timed_out,
        truncated=result.truncated,
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host=SETTINGS.host, port=SETTINGS.port, log_level="info")
