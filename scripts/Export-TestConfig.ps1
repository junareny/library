param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.local\dev-env.ps1'
if (!(Test-Path -LiteralPath $envFile)) {
    throw '缺少 .local/dev-env.ps1，请先配置本地测试数据库。'
}
. $envFile

# 导出为 Spring 可直接加载的本地配置，兼容 IDE 直接运行 JUnit。
function ConvertTo-PropertyValue([string]$Value) {
    return $Value.Replace('\', '\\').Replace("`r", '\r').Replace("`n", '\n').Replace("`t", '\t').Replace(' ', '\ ')
}
$lines = @('# 本地测试数据库配置，由 scripts/Export-TestConfig.ps1 生成，不得提交。')
foreach ($name in @('TEST_DB_URL', 'TEST_DB_USERNAME', 'TEST_DB_PASSWORD')) {
    $value = [Environment]::GetEnvironmentVariable($name, 'Process')
    if ([string]::IsNullOrWhiteSpace($value)) { throw "缺少测试数据库配置项：$name" }
    $lines += "$name=$(ConvertTo-PropertyValue $value)"
}
$targetFile = Join-Path $projectRoot '.local\test-database.properties'
[IO.File]::WriteAllLines($targetFile, $lines, [Text.UTF8Encoding]::new($false))
Write-Output '已生成本地测试配置，可在项目根目录使用 IDE 或 Maven 直接运行测试。'
