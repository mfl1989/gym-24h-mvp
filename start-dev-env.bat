@echo off
setlocal

set "ROOT=%~dp0"

start "Gym24h Local Dev" powershell -ExecutionPolicy Bypass -File "%ROOT%scripts\start-local-dev.ps1" -OpenBrowser -StartStripeListener
start "Gym24h Ngrok Backend" cmd /k "cd /d "%ROOT%" && .\ngrok.exe http 8080"
start "Gym24h Cloudflare Frontend" cmd /k "cd /d "%ROOT%gym24h-frontend" && npx untun@latest tunnel http://localhost:5173"

endlocal