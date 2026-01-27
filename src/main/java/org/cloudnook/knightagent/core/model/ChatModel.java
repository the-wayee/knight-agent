package org.cloudnook.knightagent.core.model;

import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.message.AIMessage;
import org.cloudnook.knightagent.core.streaming.StreamCallback;

import java.util.List;

/**
 * 聊天模型接口
 * <p>
 * 定义与大语言模型（LLM）交互的统一接口。
 * 支持同步调用、流式调用、Token 计数等功能。
 * <p>
 * 实现此接口来支持不同的 LLM 提供商：
 * <ul>
 *   <li>OpenAI (GPT-4, GPT-3.5)</li>
 *   <li>Anthropic (Claude)</li>
 *   <li>Google (Gemini)</li>
 *   <li>本地模型 (Ollama, LocalAI)</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * ChatModel model = new OpenAIChatModel(apiKey);
 *
 * // 同步调用
 * List<Message> messages = List.of(
 *     HumanMessage.of("你好，请介绍一下你自己")
 * );
 * AIMessage response = model.chat(messages, ChatOptions.defaults());
 * System.out.println(response.getContent());
 *
 * // 流式调用
 * model.chatStream(messages, ChatOptions.defaults(), new StreamCallbackAdapter() {
 *     @Override
 *     public void onToken(String token) {
 *         System.out.print(token);
 *     }
 * });
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public interface ChatModel {

    /**
     * 同步调用模型
     * <p>
     * 发送消息列表到 LLM，等待完整响应后返回。
     * <p>
     * 注意：
     * <ul>
     *   <li>此方法是阻塞的，响应时间取决于模型和输入长度</li>
     *   <li>返回的 AIMessage 可能包含工具调用请求</li>
     *   <li>如果模型不支持某些选项，选项会被忽略</li>
     * </ul>
     *
     * @param messages 消息列表，按时间顺序排列（最早的消息在前）
     * @param options  调用选项，可以为 null 使用默认值
     * @return AI 响应消息
     * @throws ModelException 调用失败时抛出
     */
    AIMessage chat(List<Message> messages, ChatOptions options) throws ModelException;

    /**
     * 同步调用模型（使用默认选项）
     *
     * @param messages 消息列表
     * @return AI 响应消息
     * @throws ModelException 调用失败时抛出
     */
    default AIMessage chat(List<Message> messages) throws ModelException {
        return chat(messages, ChatOptions.defaults());
    }

    /**
     * 流式调用模型
     * <p>
     * 发送消息到 LLM，通过回调逐步接收响应。
     * <p>
     * 流式输出的特点：
     * <ul>
     *   <li>Token 会增量地通过 {@link StreamCallback#onToken(String)} 传递</li>
     *   <li>工具调用事件会通过 {@link StreamCallback#onToolCall} 传递</li>
     *   <li>流结束时调用 {@link StreamCallback#onCompletion(StreamCompleteResponse)}</li>
     *   <li>发生错误时调用 {@link StreamCallback#onError(Throwable)}</li>
     * </ul>
     *
     * @param messages 消息列表
     * @param options  调用选项
     * @param callback 流式回调接口
     * @throws ModelException 调用失败时抛出
     */
    void chatStream(List<Message> messages, ChatOptions options, StreamCallback callback) throws ModelException;

    /**
     * 流式调用模型（使用默认选项）
     *
     * @param messages 消息列表
     * @param callback 流式回调接口
     * @throws ModelException 调用失败时抛出
     */
    default void chatStream(List<Message> messages, StreamCallback callback) throws ModelException {
        chatStream(messages, ChatOptions.defaults(), callback);
    }


    /**
     * 获取模型标识
     * <p>
     * 返回模型的唯一标识符，如 "gpt-4", "claude-3-opus" 等。
     *
     * @return 模型标识
     */
    String getModelId();

    /**
     * 检查模型是否可用
     * <p>
     * 用于健康检查，可以发送简单的测试请求。
     *
     * @return 如果模型可用返回 true
     */
    default boolean isAvailable() {
        try {
            // 发送简单的测试消息
            chat(java.util.List.of(org.cloudnook.knightagent.core.message.HumanMessage.of("Hi")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 关闭模型
     * <p>
     * 释放模型占用的资源，如关闭连接、清理缓存等。
     */
    default void close() {
        // 默认空实现
    }

    /**
     * 获取模型版本
     *
     * @return 模型版本，默认 "1.0.0"
     */
    default String getVersion() {
        return "1.0.0";
    }
}
