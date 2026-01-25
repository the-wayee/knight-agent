# KnightAgent 工作流项目 - AI 开发指南

> 本文档用于 AI 协作开发工作流引擎和 API，请仔细阅读后开始工作。

---

## 项目背景

### 项目定位
**KnightAgent** 是一个 Agent & Workflow 可视化平台，类似 Dify，但基于 Java 构建。

```
KnightAgent = Agent 框架 (已完成) + 工作流引擎 (开发中) + Web 平台 (开发中)
```

### 当前状态
```
✅ Agent 框架 (core/ 目录) - 已完成
   - 消息系统、模型抽象、工具系统
   - Agent 执行引擎、中间件系统
   - Multi-Agent 协作、MCP 协议支持

⏳ 工作流引擎 (workflow/ 目录) - 待开发
   - 节点抽象、工作流定义
   - 执行引擎、内置节点实现

⏳ REST API (api/ 目录) - 待开发
   - 工作流 CRUD
   - 执行控制
   - WebSocket 流式推送
```

### 项目结构
```
knight-agent/
├── src/main/java/org/cloudnook/knightagent/
│   ├── core/              # Agent 框架（已完成）
│   ├── workflow/          # 工作流引擎（待开发）⭐
│   └── api/               # REST API（待开发）⭐
├── src/main/resources/
│   └── application.yml    # Spring Boot 配置
├── docs/
│   ├── V0_PROMPT_V2.md    # 前端开发提示词
│   └── WORKFLOW_DEV.md    # 本文档
└── pom.xml                # Maven 配置
```

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.5.10 |
| 数据库 | PostgreSQL | 15+ |
| ORM | Spring Data JPA | 3.x |
| JSON | Jackson | 2.x |
| WebSocket | Spring WebSocket | 6.x |
| 构建工具 | Maven | 3.9+ |
| 代码生成 | Lombok | 1.18.x |

---

## 工作流引擎设计

### 核心概念

```
工作流 (Workflow) = 节点 (Nodes) + 连接 (Edges)
```

### 节点类型

| 节点类型 | 代码 | 功能 | 优先级 |
|---------|------|------|--------|
| Input | `input` | 工作流输入定义 | P0 |
| Output | `output` | 工作流输出定义 | P0 |
| Agent | `agent` | AI Agent 执行 | P0 |
| Code | `code` | JavaScript 代码执行 | P1 |
| Condition | `condition` | 条件分支 | P1 |
| HTTP | `http` | HTTP 请求 | P1 |
| Tool | `tool` | 单独工具调用 | P2 |

### 数据结构

```java
// 工作流定义
Workflow {
    String id;
    String name;
    String description;
    List<WorkflowNode> nodes;
    List<WorkflowEdge> edges;
    Map<String, Object> settings;
}

// 节点定义
WorkflowNode {
    String id;              // 唯一标识
    NodeType type;          // 节点类型
    String name;            // 显示名称
    Point position;         // 画布位置 {x, y}
    NodeConfig config;      // 节点配置
}

// 连接定义
WorkflowEdge {
    String id;
    String source;          // 源节点 ID
    String target;          // 目标节点 ID
    String sourceHandle;    // 源锚点
    String targetHandle;    // 目标锚点
    String condition;       // 条件表达式（可选）
}
```

### 执行流程

```
1. 解析工作流定义 (JSON)
2. 拓扑排序，确定执行顺序
3. 按顺序执行节点
4. 处理条件分支
5. 处理循环（如需要）
6. 收集最终输出
```

---

## 开发任务清单

### 阶段 W1：工作流引擎核心 (P0)

#### 1.1 节点抽象层
```java
// src/main/java/org/cloudnook/knightagent/workflow/node/
├── WorkflowNode.java           // 节点接口
├── NodeType.java               // 节点类型枚举
├── NodeContext.java            // 执行上下文
├── NodeConfig.java             // 配置基类
├── NodeExecutionResult.java    // 执行结果
└── AbstractNode.java           // 抽象基类
```

