package org.cloudnook.knightagent.core.interception;

/**
 * 中断恢复命令
 * <p>
 * 用于从中断状态恢复执行时传递给 Agent.resume()。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 被中断
 * AgentResponse response = agent.invoke(request);
 * Interrupt interrupt = response.getInterrupt();
 *
 * // 用户做出决策
 * ApprovalRequest approval = interrupt.toApprovalRequest();
 * approval.allow();
 *
 * // 恢复执行
 * InterruptCommand command = interrupt.toCommand(approval);
 * AgentResponse resumed = agent.resume(command);
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public sealed interface InterruptCommand permits
        ApprovalInterruptCommand,
        RateLimitWaitCommand {

    /**
     * 获取中断 ID
     * <p>
     * 用于匹配原始中断。
     *
     * @return 中断 ID
     */
    String interruptId();

    /**
     * 获取线程 ID
     *
     * @return 线程 ID
     */
    String threadId();

    /**
     * 获取检查点 ID
     * <p>
     * 用于恢复执行状态。
     *
     * @return 检查点 ID
     */
    String checkpointId();
}
