package org.cloudnook.knightagent.core.mcp;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * MCP 配置
 * <p>
 * 封装 MCP 服务器连接配置参数。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 使用 STDIO 协议（本地 npx 包）
 * McpConfig config = McpConfig.builder()
 *     .protocol(McpProtocol.STDIO)
 *     .entrypoint("npx -y @modelcontextprotocol/server-filesystem /path/to/files")
 *     .build();
 *
 * // 使用 SSE 协议（HTTP 服务器）
 * McpConfig config = McpConfig.builder()
 *     .protocol(McpProtocol.SSE)
 *     .entrypoint("http://localhost:3000/sse")
 *     .timeout(Duration.ofSeconds(30))
 *     .build();
 *
 * // 使用 WebSocket 协议
 * McpConfig config = McpConfig.builder()
 *     .protocol(McpProtocol.WS)
 *     .entrypoint("ws://localhost:3000/ws")
 *     .headers(Map.of("Authorization", "Bearer token123"))
 *     .build();
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
public class McpConfig {

    /**
     * MCP 协议类型
     * <p>
     * 决定客户端与服务器通信的方式。
     */
    private McpProtocol protocol;

    /**
     * 连接入口点
     * <p>
     * 根据 protocol 不同，entrypoint 的含义：
     * <ul>
     *   <li>STDIO: 启动 MCP 服务器的命令行（如 "npx @modelcontextprotocol/server-filesystem"）</li>
     *   <li>SSE: SSE 服务器的 URL（如 "http://localhost:3000/sse"）</li>
     *   <li>WS: WebSocket 服务器的 URL（如 "ws://localhost:3000/ws"）</li>
     * </ul>
     */
    private String entrypoint;

    /**
     * 连接超时时间
     * <p>
     * 建立 MCP 连接的最大等待时间。
     * 默认值：30 秒
     */
    @Builder.Default
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * 环境变量
     * <p>
     * 传递给 MCP 服务器的环境变量（主要适用于 STDIO 协议）。
     */
    private Map<String, String> env;

    /**
     * HTTP 请求头
     * <p>
     * 用于 SSE 和 WebSocket 协议的额外请求头。
     */
    private Map<String, String> headers;

    /**
     * 是否启用工具自动发现
     * <p>
     * 如果启用，会自动从 MCP 服务器获取可用工具列表并注册。
     * 默认值：true
     */
    @Builder.Default
    private boolean autoDiscoverTools = true;

    /**
     * 是否启用资源自动发现
     * <p>
     * 如果启用，会自动从 MCP 服务器获取可用资源列表。
     * 默认值：true
     */
    @Builder.Default
    private boolean autoDiscoverResources = true;

    /**
     * 是否启用提示词自动发现
     * <p>
     * 如果启用，会自动从 MCP 服务器获取可用提示词列表。
     * 默认值：true
     */
    @Builder.Default
    private boolean autoDiscoverPrompts = true;

    /**
     * 最大重连次数
     * <p>
     * 连接失败时的最大重试次数。
     * 默认值：3
     */
    @Builder.Default
    private int maxReconnectAttempts = 3;

    /**
     * 重连延迟
     * <p>
     * 重连之间的等待时间。
     * 默认值：1 秒
     */
    @Builder.Default
    private Duration reconnectDelay = Duration.ofSeconds(1);

    /**
     * 获取环境变量值
     *
     * @param key 环境变量名
     * @return 环境变量值的 Optional 包装
     */
    public Optional<String> getEnv(String key) {
        if (env == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(env.get(key));
    }

    /**
     * 获取请求头值
     *
     * @param key 请求头名
     * @return 请求头值的 Optional 包装
     */
    public Optional<String> getHeader(String key) {
        if (headers == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(headers.get(key));
    }

    /**
     * 添加环境变量
     *
     * @param key   环境变量名
     * @param value 环境变量值
     * @return 新的配置实例
     */
    public McpConfig withEnv(String key, String value) {
        Map<String, String> newEnv = env != null ? new HashMap<>(env) : new HashMap<>();
        newEnv.put(key, value);
        return this.toBuilder().env(newEnv).build();
    }

    /**
     * 添加请求头
     *
     * @param key   请求头名
     * @param value 请求头值
     * @return 新的配置实例
     */
    public McpConfig withHeader(String key, String value) {
        Map<String, String> newHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
        newHeaders.put(key, value);
        return this.toBuilder().headers(newHeaders).build();
    }

    /**
     * 验证配置
     *
     * @throws IllegalArgumentException 如果配置不合法
     */
    public void validate() {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol 不能为空");
        }
        if (entrypoint == null || entrypoint.isBlank()) {
            throw new IllegalArgumentException("entrypoint 不能为空");
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout 必须大于 0");
        }
        if (maxReconnectAttempts < 0) {
            throw new IllegalArgumentException("maxReconnectAttempts 不能小于 0");
        }
        if (reconnectDelay == null || reconnectDelay.isNegative()) {
            throw new IllegalArgumentException("reconnectDelay 不能为负数");
        }
    }

    /**
     * 创建默认配置
     *
     * @return 默认的 McpConfig（STDIO 协议）
     */
    public static McpConfig defaults() {
        return McpConfig.builder()
                .protocol(McpProtocol.STDIO)
                .timeout(Duration.ofSeconds(30))
                .autoDiscoverTools(true)
                .autoDiscoverResources(true)
                .autoDiscoverPrompts(true)
                .maxReconnectAttempts(3)
                .reconnectDelay(Duration.ofSeconds(1))
                .build();
    }
}
