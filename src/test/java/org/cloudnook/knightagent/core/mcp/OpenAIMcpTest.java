package org.cloudnook.knightagent.core.mcp;

import org.cloudnook.knightagent.core.agent.Agent;
import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.agent.AgentExecutionException;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.factory.DefaultAgentFactory;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.OpenAIChatModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 集成测试（真实 API）
 * <p>
 * 使用真实的 LLM API 和 MCP 服务器测试 MCP 集成功能。
 * <p>
 * 前置条件：
 * 1. MCP 服务器运行在 localhost:8000/mcp
 * 2. 使用 FastMCP 实现的 Demo 服务器
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@DisplayName("MCP 集成测试（真实 API）")
class OpenAIMcpTest {

    // 使用 Groq API（兼容 OpenAI 格式）
    private static final String API_KEY = "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";
    private static final String BASE_URL = "https://api.groq.com/openai/v1";
    private static final String MODEL = "llama-3.3-70b-versatile";

    // MCP 服务器地址
    private static final String MCP_SERVER_URL = "http://localhost:8000/mcp";

    private static ChatModel chatModel;

    @BeforeAll
    static void setUp() {
        chatModel = OpenAIChatModel.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .modelId(MODEL)
                .build();
    }

    @Test
    @DisplayName("测试 MCP 工具发现")
    void testMcpToolDiscovery() throws Exception {
        McpConfig mcpConfig = McpConfig.builder()
                .protocol(McpProtocol.STREAMABLE_HTTP)
                .entrypoint(MCP_SERVER_URL)
                .timeout(Duration.ofSeconds(30))
                .build();

        McpToolRegistryWrapper registry = new McpToolRegistryWrapper(mcpConfig);
        registry.initialize();

        System.out.println("=== MCP 工具发现测试 ===");
        System.out.println("协议: " + mcpConfig.getProtocol());
        System.out.println("入口点: " + mcpConfig.getEntrypoint());
        System.out.println("已注册工具数: " + registry.getToolCount());

        // 打印所有发现的工具
        registry.getRegisteredTools().forEach(tool -> {
            System.out.println("  - " + tool.getName() + ": " + tool.getDescription());
        });

        // 在关闭前验证
        int toolCount = registry.getToolCount();

        registry.close();

        assertTrue(toolCount > 0, "应该发现至少一个工具");
    }

    @Test
    @DisplayName("测试 MCP 资源发现")
    void testMcpResourceDiscovery() throws Exception {
        McpConfig mcpConfig = McpConfig.builder()
                .protocol(McpProtocol.STREAMABLE_HTTP)
                .entrypoint(MCP_SERVER_URL)
                .timeout(Duration.ofSeconds(30))
                .autoDiscoverTools(false)
                .autoDiscoverResources(true)
                .autoDiscoverPrompts(false)
                .build();

        McpToolRegistryWrapper registry = new McpToolRegistryWrapper(mcpConfig);
        registry.initialize();

        System.out.println("=== MCP 资源发现测试 ===");
        System.out.println("已连接到 MCP 服务器");

        // 尝试读取 greeting://theway 资源
        try {
            String greeting = registry.readResource("greeting://theway");
            System.out.println("资源内容: " + greeting);
        } catch (Exception e) {
            System.out.println("读取资源失败: " + e.getMessage());
        }

        registry.close();
    }

    @Test
    @DisplayName("测试 MCP 提示词发现")
    void testMcpPromptDiscovery() throws Exception {
        McpConfig mcpConfig = McpConfig.builder()
                .protocol(McpProtocol.STREAMABLE_HTTP)
                .entrypoint(MCP_SERVER_URL)
                .timeout(Duration.ofSeconds(30))
                .autoDiscoverTools(false)
                .autoDiscoverResources(false)
                .autoDiscoverPrompts(true)
                .build();

        McpToolRegistryWrapper registry = new McpToolRegistryWrapper(mcpConfig);
        registry.initialize();

        System.out.println("=== MCP 提示词发现测试 ===");
        System.out.println("已连接到 MCP 服务器");

        registry.close();
    }

