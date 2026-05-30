#!/usr/bin/env bash
# Pré-télécharge les modèles Ollama nécessaires. À lancer après "docker compose up -d".
set -euo pipefail

MODEL="${LLM_DEFAULT_MODEL:-llama3.1:8b}"
CONTAINER="${OLLAMA_CONTAINER:-localaiagent-ollama}"

echo "Pulling model $MODEL into $CONTAINER..."
docker exec "$CONTAINER" ollama pull "$MODEL"
echo "Done. Available models:"
docker exec "$CONTAINER" ollama list
