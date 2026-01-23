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
     *     private final StringBuilder content = new StringBuilder();
     *
     *     @Override
     *     public void onToken(String token) {
     *         content.append(token);
     *         System.out.print(token);
     *     }
     *
     *     @Override
     *     public void onComplete() {
     *         System.out.println("\n完整内容: " + content);
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
    public void onToken(String token) {
        // 默认空实现
    }

    @Override
    public void onToolCall(ToolCall toolCall) {
        // 默认空实现
    }

    @Override
    public void onReasoning(String reasoning) {
        // 默认空实现
    }

    @Override
    public void onStart() {
        // 默认空实现
    }

    @Override
    public void onComplete() {
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
