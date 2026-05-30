"""Extraction texte depuis PDF / DOCX / TXT.

Phase 2 reste simple : pypdf pour les PDF, python-docx pour les DOCX, brut UTF-8
pour les TXT/MD. Pas d'OCR (Phase 3) ni de tables structurées (Phase 3+).
"""

from __future__ import annotations

import io
import logging
from typing import List, Tuple

from docx import Document as DocxDocument
from pypdf import PdfReader

log = logging.getLogger(__name__)


def parse_file(file_bytes: bytes, file_name: str, mime_type: str | None) -> List[Tuple[int, str]]:
    """Renvoie [(page_number, text), ...]. Pour DOCX/TXT, une seule "page" (n=1)."""
    lower_name = (file_name or "").lower()
    mime = (mime_type or "").lower()

    if mime == "application/pdf" or lower_name.endswith(".pdf"):
        return _parse_pdf(file_bytes)
    if mime in {"application/vnd.openxmlformats-officedocument.wordprocessingml.document"} \
       or lower_name.endswith(".docx"):
        return _parse_docx(file_bytes)
    if mime.startswith("text/") or lower_name.endswith((".txt", ".md")):
        text = file_bytes.decode("utf-8", errors="replace").strip()
        return [(1, text)] if text else []
    raise ValueError(f"Unsupported file type: name={file_name!r} mime={mime_type!r}")


def _parse_pdf(file_bytes: bytes) -> List[Tuple[int, str]]:
    reader = PdfReader(io.BytesIO(file_bytes))
    pages: List[Tuple[int, str]] = []
    for i, page in enumerate(reader.pages, start=1):
        try:
            text = page.extract_text() or ""
        except Exception as ex:  # robustesse : on n'abandonne pas le doc pour 1 page
            log.warning("Failed to extract page %s: %s", i, ex)
            text = ""
        text = text.strip()
        if text:
            pages.append((i, text))
    return pages


def _parse_docx(file_bytes: bytes) -> List[Tuple[int, str]]:
    doc = DocxDocument(io.BytesIO(file_bytes))
    parts = []
    for para in doc.paragraphs:
        if para.text and para.text.strip():
            parts.append(para.text.strip())
    text = "\n".join(parts).strip()
    return [(1, text)] if text else []
