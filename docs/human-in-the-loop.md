# 人机协作（Human-in-the-Loop）指南

## 概述

KnightAgent 的人机协作（Human-in-the-Loop，简称 HITL）功能允许 Agent 在执行敏感操作前暂停，等待人工审批。这为 AI Agent 的安全性和可控性提供了重要保障。

### 核心特性

- ✅ **中断暂停** - Agent 执行到敏感工具时自动暂停
- ✅ **状态保存** - 自动保存执行状态到 checkpoint
- ✅ **审批决策** - 支持允许、拒绝、修改参数三种决策模式
- ✅ **智能反馈** - 拒绝原因会反馈给 LLM，帮助其调整策略
- ✅ **无缝恢复** - 审批后从 checkpoint 恢复执行

### 适用场景

| 场景 | 说明 |
|------|------|
| **敏感操作** | 文件删除、邮件发送、数据库修改等 |
| **高风险操作** | 系统配置变更、权限变更、资金操作 |
| **合规要求** | 金融、医疗等需要人工确认的场景 |
| **调试模式** | 开发时观察和控制 Agent 行为 |

---

## 工作流程

```
┌─────────────────────────────────────────────────────────────────┐
│                     人机协作执行流程                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐                                           │
│  │  1. Agent 执行   │                                           │
│  └────────┬────────┘                                           │
│           │                                                    │
│           v                                                    │
│  ┌─────────────────┐                                           │
│  │ 2. LLM 决策调用  │                                           │
│  │    敏感工具       │                                           │
│  └────────┬────────┘                                           │
│           │                                                    │
│           v                                                    │
│  ┌─────────────────────────────────────┐                       │
│  │ 3. HumanInTheLoopMiddleware 检测     │                       │
│  │    └─> 设置 context.pendingApproval  │                       │
│  └────────┬────────────────────────────┘                       │
│           │                                                    │
│           v                                                    │
│  ┌─────────────────────────────────────┐                       │
│  │ 4. ReActStrategy 检测到待审批        │                       │
│  │    └─> 保存 checkpoint              │                       │
│  │    └─> 返回"等待审批"响应            │                       │
│  └────────┬────────────────────────────┘                       │
│           │                                                    │
│           v                                                    │
│  ┌─────────────────────────────────────┐   ┌─────────────────┐ │
│  │ 5. 应用展示审批界面                  │──>│  用户做出决策    │ │
│  │    - 显示工具名称和参数              │   │                 │ │
│  │    - 显示审批按钮                    │   │  ALLOW/REJECT/  │ │
│  └─────────────────────────────────────┘   │  EDIT           │ │
│                                            └─────────────────┘ │
│                                                       │         │
└──────────────────────────────────────────────┼─────────────────┘
                                                       │
                                                       v
┌─────────────────────────────────────────────────────────────────┐
│  6. 调用 agent.resume(checkpointId, approval)                      │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ ReActStrategy.resumeFromApproval()                        │    │
│  │   └─> 从 checkpoint 恢复状态                               │    │
│  │   └─> 根据决策处理：                                       │    │
│  │        • ALLOW: 执行工具                                   │    │
│  │        • REJECT: 拒绝原因反馈给 LLM                        │    │
│  │        • EDIT: 使用修改后的参数执行工具                     │    │
│  └────────┬────────────────────────────────────────────────┘    │
│           │                                                      │
│           v                                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ 7. 继续执行剩余的 Agent 流程                               │    │
│  │    └─> 调用 LLM                                            │    │
│  │    └─> 执行工具                                            │    │
│  │    └─> 返回最终响应                                        │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 快速开始

### 基础配置

```java
// 1. 创建 Checkpointer（必需，用于保存和恢复状态）
Checkpointer checkpointer = new PostgresSaver(dataSource);

