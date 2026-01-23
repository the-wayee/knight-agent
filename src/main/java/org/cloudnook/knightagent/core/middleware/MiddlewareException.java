package org.cloudnook.knightagent.core.middleware;

/**
 * 中间件异常
 * <p>
 * 当中间件处理过程中发生错误时抛出。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class MiddlewareException extends Exception {

    /**
     * 发生异常的中间件名称
     */
    private final String middlewareName;

    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public MiddlewareException(String message) {
        this(message, null, null);
    }

    /**
     * 构造函数（带中间件名称）
     *
     * @param message         错误信息
     * @param middlewareName  中间件名称
     */
    public MiddlewareException(String message, String middlewareName) {
        this(message, middlewareName, null);
    }

    /**
     * 构造函数（带原因）
     *
     * @param message        错误信息
     * @param middlewareName 中间件名称
     * @param cause          原始异常
     */
    public MiddlewareException(String message, String middlewareName, Throwable cause) {
        super(message, cause);
        this.middlewareName = middlewareName;
    }

    /**
     * 获取中间件名称
     */
    public String getMiddlewareName() {
        return middlewareName;
    }
}
