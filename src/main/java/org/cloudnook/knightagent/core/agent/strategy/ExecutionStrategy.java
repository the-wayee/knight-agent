package org.cloudnook.knightagent.core.agent.strategy;

import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;

/**
 * Agent 执行策略接口
 * <p>
 * 定义 Agent 的执行模式，允许不同的执行策略：
 * <ul>
 *   <li>ReAct - 推理-行动-观察循环</li>
 *   <li>Plan-and-Execute - 先规划后执行</li>
 *   <li>ReWOO - 无推理观察的推理</li>
 * </ul>
 * <p>
 * 策略模式允许框架支持多种 Agent 类型，而无需修改核心执行逻辑。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public interface ExecutionStrategy {

    /**
     * 执行 Agent 请求
     *
     * @param request  Agent 请求
     * @param context 执行上下文
     * @return Agent 响应
     * @throws org.cloudnook.knightagent.core.agent.AgentExecutionException 执行失败
     */
    AgentResponse execute(AgentRequest request, ExecutionContext context)
            throws org.cloudnook.knightagent.core.agent.AgentExecutionException;

    /**
     * 获取策略名称
     * <p>
     * 用于日志记录和调试。
     *
     * @return 策略名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
