package org.cloudnook.knightagent.core.agent;

import org.cloudnook.knightagent.core.agent.strategy.ExecutionContext;
import org.cloudnook.knightagent.core.agent.strategy.ExecutionStrategy;
import org.cloudnook.knightagent.core.agent.strategy.ReActStrategy;
import org.cloudnook.knightagent.core.checkpoint.CheckpointException;
import org.cloudnook.knightagent.core.checkpoint.Checkpointer;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.message.ToolMessage;
import org.cloudnook.knightagent.core.message.ToolResult;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.state.AgentState;
import org.cloudnook.knightagent.core.streaming.StreamCallback;
import org.cloudnook.knightagent.core.tool.McpTool;
import org.cloudnook.knightagent.core.tool.ToolExecutionException;
import org.cloudnook.knightagent.core.tool.ToolInvoker;
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.middleware.MiddlewareChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 执行器
 * <p>
 * 负责 Agent 的核心执行逻辑，包括：
 * <ul>
 *   <li>策略执行委托</li>
 *   <li>人机交互处理（通用逻辑）</li>
 *   <li>审批恢复执行（通用逻辑）</li>
 * </ul>
 * 实现 {@link Agent} 接口，作为框架的主要入口点。
 * <p>
 * 执行流程：
 * <pre>
 * 1. 执行策略
 * 2. 检查是否需要审批
 * 3. 如果需要审批：保存 checkpoint，返回等待审批响应
 * 4. 否则：返回正常响应
 * </pre>
 * <p>
 * 恢复流程：
 * <pre>
 * 1. 从 checkpoint 恢复状态
 * 2. 根据审批决策处理工具
 * 3. 继续执行策略
 * </pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class AgentExecutor implements Agent {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutor.class);

    // 策略模式支持
    private final ExecutionStrategy strategy;
    private final ExecutionContext executionContext;

    // 向后兼容的遗留字段
    private final ChatModel model;
    private final List<McpTool> tools;
    private final ToolInvoker toolInvoker;
    private final Checkpointer checkpointer;
    private final AgentConfig config;
    private final MiddlewareChain middlewareChain;

    /**
     * 策略模式构造函数（推荐）
     *
     * @param strategy        执行策略
     * @param executionContext 执行上下文
     */
    public AgentExecutor(ExecutionStrategy strategy, ExecutionContext executionContext) {
        this.strategy = strategy;
        this.executionContext = executionContext;

        // 设置兼容字段
        this.model = executionContext.getModel();
        this.config = executionContext.getConfig();
        this.middlewareChain = executionContext.getMiddlewareChain();
        this.toolInvoker = executionContext.getToolInvoker();
        this.checkpointer = executionContext.getCheckpointer();
        this.tools = List.of(); // 策略模式下不需要单独存储工具
    }

    /**
     * 传统构造函数（向后兼容）
     *
     * @param model        LLM 模型
     * @param tools        工具列表
     * @param checkpointer 检查点存储
     * @param config       Agent 配置
     * @param middlewares  中间件列表
     */
    @SuppressWarnings("unused")
    public AgentExecutor(ChatModel model, List<McpTool> tools, Checkpointer checkpointer,
                          AgentConfig config, List<Middleware> middlewares) {
        // 使用默认 ReAct 策略
        this.strategy = new ReActStrategy();

        this.model = model;
        this.tools = tools != null ? tools : List.of();
        this.toolInvoker = new ToolInvoker(this.tools);
        this.checkpointer = checkpointer;
        this.config = config != null ? config : AgentConfig.defaults();
        this.middlewareChain = new MiddlewareChain(
                middlewares != null ? middlewares : List.of()
        );

        // 构建执行上下文
        this.executionContext = ExecutionContext.builder()
                .model(this.model)
                .toolInvoker(this.toolInvoker)
                .checkpointer(this.checkpointer)
                .config(this.config)
                .middlewareChain(this.middlewareChain)
                .build();
    }

    /**
     * 执行 Agent（同步）
     * <p>
     * 使用配置的执行策略执行请求，并处理人机交互。
     *
     * @param request Agent 请求
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    public AgentResponse execute(AgentRequest request) throws AgentExecutionException {
        return strategy.execute(request, executionContext);
    }

    /**
     * 流式执行 Agent
     * <p>
     * 如果策略是 ReActStrategy，使用其流式方法；否则抛出异常。
     *
     * @param request  Agent 请求
     * @param callback 流式回调
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    public AgentResponse executeStream(AgentRequest request, StreamCallback callback) throws AgentExecutionException {
        return strategy.executeStream(request, callback, executionContext);
    }

    // ==================== Agent 接口实现 ====================

    /**
     * 同步执行 Agent（Agent 接口实现）
     * <p>
     * 处理人机交互的通用逻辑：
     * <ul>
     *   <li>执行策略</li>
     *   <li>检查是否需要审批</li>
     *   <li>如果需要审批：保存 checkpoint（如果需要），返回等待审批响应</li>
     *   <li>否则：返回正常响应</li>
     * </ul>
     *
     * @param request Agent 请求
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    @Override
    public AgentResponse invoke(AgentRequest request) throws AgentExecutionException {
        AgentResponse response = execute(request);

        // 检查是否需要审批
        if (response.requiresApproval()) {
            ApprovalRequest approval = response.getApprovalRequest();

            // 保存 checkpoint（如果策略没有保存）
            if (approval.getCheckpointId() == null && checkpointer != null) {
                try {
                    String checkpointId = checkpointer.save(config.getThreadId(), response.getState());
                    approval.setCheckpointId(checkpointId);
                    response = response.toBuilder()
                            .checkpointId(checkpointId)
                            .approvalRequest(approval)
                            .build();
                } catch (CheckpointException e) {
                    throw new AgentExecutionException("保存 checkpoint 失败: " + e.getMessage(), e, "CHECKPOINT_ERROR");
                }
            }

            log.info("Agent 执行暂停，等待人工审批: 审批ID={}, 工具={}",
                    approval.getApprovalId(), approval.getToolName());
        }

        return response;
    }

    /**
     * 流式执行 Agent（Agent 接口实现）
     *
     * @param request  Agent 请求
     * @param callback 流式回调
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    @Override
    public AgentResponse stream(AgentRequest request, StreamCallback callback) throws AgentExecutionException {
        return executeStream(request, callback);
    }

    /**
     * 批量执行 Agent（Agent 接口实现）
     * <p>
     * 按顺序执行多个请求，返回响应列表。
     *
     * @param requests Agent 请求列表
     * @return Agent 响应列表
     * @throws AgentExecutionException 执行失败
     */
    @Override
    public List<AgentResponse> batch(List<AgentRequest> requests) throws AgentExecutionException {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        List<AgentResponse> responses = new ArrayList<>();
        for (AgentRequest request : requests) {
            responses.add(invoke(request));
        }
        return responses;
    }

    /**
     * 获取 Agent 配置（Agent 接口实现）
     *
     * @return Agent 配置
     */
    @Override
    public AgentConfig getConfig() {
        return config;
    }

    /**
     * 从审批恢复执行（Agent 接口实现）
     * <p>
     * 通用的恢复执行逻辑，不依赖具体的 Strategy 实现：
     * <ol>
     *   <li>从 checkpoint 恢复状态</li>
     *   <li>根据审批决策处理工具（REJECT/ALLOW/EDIT）</li>
     *   <li>继续执行策略</li>
     * </ol>
     *
     * @param checkpointId checkpoint ID
     * @param approval     审批请求（必须包含决策）
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    @Override
    public AgentResponse resume(String checkpointId, ApprovalRequest approval) throws AgentExecutionException {
        if (checkpointer == null) {
            throw new AgentExecutionException("Checkpointer 未配置，无法恢复执行");
        }

        try {
            // 1. 从 checkpoint 恢复状态
            AgentState state = checkpointer.load(approval.getThreadId(), checkpointId)
                    .orElseThrow(() -> new AgentExecutionException("Checkpoint 不存在: " + checkpointId));

            log.info("从 checkpoint 恢复执行: checkpointId={}, 决策={}", checkpointId, approval.getDecision());

            // 2. 根据审批决策处理工具
            state = handleApprovalDecision(state, approval);

            // 3. 构建继续执行的请求（不需要传递 state 和 threadId，已在 config 中）
            AgentRequest continuationRequest = AgentRequest.builder()
                    .build();

            // 4. 继续执行策略
            return execute(continuationRequest);

        } catch (CheckpointException e) {
            throw new AgentExecutionException("Checkpoint 操作失败: " + e.getMessage(), e, "CHECKPOINT_ERROR");
        } catch (ToolExecutionException e) {
            throw new AgentExecutionException("工具执行失败: " + e.getMessage(), e, "TOOL_ERROR");
        } catch (Exception e) {
            throw new AgentExecutionException("恢复执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理审批决策
     * <p>
     * 根据审批决策对状态进行相应的处理：
     * <ul>
     *   <li>REJECT: 添加拒绝消息到状态</li>
     *   <li>ALLOW: 执行工具，添加结果到状态</li>
     *   <li>EDIT: 使用修改后的参数执行工具</li>
     * </ul>
     *
     * @param state    当前状态
     * @param approval 审批请求
     * @return 更新后的状态
     * @throws ToolExecutionException 工具执行失败
     */
    private AgentState handleApprovalDecision(AgentState state, ApprovalRequest approval) throws ToolExecutionException {
        return switch (approval.getDecision()) {
            case REJECT -> {
                // 拒绝：将拒绝原因作为 ToolMessage 返回给 LLM
                String rejectReason = approval.getRejectReason() != null
                        ? approval.getRejectReason()
                        : "用户拒绝了该操作";

                ToolMessage toolMessage = ToolMessage.builder()
                        .toolCallId(approval.getToolCall().getId())
                        .result("[用户拒绝] " + rejectReason)
                        .error(true)
                        .errorMessage(rejectReason)
                        .build();

                yield state.addMessage(toolMessage);
            }

            case ALLOW -> {
                // 允许：使用原始参数执行工具
                ToolResult result = toolInvoker.invoke(approval.getToolCall());
                try {
                    middlewareChain.afterToolCall(approval.getToolCall(), result,
                            new org.cloudnook.knightagent.core.middleware.AgentContext(
                                    AgentRequest.builder().build()));
                } catch (org.cloudnook.knightagent.core.middleware.MiddlewareException e) {
                    log.warn("中间件 afterToolCall 处理失败: {}", e.getMessage());
                }

                ToolMessage toolMessage = result.toMessage();
                yield state.addMessage(toolMessage);
            }

            case EDIT -> {
                // 编辑：使用修改后的参数执行工具
                ToolCall modifiedToolCall = ToolCall.builder()
                        .id(approval.getToolCall().getId())
                        .name(approval.getToolName())
                        .arguments(approval.getModifiedArguments())
                        .build();

                ToolResult result = toolInvoker.invoke(modifiedToolCall);
                try {
                    middlewareChain.afterToolCall(modifiedToolCall, result,
                            new org.cloudnook.knightagent.core.middleware.AgentContext(
                                    AgentRequest.builder().build()));
                } catch (org.cloudnook.knightagent.core.middleware.MiddlewareException e) {
                    log.warn("中间件 afterToolCall 处理失败: {}", e.getMessage());
                }

                ToolMessage toolMessage = result.toMessage();
                yield state.addMessage(toolMessage);
            }
        };
    }
}
