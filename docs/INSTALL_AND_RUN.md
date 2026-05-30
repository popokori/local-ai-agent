# Installation sur une nouvelle machine + procédure de démarrage

Procédure complète pour cloner le projet sur une **machine Windows neuve**
(testée sur Windows Server 2019, fonctionne aussi sur Windows 10/11). Zéro
Docker requis. Tout local.

> Pré-requis matériel minimum : 16 Go RAM, 4 cœurs CPU, 30 Go disque libre.
> Recommandé : 64 Go RAM, 6+ cœurs (les embeddings BGE-M3 prennent ~2 Go RAM
> et le LLM 8B ~5 Go).

---

## A. Installation initiale (1 seule fois)

### 1. Récupérer le projet

```powershell
cd C:\Users\Administrateur\Pictures
git clone <URL_DU_REPO> llmproject
# OU si pas de Git : copier le dossier llmproject\ depuis une archive
cd llmproject
```

### 2. Installer Chocolatey (gestionnaire de paquets Windows)

Si pas déjà installé. **Lancer PowerShell en administrateur** :

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = `
    [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString(
    'https://community.chocolatey.org/install.ps1'))
```

Vérifier : `choco -v` doit renvoyer une version.

### 3. Installer JDK 21 + Maven + Python 3.11

```powershell
choco install temurin21 maven python311 -y --no-progress
```

Attendre la fin. Fermer puis rouvrir PowerShell pour que le PATH se rafraîchisse.

Vérifier :
```powershell
java -version    # → openjdk version "21..."
mvn -version     # → Apache Maven 3.9.x
py -3.11 --version  # → Python 3.11.x
```

Si `java` ne trouve pas la version 21, définir manuellement :
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```
(Adapter le chemin selon la version réellement installée — vérifier dans `C:\Program Files\Eclipse Adoptium\`.)

### 4. Installer PostgreSQL 16

```powershell
choco install postgresql16 --params "/Password:postgres_admin_pass /Port:5432" -y --no-progress
```

**Note** : `postgres_admin_pass` est le mot de passe du superuser `postgres`.
Le changer pour quelque chose de propre en prod (jamais commit ce mot de passe).

Vérifier que le service tourne :
```powershell
Get-Service postgresql-x64-16
# → Status = Running
```

### 5. Créer la DB applicative

```powershell
$env:PGPASSWORD = "postgres_admin_pass"
& 'C:\Program Files\PostgreSQL\16\bin\psql.exe' -h localhost -U postgres -d postgres -c `
    "CREATE USER localaiagent WITH PASSWORD 'localaiagent'; CREATE DATABASE localaiagent OWNER localaiagent; GRANT ALL PRIVILEGES ON DATABASE localaiagent TO localaiagent;"
```

Tester la connexion utilisateur :
```powershell
$env:PGPASSWORD = "localaiagent"
& 'C:\Program Files\PostgreSQL\16\bin\psql.exe' -h localhost -U localaiagent -d localaiagent -c "SELECT current_user, current_database();"
```

### 6. Installer Ollama + pull du modèle

```powershell
choco install ollama -y --no-progress
```

Ollama démarre automatiquement comme service Windows et écoute sur `127.0.0.1:11434`.

Pré-télécharger le modèle par défaut (~4.9 Go) :
```powershell
ollama pull llama3.1:8b
ollama list   # → llama3.1:8b doit apparaître
```

Tester :
```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:11434/api/tags | Select-Object -ExpandProperty Content
```

### 7. Préparer le worker Python (venv + dépendances)

```powershell
cd C:\Users\Administrateur\Pictures\llmproject\worker
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
deactivate
```

Cette étape télécharge **~600 Mo** (torch, sentence-transformers, etc.) et
prend 5-10 minutes selon la connexion.

> Le modèle BGE-M3 (~2.3 Go) sera téléchargé **au premier appel à `/embed`**,
> pas tout de suite. Première requête d'embedding = ~45 s.

### 7.5. Installer Node.js + Ionic CLI (pour le frontend)

