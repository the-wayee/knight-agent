package org.cloudnook.knightagent.core.agent;

import org.cloudnook.knightagent.core.agent.strategy.ExecutionContext;
import org.cloudnook.knightagent.core.agent.strategy.ExecutionStrategy;
import org.cloudnook.knightagent.core.agent.strategy.ReActStrategy;
import org.cloudnook.knightagent.core.checkpoint.Checkpointer;
import org.cloudnook.knightagent.core.message.*;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.state.AgentState;
import org.cloudnook.knightagent.core.streaming.StreamCallback;
import org.cloudnook.knightagent.core.tool.McpTool;
import org.cloudnook.knightagent.core.tool.ToolInvoker;
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.middleware.MiddlewareChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 执行器
 * <p>
 * 负责 Agent 的核心执行逻辑。
 * 实现工具调用循环、消息构建、状态管理等。
 * 实现 {@link Agent} 接口，作为框架的主要入口点。
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
     * 使用配置的执行策略执行请求。
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
        if (strategy instanceof ReActStrategy reactStrategy) {
            return reactStrategy.executeStream(request, callback, executionContext);
        } else {
            throw new AgentExecutionException("流式执行暂不支持该策略: " + strategy.getName());
        }
    }

    // ==================== Agent 接口实现 ====================

    /**
     * 同步执行 Agent（Agent 接口实现）
     *
     * @param request Agent 请求
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    @Override
    public AgentResponse invoke(AgentRequest request) throws AgentExecutionException {
        return execute(request);
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
            responses.add(execute(request));
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
     *
     * @param checkpointId checkpoint ID
     * @param approval     审批请求（必须包含决策）
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    @Override
    public AgentResponse resume(String checkpointId, ApprovalRequest approval) throws AgentExecutionException {
        if (strategy instanceof ReActStrategy reactStrategy) {
            return reactStrategy.resumeFromApproval(checkpointId, approval, executionContext);
        } else {
            throw new AgentExecutionException("该策略不支持从审批恢复执行: " + strategy.getName());
        }
    }
}
