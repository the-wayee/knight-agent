package org.cloudnook.knightagent.core.mcp;

/**
 * MCP 协议类型
 * <p>
 * 定义支持的 MCP 传输协议类型。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public enum McpProtocol {

    /**
     * 标准输入/输出协议
     * <p>
     * 通过子进程标准输入输出进行通信。
     * 适用于本地 MCP 服务器，如 npm 包或可执行文件。
     * <p>
     * 示例：
     * <pre>{@code
     * McpConfig.builder()
     *     .protocol(McpProtocol.STDIO)
     *     .entrypoint("npx -y @modelcontextprotocol/server-filesystem /path/to/files")
     *     .build()
     * }</pre>
     */
    STDIO("stdio", "通过标准输入输出通信"),

    /**
     * Streamable HTTP 协议
     * <p>
     * 通过 HTTP 流式传输进行通信。
     * 适用于 MCP 服务器的标准 HTTP 传输。
     * <p>
     * 示例：
     * <pre>{@code
     * McpConfig.builder()
     *     .protocol(McpProtocol.STREAMABLE_HTTP)
     *     .entrypoint("http://localhost:8000/mcp")
     *     .build()
     * }</pre>
     */
    STREAMABLE_HTTP("streamable-http", "通过流式 HTTP 通信"),

    /**
     * Server-Sent Events 协议
     * <p>
     * 通过 HTTP SSE 进行单向通信（服务器到客户端）。
     * 适用于需要实时推送的场景。
     * <p>
     * 示例：
     * <pre>{@code
     * McpConfig.builder()
     *     .protocol(McpProtocol.SSE)
     *     .entrypoint("http://localhost:3000/sse")
     *     .build()
     * }</pre>
     */
    SSE("sse", "通过 Server-Sent Events 通信"),

    /**
     * WebSocket 协议
     * <p>
     * 通过 WebSocket 进行双向通信。
     * 适用于需要实时双向交互的场景。
     * <p>
     * 示例：
     * <pre>{@code
     * McpConfig.builder()
     *     .protocol(McpProtocol.WS)
     *     .entrypoint("ws://localhost:3000/ws")
     *     .build()
     * }</pre>
     */
    WS("ws", "通过 WebSocket 通信");

    private final String code;
    private final String description;

    McpProtocol(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取协议类型
     *
     * @param code 协议代码
     * @return 协议枚举，如果不存在返回 null
     */
    public static McpProtocol fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (McpProtocol protocol : values()) {
            if (protocol.code.equalsIgnoreCase(code)) {
                return protocol;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return code;
    }
}
