package org.cloudnook.knightagent.core.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cloudnook.knightagent.core.message.ToolCall;

import java.time.Instant;

/**
 * 人工审批请求
 * <p>
 * 当 Agent 执行到需要人工审批的工具调用时，
 * 会创建此对象并返回给调用者，等待外部审批决策。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    /**
     * 审批请求 ID
     */
    private String approvalId;

    /**
     * Thread ID
     */
    private String threadId;

    /**
     * Checkpoint ID
     * <p>
     * 保存的执行状态，用于审批后恢复执行。
     */
    private String checkpointId;

    /**
     * 等待审批的工具调用
     */
    private ToolCall toolCall;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 原始工具参数（JSON 格式）
     */
    private String originalArguments;

    /**
     * 修改后的工具参数（EDIT 模式下使用）
     */
    private String modifiedArguments;

    /**
     * 拒绝原因（REJECT 模式下使用）
     * <p>
     * 会作为 ToolMessage 返回给 LLM，帮助 LLM 理解为什么被拒绝
     * 并调整后续策略。
     */
    private String rejectReason;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 审批决策
     */
    private ApprovalDecision decision;

    /**
     * 审批决策
     */
    public enum ApprovalDecision {
        /**
         * 允许执行 - 按原始参数执行工具
         */
        ALLOW,

        /**
         * 拒绝执行 - 不执行工具，将拒绝原因反馈给 LLM
         * <p>
         * LLM 收到拒绝原因后可以：
         * <ul>
         *   <li>调整策略，尝试其他方法</li>
         *   <li>向用户解释，请求更多信息</li>
         *   <li>修改请求参数后重新尝试</li>
         * </ul>
         */
        REJECT,

        /**
         * 修改后执行 - 使用修改后的参数执行工具
         * <p>
         * 用户可以修改工具参数，然后执行。
         * 例如：将文件路径从 `/home/user/important.txt` 改为 `/home/user/temp.txt`
         */
        EDIT
    }

    /**
     * 是否已处理
     */
    public boolean isProcessed() {
        return decision != null;
    }

    /**
     * 获取最终执行的工具参数
     * <p>
     * - ALLOW: 返回原始参数
     * - EDIT: 返回修改后的参数
     * - REJECT: 返回 null
     *
     * @return 最终参数，或 null（如果拒绝）
     */
    public String getFinalArguments() {
        return switch (decision) {
            case ALLOW -> originalArguments;
            case EDIT -> modifiedArguments;
            case REJECT -> null;
        };
    }

    /**
     * 创建 ALLOW 决策
     */
    public ApprovalRequest allow() {
        this.decision = ApprovalDecision.ALLOW;
        return this;
    }

    /**
     * 创建 REJECT 决策
     *
     * @param reason 拒绝原因（会反馈给 LLM）
     */
    public ApprovalRequest reject(String reason) {
        this.decision = ApprovalDecision.REJECT;
        this.rejectReason = reason;
        return this;
    }

    /**
     * 创建 EDIT 决策
     *
     * @param modifiedArguments 修改后的工具参数（JSON 格式）
     */
    public ApprovalRequest edit(String modifiedArguments) {
        this.decision = ApprovalDecision.EDIT;
        this.modifiedArguments = modifiedArguments;
        return this;
    }

    /**
     * 从 ToolCall 创建审批请求
     */
    public static ApprovalRequest fromToolCall(ToolCall toolCall, String threadId, String checkpointId) {
        return ApprovalRequest.builder()
                .approvalId("approve_" + System.currentTimeMillis())
                .threadId(threadId)
                .checkpointId(checkpointId)
                .toolCall(toolCall)
                .toolName(toolCall.getName())
                .originalArguments(toolCall.getArguments())
                .createdAt(Instant.now())
                .build();
    }
}
