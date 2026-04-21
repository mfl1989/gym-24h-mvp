# gym-24h-mvp

24時間無人ジムSaaSのMVP骨格です。Spring Boot 3.x / Java 21 / Gradle を前提に、ドメイン・アプリケーション・インフラ・プレゼンテーションを分離した構成で立ち上げています。

## 起動

```bash
./gradlew bootRun
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