```powershell
# Node.js 20 LTS + npm
choco install nodejs-lts -y --no-progress
# Rouvre PowerShell pour rafraîchir le PATH

# Vérifier
node -v   # → v20.x.x
npm -v    # → 10.x.x ou 11.x.x

# Installer Ionic CLI globalement
npm install -g @ionic/cli@latest
ionic --version  # → 7.x.x
```

### 7.6. Installer les dépendances du frontend

```powershell
cd C:\Users\Administrateur\Pictures\llmproject\frontend
npm install
```

Télécharge ~1 200 packages (Angular, Ionic, Capacitor, RxJS, etc.). Une fois.

### 8. Configurer les variables d'environnement

Créer le fichier `C:\Users\Administrateur\Pictures\llmproject\.env.ps1` (git-ignoré) :

```powershell
# Profil & port
$env:SPRING_PROFILES_ACTIVE = "windows-local"
$env:SERVER_PORT            = "8081"   # 8080 souvent occupé sur Windows Server

# Base de données
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "localaiagent"
$env:DB_USER = "localaiagent"
$env:DB_PASS = "localaiagent"

# Sécurité JWT (≥ 32 caractères en prod !)
$env:JWT_SECRET = "change-me-to-a-real-32+chars-secret-please"

# Frontend autorisé (CORS)
$env:FRONT_ORIGIN = "http://localhost:4200"

# LLM (Ollama Windows natif)
$env:LLM_PROVIDER         = "ollama"
$env:LLM_BASE_URL         = "http://localhost:11434/v1"
$env:LLM_NATIVE_BASE_URL  = "http://localhost:11434"
$env:LLM_DEFAULT_MODEL    = "llama3.1:8b"
$env:LLM_EXPERT_MODEL     = "llama3.1:8b"
$env:LLM_FACT_MODEL       = "llama3.1:8b"

# Worker Python
$env:WORKER_BASE_URL = "http://localhost:9000"
$env:WORKER_TOKEN    = "change-me-shared-secret-between-backend-and-worker"

# Stockage uploads
$env:UPLOAD_DIR = "C:\Users\Administrateur\Pictures\llmproject\backend\data\uploads"

# JDK 21 (si pas dans le PATH)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
$env:PATH      = "$env:JAVA_HOME\bin;$env:PATH"
```

Tu chargeras ce fichier au début de chaque session shell (cf. section B).

### 9. Compiler le backend

```powershell
cd C:\Users\Administrateur\Pictures\llmproject\backend
mvn -B package -DskipTests
```

Doit afficher `BUILD SUCCESS` et produire `target\localaiagent-backend.jar` (~70 Mo, fat jar exécutable).

---

## B. Démarrage quotidien

### B.1. Préparer le terminal

Ouvrir **deux fenêtres PowerShell**. Dans chaque :

```powershell
cd C:\Users\Administrateur\Pictures\llmproject
. .\.env.ps1
```

### B.2. Démarrer le worker Python (fenêtre 1)

```powershell
cd C:\Users\Administrateur\Pictures\llmproject\worker
.\.venv\Scripts\Activate.ps1
python -m app.main
```

Attendre la ligne :
```
INFO:     Uvicorn running on http://127.0.0.1:9000
```

Vérifier dans une 3e fenêtre :
```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:9000/health | Select-Object -ExpandProperty Content
# → {"status":"UP","embeddingModel":"BAAI/bge-m3",...}
```

### B.3. Démarrer le backend Spring Boot (fenêtre 2)

```powershell
cd C:\Users\Administrateur\Pictures\llmproject\backend
java -jar target\localaiagent-backend.jar
```

Attendre la ligne :
```
Started LocalAiAgentApplication in X.XXX seconds
```

Vérifier :
```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8081/actuator/health | Select-Object -ExpandProperty Content
# → {"status":"UP","groups":["liveness","readiness"]}
```

### B.4. Démarrer le frontend Ionic (fenêtre 3)

