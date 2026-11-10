param([switch]$Test, [switch]$Package, [switch]$IsolatedDatabase, [ValidateRange(0,65535)][int]$Port = 0)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (!$env:JAVA_HOME -or !(Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) { throw 'Set JAVA_HOME to a Java 21 JDK' }
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH
$localEnv = Join-Path $projectRoot '.local\dev-env.ps1'
if ($IsolatedDatabase) { $localEnv = Join-Path $projectRoot '.local\isolated-env.ps1' }
if (Test-Path -LiteralPath $localEnv) { . $localEnv }
if (($Test -or $Package) -and !$IsolatedDatabase -and (Test-Path -LiteralPath $localEnv)) {
    & (Join-Path $PSScriptRoot 'Export-TestConfig.ps1')
}
if ($Port -gt 0) { $env:SERVER_PORT = "$Port" }
Push-Location $projectRoot
try {
    if ($Test) { & mvn --batch-mode test }
    elseif ($Package) { & mvn --batch-mode verify }
    else { & mvn --batch-mode spring-boot:run }
    if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }
} finally { Pop-Location }
