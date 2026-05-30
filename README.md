# LocalAiAgent — Backend Spring Boot (MVP Phase 1)

Assistant IA local, généraliste et extensible. Backend Spring Boot 3 / Java 21,
LLM local via Ollama (CPU-friendly par défaut), streaming SSE, sécurité JWT.

Cible matériel : **Windows Server 2019**, Intel Xeon 6 cœurs, 64 Go RAM, sans GPU.

## Périmètre Phase 1

Modules livrés : `auth`, `user`, `chat`, `message`, `llm` (+ diagnostic), `streaming`,
`audit` minimal, `security`, `common`, `agent` (pass-through), `science` (no-op).

Préparés (interfaces vides) : `document`, `rag`, `memory`, `tools`, `websearch`,
`worker` Python.

Voir [`docs/PHASE_ROADMAP.md`](docs/PHASE_ROADMAP.md) pour les phases suivantes.

---

## Deux modes de déploiement

### Mode A — Docker (recommandé)

Pré-requis : Docker Desktop / Engine sur l'hôte.

```bash
cp .env.example .env       # adapter JWT_SECRET, DB_PASS
cd infra
docker compose up -d
./ollama/pull-models.sh    # ou .\ollama\pull-models.ps1 sur Windows
```

Services :
- PostgreSQL → `localhost:5432`
- Ollama (CPU) → `localhost:11434`
- Backend Spring Boot → `localhost:8080`
- Swagger UI → http://localhost:8080/swagger-ui.html

### Mode B — Lancement local Windows (sans Docker)

Pour itérer rapidement depuis l'IDE. Voir [`docs/WINDOWS_LOCAL.md`](docs/WINDOWS_LOCAL.md).

```powershell
# Pré-requis : Java 21, Maven 3.9+, PostgreSQL 16, Ollama Windows
$env:DB_PASS = "..."
$env:JWT_SECRET = "..."   # ≥ 32 chars
$env:LLM_BASE_URL = "http://localhost:11434/v1"

cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=windows-local
```

---

## Premier test (smoke)

```bash
# Inscription
curl -X POST localhost:8080/api/v1/auth/register \
  -H 'content-type: application/json' \
  -d '{"username":"alice","email":"a@a.com","password":"Password1!","displayName":"Alice"}'

# Login
TOK=$(curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'content-type: application/json' \
  -d '{"username":"alice","password":"Password1!"}' | jq -r .accessToken)

# Diagnostic LLM (dev — accessible authentifié ; prod — ADMIN/EXPERT)
curl -H "Authorization: Bearer $TOK" localhost:8080/api/v1/llm/health

# Crée une session
SID=$(curl -s -X POST localhost:8080/api/v1/chats \
  -H "Authorization: Bearer $TOK" -H 'content-type: application/json' \
  -d '{"title":"hello"}' | jq -r .id)

# Envoie un message — flux SSE
curl -N -X POST localhost:8080/api/v1/chats/$SID/messages \
  -H "Authorization: Bearer $TOK" -H 'content-type: application/json' \
  -d '{"content":"Bonjour, présente-toi en 2 phrases","clientRequestId":"11111111-1111-1111-1111-111111111111"}'
```

Le script complet est dans [`docs/SMOKE_TEST.md`](docs/SMOKE_TEST.md).

---

## Configuration LLM

Modèle par défaut adapté à du CPU Xeon 6 cœurs : `llama3.1:8b`.

Pour changer sans recompiler :
```bash
export LLM_DEFAULT_MODEL=qwen2.5:7b-instruct
# ou
export LLM_DEFAULT_MODEL=mistral:7b-instruct
```

Modèles plus gros (lents sur CPU mais possibles) : `qwen2.5:14b-instruct`.

---

## Documentation

| Document | Contenu |
|---|---|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Vue d'ensemble, modules, flux |
| [`docs/DESIGN_RULES.md`](docs/DESIGN_RULES.md) | Règles transversales (ownership, sync PG/Qdrant, disclaimer médical, etc.) |
| [`docs/PHASE_ROADMAP.md`](docs/PHASE_ROADMAP.md) | Roadmap Phases 1 → 5 |
| [`docs/WINDOWS_LOCAL.md`](docs/WINDOWS_LOCAL.md) | Installation Windows sans Docker |
| [`docs/SMOKE_TEST.md`](docs/SMOKE_TEST.md) | Scénario de test end-to-end |

---

## Stack

Spring Boot 3.3 · Java 21 (virtual threads) · Spring Security JWT · Spring Data JPA ·
PostgreSQL 16 · Flyway · MapStruct · Lombok · SSE · WebClient · Ollama (Phase 1)
