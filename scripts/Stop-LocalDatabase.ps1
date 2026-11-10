param([string]$MySqlHome = $env:MYSQL_HOME)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($MySqlHome)) { throw '请设置 MYSQL_HOME 或传入 -MySqlHome。' }
$projectRoot = Split-Path -Parent $PSScriptRoot
$config = Join-Path $projectRoot '.local\root-client.ini'
$mysqladmin = Join-Path $MySqlHome 'bin\mysqladmin.exe'
if (!(Test-Path -LiteralPath $config)) { throw 'Isolated database root-client.ini is missing' }
& $mysqladmin "--defaults-file=$config" shutdown
if ($LASTEXITCODE -ne 0) { throw 'Isolated database shutdown failed' }