```powershell
cd C:\Users\Administrateur\Pictures\llmproject\frontend
ionic serve --no-open --port 8100
```

Attendre la ligne `Compiled successfully`. L'app est servie sur :
**http://localhost:8100**

### B.5. Accès aux interfaces

| URL | Description |
|---|---|
| http://localhost:8100 | **App principale (UI Ionic)** |
| http://localhost:8081/swagger-ui.html | Doc interactive REST |
| http://localhost:8081/actuator/health | Health backend |
| http://localhost:9000/health | Health worker Python |
| http://localhost:9000/docs | FastAPI auto-doc worker |

Ouvre l'app sur http://localhost:8100, login `alice` / `Password1!` (ou crée un compte).

---

## C. Vérification end-to-end (smoke test)

À copier-coller dans un terminal **bash** (Git Bash sur Windows, ou WSL) :

```bash
BASE=http://localhost:8081

# 1) Register
curl -X POST $BASE/api/v1/auth/register \
  -H 'content-type: application/json; charset=utf-8' \
  -d '{"username":"alice","email":"a@a.com","password":"Password1!","displayName":"Alice"}'

# 2) Login
TOK=$(curl -s -X POST $BASE/api/v1/auth/login \
  -H 'content-type: application/json' \
  -d '{"username":"alice","password":"Password1!"}' \
  | python -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# 3) Création session
SID=$(curl -s -X POST $BASE/api/v1/chats \
  -H "Authorization: Bearer $TOK" -H 'content-type: application/json; charset=utf-8' \
  -d '{"title":"hello"}' | python -c "import sys,json; print(json.load(sys.stdin)['id'])")

# 4) Envoi de message — SSE qui défile
curl -N -X POST $BASE/api/v1/chats/$SID/messages \
  -H "Authorization: Bearer $TOK" -H 'content-type: application/json; charset=utf-8' \
  -d '{"content":"Hello! Briefly introduce yourself."}'
```

Tu dois voir un flux `event: token data: {"text":"..."}` défiler, suivi de `event: final`.

---

## D. Procédure d'arrêt

### Arrêt propre

Dans chaque fenêtre PowerShell (worker, backend) : `Ctrl+C`. Le backend
respecte un shutdown gracieux (termine les SSE en cours).

### Arrêt forcé (si bloqué)

