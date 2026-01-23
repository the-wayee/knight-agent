package org.cloudnook.knightagent.core.streaming;

import org.cloudnook.knightagent.core.message.ToolCall;

/**
 * 流式输出回调接口
 * <p>
 * 用于接收 LLM 流式输出的各种事件。
 * 通过实现这个接口，可以实时处理 AI 的响应，而不是等待完整响应。
 * <p>
 * 支持的事件类型：
 * <ul>
 *   <li>增量 Token - 文本内容的逐步生成</li>
 *   <li>工具调用 - LLM 决定调用工具</li>
 *   <li>思考过程 - 模型的推理链（如 Claude 的 thinking）</li>
 *   <li>完成/错误 - 流的结束状态</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * StreamCallback callback = new StreamCallback() {
 *     private final StringBuilder fullContent = new StringBuilder();
 *
 *     @Override
 *     public void onToken(String token) {
 *         fullContent.append(token);
 *         System.out.print(token); // 实时输出
 *     }
 *
 *     @Override
 *     public void onComplete() {
 *         System.out.println("\n完成！总长度: " + fullContent.length());
 *     }
 * };
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public interface StreamCallback {

    /**
     * 接收增量 Token
     * <p>
     * LLM 每生成一段文本，就会调用此方法。
     * Token 可能是一个词、一个字符，或一个完整的短语，具体取决于模型实现。
     *
     * @param token 增量文本内容
     */
    void onToken(String token);

    /**
     * 接收工具调用事件
     * <p>
     * 当 LLM 决定调用工具时触发。
     * 注意：工具调用通常是一次性事件，不是流式的。
     *
     * @param toolCall 工具调用信息
     */
    default void onToolCall(ToolCall toolCall) {
        // 默认空实现，子类可以选择性重写
    }

    /**
     * 接收思考链内容
     * <p>
     * 某些模型（如 Claude 的 thinking 模式）会输出推理过程。
     * 这部分内容对于调试和分析很有用，但通常不需要展示给最终用户。
     *
     * @param reasoning 思考内容
     */
    default void onReasoning(String reasoning) {
        // 默认空实现，子类可以选择性重写
    }

    /**
     * 流完成
     * <p>
     * 当 LLM 完成所有输出（包括工具调用）后触发。
     * 此时可以获取完整的响应内容。
     */
    default void onComplete() {
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
     * 流开始
     * <p>
     * 当流式输出开始时触发，在任何其他事件之前。
     * 可以用于初始化资源。
     */
    default void onStart() {
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
