$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$metadataPath = Join-Path $projectRoot '.local\app-process.json'
if (!(Test-Path -LiteralPath $metadataPath)) { throw 'No recorded application process' }
$metadata = Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json
$process = Get-CimInstance Win32_Process -Filter "ProcessId=$($metadata.pid)"
if (!$process) { Write-Output 'Application is already stopped'; return }
$liveStart = (Get-Process -Id $metadata.pid).StartTime.ToUniversalTime().ToString('o')
$recordedStart = ([datetime]$metadata.startedAt).ToUniversalTime().ToString('o')
if (!$process.CommandLine.Contains($metadata.jar) -or $liveStart -ne $recordedStart) {
    throw 'Recorded PID no longer belongs to the expected application; refusing to stop it'
}
Stop-Process -Id $metadata.pid
Write-Output 'Application stopped'