```powershell
# Tuer ce qui écoute sur le port 8081 (backend)
Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue |
    ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }

# Tuer ce qui écoute sur le port 9000 (worker)
Get-NetTCPConnection -LocalPort 9000 -State Listen -ErrorAction SilentlyContinue |
    ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

PostgreSQL et Ollama sont des services Windows → laissés tournants, redémarrent
au boot. Pour les arrêter :
```powershell
Stop-Service postgresql-x64-16
Stop-Service Ollama        # nom exact à vérifier : Get-Service *ollama*
```

---

## E. Mise à jour du code

### E.1. Reprendre les sources

```powershell
cd C:\Users\Administrateur\Pictures\llmproject
git pull
```

### E.2. Recompiler le backend

```powershell
cd backend
mvn -B package -DskipTests
```

### E.3. (Optionnel) Mettre à jour le worker

Si `worker\requirements.txt` a changé :
```powershell
cd C:\Users\Administrateur\Pictures\llmproject\worker
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt --upgrade
deactivate
```

### E.4. (Optionnel) Pull d'un nouveau modèle Ollama

```powershell
ollama pull qwen2.5:7b-instruct      # alternative plus légère
ollama pull qwen2.5:14b-instruct     # plus puissant mais lent CPU
ollama pull mistral:7b-instruct
```

Puis changer `$env:LLM_DEFAULT_MODEL` dans `.env.ps1` et redémarrer le backend.

### E.5. Redémarrer worker + backend

Voir section B.

---

## F. Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| `java: command not found` | PATH non rafraîchi après install JDK | Fermer/rouvrir PowerShell ou sourcer `.env.ps1` |
| `JWT secret too short` au démarrage | `JWT_SECRET` < 32 chars en `prod` | Allonger dans `.env.ps1`. Profil `windows-local` est plus permissif. |
| Backend bloqué sur `Flyway validation failed` | Une migration a été modifiée a posteriori | Ne JAMAIS modifier une migration appliquée. Sinon : DROP/CREATE la DB. |
| `Connection refused` sur 5432 | PG arrêté | `Get-Service postgresql*` puis `Start-Service` |
| `Connection refused` sur 11434 | Ollama arrêté | Vérifier l'icône taskbar Ollama, ou `Start-Service Ollama`, ou `ollama serve` manuellement |
| `Connection refused` sur 9000 | Worker pas démarré | Voir B.2 |
| Worker plante avec `ImportError torch` | venv pas activé ou requirements absents | Re-faire l'étape 7 |
| Premier embed très long (45 s+) | Téléchargement BGE-M3 ~2.3 Go | C'est normal au premier appel uniquement |
| Réponse LLM lente (60 s+) | Llama 3.1 8B sur CPU 6 cœurs | Acceptable. Pour accélérer : modèle 7B (qwen2.5:7b), ou GPU. |
| Port 8080 occupé par un `javaw` | Service système préexistant | Utiliser `SERVER_PORT=8081` (déjà fait) |
| `/llm/health` renvoie 403 | Profil `prod` exige rôle ADMIN/EXPERT | Soit profil `dev`/`windows-local` (ouvert authenticated), soit promouvoir l'user ADMIN : voir ci-dessous |
| Document reste UPLOADED indéfiniment | Worker down ou parse failed | Voir backend logs `DocumentIngestionService` |
| DuckDuckGo renvoie 0 résultats | Anti-bot temporaire | Réessayer plus tard, ou switcher sur Brave Search API |
| Ingestion d'un gros PDF tombe en TIMEOUT | `app.worker.timeout-seconds` (défaut 300) trop court | Augmenter dans `application.yml` |
| LLM répond "Je ne peux pas vous aider…" / moralise | Mémoire auto a stocké des "faits" qui contaminent le prompt | Page **Mémoire** dans l'UI → 🗑 "Tout effacer". Ou désactiver `AGENT_MEMORY_CONSOLIDATE=false` (déjà fait par défaut). |
| Pas de streaming live (réponse arrive d'un coup) | Auto-détection ReAct vs direct dans `AgentService.handleDelta` ne se déclenche pas | Vérifier que la réponse LLM ne contient ni "Thought:" ni "Action:" dans les 60 premiers chars. Sinon ajuster `DIRECT_MODE_DETECTION_CHARS`. |
| `ERR_INCOMPLETE_CHUNKED_ENCODING` côté front | Réponse SSE arrive jusqu'au bout mais Spring `SseEmitter` ferme sans marqueur final chunked | C'est géré côté front (`handleError` résilient). Ignorer le warning dans la console. |
| CORS 403 depuis le frontend | Port frontend pas dans `FRONT_ORIGIN` | Ajouter dans `application-windows-local.yml` la liste des origines (8100, 4200, 5173, 3000) |
| Ionic serve ne voit pas un nouveau fichier | Cache TypeScript stale | Kill `ionic serve` (Ctrl+C) puis relance |
| Frontend boutton send disabled même avec texte | Bug de signal de Phase F2 résolu | À jour si tu pull les sources |
| Réponse LLM trop courte avec dolphin | dolphin-mistral est plus terre-à-terre | Demande explicitement "réponse détaillée 3-5 phrases" |
| `ollama list` ne montre pas les modèles | Service Ollama arrêté | `Get-Service "ollama*"` puis `Start-Service` |
| Changer de modèle | Variable d'env `LLM_DEFAULT_MODEL` au démarrage backend | Ex. `$env:LLM_DEFAULT_MODEL = "qwen2.5:7b-instruct"` puis redémarre |

### Modèles Ollama recommandés

| Modèle | Tradeoff | Comment installer |
|---|---|---|
| `llama3.1:8b` | Bon mais moralisateur | `ollama pull llama3.1:8b` |
| `dolphin-mistral:7b` | **Direct sans filtre**, plus rapide | `ollama pull dolphin-mistral:7b` |
| `qwen2.5:7b-instruct` | Bon compromis | `ollama pull qwen2.5:7b-instruct` |
| `mistral:7b-instruct` | Léger et rapide | `ollama pull mistral:7b-instruct` |
| `qwen2.5:14b-instruct` | Plus capable, plus lent sur CPU | `ollama pull qwen2.5:14b-instruct` |

### Promouvoir un utilisateur ADMIN

Une fois le user créé via `/auth/register`, lui ajouter le rôle ADMIN en SQL :

```powershell
$env:PGPASSWORD = "localaiagent"
& 'C:\Program Files\PostgreSQL\16\bin\psql.exe' -h localhost -U localaiagent -d localaiagent -c `
    "INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username='alice' AND r.name='ADMIN' ON CONFLICT DO NOTHING;"
```

