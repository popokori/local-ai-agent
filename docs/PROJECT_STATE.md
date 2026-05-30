# LocalAiAgent — État du projet et roadmap

Document de référence pour reprendre / faire évoluer le projet plus tard.
Tout ce qui a été livré (**backend Phase 1→4 + corrections post-4 + frontend F1→F6**),
tout ce qui reste à faire, et les décisions prises.

> Pour la doc frontend détaillée, voir [`FRONTEND_STATE.md`](FRONTEND_STATE.md).
> Pour l'installation pas-à-pas, voir [`INSTALL_AND_RUN.md`](INSTALL_AND_RUN.md).

---

## 1. Vue d'ensemble

Assistant IA local, généraliste, multi-domaines (science, biologie, chimie,
médecine, informatique, mathématiques, documents personnels, médias, culture…),
avec :

- **LLM local Ollama** (`dolphin-mistral:7b` actuellement par défaut, switchable via env)
- **Embeddings BGE-M3** (1024 dim) via worker Python
- **Stockage vectoriel brute-force PostgreSQL** (BYTEA), pas de Qdrant/pgvector
- **Streaming SSE** token par token (auto-détection ReAct vs réponse directe)
- **Mémoire long terme** : profil structuré + souvenirs épisodiques (consolidation auto-désactivable)
- **Boucle agent ReAct** texte (Thought / Action / Observation / Final Answer)
- **5 outils** : `calculator`, `web_search`, `web_fetch`, `python_exec`, `rag_search`
- **Sécurité JWT** (access 15 min + refresh 7 j avec rotation)
- **Frontend Ionic + Angular** (web + futur mobile Capacitor) avec dark mode, multi-pages

**Stack** : Spring Boot 3.3 · Java 21 · PostgreSQL 16 · FastAPI · sentence-transformers ·
Jsoup · exp4j · Ollama · Ionic 8 · Angular 20 · Capacitor 7.

**Cible matériel** : Windows Server 2019, Xeon 6 cœurs, 64 Go RAM, **sans GPU**.
Tout tourne 100 % local, zéro Docker, zéro dépendance cloud.

---

## 2. Phases livrées — backend

### Phase 1 — MVP (auth / chat / streaming)

| Module | Contenu |
|---|---|
| `common` | `GlobalExceptionHandler`, `ApiError`, `WebClientConfig`, `OpenApiConfig`, `CorsConfig`, `AsyncConfig`, `JacksonConfig`, `PageResponse` |
| `security` | `SecurityConfig` (stateless dev + prod), `JwtService` (HS256), `JwtAuthenticationFilter`, `JwtProperties` (≥ 32 chars en prod), `RestAuthEntryPoint`, `RestAccessDeniedHandler`, `BCryptPasswordEncoder(12)` |
| `auth` | `AuthController` register/login/refresh/logout, hash SHA-256 des refresh tokens, rotation, révocation au logout |
| `user` | CRUD `/users/me` + change-password |
| `chat` | `ChatSession` (mode NORMAL/EXPERT/FACT_CHECK), CRUD + ownership systématique → 404 |
| `message` | Historique paginé + **endpoint SSE** `POST /api/v1/chats/{id}/messages`, idempotence `clientRequestId` |
| `llm` | `LlmClient` interface, `OpenAiCompatibleLlmClient` (Ollama/vLLM via même API), endpoints diagnostic `/llm/health` `/llm/test` |
| `streaming` | `SseStreamService`, enum `StreamEventType` (TOKEN, FINAL, ERROR, + TOOL_START/END/SOURCE pour phases ultérieures) |
| `audit` | `AuditService.log` async, table `audit_logs` |
| `agent` | `AgentService` initial (pass-through LlmService) |

**Migrations Flyway V1–V5**, profils Spring `dev/prod/windows-local`.

### Phase 2 — Documents + RAG

