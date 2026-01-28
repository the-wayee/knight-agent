package org.cloudnook.knightagent.core.middleware.builtin;

import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.ApprovalRequest;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.message.ToolResult;
import org.cloudnook.knightagent.core.middleware.AgentContext;
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.state.AgentState;

import java.util.List;

/**
 * 人机协作中间件
 * <p>
 * 在执行敏感工具调用前暂停，等待人工审核。
 * <p>
 * 工作流程：
 * <pre>
 * 1. Agent 执行中遇到需要审批的工具
 * 2. 中间件创建 ApprovalRequest 并设置到 context.pendingApproval
 * 3. ReActStrategy 检测到 pendingApproval，保存 checkpoint，返回"等待审批"响应
 * 4. 调用者展示审批界面，用户做出决策
 * 5. 调用 agent.resume(checkpointId, approvalDecision) 恢复执行
 * 6. ReActStrategy 从 checkpoint 恢复状态，根据决策继续执行
 * </pre>
 * <p>
 * 审核模式：
 * <ul>
 *   <li>ALWAYS - 所有工具调用都需要审核</li>
 *   <li>WHITELIST - 只有白名单中的工具需要审核</li>
 *   <li>BLACKLIST - 除了黑名单中的工具外都需要审核</li>
 *   <li>NEVER - 不进行审核（直接放行）</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * Agent agent = AgentBuilder.builder()
 *     .model(chatModel)
 *     .middleware(HumanInTheLoopMiddleware.builder()
 *         .mode(ReviewMode.WHITELIST)
 *         .whitelist(List.of("delete_file", "send_email"))
 *         .build())
 *     .build();
 *
 * // 执行 Agent
 * AgentResponse response = agent.invoke(request);
 *
 * // 检查是否需要审批
 * if (response.requiresApproval()) {
 *     ApprovalRequest approval = response.getApprovalRequest();
 *     // 展示审批界面...
 *
 *     // 用户审批后恢复执行
 *     response = agent.resume(
 *         approval.getCheckpointId(),
 *         approval.allow()
 *     );
 * }
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Slf4j
public class HumanInTheLoopMiddleware implements Middleware {

    /**
     * 审核模式
     */
    public enum ReviewMode {
        /**
         * 审核所有工具调用
         */
        ALWAYS,
        /**
         * 只审核白名单中的工具
         */
        WHITELIST,
        /**
         * 审核除黑名单外的所有工具
         */
        BLACKLIST,
        /**
         * 不进行审核
         */
        NEVER
    }

    private final ReviewMode mode;
    private final List<String> whitelist;
    private final List<String> blacklist;

    private HumanInTheLoopMiddleware(Builder builder) {
        this.mode = builder.mode;
        this.whitelist = builder.whitelist;
        this.blacklist = builder.blacklist;
    }

    @Override
    public boolean beforeToolCall(ToolCall toolCall, AgentContext context) {
        // 检查是否需要审批
        if (!needsReview(toolCall)) {
            return true;
        }

        log.info("工具需要人工审批: 工具={}, 调用={}", toolCall.getName(), toolCall.getId());

        // 创建审批请求
        ApprovalRequest approval = ApprovalRequest.fromToolCall(
                toolCall,
                context.getRequest().getThreadId(),
                null // checkpointId 会在 ReActStrategy 中设置
        );

        // 设置到 context，ReActStrategy 会处理
        context.setPendingApproval(approval);

        // 返回 false 阻止工具执行
        return false;
    }

    /**
     * 检查是否需要审批
     */
    private boolean needsReview(ToolCall toolCall) {
        return switch (mode) {
            case ALWAYS -> true;
            case NEVER -> false;
            case WHITELIST -> whitelist.contains(toolCall.getName());
            case BLACKLIST -> !blacklist.contains(toolCall.getName());
        };
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器
     */
    public static class Builder {

        private ReviewMode mode = ReviewMode.WHITELIST;
        private List<String> whitelist = List.of();
        private List<String> blacklist = List.of();

        /**
         * 设置审核模式（默认 WHITELIST）
         */
        public Builder mode(ReviewMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * 设置白名单
         */
        public Builder whitelist(List<String> whitelist) {
            this.whitelist = whitelist != null ? whitelist : List.of();
            return this;
        }

        /**
         * 设置黑名单
         */
        public Builder blacklist(List<String> blacklist) {
            this.blacklist = blacklist != null ? blacklist : List.of();
            return this;
        }

        /**
         * 构建中间件实例
         */
        public HumanInTheLoopMiddleware build() {
            return new HumanInTheLoopMiddleware(this);
        }
    }
}
