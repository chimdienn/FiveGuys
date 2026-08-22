param(
    [string]$RepoPath = $PSScriptRoot
)

$ErrorActionPreference = "Stop"
$RepoPath = (Resolve-Path $RepoPath).Path
$apk = Join-Path $RepoPath "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) {
    throw "APK not found. Run .\verify_and_build.ps1 first."
}

$sdk = $null
$localProps = Join-Path $RepoPath "local.properties"
if (Test-Path $localProps) {
    $line = Get-Content $localProps | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
    if ($line) {
        $sdk = ($line -replace '^sdk\.dir=', '') -replace '\\\\', '\'
        $sdk = $sdk -replace '\\:', ':'
    }
}
if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
if (-not (Test-Path $adb)) { throw "adb.exe not found at $adb" }

& $adb devices
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) {
    Write-Warning "If you see INSTALL_FAILED_UPDATE_INCOMPATIBLE, uninstall the old Biomate app once, then rerun this script."
    exit $LASTEXITCODE
}
Write-Host "OK: Biomate installed on the connected device." -ForegroundColor Green
