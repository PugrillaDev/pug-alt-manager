[CmdletBinding()]
param(
   [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$wrapper = Join-Path $projectRoot 'gradlew.bat'

if (-not $env:JAVA_HOME) {
   throw 'JAVA_HOME must point to a Java 8 JDK.'
}

$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) {
   throw "Java was not found at $java"
}

$versionOutput = & $java -version 2>&1 | Out-String
if ($versionOutput -notmatch 'version "1\.8\.') {
   throw "Java 8 is required. Detected:`n$versionOutput"
}

$arguments = @('clean', 'build', '--console', 'plain')
if ($Offline) {
   $arguments += '--offline'
}

Push-Location $projectRoot
try {
   & $wrapper @arguments
   if ($LASTEXITCODE -ne 0) {
      throw "Gradle build failed with exit code $LASTEXITCODE"
   }
} finally {
   Pop-Location
}

Get-ChildItem -LiteralPath (Join-Path $projectRoot 'build\libs') -Filter '*.jar' |
   Select-Object Name, Length, LastWriteTime
