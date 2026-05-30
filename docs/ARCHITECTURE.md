# Architecture — Phase 1

## Vue d'ensemble

```
Frontend (Angular/Ionic ou React, hors scope)
   │
   ▼ HTTPS REST + SSE
┌─────────────────────────────────────────────────────────┐
│ Spring Boot 3 — Java 21 — virtual threads               │
│                                                          │
│   AuthController    UserController    ChatController    │
│   MessageController LlmDiagnosticController             │
│         │                  │                 │           │
│         ▼                  ▼                 ▼           │
│   AuthService  ChatService  MessageService              │
│                                │                         │
│                                ▼                         │
│                         AgentService                     │
│                                │                         │
│              ┌─────────────────┴──────────────┐         │
│              ▼                                ▼         │
│        PromptBuilder                     LlmService     │
│              │                                │         │
│              ▼                                ▼         │
│      prompts/*.md                  OpenAiCompatibleLlmClient
│                                               │         │
│   AuditService (async)                        │         │
└───────────────────────────────────────────────┼─────────┘
                                                │
                                ┌───────────────▼──────────────┐
                                │  Ollama  (port 11434)        │
                                │  OpenAI-compatible /v1/chat  │
                                └──────────────────────────────┘

       ┌──────────────────────────┐
       │  PostgreSQL 16 + Flyway  │
       │  users, refresh_tokens,  │
       │  chat_sessions, messages │
       │  audit_logs              │
       └──────────────────────────┘
```

## Modules Java

| Package | Rôle | Phase |
|---|---|---|
| `common` | exceptions, config Web/Async/Jackson, pagination | 1 |
| `security` | JWT stateless, filtre, handlers 401/403, UserDetails | 1 |
| `auth` | register / login / refresh / logout | 1 |
| `user` | `/users/me` | 1 |
| `chat` | sessions CRUD, ownership 404 | 1 |
| `message` | historique + endpoint SSE (idempotence) | 1 |
| `llm` | client OpenAI-compatible (Ollama/vLLM), diagnostic | 1 |
| `streaming` | helpers SSE | 1 |
| `agent` | pass-through Phase 1, ReAct en Phase 2+ | 1 (squelette) |
| `audit` | log async des actions sensibles | 1 minimal |
| `science` | `NoopScientificRouter` | 1 stub |
| `document`, `rag`, `memory`, `tools`, `websearch` | interfaces vides — Phase 2/3/4 | — |

## Flux d'une requête `POST /api/v1/chats/{id}/messages`

1. `JwtAuthenticationFilter` valide le token → peuple `SecurityContext`.
2. `MessageController.send(...)` audite l'action et délègue à `MessageService.sendAndStream(...)`.
3. `ChatService.loadOwned(...)` vérifie l'ownership (404 sinon).
4. Idempotence : si `clientRequestId` déjà vu → re-stream la réponse cachée et termine.
5. Persiste un `Message(role=USER)`.
6. Construit l'historique fenêtré (20 derniers messages).
7. `AgentService.handle(...)` :
   - `PromptBuilder` produit `[system(normal + medical_disclaimer), ...history, user(content)]`.
   - `LlmService.streamChat(...)` ouvre un flux SSE vers Ollama via `WebClient`.
8. `OpenAiCompatibleLlmClient` parse les chunks `data: {...}` → `ChatDelta`.
9. Pour chaque `ChatDelta` :
   - **token** → `SseStreamService.send(emitter, TOKEN, {text})`.
   - **fin** → persiste `Message(role=ASSISTANT)` + envoie `FINAL`.
10. `AuditService.log(LLM_CALL, ...)` (async, virtual thread).

## Choix techniques majeurs

- **Spring MVC + SSE + virtual threads**, pas WebFlux : compatibilité JPA bloquant, simplicité, perf suffisante.
- **`LlmClient` unique OpenAI-compatible** : Ollama / vLLM interchangeables par config.
- **Modèle 8B par défaut** (`llama3.1:8b`) — CPU-friendly Xeon 6 cœurs / 64 Go RAM.
- **Disclaimer médical injecté systématiquement** par `PromptBuilder` (cf. DESIGN_RULES.md §4).
- **Ownership systématique** (`findByIdAndOwnerId`) — anticipe la sécurité multi-utilisateurs.
- **Idempotence via `clientRequestId`** — un message rejoué ne re-déclenche pas le LLM.
- **Audit asynchrone** — pas de pénalité de latence.
- **Resilience4j en dépendance mais désactivé** — timeout WebClient simple en Phase 1.

## Persistance

| Table | Rôle |
|---|---|
| `users` / `roles` / `user_roles` | identité |
| `refresh_tokens` | hash SHA-256, rotation à chaque usage |
| `chat_sessions` | sessions de chat (owner_id, mode, model_name) |
| `messages` | historique (role, content, tokens, latency, `client_request_id` unique) + colonnes jsonb prêtes pour Phase 2 (`tool_calls_json`, `sources_json`) |
| `audit_logs` | toutes les actions sensibles |