**WorkflowNode 接口**:
```java
public interface WorkflowNode {
    String getId();
    NodeType getType();
    String getName();
    NodeExecutionResult execute(NodeContext context);
    NodeConfig getConfig();
    void setConfig(NodeConfig config);
}
```

#### 1.2 工作流定义
```java
// src/main/java/org/cloudnook/knightagent/workflow/definition/
├── WorkflowDefinition.java     // 工作流定义
├── WorkflowEdge.java           // 连接定义
├── WorkflowParser.java         // JSON 解析器
└── WorkflowValidator.java      // 验证器
```

**WorkflowDefinition**:
```java
@Data
@Builder
public class WorkflowDefinition {
    private String id;
    private String name;
    private String description;
    private List<NodeDefinition> nodes;
    private List<EdgeDefinition> edges;
    private Map<String, Object> settings;

    // 验证工作流是否合法
    public ValidationResult validate();
}
```

#### 1.3 执行引擎
```java
// src/main/java/org/cloudnook/knightagent/workflow/engine/
├── WorkflowEngine.java         // 执行引擎接口
├── DefaultWorkflowEngine.java  // 默认实现
├── ExecutionContext.java        // 执行上下文
├── ExecutionResult.java        // 执行结果
└── ExecutionStatus.java        // 执行状态枚举
```

**WorkflowEngine 接口**:
```java
public interface WorkflowEngine {
    // 同步执行
    ExecutionResult execute(WorkflowDefinition workflow, Map<String, Object> input);

    // 异步执行
    CompletableFuture<ExecutionResult> executeAsync(
        WorkflowDefinition workflow,
        Map<String, Object> input
    );

    // 流式执行（返回事件）
    Flowable<ExecutionEvent> executeStream(
        WorkflowDefinition workflow,
        Map<String, Object> input
    );
}
```

### 阶段 W2：内置节点实现 (P0-P1)

```java
// src/main/java/org/cloudnook/knightagent/workflow/nodes/
├── io/
│   ├── InputNode.java           // 输入节点
│   └── OutputNode.java          // 输出节点
├── agent/
│   └── AgentNode.java           // Agent 节点（集成 core/agent）
├── logic/
│   ├── CodeNode.java            // 代码节点
│   └── ConditionNode.java       // 条件节点
└── external/
    └── HttpNode.java            // HTTP 节点
```

**AgentNode 实现**:
```java
@Component
public class AgentNode extends AbstractNode<AgentNodeConfig> {
    private final AgentFactory agentFactory;

    @Override
    public NodeExecutionResult execute(NodeContext context) {
        AgentNodeConfig config = getConfig();

        // 创建 Agent（复用 core/agent 的 AgentFactory）
        Agent agent = agentFactory.createAgent()
            .model(loadModel(config.getModel()))
            .tools(loadMcpTools(config.getMcpTools()))
            .config(AgentConfig.builder()
                .systemPrompt(config.getSystemPrompt())
                .temperature(config.getTemperature())
                .build())
            .build();

        // 执行 Agent
        AgentRequest request = AgentRequest.builder()
            .input(context.getInput())
            .build();

        AgentResponse response = agent.invoke(request);

        return NodeExecutionResult.builder()
            .output(Map.of(
                "answer", response.getOutput(),
                "toolCalls", response.getToolCalls()
            ))
            .status(ExecutionStatus.COMPLETED)
            .build();
    }
}
```

### 阶段 W3：持久化 (P1)

```java
// src/main/java/org/cloudnook/knightagent/workflow/repository/
├── WorkflowRepository.java       // JPA Repository
├── WorkflowEntity.java           // 数据库实体
└── ExecutionRepository.java      // 执行历史
```

