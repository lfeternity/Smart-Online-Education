# Tianji Agent Service

天机学堂独立学习 Agent，基于 Spring Boot 3.5、LangChain4j 1.19 和 Java 17。服务提供流式对话、会话记忆、RAG、Function Calling、受控业务 Tools、写操作确认、知识摄取、字幕/ASR、JWT/JWKS、分布式限流和模型治理。

## 本地运行

默认使用内存 H2、本地记忆和不调用真实模型的降级回答：

```powershell
cd tianji-agent
mvn spring-boot:run
```

服务监听 `8094`，健康检查为 `GET /api/v1/health`，Actuator 健康检查为 `GET /actuator/health`。

## Chat 模型协议

Chat 模型支持两种 OpenAI-compatible 流式协议：

| `AGENT_AI_PROTOCOL` | 请求路径 | 适用端点 |
|---|---|---|
| `CHAT_COMPLETIONS` | `/chat/completions` | OpenAI Chat Completions 兼容服务 |
| `RESPONSES` | `/responses` | OpenAI Responses API 兼容服务 |

Base URL 应配置为 API 根地址，通常以 `/v1` 结尾。代码不会写死供应商域名，也不会把 API Key 写入日志。

```powershell
$env:AGENT_AI_ENABLED="true"
$env:AGENT_AI_PROTOCOL="RESPONSES" # 或 CHAT_COMPLETIONS
$env:AGENT_AI_BASE_URL="https://provider.example/v1"
$env:AGENT_AI_API_KEY="your-chat-key"
$env:AGENT_AI_CHAT_MODEL="your-chat-model"
mvn spring-boot:run
```

## 独立模型端点

Chat、Embedding、ASR 和 Rerank 可以使用完全独立的地址、API Key 和模型。空的 Embedding 地址/Key 会回退到 Chat 配置；ASR Key 为空时也会回退到 Chat Key；Rerank 必须显式配置。

| 能力 | 主要配置 |
|---|---|
| Chat | `AGENT_AI_PROTOCOL`、`AGENT_AI_BASE_URL`、`AGENT_AI_API_KEY`、`AGENT_AI_CHAT_MODEL` |
| Embedding | `AGENT_AI_EMBEDDING_PROVIDER`、`AGENT_AI_EMBEDDING_BASE_URL`、`AGENT_AI_EMBEDDING_API_KEY`、`AGENT_AI_EMBEDDING_MODEL` |
| ASR | `AGENT_ASR_ENABLED`、`AGENT_ASR_BASE_URL`、`AGENT_ASR_ENDPOINT`、`AGENT_ASR_API_KEY`、`AGENT_ASR_MODEL` |
| Rerank | `AGENT_RERANK_ENABLED`、`AGENT_RERANK_BASE_URL`、`AGENT_RERANK_ENDPOINT`、`AGENT_RERANK_API_KEY`、`AGENT_RERANK_MODEL` |

示例：

```powershell
$env:AGENT_AI_EMBEDDING_PROVIDER="OPENAI"
$env:AGENT_AI_EMBEDDING_BASE_URL="https://embedding.example/v1"
$env:AGENT_AI_EMBEDDING_API_KEY="your-embedding-key"
$env:AGENT_AI_EMBEDDING_MODEL="text-embedding-model"

$env:AGENT_ASR_ENABLED="true"
$env:AGENT_ASR_BASE_URL="https://asr.example/v1"
$env:AGENT_ASR_ENDPOINT="/audio/transcriptions"
$env:AGENT_ASR_API_KEY="your-asr-key"
$env:AGENT_ASR_MODEL="whisper-1"

$env:AGENT_RERANK_ENABLED="true"
$env:AGENT_RERANK_BASE_URL="https://rerank.example/v1"
$env:AGENT_RERANK_ENDPOINT="/rerank"
$env:AGENT_RERANK_API_KEY="your-rerank-key"
$env:AGENT_RERANK_MODEL="rerank-multilingual-v3.0"
```

## 主备模型与熔断

可为备用 Chat 模型配置独立协议、地址和 Key。主模型在尚未输出内容或 Tool 调用前失败时才会切换，避免有副作用的请求被重复执行。

```powershell
$env:AGENT_AI_FALLBACK_ENABLED="true"
$env:AGENT_AI_FALLBACK_PROTOCOL="CHAT_COMPLETIONS"
$env:AGENT_AI_FALLBACK_BASE_URL="https://fallback.example/v1"
$env:AGENT_AI_FALLBACK_API_KEY="your-fallback-key"
$env:AGENT_AI_FALLBACK_CHAT_MODEL="fallback-model"
$env:AGENT_AI_CIRCUIT_FAILURE_THRESHOLD="3"
$env:AGENT_AI_CIRCUIT_OPEN_DURATION="30s"
```

## 生产依赖与安全

`docker compose up -d` 可启动独立开发依赖。接入现有天机环境时使用 `prod` Profile，并配置 MySQL、Redis、RabbitMQ、业务服务和 Nacos：

```text
SPRING_PROFILES_ACTIVE=prod
AGENT_DB_URL / AGENT_DB_USERNAME / AGENT_DB_PASSWORD
REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
RABBITMQ_HOST / RABBITMQ_PORT / RABBITMQ_USERNAME / RABBITMQ_PASSWORD
COURSE_SERVICE_URL / LEARNING_SERVICE_URL / EXAM_SERVICE_URL / SEARCH_SERVICE_URL
AGENT_NACOS_ENABLED / NACOS_SERVER_ADDR / AGENT_NACOS_SERVICE_NAME
```