    @Test
    @DisplayName("测试 Agent 使用 MCP 工具 - 加法运算")
    void testAgentWithMcpAddTool() throws AgentExecutionException {
        System.out.println("=== Agent + MCP 加法工具测试 ===\n");

        // 创建 Agent，配置 MCP
        Agent agent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .mcp(McpConfig.builder()
                        .protocol(McpProtocol.STREAMABLE_HTTP)
                        .entrypoint(MCP_SERVER_URL)
                        .timeout(Duration.ofSeconds(30))
                        .build())
                .config(AgentConfig.builder()
                        .systemPrompt(
                                "你是一个数学计算助手。" +
                                "当用户要求计算时，使用 add 工具进行加法运算。" +
                                "直接给出计算结果，不需要过多解释。"
                        )
                        .maxIterations(5)
                        .build())
                .build();

        // 测试加法
        AgentRequest request = AgentRequest.of("125 + 287 等于多少？");

        long startTime = System.currentTimeMillis();
        AgentResponse response = agent.invoke(request);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("耗时: " + duration + "ms");
        System.out.println("响应:\n" + response.getOutput());
        System.out.println();

        assertNotNull(response);
        assertNotNull(response.getOutput());
        assertFalse(response.getOutput().isEmpty());

        // 验证响应中包含正确的结果（412）
        assertTrue(response.getOutput().contains("412") ||
                response.getOutput().contains("412"),
                "响应应该包含计算结果 412");
    }

    @Test
    @DisplayName("测试 Agent 使用 MCP 工具 - 多次计算")
    void testAgentWithMcpMultipleCalculations() throws AgentExecutionException {
        System.out.println("=== Agent + MCP 多次计算测试 ===\n");

        Agent agent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .mcp(McpConfig.builder()
                        .protocol(McpProtocol.STREAMABLE_HTTP)
                        .entrypoint(MCP_SERVER_URL)
                        .timeout(Duration.ofSeconds(30))
                        .build())
                .config(AgentConfig.builder()
                        .systemPrompt(
                                "你是一个数学计算助手。" +
                                "使用 add 工具进行所有加法运算。" +
                                "简洁地给出结果。"
                        )
                        .maxIterations(10)
                        .build())
                .build();

        // 请求多个计算
        AgentRequest request = AgentRequest.of(
                "帮我计算以下结果：" +
                "1) 100 + 200，" +
                "2) 1500 + 350，" +
                "3) 77 + 33"
        );

        long startTime = System.currentTimeMillis();
        AgentResponse response = agent.invoke(request);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("耗时: " + duration + "ms");
        System.out.println("响应:\n" + response.getOutput());
        System.out.println();

        assertNotNull(response);
        assertTrue(response.getOutput().contains("300") ||
                response.getOutput().contains("1850") ||
                response.getOutput().contains("110"),
                "响应应包含计算结果");
    }

    @Test
    @DisplayName("测试 MCP 连接错误处理")
    void testMcpConnectionError() {
        System.out.println("=== MCP 连接错误处理测试 ===\n");

        // 使用错误的地址
        McpConfig mcpConfig = McpConfig.builder()
                .protocol(McpProtocol.STREAMABLE_HTTP)
                .entrypoint("http://localhost:9999/invalid")
                .timeout(Duration.ofSeconds(5))
                .build();

        McpToolRegistryWrapper registry = new McpToolRegistryWrapper(mcpConfig);

        // 应该抛出异常或处理错误
        assertThrows(Exception.class, () -> {
            registry.initialize();
        }, "连接到不存在的服务器应该抛出异常");
    }

    @Test
    @DisplayName("测试 Agent 使用 MCP - 复杂任务")
    void testAgentWithMcpComplexTask() throws AgentExecutionException {
        System.out.println("=== Agent + MCP 复杂任务测试 ===\n");

        Agent agent = new DefaultAgentFactory().createAgent()
                .model(chatModel)
                .mcp(McpConfig.builder()
                        .protocol(McpProtocol.STREAMABLE_HTTP)
                        .entrypoint(MCP_SERVER_URL)
                        .timeout(Duration.ofSeconds(30))
                        .build())
                .config(AgentConfig.builder()
                        .systemPrompt(
                                "你是一个智能助手，可以使用 add 工具进行计算。" +
                                "请仔细理解用户的需求，使用工具完成任务。" +
                                "给出清晰、简洁的答案。"
                        )
                        .maxIterations(8)
                        .build())
                .build();

        AgentRequest request = AgentRequest.of(
                "我需要计算购物车总价：" +
                "商品A 价格 299 元，商品B 价格 499 元，" +
                "另外还有运费 30 元。" +
                "请使用加法工具计算总价。"
        );

        long startTime = System.currentTimeMillis();
        AgentResponse response = agent.invoke(request);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("耗时: " + duration + "ms");
        System.out.println("响应:\n" + response.getOutput());
        System.out.println();

        assertNotNull(response);
        assertNotNull(response.getOutput());

        // 验证：299 + 499 + 30 = 828
        assertTrue(response.getOutput().contains("828"),
                "响应应包含计算结果 828");
    }
}
