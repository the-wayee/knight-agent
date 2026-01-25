package org.cloudnook.knightagent.core.mcp;

/**
 * MCP 异常
 * <p>
 * 表示 MCP 操作过程中的错误。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class McpException extends Exception {

    /**
     * 错误代码
     */
    private final McpErrorCode errorCode;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public McpException(String message) {
        this(McpErrorCode.UNKNOWN, message, null);
    }

    /**
     * 构造函数（带原因）
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public McpException(String message, Throwable cause) {
        this(McpErrorCode.UNKNOWN, message, cause);
    }

    /**
     * 完整构造函数
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     原因
     */
    public McpException(McpErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : McpErrorCode.UNKNOWN;
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码
     */
    public McpErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * MCP 错误代码枚举
     */
    public enum McpErrorCode {
        /**
         * 未知错误
         */
        UNKNOWN,

        /**
         * 连接失败
         */
        CONNECTION_FAILED,

        /**
         * 初始化失败
         */
        INITIALIZATION_FAILED,

        /**
         * 协议不支持
         */
        UNSUPPORTED_PROTOCOL,

        /**
         * 请求超时
         */
        TIMEOUT,

        /**
         * 工具不存在
         */
        TOOL_NOT_FOUND,

        /**
         * 工具执行失败
         */
        TOOL_EXECUTION_FAILED,

        /**
         * 资源不存在
         */
        RESOURCE_NOT_FOUND,

        /**
         * 资源读取失败
         */
        RESOURCE_READ_FAILED,

        /**
         * 无效配置
         */
        INVALID_CONFIG,

        /**
         * 解析错误
         */
        PARSE_ERROR,

        /**
         * 服务器错误
         */
        SERVER_ERROR
    }
}
