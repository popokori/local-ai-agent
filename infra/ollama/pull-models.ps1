# Pré-télécharge les modèles Ollama (Windows / PowerShell).
# Usage : .\pull-models.ps1
# Avec Docker compose : conteneur ; avec Ollama Windows natif : ollama.exe direct.

param(
    [string]$Model = $env:LLM_DEFAULT_MODEL,
    [string]$Container = "localaiagent-ollama",
    [switch]$Native
)

if ([string]::IsNullOrWhiteSpace($Model)) {
    $Model = "llama3.1:8b"
}

if ($Native) {
    Write-Host "Pulling $Model via Ollama Windows natif..."
    & ollama pull $Model
    & ollama list
} else {
    Write-Host "Pulling $Model into container $Container..."
    & docker exec $Container ollama pull $Model
    & docker exec $Container ollama list
}
