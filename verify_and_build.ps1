param(
    [string]$RepoPath = $PSScriptRoot
)

$ErrorActionPreference = "Stop"
$RepoPath = (Resolve-Path $RepoPath).Path
$envFile = Join-Path $RepoPath ".env"

if (-not (Test-Path $envFile)) {
    throw "Missing .env in $RepoPath. Copy .env.example to .env and add your keys."
}

$values = @{}
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $parts = $line.Split("=", 2)
        $values[$parts[0].Trim()] = $parts[1].Trim()
    }
}

$maps = $values["MAPS_API_KEY"]
$gemini = $values["GEMINI_API_KEY"]
if ([string]::IsNullOrWhiteSpace($maps) -or $maps -match "YOUR_.*API_KEY") {
    throw "MAPS_API_KEY is missing or still a placeholder."
}
if ([string]::IsNullOrWhiteSpace($gemini) -or $gemini -match "YOUR_.*API_KEY|MY_GEMINI_API_KEY") {
    throw "GEMINI_API_KEY is missing or still a placeholder."
}

Write-Host "OK: MAPS_API_KEY configured (value hidden)" -ForegroundColor Green
Write-Host "OK: GEMINI_API_KEY configured (value hidden)" -ForegroundColor Green

# Prefer an installed Adoptium JDK 17 when JAVA_HOME is missing or points elsewhere.
$jdk17 = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-17*-hotspot" -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1
if ($jdk17) {
    $env:JAVA_HOME = $jdk17.FullName
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
}

Write-Host "Java:" -ForegroundColor Cyan
& java -version

Push-Location $RepoPath
try {
    if (-not (Test-Path ".\gradlew.bat")) {
        throw "gradlew.bat is missing from the repository root."
    }
    & .\gradlew.bat --stop | Out-Null
    & .\gradlew.bat :app:assembleDebug --no-daemon --no-parallel --no-configuration-cache --stacktrace
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }

    $apk = Join-Path $RepoPath "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apk)) { throw "Gradle finished but app-debug.apk was not found." }
    Write-Host "OK: APK built" -ForegroundColor Green
    Write-Host $apk
} finally {
    Pop-Location
}