生产建议启用 Agent 二次 JWT 校验：

```text
AGENT_JWT_ENABLED=true
AGENT_JWT_JWK_SET_URI=http://auth-service:8081/jwks
AGENT_JWT_ISSUER_URI=             # 可选
AGENT_JWT_AUDIENCE=               # 可选
AGENT_ADMIN_USER_IDS=             # 逗号分隔
AGENT_ADMIN_ROLE_IDS=             # 逗号分隔
AGENT_TEACHER_ROLE_IDS=           # 逗号分隔
```

Gateway 必须先删除客户端传入的 `user-info` 和 `role-info`。启用 JWT 后，Agent 会从已验证 Token 的 `user.userId/user.roleId` claim 重建可信身份。所有 Key 和密码必须通过环境变量或 Secret 管理，不得提交到 Git。

## RAG、限流与知识任务

- `AGENT_QDRANT_ENABLED=true` 启用 Embedding + Qdrant 稠密召回；关闭或故障时自动使用本地关键词检索。
- `AGENT_AI_EMBEDDING_PROVIDER=LOCAL_HASH` 使用内置低内存 1536 维哈希向量，不依赖外部 Embedding 服务；它适合当前测试环境，语义质量低于专业 Embedding 模型。获得可用端点后改为 `OPENAI` 即可。
- `LOCAL_HASH` 建议将 `AGENT_RETRIEVAL_MIN_SCORE` 设为 `0.05`；切换专业 Embedding 模型时应重新评测阈值并重建 Qdrant collection/索引。
- 检索会融合稠密与关键词结果并执行 RRF；`AGENT_RERANK_ENABLED=true` 后再调用外部 Rerank。
- `AGENT_REDIS_MEMORY_ENABLED=true` 启用 Redis 短期记忆。
- `AGENT_LIMITS_REDIS_ENABLED=true` 启用跨实例请求、并发、Token 和成本配额。
- 知识发布是异步任务，支持状态查询、指数退避、死信和人工重试。
- 字幕支持 JSON 时间轴和 SRT/VTT/TXT；音频转写使用 OpenAI-compatible multipart ASR 接口。

Flyway 脚本位于 `src/main/resources/db/migration`。生产 Profile 使用 `ddl-auto=validate`，实体字段与 MySQL `LONGTEXT` schema 会在启动时校验。

## 主要接口

```text
POST   /api/v1/conversations
GET    /api/v1/conversations
GET    /api/v1/conversations/{id}/messages
DELETE /api/v1/conversations/{id}
POST   /api/v1/conversations/{id}/messages:stream
POST   /api/v1/actions/{id}/confirm
POST   /api/v1/actions/{id}/cancel
POST   /api/v1/messages/{id}/feedback
GET    /api/v1/profile
PUT    /api/v1/profile
DELETE /api/v1/profile
POST   /api/v1/admin/knowledge/documents
POST   /api/v1/admin/knowledge/documents/{id}:publish
POST   /api/v1/admin/knowledge/documents/{id}:reindex
GET    /api/v1/admin/knowledge/jobs/{id}
POST   /api/v1/admin/knowledge/jobs/{id}:retry
POST   /api/v1/admin/knowledge/transcripts/subtitles:upload
POST   /api/v1/admin/knowledge/transcripts/audio:transcribe
GET/POST /api/v1/admin/prompts
POST   /api/v1/admin/prompts/{id}:publish
```

## 验证与当前部署状态

截至 2026-08-30：

- `mvn -q test`：共 31 项测试，30 项通过、0 失败、0 错误；1 项 Testcontainers 测试因本机无 Docker 跳过。
- Chat Completions 和 Responses 真实端点均已验证。
- Responses + RAG + Tool + SSE 引用链路及 Prepare/Confirm 已通过真实模型联调；标准 `Bearer <token>` 经 Gateway 的浏览器链路已复测通过。
- Agent、认证服务、Gateway 和学员端已部署到 `192.168.150.101`；Agent 监听 `8094` 并在 Nacos 注册健康实例。
- 当前 Agent 镜像为 `tj-agent:codex-20260830-v3-history-qdrant`，运行配置为 `AGENT_AI_EMBEDDING_PROVIDER=LOCAL_HASH`、`AGENT_QDRANT_ENABLED=true`、`AGENT_RETRIEVAL_MIN_SCORE=0.05`。
- 旧 Gateway 发现客户端无法消费 Agent 的 Nacos 实例推送，当前使用 `AGENT_SERVICE_URI=http://tj-agent:8094` 回退；不影响已验证的 Gateway 调用。
- 测试供应商的 `/v1/embeddings` 返回 HTTP 503，且模型列表没有 Embedding 模型；服务已增加 `LOCAL_HASH` Provider，使 Qdrant 可以在不增加大型模型内存占用的情况下完成实际向量写入和召回。
- 已通过正式知识 API 发布 2 份现有课程大纲，Qdrant collection 为 1536 维、2 个 point；真实 Agent 请求产生 `points/search` 200、Tool 事件和 citation。
- 学员端已用真实浏览器验证全局/学习页对话记录列表、会话切换、消息与引用恢复；删除与新建使用同一组已验证 API。
- ASR 和 Rerank 代码已实现并有 Mock 测试；由于没有可用服务端点，当前环境未启用。

完整建设状态和剩余运维工作见 `../docs/ai-agent-langchain4j-long-term-plan.md`。
