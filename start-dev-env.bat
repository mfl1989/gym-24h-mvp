@echo off
setlocal

set "ROOT=%~dp0"

start "Gym24h Backend" cmd /k "cd /d "%ROOT%" && .\gradle-8.5\bin\gradle.bat bootRun --args=--spring.profiles.active=local"
start "Gym24h Frontend" cmd /k "cd /d "%ROOT%gym24h-frontend" && npm run dev -- --host 0.0.0.0"
start "Gym24h Ngrok Backend" cmd /k "cd /d "%ROOT%" && .\ngrok.exe http 8080"
start "Gym24h Cloudflare Frontend" cmd /k "cd /d "%ROOT%gym24h-frontend" && npx untun@latest tunnel http://localhost:5173"

endlocal