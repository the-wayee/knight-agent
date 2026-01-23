package org.cloudnook.knightagent.core.agent;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Agent 响应
 * <p>
 * 封装 Agent 执行后的所有输出信息。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder
public class AgentResponse {

    /**
     * 最终 AI 消息
     * <p>
     * Agent 执行完成后的最终响应消息。
     */
    private final String output;

    /**
     * 消息历史
     * <p>
     * 包含本次执行过程中的所有消息。
     */
    private final List<org.cloudnook.knightagent.core.message.Message> messages;

    /**
     * 使用的工具调用
     * <p>
     * 本次执行中调用的工具列表。
     */
    private final List<org.cloudnook.knightagent.core.message.ToolCall> toolCalls;

    /**
     * 状态
     * <p>
     * Agent 执行后的状态快照。
     */
    private final org.cloudnook.knightagent.core.state.AgentState state;

    /**
     * Thread ID
     * <p>
     * 关联的对话会话 ID。
     */
    private final String threadId;

    /**
     * 检查点 ID
     * <p>
     * 保存的状态检查点 ID。
     */
    private final String checkpointId;

    /**
     * 执行耗时（毫秒）
     */
    private final long durationMs;

    /**
     * 使用的 Token 数量
     */
    private final int tokensUsed;

    /**
     * 开始时间
     */
    private final Instant startTime;

    /**
     * 结束时间
     */
    private final Instant endTime;

    /**
     * 错误信息（如果执行失败）
     */
    private final String error;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return error == null || error.isEmpty();
    }

    /**
     * 获取最终消息（便捷方法）
     */
    public Optional<org.cloudnook.knightagent.core.message.AIMessage> getFinalMessage() {
        if (messages == null || messages.isEmpty()) {
            return Optional.empty();
        }
        // 返回最后一条消息
        org.cloudnook.knightagent.core.message.Message last = messages.get(messages.size() - 1);
        if (last instanceof org.cloudnook.knightagent.core.message.AIMessage) {
            return Optional.of((org.cloudnook.knightagent.core.message.AIMessage) last);
        }
        return Optional.empty();
    }

    /**
     * 是否有工具调用
     */
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    /**
     * 获取工具调用数量
     */
    public int getToolCallCount() {
        return toolCalls != null ? toolCalls.size() : 0;
    }

    /**
     * 创建错误响应
     *
     * @param error 错误信息
     * @return Agent 响应
     */
    public static AgentResponse error(String error) {
        return AgentResponse.builder()
                .output(error)
                .error(error)
                .build();
    }

    /**
     * 创建简单响应
     *
     * @param output 输出文本
     * @return Agent 响应
     */
    public static AgentResponse of(String output) {
        return AgentResponse.builder()
                .output(output)
                .build();
    }
}
