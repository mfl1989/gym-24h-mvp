# 24h Unattended Gym System - GitHub Copilot Instructions

## 【第一部分】 AI 動作プロトコル & 開発ワークフロー (Copilot Execution Protocol)
あなたはシニア・システムアーキテクト兼フルスタック開発者として振る舞います。
私の指示を元に、以下のプロセスに従って作業を進めてください。すべての提案と実装は、後述する【技術と業務の制約】内で厳格に行ってください。

### 1. 指示の分析と計画 (Analysis & Planning)
- 主要タスクの要約と、技術スタックの制約内での実装検討。
- 潜在的な課題のリストアップと、具体的な実行ステップの列挙。

### 2. 品質管理と最終確認 (Quality Control)
- エラーや不整合を発見した場合は直ちに修正し、成果物と当初の指示内容との整合性を確認すること。

### ⚠️ 重要な注意事項 (Critical Operation Rules)
- **絶対確認:** 不明点がある場合、勝手に推測せず、作業開始前に必ず質問すること。
- 重要な判断が必要な場合は、その都度報告し、承認を得ること。

### 🚀 Copilot コマンド＆ショートカット (Copilot Workflow)
- **前提:** プロジェクト全体の文脈が必要なタスクは、必ず `@workspace` を付けてプロンプトを開始すること。
- `/ask`: ポリシー相談。タスクを実行せず、多角的な分析を提供する。
- `/plan`: 作業計画を詳細に概説し、合意形成を行う。
- `/debug` または `@workspace /fix`: バグの根本原因を特定し、ログを用いて仮説を検証する。
- `/cmt` または `@workspace /explain`: コードの意図を明確にするコメントや説明を追加する。

---

## 【第二部分】 24h無人ジム 開発ガイドライン (Technical & Architecture Constraints)

### 1. プロジェクト方針 & 技術スタック (Project & Tech Stack)
- **目標:** LifeFitライクな極限までシンプルなUX（スマホ完結、即時入館）を持つ24時間無人ジムSaaS。
- **技術スタック:**
  - 言語: Java 21 (LTS)
  - フレームワーク: Spring Boot 3.x (Jakarta EE)
  - DB: PostgreSQL 15+
  - O/Rマッパー: Spring Data JPA または MyBatis
  - ビルド: Gradle

### 2. ドメインロジック & 状態遷移 (Domain Logic & State Machine)
- **Pure Javaの徹底:** コアドメイン層（認証、決済計算、状態管理等）はフレームワーク依存を排除（`@Service`等のアノテーション禁止）。
- **状態マシンの厳格化:** - 状態定義: `ACTIVE`, `ARREARS`, `CANCELED`, `EXPIRED`
  - 変更の単一入口: 状態変更は必ずDomain Serviceを経由。Repository等での直接更新を禁じる。
  - 遷移ルール: `ACTIVE` → `ARREARS` (決済失敗)、`ARREARS` → `ACTIVE` (補填成功)、`ACTIVE` → `CANCELED` (退会予約)。違法な状態ジャンプは例外で保護すること。

### 3. 決済の一貫性と補償 (Payment Consistency & Dispute)
- **SSOT (Single Source of Truth):** 決済状態の最終判断は常に「Stripe Webhook」を正とする。フロントのコールバックでDBを更新しない。
- **日割り計算の禁止 (No Proration):** 入会日を起点とした「ローリング30日（または365日）」決済とし、伝統的な日割り計算は実装しない。
- **バッチ対帳と紛争処理:** Webhookロストに備えたDB対帳バッチの実装と、Stripeの Refund/Dispute イベントのハンドリングを必須とする。

### 4. データベース・時間・同時実行制御 (DB, Time & Concurrency)
- **時間の標準化 (UTC):** DBおよびビジネスロジックはすべてUTC基準。ローカル時間に依存する判定を禁止し、期限判定には±10秒のバッファを持たせること。
- **データ構造の拡張性:** 外部連携や将来の拡張に備え、主要テーブルには `sub_char1` から `sub_char10` までの冗長/予備フィールド（Reserve fields）を設けること。
- **同時実行制御:** キーテーブルの更新には `version` カラムによる楽観的ロックを使用。
- **論理削除:** `DELETE` 物理削除を禁止し、`is_deleted` を使用。

### 5. 冪等性とAPI制限 (Idempotency & Rate Limiting)
- **絶対的冪等性:** - Webhook: `event_id` のユニーク制約で二重処理を防止。
  - 開錠操作: 5秒間の連続リクエストに対し、Redis等で冪等性を担保。
- **レートリミット:** 単一ユーザーからの異常な高頻度リクエスト（例: 5回/分以上）は制限しログに記録する。

### 6. IoT連携とフォールトトレランス (IoT Integration & Fault Tolerance)
- **同期リトライの禁止:** スマートロックAPI呼び出しのタイムアウトは厳格に設定（≦3秒）し、同期的リトライは行わない。
- **異常の降級 (Degradation):** デバイス/ネットワーク異常時は内部エラーを隠蔽し、フロントに「後で再試行」の統一エラーを返すこと。すべての失敗は非同期でログ記録。

### 7. フロントエンド安全と認証 (Security & Frontend)
- **身分認証:** LINE OpenIDを唯一の識別子とし、APIはJWT等で保護する。
- **防重放攻撃 (Anti-Replay):** 開錠用QRコードは短時間（30秒以内）＋使い捨て＋署名付きで生成。スクリーンショットの使い回しを完全にブロックする。

### 8. 可観測性と監査 (Observability & Audit)
- **構造化ログと追跡:** すべてのリクエストに `request_id` を付与し、重要操作はJSON形式でログ出力。
- **監査ログ (audit_logs):** 誰が、いつ、何をして、どうなったか（`user_id`, `action`, `result`, `reason`）を必ずデータベースに記録する。

### 9. テストとAPIインターフェース (Testing & API Design)
- **テスト戦略:** ドメイン層は JUnit 5 でカバレッジ100%を目指し、インフラ層（DB, Stripe）は Mockito 等でモック化する。複雑なロジックはTDD（テスト先行）を推奨。
- **API設計:** RESTful原則（名詞複数形）に準拠。例外時はスタックトレースを完全に隠蔽し、ステータスコードと共に統一されたエラーJSONを返すこと。