package org.cloudnook.knightagent.core.agent.strategy;

import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.agent.AgentExecutionException;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.AgentStatus;
import org.cloudnook.knightagent.core.agent.ApprovalRequest;
import org.cloudnook.knightagent.core.checkpoint.Checkpointer;
import org.cloudnook.knightagent.core.checkpoint.CheckpointException;
import org.cloudnook.knightagent.core.message.*;
import org.cloudnook.knightagent.core.middleware.AgentContext;
import org.cloudnook.knightagent.core.middleware.MiddlewareChain;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.ChatOptions;
import org.cloudnook.knightagent.core.model.ModelException;
import org.cloudnook.knightagent.core.state.AgentState;
import org.cloudnook.knightagent.core.streaming.StreamCallback;
import org.cloudnook.knightagent.core.streaming.StreamCallbackAdapter;
import org.cloudnook.knightagent.core.streaming.StreamChunk;
import org.cloudnook.knightagent.core.streaming.StreamCompleteResponse;
import org.cloudnook.knightagent.core.tool.ToolExecutionException;
import org.cloudnook.knightagent.core.tool.ToolInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ReAct（推理-行动-观察）执行策略
 * <p>
 * 实现 ReAct 循环模式：
 * <pre>
 * Thought（思考）→ Action（行动）→ Observation（观察）→ ... → Final Answer
 * </pre>
 * <p>
 * 执行流程：
 * <ol>
 *   <li>加载历史状态（或使用 request 中的 state）</li>
 *   <li>通过中间件处理请求</li>
 *   <li>循环执行（最多 maxIterations 次）：
 *     <ul>
 *       <li>调用 LLM 获取响应</li>
 *       <li>如果有工具调用：执行工具 → 添加结果 → 继续循环</li>
 *       <li>否则：结束循环</li>
 *     </ul>
 *   </li>
 *   <li>应用状态归约器</li>
 *   <li>保存检查点</li>
 *   <li>通过中间件处理响应</li>
 * </ol>
 * <p>
 * 人机交互：
 * <br>
 * 当中间件触发审批（通过 {@link AgentContext#hasPendingApproval()}）时，
 * 策略会立即返回"等待审批"响应，由 {@code AgentExecutor} 统一处理审批流程。
 * 策略不需要关心如何保存 checkpoint 或如何恢复执行。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class ReActStrategy implements ExecutionStrategy {

    private static final Logger log = LoggerFactory.getLogger(ReActStrategy.class);

    @Override
    public String getName() {
        return "ReAct";
    }

    @Override
    public AgentResponse execute(AgentRequest request, ExecutionContext context) throws AgentExecutionException {
        long startTime = System.currentTimeMillis();
        Instant startInstant = Instant.now();

        ExecutionState executionState = null;
        try {
            executionState = initializeExecution(request, context);

            for (int i = 0; i < executionState.maxIterations; i++) {
                executionState.agentContext.setIteration(i + 1);
                log.debug("ReAct 迭代 {}/{}", i + 1, executionState.maxIterations);

                // 调用 LLM
                AIMessage aiMessage = callLLM(executionState);
                executionState.state = executionState.state.addMessage(aiMessage);
executionState.agentContext.setState(executionState.state);

                if (!aiMessage.hasToolCalls()) {
                    log.debug("无工具调用，结束循环");
                    return finalizeResponse(aiMessage, executionState, startTime, startInstant);
                }

                // 执行工具调用
                AgentResponse approvalResponse = executeToolCalls(aiMessage.getToolCalls(), executionState, startTime, startInstant);
                if (approvalResponse != null) {
                    return approvalResponse; // 需要审批
                }
            }

            return finalizeResponse(executionState.lastMessage, executionState, startTime, startInstant);

        } catch (ModelException e) {
            if (executionState != null) {
                executionState.agentContext.setStatus(AgentStatus.error("模型调用失败: " + e.getMessage()));
            }
            throw new AgentExecutionException("模型调用失败: " + e.getMessage(), e, "MODEL_ERROR");
        } catch (CheckpointException e) {
            if (executionState != null) {
                executionState.agentContext.setStatus(AgentStatus.error("检查点操作失败: " + e.getMessage()));
            }
            throw new AgentExecutionException("检查点操作失败: " + e.getMessage(), e, "CHECKPOINT_ERROR");
        } catch (Exception e) {
            if (executionState != null) {
                executionState.agentContext.setStatus(AgentStatus.error("Agent 执行失败: " + e.getMessage()));
            }
            throw new AgentExecutionException("Agent 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式执行
     * <p>
     * 支持流式输出和人机交互：
     * <ul>
     *   <li>实时流式返回 tokens 给用户</li>
     *   <li>工具调用时触发人机交互（如果需要）</li>
     *   <li>审批后可继续流式输出</li>
     * </ul>
     */
    public AgentResponse executeStream(AgentRequest request, StreamCallback callback, ExecutionContext context)
            throws AgentExecutionException {
        long startTime = System.currentTimeMillis();
        Instant startInstant = Instant.now();

        ExecutionState executionState = null;
        try {
            callback.onStart();

            executionState = initializeExecution(request, context);
            executionState.callback = callback;

            for (int i = 0; i < executionState.maxIterations; i++) {
                executionState.agentContext.setIteration(i + 1);

                // 流式调用 LLM
                AIMessage aiMessage = callLLMStream(executionState);
                executionState.state = executionState.state.addMessage(aiMessage);
executionState.agentContext.setState(executionState.state);

                if (!aiMessage.hasToolCalls()) {
                    log.debug("无工具调用，结束流式循环");
                    return finalizeResponse(aiMessage, executionState, startTime, startInstant);
                }

                // 执行工具调用
                AgentResponse approvalResponse = executeToolCalls(aiMessage.getToolCalls(), executionState, startTime, startInstant);
                if (approvalResponse != null) {
                    return approvalResponse; // 需要审批
                }
            }

            return finalizeResponse(executionState.lastMessage, executionState, startTime, startInstant);

        } catch (ModelException e) {
            if (executionState != null) {
                executionState.agentContext.setStatus(AgentStatus.error("模型调用失败: " + e.getMessage()));
            }
            callback.onError(e);
            throw new AgentExecutionException("模型调用失败: " + e.getMessage(), e, "MODEL_ERROR");
        } catch (CheckpointException e) {
            if (executionState != null) {
                executionState.agentContext.setStatus(AgentStatus.error("检查点操作失败: " + e.getMessage()));
            }
            callback.onError(e);
            throw new AgentExecutionException("检查点操作失败: " + e.getMessage(), e, "CHECKPOINT_ERROR");
        } catch (Exception e) {
            if (executionState != null) {
                executionState.agentContext.setStatus(AgentStatus.error("Agent 流式执行失败: " + e.getMessage()));
            }
            callback.onError(e);
            throw new AgentExecutionException("Agent 流式执行失败: " + e.getMessage(), e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 初始化执行状态
     */
    private ExecutionState initializeExecution(AgentRequest request, ExecutionContext context)
            throws CheckpointException, AgentExecutionException {
        ChatModel model = context.getModel();
        ToolInvoker toolInvoker = context.getToolInvoker();
        Checkpointer checkpointer = context.getCheckpointer();
        AgentConfig config = context.getConfig();
        MiddlewareChain middlewareChain = context.getMiddlewareChain();

        AgentContext agentContext = new AgentContext(request);

        AgentState state = loadOrCreateState(config, checkpointer);
        if (request.getInput() != null && !request.getInput().isBlank()) {
            state = state.addMessage(HumanMessage.of(request.getInput(), request.getUserId()));
        }

        // 同步到 agentContext，供中间件访问
        agentContext.setState(state);

        // 设置初始状态为 READY
        agentContext.setStatus(AgentStatus.ready());

        try {
            middlewareChain.beforeInvoke(request, agentContext);
        } catch (org.cloudnook.knightagent.core.middleware.MiddlewareException e) {
            throw new AgentExecutionException("中间件处理失败: " + e.getMessage(), e, "MIDDLEWARE_ERROR");
        }

        int maxIterations = getMaxIterations(request, config);

        return new ExecutionState(model, toolInvoker, checkpointer, config, middlewareChain,
                agentContext, state, maxIterations, request);
    }

    /**
     * 调用 LLM（同步）
     */
    private AIMessage callLLM(ExecutionState state) throws ModelException {
        // 设置运行状态
        state.agentContext.setStatus(AgentStatus.running(state.config.getThreadId()));

        List<Message> messages = buildMessages(state);
        ChatOptions options = getChatOptions(state);

        if (state.toolInvoker.getTools() != null && !state.toolInvoker.getTools().isEmpty()) {
            options = options.toBuilder()
                    .tools(state.toolInvoker.getTools())
                    .build();
        }

        return state.model.chat(messages, options);
    }

    /**
     * 调用 LLM（流式）
     */
    private AIMessage callLLMStream(ExecutionState state) throws ModelException {
        // 设置运行状态
        state.agentContext.setStatus(AgentStatus.running(state.config.getThreadId()));

        List<Message> messages = buildMessages(state);
        ChatOptions options = getChatOptions(state);

        if (state.toolInvoker.getTools() != null && !state.toolInvoker.getTools().isEmpty()) {
            options = options.toBuilder()
                    .tools(state.toolInvoker.getTools())
                    .build();
        }

        AtomicReference<String> fullContent = new AtomicReference<>();
        AtomicReference<List<ToolCall>> toolCalls = new AtomicReference<>();

        state.model.chatStream(messages, options, new StreamCallbackAdapter() {
            @Override
            public void onToken(StreamChunk chunk) {
                state.callback.onToken(chunk);
            }

            @Override
            public void onToolCall(StreamChunk chunk, ToolCall toolCall) {
                state.callback.onToolCall(chunk, toolCall);
            }

            @Override
            public void onReasoning(StreamChunk chunk, String reasoning) {
                state.callback.onReasoning(chunk, reasoning);
            }

            @Override
            public void onCompletion(StreamCompleteResponse response) {
                fullContent.set(response.getFullContent());
                toolCalls.set(response.getToolCalls());
                state.callback.onCompletion(response);
            }

            @Override
            public void onError(Throwable error) {
                state.callback.onError(error);
            }
        });

        state.lastMessage = AIMessage.of(
                fullContent.get() != null ? fullContent.get() : "",
                toolCalls.get() != null ? toolCalls.get() : List.of()
        );
        return state.lastMessage;
    }

    /**
     * 执行工具调用
     * @return 如果需要审批返回审批响应，否则返回 null
     */
    private AgentResponse executeToolCalls(List<ToolCall> toolCalls, ExecutionState state,
                                         long startTime, Instant startInstant) {
        for (ToolCall toolCall : toolCalls) {
            // 更新状态为等待工具执行
            state.agentContext.setStatus(AgentStatus.waitingForTool(
                    state.config.getThreadId(),
                    state.agentContext.getIteration()
            ));

            try {
                state.middlewareChain.beforeToolCall(toolCall, state.agentContext);
            } catch (org.cloudnook.knightagent.core.middleware.MiddlewareException e) {
                log.error("中间件 beforeToolCall 失败: {}", e.getMessage(), e);
                // 恢复运行状态
                state.agentContext.setStatus(AgentStatus.running(state.config.getThreadId()));
                continue;
            }

            if (state.agentContext.hasPendingApproval()) {
                // 设置等待审批状态
                state.agentContext.setStatus(AgentStatus.builder()
                        .statusType(AgentStatus.StatusType.WAITING_FOR_APPROVAL)
                        .description("等待工具审批: " + toolCall.getName())
                        .currentThreadId(state.config.getThreadId())
                        .currentIteration(state.agentContext.getIteration())
                        .build());
                return buildApprovalResponse(state, startTime, startInstant);
            }

            if (state.agentContext.isStopped()) {
                continue;
            }

            try {
                ToolResult toolResult = state.toolInvoker.invoke(toolCall);
                state.middlewareChain.afterToolCall(toolCall, toolResult, state.agentContext);
                state.state = state.state.addMessage(toolResult.toMessage());
                state.agentContext.setState(state.state);
            } catch (ToolExecutionException e) {
                log.error("工具执行失败: {}", e.getMessage(), e);
                // 继续执行，让 LLM 知道错误
                state.state = state.state.addMessage(resultToMessage(toolCall, e));
                state.agentContext.setState(state.state);
            } catch (org.cloudnook.knightagent.core.middleware.MiddlewareException e) {
                log.error("中间件 afterToolCall 失败: {}", e.getMessage(), e);
            }

            // 恢复运行状态
            state.agentContext.setStatus(AgentStatus.running(state.config.getThreadId()));
        }
        return null; // 不需要审批
    }

    /**
     * 将工具执行异常转换为 ToolMessage
     */
    private ToolMessage resultToMessage(ToolCall toolCall, ToolExecutionException e) {
        return ToolMessage.builder()
                .toolCallId(toolCall.getId())
                .result("执行失败: " + e.getMessage())
                .error(true)
                .errorMessage(e.getMessage())
                .build();
    }

    /**
     * 完成响应构建
     */
    private AgentResponse finalizeResponse(AIMessage finalMessage, ExecutionState state,
                                          long startTime, Instant startInstant) {
        try {
            // 应用状态归约器
            if (state.config.getStateReducer() != null) {
                state.state = state.config.getStateReducer().reduce(state.state, state.state);
                state.agentContext.setState(state.state);
            }

            // 通过中间件处理状态更新
            state.state = state.middlewareChain.onStateUpdate(state.state, state.agentContext);
            state.agentContext.setState(state.state);

            // 保存检查点
            String checkpointId = null;
            if (state.config.isCheckpointEnabled() && state.config.getThreadId() != null) {
                checkpointId = state.checkpointer.save(state.config.getThreadId(), state.state);
            }

            // 构建响应
            AgentResponse response = AgentResponse.builder()
                    .output(finalMessage != null ? finalMessage.getContent() : "")
                    .messages(state.state.getMessages())
                    .toolCalls(finalMessage != null ? finalMessage.getToolCalls() : List.of())
                    .state(state.state)
                    .threadId(state.config.getThreadId())
                    .checkpointId(checkpointId)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .startTime(startInstant)
                    .endTime(Instant.now())
                    .build();

            // 设置完成状态
            state.agentContext.setStatus(AgentStatus.idle());

            // 通过中间件处理响应
            state.middlewareChain.afterInvoke(response, state.agentContext);

            return response;
        } catch (org.cloudnook.knightagent.core.middleware.MiddlewareException e) {
            log.error("中间件处理失败: {}", e.getMessage(), e);
            // 即使中间件失败，仍然返回响应
            return AgentResponse.builder()
                    .output(finalMessage != null ? finalMessage.getContent() : "")
                    .messages(state.state.getMessages())
                    .toolCalls(finalMessage != null ? finalMessage.getToolCalls() : List.of())
                    .state(state.state)
                    .threadId(state.config.getThreadId())
                    .checkpointId(null)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .startTime(startInstant)
                    .endTime(Instant.now())
                    .build();
        } catch (CheckpointException e) {
            // 包装为运行时异常，由上层处理
            throw new RuntimeException("检查点操作失败", e);
        }
    }

    /**
     * 构建等待审批的响应
     */
    private AgentResponse buildApprovalResponse(ExecutionState state, long startTime, Instant startInstant) {
        ApprovalRequest approval = state.agentContext.getPendingApproval();

        AgentResponse response = AgentResponse.builder()
                .output("等待人工审批: " + approval.getToolName())
                .messages(state.state.getMessages())
                .toolCalls(List.of(approval.getToolCall()))
                .state(state.state)
                .threadId(state.config.getThreadId())
                .checkpointId(null)
                .approvalRequest(approval)
                .durationMs(System.currentTimeMillis() - startTime)
                .startTime(startInstant)
                .endTime(Instant.now())
                .build();

        try {
            state.middlewareChain.afterInvoke(response, state.agentContext);
        } catch (Exception e) {
            log.warn("中间件 afterInvoke 处理失败: {}", e.getMessage());
        }

        return response;
    }

    /**
     * 加载或创建状态
     */
    private AgentState loadOrCreateState(AgentConfig config, Checkpointer checkpointer) throws CheckpointException {
        if (config.getThreadId() != null && checkpointer != null) {
            return checkpointer.loadLatest(config.getThreadId()).orElse(AgentState.initial());
        }
        return AgentState.initial();
    }

    /**
     * 构建消息列表
     */
    private List<Message> buildMessages(ExecutionState state) {
        List<Message> messages = new ArrayList<>();

        // 优先使用 agentContext 中的 request（可能被中间件修改）
        String systemPrompt = state.agentContext.getRequest().getSystemPrompt() != null
                ? state.agentContext.getRequest().getSystemPrompt()
                : state.config.getSystemPrompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(SystemMessage.of(systemPrompt));
        }

        messages.addAll(state.state.getMessages());
        return messages;
    }

    /**
     * 获取最大迭代次数
     */
    private int getMaxIterations(AgentRequest request, AgentConfig config) {
        if (request.getMaxIterations() != null) {
            return request.getMaxIterations();
        }
        return config.getMaxIterations();
    }

    /**
     * 获取调用选项
     */
    private ChatOptions getChatOptions(ExecutionState state) {
        ChatOptions options = state.config.getChatOptions();
        if (options == null) {
            options = ChatOptions.defaults();
        }
        return options;
    }

    /**
     * 执行状态（内部类，用于减少参数传递）
     */
    private static class ExecutionState {
        final ChatModel model;
        final ToolInvoker toolInvoker;
        final Checkpointer checkpointer;
        final AgentConfig config;
        final MiddlewareChain middlewareChain;
        final AgentContext agentContext;
        AgentState state;
        final int maxIterations;
        final AgentRequest request;
        AIMessage lastMessage;
        StreamCallback callback; // 仅流式执行时使用

        ExecutionState(ChatModel model, ToolInvoker toolInvoker, Checkpointer checkpointer,
                        AgentConfig config, MiddlewareChain middlewareChain,
                        AgentContext agentContext, AgentState state, int maxIterations,
                        AgentRequest request) {
            this.model = model;
            this.toolInvoker = toolInvoker;
            this.checkpointer = checkpointer;
            this.config = config;
            this.middlewareChain = middlewareChain;
            this.agentContext = agentContext;
            this.state = state;
            this.maxIterations = maxIterations;
            this.request = request;
        }
    }
}
