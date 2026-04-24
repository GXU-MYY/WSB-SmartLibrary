param(
    [string]$BaseUrl = "http://127.0.0.1:8848",
    [string]$Group = "DEFAULT_GROUP"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$configFiles = Get-ChildItem -LiteralPath $scriptDir -File -Filter *.yml | Sort-Object Name

if (-not $configFiles) {
    throw "No Nacos config files found in $scriptDir"
}

foreach ($file in $configFiles) {
    $content = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
    $response = Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/nacos/v1/cs/configs" `
        -ContentType "application/x-www-form-urlencoded; charset=UTF-8" `
        -Body @{
            dataId = $file.Name
            group = $Group
            content = $content
        }

    if ($response -ne "true") {
        throw "Import failed for $($file.Name): $response"
    }

    Write-Host "Imported $($file.Name)"
}
