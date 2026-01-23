package org.cloudnook.knightagent.core.tool;

/**
 * 工具执行异常
 * <p>
 * 当工具执行过程中发生错误时抛出此异常。
 * 异常信息会被传递回 LLM，帮助 LLM 理解错误原因并决定后续操作。
 * <p>
 * 使用示例：
 * <pre>{@code
 * public class DatabaseTool extends AbstractTool {
 *
 *     @Override
 *     protected ToolResult executeInternal(Map<String, Object> arguments) throws Exception {
 *         try {
 *             return queryDatabase(arguments);
 *         } catch (SQLException e) {
 *             throw new ToolExecutionException("数据库查询失败: " + e.getMessage(), e);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class ToolExecutionException extends Exception {

    /**
     * 错误代码
     * <p>
     * 可用于程序化地识别错误类型。
     */
    private final String errorCode;

    /**
     * 是否可重试
     * <p>
     * 标记此错误是否可以通过重试来恢复。
     */
    private final boolean retryable;

    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public ToolExecutionException(String message) {
        this(message, null);
    }

    /**
     * 构造函数（带原因）
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public ToolExecutionException(String message, Throwable cause) {
        this(message, cause, false);
    }

    /**
     * 完整构造函数
     *
     * @param message   错误信息
     * @param cause     原始异常
     * @param retryable 是否可重试
     */
    public ToolExecutionException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.errorCode = deriveErrorCode(cause);
        this.retryable = retryable;
    }

    /**
     * 构造函数（带错误代码）
     *
     * @param message   错误信息
     * @param errorCode 错误代码
     * @param retryable 是否可重试
     */
    public ToolExecutionException(String message, String errorCode, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
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
     * 是否可重试
     *
     * @return 如果可重试返回 true
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * 从异常推导错误代码
     *
     * @param cause 原始异常
     * @return 错误代码
     */
    private String deriveErrorCode(Throwable cause) {
        if (cause == null) {
            return "UNKNOWN_ERROR";
        }

        String className = cause.getClass().getSimpleName();
        // 常见异常映射到错误代码
        return switch (className) {
            case "SocketTimeoutException", "TimeoutException" -> "TIMEOUT";
            case "ConnectException" -> "CONNECTION_FAILED";
            case "UnknownHostException" -> "HOST_NOT_FOUND";
            case "SSLHandshakeException" -> "SSL_ERROR";
            case "JsonProcessingException", "JsonParseException" -> "INVALID_JSON";
            case "IllegalArgumentException" -> "INVALID_ARGUMENT";
            default -> "EXECUTION_ERROR";
        };
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建参数错误异常
     *
     * @param message 错误信息
     * @return 工具执行异常
     */
    public static ToolExecutionException invalidArgument(String message) {
        return new ToolExecutionException(message, "INVALID_ARGUMENT", false);
    }

    /**
     * 创建超时异常
     *
     * @param message 错误信息
     * @return 工具执行异常
     */
    public static ToolExecutionException timeout(String message) {
        return new ToolExecutionException(message, "TIMEOUT", true);
    }

    /**
     * 创建连接失败异常
     *
     * @param message 错误信息
     * @return 工具执行异常
     */
    public static ToolExecutionException connectionFailed(String message) {
        return new ToolExecutionException(message, "CONNECTION_FAILED", true);
    }

    /**
     * 创建认证失败异常
     *
     * @param message 错误信息
     * @return 工具执行异常
     */
    public static ToolExecutionException authFailed(String message) {
        return new ToolExecutionException(message, "AUTH_FAILED", false);
    }

    /**
     * 创建未找到异常
     *
     * @param message 错误信息
     * @return 工具执行异常
     */
    public static ToolExecutionException notFound(String message) {
        return new ToolExecutionException(message, "NOT_FOUND", false);
    }
}
