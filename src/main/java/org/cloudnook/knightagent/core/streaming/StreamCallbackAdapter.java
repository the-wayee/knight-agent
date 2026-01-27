package org.cloudnook.knightagent.core.streaming;

import org.cloudnook.knightagent.core.message.ToolCall;

/**
 * 流式回调适配器
 * <p>
 * 提供所有 {@link StreamCallback} 方法的空实现。
 * 继承此类后，只需重写感兴趣的方法，简化代码编写。
 * <p>
 * 使用示例：
 * <pre>{@code
 * StreamCallback callback = new StreamCallbackAdapter() {
 *
 *     @Override
 *     public void onToken(StreamChunk chunk) {
 *         System.out.print(chunk.getContent());  // 实时输出每个 token
 *     }
 *
 *     @Override
 *     public void onCompletion(StreamCompleteResponse response) {
 *         // 获取完整内容（框架已自动累积）
 *         System.out.println("\n完整内容: " + response.getFullContent());
 *
 *         // 获取 token 用量
 *         if (response.hasUsage()) {
 *             System.out.println("总 tokens: " + response.getUsage().getTotalTokens());
 *         }
 *     }
 * };
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public abstract class StreamCallbackAdapter implements StreamCallback {

    private volatile boolean finished = false;

    @Override
    public void onToken(StreamChunk chunk) {
        // 默认空实现，子类可以重写
    }

    @Override
    public void onToolCall(StreamChunk chunk, ToolCall toolCall) {
        // 默认空实现，子类可以重写
    }

    @Override
    public void onReasoning(StreamChunk chunk, String reasoning) {
        // 默认空实现，子类可以重写
    }

    @Override
    public void onStart() {
        // 默认空实现，子类可以重写
    }

    @Override
    public void onCompletion(StreamCompleteResponse response) {
        this.finished = true;
    }

    @Override
    public void onError(Throwable error) {
        this.finished = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    /**
     * 标记为完成状态
     * <p>
     * 子类可以在需要时主动标记流结束。
     */
    protected void markFinished() {
        this.finished = true;
    }
}
