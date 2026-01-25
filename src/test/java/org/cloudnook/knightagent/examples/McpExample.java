package org.cloudnook.knightagent.examples;

import org.cloudnook.knightagent.core.agent.Agent;
import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.agent.AgentExecutionException;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.factory.DefaultAgentFactory;
import org.cloudnook.knightagent.core.mcp.McpConfig;
import org.cloudnook.knightagent.core.mcp.McpProtocol;
import org.cloudnook.knightagent.core.model.ChatModel;

/**
 * MCP (Model Context Protocol) 使用示例
 * <p>
 * 演示如何在 KnightAgent 中集成和使用 MCP 工具。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class McpExample {

    /**
     * 示例 1：使用 STDIO 协议连接本地 MCP 服务器
     * <p>
     * 场景：使用官方 filesystem MCP 服务器访问本地文件
     */
    public static void example1_StdioProtocol() throws AgentExecutionException {
        System.out.println("=== 示例 1：STDIO 协议（文件系统服务器）===\n");

        // 假设已经有一个 ChatModel 实例
        ChatModel chatModel = createMockModel();

        // 创建 Agent，配置 MCP
        Agent agent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .mcp(McpConfig.builder()
                        .protocol(McpProtocol.STDIO)
                        .entrypoint("npx -y @modelcontextprotocol/server-filesystem /path/to/files")
                        .build())
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个文件助手，可以帮助用户读取和管理文件。")
                        .maxIterations(5)
                        .build())
                .build();

        // 使用 Agent
        AgentRequest request = AgentRequest.of(
                "请列出当前目录下的所有文件，并读取 README.md 的内容"
        );

        AgentResponse response = agent.invoke(request);

        System.out.println("响应:");
        System.out.println(response.getOutput());
        System.out.println();
    }

    /**
     * 示例 2：使用 SSE 协议连接远程 MCP 服务器
     * <p>
     * 场景：通过 HTTP SSE 连接到 MCP 服务器
     */
    public static void example2_SseProtocol() throws AgentExecutionException {
        System.out.println("=== 示例 2：SSE 协议（HTTP 服务器）===\n");

        ChatModel chatModel = createMockModel();

        // 创建 Agent，配置 MCP（SSE 协议）
        Agent agent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .mcp(McpConfig.builder()
                        .protocol(McpProtocol.SSE)
                        .entrypoint("http://localhost:3000/sse")
                        .timeout(java.time.Duration.ofSeconds(30))
                        .build())
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个数据分析助手。")
                        .build())
                .build();

        AgentRequest request = AgentRequest.of(
                "分析数据集并提供统计摘要"
        );

        AgentResponse response = agent.invoke(request);

        System.out.println("响应:");
        System.out.println(response.getOutput());
        System.out.println();
    }

    /**
     * 示例 3：使用 WebSocket 协议
     * <p>
     * 场景：通过 WebSocket 连接到 MCP 服务器
     */
    public static void example3_WebSocketProtocol() throws AgentExecutionException {
        System.out.println("=== 示例 3：WebSocket 协议 ===\n");

        ChatModel chatModel = createMockModel();

        // 创建 Agent，配置 MCP（WebSocket 协议）
        Agent agent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .mcp(McpConfig.builder()
                        .protocol(McpProtocol.WS)
                        .entrypoint("ws://localhost:3000/ws")
                        .headers(java.util.Map.of(
                                "Authorization", "Bearer your-token",
                                "X-Custom-Header", "custom-value"
                        ))
                        .build())
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个实时数据处理助手。")
                        .build())
                .build();

        AgentRequest request = AgentRequest.of(
                "获取最新的实时数据并分析"
        );

        AgentResponse response = agent.invoke(request);

        System.out.println("响应:");
        System.out.println(response.getOutput());
        System.out.println();
    }

    /**
     * 示例 4：连接多个 MCP 服务器
     * <p>
     * 场景：同时使用多个 MCP 服务的工具
     */
    public static void example4_MultipleMcpServers() throws AgentExecutionException {
        System.out.println("=== 示例 4：多个 MCP 服务器 ===\n");

        ChatModel chatModel = createMockModel();

        // 创建 Agent，配置多个 MCP 服务器
        Agent agent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                // 文件系统 MCP
                .mcp(McpConfig.builder()
                        .protocol(McpProtocol.STDIO)
                        .entrypoint("npx -y @modelcontextprotocol/server-filesystem /data")
                        .build())
                // 数据库 MCP
                .mcp(McpConfig.builder()
                        .protocol(McpProtocol.STDIO)
                        .entrypoint("npx -y @modelcontextprotocol/server-postgres postgres://user:pass@localhost/db")
                        .build())
                // 搜索 MCP
                .mcp(McpConfig.builder()
                        .protocol(McpProtocol.SSE)
                        .entrypoint("http://localhost:8080/mcp")
                        .build())
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个全能助手，可以访问文件系统、数据库和搜索引擎。")
                        .maxIterations(10)
                        .build())
                .build();

        AgentRequest request = AgentRequest.of(
                "从数据库查询用户数据，将结果保存到文件，并生成搜索索引"
        );

        AgentResponse response = agent.invoke(request);

        System.out.println("响应:");
        System.out.println(response.getOutput());
        System.out.println();
    }

    /**
     * 示例 5：MCP 配置详解
     * <p>
     * 展示所有 MCP 配置选项
     */
    public static void example5_AdvancedMcpConfig() throws AgentExecutionException {
        System.out.println("=== 示例 5：高级 MCP 配置 ===\n");

        ChatModel chatModel = createMockModel();

        // 创建 Agent，使用高级 MCP 配置
        Agent agent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .mcp(McpConfig.builder()
                        .protocol(McpProtocol.STDIO)
                        .entrypoint("npx -y @modelcontextprotocol/server-filesystem /data")

                        // 超时设置
                        .timeout(java.time.Duration.ofSeconds(60))

                        // 环境变量
                        .env(java.util.Map.of(
                                "NODE_ENV", "production",
                                "LOG_LEVEL", "debug"
                        ))

                        // 自动发现设置
                        .autoDiscoverTools(true)
                        .autoDiscoverResources(true)
                        .autoDiscoverPrompts(false)

                        // 重连设置
                        .maxReconnectAttempts(5)
                        .reconnectDelay(java.time.Duration.ofSeconds(2))

                        .build())
                .config(AgentConfig.builder()
                        .systemPrompt("你是一个高级数据处理助手。")
                        .build())
                .build();

        AgentRequest request = AgentRequest.of(
                "处理数据并生成报告"
        );

        AgentResponse response = agent.invoke(request);

        System.out.println("响应:");
        System.out.println(response.getOutput());
        System.out.println();
    }

    /**
     * 创建 Mock 模型（仅用于示例）
     */
    private static ChatModel createMockModel() {
        return new ChatModel() {
            @Override
            public org.cloudnook.knightagent.core.message.AIMessage chat(
                    java.util.List<org.cloudnook.knightagent.core.message.Message> messages,
                    org.cloudnook.knightagent.core.model.ChatOptions options) {
                return org.cloudnook.knightagent.core.message.AIMessage.of(
                        "这是一个模拟响应。实际使用时，请使用真实的 LLM 模型（如 OpenAIChatModel）。"
                );
            }

            @Override
            public void chatStream(
                    java.util.List<org.cloudnook.knightagent.core.message.Message> messages,
                    org.cloudnook.knightagent.core.model.ChatOptions options,
                    org.cloudnook.knightagent.core.streaming.StreamCallback callback) {
                callback.onToken("模拟");
                callback.onToken("响应");
                callback.onComplete();
            }

            @Override
            public int countTokens(String text) {
                return text.length();
            }

            @Override
            public org.cloudnook.knightagent.core.model.ModelCapabilities getCapabilities() {
                return org.cloudnook.knightagent.core.model.ModelCapabilities.builder().build();
            }

            @Override
            public String getModelId() {
                return "mock-model";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }
        };
    }

    /**
     * 主方法
     */
    public static void main(String[] args) throws AgentExecutionException {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           MCP (Model Context Protocol) 使用示例            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        // 注意：实际运行需要：
        // 1. 真实的 ChatModel 实现（如 OpenAIChatModel）
        // 2. 已安装并运行的 MCP 服务器
        // 3. 正确的配置参数

        System.out.println("注意：以下示例仅供演示，实际运行需要真实的环境配置。");
        System.out.println("请参考示例代码进行实际集成。\n");

        example1_StdioProtocol();
        example2_SseProtocol();
        example3_WebSocketProtocol();
        example4_MultipleMcpServers();
        example5_AdvancedMcpConfig();

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    所有示例运行完成                         ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
}