| Module | Contenu |
|---|---|
| `document` | `KnowledgeBase`, `Document` (UPLOADED/PARSING/INDEXED/FAILED), `DocumentChunk` (embedding bytea), CRUD KB, upload multipart, ingestion async |
| `rag` | `VectorStoreService` interface + `PostgresVectorStore` (brute-force cosinus Java), `VectorCodec` float32 LE, `RagService` |
| `worker` | Client Java `PythonWorkerClient` + impl `HttpPythonWorkerClient`, DTOs |
| `science` | `Domain` enum, interface `ScientificRouter` |

**Worker Python** (FastAPI) : `/health`, `/embed` (BGE-M3 lazy-load), `/parse` (pypdf + python-docx), auth interne `X-Internal-Token`.

**Migrations V6, V7**. `ownerId` denormalisé dans chunks. **RAG auto** si la session a une KB attachée.

### Phase 3 — Outils + WebSearch + ReAct

| Outil | Description |
|---|---|
| `calculator` | exp4j (sqrt, sin, cos, log, ^, pi, e…) |
| `web_search` | DuckDuckGo HTML scraping via Jsoup (zéro install) |
| `web_fetch` | Jsoup readable extract, max 6 KB |
| `python_exec` | Subprocess Python `-I -S` avec timeout, max 8 KB output |
| `rag_search` | Wrapper du RagService (Phase 2), filtré dynamiquement si pas de KB |

**Worker Python** : ajout `/run-code`.

**Boucle ReAct** : `AgentService` réécrit, max 5 itérations, format Thought / Action / Action Input / Final Answer (plus tolérant que tool calling natif sur petits modèles), `ReactParser` tolérant, `ReactPromptBuilder` injecte le catalogue avec JSON schémas, stop sequences `["\nObservation:"]`. Événements SSE `tool_start` / `tool_end`.

### Phase 4 — Mémoire + streaming Final Answer + modes + Scientific Router

| Composant | Contenu |
|---|---|
| `memory` | `UserFact` (profil clé/valeur), `MemoryEntry` (EPISODIC/SEMANTIC, embedding bytea, importance), `MemoryService` + impl, `MemoryConsolidator` (`@Async`, extraction LLM JSON), `MemoryBlockBuilder` (injection prompt), REST CRUD `/memory/facts` `/memory/entries` |
| `science` | `KeywordScientificRouter` (@Primary) : regex + scoring de mots-clés par domaine |
| `agent` | Stream LLM chunk-par-chunk, parsing incrémental de `Final Answer:`, sélection modèle selon mode, topK RAG ×2 en EXPERT, filtrage `rag_search` si pas de KB |
| `llm` | Surcharges `streamChat/simpleChat` avec `stop` sequences |
| `message` | Hook `memoryConsolidator.consolidate(...)` après chaque END |

**Migration V8** (user_facts, memory_entries).

---

## 3. Corrections **post-Phase 4** (entre Phase 4 et frontend F6)

### 3.1 Toggle `auto-consolidate` (cause de pollution du contexte LLM)

**Symptôme observé** : `MemoryConsolidator` extrayait à chaque échange des "faits" trop bavards (genre `motivation=streaming` ou `intérêt=One Piece`) qui contaminaient le system prompt et faisaient refuser le LLM ("Je ne peux pas vous aider…").

**Fix** dans `MemoryConsolidator.java` :
```java
@Value("${app.agent.memory-consolidate-enabled:true}")
private boolean autoConsolidateEnabled;
```

Avec dans `application-windows-local.yml` :
```yaml
app.agent.memory-consolidate-enabled: ${AGENT_MEMORY_CONSOLIDATE:false}
```

