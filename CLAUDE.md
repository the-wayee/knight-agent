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

## 技术栈

- **Java 17** - 最低 Java 版本
- **Spring Boot 3.5.10** - 应用框架
- **PostgreSQL** - 持久化存储（可选）
- **Lombok** - 代码生成
- **Maven** - 构建工具

**架构选择：单模块 Maven 项目**（暂未分模块，按需拆分）

---

## 当前开发进度（截至对话结束）

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
│   └── ModelException.java    # 模型异常
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
│   └── AgentExecutor.java         # 执行引擎（ReAct 循环）
└── middleware/        # 中间件系统（接口层）
    ├── Middleware.java            # 中间件接口（5 个拦截点）
    ├── AgentContext.java          # 中间件上下文
    ├── MiddlewareException.java
    └── MiddlewareChain.java       # 中间件链执行器
```

### ⏳ 待完成阶段

#### 阶段四：中间件系统 (0% - 接口已定义，需实现内置中间件)
- 对话摘要中间件 (`SummarizationMiddleware`)
- 人机协作中间件 (`HumanInTheLoopMiddleware`)
- 状态注入中间件 (`StateInjectionMiddleware`)

#### 阶段五：高层 API (0%)
- `create_agent` 工厂方法
- AgentFactory 类
- 预构建 Agent 模板（ReActAgent、ToolCallingAgent）

#### 阶段六：存储和优化 (0%)
- 对话存储接口 (`ConversationStore`)
- Token 压缩器 (`TokenCompressor`)
- 时间旅行支持

#### 阶段七：集成和示例 (0%)
- Spring Boot 自动配置
- 示例 Agent

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
    │   └── AgentExecutor.java         # 核心执行逻辑
    ├── checkpoint/
    │   ├── Checkpointer.java         # .save(), .load(), .list()
    │   ├── CheckpointInfo.java       # 检查点元数据
    │   ├── CheckpointException.java
    │   ├── InMemorySaver.java        # 内存实现（测试用）
    │   └── PostgresSaver.java        # PostgreSQL 实现
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
    │   └── MiddlewareException.java
    ├── model/
    │   ├── ChatModel.java            # .chat(), .chatStream(), .countTokens()
    │   ├── ChatOptions.java          # temperature, maxTokens, topP
    │   ├── ModelCapabilities.java    # 模型能力描述
    │   └── ModelException.java
    ├── state/
    │   ├── AgentState.java           # 不可变状态，支持 .addMessage()
    │   ├── StateReducer.java         # 状态归约器
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

### 1. 消息类型系统

```java
// 用户消息
HumanMessage userMsg = HumanMessage.of("今天北京天气怎么样？", "user-123");

// AI 消息（可包含工具调用）
AIMessage aiMsg = AIMessage.builder()
    .content("我来帮你查询")
    .toolCalls(List.of(ToolCall.of("get_weather", "{\"city\": \"北京\"}")))
    .build();

// 工具结果消息
ToolMessage toolMsg = ToolMessage.success("call_123", "{\"temp\": 25}");

// 系统消息
SystemMessage sysMsg = SystemMessage.of("你是一个专业的Java开发助手");
```

### 2. 状态管理（不可变）

```java
// 创建初始状态
AgentState state = AgentState.initial();

// 添加消息（返回新状态）
state = state.addMessage(HumanMessage.of("Hello"));
state = state.addMessage(AIMessage.of("Hi there!"));

// 设置自定义数据
state = state.put("userId", "user-123");
state = state.put("counter", 42);
```

### 3. Checkpoint 系统

```java
Checkpointer checkpointer = new InMemorySaver(); // 或 new PostgresSaver(dataSource)

// 保存状态
String threadId = "conversation-123";
String checkpointId = checkpointer.save(threadId, state);

// 加载最新状态
Optional<AgentState> latest = checkpointer.loadLatest(threadId);

// 列出所有检查点
List<CheckpointInfo> checkpoints = checkpointer.list(threadId);

