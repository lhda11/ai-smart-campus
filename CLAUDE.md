# AI Smart Campus - 智慧园区

AI智慧园区后端服务，基于SpringBoot + SpringAI构建，对接大模型实现园区智能化。

## Quick Start

```bash
cd ai-smart-campus
mvn clean compile

# 中间件要求
# MySQL 8.0+     — docker/本地，端口 3306
# Redis Stack    — docker redis-stack，端口 6380
# RabbitMQ       — docker，端口 5672
# MinIO          — docker，端口 9001

# 设置环境变量
export AI_API_KEY=你的DeepSeek_API_Key
export AI_BASE_URL=https://api.deepseek.com
export SILICONFLOW_API_KEY=你的硅基流动_API_Key

# 启动
mvn spring-boot:run -pl campus-core
# 或
mvn clean package -DskipTests
java -jar campus-core/target/campus-core-1.0.0-SNAPSHOT.jar
```

## Architecture

```
ai-smart-campus/
├── campus-common/     — DTOs、异常、枚举
├── campus-core/       — 主业务模块
│   ├── chat/          — AI对话 (ChatClient + SSE + RAG)
│   ├── search/        — 资料检索 (Embedding + MySQL)
│   ├── club/          — 社团推荐 (Jaccard + LLM重排)
│   ├── announcement/  — 园区墙/公告管理
│   ├── ai/            — Function Calling (CampusFunctions)
│   ├── vector/        — Redis Stack向量存储
│   ├── mq/            — RabbitMQ异步向量同步
│   ├── config/        — Spring Bean配置
│   └── controller/    — REST API 端点
├── sql/               — 数据库初始化脚本
├── docs/plans/        — 实施计划文档
└── PRD.md             — 产品需求文档
```

## API Endpoints

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/chat/send | SSE流式对话 |
| POST | /api/chat/send-sync | 多轮对话 + Function Calling |
| POST | /api/chat/send-test | 基础对话（无 Tools） |
| POST | /api/chat/send-rag | RAG检索增强问答 |
| GET  | /api/chat/conversations | 会话列表 |
| GET  | /api/chat/messages?convId= | 消息历史 |
| DELETE | /api/chat/conversations/{id} | 删除会话 |
| POST | /api/search | 语义搜索 |
| POST | /api/announcements/upload | 文档上传 |
| GET  | /api/announcements?category= | 分类公告 |
| DELETE | /api/announcements/{id} | 删除公告 |
| GET  | /api/club/recommend?tags= | 标签推荐 |
| GET  | /api/club/recommend/ai?tags= | LLM精排推荐 |
| GET  | /api/club | 社团列表 |
| POST | /api/apply | 报名 |
| GET  | /api/apply/my | 我的报名 |
| POST | /api/admin/rebuild-index | 重建向量索引 |
| GET  | /doc.html | Knife4j API 文档 |

## AI Models

- **Chat**: DeepSeek `deepseek-v4-flash` (OpenAI 兼容)
- **Embedding**: 硅基流动 `Qwen/Qwen3-Embedding-8B` (4096 维)
- **Vector Store**: Redis Stack (RediSearch HNSW)

## Tech Stack

| 组件 | 版本 |
|---|---|
| SpringBoot | 3.2.6 |
| SpringAI | 1.0.0-M4 |
| Mybatis-Plus | 3.5.7 |
| MySQL | 8.0+ |
| Redis Stack | 7.2+ |
| RabbitMQ | 3.12+ |
| MinIO | latest |
| DeepSeek API | — |
| 硅基流动 API | — |

## Tests

```bash
# 设置 API Key 后运行
export AI_API_KEY=sk-xxx
mvn test -pl campus-core
# Tests: 11, Failures: 0, Errors: 0
```

## Env Variables

| 变量 | 必填 | 默认值 |
|---|---|---|
| AI_API_KEY | Chat 需要 | sk-demo-key |
| AI_BASE_URL | Chat 需要 | https://api.deepseek.com |
| SILICONFLOW_API_KEY | Embedding 需要 | — |
| MYSQL_USER / MYSQL_PASSWORD | ✅ | root / 123456 |
| REDIS_HOST / REDIS_PORT | — | localhost / 6380 |
| RABBITMQ_HOST / RABBITMQ_PORT | — | localhost / 5672 |
| MINIO_ENDPOINT / _ACCESS_KEY / _SECRET_KEY | — | localhost:9001 |
