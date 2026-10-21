$ErrorActionPreference = 'Stop'
$ProjectRoot = Join-Path $PSScriptRoot '..\nabta_phase2_frontend' | Resolve-Path
Set-Location $ProjectRoot

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw 'Node.js/npm was not found. Install a current Node.js LTS version, restart VS Code, and try again.'
}

# 10.0.2.2 is the Android Emulator bridge to the host computer's localhost.
$env:VITE_NABTA_API_BASE = 'http://10.0.2.2:8080/api'
$env:NABTA_ANDROID_DEV_HTTP = 'true'

Write-Host ''
Write-Host 'Nabta Android emulator setup' -ForegroundColor Green
Write-Host "Frontend: $ProjectRoot"
Write-Host "Backend API: $env:VITE_NABTA_API_BASE"
Write-Host 'Ensure the Java backend is running and MySQL is connected.'
Write-Host ''

npm install
if ($LASTEXITCODE -ne 0) { throw 'npm install failed. Fix the npm error before continuing.' }

npm run build
if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed. Fix the build error before continuing.' }

if (-not (Test-Path (Join-Path $ProjectRoot 'android'))) {
    npx cap add android
    if ($LASTEXITCODE -ne 0) { throw 'Capacitor could not create the Android project.' }
}

npx cap sync android
if ($LASTEXITCODE -ne 0) { throw 'Capacitor sync failed.' }

Write-Host ''
Write-Host 'Opening Android Studio...' -ForegroundColor Green
npx cap open android
if ($LASTEXITCODE -ne 0) {
    Write-Host 'If Android Studio did not open, launch it and choose Open, then select the android folder inside nabta_phase2_frontend.' -ForegroundColor Yellow
}