**Désactivé par défaut en local.** Réactivable via env `AGENT_MEMORY_CONSOLIDATE=true` ou via UI (Phase 5 du frontend si on l'ajoute).

### 3.2 Toggle `medical-disclaimer`

**Pourquoi** : le disclaimer poussait les modèles vers une prudence excessive même hors sujet médical.

**Fix** : flag `app.agent.medical-disclaimer-enabled` (défaut `false` en local).
```yaml
medical-disclaimer-enabled: ${AGENT_MEDICAL_DISCLAIMER:false}
```

### 3.3 `system_normal.md` réécrit

Le prompt système original mentionnait "rigoureux", "prudent" — patterns qui activaient les filtres RLHF. Nouveau prompt explicite : *"Tu ne fais pas de préambule moralisateur. L'utilisateur est adulte."*

### 3.4 Auto-détection ReAct vs réponse directe (streaming)

**Symptôme** : les modèles non-ReAct (dolphin, mistral) ne suivent pas le format. L'`AgentService` les buffer en attendant `Final Answer:` qui n'arrive jamais → texte livré en un bloc à la fin.

**Fix** dans `AgentService.handleDelta` :
```java
private static final int DIRECT_MODE_DETECTION_CHARS = 60;
// Si après 60 chars on n'a vu ni "Thought:" ni "Action:" → bascule en streaming
```

→ dolphin envoie **102 events:token** au lieu d'1 seul gros. Vrai streaming en live.

### 3.5 Profil `windows-local` corrigé pour respecter les env vars LLM

L'ancien yml hardcodait `default-model: llama3.1:8b` et ignorait `$LLM_DEFAULT_MODEL`. Corrigé en `${LLM_DEFAULT_MODEL:llama3.1:8b}` + `expert-model` + `fact-check-model` aussi.

### 3.6 CORS étendu à toutes les origines de dev

`application-windows-local.yml` : `${FRONT_ORIGIN:http://localhost:4200,http://localhost:5173,http://localhost:3000,http://localhost:8100}` (ajout Ionic serve 8100).

### 3.7 Modèle alternatif `dolphin-mistral:7b` installé

```bash
ollama pull dolphin-mistral:7b      # 4.1 GB, uncensored, Mistral-based
```

Comparaison rapide (sur Xeon 6c) :

| Modèle | Latence/réponse | Filtres | Quality |
|---|---|---|---|
| `llama3.1:8b` | 9-15 s | Strict RLHF | Bonne mais moralisatrice |
| `dolphin-mistral:7b` | 7 s | Aucun | Directe, parfois courte |
| `qwen2.5:7b-instruct` | 8 s | Modéré | Bon compromis |

Switchable via `$env:LLM_DEFAULT_MODEL` au démarrage backend.

---

## 4. Frontend — Phases F1 → F6 livrées

### Stack frontend

- **Ionic 8** (composants natifs-look, dark mode intégré)
- **Angular 20** (standalone components, signals, control flow `@if/@for`)
- **Capacitor 7.6** (bridge mobile, déjà installé, build mobile à venir en F7)
- **TypeScript strict**
- **Pas de NgRx** : Angular Signals partout
- **SSE via `fetch()` + `ReadableStream`** (pas `EventSource` — voir `FRONTEND_STATE.md` §6)
- **Theme** : palette indigo + accent teal, dark par défaut, persisté

| Phase | Livrable principal |
|---|---|
| **F1** | Bootstrap Angular standalone + Auth (login/register/refresh/guard/interceptor) + ChatList placeholder |
| **F2** | Refonte UX (palette, shell sidebar split-pane, login/register card), ChatService + MessageService + pages chat liste/détail/création sans streaming |
| **F3** | Streaming SSE réel : `SseStreamService` (fetch + ReadableStream), événements typés (`source`, `tool_start`, `tool_end`, `token`, `final`, `error`), `SourcePillComponent` (modal Ionic), `ToolCallAccordionComponent`, curseur clignotant pendant la génération |
| **F4** | UI Knowledge Bases : `KbListPage` (cards par domaine), `KbCreatePage` (picker 7 domaines), `KbDetailPage` (drag-drop upload avec progress bar Ionic, liste documents avec status pills animés, polling status auto toutes les 2s), KB picker dans ChatCreate |
| **F5** | UI Mémoire : `MemoryPage` avec tabs Profil/Souvenirs, ajout manuel + édition inline des faits, filtre par kind sur les souvenirs, bouton **🗑 Tout effacer** dans header |
| **F6** | Settings : `ThemeService` (dark/light/auto persisté avec init pré-paint), `SettingsService` (URL backend + modèle préféré persistés), `UserService` (édit profil + change password), `SettingsPage` avec 5 sections |

**Détail complet** dans `FRONTEND_STATE.md`.

### Bugs résolus côté front

| Bug | Cause | Fix |
|---|---|---|
| Input chat invisible (Phase F2) | `ion-textarea` ne s'affichait pas dans flex container (shadow DOM) | Remplacé par `<textarea>` HTML natif stylé |
| Composer 30% visible | `.page { height: 100% }` dans `ion-content` débordait | Passé à `<ion-footer>` natif |
| Composer invisible (avec ion-footer) | `:host { display: contents }` empêchait IonRouterOutlet de poser ion-page | Retiré `:host { display: contents }` du chat-detail |
| Bouton send toujours disabled | `draft = ''` était une string, pas un signal → `computed(canSend)` ne se recalculait jamais | Converti `draft` en `signal('')` + `[value]/(input)` au lieu de `[(ngModel)]` |
| `ERR_INCOMPLETE_CHUNKED_ENCODING` | Spring `SseEmitter` ferme la connexion sans marqueur final chunked, Angular `HttpClient` levait | Switch vers `fetch()` + `ReadableStream` (qui tolère cette fermeture) |
| LLM moralisait sur One Piece | Mémoire auto contenait "intérêt=One Piece" + "motivation=streaming" injectés à chaque conversation | Toggle `app.agent.memory-consolidate-enabled=false` + purge UI |
| Pas de streaming live avec dolphin | AgentService buffer en attendant `Final Answer:` qui n'arrive jamais | Auto-détection après 60 chars (cf. §3.4) |

---

## 5. Phase 5 backend — Non livrée, à faire

Périmètre prévu (cf. `PHASE_ROADMAP.md`) :

| Item | Notes pour la suite |
|---|---|
| **`@Audited` AOP** | Remplacer les appels explicites `auditService.log(...)`. Créer `@Audited(action=…)` + `AuditAspect` avec `@Around`. Fix bonus : la race FK connue (LOGIN_FAILED tente d'insérer userId pas encore committé) via `@TransactionalEventListener(phase=AFTER_COMMIT)`. |
| **Resilience4j actif** | Aujourd'hui seulement timeout WebClient. Activer `@Retry`, `@CircuitBreaker`, `@TimeLimiter` sur `OpenAiCompatibleLlmClient` et `HttpPythonWorkerClient`. Config dans `application.yml`. |
| **Rate limit auth** | `/auth/login/register/refresh` n'ont aucune protection brute-force. Bucket4j + filter avant `JwtAuthenticationFilter`. |
| **Métriques custom Micrometer** | `llm.calls.duration`, `tool.calls.duration{tool=…}`, `ingestion.duration`, `rag.search.duration`. Endpoint `/actuator/prometheus` déjà exposé. |
| **Logback JSON en prod** | `logback-spring-json.xml` déjà écrit. MDC déjà peuplé par `JwtAuthenticationFilter`. Ajouter MDC `sessionId`, `traceId`. |
| **Dashboards Grafana** | JSON dashboard à committer dans `infra/grafana/`. |
| **Scan dépendances** | OWASP Dependency-Check Maven plugin : `mvn dependency-check:check`. |
| **Tests de charge SSE** | k6 ou Gatling. Script `infra/loadtest/sse.js`. |
| **CORS strict en prod** | Whitelist explicite par environnement. |
| **Hibernate batching** | `hibernate.jdbc.batch_size: 30` pour ingestion gros docs. |
| **CI pipeline** | GitHub Actions / GitLab CI : `mvn verify` + Testcontainers + linter Python + audit JS frontend. |

---

## 6. Phases F7 / F8 frontend — Non livrées

| Phase | Périmètre | Estimation |
|---|---|---|
| **F7 — Mobile Capacitor (Android)** | `ionic capacitor add android`, icons + splash via `@capacitor/assets`, build APK debug, fix layouts mobile, plugins Preferences/StatusBar/Keyboard/Filesystem | 4 j |
| **F8 — iOS + PWA prod + signing** | `cap add ios` (Mac requis), service worker PWA, manifest, signing release Android, TestFlight iOS | 5 j |

---

## 7. Bugs connus restants

| Bug | Sévérité | Phase |
|---|---|---|
| Race FK sur `audit_logs` lors du REGISTER | Cosmétique (swallowed) | Backend 1 |
| Llama 3.1 8B peut produire `Action: Final Answer` au lieu de `Final Answer:` direct | Cosmétique (1ère iter perdue) | Backend 3 |
| Mode EXPERT peut consommer 5 itérations sans Final Answer | UX | Backend 4 |
| DuckDuckGo peut renvoyer 403 (anti-bot) en cas de requêtes fréquentes | Externe | Backend 3 |
| Port 8080 occupé par `javaw` système → backend sur 8081 | Cosmétique | Backend 1 |
| `AsyncRequestNotUsableException` côté backend après end du stream | Bénin (front gère) | Backend 1 + Frontend F3 |

---

## 8. Décisions architecturales clés (récap)

| Décision | Raison |
|---|---|
| **Spring MVC + SSE + virtual threads Java 21** (pas WebFlux) | Compat JPA bloquant, perf suffisante, simplicité |
| **API OpenAI-compatible unique** | Ollama / vLLM / autres providers OpenAI-compatible interchangeables par config |
| **Modèle 7-8B par défaut** | Xeon 6c / 64 Go RAM sans GPU |
| **PostgresVectorStore brute-force** | Pas de Docker, pas de compilation pgvector ; OK jusqu'à ~50k chunks/KB |
| **Worker Python séparé** | Tâches IA lourdes hors JVM |
| **ReAct texte (pas function calling natif)** + auto-détection direct | Plus tolérant sur les modèles non-fine-tunés tool-use |
| **`ownerId` denormalisé partout** | Filtres O(1) ; aucune query "globale" possible hors `/admin/**` |
| **Idempotence `clientRequestId`** | Rejouer un message ne redéclenche pas le LLM |
| **Auto-consolidation mémoire désactivée par défaut** | Évite la pollution du contexte par des "faits" hasardeux |
| **Frontend Ionic + Angular standalone** | Single codebase web + mobile via Capacitor |
| **`fetch()` + `ReadableStream`** côté front (pas `EventSource`) | EventSource ne supporte pas le header Authorization, fragile en WebView mobile |
| **Angular Signals** (pas NgRx) | Single-user, état raisonnable, perf + simplicité |
| **Dark mode par défaut** | UI plus moderne LLM-like |
| **Package racine `mr.popo.localaiagent`** | Préférence utilisateur |
| **SERVER_PORT = 8081** | Le 8080 est occupé sur Windows Server 2019 |

---

## 9. Inventaire — fichiers et packages

### Structure backend Java
```
backend/src/main/java/mr/popo/localaiagent/
├── LocalAiAgentApplication.java
├── common/        exceptions, configs, pagination
├── security/      JWT, filter, handlers
├── auth/          register/login/refresh/logout
├── user/          /users/me
├── chat/          sessions CRUD
├── message/       historique + endpoint SSE
├── llm/           client OpenAI-compatible + diagnostic
├── streaming/     helpers SSE + StreamEventType
├── agent/         AgentService + ReactParser + PromptBuilder + ReactPromptBuilder + AgentEvent + auto-détection ReAct
├── audit/         AuditService async + AuditLog
├── document/      KB + Document + Chunk + ingestion async + DocumentStatusUpdater
├── rag/           VectorStoreService + PostgresVectorStore + VectorCodec + RagService
├── memory/        UserFact + MemoryEntry + MemoryServiceImpl + MemoryConsolidator + MemoryBlockBuilder
├── tools/         Tool interface + ToolRegistry + ToolDispatcher + 5 impls
├── science/       Domain + ScientificRouter + KeywordScientificRouter
├── websearch/     WebSearchService + DuckDuckGoWebSearchService
└── worker/        PythonWorkerClient + HttpPythonWorkerClient + DTOs
```

### Structure worker Python
```
worker/
├── pyproject.toml, requirements.txt
├── README.md
├── .venv/                    (créé localement, gitignored)
└── app/
    ├── __init__.py
    ├── settings.py           env vars
    ├── main.py               FastAPI : /health, /embed, /parse, /run-code
    ├── embeddings.py         BGE-M3 lazy-load
    ├── parsing.py            pypdf + python-docx
    └── sandbox.py            subprocess.run isolé + timeout
```

### Structure frontend (cf. `FRONTEND_STATE.md` pour le détail)
```
frontend/
├── ionic.config.json, capacitor.config.ts, package.json
├── src/
│   ├── main.ts                (applyBootEnvironment + theme pré-paint + bootstrapApplication)
│   ├── environments/          environment.ts + environment.prod.ts (apiUrl)
│   ├── theme/variables.scss   palette indigo/teal + dark mode
│   ├── global.scss
│   └── app/
│       ├── app.config.ts      provideRouter + provideHttpClient(authInterceptor) + provideIonicAngular
│       ├── app.routes.ts      auth (login/register) + shell (chats/kbs/memory/settings)
│       ├── app.component.ts   <ion-app><ion-router-outlet/></ion-app>
│       ├── core/
│       │   ├── api/           sse-stream.service + sse-event.types
│       │   ├── auth/          auth.service + token-storage + interceptor + guard + user.service + types
│       │   └── ui/            toast + theme + settings
│       ├── shared/
│       │   ├── components/    app-shell + source-pill + tool-call-accordion
│       │   └── pipes/         relative-date
│       └── features/
│           ├── auth/          login.page + register.page
│           ├── chat/          chat-list + chat-detail + chat-create + chat.service + message.service + types
│           ├── kb/            kb-list + kb-detail + kb-create + kb.service + document.service + types
│           ├── memory/        memory.page + memory.service + types
│           └── settings/      settings.page
```

### Migrations Flyway
- V1 : users, roles, user_roles
- V2 : refresh_tokens
- V3 : chat_sessions, messages (jsonb tool_calls/sources prêts)
- V4 : audit_logs
- V5 : seed roles (USER, ADMIN, EXPERT)
- V6 : knowledge_bases, documents, document_chunks
- V7 : chat_sessions.knowledge_base_id
- V8 : user_facts, memory_entries

### Profils Spring
- `dev` : DB locale, CORS large, logs DEBUG, `/llm/**` ouvert authenticated
- `prod` : DB env vars, CORS strict, logs JSON, `/llm/**` ADMIN/EXPERT, `JWT_SECRET` ≥ 32 chars
- `windows-local` : combinaison dev sur localhost + flags mémoire/disclaimer désactivés + CORS étendu 8100

### Variables d'environnement principales

| Variable | Défaut | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | dev | dev / prod / windows-local |
| `SERVER_PORT` | 8080 | **8081** sur cette machine |
| `DB_HOST/PORT/NAME/USER/PASS` | localhost / 5432 / localaiagent / localaiagent / change_me | PG |
| `JWT_SECRET` | (placeholder) | ≥ 32 chars en prod |
| `LLM_PROVIDER` | ollama | ollama \| vllm |
| `LLM_BASE_URL` | http://localhost:11434/v1 | API OpenAI-compatible |
| `LLM_NATIVE_BASE_URL` | http://localhost:11434 | /api/tags health |
| `LLM_DEFAULT_MODEL` | llama3.1:8b | actuellement `dolphin-mistral:7b` |
| `LLM_EXPERT_MODEL` / `LLM_FACT_MODEL` | llama3.1:8b | utilisés en mode EXPERT/FACT_CHECK |
| `WORKER_BASE_URL` | http://localhost:9000 | Worker Python |
| `WORKER_TOKEN` | (vide) | Auth interne backend ↔ worker |
| `UPLOAD_DIR` | ./data/uploads | Stockage fichiers ingérés |
| `AGENT_MEMORY_CONSOLIDATE` | `false` (windows-local) | active l'auto-extraction de faits |
| `AGENT_MEDICAL_DISCLAIMER` | `false` (windows-local) | disclaimer médical dans le prompt |
| `FRONT_ORIGIN` | liste CSV | origines CORS autorisées (8100/4200/5173/3000) |

### Endpoints API (récap)
Documentation Swagger : http://localhost:8081/swagger-ui.html

**Auth** : `/api/v1/auth/{register,login,refresh,logout}`
**Users** : `GET /api/v1/users/me`, `PATCH /api/v1/users/me`, `POST /api/v1/users/me/password`
**Chats** : `GET/POST/PATCH/DELETE /api/v1/chats[/{id}]` ; `GET /chats/{id}/messages` ; **SSE** `POST /chats/{id}/messages`
**Knowledge Bases** : `GET/POST/DELETE /api/v1/kbs[/{id}]`
**Documents** : multipart `POST /kbs/{kbId}/documents`, `GET /kbs/{kbId}/documents`, `GET/DELETE /documents/{id}`
**Memory** : `GET/POST/DELETE /api/v1/memory/facts[/{id}]`, `GET/DELETE /api/v1/memory/entries[/{id}]`
**LLM diag** : `GET /api/v1/llm/health`, `POST /api/v1/llm/test` (ADMIN/EXPERT en non-dev)
**Actuator** : `GET /actuator/health`, `GET /actuator/prometheus` (ADMIN)

### Événements SSE typés
```
event: source       data: {documentId, documentName, page, score, snippet, index}
event: tool_start   data: {iteration, name, arguments}
event: tool_end     data: {iteration, name, success, summary}
event: token        data: {text}
event: final        data: {messageId, userMessageId, replayed?}
event: error        data: {message}
```

---

## 10. Référence rapide — comment évoluer le code

| Je veux… | Où regarder |
|---|---|
| Ajouter un outil à l'agent | Créer `mr.popo.localaiagent.tools.impl.MonOutil implements Tool` → auto-découvert par `ToolRegistry` |
| Changer le format ReAct | `ReactPromptBuilder` (instructions) + `ReactParser` (parsing) + `AgentService.handleDelta` (détection direct) |
| Ajouter un mode | `ChatMode` enum + `PromptBuilder.appendModeNote` + `AgentService.resolveModelForMode` |
| Changer la stratégie de chunking | `TextChunker` (Phase 2). Phase 3+ : passer à un splitter sémantique côté worker. |
| Migrer le storage vectoriel | Implémenter `VectorStoreService` (interface) avec impl Qdrant ou pgvector. `PostgresVectorStore` reste comme fallback. |
| Ajouter un endpoint REST | Créer `Controller` + `Service` + `Mapper` + DTOs. Update `SecurityConfig` si auth différente. |
| Ajouter une migration | `db/migration/V9__xxx.sql`. Ne jamais modifier une migration appliquée. |
| Changer le LLM | Variables env `LLM_*`. Pour un autre provider OpenAI-compatible, juste l'URL. Sinon nouveau bean `LlmClient`. |
| Activer la mémoire auto | `$env:AGENT_MEMORY_CONSOLIDATE = "true"` et redémarrer |
| Réactiver disclaimer médical | `$env:AGENT_MEDICAL_DISCLAIMER = "true"` |
| Ajouter une page frontend | Créer dossier `features/xxx/`, page standalone, ajouter route dans `app.routes.ts`, ajouter dans `navLinks` de `app-shell.component.ts` |
| Ajouter une couleur de mode | `chat.types.ts` + style `.mode-XXX` dans `chat-list.page.ts` et `chat-detail.page.ts` |
| Modifier les flags consolidation/disclaimer via UI | Créer endpoints backend `GET/PUT /api/v1/settings/agent` + UI Settings F6 |
| Changer la palette de thème | `frontend/src/theme/variables.scss` (`--laa-brand`, `--laa-accent`, etc.) |

---

## 11. Logs et débugging

| Symptôme | Action |
|---|---|
| Backend ne démarre pas | `mvn spring-boot:run` en foreground → lire la stacktrace |
| `JWT secret too short` | `JWT_SECRET` ≥ 32 chars en prod (variable d'env) |
| `Flyway validation failed` | Une migration a changé : ne jamais modifier les V appliquées, créer une nouvelle Vn+1 |
| LLM "Désolé, je n'ai pas pu finaliser" | Max iterations atteint. Augmenter `app.agent.max-iterations` ou voir le log DEBUG `ReAct iter N llm raw:` |
| Doc reste UPLOADED indéfiniment | Vérifier worker UP (`/health`), regarder les logs `DocumentIngestionService` |
| SSE coupe avec `curl: (18)` | Artefact bénin (SIGPIPE quand le pipe se ferme), pas un bug |
| Embeddings très lents | Premier appel charge BGE-M3 (~2 Go RAM, ~45 s). Suivants rapides. |
| Port 8080 occupé | `SERVER_PORT=8081` (déjà le cas) |
| LLM répond "Je ne peux pas vous aider…" | Mémoire polluée → `GET /api/v1/memory/facts` puis purge UI ou SQL |
| Pas de streaming live | Vérifier auto-détection `AgentService.DIRECT_MODE_DETECTION_CHARS` (60). Modèle non-ReAct = doit basculer après 60 chars. |
| `ERR_INCOMPLETE_CHUNKED_ENCODING` en front | Vérifier `MessageService.sendStream` utilise bien `SseStreamService` (fetch) et pas `HttpClient.post text` |
| Frontend ne se recharge pas | `Ctrl+Shift+R` pour bypass cache navigateur |

Le profil `dev` ouvre `Logger level=DEBUG` sur `mr.popo.localaiagent.*` → tu vois tous les iterations ReAct, RAG hits, etc.

---

## 12. Liens internes

- [`docs/FRONTEND_STATE.md`](FRONTEND_STATE.md) — **doc frontend détaillée**
- [`docs/INSTALL_AND_RUN.md`](INSTALL_AND_RUN.md) — installation sur une nouvelle machine + démarrage
- [`docs/ARCHITECTURE.md`](ARCHITECTURE.md) — vue d'ensemble backend (Phase 1, à actualiser)
- [`docs/DESIGN_RULES.md`](DESIGN_RULES.md) — règles transversales (ownership, sync, disclaimer médical, idempotence…)
- [`docs/PHASE_ROADMAP.md`](PHASE_ROADMAP.md) — roadmap d'origine
- [`docs/SMOKE_TEST.md`](SMOKE_TEST.md) — scénario de validation Phase 1
- [`docs/WINDOWS_LOCAL.md`](WINDOWS_LOCAL.md) — mode Windows sans Docker
- [`docs/FRONTEND_PLAN.md`](FRONTEND_PLAN.md) — plan d'origine frontend

---

*Dernière mise à jour : fin Phase F6 frontend. Phase 5 backend non livrée. Phases F7/F8 mobile non livrées.*
