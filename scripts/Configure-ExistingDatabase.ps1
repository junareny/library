param(
    [string]$MySqlHome = $env:MYSQL_HOME,
    [string]$RootClientConfig = (Join-Path (Split-Path -Parent $PSScriptRoot) '.local\original-root-client.ini'),
    [int]$Port = 3306
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($MySqlHome)) { throw '请设置 MYSQL_HOME 或传入 -MySqlHome。' }
$projectRoot = Split-Path -Parent $PSScriptRoot
$localDir = Join-Path $projectRoot '.local'
$mysql = Join-Path $MySqlHome 'bin\mysql.exe'
if (!(Test-Path -LiteralPath $RootClientConfig)) { throw 'Provide a private MySQL client config using -RootClientConfig' }
$existing = & $mysql "--defaults-file=$RootClientConfig" --host=127.0.0.1 --protocol=TCP "--port=$Port" --batch --skip-column-names -e "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME IN ('library_app','library_app_test'); SELECT user FROM mysql.user WHERE user IN ('library_app','library_app_test');"
if ($LASTEXITCODE -ne 0) { throw 'Root connection failed' }
if ($existing) { throw 'Target schemas or usernames already exist; refusing to overwrite them' }
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
function New-LocalSecret {
    $bytes = New-Object byte[] 32
    $random.GetBytes($bytes)
    return [Convert]::ToBase64String($bytes)
}
$devPassword = New-LocalSecret
$testPassword = New-LocalSecret
$jwtSecret = New-LocalSecret
$sql = @"
CREATE DATABASE library_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE library_app_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'library_app'@'127.0.0.1' IDENTIFIED BY '$devPassword';
CREATE USER 'library_app_test'@'127.0.0.1' IDENTIFIED BY '$testPassword';
GRANT ALL PRIVILEGES ON library_app.* TO 'library_app'@'127.0.0.1';
GRANT ALL PRIVILEGES ON library_app_test.* TO 'library_app_test'@'127.0.0.1';
"@
$sql | & $mysql "--defaults-file=$RootClientConfig" --host=127.0.0.1 --protocol=TCP "--port=$Port" --default-character-set=utf8mb4
if ($LASTEXITCODE -ne 0) { throw 'Database setup failed; inspect target schemas before retrying' }
New-Item -ItemType Directory -Force -Path $localDir | Out-Null
$envFile = Join-Path $localDir 'dev-env.ps1'
$isolatedEnv = Join-Path $localDir 'isolated-env.ps1'
if ((Test-Path -LiteralPath $envFile) -and !(Test-Path -LiteralPath $isolatedEnv)) {
    Copy-Item -LiteralPath $envFile -Destination $isolatedEnv
}
@"
`$env:DB_URL = 'jdbc:mysql://127.0.0.1:$Port/library_app?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&useSSL=false&allowPublicKeyRetrieval=true'
`$env:DB_USERNAME = 'library_app'
`$env:DB_PASSWORD = '$devPassword'
`$env:TEST_DB_URL = 'jdbc:mysql://127.0.0.1:$Port/library_app_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&useSSL=false&allowPublicKeyRetrieval=true'
`$env:TEST_DB_USERNAME = 'library_app_test'
`$env:TEST_DB_PASSWORD = '$testPassword'
`$env:JWT_SECRET = '$jwtSecret'
"@ | Set-Content -LiteralPath $envFile -Encoding ascii
. $envFile
& (Join-Path $PSScriptRoot 'Export-TestConfig.ps1')
Write-Output "Dedicated development and test schemas/accounts configured on 127.0.0.1:$Port. Local environment updated."
