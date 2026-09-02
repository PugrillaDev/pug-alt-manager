[CmdletBinding()]
param(
   [string]$MinecraftDirectory = (Join-Path $env:APPDATA '.minecraft'),
   [switch]$SkipBuild,
   [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

if (-not $SkipBuild) {
   & (Join-Path $PSScriptRoot 'build.ps1') -Offline:$Offline
}

$artifact = Join-Path $projectRoot 'build\libs\pug-alt-manager-1.0.jar'
if (-not (Test-Path -LiteralPath $artifact)) {
   throw "Production artifact does not exist: $artifact"
}

$modsDirectory = Join-Path ([IO.Path]::GetFullPath($MinecraftDirectory)) 'mods'
New-Item -ItemType Directory -Force -Path $modsDirectory | Out-Null

$destination = Join-Path $modsDirectory (Split-Path -Leaf $artifact)
Copy-Item -LiteralPath $artifact -Destination $destination -Force

$sourceHash = (Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash
$installedHash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
if ($sourceHash -ne $installedHash) {
   throw 'Installed JAR hash does not match the build artifact.'
}

[pscustomobject]@{
   InstalledArtifact = $destination
   Sha256 = $installedHash
}
