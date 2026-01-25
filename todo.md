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

- [x] **Multi-Agent System（多 Agent 协作）**
  - [x] Supervisor 模式（主控 Agent）
  - [x] Peer 模式（平等协作）
  - [x] 手off 机制
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

### 最近更新 (2026-01-25)

#### 修复与重构 (Refactoring & Fixes)
- ✅ **Tool 概念统一**：将 `Tool` 接口重命名为 `McpTool`，统一 Core 和 MCP 层的工具抽象，消除歧义。
- ✅ **OpenAI 兼容性修复**：修复 `OpenAIChatModel` 中 `tool_call_id` 缺失导致的 400 错误。
- ✅ **前端配置适配**：修复 `AgentNode` 无法识别前端扁平化 `tools` ("serverId/toolName") 配置的问题。
- ✅ **运行时修复**：修复 `ExecutionEventType` 缺失 `TOOL_CALL` 枚举值导致的异常。
- ✅ **构建修复**：消除 Lombok `@Builder` 命名冲突和所有编译错误。

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
- ✅ **完成 Multi-Agent System（多 Agent 协作）**
- ✅ **完成真实 API 集成测试（Multi-Agent）**
- ✅ **完成 MCP (Model Context Protocol) 支持**

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
- `core/multiagent/` - 多 Agent 系统（5 个文件）：
  - `AgentNode` - Agent 节点包装
  - `AgentHandoff` - 手 off 消息
  - `HandoffStrategy` - 策略接口
  - `SupervisorStrategy` - LLM 路由策略
  - `MultiAgentSystem` - 多 Agent 协调器
- `core/mcp/` - MCP 协议支持（10 个文件）：
  - `McpProtocol` - MCP 协议类型枚举（STDIO, SSE, WS）
  - `McpConfig` - MCP 配置类（支持 Builder）
  - `McpException` - MCP 异常类
  - `McpClient` - MCP 客户端（实现 JSON-RPC 通信）
  - `McpToolDescription` - MCP 工具描述
  - `McpToolResult` - MCP 工具执行结果
  - `McpResourceDescription` - MCP 资源描述
  - `McpPromptDescription` - MCP 提示词描述
  - `McpTool` - MCP 工具适配器（实现 Tool 接口）
  - `McpToolRegistry` - MCP 工具注册器（自动发现和注册）
- `test/core/agent/` - 测试类（3 个文件）：
  - `AgentIntegrationTest` - Mock 模型集成测试（18 个测试用例）
  - `OpenAIIntegrationTest` - 真实 API 集成测试
  - `MockWeatherTool` - 测试用工具
- `test/examples/` - 示例类（4 个文件）：
  - `StateInjectionExample` - 状态注入中间件示例（8 个示例）
  - `MultiAgentExample` - 多 Agent 系统示例（5 个示例）
  - `OpenAIMultiAgentTest` - Multi-Agent 真实 API 测试（7 个测试用例）
  - `McpExample` - MCP 使用示例（5 个示例）

### Multi-Agent 系统测试结果

| 测试用例 | 耗时 | 结果 |
|---------|------|------|
| 基础双 Agent 协作 | 2334ms | ✅ 成功 |
| 三 Agent 工作流（分析→开发→审查） | 3066ms | ✅ 成功 |
| 专业分工（搜索→写作） | 2935ms | ✅ 成功 |

### 下一步计划
1. **运行真实 API 测试** - 验证与 OpenAI 的集成
2. **MCP 真实服务器测试** - 测试与实际 MCP 服务器的集成 ✅
3. **阶段六** - 实现存储和优化功能
4. **阶段七** - Spring Boot 自动配置和示例
5. **阶段八** - 高级功能（可选）

### MCP 测试结果

使用 FastMCP Demo 服务器测试（localhost:8000/mcp）：

| 测试用例 | 耗时 | 结果 |
|---------|------|------|
| MCP 工具发现 | - | ✅ 成功 |
| Agent + MCP 加法 | 532ms | ✅ 412 |
| 多次计算 | 814ms | ✅ 300, 1850, 110 |
| 复杂任务计算 | - | ✅ 成功 |
| 错误处理 | - | ✅ 成功 |

### MCP 支持说明

**MCP (Model Context Protocol)** 是一个开放协议，允许 AI 应用连接到外部数据源和工具。

**支持的协议类型：**
- **STDIO** - 通过标准输入输出通信（适用于本地 npx 包）
- **SSE** - 通过 Server-Sent Events 通信（HTTP 服务器）
- **WS** - 通过 WebSocket 通信（实时双向交互）

**使用方式：**
```java
Agent agent = new DefaultAgentFactory().createAgent()
    .model(chatModel)
    .mcp(McpConfig.builder()
        .protocol(McpProtocol.STDIO)
        .entrypoint("npx -y @modelcontextprotocol/server-filesystem /path/to/files")
        .build())
    .build();
```

