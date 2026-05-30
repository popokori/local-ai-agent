# Python Worker

Service FastAPI séparé qui héberge :
- modèle d'embeddings **BGE-M3** (1024 dim, multilingue FR/EN)
- parsing **PDF / DOCX / TXT / MD** (pypdf + python-docx)

Le backend Spring Boot appelle ces endpoints via HTTP (WebClient).

## Endpoints

| Méthode | Path | Auth | Description |
|---|---|---|---|
| GET  | `/health` | public | UP + uptime + dim modèle si chargé |
| POST | `/embed`  | `X-Internal-Token` | `{texts: [...]}` → `{embeddings: [[...]]}` |
| POST | `/parse`  | `X-Internal-Token` | multipart `file` → `{pages: [{n, text}], ...}` |

L'auth interne (`X-Internal-Token`) est vérifiée si la variable `WORKER_TOKEN` est
non vide. Sinon désactivée (dev).

## Lancement (Windows local)

```powershell
cd worker
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt

# Optionnel : token interne pour matcher le backend
$env:WORKER_TOKEN = "<un secret partagé>"

python -m app.main
```

Au **premier appel à `/embed`**, le modèle BGE-M3 est téléchargé (~2.3 Go)
depuis Hugging Face puis chargé en mémoire (~2 Go RAM). Les appels suivants
sont rapides (~30-50 tokens/s sur CPU).

## Phase 3+ (non implémenté ici)

- `/rerank` — bge-reranker-v2-m3
- `/ocr` — paddleocr / tesseract
- `/run-code` — exécution Python sandboxée
