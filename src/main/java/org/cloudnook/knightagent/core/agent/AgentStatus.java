package org.cloudnook.knightagent.core.agent;

import lombok.Builder;
import lombok.Data;

/**
 * Agent 状态
 * <p>
 * 表示 Agent 的运行时状态。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder
public class AgentStatus {

    /**
     * 状态类型
     */
    private final StatusType statusType;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 当前 Thread ID（如果在执行中）
     */
    private final String currentThreadId;

    /**
     * 当前迭代次数
     */
    private final int currentIteration;

    /**
     * 开始时间（如果在执行中）
     */
    private final long startTime;

    /**
     * 状态类型枚举
     */
    public enum StatusType {
        /**
         * 空闲
         */
        IDLE,
        /**
         * 执行中
         */
        RUNNING,
        /**
         * 等待工具调用
         */
        WAITING_FOR_TOOL,
        /**
         * 等待用户输入
         */
        WAITING_FOR_USER,
        /**
         * 错误
         */
        ERROR,
        /**
         * 已停止
         */
        STOPPED
    }

    /**
     * 创建空闲状态
     */
    public static AgentStatus idle() {
        return AgentStatus.builder()
                .statusType(StatusType.IDLE)
                .description("Agent 空闲")
                .build();
    }

    /**
     * 创建运行中状态
     *
     * @param threadId Thread ID
     */
    public static AgentStatus running(String threadId) {
        return AgentStatus.builder()
                .statusType(StatusType.RUNNING)
                .description("Agent 执行中")
                .currentThreadId(threadId)
                .startTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建等待工具状态
     *
     * @param threadId   Thread ID
     * @param iteration  当前迭代次数
     */
    public static AgentStatus waitingForTool(String threadId, int iteration) {
        return AgentStatus.builder()
                .statusType(StatusType.WAITING_FOR_TOOL)
                .description("等待工具执行")
                .currentThreadId(threadId)
                .currentIteration(iteration)
                .build();
    }

    /**
     * 创建错误状态
     *
     * @param error 错误信息
     */
    public static AgentStatus error(String error) {
        return AgentStatus.builder()
                .statusType(StatusType.ERROR)
                .description(error)
                .build();
    }

    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return statusType == StatusType.RUNNING ||
                statusType == StatusType.WAITING_FOR_TOOL ||
                statusType == StatusType.WAITING_FOR_USER;
    }

    /**
     * 是否空闲
     */
    public boolean isIdle() {
        return statusType == StatusType.IDLE;
    }

    /**
     * 是否出错
     */
    public boolean isError() {
        return statusType == StatusType.ERROR;
    }

    /**
     * 获取已运行时间（毫秒）
     */
    public long getRunningDuration() {
        if (startTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }
}