// 时间旅行：回退到指定检查点
Optional<AgentState> earlier = checkpointer.load(threadId, "checkpoint-456");
```

### 4. 工具系统

```java
// 定义工具
public class WeatherTool extends AbstractTool {
    @Override
    public String getName() { return "get_weather"; }

    @Override
    public String getDescription() {
        return "获取指定城市的天气信息";
    }

    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "city": {"type": "string", "description": "城市名称"}
                },
                "required": ["city"]
            }
            """;
    }

    @Override
    protected ToolResult executeInternal(Map<String, Object> arguments) {
        String city = getStringParam(arguments, "city");
        // 执行天气查询逻辑
        return ToolResult.success(generateCallId(), "{\"temp\": 25, \"condition\": \"晴\"}");
    }
}

// 使用工具
ToolInvoker invoker = new ToolInvoker();
invoker.register(new WeatherTool());
ToolResult result = invoker.invoke(ToolCall.of("get_weather", "{\"city\": \"北京\"}"));
```

### 5. Agent 执行流程

```
用户输入
  ↓
加载历史状态（如果有 Thread ID）
  ↓
添加用户消息 → state
  ↓
┌─────────────────────────────────────────┐
│  循环（最多 maxIterations 次）            │
│                                           │
│  1. 构建消息列表                          │
│     - SystemMessage（如果有）             │
│     - state.getMessages()（历史消息）     │
│                                           │
│  2. 中间件：beforeInvoke                   │
│                                           │
│  3. 调用 LLM：model.chat(messages, options) │
│                                           │
│  4. 接收 AIMessage                         │
│     - 如果有 toolCalls：                   │
│       → 对每个 ToolCall：                 │
│         - 中间件：beforeToolCall          │
│         - toolInvoker.invoke()            │
│         - 中间件：afterToolCall           │
│         - 添加 ToolMessage 到状态         │
│       → 继续循环                          │
│     - 否则：结束循环                      │
│                                           │
│  5. 中间件：afterInvoke                    │
└─────────────────────────────────────────┘
  ↓
应用 StateReducer
  ↓
保存 Checkpoint（如果启用）
  ↓
返回 AgentResponse
```

---

## 关键类和接口说明

### Agent 核心接口

```java
public interface Agent {
    // 同步执行
    AgentResponse invoke(AgentRequest request) throws AgentExecutionException;

    // 流式执行
    void stream(AgentRequest request, StreamCallback callback) throws AgentExecutionException;

    // 批量执行
    List<AgentResponse> batch(List<AgentRequest> requests) throws AgentExecutionException;
}
```

### AgentExecutor 执行引擎

**核心职责**：
- 管理 ReAct 循环（思考 → 行动 → 观察）
- 协调 LLM 调用和工具执行
- 管理状态更新和检查点保存
- 通过中间件链处理所有事件

**关键方法**：
```java
public class AgentExecutor {
    public AgentResponse execute(AgentRequest request) throws AgentExecutionException {
        // 1. 创建中间件上下文
        // 2. 加载或创建状态
        // 3. 添加用户消息
        // 4. 执行 ReAct 循环
        // 5. 保存检查点
        // 6. 返回响应
    }
}
```

### 中间件系统（5 个拦截点）

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

### Checkpointer 接口

```java
public interface Checkpointer {
    // 保存状态（自动生成 checkpoint ID）
    String save(String threadId, AgentState state);

    // 加载指定检查点
    Optional<AgentState> load(String threadId, String checkpointId);

    // 加载最新检查点
    Optional<AgentState> loadLatest(String threadId);

    // 列出所有检查点（倒序）
    List<CheckpointInfo> list(String threadId);

    // 时间旅行：回退到指定检查点
    boolean delete(String threadId, String checkpointId);
}
```

---

## 使用示例（伪代码，待实现 ChatModel 后可用）

```java
// 1. 创建 Agent（待实现 AgentFactory 后）
Agent agent = AgentFactory.builder()
    .model(new OpenAIChatModel(apiKey))
    .tools(List.of(new WeatherTool(), new SearchTool()))
    .checkpointer(new PostgresSaver(dataSource))
    .config(AgentConfig.builder()
        .systemPrompt("你是一个专业的Java开发助手")
        .maxIterations(10)
        .build())
    .build();

// 2. 同步调用
AgentRequest request = AgentRequest.of("今天北京天气怎么样？", "thread-123");
AgentResponse response = agent.invoke(request);
System.out.println(response.getOutput());

// 3. 流式调用
agent.stream(request, new StreamCallbackAdapter() {
    @Override
    public void onToken(String token) {
        System.out.print(token); // 实时输出
    }
});

// 4. 带历史的对话
AgentRequest request2 = AgentRequest.of("那上海呢？", "thread-123");
AgentResponse response2 = agent.invoke(request2); // 自动加载历史

// 5. 时间旅行
List<CheckpointInfo> checkpoints = agent.getCheckpointer().list("thread-123");
// 回退到上一个检查点
// （待实现功能）
```

---

## 未来开发计划

### 短期目标（优先级高）

1. **实现 ChatModel 的具体实现**
   - `OpenAIChatModel` - 支持 GPT-4/GPT-3.5
   - 使用 OkHttp 或 Java 11+ HttpClient
   - 支持流式 SSE 接收

2. **实现 AgentFactory**
   - 简化 Agent 创建
   - Fluent Builder API
   - 自动配置默认组件

3. **内置中间件实现**
   - `LoggingMiddleware` - 日志记录
   - `SummarizationMiddleware` - Token 压缩
   - `HumanInTheLoopMiddleware` - 人机协作

### 中期目标

4. **Spring Boot Starter**
   - 自动配置类
   - 配置属性类
   - `@EnableAgents` 注解

5. **对话存储**
   - `ConversationStore` 接口
   - `InMemoryStore` 实现
   - `PostgresStore` 实现

6. **Token 压缩器**
   - 基于长度的自动摘要
   - 保留关键信息的策略

### 长期目标

7. **高级功能**
   - 多 Agent 协作
   - 子 Agent 生成
   - 分布式执行支持

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
- 集成测试使用真实数据库

### 设计原则
1. **接口驱动** - 所有核心组件都是接口，支持多种实现
2. **可组合性** - 中间件、工具、状态归约器都可以组合
3. **不可变性** - 状态和消息类不可变，每次更新返回新实例
4. **容错性** - Checkpoint 支持故障恢复
5. **可测试性** - 内存实现支持快速测试

---

## 重要注意事项

### 当前限制
1. **尚未实现 ChatModel 的具体实现** - 无法真实调用 LLM
2. **AgentFactory 未实现** - 需要手动创建 Agent 实例
3. **内置中间件未实现** - 只有接口定义
4. **序列化/反序列化不完整** - `AgentState.fromBytes()` 需要完善

### 待解决问题
1. **消息序列化** - 需要实现 JSON 序列化/反序列化逻辑
2. **ChatOptions.Builder** - 需要添加 toBuilder 方法支持
3. **流式输出的状态管理** - 流式过程中如何更新状态
4. **中间件的错误处理** - 需要定义清晰的错误传播机制

### 下一步建议
1. **优先级 1**：实现 `OpenAIChatModel` - 这样可以真实测试框架
2. **优先级 2**：实现 `AgentFactory` - 简化 Agent 创建
3. **优先级 3**：完善消息序列化 - 支持状态持久化
4. **优先级 4**：实现日志中间件 - 便于调试

---

## 参考架构

本框架参考以下架构设计：

- **LangChain (Python/JS)** - `create_agent` API、中间件系统、Checkpoint 概念
- **LangGraph** - 状态图执行引擎、Thread 概念
- **Spring AI** - ChatModel 抽象、工具调用接口

核心差异：
- 使用 **单模块** 而非多模块（可后续拆分）
- 使用 **纯 JDBC** 而非 Spring JDBC（减少依赖）
- **更简化的 API** - 专注于核心功能

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

### 启动应用
```bash
./mvnw spring-boot:run
```

### 查看当前文件结构
```bash
find src/main/java -name "*.java" | head -30
```

### 查找特定类
```bash
grep -r "class.*Agent" src/main/java/
```
