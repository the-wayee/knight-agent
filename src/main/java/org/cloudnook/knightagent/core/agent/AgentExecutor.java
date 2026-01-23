package org.cloudnook.knightagent.core.agent;

import org.cloudnook.knightagent.core.checkpoint.Checkpointer;
import org.cloudnook.knightagent.core.message.*;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.ChatOptions;
import org.cloudnook.knightagent.core.model.ModelException;
import org.cloudnook.knightagent.core.state.AgentState;
import org.cloudnook.knightagent.core.streaming.StreamCallback;
import org.cloudnook.knightagent.core.streaming.StreamCallbackAdapter;
import org.cloudnook.knightagent.core.tool.Tool;
import org.cloudnook.knightagent.core.tool.ToolInvoker;
import org.cloudnook.knightagent.core.tool.ToolExecutionException;
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.middleware.AgentContext;
import org.cloudnook.knightagent.core.middleware.MiddlewareChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Agent 执行器
 * <p>
 * 负责 Agent 的核心执行逻辑。
 * 实现工具调用循环、消息构建、状态管理等。
 * <p>
 * 执行流程：
 * <pre>
 * 1. 加载历史状态（如果有 Thread ID）
 * 2. 添加用户消息到状态
 * 3. 循环执行：
 *    a. 通过中间件处理
 *    b. 调用 LLM
 *    c. 如果有工具调用：
 *       - 通过中间件验证
 *       - 执行工具
 *       - 添加结果到消息列表
 *       - 继续循环
 *    d. 否则：结束循环
 * 4. 保存检查点
 * 5. 返回响应
 * </pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class AgentExecutor {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutor.class);

    private final ChatModel model;
    private final List<Tool> tools;
    private final ToolInvoker toolInvoker;
    private final Checkpointer checkpointer;
    private final AgentConfig config;
    private final MiddlewareChain middlewareChain;

    /**
     * 构造函数
     *
     * @param model          LLM 模型
     * @param tools          工具列表
     * @param checkpointer   检查点存储
     * @param config         Agent 配置
     * @param middlewares    中间件列表
     */
    public AgentExecutor(ChatModel model, List<Tool> tools, Checkpointer checkpointer,
                          AgentConfig config, List<Middleware> middlewares) {
        this.model = model;
        this.tools = tools != null ? tools : List.of();
        this.toolInvoker = new ToolInvoker(this.tools);
        this.checkpointer = checkpointer;
        this.config = config != null ? config : AgentConfig.defaults();
        this.middlewareChain = new MiddlewareChain(
                middlewares != null ? middlewares : List.of()
        );
    }

    /**
     * 执行 Agent（同步）
     *
     * @param request Agent 请求
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    public AgentResponse execute(AgentRequest request) throws AgentExecutionException {
        long startTime = System.currentTimeMillis();
        Instant startInstant = Instant.now();

        try {
            // 1. 创建上下文
            AgentContext context =
                    new AgentContext(request);

            // 2. 通过中间件处理请求
            middlewareChain.beforeInvoke(request, context);

            // 3. 加载或创建状态
            AgentState state = loadState(request.getThreadId());

            // 4. 添加用户消息
            HumanMessage userMessage = HumanMessage.of(request.getInput(), request.getUserId());
            state = state.addMessage(userMessage);

            // 5. 执行主循环
            int maxIterations = getMaxIterations(request);
            AIMessage finalMessage = null;

            for (int i = 0; i < maxIterations; i++) {
                context.setIteration(i + 1);

                log.debug("Agent 迭代 {}/{}", i + 1, maxIterations);

                // 构建消息列表
                List<Message> messages = buildMessages(state, request);

                // 调用 LLM
                ChatOptions options = getChatOptions(request);
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
                    boolean allowed = middlewareChain.beforeToolCall(toolCall, context);
                    if (!allowed || context.isStopped()) {
                        continue;
                    }

                    // 执行工具
                    ToolResult result = toolInvoker.invoke(toolCall);

                    // 通过中间件处理结果
                    middlewareChain.afterToolCall(toolCall, result, context);

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
            state = middlewareChain.onStateUpdate(state, state, context);

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
            middlewareChain.afterInvoke(response, context);

            return response;

        } catch (ModelException e) {
            throw new AgentExecutionException("模型调用失败: " + e.getMessage(), e, "MODEL_ERROR");
        } catch (ToolExecutionException e) {
            throw new AgentExecutionException("工具执行失败: " + e.getMessage(), e, "TOOL_ERROR");
        } catch (org.cloudnook.knightagent.core.checkpoint.CheckpointException e) {
            throw new AgentExecutionException("检查点操作失败: " + e.getMessage(), e, "CHECKPOINT_ERROR");
        } catch (Exception e) {
            throw new AgentExecutionException("Agent 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式执行 Agent
     *
     * @param request  Agent 请求
     * @param callback 流式回调
     * @throws AgentExecutionException 执行失败
     */
    public void executeStream(AgentRequest request, StreamCallback callback) throws AgentExecutionException {
        long startTime = System.currentTimeMillis();
        Instant startInstant = Instant.now();

        try {
            callback.onStart();

            // 1. 创建上下文
            AgentContext context =
                    new AgentContext(request);

            // 2. 通过中间件处理请求
            middlewareChain.beforeInvoke(request, context);

            // 3. 加载或创建状态
            AgentState state = loadState(request.getThreadId());

            // 4. 添加用户消息
            HumanMessage userMessage = HumanMessage.of(request.getInput(), request.getUserId());
            state = state.addMessage(userMessage);

            // 5. 执行主循环
            int maxIterations = getMaxIterations(request);
            StringBuilder fullContent = new StringBuilder();

            for (int i = 0; i < maxIterations; i++) {
                context.setIteration(i + 1);

                // 构建消息列表
                List<Message> messages = buildMessages(state, request);

                // 流式调用 LLM
                ChatOptions options = getChatOptions(request);

                // 创建收集响应的回调
                StringBuilder contentBuffer = new StringBuilder();
                List<ToolCall> collectedToolCalls = new ArrayList<>();

                model.chatStream(messages, options, new StreamCallbackAdapter() {
                    @Override
                    public void onToken(String token) {
                        contentBuffer.append(token);
                        fullContent.append(token);
                        callback.onToken(token);
                    }

                    @Override
                    public void onToolCall(ToolCall toolCall) {
                        collectedToolCalls.add(toolCall);
                        callback.onToolCall(toolCall);
                    }

                    @Override
                    public void onComplete() {
                        callback.onComplete();
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
                    boolean allowed = middlewareChain.beforeToolCall(toolCall, context);
                    if (!allowed || context.isStopped()) {
                        continue;
                    }

                    // 执行工具
                    ToolResult result = toolInvoker.invoke(toolCall);

                    // 通过中间件处理结果
                    middlewareChain.afterToolCall(toolCall, result, context);

                    // 添加工具消息到状态
                    ToolMessage toolMessage = result.toMessage();
                    state = state.addMessage(toolMessage);
                }
            }

        } catch (ModelException e) {
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
    private AgentState loadState(String threadId) throws org.cloudnook.knightagent.core.checkpoint.CheckpointException {
        if (threadId == null || checkpointer == null) {
            return AgentState.initial();
        }
        return checkpointer.loadLatest(threadId).orElse(AgentState.initial());
    }

    /**
     * 构建消息列表
     */
    private List<Message> buildMessages(AgentState state, AgentRequest request) {
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
    private int getMaxIterations(AgentRequest request) {
        if (request.getMaxIterations() != null) {
            return request.getMaxIterations();
        }
        return config.getMaxIterations();
    }

    /**
     * 获取调用选项
     */
    private ChatOptions getChatOptions(AgentRequest request) {
        ChatOptions options = config.getChatOptions();
        if (options == null) {
            options = ChatOptions.defaults();
        }
        return options;
    }
}
