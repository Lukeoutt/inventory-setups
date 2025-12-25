param(
    [switch]$RunClient,  # use -RunClient to also start RuneLite
    [switch]$RunTests    # include tests during build
)

# === Paths (adjust if you rename/move folders) ===
$pluginDir  = "D:\GitHub Projects\OSRS\inventory-setups"
$runeliteDir = "D:\GitHub Projects\OSRS\runelite"
$sideloadDir = Join-Path $env:USERPROFILE ".runelite\sideloaded-plugins"

# === 1) Build plugin ===
Write-Host "== Building plugin ==" -ForegroundColor Cyan
Push-Location $pluginDir

$gradleArgs = @("build")
if (-not $RunTests) {
    $gradleArgs += "-x"
    $gradleArgs += "test"
}

.\gradlew.bat @gradleArgs
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build FAILED (gradlew returned $LASTEXITCODE)" -ForegroundColor Red
    Pop-Location
    exit $LASTEXITCODE
}

# === 2) Copy jar to sideloaded-plugins ===
Write-Host "== Updating sideloaded-plugins ==" -ForegroundColor Cyan
if (-not (Test-Path $sideloadDir)) {
    New-Item -ItemType Directory -Path $sideloadDir | Out-Null
}

Remove-Item (Join-Path $sideloadDir "*") -Force -ErrorAction SilentlyContinue
Copy-Item ".\build\libs\*.jar" $sideloadDir

Write-Host "Jars in sideloaded-plugins:" -ForegroundColor Yellow
Get-ChildItem $sideloadDir

Pop-Location

# === 3) Optionally run dev client ===
if ($RunClient) {
    Write-Host "== Launching RuneLite dev client ==" -ForegroundColor Cyan
    Push-Location $runeliteDir

    java -ea -jar ".\runelite-client\build\libs\client-1.12.11-SNAPSHOT-shaded.jar" --developer-mode

    Pop-Location
}
