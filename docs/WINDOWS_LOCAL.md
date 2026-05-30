# Mode lancement local Windows (sans Docker)

Pour itérer rapidement depuis l'IDE sur Windows Server 2019 / Windows 10/11
sans dockeriser PostgreSQL ni Ollama.

## Pré-requis

| Outil | Version | Notes |
|---|---|---|
| Java | 21 (Eclipse Temurin) | Ajouter à `JAVA_HOME` et au `PATH` |
| Maven | 3.9+ | Vérifier `mvn -v` |
| PostgreSQL | 16 | Port 5432, créer DB et user (voir plus bas) |
| Ollama | Windows | Téléchargement : https://ollama.com/download/windows |
| Git | dernier | Pour cloner le repo |

## 1. Préparer PostgreSQL

Ouvrir `psql` en tant que postgres :

```sql
CREATE USER localaiagent WITH PASSWORD 'change_me';
CREATE DATABASE localaiagent OWNER localaiagent;
GRANT ALL PRIVILEGES ON DATABASE localaiagent TO localaiagent;
```

Vérifier la connexion :
```powershell
psql -h localhost -U localaiagent -d localaiagent -c "select version();"
```

## 2. Démarrer Ollama et pull du modèle

Lancer Ollama (icône taskbar ou commande).

```powershell
ollama pull llama3.1:8b
ollama list
```

Vérifier qu'il écoute :
```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:11434/api/tags
```

## 3. Configurer les variables d'environnement

Créer un script `.env.ps1` à la racine (ne pas commit) :

```powershell
$env:SPRING_PROFILES_ACTIVE = "windows-local"
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "localaiagent"
$env:DB_USER = "localaiagent"
$env:DB_PASS = "change_me"
$env:JWT_SECRET = "please_change_me_to_a_random_32_chars_minimum_secret"
$env:FRONT_ORIGIN = "http://localhost:4200"
$env:LLM_BASE_URL = "http://localhost:11434/v1"
$env:LLM_NATIVE_BASE_URL = "http://localhost:11434"
$env:LLM_DEFAULT_MODEL = "llama3.1:8b"
```

Sourcer le script à chaque session :
```powershell
. .\.env.ps1
```

## 4. Démarrer le backend

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=windows-local"
```

Attendre `Started LocalAiAgentApplication`, puis ouvrir :
- Swagger : http://localhost:8080/swagger-ui.html
- Healthcheck : http://localhost:8080/actuator/health

## 5. Tester end-to-end

Voir [`SMOKE_TEST.md`](SMOKE_TEST.md).

## Avantages / inconvénients

**Avantages**
- Démarrage très rapide depuis l'IDE.
- Debug Java direct.
- Pas de couche Docker pour les couches sensibles.

**Inconvénients**
- Dépendances système à maintenir à la main.
- Pas conseillé pour la prod (utiliser Docker).

## Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| `Connection refused` PG | service Postgres arrêté | `Get-Service postgresql*` puis `Start-Service` |
| `Connection refused` Ollama | Ollama tray non lancé | rouvrir Ollama, vérifier `/api/tags` |
| `JWT secret too short` | env non chargé | re-sourcer `.env.ps1` |
| `Flyway validation failed` | DB pré-existante | DROP/CREATE la DB ou ajouter migration |
| Tokens streamés lents | CPU saturé par modèle 14B+ | repasser à `llama3.1:8b` |
