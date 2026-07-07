# 🏫 AI Smart Campus - 智慧园区

基于 **SpringBoot 3.2 + SpringAI** 构建的 AI 智慧园区后端服务，对接大模型实现园区智能化管理，配套微信小程序前端。

## 🎯 项目概述

AI 智慧园区是一个面向校园/园区的综合服务平台，将 AI 大模型能力与园区管理场景深度融合，提供智能对话、社团推荐、公告管理、语义搜索等核心功能。

## ✨ 核心功能

### 🤖 AI 智能对话
- **流式对话**（SSE）：与大模型实时交互
- **RAG 检索增强**：结合园区知识库精准问答
- **Function Calling**：通过对话直接完成报名、查询等操作
- **多轮对话管理**：会话历史持久化

### 📢 园区墙 / 公告管理
- 公告分类管理（校园新闻、学术讲座、社团活动等）
- 文档上传支持（PDF/TXT/Markdown）
- AI 智能摘要生成

### 🎪 社团推荐系统
- **基于标签的 Jaccard 相似度推荐**
- **LLM 大模型精排推荐**
- 社团详情与在线报名

### 🔐 用户认证系统
- JWT Token 鉴权
- 登录/修改密码
- 首次登录引导

### 🔍 语义搜索
- **Embedding 向量检索**（基于硅基流动 Qwen3-Embedding）
- **Redis Stack 向量存储**（HNSW 索引）
- **MySQL 关键词全文检索**（双路召回）

## 🏗 技术架构

```
ai-smart-campus/
├── campus-common/       — 公共模块（DTO、异常、枚举）
├── campus-core/         — 核心业务模块
│   ├── chat/            — AI 对话（ChatClient + SSE + RAG）
│   ├── search/          — 语义搜索（Embedding + MySQL）
│   ├── club/            — 社团推荐（Jaccard + LLM 重排）
│   ├── announcement/    — 园区墙 / 公告管理
│   ├── ai/              — Function Calling
│   ├── vector/          — Redis Stack 向量存储
│   ├── mq/              — RabbitMQ 异步向量同步
│   ├── auth/            — JWT 认证拦截
│   ├── knowledge/       — 知识库管理
│   └── controller/      — REST API 端点
├── sql/                 — 数据库初始化脚本
├── miniprogram/         — 微信小程序前端
└── docs/                — 设计文档
```

## 🛠 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| SpringBoot | 3.2.6 | 应用框架 |
| SpringAI | 1.0.0-M4 | AI 模型集成 |
| Mybatis-Plus | 3.5.7 | ORM |
| MySQL | 8.0+ | 主数据库 |
| Redis Stack | 7.2+ | 缓存 + 向量存储 |
| RabbitMQ | 3.12+ | 异步消息 |
| MinIO | latest | 对象存储 |
| DeepSeek API | — | AI 对话模型 |
| 硅基流动 API | — | Embedding 模型 |

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis Stack 7.2+
- RabbitMQ 3.12+
- MinIO

### 启动步骤

```bash
# 1. 设置环境变量（将 YOUR_KEY 替换为真实的 API Key）
export AI_API_KEY=YOUR_DEEPSEEK_API_KEY
export AI_BASE_URL=https://api.deepseek.com
export SILICONFLOW_API_KEY=YOUR_SILICONFLOW_API_KEY

# 2. 编译
mvn clean compile

# 3. 启动
mvn spring-boot:run -pl campus-core
```

### 环境变量

| 变量 | 必填 | 说明 |
|------|------|------|
| `AI_API_KEY` | ✅ | DeepSeek / AI 模型 API Key |
| `AI_BASE_URL` | — | AI 接口地址（默认 DeepSeek） |
| `SILICONFLOW_API_KEY` | Embedding 需要 | 硅基流动 API Key |
| `MYSQL_USER` | ✅ | 数据库用户名 |
| `MYSQL_PASSWORD` | ✅ | 数据库密码 |
| `REDIS_HOST` | — | Redis 地址（默认 localhost） |
| `REDIS_PORT` | — | Redis 端口（默认 6380） |

## 📡 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/change-password` | 修改密码 |
| POST | `/api/chat/send` | SSE 流式对话 |
| POST | `/api/chat/send-rag` | RAG 检索增强问答 |
| POST | `/api/search` | 语义搜索 |
| GET | `/api/club/recommend/ai` | LLM 精排推荐 |
| POST | `/api/announcements/upload` | 文档上传 |
| ... | ... | ... |

完整接口文档启动后访问 `http://localhost:8080/doc.html`（Knife4j）。

## 📱 小程序

配套微信小程序源码位于 `miniprogram/` 目录，使用微信开发者工具打开即可运行。

## 📄 开源协议

本项目仅用于学习交流。
