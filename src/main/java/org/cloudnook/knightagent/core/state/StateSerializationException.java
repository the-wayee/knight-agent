package org.cloudnook.knightagent.core.state;

/**
 * 状态序列化异常
 * <p>
 * 当状态序列化或反序列化失败时抛出。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class StateSerializationException extends Exception {

    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public StateSerializationException(String message) {
        super(message);
    }

    /**
     * 构造函数（带原因）
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public StateSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
