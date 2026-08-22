param(
    [string]$RepoPath = $PSScriptRoot
)

$ErrorActionPreference = "Stop"
$envFile = Join-Path $RepoPath ".env"
if (-not (Test-Path $envFile)) {
    Write-Host "ERROR: .env was not found at $envFile" -ForegroundColor Red
    exit 1
}

$geminiKey = $null
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.StartsWith("GEMINI_API_KEY=")) {
        $geminiKey = $line.Substring("GEMINI_API_KEY=".Length).Trim().Trim('"').Trim("'")
    }
}

if ([string]::IsNullOrWhiteSpace($geminiKey) -or $geminiKey -eq "YOUR_GEMINI_API_KEY" -or $geminiKey -eq "MY_GEMINI_API_KEY") {
    Write-Host "ERROR: GEMINI_API_KEY is missing or still a placeholder." -ForegroundColor Red
    exit 1
}

Write-Host "Gemini key found (value hidden)." -ForegroundColor Green
Write-Host "Testing gemini-3.6-flash..."

$uri = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"
$headers = @{ "x-goog-api-key" = $geminiKey }
$body = @{
    contents = @(
        @{
            parts = @(
                @{ text = "Reply with exactly: BIOMATE_GEMINI_OK" }
            )
        }
    )
} | ConvertTo-Json -Depth 8

try {
    $response = Invoke-RestMethod -Uri $uri -Method Post -Headers $headers -ContentType "application/json" -Body $body -TimeoutSec 60
    $text = $response.candidates[0].content.parts[0].text
    Write-Host "Gemini API connection: OK" -ForegroundColor Green
    Write-Host "Model response: $text"
    exit 0
}
catch {
    Write-Host "Gemini API connection: FAILED" -ForegroundColor Red
    $message = $_.Exception.Message
    Write-Host $message -ForegroundColor Yellow

    try {
        if ($_.ErrorDetails.Message) {
            $detail = $_.ErrorDetails.Message | ConvertFrom-Json
            if ($detail.error.message) {
                Write-Host "Google API message: $($detail.error.message)" -ForegroundColor Yellow
            }
            if ($detail.error.status) {
                Write-Host "Google API status: $($detail.error.status)" -ForegroundColor Yellow
            }
        }
    } catch { }

    Write-Host "The API key value was not printed." -ForegroundColor DarkGray
    exit 1
}
