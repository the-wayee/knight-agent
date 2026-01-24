# TODO - KnightAgent 开发任务清单

> 完成一个功能后，从列表中移除对应的 TODO 项

---

## ✅ 阶段一：基础设施层 (已完成)

- [x] **核心模型接口 (`ChatModel`)**
  - 定义 LLM 调用的统一接口
  - 支持同步和异步调用
  - 后续实现：OpenAIModel, AnthropicModel, OllamaModel 等

- [x] **消息类型定义**
  - `Message` 基类及子类：`HumanMessage`, `AIMessage`, `SystemMessage`, `ToolMessage`
  - 消息序列化/反序列化（Jackson 多态支持）

- [x] **工具接口 (`Tool`)**
  - 定义工具调用规范
  - 工具元数据（名称、描述、参数 schema）
  - `ToolInvoker` 工具执行器（共享线程池 + 生命周期管理）

- [x] **流式接口 (`StreamCallback`)**
  - 定义流式输出回调
  - 支持增量 token、工具调用事件、思考链输出

---

## ✅ 阶段二：状态和持久化 (已完成)

- [x] **状态管理 (`AgentState`)**
  - 状态接口定义
  - 状态更新机制（reducers）
  - 消息反序列化完成

- [x] **Checkpoint 接口 (`Checkpointer`)**
  - 定义状态快照接口
  - Thread 和 CheckpointId 概念

- [x] **内存 Checkpointer 实现 (`InMemorySaver`)**
  - 基于 ConcurrentHashMap 的内存存储
  - 用于测试和快速原型

- [x] **PostgreSQL Checkpointer 实现 (`PostgresSaver`)**
  - 数据库表设计
  - 状态序列化存储（JSONB）
  - 事务支持

- [ ] **(可选) Redis Checkpointer 实现 (`RedisSaver`)**
  - 基于 Redis 的分布式缓存存储

---

## ✅ 阶段三：Agent 执行引擎 (已完成)

- [x] **Agent 核心接口 (`Agent`)**
  - `invoke()` 同步执行
  - `stream()` 流式执行
  - `batch()` 批量执行

- [x] **Agent 执行器 (`AgentExecutor`)**
  - 实现 Agent 接口
  - 工具调用循环
  - 消息构建和发送
  - 结果解析和状态更新

- [x] **Agent 配置 (`AgentConfig`)**
  - Thread ID 配置
  - 执行参数配置

---

## ✅ 阶段四：中间件系统 (已完成)

- [x] **中间件接口 (`Middleware`)**
  - 定义拦截点：beforeInvoke, afterInvoke, beforeToolCall, afterToolCall, onStateUpdate
  - 中间件链执行器（`MiddlewareChain`）

- [x] **内置中间件 - 日志记录 (`LoggingMiddleware`)**
  - 记录所有 Agent 执行过程
  - 记录工具调用和结果
  - 可配置的日志级别

- [x] **内置中间件 - 对话摘要 (`SummarizationMiddleware`)**
  - Token 计数
  - 自动摘要历史消息
  - 保留最近消息和系统提示词

- [x] **内置中间件 - 人机协作 (`HumanInTheLoopMiddleware`)**
  - 工具调用拦截
  - 审批配置（ALLOW、REJECT、EDIT）
  - 终端提示和自定义回调支持

- [x] **内置中间件 - 状态注入 (`StateInjectionMiddleware`)**
  - 动态 prompt 注入
  - 变量替换（${state:key}、${request:key}、${context:key}）
  - 多种注入模式（PREFIX、SUFFIX、REPLACE、OVERRIDE）
  - 自定义注入器支持

---

## ✅ 阶段五：高层 API (已完成)

- [x] **`create_agent` 工厂方法**
  - Fluent Builder API
  - `AgentBuilder` 类
  - `DefaultAgentFactory` 实现

- [x] **执行策略抽象**
  - `ExecutionStrategy` 接口
  - `ExecutionContext` 上下文
  - `ReActStrategy` 实现

- [x] **模型抽象层**
  - `BaseChatModel` 抽象基类
  - `OpenAIChatModel` 继承重构

---

## 🚧 阶段五+：高级 Agent 类型

### 工厂模式扩展

- [ ] **CachingAgentFactory**
  - 缓存已创建的 Agent 实例
  - 基于配置哈希的缓存键
  - 支持缓存过期和清理

- [ ] **ConfigurationBasedAgentFactory**
  - 从配置类自动装配 Agent
  - Spring Boot 自动配置支持
  - 支持多配置源（YAML、Properties）

- [ ] **PoolAgentFactory**
  - Agent 对象池管理
  - 适用于高并发场景
  - 连接池配置（最小/最大/空闲）

### 高级执行策略

- [ ] **Plan-and-Execute Strategy**
  - 规划器（Planner）：生成执行计划
  - 执行器（Executor）：按计划执行步骤
  - 支持动态调整计划
  - 适合复杂任务分解

