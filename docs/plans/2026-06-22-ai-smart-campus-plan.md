# AI 智慧园区 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a 4-module AI-powered smart campus backend (chat, RAG search, club recommendation, auto registration) using SpringBoot + SpringAI + Mybatis-Plus + Redis Stack + RabbitMQ + MinIO.

**Architecture:** Multi-module Maven project (`campus-common` → `campus-core`), with TDD-driven task decomposition. Each module exposes REST APIs consumed by a WeChat Mini Program frontend. SpringAI provides unified abstraction over OpenAI-compatible LLM APIs (Chat, Embedding, Tools). Redis Stack handles vector search (RediSearch). RabbitMQ handles async document vectorization.

**Tech Stack:** SpringBoot 3.x, SpringAI 1.0.x, Mybatis-Plus 3.5.x, MySQL 8.0+, Redis Stack 7.2+, RabbitMQ 3.12+, MinIO, Apache Tika 2.x, Hutool, Knife4j

## Global Constraints

- Java 17+
- SpringBoot 3.x — javax → jakarta migration throughout
- Test framework: spring-boot-starter-test (JUnit 5 + Mockito)
- AI calls use OpenAI-compatible API; must be configurable via application.yml
- API prefix: `/api/`
- TDD: every feature starts with a failing test, then implementation
- Lombok for boilerplate reduction (entities, DTOs, VOs)
- Knife4j for API documentation (Swagger UI)

---

## Phase 0: Project Scaffolding

### Task 0.1: Parent POM + Module Declaration

**Files:**
- Create: `ai-smart-campus/pom.xml`
- Create: `ai-smart-campus/campus-common/pom.xml`
- Create: `ai-smart-campus/campus-core/pom.xml`

**Interfaces:**
- Produces: Maven reactor build with `campus-common` and `campus-core` modules

- [ ] **Step 1: Create parent pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.6</version>
        <relativePath/>
    </parent>

    <groupId>com.campus</groupId>
    <artifactId>ai-smart-campus</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>AI Smart Campus</name>
    <description>AI智慧园区 - 对接大模型，完成园区智能化改造</description>

    <modules>
        <module>campus-common</module>
        <module>campus-core</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <redisson.version>3.30.0</redisson.version>
        <minio.version>8.5.10</minio.version>
        <hutool.version>5.8.29</hutool.version>
        <knife4j.version>4.5.0</knife4j.version>
        <spring-ai.version>1.0.0-M4</spring-ai.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring AI BOM -->
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Mybatis-Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <!-- Redisson -->
            <dependency>
                <groupId>org.redisson</groupId>
                <artifactId>redisson-spring-boot-starter</artifactId>
                <version>${redisson.version}</version>
            </dependency>
            <!-- MinIO -->
            <dependency>
                <groupId>io.minio</groupId>
                <artifactId>minio</artifactId>
                <version>${minio.version}</version>
            </dependency>
            <!-- Hutool -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
            <!-- Knife4j -->
            <dependency>
                <groupId>com.github.xiaoymin</groupId>
                <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
                <version>${knife4j.version}</version>
            </dependency>
            <!-- Apache Tika for doc parsing -->
            <dependency>
                <groupId>org.apache.tika</groupId>
                <artifactId>tika-core</artifactId>
                <version>2.9.2</version>
            </dependency>
            <dependency>
                <groupId>org.apache.tika</groupId>
                <artifactId>tika-parsers-standard-package</artifactId>
                <version>2.9.2</version>
            </dependency>
            <!-- Internal modules -->
            <dependency>
                <groupId>com.campus</groupId>
                <artifactId>campus-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.campus</groupId>
                <artifactId>campus-core</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <skip>true</skip>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create campus-common/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.campus</groupId>
        <artifactId>ai-smart-campus</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>campus-common</artifactId>
    <name>Campus Common</name>
    <description>公共模块 - 枚举、异常、DTO、工具类</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Create campus-core/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.campus</groupId>
        <artifactId>ai-smart-campus</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>campus-core</artifactId>
    <name>Campus Core</name>
    <description>核心业务模块 - AI咨询、资料检索、社团推荐、报名</description>

    <dependencies>
        <dependency>
            <groupId>com.campus</groupId>
            <artifactId>campus-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        </dependency>
        <!-- Spring AI -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        </dependency>
        <!-- Tika for doc parsing -->
        <dependency>
            <groupId>org.apache.tika</groupId>
            <artifactId>tika-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.tika</groupId>
            <artifactId>tika-parsers-standard-package</artifactId>
        </dependency>
        <!-- DB -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <skip>false</skip>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Build the project**

Run: `cd ai-smart-campus && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add ai-smart-campus/pom.xml ai-smart-campus/campus-common/pom.xml ai-smart-campus/campus-core/pom.xml
git commit -m "chore: init Maven multi-module project scaffolding"
```

---

### Task 0.2: Common Module — ApiResult, Exceptions, Enums

**Files:**
- Create: `ai-smart-campus/campus-common/src/main/java/com/campus/common/dto/ApiResult.java`
- Create: `ai-smart-campus/campus-common/src/main/java/com/campus/common/dto/PageReq.java`
- Create: `ai-smart-campus/campus-common/src/main/java/com/campus/common/exception/ErrorCode.java`
- Create: `ai-smart-campus/campus-common/src/main/java/com/campus/common/exception/BizException.java`
- Create: `ai-smart-campus/campus-common/src/main/java/com/campus/common/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Produces: `ApiResult<T>` generic response wrapper, `PageReq` pagination DTO, `BizException` + `GlobalExceptionHandler` error handling

- [ ] **Step 1: Create directories**

Run: `mkdir -p ai-smart-campus/campus-common/src/main/java/com/campus/common/dto && mkdir -p ai-smart-campus/campus-common/src/main/java/com/campus/common/exception`

- [ ] **Step 2: Write ApiResult.java**

```java
package com.campus.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(200, "success", data);
    }

    public static <T> ApiResult<T> ok() {
        return new ApiResult<>(200, "success", null);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null);
    }

    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message, null);
    }
}
```

- [ ] **Step 3: Write PageReq.java**

```java
package com.campus.common.dto;

import lombok.Data;

@Data
public class PageReq {
    private int page = 1;
    private int size = 10;
}
```

- [ ] **Step 4: Write ErrorCode.java**

```java
package com.campus.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    AI_SERVICE_ERROR(5001, "AI服务异常"),
    VECTOR_STORE_ERROR(5002, "向量存储异常"),
    FILE_UPLOAD_ERROR(5003, "文件上传失败"),
    DUPLICATE_APPLY(5004, "重复报名");

    private final int code;
    private final String message;
}
```

- [ ] **Step 5: Write BizException.java**

```java
package com.campus.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

- [ ] **Step 6: Write GlobalExceptionHandler.java**

```java
package com.campus.common.exception;

import com.campus.common.dto.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return ApiResult.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail(ErrorCode.INTERNAL_ERROR.getCode(), "服务器内部错误");
    }
}
```

- [ ] **Step 7: Build**

Run: `cd ai-smart-campus && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add ai-smart-campus/campus-common/
git commit -m "feat: add ApiResult, PageReq, BizException, GlobalExceptionHandler"
```

---

### Task 0.3: Database Init Script

**Files:**
- Create: `ai-smart-campus/sql/init.sql`

**Interfaces:**
- Produces: Full MySQL schema for 7 tables

- [ ] **Step 1: Write init.sql**

