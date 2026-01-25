c lu# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 项目愿景

**KnightAgent** 的使命是成为 **最易用的 Java AI Agent 与工作流平台**。

我们相信 AI Agent 的力量不应该被编程门槛所限制。KnightAgent 让：
- **开发者** 可以通过代码快速构建复杂的 Agent 应用
- **非技术用户** 可以通过可视化界面拖拽创建 AI 工作流
- **企业** 可以将 AI 能力集成到现有业务流程中

### 核心价值

| 价值 | 描述 |
|------|------|
| **低门槛** | 拖拽式编排，无需编码即可创建 AI 应用 |
| **高灵活性** | 支持代码扩展，满足复杂定制需求 |
| **可扩展** | MCP 协议连接无限外部工具 |
| **生产就绪** | 企业级持久化、监控、版本管理 |

### 与 Dify 的差异

| 特性 | Dify | KnightAgent |
|------|------|-------------|
| 后端语言 | Python | **Java** |
| 目标用户 | 业务用户 | **开发者 + 业务用户** |
| 编程模型 | 闭源 | **开源框架** |
| 部署方式 | 云服务为主 | **私有化部署友好** |
| MCP 支持 | 部分 | **完整支持** |

### 理想使用场景

```
场景 1: 开发者快速原型
├── 用 Java 代码创建 Agent
├── 配置 MCP 工具连接数据源
└── 通过 API 集成到现有系统

场景 2: 业务用户自动化流程
├── 拖拽创建工作流
├── 配置 Agent 提示词
└── 一键执行，实时查看结果

场景 3: 企业级 AI 平台
├── 多用户协作
├── 工作流版本管理
└── 私有化部署，数据安全
```

---

## 项目概述

**KnightAgent** 是一个 **Agent & Workflow 可视化平台**，提供：

1. **Agent 框架**（core/）- 轻量级 Java Agent 框架，参考 2025 年 10 月底发布的 LangChain 架构设计
2. **工作流引擎**（workflow/）- 可视化工作流编排和执行引擎
3. **Web 平台**（api/ + web/）- 前后端分离的 No-Code/Low-Code 平台

### Agent 框架能力
- **流式返回** - 实时 token 流式输出
- **Agent 创建** - `create_agent` 风格的工厂方法
- **工具调用** - 函数/工具调用能力
- **中间件拦截** - 可组合的中间件：prompt 工程、护栏、状态管理、对话摘要
- **对话持久化** - 对话历史持久化存储
- **Token 压缩** - 自动摘要管理上下文窗口限制
- **运行时状态持久化** - 自定义状态的序列化
- **Checkpoint 机制** - 基于 Thread 的状态快照：时间旅行、人机协作、容错恢复
- **Multi-Agent 协作** - 多 Agent 协作完成复杂任务
- **MCP 协议** - Model Context Protocol 支持，连接外部工具

### 工作流平台能力
- **可视化编排** - 拖拽式节点画布（类似 Dify、LangFlow）
- **多种节点类型** - Agent 节点、代码节点、条件分支、工具调用、HTTP 请求等
- **自定义配置** - Agent 节点支持自定义提示词、MCP 工具、模型参数
- **实时执行** - WebSocket 流式返回执行结果
- **版本管理** - 工作流版本控制和历史记录

## 技术栈

- **Java 17** - 最低 Java 版本
- **Spring Boot 3.5.10** - 应用框架
- **PostgreSQL** - 持久化存储（可选）
- **Lombok** - 代码生成
- **Maven** - 构建工具

**架构选择：多模块 Maven 项目**
```
knight-agent/
├── knight-agent-core/     # Agent 框架（已完成）
├── knight-agent-workflow/ # 工作流引擎（开发中）
├── knight-agent-api/      # 后端 REST API（开发中）
└── knight-agent-web/      # 前端项目（独立仓库，开发中）
```

---

## 当前开发进度（截至 2026-01-25）

