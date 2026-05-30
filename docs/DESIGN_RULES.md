# Design Rules — Règles transversales

Ces règles s'appliquent à **tous les modules présents et à venir**.
À relire avant chaque sprint pour ne pas accumuler de dette technique.

## 1. Ownership systématique

Toute ressource utilisateur (`ChatSession`, `Message`, futur `Document`, `UserFact`, etc.)
porte un champ `ownerId` (FK `users.id`).

- Repositories exposent `findByIdAndOwnerId(...)`.
- Services lèvent `ResourceNotFoundException` (→ 404) si non-owner — **on ne leak pas l'existence**.
- Aucune query "globale" hors `/api/v1/admin/**`.

## 2. Permissions Qdrant (Phase 2)

Chaque point Qdrant aura payload `{ownerId, kbId, documentId, chunkId}`.
Toute `search()` portera un filtre `must: [{key:"ownerId", match:{value: currentUserId}}]`.

Le futur `QdrantClientImpl` imposera ce filtre **par construction** — pas un paramètre
optionnel du caller. Un appel sans `ownerId` doit être impossible à compiler.

## 3. Synchronisation PostgreSQL ↔ Qdrant (Phase 2)

PostgreSQL = **source de vérité des métadonnées** (`Document`, `DocumentChunk`).
Qdrant ne stocke que les **vecteurs** + payload minimal.

Ordre strict des opérations :

| Action | Ordre | Politique en cas d'échec |
|---|---|---|
| Ingestion | INSERT chunks → `qdrant.upsert(points)` | échec Qdrant → reprocess job, ne pas valider l'état `INDEXED` |
| Suppression | `qdrant.deleteByDocumentId(...)` → DELETE chunks → DELETE document | échec Qdrant → ne pas supprimer en DB (cohérence forte côté DB) |

Job `@Scheduled` (Phase 2) audite la cohérence : documents indexés en DB mais
absents Qdrant et inversement → métrique Prometheus + alerte.

## 4. Disclaimer médical (déjà actif en Phase 1)

`PromptBuilder` injecte dans tout system prompt, quel que soit le mode :

> Pour toute question d'ordre médical (symptômes, médicaments, posologie, diagnostic,
> traitement, interactions), tu fournis une information générale, prudente et
> pédagogique. Tu ne poses jamais de diagnostic et tu ne prescris jamais. Tu rappelles
> systématiquement à l'utilisateur de consulter un professionnel de santé qualifié
> pour toute décision le concernant.

Stocké dans `resources/prompts/system_medical_disclaimer.md`.
Désactivable uniquement via `app.agent.medical-disclaimer-enabled=false` (à n'utiliser que pour des tests internes).

## 5. DTO en frontière

- Aucune entité JPA exposée par un controller.
- Aucune entité désérialisée depuis une requête.
- MapStruct partout.
- `@JsonIgnore` sur les champs sensibles (`passwordHash`, `tokenHash`) en defense-in-depth.

## 6. Pas d'OpenSessionInView

`spring.jpa.open-in-view: false`.

Toute traversée lazy se fait **dans la couche service** sous transaction explicite.

## 7. Erreurs uniformes

`GlobalExceptionHandler` mappe :

| Exception | Statut | Note |
|---|---|---|
| `ResourceNotFoundException` | 404 | message exposé |
| `BusinessException` | 400 | message exposé |
| `ForbiddenException` | 403 | message exposé |
| `BadCredentialsException` / `AuthenticationException` | 401 | message générique |
| `AccessDeniedException` | 403 | message générique |
| `MethodArgumentNotValidException` | 400 | + `validationErrors[]` |
| Fallback `Exception` | 500 | message générique, pas de stack-trace |

## 8. Idempotence des messages

`SendMessageRequest.clientRequestId` (UUID).
`MessageService` rejette les doublons : renvoie le message existant sans rappeler le LLM.

Évite la re-génération en cas de retry SSE après déconnexion réseau.

## 9. Logging structuré

SLF4J + Logback. JSON encoder (logstash-logback-encoder) en profil `prod`.

Champs MDC systématiques : `traceId`, `userId`, `sessionId`, `action`.
Peuplés dans `JwtAuthenticationFilter` (et par les contrôleurs pour `sessionId`).

## 10. Sécurité des secrets

`JWT_SECRET`, `DB_PASS` lus via env. `.env` git-ignoré, `.env.example` versionné.

Validation au démarrage (`@PostConstruct` dans `JwtProperties`) : refuse de démarrer
en profil `prod` si `JWT_SECRET` < 32 chars.

## 11. Modes (NORMAL / EXPERT / FACT_CHECK)

Enum déjà présente sur `ChatSession`. Phase 1 ne câble que NORMAL.
Phase 4 ajoutera :
- EXPERT : `expert-model`, prompt instructif, RAG topK augmenté.
- FACT_CHECK : pass `fact_check` après réponse, annotation `[vérifié]` / `[incertain]`.

L'API REST accepte déjà les 3 valeurs dès Phase 1 — pas de breaking change à craindre.

## 12. Résilience progressive

Phase 1 : timeout `WebClient` 120 s + connect 10 s + logs.
Phase 2 fin : activation `@Retry(maxAttempts=2)` + `@CircuitBreaker` + `@TimeLimiter`
sur `LlmClient.streamChat` après stabilité du flux SSE.

Pas d'activation prématurée pour éviter les bugs de configuration qui ralentiraient le MVP.
