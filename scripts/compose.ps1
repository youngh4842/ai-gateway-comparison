param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $ComposeArgs
)

$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    docker compose -f docker-compose.apps.yml -f gateway/docker-compose.gateway.yml @ComposeArgs
}
finally {
    Pop-Location
}