### ✅ 已完成阶段

#### 阶段一：基础设施层 (100%)
```
core/
├── message/          # 消息类型系统
│   ├── Message.java           # 消息基类
│   ├── SystemMessage.java     # 系统消息
│   ├── HumanMessage.java      # 用户消息
│   ├── AIMessage.java         # AI 消息（含工具调用）
│   ├── ToolMessage.java       # 工具结果消息
│   ├── ToolCall.java          # 工具调用定义
│   └── ToolResult.java        # 工具执行结果
├── model/             # 模型抽象层
│   ├── ChatModel.java         # 统一 LLM 调用接口
│   ├── ChatOptions.java       # 调用参数配置
│   ├── ModelCapabilities.java # 模型能力描述
│   ├── ModelException.java    # 模型异常
│   └── base/                  # 模型基类
│       └── BaseChatModel.java
├── tool/              # 工具系统
│   ├── Tool.java              # 工具接口
│   ├── AbstractTool.java      # 工具抽象基类
│   ├── ToolExecutionException.java  # 工具异常
│   └── ToolInvoker.java       # 工具执行器
└── streaming/         # 流式接口
    ├── StreamCallback.java    # 流式回调接口
    ├── StreamCallbackAdapter.java  # 适配器
    └── StreamEvent.java       # 流式事件封装
```

#### 阶段二：状态和持久化 (100%)
```
core/
├── state/             # 状态管理
│   ├── AgentState.java              # 不可变状态（Builder 模式）
│   ├── StateReducer.java            # 状态归约器（函数式）
│   ├── StateContext.java            # 状态上下文
│   └── StateSerializationException.java
└── checkpoint/        # 检查点系统
    ├── Checkpointer.java            # 检查点接口
    ├── CheckpointInfo.java          # 检查点元数据
    ├── CheckpointException.java     # 检查点异常
    ├── InMemorySaver.java           # 内存实现
    └── PostgresSaver.java           # PostgreSQL 实现
```

#### 阶段三：Agent 执行引擎 (100%)
```
core/
├── agent/             # Agent 核心系统
│   ├── Agent.java                 # Agent 接口
│   ├── AgentRequest.java          # 请求封装
│   ├── AgentResponse.java         # 响应封装
│   ├── AgentConfig.java           # 配置类（Builder 模式）
│   ├── AgentStatus.java           # 运行状态
│   ├── AgentExecutionException.java
│   ├── AgentExecutor.java         # 执行引擎（ReAct 循环）
│   ├── factory/                   # 工厂模式
│   │   ├── AgentFactory.java      # 工厂接口
│   │   ├── AgentBuilder.java      # Builder 类
│   │   └── DefaultAgentFactory.java  # 默认实现
│   └── strategy/                  # 执行策略
│       ├── ExecutionStrategy.java  # 策略接口
│       ├── ExecutionContext.java    # 执行上下文
│       └── ReActStrategy.java       # ReAct 策略实现
```

#### 阶段四：中间件系统 (100%)
```
core/
└── middleware/        # 中间件系统
    ├── Middleware.java            # 中间件接口（5 个拦截点）
    ├── AgentContext.java          # 中间件上下文
    ├── MiddlewareException.java
    ├── MiddlewareChain.java       # 中间件链执行器
    └── builtin/                   # 内置中间件
        ├── LoggingMiddleware           # 日志记录
        ├── SummarizationMiddleware    # Token 压缩
        ├── HumanInTheLoopMiddleware    # 人机协作
        └── StateInjectionMiddleware     # 状态注入
```

#### 阶段五：高层 API (100%)
```
core/
└── agent/
    ├── factory/                   # 工厂模式
    │   ├── AgentFactory.java      # 工厂接口
    │   ├── AgentBuilder.java      # Builder 类
    │   └── DefaultAgentFactory.java  # 默认实现
    └── strategy/                  # 执行策略
        ├── ExecutionStrategy.java  # 策略接口
        ├── ExecutionContext.java    # 执行上下文
        └── ReActStrategy.java       # ReAct 策略实现
```

