package org.cloudnook.knightagent.core.streaming;

import org.cloudnook.knightagent.core.message.ToolCall;

/**
 * 流式输出回调接口
 * <p>
 * 用于接收 LLM 流式输出的各种事件。
 * 通过实现这个接口，可以实时处理 AI 的响应，而不是等待完整响应。
 * <p>
 * OpenAI SSE 流格式：
 * <pre>{@code
 * data: {
 *   "id": "chatcmpl-123",
 *   "object": "chat.completion.chunk",
 *   "created": 1694268190,
 *   "model": "gpt-3.5-turbo-0125",
 *   "choices": [{
 *     "index": 0,
 *     "delta": {"content": "Hello"},
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
 * <p>
 * 支持的事件类型：
 * <ul>
 *   <li>{@link #onToken(StreamChunk)} - 增量 Token</li>
 *   <li>{@link #onToolCall(StreamChunk, ToolCall)} - 工具调用</li>
 *   <li>{@link #onComplete(StreamChunk)} - 流完成</li>
 *   <li>{@link #onError(Throwable)} - 错误</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * StreamCallback callback = new StreamCallback() {
 *     private final StringBuilder fullContent = new StringBuilder();
 *     private int totalTokens = 0;
 *
 *     @Override
 *     public void onStart() {
 *         System.out.println("开始流式输出...");
 *     }
 *
 *     @Override
 *     public void onToken(StreamChunk chunk) {
 *         fullContent.append(chunk.getContent());
 *         System.out.print(chunk.getContent());
 *     }
 *
 *     @Override
 *     public void onComplete(StreamChunk finalChunk) {
 *         if (finalChunk.hasUsage()) {
 *             totalTokens = finalChunk.getUsage().getTotalTokens();
 *         }
 *         System.out.println("\n完成！总长度: " + fullContent.length()
 *             + ", tokens: " + totalTokens);
 *     }
 * };
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public interface StreamCallback {

    /**
     * 流开始
     * <p>
     * 当流式输出开始时触发，在任何其他事件之前。
     * 可以用于初始化资源。
     */
    default void onStart() {
        // 默认空实现，子类可以选择性重写
    }

    /**
     * 接收增量 Token
     * <p>
     * LLM 每生成一段文本，就会调用此方法。
     * Token 可能是一个词、一个字符，或一个完整的短语，具体取决于模型实现。
     * <p>
     * 通过 {@link StreamChunk} 可以获取完整的上下文信息：
     * <ul>
     *   <li>{@link StreamChunk#getContent()} - 文本内容</li>
     *   <li>{@link StreamChunk#getId()} - 响应 ID</li>
     *   <li>{@link StreamChunk#getModel()} - 模型名称</li>
     *   <li>{@link StreamChunk#getFinishReason()} - 结束原因（最后一行）</li>
     * </ul>
     *
     * @param chunk 数据块，包含内容和元数据
     */
    void onToken(StreamChunk chunk);

    /**
     * 接收工具调用事件
     * <p>
     * 当 LLM 决定调用工具时触发。
     * 注意：工具调用通常是一次性事件，不是流式的。
     *
     * @param chunk     数据块
     * @param toolCall 工具调用信息
     */
    default void onToolCall(StreamChunk chunk, ToolCall toolCall) {
        // 默认空实现，子类可以选择性重写
    }

    /**
     * 接收思考链内容
     * <p>
     * 某些模型（如 Claude 的 thinking 模式）会输出推理过程。
     * 这部分内容对于调试和分析很有用，但通常不需要展示给最终用户。
     *
     * @param chunk     数据块
     * @param reasoning 思考内容
     */
    default void onReasoning(StreamChunk chunk, String reasoning) {
        // 默认空实现，子类可以选择性重写
    }

    /**
     * 流完成
     * <p>
     * 当 LLM 完成所有输出（包括工具调用）后触发。
     * 此时 {@link StreamChunk} 包含完整的使用情况：
     * <ul>
     *   <li>{@link StreamChunk#getFinishReason()} - 结束原因</li>
     *   <li>{@link StreamChunk#getUsage()} - Token 使用情况</li>
     * </ul>
     *
     * @param finalChunk 最后一个数据块，包含 finish_reason 和 usage
     */
    default void onComplete(StreamChunk finalChunk) {
        // 默认空实现，子类可以选择性重写
    }

    /**
     * 发生错误
     * <p>
     * 当流式输出过程中发生错误时触发。
     * 错误发生后，将不会再有其他事件。
     *
     * @param error 错误信息
     */
    default void onError(Throwable error) {
        // 默认空实现，子类可以选择性重写
    }

    /**
     * 获取是否已完成
     * <p>
     * 用于判断流是否已经结束（完成或出错）。
     *
     * @return 如果流已结束返回 true
     */
    default boolean isFinished() {
        return false;
    }
}