**数据库表设计**:
```sql
-- 工作流表
CREATE TABLE workflows (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    definition JSONB NOT NULL,  -- 工作流定义
    version INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    tags TEXT[]
);

-- 执行历史表
CREATE TABLE workflow_executions (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) REFERENCES workflows(id),
    status VARCHAR(32) NOT NULL,
    input JSONB,
    output JSONB,
    error TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms BIGINT
);

-- 节点执行历史表
CREATE TABLE node_executions (
    id VARCHAR(64) PRIMARY KEY,
    execution_id VARCHAR(64) REFERENCES workflow_executions(id),
    node_id VARCHAR(64) NOT NULL,
    node_name VARCHAR(255),
    status VARCHAR(32),
    input JSONB,
    output JSONB,
    error TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);
```

### 阶段 W4：REST API (P0)

```java
// src/main/java/org/cloudnook/knightagent/api/
├── controller/
│   ├── WorkflowController.java    // 工作流 CRUD
│   ├── ExecutionController.java   // 执行控制
│   └── WebSocketController.java   // WebSocket
├── service/
│   ├── WorkflowService.java       // 业务逻辑
│   └── ExecutionService.java      // 执行服务
├── repository/
│   └── (见上文)
└── dto/
    ├── WorkflowDTO.java
    ├── ExecutionDTO.java
    └── ApiResponse.java
```

**API 端点**:
```java
// WorkflowController
@GetMapping("/api/workflows")
public PaginatedResponse<WorkflowDTO> listWorkflows(
    @RequestParam int page,
    @RequestParam int pageSize,
    @RequestParam(required = false) String search
) { ... }

@GetMapping("/api/workflows/{id}")
public WorkflowDTO getWorkflow(@PathVariable String id) { ... }

@PostMapping("/api/workflows")
public WorkflowDTO createWorkflow(@RequestBody CreateWorkflowDTO dto) { ... }

@PutMapping("/api/workflows/{id}")
public WorkflowDTO updateWorkflow(
    @PathVariable String id,
    @RequestBody UpdateWorkflowDTO dto
) { ... }

@DeleteMapping("/api/workflows/{id}")
public void deleteWorkflow(@PathVariable String id) { ... }

// ExecutionController
@PostMapping("/api/workflows/{id}/execute")
public ExecutionDTO executeWorkflow(
    @PathVariable String id,
    @RequestBody ExecuteRequestDTO request
) { ... }

@PostMapping("/api/workflows/{id}/stream")
public StreamInfoDTO streamWorkflow(
    @PathVariable String id,
    @RequestBody ExecuteRequestDTO request
) { ... }

@GetMapping("/api/executions/{id}")
public ExecutionDTO getExecution(@PathVariable String id) { ... }

@GetMapping("/api/workflows/{id}/executions")
public PaginatedResponse<ExecutionDTO> getExecutionHistory(
    @PathVariable String id,
    @RequestParam int page,
    @RequestParam int pageSize
) { ... }

@Delete("/api/executions/{id}")
public void cancelExecution(@PathVariable String id) { ... }
```

### 阶段 W5：WebSocket 支持 (P1)

```java
// src/main/java/org/cloudnook/knightagent/api/websocket/
├── WorkflowExecutionHandler.java  // WebSocket 处理器
├── ExecutionEvent.java             // 事件类型
└── EventBroadcaster.java           // 事件广播器
```

**事件类型**:
```java
public enum ExecutionEventType {
    // 工作流级别
    WORKFLOW_STARTED,
    WORKFLOW_COMPLETED,
    WORKFLOW_FAILED,

    // 节点级别
    NODE_STARTED,
    NODE_COMPLETED,
    NODE_FAILED,

    // 流式输出
    TOKEN,              // AI 生成的 token
    TOOL_CALL,          // 工具调用
    TOOL_RESULT,        // 工具结果

    // 连接
    CONNECTED,
    DISCONNECTED,
    ERROR
}
```

