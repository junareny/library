param([string]$BaseUrl = 'http://127.0.0.1:18080')
$ErrorActionPreference = 'Stop'
function Invoke-Biz([string]$Path, [object]$Body, [hashtable]$Headers = @{}) {
    $json = $Body | ConvertTo-Json -Depth 5 -Compress
    $result = Invoke-RestMethod "$BaseUrl$Path" -Method Post -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($json)) -Headers $Headers
    if ($result.code -ne '0') { throw "Business call failed: $Path ($($result.code))" }
    return $result.body
}
$username = 'smoke_' + [guid]::NewGuid().ToString('N').Substring(0,12)
$password = [guid]::NewGuid().ToString('N')
$credentials = @{username=$username; password=$password}
$books = Invoke-Biz '/IBookBiz/queryBook/page' @{pageCurrent=1; pageSize=20}
if ($books.total -ne 10) { throw 'Unexpected seed book count' }
$detail = Invoke-Biz '/IBookBiz/queryBook' @{id=1}
if ($detail.availableCopies -lt 1) { throw 'Smoke test needs an available copy of book 1' }
$registration = Invoke-Biz '/IAuthBiz/register' $credentials
$login = Invoke-Biz '/IAuthBiz/login' $credentials
$headers = @{Authorization="Bearer $($login.accessToken)"}
$loan = Invoke-Biz '/ILoanBiz/borrowBook' @{bookId=1} $headers
try {
    $current = Invoke-Biz '/ILoanBiz/queryCurrentLoan/page' @{pageCurrent=1; pageSize=20} $headers
    if ($current.total -ne 1 -or $current.data[0].id -ne $loan.id) { throw 'Current loans mismatch' }
} finally {
    $null = Invoke-Biz '/ILoanBiz/returnBook' @{id=$loan.id} $headers
}
$after = Invoke-Biz '/ILoanBiz/queryCurrentLoan/page' @{} $headers
$afterBook = Invoke-Biz '/IBookBiz/queryBook' @{id=1}
if ($after.total -ne 0 -or $afterBook.availableCopies -ne $detail.availableCopies) { throw 'Inventory or active loan mismatch after return' }
$openapi = Invoke-RestMethod "$BaseUrl/v3/api-docs"
if (!$openapi.components.securitySchemes.bearerAuth) { throw 'Missing OpenAPI Bearer authentication scheme' }
$operations = @($openapi.paths.PSObject.Properties | ForEach-Object { $_.Value.PSObject.Properties | Where-Object Name -eq 'post' }).Count
if ($operations -ne 7) { throw 'Unexpected API operation count' }
$swagger = Invoke-WebRequest "$BaseUrl/swagger-ui/index.html" -UseBasicParsing
if ($swagger.StatusCode -ne 200) { throw 'Swagger UI failed' }
@{result='PASS'; apiOperations=$operations; swagger='PASS'; inventoryRestored=$true; username=$username; loanId=$loan.id} | ConvertTo-Json
