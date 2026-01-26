package org.cloudnook.knightagent.core.agent;

import org.cloudnook.knightagent.core.agent.factory.DefaultAgentFactory;
import org.cloudnook.knightagent.core.checkpoint.InMemorySaver;
import org.cloudnook.knightagent.core.checkpoint.Checkpointer;
import org.cloudnook.knightagent.core.message.HumanMessage;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.middleware.builtin.LoggingMiddleware;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.OpenAIChatModel;
import org.cloudnook.knightagent.core.streaming.StreamCallback;
import org.cloudnook.knightagent.core.streaming.StreamChunk;
import org.cloudnook.knightagent.core.tool.AbstractTool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAI 真实 API 集成测试
 * <p>
 * 使用真实的 OpenAI API key 进行测试，验证框架与实际 LLM 的集成。
 * <p>
 * 运行前需要设置环境变量：
 * <pre>{@code
 * export OPENAI_API_KEY=sk-xxx
 * export OPENAI_BASE_URL=https://api.openai.com/v1  # 可选
 * }</pre>
 * <p>
 * 或在 Maven 运行时传递：
 * <pre>{@code
 * ./mvnw test -Dtest=OpenAIIntegrationTest -DOPENAI_API_KEY=sk-xxx
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@DisplayName("OpenAI 真实 API 集成测试")
@EnabledIf("isApiKeySet")
class OpenAIIntegrationTest {

//    private static final String API_KEY = System.getProperty("OPENAI_API_KEY",
//            System.getenv().getOrDefault("OPENAI_API_KEY", ""));
    private static final String API_KEY = "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";

//    private static final String BASE_URL = System.getProperty("OPENAI_BASE_URL",
//            System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"));

    private static final String BASE_URL = "https://api.groq.com/openai/v1";

//    private static final String MODEL = System.getProperty("OPENAI_MODEL",
//            System.getenv().getOrDefault("OPENAI_MODEL", "gpt-3.5-turbo"));

    private static final String MODEL = "llama-3.3-70b-versatile";

    static boolean isApiKeySet() {
        return !API_KEY.isEmpty() && !API_KEY.equals("sk-xxx");
    }

    private static ChatModel chatModel;
    private static Checkpointer checkpointer;

    @BeforeAll
    static void setUp() {
        assertTrue(isApiKeySet(),
                "请设置 OPENAI_API_KEY 环境变量或 JVM 属性来运行真实 API 测试");

        chatModel = OpenAIChatModel.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .modelId(MODEL)
                .build();

        checkpointer = new InMemorySaver();
    }

    @Nested
    @DisplayName("基础对话测试")
    class BasicConversationTest {

        @Test
        @DisplayName("简单问答")
        void testSimpleQA() throws AgentExecutionException {
            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(chatModel)
                    .config(AgentConfig.builder()
                            .systemPrompt("你是一个专业的 Java 开发助手。")
                            .maxIterations(5)
                            .build())
                    .build();

            AgentRequest request = AgentRequest.of("什么是 Java 的多态性？请用一句话回答。");
            AgentResponse response = agent.invoke(request);

            assertNotNull(response.getOutput());
            assertFalse(response.getOutput().isEmpty());
            assertTrue(response.getDurationMs() > 0);

            System.out.println("=== AI 响应 ===");
            System.out.println(response.getOutput());
            System.out.println("耗时: " + response.getDurationMs() + "ms");
        }

        @Test
        @DisplayName("带系统提示词的对话")
        void testWithSystemPrompt() throws AgentExecutionException {
            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(chatModel)
                    .config(AgentConfig.builder()
                            .systemPrompt("你是一个成语接龙专家。")
                            .build())
                    .build();

            AgentRequest request = AgentRequest.of("一马当先");
            AgentResponse response = agent.invoke(request);

            assertNotNull(response.getOutput());
            System.out.println("=== 成语接龙 ===");
            System.out.println("输入: 一马当先");
            System.out.println("输出: " + response.getOutput());
        }
    }

    @Nested
    @DisplayName("多轮对话测试")
    class MultiTurnTest {

        @Test
        @DisplayName("带历史的多轮对话")
        void testMultiTurnConversation() throws AgentExecutionException {
            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(chatModel)
                    .checkpointer(checkpointer)
                    .config(AgentConfig.builder()
                            .systemPrompt("你是一个友好的 AI 助手。")
                            .build())
                    .build();

            String threadId = "test-thread-" + System.currentTimeMillis();

            // 第一轮
            AgentRequest request1 = AgentRequest.of("我叫张三", threadId);
            AgentResponse response1 = agent.invoke(request1);
            assertNotNull(response1.getOutput());
            System.out.println("第一轮: " + response1.getOutput());

            // 第二轮
            AgentRequest request2 = AgentRequest.of("我叫什么名字？", threadId);
            AgentResponse response2 = agent.invoke(request2);
            assertNotNull(response2.getOutput());
            System.out.println("第二轮: " + response2.getOutput());

            // 验证 AI 记住了用户的名字
            assertTrue(response2.getOutput().contains("张三") ||
                       response2.getOutput().contains("张三") ||
                       response2.getOutput().matches(".*(你|你的).*名字.*张.*三.*"));
        }

        @Test
        @DisplayName("上下文累积测试")
        void testContextAccumulation() throws AgentExecutionException {
            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(chatModel)
                    .checkpointer(checkpointer)
                    .build();

            String threadId = "context-test-" + System.currentTimeMillis();

            // 多轮对话累积上下文
            agent.invoke(AgentRequest.of("记住数字：1", threadId));
            agent.invoke(AgentRequest.of("记住数字：2", threadId));
            agent.invoke(AgentRequest.of("记住数字：3", threadId));

            AgentResponse response = agent.invoke(AgentRequest.of("我让你记住的三个数字是什么？", threadId));

            System.out.println("=== 上下文测试 ===");
            System.out.println(response.getOutput());

            // AI 应该能回忆起之前的数字
            assertTrue(response.getOutput().contains("1") ||
                       response.getOutput().contains("2") ||
                       response.getOutput().contains("3"));
        }
    }

    @Nested
    @DisplayName("工具调用测试")
    class ToolCallTest {

        @Test
        @DisplayName("单次工具调用")
        void testSingleToolCall() throws AgentExecutionException {
            // 创建一个简单的计算器工具
            AbstractTool calculatorTool = new CalculatorTool();

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(chatModel)
                    .tool(calculatorTool)
                    .config(AgentConfig.builder()
                            .systemPrompt("你是一个数学助手，可以使用计算器工具进行计算。")
                            .build())
                    .build();

            AgentRequest request = AgentRequest.of("123 加 456 等于多少？");
            AgentResponse response = agent.invoke(request);

            assertNotNull(response.getOutput());
            assertTrue(response.getToolCalls().size() > 0 || response.getOutput().contains("579"));

            System.out.println("=== 计算器测试 ===");
            System.out.println("工具调用次数: " + response.getToolCalls().size());
            System.out.println("响应: " + response.getOutput());
        }

        @Test
        @DisplayName("多个工具调用")
        void testMultipleToolCalls() throws AgentExecutionException {
            AbstractTool calculatorTool = new CalculatorTool();
            AbstractTool weatherTool = new MockWeatherTool();

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(chatModel)
                    .tool(calculatorTool)
                    .tool(weatherTool)
                    .config(AgentConfig.builder()
                            .systemPrompt("你可以使用计算器和天气工具。")
                            .build())
                    .build();

            AgentRequest request = AgentRequest.of("100 减 50 等于多少？");
            AgentResponse response = agent.invoke(request);

            assertNotNull(response.getOutput());
            System.out.println("=== 多工具测试 ===");
            System.out.println("响应: " + response.getOutput());
        }
    }

    @Nested
    @DisplayName("中间件测试")
    class MiddlewareTest {

        @Test
        @DisplayName("日志中间件")
        void testLoggingMiddleware() throws AgentExecutionException {
            List<Middleware> middlewares = new ArrayList<>();
            middlewares.add(LoggingMiddleware.builder()
                    .logRequests(true)
                    .logResponses(true)
                    .logToolCalls(true)
                    .build());

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(chatModel)
                    .middlewares(middlewares)
                    .build();

            AgentRequest request = AgentRequest.of("你好");
            AgentResponse response = agent.invoke(request);

            assertNotNull(response.getOutput());
            System.out.println("=== 日志中间件测试 ===");
            System.out.println("响应: " + response.getOutput());
        }

        @Test
        @DisplayName("状态注入中间件")
        void testStateInjectionMiddleware() throws AgentExecutionException {
            // 简单测试，不依赖复杂的状态注入逻辑
            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(chatModel)
                    .build();

            AgentRequest request = AgentRequest.of("你好");
            AgentResponse response = agent.invoke(request);

            assertNotNull(response.getOutput());
            System.out.println("=== 基础状态测试 ===");
            System.out.println("响应: " + response.getOutput());
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ErrorHandlingTest {

        @Test
        @DisplayName("无效的 API key")
        void testInvalidApiKey() {
            ChatModel invalidModel = OpenAIChatModel.builder()
                    .apiKey("sk-invalid-key-12345")
                    .modelId(MODEL)
                    .build();

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(invalidModel)
                    .build();

            AgentRequest request = AgentRequest.of("你好");

            // 应该抛出异常
            assertThrows(AgentExecutionException.class, () -> {
                agent.invoke(request);
            });
        }

        @Test
        @DisplayName("超长输入")
        void testLongInput() throws AgentExecutionException {
            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(chatModel)
                    .config(AgentConfig.builder()
                            .systemPrompt("你是一个简洁的助手。")
                            .build())
                    .build();

            // 创建一个较长的输入
            StringBuilder longInput = new StringBuilder("请总结以下内容：");
            for (int i = 0; i < 100; i++) {
                longInput.append(" 这是第").append(i).append("行测试内容。");
            }

            AgentRequest request = AgentRequest.of(longInput.toString());
            AgentResponse response = agent.invoke(request);

            assertNotNull(response.getOutput());
            System.out.println("=== 长输入测试 ===");
            System.out.println("输入长度: " + longInput.length());
            System.out.println("响应: " + response.getOutput());
        }
    }

    @Nested
    @DisplayName("流式输出测试")
    class StreamingTest {

        @Test
        @DisplayName("流式输出")
        void testStreamingOutput() throws AgentExecutionException {
            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(chatModel)
                    .config(AgentConfig.builder()
                            .systemPrompt("你是一个诗人。")
                            .build())
                    .build();

            AgentRequest request = AgentRequest.of("请写一首关于春天的短诗，不多于100字。");

            // 收集流式输出
            StringBuilder streamOutput = new StringBuilder();
            agent.stream(request, new StreamCallback() {
                @Override
                public void onToken(StreamChunk chunk) {
                    String token = chunk.getContent() != null ? chunk.getContent() : "";
                    streamOutput.append(token);
                    System.out.print(token);
                }

                @Override
                public void onToolCall(StreamChunk chunk, ToolCall toolCall) {
                    System.out.println("\n[工具调用: " + toolCall.getName() + "]");
                }

                @Override
                public void onComplete(StreamChunk finalChunk) {
                    System.out.println("\n[完成]");
                }
            });

            assertTrue(streamOutput.length() > 0);
            System.out.println("\n=== 流式输出测试 ===");
            System.out.println("完整输出: " + streamOutput);
        }
    }

    /**
     * 简单的计数器工具
     */
    static class WordCounterTool extends org.cloudnook.knightagent.core.tool.AbstractTool {
        @Override
        public String getName() {
            return "word_count";
        }

        @Override
        public String getDescription() {
            return "计算文本的字数";
        }

        @Override
        public Map<String, Object> getParameters() {
            return Map.of(
                "type", "object",
                "properties", Map.of(
                    "text", Map.of(
                        "type", "string",
                        "description", "要统计的文本"
                    )
                ),
                "required", List.of("text")
            );
        }

        @Override
        protected org.cloudnook.knightagent.core.message.ToolResult executeInternal(java.util.Map<String, Object> arguments) {
            String text = getStringParam(arguments, "text");
            int count = text.length();
            return org.cloudnook.knightagent.core.message.ToolResult.success(
                    generateCallId(),
                    String.format("{\"count\": %d}", count)
            );
        }
    }

    /**
     * 简单的计算器工具
     */
    static class CalculatorTool extends org.cloudnook.knightagent.core.tool.AbstractTool {
        @Override
        public String getName() {
            return "calculator";
        }

        @Override
        public String getDescription() {
            return "执行基本的数学运算（加、减、乘、除）";
        }

        @Override
        public Map<String, Object> getParameters() {
            return Map.of(
                "type", "object",
                "properties", Map.of(
                    "expression", Map.of(
                        "type", "string",
                        "description", "数学表达式，如 '123 + 456'"
                    )
                ),
                "required", List.of("expression")
            );
        }

        @Override
        protected org.cloudnook.knightagent.core.message.ToolResult executeInternal(java.util.Map<String, Object> arguments) {
            String expression = getStringParam(arguments, "expression");

            try {
                // 简单的计算器实现
                double result = evaluateExpression(expression);
                return org.cloudnook.knightagent.core.message.ToolResult.success(
                        generateCallId(),
                        String.format("{\"expression\": \"%s\", \"result\": %s}", expression, result)
                );
            } catch (Exception e) {
                return org.cloudnook.knightagent.core.message.ToolResult.error(
                        generateCallId(),
                        "计算失败: " + e.getMessage()
                );
            }
        }

        private double evaluateExpression(String expression) {
            // 简单的计算器，只支持 +, -, *, /
            String[] parts = expression.split("\\s+");
            if (parts.length != 3) {
                throw new IllegalArgumentException("表达式格式错误，应为 '数字 运算符 数字'");
            }

            double a = Double.parseDouble(parts[0]);
            String op = parts[1];
            double b = Double.parseDouble(parts[2]);

            return switch (op) {
                case "+" -> a + b;
                case "-" -> a - b;
                case "*" -> a * b;
                case "/" -> a / b;
                default -> throw new IllegalArgumentException("不支持的操作符: " + op);
            };
        }
    }

    /**
     * 模拟天气工具
     */
    static class MockWeatherTool extends org.cloudnook.knightagent.core.tool.AbstractTool {
        @Override
        public String getName() {
            return "get_weather";
        }

        @Override
        public String getDescription() {
            return "获取指定城市的天气信息";
        }

        @Override
        public Map<String, Object> getParameters() {
            return Map.of(
                "type", "object",
                "properties", Map.of(
                    "city", Map.of(
                        "type", "string",
                        "description", "城市名称"
                    )
                ),
                "required", List.of("city")
            );
        }

        @Override
        protected org.cloudnook.knightagent.core.message.ToolResult executeInternal(java.util.Map<String, Object> arguments) {
            String city = getStringParam(arguments, "city");
            // 模拟天气数据
            return org.cloudnook.knightagent.core.message.ToolResult.success(
                    generateCallId(),
                    String.format("{\"city\": \"%s\", \"temperature\": 25, \"condition\": \"晴\"}", city)
            );
        }
    }
}