#### 阶段五+：Multi-Agent 系统 (100%)
```
core/
└── multiagent/        # 多 Agent 系统
    ├── AgentNode.java             # Agent 节点包装
    ├── AgentHandoff.java          # 手 off 消息
    ├── HandoffStrategy.java       # 路由策略接口
    ├── SupervisorStrategy.java    # LLM 决策路由
    └── MultiAgentSystem.java      # 多 Agent 协调器
```

---

## 核心代码结构

```
src/main/java/org/cloudnook/knightagent/
├── KnightAgentApplication.java    # Spring Boot 入口
└── core/
    ├── agent/
    │   ├── Agent.java                 # Agent 接口
    │   ├── AgentRequest.java          # .input, .threadId, .userId
    │   ├── AgentResponse.java         # .output, .messages, .state
    │   ├── AgentConfig.java           # 配置：systemPrompt, maxIterations
    │   ├── AgentStatus.java           # 状态：IDLE, RUNNING, ERROR
    │   ├── AgentExecutionException.java
    │   ├── AgentExecutor.java         # 核心执行逻辑
    │   ├── factory/                   # 工厂模式
    │   │   ├── AgentFactory.java
    │   │   ├── AgentBuilder.java
    │   │   └── DefaultAgentFactory.java
    │   └── strategy/                  # 执行策略
    │       ├── ExecutionStrategy.java
    │       ├── ExecutionContext.java
    │       └── ReActStrategy.java
    ├── checkpoint/
    │   ├── Checkpointer.java         # .save(), .load(), .list()
    │   ├── CheckpointInfo.java       # 检查点元数据
    │   ├── CheckpointException.java
    │   ├── InMemorySaver.java        # 内存实现（测试用）
    │   └── PostgresSaver.java        # PostgreSQL 实现
    ├── exception/                   # 统一异常系统
    │   ├── ErrorCode.java            # 错误码枚举
    │   └── KnightAgentException.java # 基类异常
    ├── message/
    │   ├── Message.java               # 抽象基类
    │   ├── SystemMessage.java
    │   ├── HumanMessage.java
    │   ├── AIMessage.java            # .toolCalls, .reasoning
    │   ├── ToolMessage.java
    │   ├── ToolCall.java
    │   └── ToolResult.java
    ├── middleware/
    │   ├── Middleware.java            # 5 个拦截点
    │   ├── AgentContext.java
    │   ├── MiddlewareChain.java
    │   └── builtin/                   # 内置中间件
    │       ├── LoggingMiddleware
    │       ├── SummarizationMiddleware
    │       ├── HumanInTheLoopMiddleware
    │       └── StateInjectionMiddleware
    ├── model/
    │   ├── ChatModel.java            # .chat(), .chatStream(), .countTokens()
    │   ├── ChatOptions.java          # temperature, maxTokens, topP
    │   ├── ModelCapabilities.java    # 模型能力描述
    │   ├── ModelException.java
    │   └── base/
    │       └── BaseChatModel.java   # 模型抽象基类
    ├── multiagent/                   # Multi-Agent 系统
    │   ├── AgentNode.java            # Agent 节点包装
    │   ├── AgentHandoff.java         # 手 off 消息
    │   ├── HandoffStrategy.java      # 路由策略接口
    │   ├── SupervisorStrategy.java   # LLM 决策路由
    │   └── MultiAgentSystem.java     # 多 Agent 协调器
    ├── state/
    │   ├── AgentState.java           # 不可变状态，支持 .addMessage()
    │   ├── StateReducer.java         # 甯一状态归约器
    │   ├── StateContext.java
    │   └── StateSerializationException.java
    ├── streaming/
    │   ├── StreamCallback.java       # .onToken(), .onToolCall()
    │   ├── StreamCallbackAdapter.java
    │   └── StreamEvent.java
    └── tool/
        ├── Tool.java                  # 工具接口
        ├── AbstractTool.java          # 抽象基类（简化实现）
        ├── ToolExecutionException.java
        └── ToolInvoker.java           # 工具注册和执行
```

