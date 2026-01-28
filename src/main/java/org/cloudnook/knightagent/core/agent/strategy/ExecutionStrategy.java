package org.cloudnook.knightagent.core.agent.strategy;

import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.ApprovalRequest;

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
     * 从审批恢复执行
     * <p>
     * 当 Agent 执行被中断等待人工审批后，
     * 使用此方法从 checkpoint 恢复执行。
     * <p>
     * 默认实现不支持恢复，子类可以重写此方法提供支持。
     *
     * @param checkpointId checkpoint ID
     * @param approval     审批请求（包含决策）
     * @param context      执行上下文
     * @return Agent 响应
     * @throws org.cloudnook.knightagent.core.agent.AgentExecutionException 执行失败
     */
    default AgentResponse resumeFromApproval(
            String checkpointId,
            ApprovalRequest approval,
            ExecutionContext context) throws org.cloudnook.knightagent.core.agent.AgentExecutionException {
        throw new org.cloudnook.knightagent.core.agent.AgentExecutionException(
                "该策略不支持从审批恢复执行");
    }

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
