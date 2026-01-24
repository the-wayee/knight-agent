package org.cloudnook.knightagent.core.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * KnightAgent 异常基类
 * <p>
 * 所有框架异常的基类，提供统一的错误码和上下文信息支持。
 * <p>
 * 使用示例：
 * <pre>{@code
 * throw new ModelException(
 *     ErrorCode.MODEL_ERROR,
 *     "调用模型失败",
 *     cause
 * );
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public abstract class KnightAgentException extends Exception {

    private final ErrorCode errorCode;
    private final Map<String, Object> context;

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误消息
     */
    protected KnightAgentException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    /**
     * 构造函数（带原因）
     *
     * @param errorCode 错误码
     * @param message   错误消息
     * @param cause     原始异常
     */
    protected KnightAgentException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误上下文（不可变视图）
     *
     * @return 错误上下文
     */
    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }

    /**
     * 添加上下文信息
     *
     * @param key   键
     * @param value 值
     * @return this
     */
    public KnightAgentException addContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "errorCode=" + errorCode +
                ", message='" + getMessage() + '\'' +
                (getCause() != null ? ", cause=" + getCause().getClass().getSimpleName() : "") +
                '}';
    }
}