---

## 核心设计概念

### 1. Multi-Agent 系统

```java
// 创建专业化的 Agent
Agent researchAgent = new DefaultAgentFactory().createAgent()
    .model(chatModel)
    .config(AgentConfig.builder()
        .systemPrompt("你是一个研究助手，负责搜索和整理信息。")
        .build())
    .build();

Agent codeAgent = new DefaultAgentFactory().createAgent()
    .model(chatModel)
    .config(AgentConfig.builder()
        .systemPrompt("你是一个编程助手，负责编写代码。")
        .build())
    .build();

// 创建多 Agent 系统
MultiAgentSystem multiAgent = MultiAgentSystem.builder()
    .addNode("researcher", researchAgent, "负责搜索信息")
    .addNode("coder", codeAgent, "负责编写代码")
    .entryPoint("researcher")
    .strategy(new SupervisorStrategy(chatModel))
    .maxHandoffs(5)
    .build();

// 使用
AgentResponse response = multiAgent.invoke(
    AgentRequest.of("研究 Java 新特性并写个示例")
);
```

### 2. 手 off 机制

```java
// Agent 通过特定格式发起手 off
// 格式1: HANDOFF:agentName:message
// 格式2: [HANDOFF agentName] message

// 在响应中输出
String response = "我已经完成研究工作。HANDOFF:coder:请根据研究结果编写代码";

// 系统自动解析并转交给 coder
```

### 3. Supervisor 路由策略

```java
// 由 LLM 决定下一个调用哪个 Agent
MultiAgentSystem system = MultiAgentSystem.builder()
    .addNode("searcher", searchAgent, "搜索专家")
    .addNode("math", mathAgent, "数学专家")
    .addNode("writer", writerAgent, "写作专家")
    .entryPoint("searcher")
    .strategy(new SupervisorStrategy(supervisorModel))  // LLM 决定路由
    .build();
```

### 4. 中间件系统（5 个拦截点）

```java
public interface Middleware {
    // Agent 调用前
    void beforeInvoke(AgentRequest request, AgentContext context);

    // Agent 调用后
    void afterInvoke(AgentResponse response, AgentContext context);

    // 工具调用前（返回 false 阻止调用）
    boolean beforeToolCall(ToolCall toolCall, AgentContext context);

    // 工具调用后
    void afterToolCall(ToolCall toolCall, ToolResult toolResult, AgentContext context);

    // 状态更新时（可以修改状态）
    AgentState onStateUpdate(AgentState oldState, AgentState newState, AgentContext context);
}
```

### 5. 状态注入中间件

```java
// 动态注入状态到系统提示词
.middleware(StateInjectionMiddleware.builder()
    .injectionMode(InjectionMode.SUFFIX)
    .template("""
        === 用户信息 ===
        用户名：${request:userName}
        会员等级：${request:userLevel}
        ====================
        """)
    .build())
```

---

## 使用示例

### 基础 Agent 创建

```java
Agent agent = new DefaultAgentFactory().createAgent()
    .model(new OpenAIChatModel(apiKey))
    .tool(new WeatherTool())
    .config(AgentConfig.builder()
        .systemPrompt("你是一个专业的Java开发助手")
        .maxIterations(10)
        .build())
    .build();

AgentResponse response = agent.invoke(AgentRequest.of("今天北京天气怎么样？"));
```

### Multi-Agent 协作

```java
MultiAgentSystem system = MultiAgentSystem.builder()
    .addNode("analyst", analystAgent, "需求分析")
    .addNode("developer", developerAgent, "代码开发")
    .addNode("reviewer", reviewerAgent, "代码审查")
    .entryPoint("analyst")
    .maxHandoffs(5)
    .build();

AgentResponse response = system.invoke(
    AgentRequest.of("我需要一个 Java 工具类来计算日期差")
);
```

