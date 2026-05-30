# Phase Roadmap

| Phase | Périmètre | Critère de sortie |
|---|---|---|
| **1 — MVP** (livrée) | auth, user, chat, message, llm + diagnostic, streaming, audit minimal + interfaces stubs | `docker compose up` → register → login → créer chat → envoyer message → tokens streamés depuis Ollama → historique persisté → `GET /llm/health` répond |
| **2 — Documents + RAG** | `document`, `rag`, Qdrant, KB unique par user, worker Python (`/parse`, `/embed`), tool `rag_search`, événement SSE `SOURCE`, ingestion async. Activation Resilience4j complet. | Upload PDF → indexé → question → réponse avec citations + n° page. Cohérence PG↔Qdrant testée. |
| **3 — Outils + WebSearch** | `tools` réel (`Tool`, `ToolRegistry` peuplé), `ToolDispatcher` actif, `SearxngClient`, `web_search`/`web_fetch`/`calculator`, `python_exec` via worker, boucle ReAct active dans `AgentService` | Question type "calcule X puis cherche Y" → trace tools + sources |
| **4 — Mémoire + Multi-KB + Modes** | `memory` (profil + épisodique + sémantique), `MemoryConsolidator` async, `KnowledgeBase` multiple, `ScientificRouter` réel, modes EXPERT et FACT_CHECK câblés | Mémoire persiste entre sessions ; routing auto vers KB scientifique ; mode FACT_CHECK annote la réponse |
| **5 — Audit AOP + Observabilité + Durcissement** | `@Audited` AOP, dashboards Grafana, rate limit auth (bucket4j), tests de charge SSE, scan deps | Production-ready : k6 50 SSE concurrents stable, audit complet de chaque tool/LLM call |

Chaque phase = livraison **stable, compilable, testable**.

## Ce qui est déjà préparé (Phase 1) pour les phases suivantes

- Colonnes JSONB `tool_calls_json` et `sources_json` dans `messages`.
- Enum `StreamEventType` avec `TOOL_START`, `TOOL_END`, `SOURCE` (non utilisés en Phase 1).
- Enum `ChatMode` complète avec `EXPERT` et `FACT_CHECK`.
- Interfaces vides : `DocumentService`, `RagService`, `QdrantClient`, `MemoryService`, `Tool`, `ToolRegistry`, `ToolDispatcher`, `ScientificRouter`, `WebSearchService`.
- `LlmProperties` expose déjà `expertModel` et `factCheckModel`.
- Profils Spring séparés (`dev`, `prod`, `windows-local`) prêts.
- Override Docker GPU (`docker-compose.gpu.yml`) pour passer au GPU sans refonte.
