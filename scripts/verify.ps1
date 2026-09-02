[CmdletBinding()]
param(
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

$jarTool = Join-Path $env:JAVA_HOME 'bin\jar.exe'
if (-not (Test-Path -LiteralPath $jarTool)) {
   throw 'The Java 8 jar tool could not be found through JAVA_HOME.'
}

$sourceRoot = Join-Path $projectRoot 'src\main\java'
$sourceFiles = Get-ChildItem -LiteralPath $sourceRoot -Filter '*.java' -Recurse
$placeholderPattern = '\$VF:|\bclass_[0-9]+\b|\bmethod_[0-9]+\b|\blambda\$|\bf75\b'
$sourceHits = $sourceFiles | Select-String -Pattern $placeholderPattern
if ($sourceHits) {
   $sourceHits | ForEach-Object { Write-Error $_.ToString() }
   throw 'Decompiler or obfuscation placeholders remain in source.'
}

$entries = & $jarTool tf $artifact
if ($LASTEXITCODE -ne 0) {
   throw 'The production JAR could not be read.'
}

$classCount = @($entries | Where-Object { $_ -like '*.class' }).Count
if ($classCount -ne 78) {
   throw "Expected 78 class entries, found $classCount."
}

$obfuscatedEntries = @($entries | Where-Object { $_ -like 'f75/*' })
if ($obfuscatedEntries.Count -ne 0) {
   throw "The production JAR still contains $($obfuscatedEntries.Count) f75 entries."
}

if ($entries -notcontains 'dev/pugrilla/altmanager/AltManager.class') {
   throw 'The Forge mod entry class is missing.'
}

if ($entries -notcontains 'mcmod.info') {
   throw 'mcmod.info is missing.'
}

$hash = (Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash

[pscustomobject]@{
   Artifact = $artifact
   Sha256 = $hash
   ClassCount = $classCount
   ObfuscatedPackageEntries = $obfuscatedEntries.Count
   SourcePlaceholderHits = @($sourceHits).Count
}
