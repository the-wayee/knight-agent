# Agent & Workflow Studio

<div align="center">

**🤖 生产级 AI Agent 框架 + 🎨 可视化工作流平台**

*用 Java 构建智能 AI 应用 - 从代码到零代码*

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-16.0-black.svg)](https://nextjs.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

[特性](#-特性) • [快速开始](#-快速开始) • [文档](#-文档) • [示例](#-示例)

</div>

---

## 🎯 什么是 KnightAgent？

**KnightAgent** 是最易用的 Java AI Agent 应用和可视化工作流平台。它由两个强大的组件组成：

### 🔧 Agent 框架（Core）
一个受 **LangChain 启发**的 Java 框架，具有生产级特性：
- **ReAct Agent** 支持工具调用
- **Multi-Agent** 协作，基于 LLM 的智能路由
- **中间件系统**，5 个拦截点
- **状态持久化**，支持 Checkpoint 机制
- **MCP 协议**支持，连接外部工具
- **流式输出**，实时 Token 反馈

### 🎨 Workflow Studio（平台）
类似 **Dify** 的可视化工作流编辑器：
- **拖拽式**节点画布
- **Agent 节点**，支持自定义提示词和 MCP 工具
- **实时执行**，WebSocket 流式推送
- **工作流版本管理**和历史追踪
- **生产就绪**，PostgreSQL 持久化

---

## ✨ 特性

### Agent 框架

<table>
<tr>
<td width="50%">

**🤖 ReAct Agent**
```java
Agent agent = new DefaultAgentFactory()
    .createAgent()
    .model(new OpenAIChatModel(apiKey))
    .tool(new WeatherTool())
    .config(AgentConfig.builder()
        .systemPrompt("你是一个有帮助的助手")
        .maxIterations(10)
        .build())
    .build();

AgentResponse response = agent.invoke(
    AgentRequest.of("北京今天天气怎么样？")
);
```

</td>
<td width="50%">

**🔄 流式输出**
```java
agent.stream(request, new StreamCallback() {
    @Override
    public void onToken(String token) {
        System.out.print(token); // 实时输出
    }

    @Override
    public void onToolCall(ToolCall toolCall) {
        System.out.println("调用工具: " + toolCall.getName());
    }
});
```

</td>
</tr>
</table>

<table>
<tr>
<td width="50%">

**🤝 Multi-Agent 协作**
```java
MultiAgentSystem system = MultiAgentSystem.builder()
    .addNode("researcher", researchAgent,
        "搜索和收集信息")
    .addNode("coder", codeAgent,
        "基于研究结果编写代码")
    .addNode("reviewer", reviewAgent,
        "审查和改进代码")
    .entryPoint("researcher")
    .strategy(new SupervisorStrategy(chatModel))
    .maxHandoffs(5)
    .build();

AgentResponse response = system.invoke(
    AgentRequest.of("创建一个 Java 日期工具类")
);
```

</td>
<td width="50%">

**🔌 MCP 协议支持**
```java
Agent agent = new DefaultAgentFactory()
    .createAgent()
    .model(chatModel)
    .mcp(McpConfig.builder()
        .protocol(McpProtocol.STDIO)
        .entrypoint("npx -y @modelcontextprotocol/" +
            "server-filesystem /path/to/files")
        .build())
    .build();

// Agent 现在可以通过 MCP 访问文件系统
```

</td>
</tr>
</table>

<table>
<tr>
<td width="50%">

**🛡️ 中间件系统**
```java
Agent agent = new DefaultAgentFactory()
    .createAgent()
    .model(chatModel)
    .middleware(new LoggingMiddleware())
    .middleware(new SummarizationMiddleware(chatModel))
    .middleware(HumanInTheLoopMiddleware.builder()
        .approvalMode(ApprovalMode.REQUIRE_APPROVAL)
        .build())
    .middleware(StateInjectionMiddleware.builder()
        .template("""
            === 用户信息 ===
            用户名：${request:userName}
            角色：${request:userRole}
            ====================
            """)
        .build())
    .build();
```

**可用的中间件：**
- `LoggingMiddleware` - 请求/响应日志记录
- `SummarizationMiddleware` - 自动 Token 压缩
- `HumanInTheLoopMiddleware` - 工具审批流程
- `StateInjectionMiddleware` - 动态 Prompt 注入

</td>
<td width="50%">

**💾 状态持久化**
```java
// 创建检查点存储器
Checkpointer checkpointer = new PostgresSaver(dataSource);

Agent agent = new DefaultAgentFactory()
    .createAgent()
    .model(chatModel)
    .checkpointer(checkpointer)
    .build();

// 对话 1
agent.invoke(AgentRequest.builder()
    .input("记住：我最喜欢的颜色是蓝色")
    .threadId("thread-123")
    .build());

// 对话 2（从 thread-123 继续）
agent.invoke(AgentRequest.builder()
    .input("我最喜欢的颜色是什么？")
    .threadId("thread-123")
    .build());
// 输出："您最喜欢的颜色是蓝色"

// 时间旅行 - 加载特定检查点
CheckpointInfo checkpoint = checkpointer.list("thread-123")
    .get(0);
AgentState state = checkpointer.load("thread-123",
    checkpoint.getCheckpointId());
```

</td>
</tr>
</table>

### Workflow 平台

<table>
<tr>
<td width="50%">

**🎨 可视化工作流编辑器**

![工作流编辑器](docs/images/workflow-editor.png)

**支持的节点类型：**
- **Input/Output** - 工作流入口/出口
- **Agent 节点** - 执行 AI Agent，支持自定义配置
- **Code 节点** - 运行 JavaScript/Python/Java 代码
- **Condition 节点** - 条件分支
- **HTTP 节点** - 外部 API 调用
- **Tool 节点** - 独立工具执行

</td>
<td width="50%">

**⚙️ Agent 节点配置**

```typescript
// 前端：配置 Agent 节点
{
  "type": "agent",
  "config": {
    "apiKeyId": "key_123",
    "model": "gpt-4o",
    "strategy": "REACT",
    "systemPrompt": "你是一个代码审查助手...",
    "temperature": 0.7,
    "maxTokens": 4096,
    "tools": [
      "server-1/list_files",
      "server-1/read_file",
      "server-2/github_search"
    ]
  }
}
```

```java
// 后端：执行工作流
WorkflowEngine engine = new DefaultWorkflowEngine();

ExecutionResult result = engine.execute(
    workflowDefinition,
    Map.of("userInput", "审查这个 PR")
);
```

</td>
</tr>
</table>

<table>
<tr>
<td width="50%">

**📡 实时流式传输**
```java
// 后端：流式执行事件
engine.executeStream(workflow, input, event -> {
    switch (event.getType()) {
        case NODE_START:
            System.out.println("开始: " + event.getNodeId());
            break;
        case TOKEN:
            System.out.print(event.getData());
            break;
        case TOOL_CALL:
            System.out.println("工具: " + event.getData());
            break;
        case NODE_COMPLETE:
            System.out.println("完成: " + event.getNodeId());
            break;
    }
});
```

```typescript
// 前端：WebSocket 连接
const ws = new WebSocket('ws://localhost:8080/workflow/stream');

ws.onmessage = (event) => {
  const msg = JSON.parse(event.data);

  if (msg.type === 'token') {
    appendToOutput(msg.text);
  } else if (msg.type === 'node_complete') {
    updateNodeStatus(msg.nodeId, 'success');
  }
};
```

</td>
<td width="50%">

**🔧 REST API**
```bash
# 创建工作流
POST /api/workflows
{
  "name": "代码审查工作流",
  "nodes": [...],
  "edges": [...]
}

# 执行工作流
POST /api/workflows/{id}/execute
{
  "input": {
    "prUrl": "https://github.com/org/repo/pull/123"
  }
}

# 获取执行状态
GET /api/executions/{id}

# 流式执行（WebSocket）
WS /api/workflows/{id}/stream
```

</td>
</tr>
</table>

---

## 🚀 快速开始

### 前置要求
- Java 17+
- Node.js 18+
- PostgreSQL 12+（可选，开发环境可使用 H2 内存数据库）
- Maven 3.8+

### 1. 克隆和构建

```bash
git clone https://github.com/yourusername/knight-agent.git
cd knight-agent

# 构建后端
./mvnw clean install

# 安装前端依赖
cd web
npm install
```

### 2. 配置 API 密钥

创建 `application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:knightagent
  jpa:
    hibernate:
      ddl-auto: update

# 通过 UI 添加 API 密钥：http://localhost:3000/settings/api-keys
```

### 3. 运行后端

```bash
./mvnw spring-boot:run
```

后端运行于：`http://localhost:8080`

### 4. 运行前端

```bash
cd web
npm run dev
```

前端运行于：`http://localhost:3000`

### 5. 创建你的第一个工作流

1. 访问 `http://localhost:3000`
2. 点击 **"新建工作流"**
3. 从工具栏拖拽节点
4. 配置 Agent 节点的提示词和工具
5. 用连线连接节点
6. 点击 **"运行"** 执行工作流

---

## 📖 文档

### Agent 框架核心概念

#### 1. **执行策略**

框架使用**策略模式**实现不同的 Agent 行为：

```java
public interface ExecutionStrategy {
    AgentResponse execute(ExecutionContext context);
}

// ReAct 策略（推理 + 行动）
public class ReActStrategy implements ExecutionStrategy {
    @Override
    public AgentResponse execute(ExecutionContext context) {
        while (!shouldStop()) {
            // 1. 使用当前状态调用 LLM
            AIMessage response = callModel(context);

            // 2. 如果有工具调用，执行它们
            if (response.hasToolCalls()) {
                List<ToolResult> results = executeTools(response.getToolCalls());
                context.addToolResults(results);
                continue; // 循环回 LLM
            }

            // 3. 返回最终响应
            return buildResponse(response);
        }
    }
}
```

#### 2. **中间件链**

中间件提供 **5 个拦截点**：

```java
public interface Middleware {
    // 1. Agent 调用前
    void beforeInvoke(AgentRequest request, AgentContext context);

    // 2. Agent 调用后
    void afterInvoke(AgentResponse response, AgentContext context);

    // 3. 工具调用前（返回 false 阻止调用）
    boolean beforeToolCall(ToolCall toolCall, AgentContext context);

    // 4. 工具调用后
    void afterToolCall(ToolCall toolCall, ToolResult result, AgentContext context);

    // 5. 状态更新时（可以修改状态）
    AgentState onStateUpdate(AgentState oldState, AgentState newState,
                             AgentContext context);
}
```

**示例：人机协作**

```java
HumanInTheLoopMiddleware middleware = HumanInTheLoopMiddleware.builder()
    .approvalMode(ApprovalMode.REQUIRE_APPROVAL)
    .approvalCallback((toolCall) -> {
        System.out.println("工具: " + toolCall.getName());
        System.out.println("参数: " + toolCall.getArguments());
        System.out.print("是否批准？(y/n): ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        return "y".equalsIgnoreCase(input);
    })
    .build();
```

#### 3. **Multi-Agent 手交（Handoff）**

Agent 可以将任务**转交**给其他 Agent：

```java
// Supervisor 决定路由到哪个 Agent
public class SupervisorStrategy implements HandoffStrategy {
    @Override
    public String decideNextAgent(AgentState state,
                                  Map<String, String> agentDescriptions) {
        // 使用 LLM 决定路由
        String prompt = buildRoutingPrompt(state, agentDescriptions);
        AIMessage decision = chatModel.chat(prompt);
        return parseAgentName(decision.getContent());
    }
}

// 手交消息格式示例
// 格式 1: HANDOFF:agentName:message
// 格式 2: [HANDOFF agentName] message

String response = "我已完成研究工作。HANDOFF:coder:请根据研究结果编写代码";
```

#### 4. **MCP 协议**

连接到 **Model Context Protocol** 服务器：

```java
// STDIO 协议（本地 npx 包）
McpConfig stdioConfig = McpConfig.builder()
    .protocol(McpProtocol.STDIO)
    .entrypoint("npx -y @modelcontextprotocol/server-filesystem /path")
    .build();

// SSE 协议（HTTP 服务器）
McpConfig sseConfig = McpConfig.builder()
    .protocol(McpProtocol.SSE)
    .url("http://localhost:8000/mcp")
    .build();

// WebSocket 协议（实时双向）
McpConfig wsConfig = McpConfig.builder()
    .protocol(McpProtocol.WS)
    .url("ws://localhost:8000/mcp")
    .build();

Agent agent = factory.createAgent()
    .model(chatModel)
    .mcp(stdioConfig)
    .build();
```

**官方 MCP 服务器：**
- `@modelcontextprotocol/server-filesystem` - 文件系统访问
- `@modelcontextprotocol/server-postgres` - PostgreSQL 数据库
- `@modelcontextprotocol/server-github` - GitHub API
- `@modelcontextprotocol/server-brave-search` - 网络搜索

### 工作流引擎架构

```
WorkflowDefinition (DSL)
    ├─ nodes: List<NodeDefinition>
    ├─ edges: List<EdgeDefinition>
    └─ validate() → 确保 DAG，无环

DefaultWorkflowEngine
    ├─ 1. 拓扑排序（依赖顺序）
    ├─ 2. 按顺序执行节点
    ├─ 3. 将输出传递给下一个节点
    └─ 4. 处理错误和回滚

NodeTypes
    ├─ InputNode → 工作流入口
    ├─ AgentNode → 执行 Agent，支持配置
    ├─ CodeNode → 运行 JavaScript/Python/Java
    ├─ ConditionNode → 条件分支
    ├─ HttpNode → 外部 API 调用
    ├─ ToolNode → 独立工具执行
    └─ OutputNode → 工作流出口
```

**工作流定义示例：**

```java
WorkflowDefinition workflow = WorkflowDefinition.builder()
    .name("代码审查工作流")
    .addNode(NodeDefinition.builder()
        .id("input")
        .type(NodeType.INPUT)
        .config(Map.of("fields", List.of(
            Map.of("name", "prUrl", "type", "text")
        )))
        .build())
    .addNode(NodeDefinition.builder()
        .id("analyzer")
        .type(NodeType.AGENT)
        .config(Map.of(
            "model", "gpt-4o",
            "systemPrompt", "分析这个 PR 并提出改进建议",
            "tools", List.of("server-1/github_get_pr", "server-1/github_get_diff")
        ))
        .build())
    .addNode(NodeDefinition.builder()
        .id("output")
        .type(NodeType.OUTPUT)
        .build())
    .addEdge("input", "analyzer")
    .addEdge("analyzer", "output")
    .build();

// 执行
ExecutionResult result = engine.execute(workflow, Map.of(
    "prUrl", "https://github.com/org/repo/pull/123"
));
```

---

## 🏗️ 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端（Next.js）                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   工作流     │  │    节点      │  │  执行监控    │      │
│  │   编辑器     │  │   配置面板   │  │    面板      │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↕ REST API / WebSocket
┌─────────────────────────────────────────────────────────────┐
│              后端（Spring Boot + Core）                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              工作流引擎层                             │  │
│  │  • WorkflowEngine • NodeExecutor • EventPublisher   │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Agent 框架层                             │  │
│  │  • AgentExecutor • MiddlewareChain • StateManager   │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                 核心抽象层                            │  │
│  │  • ChatModel • Tool • Message • Checkpointer        │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────────┐
│            外部服务（LLM、MCP、存储）                        │
│  • OpenAI / Anthropic / DeepSeek                           │
│  • MCP 服务器（文件系统、GitHub 等）                        │
│  • PostgreSQL（状态 + 工作流存储）                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 项目结构

```
knight-agent/
├── src/main/java/org/cloudnook/knightagent/
│   ├── core/                      # Agent 框架（100% 完成）
│   │   ├── agent/                 # Agent 执行引擎
│   │   │   ├── Agent.java
│   │   │   ├── AgentExecutor.java
│   │   │   ├── factory/           # 工厂模式
│   │   │   │   ├── AgentFactory.java
│   │   │   │   ├── AgentBuilder.java
│   │   │   │   └── DefaultAgentFactory.java
│   │   │   └── strategy/          # 执行策略
│   │   │       ├── ExecutionStrategy.java
│   │   │       └── ReActStrategy.java
│   │   ├── middleware/            # 中间件系统
│   │   │   ├── Middleware.java
│   │   │   ├── MiddlewareChain.java
│   │   │   └── builtin/           # 4 个内置中间件
│   │   ├── multiagent/            # Multi-Agent 系统
│   │   │   ├── MultiAgentSystem.java
│   │   │   ├── SupervisorStrategy.java
│   │   │   └── AgentHandoff.java
│   │   ├── mcp/                   # MCP 协议支持
│   │   │   ├── McpProtocol.java
│   │   │   ├── McpConfig.java
│   │   │   ├── McpClient.java
│   │   │   └── McpTool.java
│   │   ├── state/                 # 状态管理
│   │   ├── checkpoint/            # Checkpoint 机制
│   │   ├── message/               # 消息类型
│   │   ├── model/                 # LLM 抽象
│   │   ├── tool/                  # 工具系统
│   │   └── streaming/             # 流式接口
│   ├── workflow/                  # 工作流引擎（85% 完成）
│   │   ├── node/                  # 节点抽象
│   │   ├── definition/            # 工作流 DSL
│   │   ├── engine/                # 执行引擎
│   │   └── nodes/                 # 6 种内置节点
│   └── api/                       # REST API（80% 完成）
│       ├── controller/            # 控制器
│       ├── service/               # 业务逻辑
│       ├── repository/            # 数据访问
│       └── entity/                # JPA 实体
└── web/                           # 前端（70% 完成）
    ├── app/                       # Next.js 页面
    │   ├── page.tsx               # 首页（工作流列表）
    │   ├── workflow/[id]/page.tsx # 工作流编辑器
    │   └── settings/              # 设置页面
    ├── components/                # React 组件
    │   ├── workflow/              # 工作流编辑器
    │   ├── nodes/                 # 节点组件（8 种）
    │   ├── panels/                # 配置面板
    │   └── ui/                    # shadcn/ui 组件
    └── lib/                       # API 客户端 + Hooks
```

---

## 🧪 示例

### 示例 1：简单问答 Agent

```java
Agent agent = new DefaultAgentFactory().createAgent()
    .model(new OpenAIChatModel("sk-..."))
    .config(AgentConfig.builder()
        .systemPrompt("你是一个有帮助的助手")
        .build())
    .build();

AgentResponse response = agent.invoke(
    AgentRequest.of("法国的首都是哪里？")
);

System.out.println(response.getOutput());
// 输出："法国的首都是巴黎。"
```

### 示例 2：带工具的 Agent

```java
@Component
public class WeatherTool extends AbstractTool {
    @Override
    public String getName() {
        return "get_weather";
    }

    @Override
    public String getDescription() {
        return "获取城市的当前天气";
    }

    @Override
    public JsonNode getParametersSchema() {
        return new ObjectMapper().createObjectNode()
            .put("type", "object")
            .putObject("properties")
                .putObject("city")
                    .put("type", "string")
                    .put("description", "城市名称");
    }

    @Override
    protected String executeInternal(String arguments) {
        // 调用天气 API
        return "温度：22°C，晴天";
    }
}

Agent agent = factory.createAgent()
    .model(chatModel)
    .tool(new WeatherTool())
    .build();

agent.invoke(AgentRequest.of("东京今天天气怎么样？"));
// Agent 自动调用 get_weather 工具
```

### 示例 3：Multi-Agent 代码审查

```java
Agent analystAgent = factory.createAgent()
    .model(chatModel)
    .config(AgentConfig.builder()
        .systemPrompt("分析代码并识别问题")
        .build())
    .build();

Agent reviewerAgent = factory.createAgent()
    .model(chatModel)
    .config(AgentConfig.builder()
        .systemPrompt("提供详细的代码审查反馈")
        .build())
    .build();

MultiAgentSystem system = MultiAgentSystem.builder()
    .addNode("analyst", analystAgent, "识别代码问题")
    .addNode("reviewer", reviewerAgent, "编写审查评论")
    .entryPoint("analyst")
    .strategy(new SupervisorStrategy(chatModel))
    .build();

system.invoke(AgentRequest.of("审查这个 Java 类：" + codeSnippet));
```

### 示例 4：带 Agent 节点的工作流

```typescript
// 通过 API 创建工作流
const workflow = await workflowApi.createWorkflow({
  name: "AI 代码审查",
  nodes: [
    {
      id: "input",
      type: "input",
      data: {
        fields: [{ name: "code", type: "text" }]
      }
    },
    {
      id: "agent",
      type: "agent",
      data: {
        apiKeyId: "key_123",
        model: "gpt-4o",
        systemPrompt: "审查这段代码并提出改进建议",
        tools: ["server-1/code_analysis"]
      }
    },
    {
      id: "output",
      type: "output"
    }
  ],
  edges: [
    { source: "input", target: "agent" },
    { source: "agent", target: "output" }
  ]
});

// 执行工作流
const result = await workflowApi.execute(workflow.id, {
  code: "public class Example { ... }"
});
```

---

## 🛠️ 技术栈

### 后端
- **Java 17** - 核心语言
- **Spring Boot 3.5.10** - 应用框架
- **PostgreSQL** - 生产数据库
- **H2** - 开发环境内存数据库
- **Jackson** - JSON 序列化
- **Lombok** - 减少样板代码

### 前端
- **Next.js 16** - React 框架
- **React 19** - UI 库
- **TypeScript** - 类型安全
- **ReactFlow 12** - 工作流画布
- **shadcn/ui** - UI 组件
- **Tailwind CSS 4** - 样式
- **Zustand 5** - 状态管理

### AI/ML
- **OpenAI API** - GPT-4、GPT-4o
- **Anthropic API** - Claude 3.5 Sonnet/Haiku
- **DeepSeek** - 替代 LLM
- **MCP Protocol** - 工具集成

---

## 🤝 贡献

我们欢迎贡献！请查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详情。

### 开发设置

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交你的更改 (`git commit -m '添加某个很棒的特性'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开一个 Pull Request

### 路线图

- [ ] **RAG 集成** - 向量数据库支持（Pinecone、Weaviate）
- [ ] **工作流模板** - 预构建工作流市场
- [ ] **多用户协作** - 实时协作编辑
- [ ] **Docker/K8s 部署** - 生产部署工具
- [ ] **更多执行策略** - Plan-Execute、ReWOO、Reflection
- [ ] **Spring Boot Starter** - 自动配置支持

---

## 📄 许可证

本项目采用 Apache License 2.0 许可 - 详见 [LICENSE](LICENSE) 文件。

---

## 🙏 致谢

灵感来源：
- [LangChain](https://github.com/langchain-ai/langchain) - Agent 框架设计
- [Dify](https://github.com/langgenius/dify) - 工作流平台 UX
- [Model Context Protocol](https://modelcontextprotocol.io/) - 工具集成标准

---

## 📞 联系方式

- **问题反馈**：[GitHub Issues](https://github.com/yourusername/knight-agent/issues)
- **讨论**：[GitHub Discussions](https://github.com/yourusername/knight-agent/discussions)
- **邮箱**：your-email@example.com

---

<div align="center">

**⭐ 在 GitHub 上给我们点个星 — 这对我们是很大的鼓励！**

由 KnightAgent 团队用 ❤️ 制作

</div>
