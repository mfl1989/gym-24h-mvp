# gym-24h-mvp

24時間無人ジムSaaSのMVP骨格です。Spring Boot 3.x / Java 21 / Gradle を前提に、ドメイン・アプリケーション・インフラ・プレゼンテーションを分離した構成で立ち上げています。

## 起動

```bash
./gradlew bootRun
```

PostgreSQL を使う既定プロファイルで起動する場合は、事前に `DB_PASSWORD` を設定してください。

```powershell
$env:DB_PASSWORD='postgres'
.\gradle-8.5\bin\gradle.bat bootRun
```

ローカルの PostgreSQL を固定資格情報で使う場合は、専用の `postgres-local` プロファイルでも起動できます。

```powershell
.\gradle-8.5\bin\gradle.bat bootRun --args="--spring.profiles.active=postgres-local"
```

PostgreSQL の認証情報が未設定のローカル環境では、H2 を使う local プロファイルで起動できます。

```bash
./gradle-8.5/bin/gradle bootRun --args='--spring.profiles.active=local'
```

VS Code から前後端を一括で起動する場合は、`One Click Local Dev` タスクを実行してください。

PowerShell から一発で起動する場合は、以下を実行します。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local-dev.ps1 -OpenBrowser
```

Stripe の本地 webhook 联调は、先に local secrets に API Key / Price ID / Webhook Secret を入れたうえで、専用スクリプトから起動します。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-stripe-listener.ps1
```

Webhook secret だけ先に確認したい場合は以下です。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-stripe-listener.ps1 -PrintSecretOnly
```

`config/local-secrets.yml` には少なくとも以下を置きます。

```yml
stripe:
	api:
		key: sk_test_...
	webhook:
		secret: whsec_...
	checkout:
		price-id: price_...
```

Ngrok と Cloudflare を含めた真機联调用に 4 つの窗口をまとめて起動する場合は、项目根目录の `start-dev-env.bat` を実行します。

```bat
start-dev-env.bat
```

## LIFF 真機联调配置

前端 `gym24h-frontend/.env` には最低限以下を設定します。

```env
VITE_LIFF_ID=2009874045-xxxxxxxx
VITE_API_BASE_URL=https://your-backend.ngrok-free.app
```

- `VITE_LIFF_ID`: LINE Developers の LIFF ID。本物の LIFF ID を入れること。URL ではない。
- `VITE_API_BASE_URL`: 手机から到達できる后端公网地址。通常は ngrok の 8080 隧道 URL を使う。

后端侧は `LINE_CHANNEL_ID` を設定して、`/auth/line-login` の idToken 校验先 audience と一致させます。

```powershell
$env:LINE_CHANNEL_ID='2009874045'
```

真機联调前のチェックポイント:

- LIFF 控制台の endpoint URL が当前前端公网地址に一致していること
- 后端 ngrok 地址が `VITE_API_BASE_URL` と一致していること
- `LINE_CHANNEL_ID` が LIFF 所属チャネルの Channel ID と一致していること

## テスト

```bash
./gradlew test
```

## 主要方針

- コアドメイン層はフレームワーク非依存
- Stripe Webhook を決済状態のSSOTとして扱う前提
- UTC基準の期限判定と、入口側の最小限バリデーションを先に配置
- 永続化・外部連携は infrastructure 配下へ集約