---

## 工作流平台架构（开发中）

### 项目目标
开发一个类似 **Dify** 的可视化工作流编排平台，支持：
- 拖拽式节点编排
- Agent 节点自定义配置（提示词、MCP 工具、模型参数）
- 实时流式执行（WebSocket）
- 工作流版本管理
- 执行历史追踪

### 技术选型

#### 后端（Java）
```
knight-agent-workflow/     # 工作流引擎模块
├── workflow/
│   ├── node/             # 节点抽象
│   │   ├── WorkflowNode      # 节点接口
│   │   ├── NodeConfig        # 节点配置
│   │   ├── NodeContext       # 执行上下文
│   │   └── NodeType          # 节点类型枚举
│   ├── edge/             # 连接边
│   │   ├── WorkflowEdge      # 边定义
│   │   └── EdgeCondition     # 条件表达式
│   ├── definition/       # 工作流定义
│   │   ├── WorkflowDefinition # 工作流 DSL
│   │   └── WorkflowParser     # JSON/解析器
│   ├── engine/           # 执行引擎
│   │   ├── WorkflowEngine     # 执行引擎
│   │   ├── ExecutionResult    # 执行结果
│   │   └── ExecutionContext   # 执行上下文
│   └── nodes/            # 内置节点实现
│       ├── AgentNode         # Agent 节点
│       ├── CodeNode          # 代码节点
│       ├── ConditionNode     # 条件分支
│       ├── ToolNode          # 工具调用节点
│       ├── InputNode         # 输入节点
│       ├── OutputNode        # 输出节点
│       └── HttpNode          # HTTP 请求节点

knight-agent-api/          # REST API 模块
├── controller/
│   ├── WorkflowController      # 工作流 CRUD
│   ├── ExecutionController     # 执行控制
│   └── WebSocketController     # 流式推送
├── service/
│   ├── WorkflowService         # 业务逻辑
│   └── ExecutionService        # 执行服务
├── repository/
│   ├── WorkflowRepository      # 持久化
│   └── ExecutionRepository     # 执行历史
└── dto/
    ├── WorkflowDTO             # 数据传输对象
    └── ExecutionDTO
```

#### 前端（Next.js + shadcn/ui）
```
knight-agent-web/
├── app/
│   ├── page.tsx              # 首页（工作流列表）
│   ├── workflow/
│   │   ├── [id]/page.tsx     # 工作流编辑器
│   │   └── new/page.tsx      # 新建工作流
│   └── api/                  # API 路由（可选）
├── components/
│   ├── canvas/               # 画布组件
│   │   ├── WorkflowCanvas.tsx        # 主画布
│   │   ├── NodeComponent.tsx         # 节点渲染
│   │   ├── EdgeComponent.tsx         # 连接线
│   │   └── Controls.tsx              # 缩放/平移
│   ├── nodes/                # 节点组件
│   │   ├── AgentNode.tsx             # Agent 节点配置
│   │   ├── CodeNode.tsx              # 代码节点配置
│   │   ├── ConditionNode.tsx         # 条件节点
│   │   ├── ToolNode.tsx              # 工具节点
│   │   └── IoNode.tsx                # 输入输出节点
│   ├── panels/              # 配置面板
│   │   ├── NodeConfigPanel.tsx       # 节点配置
│   │   ├── AgentConfigPanel.tsx      # Agent 详细配置
│   │   └── ExecutionPanel.tsx        # 执行监控
│   └── ui/                  # shadcn/ui 组件
├── lib/
│   ├── api.ts               # API 客户端
│   ├── workflow.ts          # 工作流类型
│   └── websocket.ts         # WebSocket 客户端
└── hooks/
    ├── useWorkflow.ts       # 工作流操作
    └── useExecution.ts      # 执行状态
```

