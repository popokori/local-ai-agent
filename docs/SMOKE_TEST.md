# Smoke Test — Phase 1

Scénario manuel à passer à la fin de chaque release Phase 1.
Pré-requis : backend démarré (Docker ou Windows local) + modèle Ollama pull.

## 1. Healthchecks de base

```bash
curl -s localhost:8080/actuator/health | jq
# attendu : {"status":"UP"}
```

## 2. Register + login

```bash
curl -X POST localhost:8080/api/v1/auth/register \
  -H 'content-type: application/json' \
  -d '{"username":"alice","email":"a@a.com","password":"Password1!","displayName":"Alice"}'

TOK=$(curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'content-type: application/json' \
  -d '{"username":"alice","password":"Password1!"}' | jq -r .accessToken)

echo "Access token: ${TOK:0:30}..."

# Profil
curl -s localhost:8080/api/v1/users/me -H "Authorization: Bearer $TOK" | jq
```

## 3. Diagnostic LLM (dev profile — authentifié suffit)

```bash
curl -s -H "Authorization: Bearer $TOK" localhost:8080/api/v1/llm/health | jq
# attendu : {"provider":"ollama","reachable":true,"modelLoaded":true,...}

curl -s -X POST localhost:8080/api/v1/llm/test \
  -H "Authorization: Bearer $TOK" -H 'content-type: application/json' \
  -d '{"prompt":"ping"}' | jq
# attendu : {"model":"llama3.1:8b","reply":"...","tokensOut":...,"latencyMs":...}
```

## 4. Création d'une session

```bash
SID=$(curl -s -X POST localhost:8080/api/v1/chats \
  -H "Authorization: Bearer $TOK" -H 'content-type: application/json' \
  -d '{"title":"hello"}' | jq -r .id)
echo "Session: $SID"
```

## 5. Envoi d'un message — flux SSE

```bash
curl -N -X POST localhost:8080/api/v1/chats/$SID/messages \
  -H "Authorization: Bearer $TOK" -H 'content-type: application/json' \
  -d '{"content":"Bonjour, présente-toi en deux phrases.","clientRequestId":"11111111-1111-1111-1111-111111111111"}'
```

Attendu : flux SSE défilant token par token :
```
event: token
data: {"text":"Bon"}

event: token
data: {"text":"jour"}

...

event: final
data: {"messageId":42,"userMessageId":41}
```

## 6. Historique

```bash
curl -s localhost:8080/api/v1/chats/$SID/messages -H "Authorization: Bearer $TOK" | jq
# attendu : 2 messages (USER puis ASSISTANT)
```

## 7. Idempotence

Re-rejouer la même requête (même `clientRequestId`) :
```bash
curl -N -X POST localhost:8080/api/v1/chats/$SID/messages \
  -H "Authorization: Bearer $TOK" -H 'content-type: application/json' \
  -d '{"content":"Bonjour, présente-toi en deux phrases.","clientRequestId":"11111111-1111-1111-1111-111111111111"}'
```
Attendu : un seul `event: token` avec la réponse complète + `event: final` avec `"replayed":true`. Pas de doublon en DB.

## 8. Vérifications sécurité

```bash
# Sans Bearer
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/api/v1/users/me
# attendu : 401

# Avec un autre user, accès à la session d'Alice
curl -X POST localhost:8080/api/v1/auth/register \
  -H 'content-type: application/json' \
  -d '{"username":"bob","email":"b@b.com","password":"Password1!"}'
BOB_TOK=$(curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'content-type: application/json' \
  -d '{"username":"bob","password":"Password1!"}' | jq -r .accessToken)
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "Authorization: Bearer $BOB_TOK" localhost:8080/api/v1/chats/$SID
# attendu : 404 (pas 403 — on ne leak pas l'existence)
```

## 9. Logout puis refresh

```bash
# Logout
curl -X POST -H "Authorization: Bearer $TOK" localhost:8080/api/v1/auth/logout
# attendu : 204

# Le refresh token (récupéré au login plus haut) ne doit plus marcher
# (tous les refresh tokens de l'user ont été révoqués).
```

## Critères de réussite Phase 1

- [ ] `actuator/health` → UP
- [ ] register/login → 200 + tokens
- [ ] `/llm/health` → `reachable=true, modelLoaded=true`
- [ ] `/llm/test` répond en < 30s sur modèle 8B
- [ ] SSE : flux de tokens visible, suivi de `final`
- [ ] Historique persisté (user + assistant)
- [ ] Idempotence : `clientRequestId` doublon → pas de regénération
- [ ] Sans token → 401
- [ ] Token d'un autre user → 404 (pas de leak)
- [ ] Logout → refresh suivant échoue
