param([ValidateRange(1,65535)][int]$Port = 18080, [switch]$IsolatedDatabase)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$buildJar = Join-Path $projectRoot 'target\library-1.0.0.jar'
$localDir = Join-Path $projectRoot '.local'
if (!(Test-Path -LiteralPath $buildJar)) { throw 'Build the application with scripts/Run.ps1 -Package first' }
if (!$env:JAVA_HOME) { throw 'Set JAVA_HOME to Java 21' }
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (!(Test-Path -LiteralPath $java)) { throw 'JAVA_HOME/bin/java.exe does not exist' }
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "Port $Port is in use" }
New-Item -ItemType Directory -Force -Path $localDir | Out-Null
$runtimeDir = Join-Path $localDir 'runtime'
New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
$jar = Join-Path $runtimeDir 'library-1.0.0.jar'
Copy-Item -LiteralPath $buildJar -Destination $jar -Force
$envFile = Join-Path $localDir 'dev-env.ps1'
if ($IsolatedDatabase) { $envFile = Join-Path $localDir 'isolated-env.ps1' }
if (Test-Path -LiteralPath $envFile) { . $envFile }
$env:SERVER_PORT = "$Port"
$process = Start-Process -FilePath $java -ArgumentList @('-jar', ('"' + $jar + '"'), '--server.address=127.0.0.1') -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $localDir 'app-stdout.log') -RedirectStandardError (Join-Path $localDir 'app-stderr.log')
@{pid=$process.Id; port=$Port; jar=$jar; startedAt=$process.StartTime.ToUniversalTime().ToString('o')} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $localDir 'app-process.json') -Encoding ascii
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    if ($process.HasExited) { throw 'Application exited; inspect .local/app-stdout.log and app-stderr.log' }
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/IBookBiz/queryBook/page" -Method Post -ContentType application/json -Body '{}' -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) {
            Write-Output "Application ready: http://127.0.0.1:$Port/swagger-ui/index.html (PID $($process.Id))"
            return
        }
    } catch { }
    Start-Sleep -Milliseconds 500
}
throw 'Application readiness timed out; inspect .local logs. Stop-App.ps1 can stop the recorded process.'