### 节点类型设计

| 节点类型 | 功能 | 配置项 |
|---------|------|--------|
| **InputNode** | 工作流输入 | 输入参数 Schema 定义 |
| **OutputNode** | 工作流输出 | 输出映射配置 |
| **AgentNode** | Agent 执行 | 模型、提示词、MCP 工具、温度、最大 Token |
| **CodeNode** | 自定义代码 | JavaScript 代码片段、输入输出变量 |
| **ConditionNode** | 条件分支 | 条件表达式、分支路由 |
| **ToolNode** | 工具调用 | 选择工具、参数映射 |
| **HttpNode** | HTTP 请求 | URL、Method、Headers、Body |
| **LoopNode** | 循环执行 | 循环条件、最大次数 |

### API 设计（草案）

```typescript
// 工作流 CRUD
GET    /api/workflows              # 列表
GET    /api/workflows/:id          # 详情
POST   /api/workflows              # 创建
PUT    /api/workflows/:id          # 更新
DELETE /api/workflows/:id          # 删除

// 执行控制
POST   /api/workflows/:id/execute  # 同步执行
POST   /api/workflows/:id/stream   # 流式执行（WebSocket）
GET    /api/executions/:id         # 执行状态
GET    /api/workflows/:id/versions # 版本列表

// MCP 工具
GET    /api/mcp/tools              # 可用 MCP 工具列表
POST   /api/mcp/servers            # 添加 MCP 服务器
```

### WebSocket 协议（草案）

```typescript
// 客户端 → 服务端
{ "type": "execute", "workflowId": "xxx", "input": {...} }
{ "type": "pause", "executionId": "xxx" }
{ "type": "stop", "executionId": "xxx" }

// 服务端 → 客户端
{ "type": "node_start", "nodeId": "xxx", "nodeName": "Agent" }
{ "type": "token", "nodeId": "xxx", "text": "..." }
{ "type": "tool_call", "nodeId": "xxx", "tool": "..." }
{ "type": "node_complete", "nodeId": "xxx", "output": {...} }
{ "type": "error", "nodeId": "xxx", "error": "..." }
{ "type": "complete", "result": {...} }
```

---

## 未来开发计划

### 短期目标

1. **CachingAgentFactory** - 缓存已创建的 Agent 实例
2. **Plan-and-Execute Strategy** - 规划-执行分离模式
3. **Spring Boot 自动配置** - `@EnableAgents` 注解

### 中期目标

4. **RAG Agent** - 检索增强生成
5. **对话存储** - `ConversationStore` 接口
6. **Token 压缩器** - 智能摘要算法

### 长期目标

7. **多 Agent 协作** - ✅ 已完成
8. **子 Agent 生成** - 动态创建子任务
9. **分布式执行** - 跨节点 Agent 协作

---

## 开发约定

### 命名规范
- 包名：`org.cloudnook.knightagent.core.*`
- 接口名：简单名词（如 `Agent`, `Tool`, `ChatModel`）
- 实现类名：接口名 + 具体标识（如 `InMemorySaver`, `PostgresSaver`）
- 异常类：`*Exception` 后缀

### 代码风格
- 使用 Lombok 减少样板代码
- Builder 模式用于复杂对象构建
- 函数式接口用于回调（`StateReducer`, `StreamCallback`）
- 不可变对象优先（`AgentState`, 消息类）

### 测试策略
- 单元测试覆盖核心逻辑
- 使用 `InMemorySaver` 进行快速测试
- 集成测试使用真实 API（Groq/OpenAI）

---

## 快速参考

### 编译项目
```bash
./mvnw clean compile
```

### 运行测试
```bash
./mvnw test
```

### 运行示例
```bash
./mvnw exec:java -Dexec.mainClass="org.cloudnook.knightagent.examples.MultiAgentExample" -Dexec.classpathScope=test
```