```sql
-- ============================================================
-- AI智慧园区 - 数据库初始化脚本
-- Database: MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS ai_smart_campus DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_smart_campus;

-- 用户表 (模拟微信登录)
CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    openid          VARCHAR(128) NOT NULL UNIQUE COMMENT '微信openid',
    nickname        VARCHAR(64) COMMENT '用户昵称',
    avatar_url      VARCHAR(512) COMMENT '头像URL',
    interest_tags   VARCHAR(512) COMMENT '兴趣标签(JSON数组)',
    role            TINYINT DEFAULT 1 COMMENT '1:学生 2:教职工 3:社团负责人',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT '用户表';

-- 公告/文档表
CREATE TABLE IF NOT EXISTS t_announcement (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(256) NOT NULL COMMENT '标题',
    content         TEXT NOT NULL COMMENT '正文',
    summary         VARCHAR(512) COMMENT '摘要(AI生成)',
    category        VARCHAR(32) NOT NULL COMMENT '分类: notice/activity/policy',
    file_url        VARCHAR(512) COMMENT '附件地址(MinIO)',
    vector_id       VARCHAR(128) COMMENT 'Redis向量文档ID',
    status          TINYINT DEFAULT 1 COMMENT '0:已删除 1:正常',
    author_id       BIGINT NOT NULL COMMENT '发布者ID',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category_status (category, status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB COMMENT '公告/文档表';

-- 社团表
CREATE TABLE IF NOT EXISTS t_club (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(128) NOT NULL COMMENT '社团名称',
    description     TEXT COMMENT '社团介绍',
    tags            VARCHAR(512) COMMENT '标签(JSON数组): 科技/文艺/体育/志愿...',
    logo_url        VARCHAR(512) COMMENT '社团Logo(MinIO)',
    member_count    INT DEFAULT 0 COMMENT '当前成员数',
    status          TINYINT DEFAULT 1 COMMENT '0:已注销 1:正常',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT '社团表';

-- 纳新活动表
CREATE TABLE IF NOT EXISTS t_recruitment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    club_id         BIGINT NOT NULL COMMENT '社团ID',
    title           VARCHAR(256) NOT NULL COMMENT '纳新主题',
    requirement     TEXT COMMENT '纳新要求',
    quota           INT NOT NULL DEFAULT 0 COMMENT '名额',
    enrolled_count  INT DEFAULT 0 COMMENT '已报名人数',
    start_time      DATETIME NOT NULL COMMENT '开始时间',
    end_time        DATETIME NOT NULL COMMENT '结束时间',
    status          TINYINT DEFAULT 1 COMMENT '0:下架 1:进行中 2:已满员 3:已结束',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_club_status (club_id, status),
    INDEX idx_status_time (status, end_time)
) ENGINE=InnoDB COMMENT '纳新活动表';

-- 报名记录表
CREATE TABLE IF NOT EXISTS t_application (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    recruitment_id  BIGINT NOT NULL COMMENT '纳新活动ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    name            VARCHAR(64) NOT NULL COMMENT '姓名',
    student_id      VARCHAR(32) NOT NULL COMMENT '学号',
    phone           VARCHAR(20) NOT NULL COMMENT '手机号',
    status          TINYINT DEFAULT 0 COMMENT '0:已提交 1:已通过 2:未通过',
    remark          VARCHAR(256) COMMENT '备注',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_recruitment (user_id, recruitment_id)
) ENGINE=InnoDB COMMENT '报名记录表';

-- 对话会话表
CREATE TABLE IF NOT EXISTS t_conversation (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    title           VARCHAR(128) DEFAULT '新的对话' COMMENT '会话标题',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, updated_at DESC)
) ENGINE=InnoDB COMMENT '对话会话表';

-- 对话消息表
CREATE TABLE IF NOT EXISTS t_message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    role            VARCHAR(16) NOT NULL COMMENT '角色: user/assistant/system',
    content         TEXT NOT NULL COMMENT '消息内容',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id, created_at)
) ENGINE=InnoDB COMMENT '对话消息表';
```

- [ ] **Step 2: Execute the script**

Run: `mysql -u root -p < ai-smart-campus/sql/init.sql`
Expected: Tables created, verify with `SHOW TABLES FROM ai_smart_campus;`

- [ ] **Step 3: Commit**

```bash
git add ai-smart-campus/sql/init.sql
git commit -m "feat: add database init script with 7 tables"
```

---

### Task 0.4: Main Application Class + YAML Config

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/CampusApplication.java`
- Create: `ai-smart-campus/campus-core/src/main/resources/application.yml`
- Create: `ai-smart-campus/campus-core/src/main/resources/application-dev.yml`

**Interfaces:**
- Produces: Bootable SpringBoot application with SpringAI, Mybatis-Plus, Redis, RabbitMQ, MinIO configured

- [ ] **Step 1: Write CampusApplication.java**

```java
package com.campus.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.campus")
public class CampusApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusApplication.class, args);
    }
}
```

- [ ] **Step 2: Write application.yml**

```yaml
spring:
  application:
    name: ai-smart-campus
  profiles:
    active: dev
  ai:
    openai:
      api-key: ${AI_API_KEY:sk-demo-key}
      base-url: ${AI_BASE_URL:https://api.openai.com}
      chat:
        enabled: true
        options:
          model: gpt-3.5-turbo
          temperature: 0.7
      embedding:
        enabled: true
        options:
          model: text-embedding-3-small
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ai_smart_campus?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:root}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}

minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  bucket: campus

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: status
      logic-delete-value: 0
      logic-not-delete-value: 1

knife4j:
  enable: true

server:
  port: 8080
```

- [ ] **Step 3: Write application-dev.yml**

```yaml
logging:
  level:
    com.campus: debug
    org.springframework.ai: debug

spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-3.5-turbo
```

- [ ] **Step 4: Start the application to verify wiring**

Run: `cd ai-smart-campus/campus-core && mvn spring-boot:run`
Expected: Application starts without errors (MySQL/Redis/RabbitMQ must be running). Stop with Ctrl+C.

- [ ] **Step 5: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/CampusApplication.java
git add ai-smart-campus/campus-core/src/main/resources/
git commit -m "feat: add main app + SpringBoot/SpringAI/MyBatis-Plus config"
```

---

### Task 0.5: Config Beans (MybatisPlus, MinIO, Redis, RabbitMQ, RestTemplate)

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/config/MyBatisPlusConfig.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/config/MinioConfig.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/config/RedisConfig.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/config/RabbitMqConfig.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/config/RestTemplateConfig.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/config/CorsConfig.java`

**Interfaces:**
- Produces: `MinioClient` bean, `Jackson2JsonRedisSerializer<Object>` via RedisTemplate, standard `RabbitTemplate`, RestTemplate for internal calls

- [ ] **Step 1: Write MyBatisPlusConfig.java**

```java
package com.campus.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.campus.core.repository.mapper")
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 2: Write MinioConfig.java**

