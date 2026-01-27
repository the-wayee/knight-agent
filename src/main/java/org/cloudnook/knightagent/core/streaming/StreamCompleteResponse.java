package org.cloudnook.knightagent.core.streaming;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

/**
 * 流式输出完成响应
 * <p>
 * 在流式输出完成时，传递完整的响应信息给用户。
 * 包含所有 token 拼接后的完整内容、工具调用列表、Token 使用情况等。
 * <p>
 * 与 {@link StreamChunk} 不同，本对象表示**完整的**响应结果。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder
public class StreamCompleteResponse {

    /**
     * 完整的文本内容
     * <p>
     * 所有增量 token 拼接后的完整文本。
     * 如果模型只调用了工具而没有生成文本，此字段可能为空字符串。
     */
    private final String fullContent;

    /**
     * 完整的工具调用列表
     * <p>
     * 流式输出过程中累积的所有工具调用。
     * 如果没有工具调用，此字段为空列表。
     */
    private final List<ToolCallComplete> toolCalls;

    /**
     * 结束原因
     * <p>
     * 可能的值：
     * <ul>
     *   <li>"stop" - 正常结束</li>
     *   <li>"length" - 达到 max_tokens</li>
     *   <li>"tool_calls" - 需要调用工具</li>
     *   <li>"content_filter" - 内容被过滤</li>
     *   <li>"unknown" - 未知原因</li>
     * </ul>
     */
    private final String finishReason;

    /**
     * Token 使用情况
     */
    private final StreamChunk.Usage usage;

    /**
     * 模型名称
     * <p>
     * 实际使用的模型，如 "gpt-3.5-turbo-0125"
     */
    private final String model;

    /**
     * 响应 ID
     * <p>
     * 用于追踪整个请求的响应
     */
    private final String id;

    /**
     * 判断是否有工具调用
     */
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    /**
     * 判断是否有 usage 信息
     */
    public boolean hasUsage() {
        return usage != null;
    }

    /**
     * 获取工具调用列表（安全的 Optional 封装）
     */
    public Optional<List<ToolCallComplete>> getToolCalls() {
        return Optional.ofNullable(toolCalls);
    }

    /**
     * 工具调用完整信息
     */
    @Data
    @Builder
    public static class ToolCallComplete {
        /**
         * 工具调用 ID
         */
        private final String id;

        /**
         * 工具名称
         */
        private final String name;

        /**
         * 工具参数（完整的 JSON 字符串）
         */
        private final String arguments;
    }
}
