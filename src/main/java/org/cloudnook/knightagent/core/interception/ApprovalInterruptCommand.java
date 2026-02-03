package org.cloudnook.knightagent.core.interception;

import org.cloudnook.knightagent.core.agent.ApprovalRequest;

/**
 * 审批恢复命令
 * <p>
 * 用于从工具审批中断恢复执行。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public record ApprovalInterruptCommand(
    String interruptId,
    String threadId,
    String checkpointId,
    ApprovalRequest approval
) implements InterruptCommand {}
