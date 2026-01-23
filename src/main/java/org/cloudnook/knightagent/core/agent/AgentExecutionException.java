package org.cloudnook.knightagent.core.agent;

/**
 * Agent 执行异常
 * <p>
 * 当 Agent 执行过程中发生错误时抛出。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class AgentExecutionException extends Exception {

    /**
     * 错误代码
     */
    private final String errorCode;

    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public AgentExecutionException(String message) {
        this(message, null);
    }

    /**
     * 构造函数（带原因）
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public AgentExecutionException(String message, Throwable cause) {
        this(message, cause, deriveErrorCode(cause));
    }

    /**
     * 完整构造函数
     *
     * @param message   错误信息
     * @param cause     原始异常
     * @param errorCode 错误代码
     */
    public AgentExecutionException(String message, Throwable cause, String errorCode) {
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
            return "AGENT_ERROR";
        }

        String className = cause.getClass().getSimpleName();
        return switch (className) {
            case "TimeoutException" -> "TIMEOUT";
            case "InterruptedException" -> "INTERRUPTED";
            case "ToolExecutionException" -> "TOOL_ERROR";
            case "ModelException" -> "MODEL_ERROR";
            case "CheckpointException" -> "CHECKPOINT_ERROR";
            default -> "AGENT_ERROR";
        };
    }

    /**
     * 检查是否可重试
     */
    public boolean isRetryable() {
        return "TIMEOUT".equals(errorCode) ||
                "MODEL_ERROR".equals(errorCode);
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建超时异常
     */
    public static AgentExecutionException timeout(String message) {
        return new AgentExecutionException(message, null, "TIMEOUT");
    }

    /**
     * 创建最大迭代次数异常
     */
    public static AgentExecutionException maxIterationsExceeded(int maxIterations) {
        return new AgentExecutionException(
                "超过最大迭代次数: " + maxIterations,
                null,
                "MAX_ITERATIONS_EXCEEDED"
        );
    }

    /**
     * 创建工具执行异常
     */
    public static AgentExecutionException toolError(String message, Throwable cause) {
        return new AgentExecutionException(message, cause, "TOOL_ERROR");
    }

    /**
     * 创建模型调用异常
     */
    public static AgentExecutionException modelError(String message, Throwable cause) {
        return new AgentExecutionException(message, cause, "MODEL_ERROR");
    }

    /**
     * 创建检查点异常
     */
    public static AgentExecutionException checkpointError(String message, Throwable cause) {
        return new AgentExecutionException(message, cause, "CHECKPOINT_ERROR");
    }
}
