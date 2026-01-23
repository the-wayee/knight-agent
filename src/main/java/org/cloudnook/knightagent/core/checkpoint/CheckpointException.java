package org.cloudnook.knightagent.core.checkpoint;

/**
 * 检查点异常
 * <p>
 * 当检查点操作（保存、加载、删除等）失败时抛出。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class CheckpointException extends Exception {

    /**
     * 错误代码
     */
    private final String errorCode;

    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public CheckpointException(String message) {
        this(message, null);
    }

    /**
     * 构造函数（带原因）
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public CheckpointException(String message, Throwable cause) {
        this(message, cause, deriveErrorCode(cause));
    }

    /**
     * 完整构造函数
     *
     * @param message   错误信息
     * @param cause     原始异常
     * @param errorCode 错误代码
     */
    public CheckpointException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 从异常推导错误代码
     */
    private static String deriveErrorCode(Throwable cause) {
        if (cause == null) {
            return "CHECKPOINT_ERROR";
        }

        String className = cause.getClass().getSimpleName();
        return switch (className) {
            case "IOException" -> "IO_ERROR";
            case "SerializationException" -> "SERIALIZATION_ERROR";
            case "TimeoutException" -> "TIMEOUT";
            case "InterruptedException" -> "INTERRUPTED";
            default -> "CHECKPOINT_ERROR";
        };
    }

    /**
     * 检查是否可重试
     */
    public boolean isRetryable() {
        return "IO_ERROR".equals(errorCode) ||
                "TIMEOUT".equals(errorCode);
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建序列化错误异常
     */
    public static CheckpointException serializationError(String message, Throwable cause) {
        return new CheckpointException(message, cause, "SERIALIZATION_ERROR");
    }

    /**
     * 创建 IO 错误异常
     */
    public static CheckpointException ioError(String message, Throwable cause) {
        return new CheckpointException(message, cause, "IO_ERROR");
    }

    /**
     * 创建未找到异常
     */
    public static CheckpointException notFound(String message) {
        return new CheckpointException(message, null, "NOT_FOUND");
    }

    /**
     * 创建超时异常
     */
    public static CheckpointException timeout(String message) {
        return new CheckpointException(message, null, "TIMEOUT");
    }
}
