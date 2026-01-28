package org.cloudnook.knightagent.core.agent.strategy;

import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.agent.AgentExecutionException;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
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
 *   <li>加载历史状态</li>
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

        ChatModel model = context.getModel();
        ToolInvoker toolInvoker = context.getToolInvoker();
        Checkpointer checkpointer = context.getCheckpointer();
        AgentConfig config = context.getConfig();
        MiddlewareChain middlewareChain = context.getMiddlewareChain();

        try {
            // 1. 创建上下文
            AgentContext agentContext = new AgentContext(request);

            // 2. 通过中间件处理请求
            middlewareChain.beforeInvoke(request, agentContext);

            // 3. 加载或创建状态
            AgentState state = loadState(request.getThreadId(), checkpointer);

            // 4. 添加用户消息
            HumanMessage userMessage = HumanMessage.of(request.getInput(), request.getUserId());
            state = state.addMessage(userMessage);

            // 5. 执行主循环
            int maxIterations = getMaxIterations(request, config);
            AIMessage finalMessage = null;

            for (int i = 0; i < maxIterations; i++) {
                agentContext.setIteration(i + 1);

                log.debug("ReAct 迭代 {}/{}", i + 1, maxIterations);

                // 构建消息列表
                List<Message> messages = buildMessages(state, request, config);

                // 调用 LLM
                ChatOptions options = getChatOptions(config);
                // 注入可用工具
                if (toolInvoker.getTools() != null && !toolInvoker.getTools().isEmpty()) {
                    options = options.toBuilder()
                            .tools(toolInvoker.getTools())
                            .build();
                }
                finalMessage = model.chat(messages, options);

                // 添加 AI 消息到状态
                state = state.addMessage(finalMessage);

                // 检查是否需要调用工具
                if (!finalMessage.hasToolCalls()) {
                    log.debug("无工具调用，结束循环");
                    break;
                }

                // 执行工具调用
                for (ToolCall toolCall : finalMessage.getToolCalls()) {
                    // 通过中间件验证
                    boolean allowed = middlewareChain.beforeToolCall(toolCall, agentContext);

                    // 检查是否需要人工审批
                    if (agentContext.hasPendingApproval()) {
                        return handleApprovalRequest(agentContext, state, request, startTime, startInstant, checkpointer, config, middlewareChain);
                    }

                    if (!allowed || agentContext.isStopped()) {
                        continue;
                    }

                    // 执行工具
                    ToolResult result = toolInvoker.invoke(toolCall);

                    // 通过中间件处理结果
                    middlewareChain.afterToolCall(toolCall, result, agentContext);

                    // 添加工具消息到状态
                    ToolMessage toolMessage = result.toMessage();
                    state = state.addMessage(toolMessage);
                }
            }

            // 6. 应用状态归约器
            if (config.getStateReducer() != null) {
                AgentState oldState = state;
                state = config.getStateReducer().reduce(oldState, state);
            }

            // 7. 通过中间件处理状态更新
            state = middlewareChain.onStateUpdate(state, state, agentContext);

            // 8. 保存检查点
            String checkpointId = null;
            if (config.isCheckpointEnabled() && request.getThreadId() != null) {
                checkpointId = checkpointer.save(request.getThreadId(), state);
            }

            // 9. 构建响应
            AgentResponse response = AgentResponse.builder()
                    .output(finalMessage != null ? finalMessage.getContent() : "")
                    .messages(state.getMessages())
                    .toolCalls(finalMessage != null ? finalMessage.getToolCalls() : List.of())
                    .state(state)
                    .threadId(request.getThreadId())
                    .checkpointId(checkpointId)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .startTime(startInstant)
                    .endTime(Instant.now())
                    .build();

            // 10. 通过中间件处理响应
            middlewareChain.afterInvoke(response, agentContext);

            return response;

        } catch (org.cloudnook.knightagent.core.model.ModelException e) {
            throw new AgentExecutionException("模型调用失败: " + e.getMessage(), e, "MODEL_ERROR");
        } catch (ToolExecutionException e) {
            throw new AgentExecutionException("工具执行失败: " + e.getMessage(), e, "TOOL_ERROR");
        } catch (CheckpointException e) {
            throw new AgentExecutionException("检查点操作失败: " + e.getMessage(), e, "CHECKPOINT_ERROR");
        } catch (Exception e) {
            throw new AgentExecutionException("Agent 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式执行（可选实现）
     */
    public AgentResponse executeStream(AgentRequest request, StreamCallback callback, ExecutionContext context)
            throws AgentExecutionException {
        long startTime = System.currentTimeMillis();
        Instant startInstant = Instant.now();

        ChatModel model = context.getModel();
        ToolInvoker toolInvoker = context.getToolInvoker();
        AgentConfig config = context.getConfig();
        var middlewareChain = context.getMiddlewareChain();

        try {
            callback.onStart();

            // 1. 创建上下文
            AgentContext agentContext = new AgentContext(request);

            // 2. 通过中间件处理请求
            middlewareChain.beforeInvoke(request, agentContext);

            // 3. 加载或创建状态
            AgentState state = loadState(request.getThreadId(), context.getCheckpointer());

            // 4. 添加用户消息
            HumanMessage userMessage = HumanMessage.of(request.getInput(), request.getUserId());
            state = state.addMessage(userMessage);

            // 5. 执行主循环
            int maxIterations = getMaxIterations(request, config);
            StringBuilder fullContent = new StringBuilder();

            for (int i = 0; i < maxIterations; i++) {
                agentContext.setIteration(i + 1);

                // 构建消息列表
                List<Message> messages = buildMessages(state, request, config);

                // 流式调用 LLM
                ChatOptions options = getChatOptions(config);
                // 注入可用工具
                if (toolInvoker.getTools() != null && !toolInvoker.getTools().isEmpty()) {
                    options = options.toBuilder()
                            .tools(toolInvoker.getTools())
                            .build();
                }

                // 创建收集响应的回调
                StringBuilder contentBuffer = new StringBuilder();
                List<ToolCall> collectedToolCalls = new ArrayList<>();

                model.chatStream(messages, options, new StreamCallbackAdapter() {
                    @Override
                    public void onToken(StreamChunk chunk) {
                        String token = chunk.getContent();
                        if (token != null) {
                            contentBuffer.append(token);
                            fullContent.append(token);
                        }
                        callback.onToken(chunk);
                    }

                    @Override
                    public void onToolCall(StreamChunk chunk, ToolCall toolCall) {
                        collectedToolCalls.add(toolCall);
                        callback.onToolCall(chunk, toolCall);
                    }

                    @Override
                    public void onCompletion(StreamCompleteResponse response) {
                         // 框架已自动累积完整内容，我们主要关注上面的 token/toolCall Accumulation
                         // 完整响应在 response.getFullContent() 和 response.getToolCalls()
                    }
                });

                // 创建 AI 消息
                AIMessage aiMessage = AIMessage.of(contentBuffer.toString(), collectedToolCalls);
                state = state.addMessage(aiMessage);

                // 检查是否需要调用工具
                if (!aiMessage.hasToolCalls()) {
                    break;
                }

                // 执行工具调用
                for (ToolCall toolCall : aiMessage.getToolCalls()) {
                    // 通过中间件验证
                    boolean allowed = middlewareChain.beforeToolCall(toolCall, agentContext);
                    if (!allowed || agentContext.isStopped()) {
                        continue;
                    }

                    // 执行工具
                    ToolResult result = toolInvoker.invoke(toolCall);

                    // 通过中间件处理结果
                    middlewareChain.afterToolCall(toolCall, result, agentContext);

                    // 添加工具消息到状态
                    ToolMessage toolMessage = result.toMessage();
                    state = state.addMessage(toolMessage);
                }
            }

            // 构建最终响应
            AgentResponse response = AgentResponse.builder()
                    .output(fullContent.toString())
                    .messages(state.getMessages())
                    .state(state)
                    .threadId(request.getThreadId())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .startTime(startInstant)
                    .endTime(Instant.now())
                    .build();

            // 完成回调（传入完整的 StreamCompleteResponse）
            callback.onCompletion(org.cloudnook.knightagent.core.streaming.StreamCompleteResponse.builder()
                    .fullContent(fullContent.toString())
                    .toolCalls(java.util.Collections.emptyList())
                    .finishReason("stop")
                    .model(model.getModelId())
                    .build());

            return response;

        } catch (org.cloudnook.knightagent.core.model.ModelException e) {
            callback.onError(e);
            throw AgentExecutionException.modelError("模型调用失败: " + e.getMessage(), e);
        } catch (Exception e) {
            callback.onError(e);
            throw new AgentExecutionException("Agent 流式执行失败: " + e.getMessage(), e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 加载状态
     */
    private AgentState loadState(String threadId, Checkpointer checkpointer) throws CheckpointException {
        if (threadId == null || checkpointer == null) {
            return AgentState.initial();
        }
        return checkpointer.loadLatest(threadId).orElse(AgentState.initial());
    }

    /**
     * 构建消息列表
     */
    private List<Message> buildMessages(AgentState state, AgentRequest request, AgentConfig config) {
        List<Message> messages = new ArrayList<>();

        // 添加系统提示词
        String systemPrompt = request.getSystemPrompt() != null
                ? request.getSystemPrompt()
                : config.getSystemPrompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(SystemMessage.of(systemPrompt));
        }

        // 添加历史消息
        messages.addAll(state.getMessages());

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
    private ChatOptions getChatOptions(AgentConfig config) {
        ChatOptions options = config.getChatOptions();
        if (options == null) {
            options = ChatOptions.defaults();
        }
        return options;
    }

    /**
     * 处理审批请求
     * <p>
     * 保存当前状态到 checkpoint，返回"等待审批"响应。
     */
    private AgentResponse handleApprovalRequest(
            AgentContext agentContext,
            AgentState state,
            AgentRequest request,
            long startTime,
            Instant startInstant,
            Checkpointer checkpointer,
            AgentConfig config,
            MiddlewareChain middlewareChain) throws CheckpointException, org.cloudnook.knightagent.core.middleware.MiddlewareException {

        org.cloudnook.knightagent.core.agent.ApprovalRequest approval = agentContext.getPendingApproval();

        log.info("Agent 执行暂停，等待人工审批: 审批ID={}, 工具={}",
                approval.getApprovalId(), approval.getToolName());

        // 保存状态到 checkpoint
        String checkpointId = null;
        if (checkpointer != null && request.getThreadId() != null) {
            checkpointId = checkpointer.save(request.getThreadId(), state);
            approval.setCheckpointId(checkpointId);
        }

        // 构建等待审批的响应
        AgentResponse response = AgentResponse.builder()
                .output("等待人工审批: " + approval.getToolName())
                .messages(state.getMessages())
                .toolCalls(List.of(approval.getToolCall()))
                .state(state)
                .threadId(request.getThreadId())
                .checkpointId(checkpointId)
                .approvalRequest(approval)
                .durationMs(System.currentTimeMillis() - startTime)
                .startTime(startInstant)
                .endTime(Instant.now())
                .build();

        // 通过中间件处理响应
        middlewareChain.afterInvoke(response, agentContext);

        return response;
    }

    /**
     * 从审批恢复执行
     * <p>
     * 根据审批决策继续执行 Agent。
     *
     * @param checkpointId    checkpoint ID
     * @param approval        审批请求（包含决策）
     * @param context         执行上下文
     * @return Agent 响应
     */
    public AgentResponse resumeFromApproval(
            String checkpointId,
            org.cloudnook.knightagent.core.agent.ApprovalRequest approval,
            ExecutionContext context) throws AgentExecutionException {

        long startTime = System.currentTimeMillis();
        Instant startInstant = Instant.now();

        Checkpointer checkpointer = context.getCheckpointer();
        ToolInvoker toolInvoker = context.getToolInvoker();
        AgentConfig config = context.getConfig();
        MiddlewareChain middlewareChain = context.getMiddlewareChain();
        ChatModel model = context.getModel();

        try {
            // 1. 从 checkpoint 恢复状态
            if (checkpointer == null) {
                throw new AgentExecutionException("Checkpointer 未配置，无法恢复执行");
            }

            AgentState state = checkpointer.load(approval.getThreadId(), checkpointId)
                    .orElseThrow(() -> new AgentExecutionException("Checkpoint 不存在: " + checkpointId));

            log.info("从 checkpoint 恢复执行: checkpointId={}, 决策={}", checkpointId, approval.getDecision());

            // 2. 创建上下文（模拟恢复的执行环境）
            AgentRequest originalRequest = AgentRequest.builder()
                    .threadId(approval.getThreadId())
                    .build();

            AgentContext agentContext = new AgentContext(originalRequest);
            agentContext.setState(state);

            // 3. 根据审批决策处理
            if (approval.getDecision() == org.cloudnook.knightagent.core.agent.ApprovalRequest.ApprovalDecision.REJECT) {
                // 拒绝：将拒绝原因作为 ToolMessage 返回给 LLM
                String rejectMessage = approval.getRejectReason() != null
                        ? approval.getRejectReason()
                        : "用户拒绝了该操作";

                ToolMessage toolMessage = ToolMessage.builder()
                        .toolCallId(approval.getToolCall().getId())
                        .result("[用户拒绝] " + rejectMessage)
                        .error(true)
                        .errorMessage(rejectMessage)
                        .build();

                state = state.addMessage(toolMessage);

                // 继续执行（让 LLM 根据拒绝原因调整策略）
                return continueExecution(state, originalRequest, agentContext, context, startTime, startInstant);

            } else if (approval.getDecision() == org.cloudnook.knightagent.core.agent.ApprovalRequest.ApprovalDecision.ALLOW) {
                // 允许：使用原始参数执行工具
                ToolResult result = toolInvoker.invoke(approval.getToolCall());

                middlewareChain.afterToolCall(approval.getToolCall(), result, agentContext);

                ToolMessage toolMessage = result.toMessage();
                state = state.addMessage(toolMessage);

                // 继续执行
                return continueExecution(state, originalRequest, agentContext, context, startTime, startInstant);

            } else if (approval.getDecision() == org.cloudnook.knightagent.core.agent.ApprovalRequest.ApprovalDecision.EDIT) {
                // 编辑：使用修改后的参数执行工具
                ToolCall modifiedToolCall = ToolCall.builder()
                        .id(approval.getToolCall().getId())
                        .name(approval.getToolName())
                        .arguments(approval.getModifiedArguments())
                        .build();

                ToolResult result = toolInvoker.invoke(modifiedToolCall);

                middlewareChain.afterToolCall(modifiedToolCall, result, agentContext);

                ToolMessage toolMessage = result.toMessage();
                state = state.addMessage(toolMessage);

                // 继续执行
                return continueExecution(state, originalRequest, agentContext, context, startTime, startInstant);
            }

            throw new AgentExecutionException("未知的审批决策: " + approval.getDecision());

        } catch (ToolExecutionException e) {
            throw new AgentExecutionException("工具执行失败: " + e.getMessage(), e, "TOOL_ERROR");
        } catch (CheckpointException e) {
            throw new AgentExecutionException("Checkpoint 操作失败: " + e.getMessage(), e, "CHECKPOINT_ERROR");
        } catch (Exception e) {
            throw new AgentExecutionException("恢复执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 继续执行（审批后的后续流程）
     */
    private AgentResponse continueExecution(
            AgentState state,
            AgentRequest request,
            AgentContext agentContext,
            ExecutionContext context,
            long startTime,
            Instant startInstant) throws AgentExecutionException {

        ChatModel model = context.getModel();
        ToolInvoker toolInvoker = context.getToolInvoker();
        AgentConfig config = context.getConfig();
        MiddlewareChain middlewareChain = context.getMiddlewareChain();
        Checkpointer checkpointer = context.getCheckpointer();

        try {
            // 继续执行主循环
            int maxIterations = getMaxIterations(request, config);
            AIMessage finalMessage = null;

            for (int i = agentContext.getIteration(); i < maxIterations; i++) {
                agentContext.setIteration(i + 1);

                log.debug("ReAct 迭代 {}/{}", i + 1, maxIterations);

                // 构建消息列表
                List<Message> messages = buildMessages(state, request, config);

                // 调用 LLM
                ChatOptions options = getChatOptions(config);
                if (toolInvoker.getTools() != null && !toolInvoker.getTools().isEmpty()) {
                    options = options.toBuilder()
                            .tools(toolInvoker.getTools())
                            .build();
                }
                finalMessage = model.chat(messages, options);

                // 添加 AI 消息到状态
                state = state.addMessage(finalMessage);

                // 检查是否需要调用工具
                if (!finalMessage.hasToolCalls()) {
                    log.debug("无工具调用，结束循环");
                    break;
                }

                // 执行工具调用
                for (ToolCall toolCall : finalMessage.getToolCalls()) {
                    // 通过中间件验证
                    boolean allowed = middlewareChain.beforeToolCall(toolCall, agentContext);

                    // 检查是否又需要审批（嵌套审批）
                    if (agentContext.hasPendingApproval()) {
                        return handleApprovalRequest(agentContext, state, request, startTime, startInstant, checkpointer, config, middlewareChain);
                    }

                    if (!allowed || agentContext.isStopped()) {
                        continue;
                    }

                    // 执行工具
                    ToolResult result = toolInvoker.invoke(toolCall);

                    // 通过中间件处理结果
                    middlewareChain.afterToolCall(toolCall, result, agentContext);

                    // 添加工具消息到状态
                    ToolMessage toolMessage = result.toMessage();
                    state = state.addMessage(toolMessage);
                }
            }

            // 应用状态归约器
            if (config.getStateReducer() != null) {
                AgentState oldState = state;
                state = config.getStateReducer().reduce(oldState, state);
            }

            // 通过中间件处理状态更新
            state = middlewareChain.onStateUpdate(state, state, agentContext);

            // 保存检查点
            String checkpointId = null;
            if (config.isCheckpointEnabled() && request.getThreadId() != null) {
                checkpointId = checkpointer.save(request.getThreadId(), state);
            }

            // 构建响应
            AgentResponse response = AgentResponse.builder()
                    .output(finalMessage != null ? finalMessage.getContent() : "")
                    .messages(state.getMessages())
                    .toolCalls(finalMessage != null ? finalMessage.getToolCalls() : List.of())
                    .state(state)
                    .threadId(request.getThreadId())
                    .checkpointId(checkpointId)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .startTime(startInstant)
                    .endTime(Instant.now())
                    .build();

            // 通过中间件处理响应
            middlewareChain.afterInvoke(response, agentContext);

            return response;

        } catch (org.cloudnook.knightagent.core.model.ModelException e) {
            throw new AgentExecutionException("模型调用失败: " + e.getMessage(), e, "MODEL_ERROR");
        } catch (ToolExecutionException e) {
            throw new AgentExecutionException("工具执行失败: " + e.getMessage(), e, "TOOL_ERROR");
        } catch (CheckpointException e) {
            throw new AgentExecutionException("检查点操作失败: " + e.getMessage(), e, "CHECKPOINT_ERROR");
        } catch (Exception e) {
            throw new AgentExecutionException("Agent 执行失败: " + e.getMessage(), e);
        }
    }
}