Refaire un login → le nouveau JWT contient le rôle ADMIN.

### Reset complet (perte de données)

Si tout est cassé et qu'on veut repartir de zéro :

```powershell
# Arrêter le backend
Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue |
    ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }

# Dropper et recréer la DB
$env:PGPASSWORD = "postgres_admin_pass"
& 'C:\Program Files\PostgreSQL\16\bin\psql.exe' -h localhost -U postgres -d postgres -c `
    "DROP DATABASE IF EXISTS localaiagent; CREATE DATABASE localaiagent OWNER localaiagent;"

# Effacer les fichiers uploadés
Remove-Item -Recurse -Force C:\Users\Administrateur\Pictures\llmproject\backend\data\uploads -ErrorAction SilentlyContinue

# Optionnel : vider la mémoire BGE-M3 si trop de place HF cache
# Remove-Item -Recurse -Force $env:USERPROFILE\.cache\huggingface

# Redémarrer worker + backend → Flyway recrée tout le schéma
```

---

## G. Logs et observation

### Backend

Les logs s'affichent dans la console PowerShell où tourne `java -jar`.
Pour persister :
```powershell
java -jar target\localaiagent-backend.jar 2>&1 | Tee-Object -FilePath backend.log
```

Niveau DEBUG dans le profil `windows-local` pour le package `mr.popo.localaiagent.*`.
Tu vois ainsi chaque itération ReAct (`ReAct iter N llm raw: ...`).

### Worker

Idem côté worker Python — les logs uvicorn + applicatifs s'affichent en console.
Pour rediriger :
```powershell
python -m app.main 2>&1 | Tee-Object -FilePath worker.log
```

### Métriques Prometheus

http://localhost:8081/actuator/prometheus (requiert role ADMIN en prod).
Les métriques par défaut Spring Boot + JVM y sont exposées. Les métriques custom
(`llm.calls.duration`, `tool.calls.duration`…) seront ajoutées en Phase 5.

### PostgreSQL

```powershell
$env:PGPASSWORD = "localaiagent"
& 'C:\Program Files\PostgreSQL\16\bin\psql.exe' -h localhost -U localaiagent -d localaiagent

# Quelques requêtes utiles
\dt                                                     # liste les tables
SELECT * FROM users;
SELECT * FROM chat_sessions ORDER BY id DESC LIMIT 5;
SELECT id, role, content FROM messages ORDER BY id DESC LIMIT 10;
SELECT id, file_name, status, chunk_count, indexed_at FROM documents;
SELECT id, kind, summary FROM memory_entries;
SELECT user_id, fact_key, fact_value FROM user_facts;
SELECT action, user_id, success, created_at FROM audit_logs ORDER BY id DESC LIMIT 20;
\q
```

---

## H. Récapitulatif rapide (cheat sheet)

```powershell
# Sourcer les variables d'env
cd C:\Users\Administrateur\Pictures\llmproject ; . .\.env.ps1

