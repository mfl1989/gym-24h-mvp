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

## テスト

```bash
./gradlew test
```

## 主要方針

- コアドメイン層はフレームワーク非依存
- Stripe Webhook を決済状態のSSOTとして扱う前提
- UTC基準の期限判定と、入口側の最小限バリデーションを先に配置
- 永続化・外部連携は infrastructure 配下へ集約