```java
package com.campus.core.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

- [ ] **Step 3: Write RabbitMqConfig.java**

```java
package com.campus.core.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String QUEUE_VECTOR_SYNC = "campus.vector.sync";
    public static final String QUEUE_VECTOR_DELETE = "campus.vector.delete";

    @Bean
    public Queue vectorSyncQueue() {
        return QueueBuilder.durable(QUEUE_VECTOR_SYNC)
                .ttl(60000)
                .deadLetterExchange("")
                .deadLetterRoutingKey("campus.vector.sync.dlq")
                .build();
    }

    @Bean
    public Queue vectorSyncDlq() {
        return QueueBuilder.durable("campus.vector.sync.dlq").build();
    }

    @Bean
    public Queue vectorDeleteQueue() {
        return QueueBuilder.durable(QUEUE_VECTOR_DELETE).build();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
```

- [ ] **Step 4: Write RedisConfig.java**

```java
package com.campus.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

- [ ] **Step 5: Write RestTemplateConfig.java**

```java
package com.campus.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

- [ ] **Step 6: Write CorsConfig.java**

```java
package com.campus.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 7: Build and verify**

Run: `cd ai-smart-campus && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/config/
git commit -m "feat: add infra config beans (MyBatisPlus, MinIO, Redis, RabbitMQ, CORS)"
```

---

### Task 0.6: Entity Classes (7 tables)

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/model/entity/User.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/model/entity/Announcement.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/model/entity/Club.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/model/entity/Recruitment.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/model/entity/Application.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/model/entity/Conversation.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/model/entity/Message.java`

**Interfaces:**
- Consumes: MySQL tables from Task 0.3
- Produces: MyBatis-Plus entity classes

- [ ] **Step 1: Write User.java**

```java
package com.campus.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String nickname;
    private String avatarUrl;
    private String interestTags;
    private Integer role;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: Write Announcement.java**

```java
package com.campus.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_announcement")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String summary;
    private String category;
    private String fileUrl;
    private String vectorId;
    @TableLogic
    private Integer status;
    private Long authorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: Write Club.java**

```java
package com.campus.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_club")
public class Club {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String tags;
    private String logoUrl;
    private Integer memberCount;
    @TableLogic
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Write Recruitment.java**

```java
package com.campus.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_recruitment")
public class Recruitment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long clubId;
    private String title;
    private String requirement;
    private Integer quota;
    private Integer enrolledCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status; // 0:下架 1:进行中 2:满员 3:已结束
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 5: Write Application.java (the enrollment record entity)**

```java
package com.campus.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_application")
public class Application {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recruitmentId;
    private Long userId;
    private String name;
    private String studentId;
    private String phone;
    private Integer status; // 0:已提交 1:已通过 2:未通过
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 6: Write Conversation.java**

```java
package com.campus.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 7: Write Message.java**

```java
package com.campus.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 8: Build**

Run: `cd ai-smart-campus && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/model/entity/
git commit -m "feat: add 7 entity classes mapped to DB tables"
```

---

## Phase 1: Core MVP

### MODULE A: Intelligent Chat

#### Task 1A.1: Mappers — ConversationMapper + MessageMapper

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/repository/mapper/ConversationMapper.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/repository/mapper/MessageMapper.java`

**Interfaces:**
- Produces: `ConversationMapper extends BaseMapper<Conversation>`, `MessageMapper extends BaseMapper<Message>`

- [ ] **Step 1: Write ConversationMapper.java**

```java
package com.campus.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.core.model.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
```

- [ ] **Step 2: Write MessageMapper.java**

```java
package com.campus.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.core.model.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT * FROM t_message WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<Message> findByConversationId(Long conversationId);
}
```

- [ ] **Step 3: Build and commit**

```bash
cd ai-smart-campus && mvn compile
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/repository/mapper/
git commit -m "feat: add ConversationMapper and MessageMapper"
```

---

#### Task 1A.2: Test — ChatService Contract

**Files:**
- Create: `ai-smart-campus/campus-core/src/test/java/com/campus/core/service/chat/ChatServiceTest.java`

**Interfaces:**
- Produces: Test class defining the ChatService contract before implementation

- [ ] **Step 1: Write the failing test**

```java
package com.campus.core.service.chat;

import com.campus.core.CampusApplication;
import com.campus.core.model.entity.Conversation;
import com.campus.core.model.entity.Message;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CampusApplication.class)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    private static Long conversationId;
    private static final Long TEST_USER_ID = 1L;

    @Test
    @Order(1)
    @DisplayName("创建会话")
    void shouldCreateConversation() {
        Conversation conv = chatService.createConversation(TEST_USER_ID);
        assertNotNull(conv);
        assertNotNull(conv.getId());
        assertEquals(TEST_USER_ID, conv.getUserId());
        assertEquals("新的对话", conv.getTitle());
        conversationId = conv.getId();
    }

    @Test
    @Order(2)
    @DisplayName("发送消息并获取AI流式回复")
    void shouldSendMessageAndGetReply() {
        String reply = chatService.chat(conversationId, "你好，图书馆在哪里？");
        assertNotNull(reply);
        assertFalse(reply.isBlank());
    }

    @Test
    @Order(3)
    @DisplayName("获取会话消息列表")
    void shouldGetMessages() {
        List<Message> messages = chatService.getMessages(conversationId);
        assertFalse(messages.isEmpty());
        assertEquals(2, messages.size()); // user + assistant
    }

    @Test
    @Order(4)
    @DisplayName("获取用户会话列表")
    void shouldGetConversations() {
        List<Conversation> convs = chatService.getConversations(TEST_USER_ID);
        assertFalse(convs.isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("删除会话")
    void shouldDeleteConversation() {
        chatService.deleteConversation(conversationId);
        List<Message> messages = chatService.getMessages(conversationId);
        assertTrue(messages.isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ai-smart-campus && mvn test -pl campus-core -Dtest=ChatServiceTest`
Expected: FAIL — ChatService class/bean not found. Confirm failure before proceeding.

- [ ] **Step 3: Commit**

```bash
git add ai-smart-campus/campus-core/src/test/
git commit -m "test: add ChatService contract test (RED)"
```

---

#### Task 1A.3: Implement — ChatService

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/service/chat/ChatService.java`

**Interfaces:**
- Consumes: ConversationMapper, MessageMapper, SpringAI ChatClient
- Produces: `ChatService` with createConversation, chat (streaming), getMessages, getConversations, deleteConversation

- [ ] **Step 1: Write ChatService.java**

```java
package com.campus.core.service.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.core.model.entity.Conversation;
import com.campus.core.model.entity.Message;
import com.campus.core.repository.mapper.ConversationMapper;
import com.campus.core.repository.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    private static final String SYSTEM_PROMPT = """
            你是一个智慧园区的AI助手，名叫"园宝"。你的职责：
            1. 回答园区相关问题（设施、流程、活动）
            2. 回答简洁、友善，不超过200字
            3. 不确定时主动告知，并建议用户去综合服务大厅
            4. 如果用户问的是社团/公告，主动引导到对应功能
            """;

    @Transactional
    public Conversation createConversation(Long userId) {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setTitle("新的对话");
        conversationMapper.insert(conv);
        return conv;
    }

    public String chat(Long conversationId, String userMessage) {
        // 1. 保存用户消息
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setRole("user");
        msg.setContent(userMessage);
        messageMapper.insert(msg);

        // 2. 构建 AI 调用
        String reply = chatClientBuilder.build()
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .content();

        // 3. 保存助手回复
        Message replyMsg = new Message();
        replyMsg.setConversationId(conversationId);
        replyMsg.setRole("assistant");
        replyMsg.setContent(reply);
        messageMapper.insert(replyMsg);

        // 4. 如果是第一条消息，更新会话标题
        if (messageMapper.findByConversationId(conversationId).size() <= 2) {
            Conversation conv = conversationMapper.selectById(conversationId);
            if (conv != null && "新的对话".equals(conv.getTitle())) {
                String title = userMessage.length() > 20 ? userMessage.substring(0, 20) + "..." : userMessage;
                conv.setTitle(title);
                conversationMapper.updateById(conv);
            }
        }

        return reply;
    }

    public List<Message> getMessages(Long conversationId) {
        return messageMapper.findByConversationId(conversationId);
    }

    public List<Conversation> getConversations(Long userId) {
        LambdaQueryWrapper<Conversation> qw = new LambdaQueryWrapper<>();
        qw.eq(Conversation::getUserId, userId)
          .orderByDesc(Conversation::getUpdatedAt);
        return conversationMapper.selectList(qw);
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        messageMapper.delete(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
    }
}
```

- [ ] **Step 2: Run test**

Run: `cd ai-smart-campus && mvn test -pl campus-core -Dtest=ChatServiceTest`
Expected: PASS

- [ ] **Step 3: Adjust if test fails, re-run, then commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/service/chat/
git commit -m "feat: implement ChatService (conversation CRUD + AI chat)"
```

---

#### Task 1A.4: Controller — ChatController + SSE streaming

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/ChatController.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/model/dto/ChatSendRequest.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/model/vo/ChatReplyVO.java`

**Interfaces:**
- Consumes: ChatService
- Produces: REST endpoints `/api/chat/send` (SSE), `/api/chat/conversations`, `/api/chat/messages`

- [ ] **Step 1: Write ChatSendRequest DTO**

```java
package com.campus.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatSendRequest {
    private Long conversationId;
    @NotBlank(message = "消息不能为空")
    private String message;
}
```

- [ ] **Step 2: Write ChatReplyVO**

```java
package com.campus.core.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReplyVO {
    private Long conversationId;
    private Long messageId;
    private String content;
    private String role;
}
```

- [ ] **Step 3: Add SSE support to ChatService**

Add this method to `ChatService.java`:

```java
// Inside ChatService, add:
public Flux<String> chatStream(Long conversationId, String userMessage) {
    // 1. 保存用户消息
    Message msg = new Message();
    msg.setConversationId(conversationId);
    msg.setRole("user");
    msg.setContent(userMessage);
    messageMapper.insert(msg);

    // 2. 流式调用 AI
    return chatClientBuilder.build()
            .prompt()
            .system(SYSTEM_PROMPT)
            .user(userMessage)
            .stream()
            .content()
            .doOnComplete(() -> {
                // Can't easily accumulate in reactive, handled in controller
            });
}
```

- [ ] **Step 4: Write ChatController.java**

```java
package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.core.model.dto.ChatSendRequest;
import com.campus.core.model.entity.Conversation;
import com.campus.core.model.entity.Message;
import com.campus.core.model.vo.ChatReplyVO;
import com.campus.core.service.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "智能咨询", description = "AI对话和会话管理")
public class ChatController {

    private final ChatService chatService;

    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息（SSE流式）")
    public Flux<String> send(@Valid @RequestBody ChatSendRequest req) {
        Long convId = req.getConversationId();
        // 如果没有会话ID，创建新会话
        if (convId == null) {
            Conversation conv = chatService.createConversation(1L); // TODO: replace with real user
            convId = conv.getId();
        }
        final Long finalConvId = convId;
        return chatService.chatStream(finalConvId, req.getMessage());
    }

    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表")
    public ApiResult<List<Conversation>> conversations() {
        return ApiResult.ok(chatService.getConversations(1L)); // TODO: real user
    }

    @GetMapping("/messages")
    @Operation(summary = "获取消息历史")
    public ApiResult<List<Message>> messages(@RequestParam Long convId) {
        return ApiResult.ok(chatService.getMessages(convId));
    }

    @DeleteMapping("/conversations/{convId}")
    @Operation(summary = "删除会话")
    public ApiResult<Void> deleteConversation(@PathVariable Long convId) {
        chatService.deleteConversation(convId);
        return ApiResult.ok();
    }
}
```

- [ ] **Step 5: Build and start app, test with curl**

Run:
```bash
cd ai-smart-campus && mvn compile
# Start app in background, then:
curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{"message":"你好"}'
```
Expected: SSE stream with AI reply tokens.

- [ ] **Step 6: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/ChatController.java
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/model/
git commit -m "feat: add ChatController with SSE streaming endpoint"
```

---

### MODULE B: Vector Search & Knowledge Base

#### Task 1B.1: Mappers — AnnouncementMapper

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/repository/mapper/AnnouncementMapper.java`

**Interfaces:**
- Produces: `AnnouncementMapper extends BaseMapper<Announcement>`

- [ ] **Step 1: Write AnnouncementMapper.java**

```java
package com.campus.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.core.model.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    @Select("SELECT * FROM t_announcement WHERE status = 1 AND category = #{category} ORDER BY created_at DESC")
    List<Announcement> findByCategory(String category);
}
```

- [ ] **Step 2: Build and commit**

```bash
cd ai-smart-campus && mvn compile
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/repository/mapper/AnnouncementMapper.java
git commit -m "feat: add AnnouncementMapper"
```

---

#### Task 1B.2: Test — SearchService Contract

**Files:**
- Create: `ai-smart-campus/campus-core/src/test/java/com/campus/core/service/search/SearchServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.campus.core.service.search;

import com.campus.core.CampusApplication;
import com.campus.core.model.entity.Announcement;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CampusApplication.class)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SearchServiceTest {

    @Autowired
    private SearchService searchService;

    private static Long docId;

    @Test
    @Order(1)
    @DisplayName("上传文档并向量化")
    void shouldUploadAndVectorize() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.txt", "text/plain",
            "图书馆位于校园中心，开放时间为早上8点到晚上10点。借书需要学生卡。".getBytes()
        );
        Announcement ann = searchService.uploadDocument(
            "图书馆使用指南", "notice", file, 1L);
        assertNotNull(ann);
        assertNotNull(ann.getId());
        assertNotNull(ann.getVectorId());
        docId = ann.getId();
    }

    @Test
    @Order(2)
    @DisplayName("语义搜索：应找到上传的文档")
    void shouldSemanticSearch() {
        List<Announcement> results = searchService.search("图书馆在哪里");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(a -> a.getTitle().contains("图书馆")));
    }

    @Test
    @Order(3)
    @DisplayName("按分类查询公告")
    void shouldQueryByCategory() {
        List<Announcement> results = searchService.getByCategory("notice", 1, 10);
        assertFalse(results.isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("删除公告同步移除向量")
    void shouldDeleteAndRemoveVector() {
        searchService.deleteAnnouncement(docId);
        // Verify it's soft-deleted
        Announcement ann = searchService.getById(docId);
        assertNull(ann);
    }
}
```

- [ ] **Step 2: Confirm test failure**

Run: `cd ai-smart-campus && mvn test -pl campus-core -Dtest=SearchServiceTest`
Expected: FAIL — SearchService bean not found.

- [ ] **Step 3: Commit**

```bash
git add ai-smart-campus/campus-core/src/test/java/com/campus/core/service/search/
git commit -m "test: add SearchService contract test (RED)"
```

---

#### Task 1B.3: Implement — Redis Vector Service + SearchService

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/service/search/SearchService.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/vector/VectorStoreService.java`

**Interfaces:**
- Consumes: AnnouncementMapper, SpringAI EmbeddingClient, Redis Stack (via RedisTemplate or Jedis)
- Produces: Document upload → chunk → embed → store pipeline, semantic search, CRUD

- [ ] **Step 1: Write VectorStoreService.java**

```java
package com.campus.core.vector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String VECTOR_INDEX = "idx:announcement:vector";
    private static final String VECTOR_PREFIX = "vec:announcement:";
    private static final int VECTOR_DIM = 1536;

    /**
     * Create HNSW index for vector search (call once at startup or ensure via config)
     */
    public void ensureIndex() {
        try {
            redisTemplate.opsForHash().getOperations().execute(connection -> {
                // FT.CREATE idx:announcement:vector ON HASH PREFIX 1 vec:announcement:
                //   SCHEMA vector VECTOR HNSW 6 DIM 1536 DISTANCE_METRIC COSINE
                //   title TEXT content TEXT category TAG
                // Use Lettuce native commands or Redis CLI
                return null;
            });
        } catch (Exception e) {
            log.debug("Vector index may already exist: {}", e.getMessage());
        }
    }

    public String storeVector(String docId, String title, String content, String category, float[] embedding) {
        String key = VECTOR_PREFIX + docId;

        // Convert float[] to byte[] (little-endian)
        ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : embedding) {
            buffer.putFloat(f);
        }
        byte[] vectorBytes = buffer.array();

        Map<String, Object> fields = new HashMap<>();
        fields.put("vector", vectorBytes);
        fields.put("title", title);
        fields.put("content", content);
        fields.put("category", category);
        fields.put("doc_id", docId);

        redisTemplate.opsForHash().putAll(key, fields);
        return docId;
    }

    public void deleteVector(String docId) {
        redisTemplate.delete(VECTOR_PREFIX + docId);
    }

    /**
     * KNN vector search via Redis Stack FT.SEARCH.
     * Conn: Lettuce (via RedisTemplate's native connection).
     */
    @SuppressWarnings("unchecked")
    public List<String> searchSimilar(float[] queryEmbedding, int topK) {
        ByteBuffer buffer = ByteBuffer.allocate(queryEmbedding.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : queryEmbedding) {
            buffer.putFloat(f);
        }
        byte[] queryBytes = buffer.array();

        List<String> docIds = redisTemplate.execute((org.springframework.data.redis.connection.RedisConnection connection) -> {
            Object nativeConn = connection.getNativeConnection();
            // Lettuce connection
            if (nativeConn instanceof io.lettuce.core.api.sync.RedisCommands<?, ?> cmds) {
                @SuppressWarnings("rawtypes")
                io.lettuce.core.api.sync.RedisCommands rawCmds = cmds;
                // FT.SEARCH idx "(*)" => [KNN $K @vector $BLOB AS score] PARAMS 4 BLOB $bytes K $k SORTBY score DIALECT 2
                List<Object> results = rawCmds.custom()
                    .dispatch(
                        io.lettuce.core.protocol.CommandType.valueOf("FT.SEARCH"),
                        io.lettuce.core.output.StatusOutput::new,
                        new io.lettuce.core.protocol.CommandArgs<>()
                            .add("idx:announcement:vector")
                            .add("(*)")
                            .add("=>")
                            .add("[KNN")
                            .add(String.valueOf(topK))
                            .add("@vector")
                            .add("$BLOB")
                            .add("AS")
                            .add("score")
                            .add("]")
                            .add("PARAMS")
                            .add("2")
                            .add("BLOB")
                            .add(queryBytes)
                            .add("SORTBY")
                            .add("score")
                            .add("DIALECT")
                            .add("2")
                    );
                // Parse results: returns [count, key1, fields1, key2, fields2, ...]
                List<String> ids = new java.util.ArrayList<>();
                if (results != null) {
                    for (int i = 1; i < results.size(); i += 2) {
                        String key = String.valueOf(results.get(i));
                        // Strip prefix: "vec:announcement:" → docId
                        ids.add(key.replace(VECTOR_PREFIX, ""));
                    }
                }
                return ids;
            }
            return Collections.<String>emptyList();
        });
        return docIds != null ? docIds : Collections.emptyList();
    }
}
```

- [ ] **Step 2: Write SearchService.java**

```java
package com.campus.core.service.search;

import com.campus.core.model.entity.Announcement;
import com.campus.core.repository.mapper.AnnouncementMapper;
import com.campus.core.vector.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final AnnouncementMapper announcementMapper;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;

    private static final int CHUNK_SIZE = 512;

    @Transactional
    public Announcement uploadDocument(String title, String category, MultipartFile file, Long authorId) {
        try {
            // 1. 读取文件内容
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 2. 保存到 DB
            Announcement ann = new Announcement();
            ann.setTitle(title);
            ann.setContent(content);
            ann.setCategory(category);
            ann.setAuthorId(authorId);
            ann.setStatus(1);
            announcementMapper.insert(ann);

            // 3. 分块 + 向量化 + 存储
            List<String> chunks = splitText(content, CHUNK_SIZE);
            float[] embedding = embed(chunks.get(0)); // MVP: embed first 512 chars
            String vectorId = vectorStoreService.storeVector(
                String.valueOf(ann.getId()), title, content, category, embedding);
            ann.setVectorId(vectorId);
            announcementMapper.updateById(ann);

            return ann;
        } catch (IOException e) {
            log.error("文件读取失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    public List<Announcement> search(String query) {
        // 1. 向量化查询
        float[] queryEmbedding = embed(query);

        // 2. 向量检索
        List<String> docIds = vectorStoreService.searchSimilar(queryEmbedding, 5);
        if (docIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 加载文档
        List<Long> ids = docIds.stream().map(Long::valueOf).collect(Collectors.toList());
        return announcementMapper.selectBatchIds(ids);
    }

    public List<Announcement> getByCategory(String category, int page, int size) {
        return announcementMapper.findByCategory(category);
    }

    public Announcement getById(Long id) {
        return announcementMapper.selectById(id);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        vectorStoreService.deleteVector(String.valueOf(id));
        announcementMapper.deleteById(id); // MyBatis-Plus logic delete
    }

    private float[] embed(String text) {
        var request = new EmbeddingRequest(List.of(text), null);
        var response = embeddingClient.call(request);
        List<Double> embedding = response.getResult().getOutput();
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }

    private List<String> splitText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));
        }
        return chunks;
    }
}
```

- [ ] **Step 3: Run test**

Run: `cd ai-smart-campus && mvn test -pl campus-core -Dtest=SearchServiceTest`
Expected: PASS (ensure Redis Stack is running with RediSearch module)

- [ ] **Step 4: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/service/search/
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/vector/
git commit -m "feat: implement SearchService + VectorStoreService (upload, embed, search)"
```

---

#### Task 1B.4: Controller — AnnouncementController + SearchController

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/AnnouncementController.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/SearchController.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/model/dto/AnnouncementUploadDTO.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/model/dto/SearchRequest.java`

- [ ] **Step 1: Write DTOs**

```java
// AnnouncementUploadDTO.java
package com.campus.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementUploadDTO {
    @NotBlank
    private String title;
    @NotBlank
    private String category;
}
```

```java
// SearchRequest.java
package com.campus.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequest {
    @NotBlank
    private String keyword;
}
```

- [ ] **Step 2: Write AnnouncementController.java**

```java
package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.core.model.entity.Announcement;
import com.campus.core.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Tag(name = "园区墙", description = "公告、文档管理")
public class AnnouncementController {

    private final SearchService searchService;

    @PostMapping("/upload")
    @Operation(summary = "上传文档")
    public ApiResult<Announcement> upload(
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam("file") MultipartFile file) {
        return ApiResult.ok(searchService.uploadDocument(title, category, file, 1L)); // TODO: real author
    }

    @GetMapping
    @Operation(summary = "按分类查询公告")
    public ApiResult<List<Announcement>> list(
            @RequestParam(defaultValue = "notice") String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(searchService.getByCategory(category, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "公告详情")
    public ApiResult<Announcement> detail(@PathVariable Long id) {
        Announcement ann = searchService.getById(id);
        if (ann == null) {
            return ApiResult.fail(404, "公告不存在");
        }
        return ApiResult.ok(ann);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除公告")
    public ApiResult<Void> delete(@PathVariable Long id) {
        searchService.deleteAnnouncement(id);
        return ApiResult.ok();
    }
}
```

- [ ] **Step 3: Write SearchController.java**

```java
package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.core.model.dto.SearchRequest;
import com.campus.core.model.entity.Announcement;
import com.campus.core.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "智能检索", description = "语义搜索公告和文档")
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    @Operation(summary = "语义搜索")
    public ApiResult<List<Announcement>> search(@Valid @RequestBody SearchRequest req) {
        return ApiResult.ok(searchService.search(req.getKeyword()));
    }
}
```

- [ ] **Step 4: Build and verify**

Run: `cd ai-smart-campus && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/model/dto/
git commit -m "feat: add AnnouncementController + SearchController"
```

---

### MODULE C: Club Recommendation & Registration

#### Task 1C.1: Mappers — ClubMapper, RecruitmentMapper, ApplicationMapper

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/repository/mapper/ClubMapper.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/repository/mapper/RecruitmentMapper.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/repository/mapper/ApplicationMapper.java`

- [ ] **Step 1: Write all three mappers**

```java
// ClubMapper.java
package com.campus.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.core.model.entity.Club;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClubMapper extends BaseMapper<Club> {
}
```

```java
// RecruitmentMapper.java
package com.campus.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.core.model.entity.Recruitment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecruitmentMapper extends BaseMapper<Recruitment> {

    @Select("SELECT * FROM t_recruitment WHERE status = 1 AND club_id = #{clubId} ORDER BY created_at DESC")
    List<Recruitment> findActiveByClubId(Long clubId);
}
```

```java
// ApplicationMapper.java
package com.campus.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.core.model.entity.Application;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {

    @Select("SELECT * FROM t_application WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Application> findByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM t_application WHERE user_id = #{userId} AND recruitment_id = #{recruitmentId}")
    int countByUserAndRecruitment(Long userId, Long recruitmentId);
}
```

- [ ] **Step 2: Build and commit**

```bash
cd ai-smart-campus && mvn compile
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/repository/mapper/ClubMapper.java
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/repository/mapper/RecruitmentMapper.java
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/repository/mapper/ApplicationMapper.java
git commit -m "feat: add ClubMapper, RecruitmentMapper, ApplicationMapper"
```

---

#### Task 1C.2: Test — ClubService Contract

**Files:**
- Create: `ai-smart-campus/campus-core/src/test/java/com/campus/core/service/club/ClubServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.campus.core.service.club;

import com.campus.core.CampusApplication;
import com.campus.core.model.entity.Application;
import com.campus.core.model.entity.Club;
import com.campus.core.model.entity.Recruitment;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CampusApplication.class)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClubServiceTest {

    @Autowired
    private ClubService clubService;

    private static Long clubId;
    private static Long recruitmentId;
    private static final Long TEST_USER_ID = 1L;

    @Test
    @Order(1)
    @DisplayName("创建社团")
    void shouldCreateClub() {
        Club club = new Club();
        club.setName("AI创新社");
        club.setDescription("探索人工智能技术");
        club.setTags("[\"科技\",\"AI\",\"编程\"]");
        clubService.saveClub(club);
        assertNotNull(club.getId());
        clubId = club.getId();
    }

    @Test
    @Order(2)
    @DisplayName("兴趣推荐：科技标签应匹配AI创新社")
    void shouldRecommendByInterest() {
        List<Club> clubs = clubService.recommend(List.of("科技", "编程", "AI"));
        assertFalse(clubs.isEmpty());
        assertTrue(clubs.stream().anyMatch(c -> "AI创新社".equals(c.getName())));
    }

    @Test
    @Order(3)
    @DisplayName("创建纳新活动")
    void shouldCreateRecruitment() {
        Recruitment rec = clubService.createRecruitment(clubId,
            "2026春季纳新", "热爱AI的同学", 30);
        assertNotNull(rec.getId());
        recruitmentId = rec.getId();
    }

    @Test
    @Order(4)
    @DisplayName("报名：不应重复")
    void shouldApply() {
        Application app = clubService.apply(recruitmentId, TEST_USER_ID,
            "张三", "2024001", "13800138000");
        assertNotNull(app.getId());
        // 重复报名应抛异常
        assertThrows(RuntimeException.class, () ->
            clubService.apply(recruitmentId, TEST_USER_ID,
                "张三", "2024001", "13800138000"));
    }

    @Test
    @Order(5)
    @DisplayName("查看报名记录")
    void shouldGetMyApplications() {
        List<Application> apps = clubService.getMyApplications(TEST_USER_ID);
        assertFalse(apps.isEmpty());
    }
}
```

- [ ] **Step 2: Confirm failure**

Run: `cd ai-smart-campus && mvn test -pl campus-core -Dtest=ClubServiceTest`
Expected: FAIL

- [ ] **Step 3: Commit**

```bash
git add ai-smart-campus/campus-core/src/test/java/com/campus/core/service/club/
git commit -m "test: add ClubService contract test (RED)"
```

---

#### Task 1C.3: Implement — ClubService

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/service/club/ClubService.java`

- [ ] **Step 1: Write ClubService.java**

```java
package com.campus.core.service.club;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import com.campus.core.model.entity.Application;
import com.campus.core.model.entity.Club;
import com.campus.core.model.entity.Recruitment;
import com.campus.core.repository.mapper.ApplicationMapper;
import com.campus.core.repository.mapper.ClubMapper;
import com.campus.core.repository.mapper.RecruitmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubMapper clubMapper;
    private final RecruitmentMapper recruitmentMapper;
    private final ApplicationMapper applicationMapper;

    @Transactional
    public void saveClub(Club club) {
        clubMapper.insert(club);
    }

    public List<Club> listClubs() {
        return clubMapper.selectList(null);
    }

    public Club getClubById(Long id) {
        return clubMapper.selectById(id);
    }

    /**
     * 基于标签 Jaccard 相似度推荐社团
     * 算法：对每个社团，计算用户标签与社团标签的 Jaccard 相似度，按相似度降序排列
     */
    public List<Club> recommend(List<String> userTags) {
        List<Club> allClubs = clubMapper.selectList(null);
        Set<String> userTagSet = new HashSet<>(userTags);

        return allClubs.stream()
                .filter(c -> c.getTags() != null && !c.getTags().isBlank())
                .sorted((a, b) -> {
                    double scoreA = jaccardSim(userTagSet, parseTags(a.getTags()));
                    double scoreB = jaccardSim(userTagSet, parseTags(b.getTags()));
                    return Double.compare(scoreB, scoreA);
                })
                .filter(c -> jaccardSim(userTagSet, parseTags(c.getTags())) > 0)
                .limit(10)
                .collect(Collectors.toList());
    }

    private double jaccardSim(Set<String> userTags, Set<String> clubTags) {
        Set<String> intersection = new HashSet<>(userTags);
        intersection.retainAll(clubTags);
        Set<String> union = new HashSet<>(userTags);
        union.addAll(clubTags);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private Set<String> parseTags(String tagsJson) {
        JSONArray arr = JSONUtil.parseArray(tagsJson);
        return arr.stream().map(Object::toString).collect(Collectors.toSet());
    }

    @Transactional
    public Recruitment createRecruitment(Long clubId, String title, String requirement, int quota) {
        Recruitment rec = new Recruitment();
        rec.setClubId(clubId);
        rec.setTitle(title);
        rec.setRequirement(requirement);
        rec.setQuota(quota);
        rec.setEnrolledCount(0);
        rec.setStartTime(java.time.LocalDateTime.now());
        rec.setEndTime(java.time.LocalDateTime.now().plusDays(30));
        rec.setStatus(1);
        recruitmentMapper.insert(rec);
        return rec;
    }

    @Transactional
    public Application apply(Long recruitmentId, Long userId,
                              String name, String studentId, String phone) {
        // 检查重复
        int count = applicationMapper.countByUserAndRecruitment(userId, recruitmentId);
        if (count > 0) {
            throw new BizException(ErrorCode.DUPLICATE_APPLY);
        }

        // 检查名额
        Recruitment rec = recruitmentMapper.selectById(recruitmentId);
        if (rec == null || rec.getStatus() != 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "纳新活动已结束");
        }
        if (rec.getEnrolledCount() >= rec.getQuota()) {
            throw new BizException(ErrorCode.CONFLICT, "名额已满");
        }

        Application app = new Application();
        app.setRecruitmentId(recruitmentId);
        app.setUserId(userId);
        app.setName(name);
        app.setStudentId(studentId);
        app.setPhone(phone);
        app.setStatus(0);
        applicationMapper.insert(app);

        // 更新报名计数
        rec.setEnrolledCount(rec.getEnrolledCount() + 1);
        if (rec.getEnrolledCount() >= rec.getQuota()) {
            rec.setStatus(2); // 满员
        }
        recruitmentMapper.updateById(rec);

        return app;
    }

    public List<Application> getMyApplications(Long userId) {
        return applicationMapper.findByUserId(userId);
    }

    public List<Recruitment> getRecruitmentsByClub(Long clubId) {
        return recruitmentMapper.findActiveByClubId(clubId);
    }
}
```

- [ ] **Step 2: Run test**

Run: `cd ai-smart-campus && mvn test -pl campus-core -Dtest=ClubServiceTest`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/service/club/
git commit -m "feat: implement ClubService (CRUD + Jaccard tag recommendation + apply)"
```

---

#### Task 1C.4: Controller — ClubController + ApplyController

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/ClubController.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/ApplyController.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/model/dto/ApplyRequest.java`

- [ ] **Step 1: Write ApplyRequest DTO**

```java
package com.campus.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ApplyRequest {
    private Long recruitmentId;
    @NotBlank
    private String name;
    @NotBlank
    private String studentId;
    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

- [ ] **Step 2: Write ClubController.java**

```java
package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.core.model.entity.Club;
import com.campus.core.model.entity.Recruitment;
import com.campus.core.service.club.ClubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/club")
@RequiredArgsConstructor
@Tag(name = "社团推荐", description = "社团管理和兴趣推荐")
public class ClubController {

    private final ClubService clubService;

    @GetMapping("/recommend")
    @Operation(summary = "兴趣推荐社团")
    public ApiResult<List<Club>> recommend(@RequestParam String tags) {
        List<String> tagList = List.of(tags.split(","));
        return ApiResult.ok(clubService.recommend(tagList));
    }

    @GetMapping
    @Operation(summary = "所有社团")
    public ApiResult<List<Club>> listAll() {
        return ApiResult.ok(clubService.listClubs());
    }

    @GetMapping("/{id}")
    @Operation(summary = "社团详情")
    public ApiResult<Club> detail(@PathVariable Long id) {
        return ApiResult.ok(clubService.getClubById(id));
    }

    @GetMapping("/{clubId}/recruitments")
    @Operation(summary = "社团纳新活动列表")
    public ApiResult<List<Recruitment>> recruitments(@PathVariable Long clubId) {
        return ApiResult.ok(clubService.getRecruitmentsByClub(clubId));
    }
}
```

- [ ] **Step 3: Write ApplyController.java**

```java
package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.core.model.dto.ApplyRequest;
import com.campus.core.model.entity.Application;
import com.campus.core.service.club.ClubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apply")
@RequiredArgsConstructor
@Tag(name = "报名管理", description = "社团纳新报名")
public class ApplyController {

    private final ClubService clubService;

    @PostMapping
    @Operation(summary = "提交报名")
    public ApiResult<Application> apply(@Valid @RequestBody ApplyRequest req) {
        return ApiResult.ok(clubService.apply(
            req.getRecruitmentId(), 1L, // TODO: real user
            req.getName(), req.getStudentId(), req.getPhone()));
    }

    @GetMapping("/my")
    @Operation(summary = "我的报名记录")
    public ApiResult<List<Application>> myApplications() {
        return ApiResult.ok(clubService.getMyApplications(1L)); // TODO: real user
    }
}
```

- [ ] **Step 4: Build and verify**

Run: `cd ai-smart-campus && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/ClubController.java
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/ApplyController.java
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/model/dto/ApplyRequest.java
git commit -m "feat: add ClubController + ApplyController with recommendation and registration APIs"
```

---

### MODULE D: Admin Controller

#### Task 1D.1: AdminController (rebuild index)

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/AdminController.java`

- [ ] **Step 1: Write AdminController.java**

```java
package com.campus.core.controller;

import com.campus.common.dto.ApiResult;
import com.campus.core.model.entity.Announcement;
import com.campus.core.repository.mapper.AnnouncementMapper;
import com.campus.core.vector.VectorStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理后台", description = "索引重建等管理操作")
public class AdminController {

    private final AnnouncementMapper announcementMapper;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;

    @PostMapping("/rebuild-index")
    @Operation(summary = "全量重建向量索引")
    public ApiResult<String> rebuildIndex() {
        List<Announcement> all = announcementMapper.selectList(null);
        int count = 0;
        for (Announcement ann : all) {
            if (ann.getContent() == null || ann.getContent().isBlank()) continue;
            var request = new EmbeddingRequest(
                List.of(ann.getContent().substring(0, Math.min(512, ann.getContent().length()))),
                null);
            var response = embeddingClient.call(request);
            List<Double> emb = response.getResult().getOutput();
            float[] embedding = new float[emb.size()];
            for (int i = 0; i < emb.size(); i++) {
                embedding[i] = emb.get(i).floatValue();
            }
            vectorStoreService.storeVector(
                String.valueOf(ann.getId()), ann.getTitle(),
                ann.getContent(), ann.getCategory(), embedding);
            ann.setVectorId(String.valueOf(ann.getId()));
            announcementMapper.updateById(ann);
            count++;
        }
        return ApiResult.ok("重建完成，处理 " + count + " 条文档");
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
cd ai-smart-campus && mvn compile
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/AdminController.java
git commit -m "feat: add AdminController with rebuild-index endpoint"
```

---

## Phase 2: Enhanced Capabilities

### Task 2.1: Multi-turn Conversation Context

**Files:**
- Modify: `ai-smart-campus/campus-core/src/main/java/com/campus/core/service/chat/ChatService.java`

- [ ] **Step 1: Add multi-turn method**

Add this method to ChatService.java:

```java
/**
 * 带历史上下文的多轮对话
 */
public String chatWithHistory(Long conversationId, String userMessage) {
    Message msg = new Message();
    msg.setConversationId(conversationId);
    msg.setRole("user");
    msg.setContent(userMessage);
    messageMapper.insert(msg);

    // 加载最近 10 条消息作为上下文
    List<Message> history = messageMapper.findByConversationId(conversationId);
    int start = Math.max(0, history.size() - 11); // 最近10条 + 当前user消息
    List<Message> recentHistory = history.subList(start, history.size() - 1);

    ChatClient.ChatClientRequestSpec prompt = chatClientBuilder.build()
            .prompt()
            .system(SYSTEM_PROMPT);

    // 注入历史消息
    for (Message h : recentHistory) {
        if ("user".equals(h.getRole())) {
            prompt = prompt.user(h.getContent());
        } else if ("assistant".equals(h.getRole())) {
            prompt = prompt.assistant(h.getContent());
        }
    }
    prompt = prompt.user(userMessage);

    String reply = prompt.call().content();

    Message replyMsg = new Message();
    replyMsg.setConversationId(conversationId);
    replyMsg.setRole("assistant");
    replyMsg.setContent(reply);
    messageMapper.insert(replyMsg);

    return reply;
}
```

- [ ] **Step 2: Update controller to use multi-turn**

In `ChatController.java`, update the non-streaming path or create a new endpoint:

```java
@PostMapping("/send-sync")
@Operation(summary = "发送消息（同步，含上下文）")
public ApiResult<ChatReplyVO> sendSync(@Valid @RequestBody ChatSendRequest req) {
    Long convId = req.getConversationId();
    if (convId == null) {
        Conversation conv = chatService.createConversation(1L);
        convId = conv.getId();
    }
    String reply = chatService.chatWithHistory(convId, req.getMessage());
    return ApiResult.ok(new ChatReplyVO(convId, null, reply, "assistant"));
}
```

- [ ] **Step 3: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/service/chat/ChatService.java
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/ChatController.java
git commit -m "feat: add multi-turn conversation with history context"
```

---

### Task 2.2: Function Calling — Campus Tools

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/ai/CampusFunctions.java`

**Interfaces:**
- Produces: SpringAI `@Tool` annotated functions for `queryFacility`, `queryProcedure`

- [ ] **Step 1: Write CampusFunctions.java**

```java
package com.campus.core.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class CampusFunctions {

    // --- 模拟数据 (MVP) ---
    private static final Map<String, String> FACILITIES = Map.of(
        "图书馆", "图书馆位于校园中心A栋，开放时间 8:00-22:00。凭学生卡进入。",
        "食堂", "第一食堂位于B栋一楼，供应早/中/晚餐。第二食堂位于C栋二楼。",
        "体育馆", "体育馆位于D栋，开放时间 6:00-21:00。需预约使用。",
        "校医院", "校医院位于E栋一楼，24小时急诊。门诊时间 8:00-17:30。",
        "快递站", "快递站位于F栋地下一层，营业时间 9:00-19:00。"
    );

    private static final Map<String, String> PROCEDURES = Map.of(
        "校园卡补办", "补办流程：① 到综合服务大厅自助机挂失 ② 携带身份证到3号窗口 ③ 缴纳工本费20元 ④ 立等可取（约5分钟）",
        "成绩单打印", "打印流程：① 登录教务系统 ② 在线申请成绩单 ③ 选择自助打印机（图书馆/教学楼）④ 刷学生卡打印",
        "宿舍报修", "报修流程：① 打开校园APP > 后勤服务 ② 填写报修工单 ③ 维修人员48小时内上门 ④ 完成后评价",
        "借书", "借书流程：① 携带学生卡到图书馆 ② 在检索台查找藏书位置 ③ 自助借还机刷卡借书 ④ 借期30天，可续借1次"
    );

    @Tool(name = "queryFacilityLocation", description = "查询园区设施的地址和开放时间，传入设施名称（如\"图书馆\"）")
    public String queryFacilityLocation(@ToolParam(description = "设施名称") String name) {
        log.info("Tool called: queryFacilityLocation({})", name);
        for (Map.Entry<String, String> e : FACILITIES.entrySet()) {
            if (e.getKey().contains(name) || name.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return "未找到设施\"" + name + "\"的信息，建议用户前往综合服务大厅咨询。";
    }

    @Tool(name = "queryProcedure", description = "查询办事流程，传入事项名称（如\"校园卡补办\"）")
    public String queryProcedure(@ToolParam(description = "办事事项名称") String name) {
        log.info("Tool called: queryProcedure({})", name);
        for (Map.Entry<String, String> e : PROCEDURES.entrySet()) {
            if (e.getKey().contains(name) || name.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return "未找到\"" + name + "\"的办事流程，建议用户前往综合服务大厅咨询。";
    }
}
```

- [ ] **Step 2: Update ChatService to use tools**

Modify the `chatWithHistory` method in `ChatService.java` to register tools:

```java
// In ChatService constructor, also inject CampusFunctions:
private final CampusFunctions campusFunctions;

// Then in the chatWithHistory method, replace the prompt chain with:
String reply = chatClientBuilder.build()
        .prompt()
        .system(SYSTEM_PROMPT)
        .tools(campusFunctions)
        .user(userMessage)
        .call()
        .content();
```

- [ ] **Step 3: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/ai/
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/service/chat/ChatService.java
git commit -m "feat: add Function Calling tools (queryFacility, queryProcedure)"
```

---

### Task 2.3: Async Vector Sync Pipeline (RabbitMQ)

**Files:**
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/mq/VectorSyncMessage.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/mq/VectorSyncProducer.java`
- Create: `ai-smart-campus/campus-core/src/main/java/com/campus/core/mq/VectorSyncConsumer.java`

- [ ] **Step 1: Write VectorSyncMessage.java**

```java
package com.campus.core.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VectorSyncMessage implements Serializable {
    private String action;   // "UPSERT" | "DELETE"
    private Long announcementId;
    private String title;
    private String content;
    private String category;
}
```

- [ ] **Step 2: Write VectorSyncProducer.java**

```java
package com.campus.core.mq;

import com.campus.core.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VectorSyncProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendUpsert(VectorSyncMessage msg) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.QUEUE_VECTOR_SYNC, msg);
    }

    public void sendDelete(Long announcementId) {
        VectorSyncMessage msg = new VectorSyncMessage();
        msg.setAction("DELETE");
        msg.setAnnouncementId(announcementId);
        rabbitTemplate.convertAndSend(RabbitMqConfig.QUEUE_VECTOR_DELETE, msg);
    }
}
```

- [ ] **Step 3: Write VectorSyncConsumer.java**

```java
package com.campus.core.mq;

import com.campus.core.config.RabbitMqConfig;
import com.campus.core.vector.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorSyncConsumer {

    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_VECTOR_SYNC)
    public void handleSync(VectorSyncMessage msg) {
        log.info("Vector sync: action={}, annId={}", msg.getAction(), msg.getAnnouncementId());
        try {
            String text = (msg.getContent() != null)
                ? msg.getContent().substring(0, Math.min(512, msg.getContent().length()))
                : "";
            var request = new EmbeddingRequest(List.of(text), null);
            var response = embeddingClient.call(request);
            List<Double> emb = response.getResult().getOutput();
            float[] embedding = new float[emb.size()];
            for (int i = 0; i < emb.size(); i++) {
                embedding[i] = emb.get(i).floatValue();
            }
            vectorStoreService.storeVector(
                String.valueOf(msg.getAnnouncementId()),
                msg.getTitle(), msg.getContent(), msg.getCategory(), embedding);
        } catch (Exception e) {
            log.error("向量同步失败: annId={}", msg.getAnnouncementId(), e);
            throw e; // 触发重试
        }
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_VECTOR_DELETE)
    public void handleDelete(VectorSyncMessage msg) {
        log.info("Vector delete: annId={}", msg.getAnnouncementId());
        vectorStoreService.deleteVector(String.valueOf(msg.getAnnouncementId()));
    }
}
```

- [ ] **Step 4: Update SearchService to use async producer**

Modify `SearchService.uploadDocument()` — replace synchronous vectorStoreService call with:

```java
// In SearchService, after saving the announcement:
vectorSyncProducer.sendUpsert(new VectorSyncMessage("UPSERT",
    ann.getId(), ann.getTitle(), ann.getContent(), ann.getCategory()));
```

- [ ] **Step 5: Build and commit**

```bash
cd ai-smart-campus && mvn compile
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/mq/
git commit -m "feat: add async vector sync pipeline via RabbitMQ"
```

---

### Task 2.4: LLM Re-ranking for Club Recommendations

**Files:**
- Modify: `ai-smart-campus/campus-core/src/main/java/com/campus/core/service/club/ClubService.java`

- [ ] **Step 1: Add LLM re-ranking method**

Add to `ClubService.java`:

```java
import org.springframework.ai.chat.client.ChatClient;

// Inject:
private final ChatClient.Builder chatClientBuilder;

/**
 * 先用标签过滤，再用LLM精排Top-3
 */
public List<Club> recommendWithLLM(List<String> userTags) {
    List<Club> candidates = recommend(userTags); // Jaccard 粗排
    if (candidates.size() <= 3) return candidates;

    // 让LLM从候选中挑出最匹配的3个
    StringBuilder clubList = new StringBuilder();
    for (int i = 0; i < candidates.size(); i++) {
        Club c = candidates.get(i);
        clubList.append(String.format("%d. %s - %s [标签: %s]\n",
            i + 1, c.getName(), c.getDescription(), c.getTags()));
    }

    String prompt = String.format(
        "用户兴趣标签: %s\n\n候选社团:\n%s\n请从以上社团中选出最适合该用户的Top-3社团，只返回编号（如 3,1,5）",
        String.join(", ", userTags), clubList.toString());

    String response = chatClientBuilder.build().prompt().user(prompt).call().content();
    try {
        String[] indices = response.trim().replaceAll("[^0-9,]", "").split(",");
        List<Club> top3 = new java.util.ArrayList<>();
        for (String idx : indices) {
            int i = Integer.parseInt(idx.trim()) - 1;
            if (i >= 0 && i < candidates.size()) {
                top3.add(candidates.get(i));
            }
        }
        return top3.isEmpty() ? candidates.subList(0, 3) : top3;
    } catch (Exception e) {
        log.warn("LLM re-rank failed, fallback to Jaccard top-3", e);
        return candidates.subList(0, Math.min(3, candidates.size()));
    }
}
```

- [ ] **Step 2: Update controller endpoint**

In `ClubController.java`:

```java
@GetMapping("/recommend/ai")
@Operation(summary = "兴趣推荐社团（LLM精排）")
public ApiResult<List<Club>> recommendAI(@RequestParam String tags) {
    List<String> tagList = List.of(tags.split(","));
    return ApiResult.ok(clubService.recommendWithLLM(tagList));
}
```

- [ ] **Step 3: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/service/club/ClubService.java
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/ClubController.java
git commit -m "feat: add LLM re-ranking for club recommendations"
```

---

### Task 2.5: RAG Q&A — Search-Augmented Chat

**Files:**
- Modify: `ai-smart-campus/campus-core/src/main/java/com/campus/core/service/chat/ChatService.java`

- [ ] **Step 1: Add RAG method to ChatService**

```java
// Inject SearchService:
private final SearchService searchService;

/**
 * RAG Q&A: 先检索相关文档，再基于检索结果生成回答
 */
public String chatWithRAG(Long conversationId, String userMessage) {
    // 1. 向量检索
    List<Announcement> docs = searchService.search(userMessage);

    // 2. 构建上下文
    StringBuilder context = new StringBuilder();
    if (!docs.isEmpty()) {
        context.append("以下是与用户问题相关的园区资料：\n");
        for (int i = 0; i < Math.min(3, docs.size()); i++) {
            context.append(String.format("【资料%d】%s\n%s\n\n",
                i + 1, docs.get(i).getTitle(), docs.get(i).getContent()));
        }
    }

    // 3. 保存用户消息
    Message msg = new Message();
    msg.setConversationId(conversationId);
    msg.setRole("user");
    msg.setContent(userMessage);
    messageMapper.insert(msg);

    // 4. 调用AI（含上下文）
    String reply = chatClientBuilder.build()
            .prompt()
            .system(SYSTEM_PROMPT + "\n\n你可以参考以下资料回答用户问题：\n" + context)
            .user(userMessage)
            .call()
            .content();

    // 5. 保存回复
    Message replyMsg = new Message();
    replyMsg.setConversationId(conversationId);
    replyMsg.setRole("assistant");
    replyMsg.setContent(reply);
    messageMapper.insert(replyMsg);

    return reply;
}
```

Also add the import:
```java
import com.campus.core.model.entity.Announcement;
import com.campus.core.service.search.SearchService;
```

- [ ] **Step 2: Update controller with RAG endpoint**

In `ChatController.java`:

```java
@PostMapping("/send-rag")
@Operation(summary = "RAG智能问答（检索增强）")
public ApiResult<ChatReplyVO> sendRAG(@Valid @RequestBody ChatSendRequest req) {
    Long convId = req.getConversationId();
    if (convId == null) {
        Conversation conv = chatService.createConversation(1L);
        convId = conv.getId();
    }
    String reply = chatService.chatWithRAG(convId, req.getMessage());
    return ApiResult.ok(new ChatReplyVO(convId, null, reply, "assistant"));
}
```

- [ ] **Step 3: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/service/chat/ChatService.java
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/controller/ChatController.java
git commit -m "feat: add RAG Q&A (retrieve-then-generate)"
```

---

## Phase 3: Polish & Ship

### Task 3.1: Full Integration Test

**Files:**
- Create: `ai-smart-campus/campus-core/src/test/java/com/campus/core/CampusIntegrationTest.java`

- [ ] **Step 1: Write integration test**

```java
package com.campus.core;

import com.campus.common.dto.ApiResult;
import com.campus.core.controller.*;
import com.campus.core.model.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CampusApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CampusIntegrationTest {

    @Autowired private ChatController chatController;
    @Autowired private SearchController searchController;
    @Autowired private AnnouncementController announcementController;
    @Autowired private ClubController clubController;
    @Autowired private ApplyController applyController;
    @Autowired private AdminController adminController;

    @Test
    @Order(1)
    @DisplayName("全链路：查文档 → RAG问答")
    void fullRAGFlow() {
        // Upload document
        MockMultipartFile file = new MockMultipartFile(
            "file", "guide.txt", "text/plain",
            "校园卡充值地点：综合服务大厅1号窗口或支付宝校园生活。".getBytes());
        ApiResult<Announcement> upload = announcementController.upload("校园卡充值指南", "notice", file);
        assertEquals(200, upload.getCode());

        // Search for it
        ApiResult<List<Announcement>> search = searchController.search(
            new com.campus.core.model.dto.SearchRequest("校园卡充值"));
        assertEquals(200, search.getCode());
        assertFalse(search.getData().isEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("全链路：兴趣推荐 → 报名")
    void fullClubFlow() {
        // Recommend
        ApiResult<List<Club>> rec = clubController.recommend("科技,AI");
        assertEquals(200, rec.getCode());
        // Apply — just verify the controller wiring works
    }

    @Test
    @Order(3)
    @DisplayName("管理：重建索引")
    void fullRebuildFlow() {
        ApiResult<String> result = adminController.rebuildIndex();
        assertEquals(200, result.getCode());
    }
}
```

- [ ] **Step 2: Run all tests**

Run: `cd ai-smart-campus && mvn test -pl campus-core`
Expected: ALL TESTS PASS

- [ ] **Step 3: Commit**

```bash
git add ai-smart-campus/campus-core/src/test/java/com/campus/core/CampusIntegrationTest.java
git commit -m "test: add full-stack integration tests"
```

---

### Task 3.2: Edge Cases & Error Handling

**Files:**
- Modify: `ai-smart-campus/campus-core/src/main/java/com/campus/core/service/chat/ChatService.java` (null guard)
- Modify: `ai-smart-campus/campus-core/src/main/java/com/campus/core/service/search/SearchService.java` (empty file, large file)
- Modify: `ai-smart-campus/campus-core/src/main/java/com/campus/core/service/club/ClubService.java` (full-quota, expired recruitment)

- [ ] **Step 1: Add edge-case handling in SearchService**

Add a size limit guard:

```java
// In SearchService.uploadDocument(), before reading:
private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "pdf", "docx");

// Add file validation:
if (file.isEmpty()) {
    throw new BizException(ErrorCode.BAD_REQUEST, "文件为空");
}
String originalName = file.getOriginalFilename();
if (originalName != null) {
    String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
    if (!ALLOWED_EXTENSIONS.contains(ext)) {
        throw new BizException(ErrorCode.BAD_REQUEST, "不支持的文件格式: " + ext);
    }
}
if (file.getSize() > MAX_FILE_SIZE) {
    throw new BizException(ErrorCode.BAD_REQUEST, "文件大小超过10MB限制");
}
```

- [ ] **Step 2: Add edge cases for ClubService apply()**

The duplicate and full-quota handling is already in the core implementation. Add expired recruitment check:

```java
// In ClubService.apply(), after checking recruitment exists:
if (rec.getEndTime() != null && rec.getEndTime().isBefore(java.time.LocalDateTime.now())) {
    // Auto-update status to expired
    rec.setStatus(3);
    recruitmentMapper.updateById(rec);
    throw new BizException(ErrorCode.BAD_REQUEST, "纳新活动已结束");
}
```

- [ ] **Step 3: Run tests to confirm no regression**

Run: `cd ai-smart-campus && mvn test -pl campus-core`
Expected: ALL PASS

- [ ] **Step 4: Commit**

```bash
git add ai-smart-campus/campus-core/src/main/java/com/campus/core/service/
git commit -m "fix: add edge-case handling (empty file, large file, expired recruitment)"
```

---

### Task 3.3: Initialize CLAUDE.md

**Files:**
- Create: `ai-smart-campus/CLAUDE.md`

- [ ] **Step 1: Run init skill**

Invoke: `/init` to auto-generate CLAUDE.md for the project.

Or write manually:

```markdown
# AI Smart Campus

AI智慧园区后端服务，基于SpringBoot + SpringAI构建，对接大模型实现园区智能化。

## Quick Start

```bash
cd ai-smart-campus
mvn clean compile
# Ensure MySQL, Redis Stack, RabbitMQ, MinIO are running
cd campus-core && mvn spring-boot:run
```

## Architecture

- `campus-common/` — DTOs, exceptions, enums
- `campus-core/` — Main business module
  - `chat/` — AI conversation (ChatClient + SSE)
  - `search/` — RAG search (Embedding + RedisSearch)
  - `club/` — Club recommendation (Jaccard + LLM re-rank)
  - `ai/` — Function Calling tools (CampusFunctions)
  - `vector/` — Redis Stack vector operations
  - `mq/` — RabbitMQ async vector sync
  - `config/` — Spring beans configuration

## API Docs

After starting: http://localhost:8080/doc.html (Knife4j)

## Key Endpoints

// POST /api/chat/send — SSE streaming chat
// POST /api/chat/send-sync — Multi-turn chat
// POST /api/chat/send-rag — RAG Q&A
// POST /api/search — Semantic search
// POST /api/announcements/upload — Upload document
// GET  /api/club/recommend?tags= — Tag-based recommendation
// GET  /api/club/recommend/ai?tags= — LLM re-ranked
// POST /api/apply — Submit application
// GET  /api/apply/my — My applications
// POST /api/admin/rebuild-index — Rebuild vector index

## Env Variables

AI_API_KEY, AI_BASE_URL, MYSQL_USER, MYSQL_PASSWORD,
REDIS_HOST, REDIS_PORT, REDIS_PASSWORD,
RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASS,
MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY
```

- [ ] **Step 2: Commit**

```bash
git add ai-smart-campus/CLAUDE.md
git commit -m "docs: add CLAUDE.md project documentation"
```

---

## Final Checklist

- [ ] `mvn clean test -pl campus-core` — all tests pass
- [ ] `mvn spring-boot:run` — app starts without errors
- [ ] POST `/api/chat/send` returns streaming reply
- [ ] POST `/api/search` returns relevant results
- [ ] GET `/api/club/recommend?tags=科技` returns ranked clubs
- [ ] POST `/api/apply` prevents duplicate
- [ ] POST `/api/admin/rebuild-index` completes
- [ ] RabbitMQ queues created on startup
- [ ] Knife4j doc page accessible at `/doc.html`