# Fenêtre 1 — worker Python (BGE-M3 + parsing)
cd worker ; .\.venv\Scripts\Activate.ps1 ; python -m app.main

# Fenêtre 2 — backend Spring Boot
cd backend ; java -jar target\localaiagent-backend.jar

# Fenêtre 3 — frontend Ionic + Angular
cd frontend ; ionic serve --no-open --port 8100

# Vérifier (Git Bash)
curl http://localhost:8081/actuator/health
curl http://localhost:9000/health
curl http://localhost:8100/

# Ouvrir l'app dans le navigateur
start http://localhost:8100

# Swagger backend
start http://localhost:8081/swagger-ui.html

# Arrêter chaque service
Ctrl+C dans chaque fenêtre
```

---

## I. Adresses et chemins de référence

| Service | URL | Notes |
|---|---|---|
| **App (frontend)** | http://localhost:8100 | **Interface principale** Ionic + Angular |
| Backend Spring Boot | http://localhost:8081 | Port 8081 (8080 occupé sur Windows Server) |
| Swagger UI | http://localhost:8081/swagger-ui.html | Doc interactive REST |
| Actuator health | http://localhost:8081/actuator/health | Public |
| Actuator Prometheus | http://localhost:8081/actuator/prometheus | ADMIN en prod |
| Worker Python | http://localhost:9000 | FastAPI auto-doc : http://localhost:9000/docs |
| Worker health | http://localhost:9000/health | Public |
| Ollama natif | http://localhost:11434 | API OpenAI : /v1/chat/completions |
| Ollama tags | http://localhost:11434/api/tags | Liste modèles |
| PostgreSQL | localhost:5432 | DB `localaiagent`, user `localaiagent` |

| Dossier | Contenu |
|---|---|
| `backend/` | Code Spring Boot, `pom.xml`, build vers `target/` |
| `backend/data/uploads/{ownerId}/{kbId}/` | Fichiers uploadés persistés sur disque |
| `worker/` | Code FastAPI |
| `worker/.venv/` | venv Python (gitignored) |
| `frontend/` | Code Ionic + Angular |
| `frontend/node_modules/` | Dépendances JS (gitignored) |
| `frontend/www/` | Build prod (`ionic build`) |
| `infra/` | docker-compose (optionnel), scripts Ollama |
| `docs/` | Documentation : PROJECT_STATE.md, FRONTEND_STATE.md, ARCHITECTURE.md, etc. |
| `.env.example` | Modèle de variables d'env |
| `.env.ps1` | Tes variables (gitignored) |

---

## J. Documentation détaillée

| Document | Contenu |
|---|---|
| [`PROJECT_STATE.md`](PROJECT_STATE.md) | **Vue d'ensemble** : architecture, phases livrées (backend + frontend), bugs résolus, décisions clés, roadmap, inventaire fichiers/endpoints |
| [`FRONTEND_STATE.md`](FRONTEND_STATE.md) | **Doc frontend détaillée** : structure dossiers, routes, services, theme, SSE, signals, composants, mapping endpoints |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Diagrammes backend (datée Phase 1) |
| [`DESIGN_RULES.md`](DESIGN_RULES.md) | Règles transversales : ownership, sync PG↔Qdrant, idempotence, disclaimer |
| [`PHASE_ROADMAP.md`](PHASE_ROADMAP.md) | Roadmap d'origine (5 phases backend) |
| [`FRONTEND_PLAN.md`](FRONTEND_PLAN.md) | Plan d'origine frontend (F1→F8) |
| [`SMOKE_TEST.md`](SMOKE_TEST.md) | Scénario de validation Phase 1 |
| [`WINDOWS_LOCAL.md`](WINDOWS_LOCAL.md) | Mode Windows sans Docker |

---

*Dernière mise à jour : après Phase F6 frontend. Configuration testée sur Windows Server 2019,
Xeon 6 cœurs, 64 Go RAM, sans GPU. LLM actuellement utilisé : `dolphin-mistral:7b`.*