**WebSocket 消息格式**:
```json
// 服务端 → 客户端
{
  "type": "node.started",
  "timestamp": "2026-01-25T09:23:45Z",
  "data": {
    "executionId": "exec_123",
    "nodeId": "agent_1",
    "nodeName": "Search Agent",
    "nodeType": "agent",
    "input": {"query": "weather Beijing"}
  }
}

{
  "type": "token",
  "timestamp": "2026-01-25T09:23:46Z",
  "data": {
    "executionId": "exec_123",
    "nodeId": "agent_1",
    "text": "Let me check the weather for Beijing..."
  }
}
```

---

## MCP 集成

### MCP 服务器配置

```java
// src/main/java/org/cloudnook/knightagent/workflow/mcp/
├── McpServerConfig.java         // MCP 配置
├── McpServerManager.java        // 服务器管理器
└── McpToolLoader.java           // 工具加载器
```

**McpServerConfig**:
```java
@Data
@Builder
public class McpServerConfig {
    private String id;
    private String name;
    private String description;
    private McpProtocol protocol;  // STDIO, SSE, WS
    private String command;        // for STDIO
    private String url;            // for SSE/WS
    private List<String> args;
    private Map<String, String> envVars;
}
```

### Agent 节点集成 MCP

```java
// Agent 节点配置中使用 MCP 工具
@Data
public class AgentNodeConfig {
    private String model;
    private String systemPrompt;
    private List<McpToolRef> mcpTools;  // 引用的 MCP 工具
    private Double temperature;
    private Integer maxTokens;
    // ...
}

@Data
public class McpToolRef {
    private String serverId;      // MCP 服务器 ID
    private List<String> tools;   // 工具名称列表
}
```

---

## 变量解析系统

### 变量语法
```
{{input.fieldName}}     - 工作流输入
{{nodeId.fieldName}}    - 节点输出
{{context.varName}}     - 上下文变量
```

### 解析器实现

```java
// src/main/java/org/cloudnook/knightagent/workflow/variable/
├── VariableParser.java         // 解析器
├── VariableResolver.java       // 解析器（替换变量为实际值）
└── VariableContext.java        // 变量上下文
```

**VariableParser**:
```java
public class VariableParser {
    private static final Pattern VARIABLE_PATTERN =
        Pattern.compile("\\{\\{([^}]+)\\}\\}");

    // 提取所有变量引用
    public static List<VariableReference> extractVariables(String template) {
        List<VariableReference> refs = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            refs.add(VariableReference.builder()
                .raw(matcher.group(0))       // {{input.query}}
                .expression(matcher.group(1)) // input.query
                .start(matcher.start())
                .end(matcher.end())
                .build());
        }
        return refs;
    }

    // 解析变量引用
    public static VariableReference parseVariable(String expression) {
        String[] parts = expression.split("\\.");
        if (parts.length < 2) {
            throw new VariableParseException("Invalid variable: " + expression);
        }
        return VariableReference.builder()
            .source(parts[0])          // input / nodeId / context
            .path(Arrays.copyOfRange(parts, 1, parts.length))
            .build();
    }
}
```

**VariableResolver**:
```java
public class VariableResolver {
    // 解析变量（替换为实际值）
    public static String resolve(String template, VariableContext context) {
        List<VariableReference> vars = VariableParser.extractVariables(template);
        String result = template;
        for (VariableReference var : vars) {
            Object value = context.getVariable(var.getExpression());
            result = result.replace(var.getRaw(), String.valueOf(value));
        }
        return result;
    }

    // 解析 JSON 中的变量
    public static Object resolveJson(Object json, VariableContext context) {
        if (json instanceof String) {
            return resolve((String) json, context);
        } else if (json instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) json).entrySet()) {
                result.put(entry.getKey(), resolveJson(entry.getValue(), context));
            }
            return result;
        } else if (json instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object item : (List<?>) json) {
                result.add(resolveJson(item, context));
            }
            return result;
        }
        return json;
    }
}
```

---

## 开发规范

