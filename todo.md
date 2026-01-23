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
  - 消息序列化/反序列化

- [x] **工具接口 (`Tool`)**
  - 定义工具调用规范
  - 工具元数据（名称、描述、参数 schema）
  - `ToolInvoker` 工具执行器

- [x] **流式接口 (`StreamCallback`)**
  - 定义流式输出回调
  - 支持增量 token、工具调用事件、思考链输出

---

## ✅ 阶段二：状态和持久化 (已完成)

- [x] **状态管理 (`AgentState`)**
  - 状态接口定义
  - 状态更新机制（reducers）
  - 自定义 StateSchema 支持

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
  - 工具调用循环
  - 消息构建和发送
  - 结果解析和状态更新

- [x] **Agent 配置 (`AgentConfig`)**
  - Thread ID 配置
  - 执行参数配置

---

## 阶段四：中间件系统

- [ ] **中间件接口 (`Middleware`)**
  - 定义拦截点：beforeInvoke, afterInvoke, onToolCall, onStateUpdate
  - 中间件链执行器

- [ ] **内置中间件 - 对话摘要 (`SummarizationMiddleware`)**
  - Token 计数
  - 自动摘要历史消息

- [ ] **内置中间件 - 人机协作 (`HumanInTheLoopMiddleware`)**
  - 工具调用拦截
  - 审批配置（允许接受、编辑、拒绝）

- [ ] **内置中间件 - 状态注入 (`StateInjectionMiddleware`)**
  - 动态 prompt 注入
  - 上下文增强

---

## 阶段五：高层 API

- [ ] **`create_agent` 工厂方法**
  - Fluent Builder API
  - 参数：model, tools, stateSchema, middleware, checkpointer

- [ ] **预构建 Agent 模板**
  - `ReActAgent` - 推理-行动循环
  - `ToolCallingAgent` - 工具调用专用

---

## 阶段六：存储和优化

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

## 阶段七：集成和示例

- [ ] **Spring Boot 自动配置**
  - 自动装配核心组件
  - 配置属性类

- [ ] **示例 Agent**
  - 简单问答 Agent
  - 工具调用 Agent（如天气查询）
  - 多轮对话 Agent

- [ ] **集成测试**
  - 端到端测试套件
  - 性能基准测试

---

## 阶段八：(可选) 高级功能

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

## 开发优先级

> 建议按以下顺序开发，每个阶段完成后可以测试和验证：

1. **阶段一** → 基础数据结构和接口
2. **阶段二** → 状态持久化（先 InMemory，后 Postgres）
3. **阶段三** → 核心 Agent 执行逻辑
4. **阶段四** → 中间件系统（核心功能）
5. **阶段五** → 高层 API（易用性）
6. **阶段六** → 生产就绪特性
7. **阶段七** → 完善和示例
8. **阶段八** → 高级特性（可选）
