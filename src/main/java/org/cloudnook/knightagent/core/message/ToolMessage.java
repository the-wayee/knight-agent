package org.cloudnook.knightagent.core.message;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 工具消息
 * <p>
 * 用于向 LLM 反馈工具执行的结果。
 * 当 Agent 执行完工具调用后，需要将结果通过 ToolMessage 传回给 LLM，
 * LLM 基于这个结果继续生成最终响应或进行下一步操作。
 * <p>
 * 示例：
 * <pre>{@code
 * ToolMessage message = ToolMessage.builder()
 *     .toolCallId("call_abc123")
 *     .result("{\"temperature\": 25, \"condition\": \"晴朗\"}")
 *     .error(false)
 *     .build();
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ToolMessage extends Message {

    /**
     * 关联的工具调用 ID
     * <p>
     * 必须与 {@link ToolCall#id} 对应，用于将结果与请求关联起来。
     */
    private String toolCallId;

    /**
     * 工具执行结果
     * <p>
     * 工具执行后返回的结果，通常是 JSON 字符串格式。
     */
    private String result;

    /**
     * 是否为错误结果
     * <p>
     * 如果工具执行过程中发生错误，将此字段设为 true，
     * LLM 可以根据错误信息进行调整或重试。
     */
    private boolean error;

    /**
     * 错误信息
     * <p>
     * 当 {@link #error} 为 true 时，此字段包含错误详情。
     */
    private String errorMessage;

    @Override
    public String getContent() {
        // 对于工具消息，content 就是 result
        return result;
    }

    @Override
    public MessageType getType() {
        return MessageType.TOOL;
    }

    /**
     * 创建成功结果消息
     *
     * @param toolCallId 工具调用 ID
     * @param result     执行结果
     * @return 工具消息实例
     */
    public static ToolMessage success(String toolCallId, String result) {
        return ToolMessage.builder()
                .toolCallId(toolCallId)
                .result(result)
                .error(false)
                .build();
    }

    /**
     * 创建错误结果消息
     *
     * @param toolCallId    工具调用 ID
     * @param errorMessage 错误信息
     * @return 工具消息实例
     */
    public static ToolMessage error(String toolCallId, String errorMessage) {
        return ToolMessage.builder()
                .toolCallId(toolCallId)
                .error(true)
                .errorMessage(errorMessage)
                .result(errorMessage)
                .build();
    }

    /**
     * 创建错误结果消息（带异常）
     *
     * @param toolCallId 工具调用 ID
     * @param e          异常
     * @return 工具消息实例
     */
    public static ToolMessage error(String toolCallId, Exception e) {
        return ToolMessage.builder()
                .toolCallId(toolCallId)
                .error(true)
                .errorMessage(e.getMessage())
                .result(e.getClass().getSimpleName() + ": " + e.getMessage())
                .build();
    }
}
