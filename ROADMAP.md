Phase 1: 基础设施与核心地基 (Foundation) —— 📍 我们在这里
这一阶段的目标是把房子打好地基，确保最核心的业务规则没有外部依赖。

[x] 项目骨架搭建 (Spring Boot 3, Java 21, Gradle, DDD 目录结构)

[x] 数据库自动化初始化 (PostgreSQL, Flyway 脚本, 预留 sub_char 字段)

[x] 核心领域模型建立 (Pure Java 的 User, Subscription 实体)

[x] 核心业务规则引擎 (EntranceValidator 门禁鉴权与 ±10秒容差逻辑)

[x] 数据持久化桥梁 (RepositoryImpl 实现，打通 Domain 与 Spring Data JPA)

[x] 基础配置调通 (application.yml，确保应用能连上 DB 并成功启动)

Phase 2: 资金生命线 (Subscription & Payment)
这一阶段目标是跑通收钱逻辑，确保账目清晰，状态流转正确。

[ ] Stripe 基础集成 (Customer 创建, Checkout Session URL 生成)

[ ] Webhook 监听与幂等防御 (根据 event_id 确保不重复扣款)

[ ] 支付状态机流转 (通过 Domain Service 处理 ACTIVE / ARREARS 逻辑)

[ ] 账单流水落库 (Invoices 表更新，与 Stripe 交易对齐)

Phase 3: 核心体验流 (Entrance & IoT)
这一阶段目标是让用户爽快地扫码进门，同时保障硬件容灾。

[ ] 动态防伪凭证 (短时效、带签名的 QR Code 生成逻辑)

[ ] IoT 智能锁客户端 (对接 Akerun/RemoteLock API，带 3秒超时熔断)

[ ] 开门应用服务编排 (EntranceCommandService：鉴权 -> 调用硬件 -> 记日志)

[ ] 防抖与限流 (Redis 5秒开门防抖，防止用户狂点按钮)

[ ] 合规审计落地 (每一次开门动作异步写入 AuditLog)

Phase 4: 鉴权与前端交互层 (API & Security)
这一阶段目标是保护我们的接口，并给前端提供标准的数据。

[ ] LINE OpenID 登录集成 (获取用户唯一身份，发放 JWT)

[ ] 全局异常拦截 (GlobalExceptionHandler，隐藏堆栈，返回标准 JSON 错误码)

[ ] 链路追踪 (RequestIdFilter，为每个请求注入唯一 ID)

[ ] RESTful API 暴露 (编写 Controller 层，对接前端 LIFF)

Phase 5: 闭环与自动化运维 (Batch Jobs)
这一阶段目标是让系统能够真正 24 小时无人值守，自我修复。

[ ] 夜间状态盘点 Batch (扫描过期用户，变更状态为 EXPIRED)

[ ] Stripe 对账补偿机制 (防止 Webhook 丢包导致的“付了钱进不去”问题)