package org.cloudnook.knightagent.core.streaming;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * SSE 流式数据块
 * <p>
 * 封装 OpenAI/Anthropic 等 LLM 返回的单次 SSE 数据。
 * <p>
 * OpenAI SSE 流格式示例：
 * <pre>{@code
 * data: {
 *   "id": "chatcmpl-123",
 *   "object": "chat.completion.chunk",
 *   "created": 1694268190,
 *   "model": "gpt-3.5-turbo-0125",
 *   "choices": [{
 *     "index": 0,
 *     "delta": {
 *       "content": "Hello",
 *       "role": null,
 *       "tool_calls": null
 *     },
 *     "finish_reason": null
 *   }],
 *   "usage": null
 * }
 * }</pre>
 * <p>
 * 最后一行包含 usage：
 * <pre>{@code
 * data: {
 *   "id": "chatcmpl-123",
 *   "choices": [{
 *     "index": 0,
 *     "delta": {},
 *     "finish_reason": "stop"
 *   }],
 *   "usage": {
 *     "prompt_tokens": 10,
 *     "completion_tokens": 5,
 *     "total_tokens": 15
 *   }
 * }
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder
public class StreamChunk {

    /**
     * 响应 ID
     * <p>
     * 用于追踪整个请求的响应
     */
    private String id;

    /**
     * 模型名称
     * <p>
     * 实际使用的模型，如 "gpt-3.5-turbo-0125"
     */
    private String model;

    /**
     * 创建时间戳
     * <p>
     * Unix 时间戳（秒）
     */
    private Long created;

    /**
     * 内容增量
     * <p>
     * 本次 chunk 包含的文本内容
     */
    private String content;

    /**
     * 结束原因
     * <p>
     * 可能的值：
     * <ul>
     *   <li>"stop" - 正常结束</li>
     *   <li>"length" - 达到 max_tokens</li>
     *   <li>"tool_calls" - 需要调用工具</li>
     *   <li>"content_filter" - 内容被过滤</li>
     * </ul>
     */
    private String finishReason;

    /**
     * Token 使用情况
     * <p>
     * 只在最后一个 chunk 中存在
     */
    private Usage usage;

    /**
     * 原始数据
     * <p>
     * 完整的 SSE JSON 数据，用于调试
     */
    private Map<String, Object> raw;

    /**
     * Token 使用情况
     */
    @Data
    @Builder
    public static class Usage {
        /**
         * 输入 token 数量
         */
        private Integer promptTokens;

        /**
         * 输出 token 数量
         */
        private Integer completionTokens;

        /**
         * 总 token 数量
         */
        private Integer totalTokens;
    }

    /**
     * 判断是否为结束 chunk
     */
    public boolean isFinished() {
        return finishReason != null && !finishReason.isEmpty();
    }

    /**
     * 判断是否包含 usage 信息
     */
    public boolean hasUsage() {
        return usage != null;
    }
}
