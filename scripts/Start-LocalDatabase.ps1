param(
    [string]$MySqlHome = $env:MYSQL_HOME,
    [int]$Port = 13307
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($MySqlHome)) { throw '请设置 MYSQL_HOME 或传入 -MySqlHome。' }
$projectRoot = Split-Path -Parent $PSScriptRoot
$localDir = Join-Path $projectRoot '.local'
$dataDir = Join-Path $localDir 'mysql-data'
$config = Join-Path $localDir 'mysql.ini'
$envFile = Join-Path $localDir 'isolated-env.ps1'
$mysqld = Join-Path $MySqlHome 'bin\mysqld.exe'
$mysql = Join-Path $MySqlHome 'bin\mysql.exe'
if (!(Test-Path -LiteralPath $mysqld)) { throw 'MySQL 5.7 server executable not found' }
New-Item -ItemType Directory -Force -Path $localDir | Out-Null
if (Test-Path -LiteralPath $envFile) {
    if (!(Test-Path -LiteralPath $config)) { throw 'Local configuration is missing; inspect .local before retrying' }
    if (!(Select-String -LiteralPath $config -Pattern "^port=$Port`$" -Quiet)) { throw 'Requested port differs from the existing isolated instance configuration' }
    $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (!$listeners) {
        Start-Process -FilePath $mysqld -ArgumentList ('--defaults-file="' + $config + '"') -WindowStyle Hidden
    }
    $rootConfig = Join-Path $localDir 'root-client.ini'
    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        & $mysql "--defaults-file=$rootConfig" --connect-timeout=1 -e 'SELECT 1' 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (!$ready) { throw 'Isolated database failed readiness verification' }
    . $envFile
    Write-Output "Existing local database configured on port $Port. Environment loaded into this shell."
    return
}
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "Port $Port is already in use" }
if (Test-Path -LiteralPath $dataDir) { throw 'Unfinished data directory exists. Inspect .local/mysql-data; this script never deletes it.' }
New-Item -ItemType Directory -Path $dataDir | Out-Null
$basePath = $MySqlHome.Replace('\', '/')
$dataPath = $dataDir.Replace('\', '/')
@"
[mysqld]
basedir=$basePath
datadir=$dataPath
port=$Port
bind-address=127.0.0.1
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default-storage-engine=INNODB
explicit_defaults_for_timestamp=ON
sql_mode=STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION
log-error=$dataPath/server-error.log
"@ | Set-Content -LiteralPath $config -Encoding ascii
& $mysqld "--defaults-file=$config" --initialize-insecure
if ($LASTEXITCODE -ne 0) { throw 'Database initialization failed; inspect .local/mysql-data/server-error.log' }
$process = Start-Process -FilePath $mysqld -ArgumentList ('--defaults-file="' + $config + '"') -WindowStyle Hidden -PassThru
$ready = $false
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    & $mysql --no-defaults --protocol=TCP -h 127.0.0.1 "-P$Port" -u root --connect-timeout=1 -e 'SELECT 1' 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep -Milliseconds 500
}
if (!$ready) { throw 'Local database did not become ready; inspect server-error.log' }
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
function New-LocalSecret {
    $bytes = New-Object byte[] 32
    $random.GetBytes($bytes)
    return [Convert]::ToBase64String($bytes)
}
$rootPassword = New-LocalSecret
$devPassword = New-LocalSecret
$testPassword = New-LocalSecret
$jwtSecret = New-LocalSecret
$sql = @"
ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword';
CREATE DATABASE library_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE library_app_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'library_app'@'127.0.0.1' IDENTIFIED BY '$devPassword';
CREATE USER 'library_app_test'@'127.0.0.1' IDENTIFIED BY '$testPassword';
GRANT ALL PRIVILEGES ON library_app.* TO 'library_app'@'127.0.0.1';
GRANT ALL PRIVILEGES ON library_app_test.* TO 'library_app_test'@'127.0.0.1';
"@
$sql | & $mysql --no-defaults --protocol=TCP -h 127.0.0.1 "-P$Port" -u root --default-character-set=utf8mb4
if ($LASTEXITCODE -ne 0) { throw 'Account creation failed; inspect the isolated instance before retrying' }
@"
[client]
host=127.0.0.1
port=$Port
user=root
password=$rootPassword
protocol=TCP
"@ | Set-Content -LiteralPath (Join-Path $localDir 'root-client.ini') -Encoding ascii
@"
`$env:DB_URL = 'jdbc:mysql://127.0.0.1:$Port/library_app?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&useSSL=false&allowPublicKeyRetrieval=true'
`$env:DB_USERNAME = 'library_app'
`$env:DB_PASSWORD = '$devPassword'
`$env:TEST_DB_URL = 'jdbc:mysql://127.0.0.1:$Port/library_app_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&useSSL=false&allowPublicKeyRetrieval=true'
`$env:TEST_DB_USERNAME = 'library_app_test'
`$env:TEST_DB_PASSWORD = '$testPassword'
`$env:JWT_SECRET = '$jwtSecret'
"@ | Set-Content -LiteralPath $envFile -Encoding ascii
$process.Id | Set-Content -LiteralPath (Join-Path $localDir 'mysql.pid') -Encoding ascii
. $envFile
if (!(Test-Path -LiteralPath (Join-Path $localDir 'dev-env.ps1'))) {
    Copy-Item -LiteralPath $envFile -Destination (Join-Path $localDir 'dev-env.ps1')
}
Write-Output "Isolated MySQL ready on 127.0.0.1:$Port. Credentials saved only in ignored .local files."