// 2. 创建带人机协作的 Agent
Agent agent = AgentBuilder.builder()
    .model(chatModel)
    .tools(List.of(
        new DeleteFileTool(),
        new SendEmailTool(),
        new SearchTool()  // 普通工具，不需要审批
    ))
    .middleware(HumanInTheLoopMiddleware.builder()
        .mode(ReviewMode.WHITELIST)  // 白名单模式
        .whitelist(List.of("delete_file", "send_email"))
        .build())
    .checkpointer(checkpointer)
    .build();
```

### 基本使用

```java
// 执行 Agent
AgentResponse response = agent.invoke(AgentRequest.of(
    "请删除 /home/user/important.txt 文件"
));

// 检查是否需要审批
if (response.requiresApproval()) {
    ApprovalRequest approval = response.getApprovalRequest();

    // 处理审批（见下一节）
    response = handleApproval(approval, agent);
}

// 获取最终结果
System.out.println(response.getOutput());
```

---

## 审批模式详解

### 审核模式（ReviewMode）

| 模式 | 说明 | 使用场景 |
|------|------|----------|
| **ALWAYS** | 所有工具调用都需要审批 | 高安全要求、调试环境 |
| **WHITELIST** | 只有白名单中的工具需要审批 | 默认推荐，明确指定敏感工具 |
| **BLACKLIST** | 除黑名单外的工具都需要审批 | 大部分工具敏感的场景 |
| **NEVER** | 不进行审批 | 生产环境、完全自动化 |

### 审批决策（ApprovalDecision）

#### ALLOW - 允许执行

使用原始参数执行工具：

```java
approval.allow();
response = agent.resume(approval.getCheckpointId(), approval);
```

#### REJECT - 拒绝执行

拒绝工具调用，**拒绝原因会反馈给 LLM**，帮助其调整策略：

```java
approval.reject("用户取消了删除文件操作");
response = agent.resume(approval.getCheckpointId(), approval);

// LLM 收到反馈后可能会说：
// "明白了，那我可以先查看文件内容，然后询问您是否真的要删除"
```

**关键特性**：REJECT 不是简单地阻止，而是让 LLM 学习用户意图，调整后续策略。

#### EDIT - 修改参数后执行

用户修改工具参数后再执行：

```java
// 原始参数: {"path": "/home/user/important.txt"}
// 修改为:   {"path": "/home/user/temp.txt"}

approval.edit("{\"path\": \"/home/user/temp.txt\"}");
response = agent.resume(approval.getCheckpointId(), approval);
```

---

## 完整示例

### 示例 1：命令行审批界面

```java
public class ApprovalExample {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // 创建 Agent
        Agent agent = createAgent();

        // 执行
        AgentResponse response = agent.invoke(AgentRequest.of(
            "请删除 /home/user/documents/report.pdf 文件"
        ));

        // 处理审批
        if (response.requiresApproval()) {
            response = handleApproval(response.getApprovalRequest(), agent);
        }

