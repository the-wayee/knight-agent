package org.cloudnook.knightagent.core.multiagent;

import lombok.Builder;
import lombok.Data;
import org.cloudnook.knightagent.core.middleware.AgentContext;

import java.util.Optional;

/**
 * Agent 手 off 消息
 * <p>
 * 表示 Agent 之间控制权转移的信号。
 * 当一个 Agent 无法完成任务或需要其他 Agent 协助时，
 * 可以发起手 off 将控制权转交给另一个 Agent。
 * <p>
 * 使用场景：
 * <ul>
 *   <li>专业分工 - 研究 Agent 将结果交给编程 Agent</li>
 *   <li>能力不足 - 常见问答 Agent 将复杂问题转给专家 Agent</li>
 *   <li>流程协作 - 审查 Agent 将问题返回给编程 Agent 修改</li>
 *   <li>完成确认 - Agent 完成任务后转交给终结器</li>
 * </ul>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder
public class AgentHandoff {

    /**
     * 来源 Agent 名称
     */
    private final String from;

    /**
     * 目标 Agent 名称
     * <p>
     * 如果为 null 或 "FINAL"，表示流程结束，不需要继续转交。
     */
    private final String to;

    /**
     * 传递的消息
     * <p>
     * 包含：
     * <ul>
     *   <li>当前状态说明</li>
     *   <li>已完成的工作</li>
     *   <li>下一步建议</li>
     * </ul>
     */
    private final String message;

    /**
     * 上下文数据
     * <p>
     * 可选的额外数据，用于在 Agent 之间传递信息。
     */
    private final AgentContext context;

    /**
     * 手 off 原因
     * <p>
     * 说明为什么发起手 off，用于调试和日志。
     */
    private final String reason;

    /**
     * 是否为最终手 off（结束流程）
     */
    public boolean isFinal() {
        return to == null || "FINAL".equalsIgnoreCase(to) || "END".equalsIgnoreCase(to);
    }

    /**
     * 创建转交给指定 Agent 的手 off
     *
     * @param toAgent 目标 Agent 名称
     * @param message 传递消息
     * @return AgentHandoff
     */
    public static AgentHandoff to(String toAgent, String message) {
        return AgentHandoff.builder()
                .to(toAgent)
                .message(message)
                .build();
    }

    /**
     * 创建转交给指定 Agent 的手 off（带原因）
     *
     * @param toAgent 目标 Agent 名称
     * @param message 传递消息
     * @param reason  手 off 原因
     * @return AgentHandoff
     */
    public static AgentHandoff to(String toAgent, String message, String reason) {
        return AgentHandoff.builder()
                .to(toAgent)
                .message(message)
                .reason(reason)
                .build();
    }

    /**
     * 创建完成手 off（结束流程）
     *
     * @param message 最终消息
     * @return AgentHandoff
     */
    public static AgentHandoff final_(String message) {
        return AgentHandoff.builder()
                .to("FINAL")
                .message(message)
                .reason("任务完成")
                .build();
    }

    /**
     * 创建带来源信息的手 off
     *
     * @param fromAgent 来源 Agent
     * @param toAgent   目标 Agent
     * @param message   传递消息
     * @return AgentHandoff
     */
    public static AgentHandoff fromTo(String fromAgent, String toAgent, String message) {
        return AgentHandoff.builder()
                .from(fromAgent)
                .to(toAgent)
                .message(message)
                .build();
    }

    /**
     * 从响应中解析手 off 消息
     * <p>
     * 尝试从 AgentResponse 的输出中解析出类似 "HANDOFF:agentName:..." 格式的指令。
     *
     * @param responseText Agent 响应文本
     * @return 解析出的手 off 消息
     */
    public static Optional<AgentHandoff> parseFromResponse(String responseText) {
        if (responseText == null || responseText.isEmpty()) {
            return Optional.empty();
        }

        // 支持多种格式
        // 1. HANDOFF:agentName:message
        // 2. [HANDOFF agentName] message
        // 3. @@agentName@@ message

        if (responseText.contains("HANDOFF:")) {
            String[] parts = responseText.split("HANDOFF:", 2);
            if (parts.length > 1) {
                String remaining = parts[1].trim();
                String[] handoffParts = remaining.split(":", 2);
                String toAgent = handoffParts[0].trim();
                String message = handoffParts.length > 1 ? handoffParts[1].trim() : "";
                return Optional.of(AgentHandoff.to(toAgent, message));
            }
        }

        if (responseText.contains("[HANDOFF ")) {
            int start = responseText.indexOf("[HANDOFF ");
            int end = responseText.indexOf("]", start);
            if (start >= 0 && end > start) {
                String agentName = responseText.substring(start + 9, end).trim();
                String message = responseText.substring(end + 1).trim();
                return Optional.of(AgentHandoff.to(agentName, message));
            }
        }

        return Optional.empty();
    }

    /**
     * 生成手 off 指令（供 Agent 输出）
     * <p>
     * Agent 可以在响应中输出此格式的指令来发起手 off。
     *
     * @return 手 off 指令字符串
     */
    public String toDirective() {
        if (isFinal()) {
            return "HANDOFF:FINAL:" + message;
        }
        return "HANDOFF:" + to + ":" + message;
    }

    /**
     * 生成人类可读的描述
     *
     * @return 描述文本
     */
    public String toDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Handoff");
        if (from != null) {
            sb.append(" from ").append(from);
        }
        if (to != null) {
            sb.append(" to ").append(to);
        }
        if (reason != null) {
            sb.append(" (").append(reason).append(")");
        }
        if (message != null && !message.isEmpty()) {
            sb.append(": ").append(message);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toDescription();
    }
}
