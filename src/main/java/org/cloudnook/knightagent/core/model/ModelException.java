package org.cloudnook.knightagent.core.model;

import org.cloudnook.knightagent.core.exception.ErrorCode;

/**
 * 模型异常
 * <p>
 * 当调用 LLM 时发生错误时抛出此异常。
 * 提供统一的错误码支持，便于程序化处理。
 * <p>
 * 常见错误原因：
 * <ul>
 *   <li>API 密钥无效或过期</li>
 *   <li>网络连接失败</li>
 *   <li>请求超时</li>
 *   <li>配额用尽</li>
 *   <li>输入内容过长</li>
 *   <li>模型服务不可用</li>
 * </ul>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class ModelException extends RuntimeException {

    /**
     * 错误码
     */
    private final ErrorCode errorCode;

    /**
     * HTTP 状态码（如果是 API 调用错误）
     */
    private final Integer httpStatusCode;

    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public ModelException(String message) {
        this(ErrorCode.MODEL_ERROR, message, null, null);
    }

    /**
     * 构造函数（带原因）
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public ModelException(String message, Throwable cause) {
        this(ErrorCode.MODEL_ERROR, message, cause, null);
    }

    /**
     * 构造函数（带错误码）
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public ModelException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    /**
     * 构造函数（带错误码和原因）
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     原始异常
     */
    public ModelException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, null);
    }

    /**
     * 完整构造函数
     *
     * @param message        错误信息
     * @param cause          原始异常
     * @param httpStatusCode HTTP 状态码
     */
    public ModelException(String message, Throwable cause, Integer httpStatusCode) {
        this(deriveErrorCode(cause, httpStatusCode), message, cause, httpStatusCode);
    }

    /**
     * 完整构造函数
     *
     * @param errorCode      错误码
     * @param message        错误信息
     * @param cause          原始异常
     * @param httpStatusCode HTTP 状态码
     */
    public ModelException(ErrorCode errorCode, String message, Throwable cause, Integer httpStatusCode) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
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
     * 获取 HTTP 状态码
     *
     * @return HTTP 状态码，如果不是 HTTP 错误返回 null
     */
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * 检查是否为认证错误
     */
    public boolean isAuthError() {
        return errorCode == ErrorCode.MODEL_UNAUTHORIZED ||
                (httpStatusCode != null && (httpStatusCode == 401 || httpStatusCode == 403));
    }

    /**
     * 检查是否为配额错误
     */
    public boolean isRateLimitError() {
        return errorCode == ErrorCode.MODEL_RATE_LIMIT ||
                (httpStatusCode != null && httpStatusCode == 429);
    }

    /**
     * 检查是否为超时错误
     */
    public boolean isTimeout() {
        return errorCode == ErrorCode.MODEL_TIMEOUT;
    }

    /**
     * 检查是否可重试
     */
    public boolean isRetryable() {
        return isTimeout() || isRateLimitError() ||
                errorCode == ErrorCode.MODEL_UNAVAILABLE;
    }

    /**
     * 从异常和 HTTP 状态码推导错误码
     */
    private static ErrorCode deriveErrorCode(Throwable cause, Integer httpStatusCode) {
        // 优先使用 HTTP 状态码
        if (httpStatusCode != null) {
            return switch (httpStatusCode) {
                case 401, 403 -> ErrorCode.MODEL_UNAUTHORIZED;
                case 429 -> ErrorCode.MODEL_RATE_LIMIT;
                case 500, 502, 503, 504 -> ErrorCode.MODEL_UNAVAILABLE;
                default -> ErrorCode.MODEL_ERROR;
            };
        }

        // 其次根据异常类型判断
        if (cause == null) {
            return ErrorCode.MODEL_ERROR;
        }

        String className = cause.getClass().getSimpleName();
        return switch (className) {
            case "SocketTimeoutException", "TimeoutException" -> ErrorCode.MODEL_TIMEOUT;
            case "ConnectException", "UnknownHostException" -> ErrorCode.MODEL_UNAVAILABLE;
            default -> ErrorCode.MODEL_ERROR;
        };
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建认证失败异常
     */
    public static ModelException unauthorized(String message) {
        return new ModelException(ErrorCode.MODEL_UNAUTHORIZED, message, null, 401);
    }

    /**
     * 创建配额超限异常
     */
    public static ModelException rateLimitExceeded(String message) {
        return new ModelException(ErrorCode.MODEL_RATE_LIMIT, message, null, 429);
    }

    /**
     * 创建超时异常
     */
    public static ModelException timeout(String message) {
        return new ModelException(ErrorCode.MODEL_TIMEOUT, message);
    }

    /**
     * 创建上下文过长异常
     */
    public static ModelException contextTooLong(String message) {
        return new ModelException(ErrorCode.MODEL_ERROR, message, null, 400);
    }

    /**
     * 创建服务不可用异常
     */
    public static ModelException serviceUnavailable(String message) {
        return new ModelException(ErrorCode.MODEL_UNAVAILABLE, message, null, 503);
    }
}
