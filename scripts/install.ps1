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

$versionLine = Get-Content -LiteralPath (Join-Path $projectRoot 'gradle.properties') | Where-Object { $_ -match '^modVersion=' }
$modVersion = ($versionLine -split '=', 2)[1].Trim()
if ($modVersion -notmatch '^\d+\.\d+(?:\.\d+)?$') { throw 'Invalid modVersion in gradle.properties.' }
$artifact = Join-Path $projectRoot "build\libs\pug-alt-manager-$modVersion.jar"
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

$backupDirectory = Join-Path (Split-Path -Parent $modsDirectory) ('mod-backups\pug-alt-manager-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$modsRoot = [IO.Path]::GetFullPath($modsDirectory).TrimEnd('\') + '\'
$backupRoot = [IO.Path]::GetFullPath($backupDirectory).TrimEnd('\') + '\'
Get-ChildItem -LiteralPath $modsDirectory -File -Filter 'pug-alt-manager-*.jar' |
   Where-Object { $_.Name -ne (Split-Path -Leaf $artifact) } |
   ForEach-Object {
      $oldPath = [IO.Path]::GetFullPath($_.FullName)
      $backupPath = [IO.Path]::GetFullPath((Join-Path $backupDirectory $_.Name))
      if (-not $oldPath.StartsWith($modsRoot, [StringComparison]::OrdinalIgnoreCase) -or
          -not $backupPath.StartsWith($backupRoot, [StringComparison]::OrdinalIgnoreCase)) {
         throw 'Refusing to move a mod outside the expected directories.'
      }
      New-Item -ItemType Directory -Force -Path $backupDirectory | Out-Null
      Move-Item -LiteralPath $oldPath -Destination $backupPath
   }

[pscustomobject]@{
   InstalledArtifact = $destination
   Sha256 = $installedHash
}
