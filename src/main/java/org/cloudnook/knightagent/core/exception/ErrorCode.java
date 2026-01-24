package org.cloudnook.knightagent.core.exception;

/**
 * 错误码枚举
 * <p>
 * 定义框架中所有可能的错误类型及其编码。
 * <p>
 * 错误码分类：
 * <ul>
 *   <li>1xxx - 模型相关错误</li>
 *   <li>2xxx - 工具相关错误</li>
 *   <li>3xxx - 检查点相关错误</li>
 *   <li>4xxx - 中间件相关错误</li>
 *   <li>5xxx - Agent 执行相关错误</li>
 * </ul>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public enum ErrorCode {

    // ==================== 模型错误 (1xxx) ====================
    /**
     * 通用模型错误
     */
    MODEL_ERROR(1000, "模型调用失败"),
    /**
     * 模型调用超时
     */
    MODEL_TIMEOUT(1001, "模型调用超时"),
    /**
     * 模型调用频率限制
     */
    MODEL_RATE_LIMIT(1002, "模型调用频率限制"),
    /**
     * 模型不可用
     */
    MODEL_UNAVAILABLE(1003, "模型不可用"),
    /**
     * 模型配额不足
     */
    MODEL_QUOTA_EXCEEDED(1004, "模型配额不足"),
    /**
     * 无效的模型 ID
     */
    INVALID_MODEL_ID(1005, "无效的模型 ID"),
    /**
     * 模型认证失败
     */
    MODEL_UNAUTHORIZED(1006, "模型认证失败"),

    // ==================== 工具错误 (2xxx) ====================
    /**
     * 通用工具错误
     */
    TOOL_ERROR(2000, "工具执行失败"),
    /**
     * 工具不存在
     */
    TOOL_NOT_FOUND(2001, "工具不存在"),
    /**
     * 工具执行超时
     */
    TOOL_TIMEOUT(2002, "工具执行超时"),
    /**
     * 工具参数无效
     */
    TOOL_INVALID_ARGUMENTS(2003, "工具参数无效"),
    /**
     * 工具执行被拒绝
     */
    TOOL_EXECUTION_DENIED(2004, "工具执行被拒绝"),

    // ==================== 检查点错误 (3xxx) ====================
    /**
     * 通用检查点错误
     */
    CHECKPOINT_ERROR(3000, "检查点操作失败"),
    /**
     * 检查点保存失败
     */
    CHECKPOINT_SAVE_FAILED(3001, "检查点保存失败"),
    /**
     * 检查点加载失败
     */
    CHECKPOINT_LOAD_FAILED(3002, "检查点加载失败"),
    /**
     * 检查点不存在
     */
    CHECKPOINT_NOT_FOUND(3003, "检查点不存在"),
    /**
     * 检查点反序列化失败
     */
    CHECKPOINT_DESERIALIZATION_FAILED(3004, "检查点反序列化失败"),

    // ==================== 中间件错误 (4xxx) ====================
    /**
     * 通用中间件错误
     */
    MIDDLEWARE_ERROR(4000, "中间件处理失败"),
    /**
     * 中间件执行失败
     */
    MIDDLEWARE_EXECUTION_FAILED(4001, "中间件执行失败"),
    /**
     * 中间件配置错误
     */
    MIDDLEWARE_CONFIG_ERROR(4002, "中间件配置错误"),

    // ==================== Agent 错误 (5xxx) ====================
    /**
     * 通用 Agent 错误
     */
    AGENT_ERROR(5000, "Agent 执行失败"),
    /**
     * Agent 执行超时
     */
    AGENT_TIMEOUT(5001, "Agent 执行超时"),
    /**
     * 超过最大迭代次数
     */
    AGENT_MAX_ITERATIONS_EXCEEDED(5002, "超过最大迭代次数"),
    /**
     * Agent 配置错误
     */
    AGENT_CONFIG_ERROR(5003, "Agent 配置错误"),
    /**
     * Agent 初始化失败
     */
    AGENT_INITIALIZATION_FAILED(5004, "Agent 初始化失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误码数字
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取错误消息
     *
     * @return 错误消息
     */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return name() + "(" + code + ": " + message + ")";
    }
}
