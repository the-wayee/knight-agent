package org.cloudnook.knightagent.core.agent;

import org.cloudnook.knightagent.core.agent.strategy.ExecutionContext;
import org.cloudnook.knightagent.core.agent.strategy.ExecutionStrategy;
import org.cloudnook.knightagent.core.interception.InterruptCommand;
import org.cloudnook.knightagent.core.streaming.StreamCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 执行器
 * <p>
 * 负责 Agent 的核心执行逻辑，委托给执行策略处理所有细节。
 * 实现 {@link Agent} 接口，作为框架的主要入口点。
 * <p>
 * 执行流程：
 * <pre>
 * 1. 策略执行请求
 * 2. 如遇中断，返回中断响应
 * 3. 使用 InterruptCommand 恢复执行
 * </pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class AgentExecutor implements Agent {

    private final ExecutionStrategy strategy;
    private final ExecutionContext executionContext;

    /**
     * 构造函数
     *
     * @param strategy        执行策略
     * @param executionContext 执行上下文
     */
    public AgentExecutor(ExecutionStrategy strategy, ExecutionContext executionContext) {
        this.strategy = strategy;
        this.executionContext = executionContext;
    }

    // ==================== Agent 接口实现 ====================

    /**
     * 同步执行 Agent
     * <p>
     * 委托给执行策略，中断处理由策略内部的 ToolCallExecutor 完成。
     *
     * @param request Agent 请求
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    @Override
    public AgentResponse invoke(AgentRequest request) throws AgentExecutionException {
        return strategy.execute(request, executionContext);
    }

    /**
     * 流式执行 Agent
     *
     * @param request  Agent 请求
     * @param callback 流式回调
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    @Override
    public AgentResponse stream(AgentRequest request, StreamCallback callback) throws AgentExecutionException {
        return strategy.executeStream(request, callback, executionContext);
    }

    /**
     * 批量执行 Agent
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
     * 获取 Agent 配置
     *
     * @return Agent 配置
     */
    @Override
    public AgentConfig getConfig() {
        return executionContext.getConfig();
    }

    /**
     * 从中断恢复执行
     * <p>
     * 使用中断命令恢复执行，策略会根据命令类型继续处理。
     *
     * @param command 中断恢复命令
     * @return Agent 响应
     * @throws AgentExecutionException 执行失败
     */
    @Override
    public AgentResponse resume(InterruptCommand command) throws AgentExecutionException {
        // 策略负责处理恢复逻辑
        // 1. 从 checkpoint 恢复状态
        // 2. 根据命令类型继续执行
        // 3. 返回结果
        // 具体实现委托给策略
        throw new AgentExecutionException("恢复执行功能由策略实现，请使用具体的策略支持的中断恢复方法");
    }
}
