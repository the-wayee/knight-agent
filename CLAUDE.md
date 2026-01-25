# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

KnightAgent 是一个轻量级的 Java Agent 框架，参考 2025 年 10 月底发布的 LangChain 架构设计。项目目标是提供 LangChain 的 Java 替代方案，支持：

- **流式返回** - 实时 token 流式输出
- **Agent 创建** - `create_agent` 风格的工厂方法
- **工具调用** - 函数/工具调用能力
- **中间件拦截** - 可组合的中间件：prompt 工程、护栏、状态管理、对话摘要
- **对话持久化** - 对话历史持久化存储
- **Token 压缩** - 自动摘要管理上下文窗口限制
- **运行时状态持久化** - 自定义状态的序列化
- **Checkpoint 机制** - 基于 Thread 的状态快照：时间旅行、人机协作、容错恢复
- **Multi-Agent 协作** - 多 Agent 协作完成复杂任务

## 技术栈

- **Java 17** - 最低 Java 版本
- **Spring Boot 3.5.10** - 应用框架
- **PostgreSQL** - 持久化存储（可选）
- **Lombok** - 代码生成
- **Maven** - 构建工具

**架构选择：单模块 Maven 项目**（暂未分模块，按需拆分）

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