        System.out.println("最终结果: " + response.getOutput());
    }

    private static AgentResponse handleApproval(ApprovalRequest approval, Agent agent) {
        System.out.println("\n===== 工具调用审批 ======");
        System.out.println("工具: " + approval.getToolName());
        System.out.println("参数: " + approval.getOriginalArguments());
        System.out.println("========================");

        while (true) {
            System.out.print("请选择 (a=允许, r=拒绝, e=编辑): ");
            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "a", "allow", "y", "yes":
                    approval.allow();
                    break;
                case "r", "reject", "n", "no":
                    System.out.print("请输入拒绝原因: ");
                    String reason = scanner.nextLine();
                    approval.reject(reason);
                    break;
                case "e", "edit":
                    System.out.print("请输入新的参数 (JSON): ");
                    String newArgs = scanner.nextLine();
                    approval.edit(newArgs);
                    break;
                default:
                    System.out.println("无效输入，请重新选择");
                    continue;
            }

            return agent.resume(approval.getCheckpointId(), approval);
        }
    }
}
```

### 示例 2：Web API 审批

```java
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final Agent agent;

    // 1. 执行 Agent
    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestBody AgentRequest request) {
        AgentResponse response = agent.invoke(request);

        if (response.requiresApproval()) {
            // 返回审批请求，前端展示审批界面
            return ResponseEntity.ok(Map.of(
                "status", "waiting_approval",
                "approval", response.getApprovalRequest()
            ));
        }

        return ResponseEntity.ok(response);
    }

    // 2. 处理审批
    @PostMapping("/approve/{approvalId}")
    public ResponseEntity<?> approve(
            @PathVariable String approvalId,
            @RequestBody ApprovalDecisionRequest decision) {

        // 获取待审批的请求（通常从缓存或数据库获取）
        ApprovalRequest approval = getPendingApproval(approvalId);

        // 设置决策
        switch (decision.getAction()) {
            case "allow" -> approval.allow();
            case "reject" -> approval.reject(decision.getReason());
            case "edit" -> approval.edit(decision.getModifiedArgs());
        }

        // 恢复执行
        AgentResponse response = agent.resume(
            approval.getCheckpointId(),
            approval
        );

        return ResponseEntity.ok(response);
    }
}
```

### 示例 3：异步审批（消息队列）

```java
@Service
public class AgentService {

    private final Agent agent;
    private final ApprovalQueue approvalQueue;

    public CompletableFuture<AgentResponse> executeAsync(AgentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            AgentResponse response = agent.invoke(request);

            if (response.requiresApproval()) {
                // 发送到审批队列
                approvalQueue.submit(response.getApprovalRequest());

                // 等待审批完成
                return waitForApproval(response.getApprovalRequest());
            }

            return response;
        });
    }

    public void processApproval(String approvalId, ApprovalDecision decision) {
        ApprovalRequest approval = approvalQueue.get(approvalId);
        approval.setDecision(decision);

        // 恢复执行
        AgentResponse response = agent.resume(
            approval.getCheckpointId(),
            approval
        );

        // 保存结果或通知调用者
        saveResult(approvalId, response);
    }
}
```

---

## API 参考

### HumanInTheLoopMiddleware

```java
public class HumanInTheLoopMiddleware implements Middleware {

    // 审核模式
    public enum ReviewMode {
        ALWAYS,      // 所有工具都需要审批
        WHITELIST,   // 白名单工具需要审批
        BLACKLIST,   // 非黑名单工具需要审批
        NEVER        // 不需要审批
    }

    // 创建中间件
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        public Builder mode(ReviewMode mode);
        public Builder whitelist(List<String> tools);
        public Builder blacklist(List<String> tools);
        public HumanInTheLoopMiddleware build();
    }
}
```

### ApprovalRequest

```java
public class ApprovalRequest {

    // 审批决策
    public enum ApprovalDecision {
        ALLOW,   // 允许执行
        REJECT,  // 拒绝执行
        EDIT     // 修改参数后执行
    }

    // 设置决策
    public ApprovalRequest allow();
    public ApprovalRequest reject(String reason);
    public ApprovalRequest edit(String modifiedArguments);

    // 获取最终参数
    public String getFinalArguments();  // 根据决策获取最终参数

    // 创建审批请求
    public static ApprovalRequest fromToolCall(
        ToolCall toolCall,
        String threadId,
        String checkpointId
    );
}
```

### Agent 接口新增方法

```java
public interface Agent {

    // 检查响应是否需要审批
    default boolean requiresApproval();

    // 从审批恢复执行
    AgentResponse resume(String checkpointId, ApprovalRequest approval)
        throws AgentExecutionException;
}
```

---

## 最佳实践

### 1. Checkpointer 配置

人机协作功能**必须**配置 Checkpointer，用于保存和恢复执行状态：

```java
// 生产环境推荐使用持久化 Checkpointer
Checkpointer checkpointer = new PostgresSaver(dataSource);

