package org.cloudnook.knightagent.core.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果
 * <p>
 * 封装工具执行后的返回结果，供内部使用。
 * 与 {@link ToolMessage} 不同，这个类主要用于在 Agent 内部传递工具执行结果，
 * 而 ToolMessage 是用于与 LLM 通信的消息类型。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {

    /**
     * 关联的工具调用 ID
     */
    private String toolCallId;

    /**
     * 执行结果
     */
    private String result;

    /**
     * 是否出错
     */
    private boolean error;

    /**
     * 错误信息（如果出错）
     */
    private String errorMessage;

    /**
     * 创建成功结果
     *
     * @param toolCallId 工具调用 ID
     * @param result     执行结果
     * @return 工具执行结果
     */
    public static ToolResult success(String toolCallId, String result) {
        return ToolResult.builder()
                .toolCallId(toolCallId)
                .result(result)
                .error(false)
                .build();
    }

    /**
     * 创建错误结果
     *
     * @param toolCallId    工具调用 ID
     * @param errorMessage 错误信息
     * @return 工具执行结果
     */
    public static ToolResult error(String toolCallId, String errorMessage) {
        return ToolResult.builder()
                .toolCallId(toolCallId)
                .error(true)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 将工具执行结果转换为工具消息
     *
     * @return 工具消息
     */
    public ToolMessage toMessage() {
        if (error) {
            return ToolMessage.error(toolCallId, errorMessage);
        } else {
            return ToolMessage.success(toolCallId, result);
        }
    }
}