- [ ] **ReWOO Strategy**
  - 一次推理生成所有工具调用
  - 批量执行工具调用
  - 减少 LLM 调用次数
  - 提高执行效率

- [ ] **Reflection Strategy**
  - 主 Agent 生成答案
  - 反思 Agent 评审和改进
  - 多轮自我完善
  - 适合需要高质量输出的场景

### 复杂 Agent 类型

- [ ] **RAG Agent（检索增强生成）**
  - 向量检索集成
  - 文档切片管理
  - 增强提示词模板
  - 适用于知识库问答

- [ ] **Multi-Agent System（多 Agent 协作）**
  - Supervisor 模式（主控 Agent）
  - Peer 模式（平等协作）
  - 手off 机制
  - 适用于复杂任务分工

- [ ] **Agentic Workflow（工作流 Agent）**
  - 有向图定义工作流
  - 节点和边配置
  - 条件分支
  - 循环和子工作流

---

## ⏳ 阶段六：存储和优化

- [ ] **对话存储接口 (`ConversationStore`)**
  - 对话历史 CRUD
  - 实现：`InMemoryStore`, `PostgresStore`

- [ ] **Token 压缩器 (`TokenCompressor`)**
  - 智能摘要算法
  - 保留关键信息的策略

- [ ] **时间旅行支持**
  - 从历史 Checkpoint 恢复
  - 分支探索

---

## ⏳ 阶段七：集成和示例

- [ ] **Spring Boot 自动配置**
  - 自动装配核心组件
  - 配置属性类
  - `@EnableAgents` 注解

- [ ] **示例 Agent**
  - 简单问答 Agent
  - 工具调用 Agent（如天气查询）
  - 多轮对话 Agent

- [ ] **集成测试**
  - 端到端测试套件
  - 性能基准测试

---

## ⏳ 阶段八：(可选) 高级功能

- [ ] **统一异常系统**
  - [x] `KnightAgentException` 基类
  - [x] `ErrorCode` 枚举（20+ 错误码）
  - [ ] 异常处理器

- [ ] **多 Agent 协作**
  - Agent 之间的通信
  - Agent 手off 机制

- [ ] **子 Agent 生成 (`SubAgentMiddleware`)**
  - 动态创建子任务
  - 结果聚合

- [ ] **LangSmith 集成**
  - 追踪和监控
  - 评估支持

---

## 最近更新 (2026-01-25)

### 已完成的重构
- ✅ 修复 `AgentState.fromBytes()` 消息反序列化
- ✅ 修复 `ToolInvoker` 线程池泄漏（共享线程池 + shutdown）
- ✅ `AgentExecutor` 实现 `Agent` 接口
- ✅ 引入 `ExecutionStrategy` 策略模式 + `ReActStrategy`
- ✅ 实现 `AgentFactory` + `AgentBuilder`
- ✅ 创建 `BaseChatModel` 抽象基类
- ✅ `ChatOptions` 添加 `toBuilder` 支持
- ✅ 统一异常系统（`KnightAgentException` + `ErrorCode`）
- ✅ 修复 `AIMessage` 不可变性
- ✅ **完成阶段四：中间件系统（4 个内置中间件）**

### 新增文件
- `core/agent/strategy/` - 执行策略（3 个文件）
- `core/agent/factory/` - 工厂模式（3 个文件）
- `core/model/base/` - 模型基类（1 个文件）
- `core/exception/` - 异常系统（2 个文件）
- `core/middleware/builtin/` - 内置中间件（4 个文件）：
  - `LoggingMiddleware` - 日志记录
  - `SummarizationMiddleware` - 对话摘要
  - `HumanInTheLoopMiddleware` - 人机协作
  - `StateInjectionMiddleware` - 状态注入
- `test/core/agent/` - 测试类（3 个文件）：
  - `AgentIntegrationTest` - Mock 模型集成测试（18 个测试用例）
  - `OpenAIIntegrationTest` - 真实 API 集成测试
  - `MockWeatherTool` - 测试用工具

### 下一步计划
1. **运行真实 API 测试** - 验证与 OpenAI 的集成
2. **阶段六** - 实现存储和优化功能
3. **阶段七** - Spring Boot 自动配置和示例
4. **阶段八** - 高级功能（可选）

---

## 开发优先级

> 建议按以下顺序开发，每个阶段完成后可以测试和验证：

1. **阶段一** ✅ → 基础数据结构和接口
2. **阶段二** ✅ → 状态持久化（先 InMemory，后 Postgres）
3. **阶段三** ✅ → 核心 Agent 执行逻辑
4. **阶段四** ✅ → 中间件系统（核心功能）
5. **阶段五** ✅ → 高层 API（易用性）
6. **集成测试** ✅ → 单元测试 + 真实 API 测试
7. **阶段六** ⏳ → 生产就绪特性
8. **阶段七** ⏳ → 完善和示例
9. **阶段八** ⏳ → 高级特性（可选）