// 开发/测试可以使用内存实现
Checkpointer checkpointer = new InMemorySaver();
```

### 2. 选择合适的审核模式

```java
// 推荐：WHITELIST 模式（默认安全）
.middleware(HumanInTheLoopMiddleware.builder()
    .mode(ReviewMode.WHITELIST)
    .whitelist(List.of(
        "delete_file",
        "send_email",
        "transfer_money",
        "execute_command"
    ))
    .build())

// 调试环境：ALWAYS 模式
.middleware(HumanInTheLoopMiddleware.builder()
    .mode(ReviewMode.ALWAYS)
    .build())

// 生产环境：NEVER 模式（完全自动化）
.middleware(HumanInTheLoopMiddleware.builder()
    .mode(ReviewMode.NEVER)
    .build())
```

### 3. 提供有意义的拒绝原因

```java
// 好的拒绝原因 - 帮助 LLM 理解用户意图
approval.reject("文件正在使用中，无法删除");

// 好的拒绝原因 - 给出替代方案
approval.reject("用户不允许删除，但可以移动到回收站");

// 不好的拒绝原因 - LLM 无法学习
approval.reject("no");
```

### 4. 处理嵌套审批

如果一次恢复后再次触发审批，需要循环处理：

```java
AgentResponse response = agent.invoke(request);

while (response.requiresApproval()) {
    ApprovalRequest approval = response.getApprovalRequest();

    // 获取用户决策
    processApproval(approval);

    // 恢复执行
    response = agent.resume(approval.getCheckpointId(), approval);
}
```

### 5. 设置审批超时

```java
public class TimedApprovalService {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AgentResponse executeWithTimeout(
        AgentRequest request,
        Agent agent,
        long timeoutMinutes
    ) {
        Future<AgentResponse> future = executor.submit(() -> agent.invoke(request));

        try {
            AgentResponse response = future.get(timeoutMinutes, TimeUnit.MINUTES);

            if (response.requiresApproval()) {
                // 等待审批，设置更长的超时
                return waitForApproval(response.getApprovalRequest(), agent, 24 * 60);
            }

            return response;
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AgentExecutionException("执行超时");
        }
    }
}
```

---

## 与其他框架对比

| 特性 | KnightAgent | LangGraph | Microsoft Agent Framework |
|------|-------------|-----------|---------------------------|
| 中断机制 | Middleware | interrupt() | InterruptAsync() |
| 状态保存 | Checkpointer | Checkpointer | Checkpointer |
| 恢复执行 | resume() | re-enter | ResumeAsync() |
| REJECT 反馈 | ✅ 反馈给 LLM | ❌ 仅阻止 | ✅ 反馈给 LLM |
| EDIT 模式 | ✅ 支持 | ❌ 不支持 | ✅ 支持 |
| 嵌套审批 | ✅ 支持 | ✅ 支持 | ✅ 支持 |

---

## 常见问题

### Q: 为什么必须配置 Checkpointer？

A: 人机协作需要在审批前后保存和恢复 Agent 的执行状态。没有 Checkpointer，无法在审批后继续执行。

### Q: REJECT 和简单阻止有什么区别？

A: REJECT 会将拒绝原因作为 ToolMessage 反馈给 LLM，让 LLM 学习用户意图并调整后续策略。简单阻止只会中断执行，LLM 无法学习。

### Q: 如何处理多个工具同时需要审批？

A: KnightAgent 会逐个处理工具调用。每个工具都会触发一次审批流程，支持嵌套审批。

### Q: 审批请求会过期吗？

A: 不会。Checkpoint 持久化保存，审批请求可以随时处理。建议在业务层添加超时机制。

### Q: 流式执行支持人机协作吗？

A: 当前流式执行暂不支持人机协作。如需此功能，建议使用同步执行。

---

## 更多资源

- [源码](https://github.com/your-org/knight-agent)
- [API 文档](https://docs.knight-agent.dev)
- [示例代码](https://github.com/your-org/knight-agent/examples)
