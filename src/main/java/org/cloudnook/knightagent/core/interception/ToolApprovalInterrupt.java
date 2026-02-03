package org.cloudnook.knightagent.core.interception;

import org.cloudnook.knightagent.core.agent.ApprovalRequest;
import org.cloudnook.knightagent.core.message.ToolCall;

import java.time.Instant;

/**
 * 工具审批中断
 * <p>
 * 当需要人工审批工具调用时触发。
 * 中断后可以：
 * <ul>
 *   <li>允许 - 使用原始参数执行工具</li>
 *   <li>拒绝 - 将拒绝原因返回给 LLM</li>
 *   <li>编辑 - 使用修改后的参数执行工具</li>
 * </ul>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public record ToolApprovalInterrupt(
    String interruptId,
    String threadId,
    Instant timestamp,
    String description,
    ToolCall toolCall
) implements Interrupt {

    /**
     * 从 ToolCall 创建工具审批中断
     *
     * @param toolCall 工具调用
     * @param threadId 线程 ID
     * @return 工具审批中断
     */
    public static ToolApprovalInterrupt fromToolCall(ToolCall toolCall, String threadId) {
        return new ToolApprovalInterrupt(
            "interrupt_" + System.currentTimeMillis(),
            threadId,
            Instant.now(),
            "等待工具审批: " + toolCall.getName(),
            toolCall
        );
    }

    /**
     * 转换为审批请求
     * <p>
     * 用于展示审批界面和收集用户决策。
     *
     * @return 审批请求
     */
    public ApprovalRequest toApprovalRequest() {
        return ApprovalRequest.fromToolCall(toolCall, threadId, null);
    }

    @Override
    public InterruptCommand toCommand(Object resumeValue) {
        return new ApprovalInterruptCommand(
            interruptId,
            threadId,
            null,  // checkpointId 由外部设置
            (ApprovalRequest) resumeValue
        );
    }
}