**官方 MCP 服务器示例：**
- `@modelcontextprotocol/server-filesystem` - 文件系统访问
- `@modelcontextprotocol/server-postgres` - PostgreSQL 数据库
- `@modelcontextprotocol/server-github` - GitHub 集成
- `@modelcontextprotocol/server-brave-search` - Brave 搜索

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

---

## 🚀 工作流平台开发计划（2026-01-25 新增）

### 项目定位
开发一个类似 **Dify** 的可视化工作流编排平台，基于 KnightAgent 框架。

### 模块结构
```
knight-agent/
├── src/main/java/org/cloudnook/knightagent/
│   ├── core/              # Agent 框架（已完成）
│   ├── workflow/          # 工作流引擎（新增）
│   └── api/               # REST API（新增）
└── knight-agent-web/      # 前端项目（独立仓库）
```

### 开发阶段

#### 阶段 W1：工作流引擎核心 (已完成 ✅)
- [x] **节点抽象层**
  - [x] `WorkflowNode` 接口
  - [x] `NodeConfig` 配置类
  - [x] `NodeContext` 执行上下文
  - [x] `NodeType` 类型枚举

- [x] **工作流定义**
  - [x] `WorkflowDefinition` DSL
  - [x] `WorkflowEdge` 连接定义
  - [x] `WorkflowParser` JSON 解析器
  - [x] 节点依赖拓扑排序

- [x] **执行引擎**
  - [x] `WorkflowEngine` 执行器
  - [x] `ExecutionResult` 结果封装
  - [x] 同步执行模式
  - [x] 错误处理和回滚

#### 阶段 W2：内置节点实现 (已完成 ✅)
- [x] **基础节点**
  - [x] `InputNode` - 输入节点
  - [x] `OutputNode` - 输出节点
  - [x] `CodeNode` - JavaScript 代码执行
  - [x] `ConditionNode` - 条件分支

- [x] **Agent 节点**
  - [x] `AgentNode` - 集成 Agent 框架
  - [x] 支持提示词配置
  - [x] 支持 MCP 工具选择
  - [x] 支持模型参数配置

- [x] **工具节点**
  - [x] `ToolNode` - 单独工具调用
  - [x] `HttpNode` - HTTP 请求

#### 阶段 W3：后端 API (后端部分已完成 ✅)
- [x] **工作流 CRUD**
  - [x] `WorkflowController`
  - [x] `WorkflowService`
  - [x] `WorkflowRepository`（Postgres/JPA）
  - [x] 版本管理

- [x] **执行 API**
  - [x] `ExecutionController`
  - [x] 同步执行接口
  - [x] 执行历史查询

- [x] **WebSocket 支持**
  - [x] `WebSocketController`
  - [x] 流式推送执行状态
  - [x] 节点进度通知

- [x] **MCP 集成**
  - [x] MCP 服务器管理 (`McpServerService`)
  - [x] 工具列表查询
  - [x] 工具动态加载

#### 阶段 W4：前端开发（v0.dev）
- [ ] **基础框架**
  - [ ] Next.js 15 项目初始化
  - [ ] shadcn/ui 组件库
  - [ ] TypeScript 配置

- [ ] **画布组件**
  - [ ] ReactFlow 集成
  - [ ] 节点拖拽
  - [ ] 连线创建
  - [ ] 缩放/平移控制
  - [ ] 网格背景

- [ ] **节点组件**
  - [ ] 节点卡片渲染
  - [ ] 节点类型图标
  - [ ] 输入/输出锚点
  - [ ] 节点状态显示

- [ ] **配置面板**
  - [ ] Agent 节点配置
    - [ ] 提示词编辑器（Monaco Editor）
    - [ ] MCP 工具多选
    - [ ] 模型参数表单
  - [ ] 其他节点配置

#### 阶段 W5：前后端联调
- [ ] **API 集成**
  - [ ] 工作流保存/加载
  - [ ] 执行触发
  - [ ] WebSocket 连接

- [ ] **执行监控**
  - [ ] 实时节点状态
  - [ ] 流式输出显示
  - [ ] 错误提示

#### 阶段 W6：完善和优化
- [ ] **用户体验**
  - [ ] 快捷键支持
  - [ ] 撤销/重做
  - [ ] 复制/粘贴节点
  - [ ] 画布导出（PNG）

- [ ] **高级功能**
  - [ ] 工作流模板
  - [ ] 节点分组
  - [ ] 执行日志
  - [ ] 性能监控

### API 接口定义

```java
// 工作流定义（保存格式）
{
  "id": "wf_xxx",
  "name": "示例工作流",
  "description": "描述",
  "nodes": [
    {
      "id": "node_1",
      "type": "agent",
      "position": { "x": 100, "y": 100 },
      "config": {
        "model": "gpt-4",
        "systemPrompt": "你是一个助手",
        "mcpTools": ["tool1", "tool2"],
        "temperature": 0.7
      }
    }
  ],
  "edges": [
    {
      "id": "edge_1",
      "source": "node_1",
      "target": "node_2",
      "sourceHandle": "output",
      "targetHandle": "input"
    }
  ]
}
```