### 包命名
```
org.cloudnook.knightagent.workflow.*
├── node/           // 节点相关
├── definition/     // 定义相关
├── engine/         // 执行引擎
├── nodes/          // 内置节点
│   ├── io/
│   ├── agent/
│   ├── logic/
│   └── external/
├── repository/     // 持久化
├── mcp/            // MCP 集成
└── variable/       // 变量系统
```

### 代码风格
- 使用 Lombok 注解减少样板代码
- Builder 模式用于复杂对象
- Optional 用于可能为空的返回值
- 异常使用 `KnightAgentException`

### 测试要求
- 单元测试覆盖率 > 80%
- 集成测试覆盖关键流程
- 使用 TestContainers 测试数据库操作

---

## 依赖关系

```
workflow 模块依赖：
├── core (Agent 框架)
│   ├── agent/          // AgentNode 需要
│   ├── tool/           // 工具调用需要
│   └── mcp/            // MCP 集成需要
└── Spring Boot
    ├── Web             // REST API
    ├── WebSocket       // 流式推送
    ├── Data JPA        // 持久化
    └── PostgreSQL      // 数据库
```

### Maven 依赖（新增）

```xml
<!-- WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- JSON Schema 验证 -->
<dependency>
    <groupId>com.networknt</groupId>
    <artifactId>json-schema-validator</artifactId>
    <version>1.0.87</version>
</dependency>

<!-- 表达式求值 (用于条件节点) -->
<dependency>
    <groupId>com.github.spullara.mustache.java</groupId>
    <artifactId>compiler</artifactId>
    <version>0.9.10</version>
</dependency>
```

---

## 开发优先级

### P0 - 核心功能（必须完成）
1. 节点抽象层（WorkflowNode 接口）
2. 工作流定义和解析
3. 基础执行引擎（同步执行）
4. Input/Output/Agent 节点
5. 工作流 CRUD API
6. 执行 API（同步）

### P1 - 重要功能
1. Condition/Code/HTTP 节点
2. 持久化（PostgreSQL）
3. WebSocket 流式执行
4. 变量解析系统
5. MCP 集成

### P2 - 增强功能
1. Tool 节点
2. Loop 节点
3. 执行历史查询
4. 工作流版本管理
5. 性能优化

---

## 快速开始

### 1. 创建基础包结构
```bash
mkdir -p src/main/java/org/cloudnook/knightagent/workflow/{node,definition,engine,nodes}
```

### 2. 先从节点抽象层开始
```java
// WorkflowNode.java
public interface WorkflowNode {
    String getId();
    NodeType getType();
    String getName();
    NodeExecutionResult execute(NodeContext context);
}
```

### 3. 然后实现工作流定义
```java
// WorkflowDefinition.java
@Data
@Builder
public class WorkflowDefinition {
    private String id;
    private String name;
    private List<NodeDefinition> nodes;
    private List<EdgeDefinition> edges;
}
```

### 4. 最后实现执行引擎
```java
// DefaultWorkflowEngine.java
@Service
public class DefaultWorkflowEngine implements WorkflowEngine {
    @Override
    public ExecutionResult execute(WorkflowDefinition workflow, Map<String, Object> input) {
        // 1. 拓扑排序
        // 2. 按顺序执行节点
        // 3. 收集输出
        // 4. 返回结果
    }
}
```

---

## 常见问题

### Q: 如何复用 core/agent 的 Agent？
A: AgentNode 中注入 AgentFactory，使用工厂方法创建 Agent。

### Q: 变量解析何时进行？
A: 节点执行前，解析配置中的变量（如 HTTP URL、Agent Prompt）。

### Q: 如何处理循环依赖？
A: 使用拓扑排序检测，如果发现循环则抛出异常。

### Q: WebSocket 如何实现？
A: 使用 Spring WebSocket，通过 SimpMessagingTemplate 发送事件。

---

## 参考文档

- 前端开发提示词：`docs/V0_PROMPT_V2.md`
- Agent 框架文档：`CLAUDE.md`
- TODO 列表：`TODO.md`

---

**开始开发吧！记住：先实现核心功能，再逐步添加细节。** 🚀
