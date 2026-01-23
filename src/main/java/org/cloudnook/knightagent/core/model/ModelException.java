package org.cloudnook.knightagent.core.model;

/**
 * 模型异常
 * <p>
 * 当调用 LLM 时发生错误时抛出此异常。
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
     * 错误代码
     * <p>
     * 用于程序化地识别错误类型。
     */
    private final String errorCode;

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
        this(message, (Throwable) null, null);
    }

    /**
     * 构造函数（带原因）
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public ModelException(String message, Throwable cause) {
        this(message, cause, null);
    }

    /**
     * 构造函数（带错误代码）
     *
     * @param message   错误信息
     * @param errorCode 错误代码
     */
    public ModelException(String message, String errorCode) {
        this(message, errorCode, null);
    }

    /**
     * 构造函数（带 HTTP 状态码）
     *
     * @param message        错误信息
     * @param httpStatusCode HTTP 状态码
     */
    public ModelException(String message, int httpStatusCode) {
        this(message, (Throwable) null, httpStatusCode);
    }

    /**
     * 完整构造函数
     *
     * @param message        错误信息
     * @param cause          原始异常
     * @param httpStatusCode HTTP 状态码
     */
    public ModelException(String message, Throwable cause, Integer httpStatusCode) {
        super(message, cause);
        this.errorCode = deriveErrorCode(cause, httpStatusCode);
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * 完整构造函数（带错误代码）
     *
     * @param message        错误信息
     * @param errorCode      错误代码
     * @param httpStatusCode HTTP 状态码
     */
    public ModelException(String message, String errorCode, Integer httpStatusCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码
     */
    public String getErrorCode() {
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
     * 从异常和 HTTP 状态码推导错误代码
     */
    private String deriveErrorCode(Throwable cause, Integer httpStatusCode) {
        // 优先使用 HTTP 状态码
        if (httpStatusCode != null) {
            return switch (httpStatusCode) {
                case 400 -> "BAD_REQUEST";
                case 401 -> "UNAUTHORIZED";
                case 403 -> "FORBIDDEN";
                case 404 -> "NOT_FOUND";
                case 429 -> "RATE_LIMIT_EXCEEDED";
                case 500, 502, 503, 504 -> "SERVICE_ERROR";
                default -> "HTTP_ERROR_" + httpStatusCode;
            };
        }

        // 其次根据异常类型判断
        if (cause == null) {
            return "UNKNOWN_ERROR";
        }

        String className = cause.getClass().getSimpleName();
        return switch (className) {
            case "SocketTimeoutException", "TimeoutException" -> "TIMEOUT";
            case "ConnectException", "UnknownHostException" -> "CONNECTION_ERROR";
            case "JsonProcessingException", "JsonParseException" -> "JSON_PARSE_ERROR";
            case "IOException" -> "IO_ERROR";
            default -> "EXECUTION_ERROR";
        };
    }

    /**
     * 检查是否为认证错误
     */
    public boolean isAuthError() {
        return "UNAUTHORIZED".equals(errorCode) || "FORBIDDEN".equals(errorCode);
    }

    /**
     * 检查是否为配额错误
     */
    public boolean isRateLimitError() {
        return "RATE_LIMIT_EXCEEDED".equals(errorCode);
    }

    /**
     * 检查是否为超时错误
     */
    public boolean isTimeout() {
        return "TIMEOUT".equals(errorCode);
    }

    /**
     * 检查是否可重试
     */
    public boolean isRetryable() {
        return isTimeout() || isRateLimitError() ||
                "SERVICE_ERROR".equals(errorCode) ||
                "CONNECTION_ERROR".equals(errorCode);
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建认证失败异常
     */
    public static ModelException unauthorized(String message) {
        return new ModelException(message, "UNAUTHORIZED", 401);
    }

    /**
     * 创建配额超限异常
     */
    public static ModelException rateLimitExceeded(String message) {
        return new ModelException(message, "RATE_LIMIT_EXCEEDED", 429);
    }

    /**
     * 创建超时异常
     */
    public static ModelException timeout(String message) {
        return new ModelException(message, "TIMEOUT", null);
    }

    /**
     * 创建上下文过长异常
     */
    public static ModelException contextTooLong(String message) {
        return new ModelException(message, "CONTEXT_TOO_LONG", 400);
    }

    /**
     * 创建服务不可用异常
     */
    public static ModelException serviceUnavailable(String message) {
        return new ModelException(message, "SERVICE_ERROR", 503);
    }
}
