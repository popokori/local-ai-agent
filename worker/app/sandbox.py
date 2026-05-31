"""Exécution de code Python en sous-processus avec timeout.

Sécurité Phase 3 (Windows, sans Docker) — modèle de menace LIMITÉ :
- single-user (l'utilisateur du backend),
- pas d'isolation réseau / FS / privilèges côté OS,
- on compte sur le timeout et l'environnement déjà restreint du worker.

Le script tourne avec `-I` (isolated : ignore PYTHONPATH et user site-packages)
MAIS sans `-S`, donc les packages installés dans le venv du worker (stdlib +
requests, numpy, pandas) sont importables. Whitelist gérée par ce que le venv
contient — voir worker/requirements.txt.

Suffisant pour un assistant local personnel. À renforcer en Phase 5 avec
un sandbox réel (Docker, firejail) si multi-tenant ou prod ouverte.
"""

from __future__ import annotations

import logging
import os
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path

log = logging.getLogger(__name__)

MAX_OUTPUT_CHARS = 8_000

# Vars d'environnement contenant probablement des secrets — exclues du subprocess.
# Toute clé d'env contenant un de ces substrings (insensible casse) est filtrée.
_SECRET_NAME_FRAGMENTS = (
    "TOKEN", "SECRET", "PASSWORD", "PASS", "API_KEY", "APIKEY",
    "PRIVATE_KEY", "CREDENTIAL", "AUTH",
)


def _build_safe_env() -> dict[str, str]:
    """Copie de os.environ moins les vars sensibles. Garde SystemRoot, PATH, TEMP,
    USERPROFILE, etc. dont Windows a besoin pour DNS / DLLs / I/O."""
    env = {}
    for k, v in os.environ.items():
        upper = k.upper()
        if any(frag in upper for frag in _SECRET_NAME_FRAGMENTS):
            continue
        env[k] = v
    env["PYTHONIOENCODING"] = "utf-8"
    env["PYTHONUTF8"] = "1"
    return env


@dataclass
class CodeRun:
    stdout: str
    stderr: str
    exit_code: int
    timed_out: bool
    truncated: bool


def run_code(code: str, timeout_sec: float = 8.0) -> CodeRun:
    if not code or not code.strip():
        return CodeRun(stdout="", stderr="empty code", exit_code=2, timed_out=False, truncated=False)

    with tempfile.TemporaryDirectory(prefix="laa_pyrun_") as tmpdir:
        script_path = Path(tmpdir) / "script.py"
        script_path.write_text(code, encoding="utf-8")
        try:
            proc = subprocess.run(
                [sys.executable, "-I", str(script_path)],
                cwd=tmpdir,
                capture_output=True,
                text=True,
                timeout=timeout_sec,
                env=_build_safe_env(),
            )
            stdout = proc.stdout or ""
            stderr = proc.stderr or ""
            truncated = False
            if len(stdout) > MAX_OUTPUT_CHARS:
                stdout = stdout[:MAX_OUTPUT_CHARS] + "\n[stdout truncated]"
                truncated = True
            if len(stderr) > MAX_OUTPUT_CHARS:
                stderr = stderr[:MAX_OUTPUT_CHARS] + "\n[stderr truncated]"
                truncated = True
            return CodeRun(
                stdout=stdout,
                stderr=stderr,
                exit_code=proc.returncode,
                timed_out=False,
                truncated=truncated,
            )
        except subprocess.TimeoutExpired as ex:
            log.info("python_exec timed out after %.1fs", timeout_sec)
            return CodeRun(
                stdout=(ex.stdout.decode("utf-8", errors="replace") if isinstance(ex.stdout, bytes) else (ex.stdout or "")),
                stderr=f"timeout after {timeout_sec}s",
                exit_code=124,
                timed_out=True,
                truncated=False,
            )
        except Exception as ex:
            log.warning("python_exec failed: %s", ex)
            return CodeRun(stdout="", stderr=str(ex), exit_code=1, timed_out=False, truncated=False)
