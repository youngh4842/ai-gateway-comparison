param(
    [Parameter(Mandatory = $true)]
    [string] $Name
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$source = Join-Path $projectRoot "gateways\$Name"
$target = Join-Path $projectRoot "gateway"

if (-not (Test-Path -LiteralPath $source)) {
    throw "Gateway template not found: $source"
}

$resolvedRoot = (Resolve-Path -LiteralPath $projectRoot).Path
$resolvedTargetParent = (Resolve-Path -LiteralPath (Split-Path -Parent $target)).Path
if ($resolvedTargetParent -ne $resolvedRoot) {
    throw "Refusing to replace gateway outside project root: $target"
}

if (Test-Path -LiteralPath $target) {
    Get-ChildItem -LiteralPath $target -Force | Remove-Item -Recurse -Force
}
else {
    New-Item -ItemType Directory -Path $target | Out-Null
}

Copy-Item -Path (Join-Path $source '*') -Destination $target -Recurse -Force
Write-Host "Active gateway switched to '$Name'."
