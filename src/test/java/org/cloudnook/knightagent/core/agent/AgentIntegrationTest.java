package org.cloudnook.knightagent.core.agent;

import org.cloudnook.knightagent.core.agent.factory.DefaultAgentFactory;
import org.cloudnook.knightagent.core.checkpoint.CheckpointException;
import org.cloudnook.knightagent.core.checkpoint.Checkpointer;
import org.cloudnook.knightagent.core.checkpoint.CheckpointInfo;
import org.cloudnook.knightagent.core.checkpoint.InMemorySaver;
import org.cloudnook.knightagent.core.message.AIMessage;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.message.ToolResult;
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.middleware.builtin.HumanInTheLoopMiddleware;
import org.cloudnook.knightagent.core.middleware.builtin.LoggingMiddleware;
import org.cloudnook.knightagent.core.middleware.builtin.StateInjectionMiddleware;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.ChatOptions;
import org.cloudnook.knightagent.core.model.ModelCapabilities;
import org.cloudnook.knightagent.core.model.ModelException;
import org.cloudnook.knightagent.core.state.AgentState;
import org.cloudnook.knightagent.core.streaming.StreamCallback;
import org.cloudnook.knightagent.core.tool.MockWeatherTool;
import org.cloudnook.knightagent.core.tool.Tool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Agent 集成测试
 * <p>
 * 测试 Agent 的核心功能，包括：
 * <ul>
 *   <li>Agent 创建和配置</li>
 *   <li>同步调用</li>
 *   <li>工具调用</li>
 *   <li>中间件功能</li>
 *   <li>状态持久化</li>
 *   <li>多轮对话</li>
 * </ul>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@DisplayName("Agent 集成测试")
class AgentIntegrationTest {

    /**
     * 简单的 Mock ChatModel 用于测试
     */
    static class SimpleMockModel implements ChatModel {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final List<String> responses;

        SimpleMockModel(List<String> responses) {
            this.responses = responses;
        }

        SimpleMockModel() {
            this(List.of("默认响应"));
        }

        @Override
        public AIMessage chat(List<Message> messages, ChatOptions options) {
            int index = callCount.getAndIncrement();
            if (index < responses.size()) {
                return AIMessage.of(responses.get(index));
            }
            return AIMessage.of("响应 #" + (index + 1));
        }

        @Override
        public void chatStream(List<Message> messages, ChatOptions options, StreamCallback callback) {
            AIMessage response = chat(messages, options);
            callback.onToken(response.getContent());
            callback.onComplete();
        }

        @Override
        public int countTokens(String text) {
            return text.length();
        }

        @Override
        public ModelCapabilities getCapabilities() {
            return ModelCapabilities.builder().build();
        }

        @Override
        public String getModelId() {
            return "simple-mock";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        public int getCallCount() {
            return callCount.get();
        }
    }

    /**
     * 支持 MockToolCall 的 Mock Model
     */
    static class ToolCallMockModel implements ChatModel {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final List<MockResponse> responses;

        record MockResponse(String content, List<ToolCall> toolCalls) {
            static MockResponse of(String content) {
                return new MockResponse(content, List.of());
            }
            static MockResponse withToolCall(String content, String toolName, String args) {
                return new MockResponse(content, List.of(ToolCall.of(toolName, args)));
            }
        }

        ToolCallMockModel(List<MockResponse> responses) {
            this.responses = responses;
        }

        @Override
        public AIMessage chat(List<Message> messages, ChatOptions options) {
            int index = callCount.getAndIncrement();
            if (index < responses.size()) {
                MockResponse r = responses.get(index);
                return AIMessage.builder()
                        .content(r.content())
                        .toolCalls(r.toolCalls())
                        .build();
            }
            return AIMessage.of("默认响应");
        }

        @Override
        public void chatStream(List<Message> messages, ChatOptions options, StreamCallback callback) {
            AIMessage response = chat(messages, options);
            callback.onToken(response.getContent());
            callback.onComplete();
        }

        @Override
        public int countTokens(String text) {
            return text.length();
        }

        @Override
        public ModelCapabilities getCapabilities() {
            return ModelCapabilities.builder().build();
        }

        @Override
        public String getModelId() {
            return "tool-mock";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }

    private ChatModel mockModel;
    private Tool mockWeatherTool;
    private Checkpointer checkpointer;

    @BeforeEach
    void setUp() {
        mockModel = new SimpleMockModel();
        mockWeatherTool = new MockWeatherTool();
        checkpointer = new InMemorySaver();
    }

    @Nested
    @DisplayName("Agent 创建测试")
    class AgentCreationTest {

        @Test
        @DisplayName("使用 AgentBuilder 创建 Agent")
        void testCreateAgentWithBuilder() {
            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(mockModel)
                    .tool(mockWeatherTool)
                    .checkpointer(checkpointer)
                    .config(AgentConfig.defaults())
                    .build();

            assertNotNull(agent);
            assertNotNull(agent.getConfig());
        }

        @Test
        @DisplayName("创建不带工具的 Agent")
        void testCreateAgentWithoutTools() {
            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(mockModel)
                    .build();

            assertNotNull(agent);
        }

        @Test
        @DisplayName("Builder 缺少必填字段应抛出异常")
        void testBuilderThrowsExceptionWithoutModel() {
            assertThrows(IllegalStateException.class, () -> {
                new DefaultAgentFactory().createAgent()
                        .tool(mockWeatherTool)
                        .build();
            });
        }
    }

    @Nested
    @DisplayName("同步调用测试")
    class InvokeTest {

        @Test
        @DisplayName("简单文本调用")
        void testSimpleInvoke() throws AgentExecutionException {
            SimpleMockModel model = new SimpleMockModel(List.of("你好！我是 AI 助手。"));

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .build();

            AgentRequest request = AgentRequest.of("你好");
            AgentResponse response = agent.invoke(request);

            assertEquals("你好！我是 AI 助手。", response.getOutput());
            assertEquals(1, response.getMessages().size());
            assertEquals(1, model.getCallCount());
        }

        @Test
        @DisplayName("带 Thread ID 的调用")
        void testInvokeWithThreadId() throws AgentExecutionException {
            SimpleMockModel model = new SimpleMockModel(List.of("第一轮响应", "第二轮响应"));

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .checkpointer(checkpointer)
                    .build();

            // 第一轮
            AgentRequest request1 = AgentRequest.of("你好", "thread-123");
            AgentResponse response1 = agent.invoke(request1);
            assertEquals("第一轮响应", response1.getOutput());

            // 第二轮（同一 thread）
            AgentRequest request2 = AgentRequest.of("你好吗", "thread-123");
            AgentResponse response2 = agent.invoke(request2);
            assertEquals("第二轮响应", response2.getOutput());

            // 验证消息历史
            List<Message> messages = response2.getState().getMessages();
            assertTrue(messages.size() >= 4);
        }

        @Test
        @DisplayName("带自定义系统提示词的调用")
        void testInvokeWithSystemPrompt() throws AgentExecutionException {
            SimpleMockModel model = new SimpleMockModel(List.of("我是专业的 Java 开发助手"));

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .config(AgentConfig.builder()
                            .systemPrompt("你是一个专业的 Java 开发助手")
                            .build())
                    .build();

            AgentRequest request = AgentRequest.of("帮我写一段 Java 代码");
            AgentResponse response = agent.invoke(request);

            assertEquals("我是专业的 Java 开发助手", response.getOutput());
        }
    }

    @Nested
    @DisplayName("工具调用测试")
    class ToolCallTest {

        @Test
        @DisplayName("单次工具调用")
        void testSingleToolCall() throws AgentExecutionException {
            ToolCallMockModel model = new ToolCallMockModel(List.of(
                    ToolCallMockModel.MockResponse.withToolCall("我来帮你查询", "get_weather", "{\"city\": \"北京\"}"),
                    ToolCallMockModel.MockResponse.of("北京今天天气晴朗")
            ));

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .tool(mockWeatherTool)
                    .build();

            AgentRequest request = AgentRequest.of("今天北京天气怎么样？");
            AgentResponse response = agent.invoke(request);

            assertEquals("北京今天天气晴朗", response.getOutput());
            assertEquals(1, response.getToolCalls().size());
        }

        @Test
        @DisplayName("多个工具调用")
        void testMultipleToolCalls() throws AgentExecutionException {
            ToolCallMockModel model = new ToolCallMockModel(List.of(
                    ToolCallMockModel.MockResponse.withToolCall("查询天气", "get_weather", "{\"city\": \"北京\"}"),
                    ToolCallMockModel.MockResponse.withToolCall("继续", "get_weather", "{\"city\": \"上海\"}"),
                    ToolCallMockModel.MockResponse.of("查询完成")
            ));

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .tool(mockWeatherTool)
                    .build();

            AgentRequest request = AgentRequest.of("查询天气");
            AgentResponse response = agent.invoke(request);

            assertEquals(2, response.getToolCalls().size());
        }

        @Test
        @DisplayName("工具调用错误处理")
        void testToolCallError() throws AgentExecutionException {
            Tool errorTool = new Tool() {
                @Override
                public String getName() {
                    return "error_tool";
                }

                @Override
                public String getDescription() {
                    return "会报错的工具";
                }

                @Override
                public String getParametersSchema() {
                    return "{}";
                }

                @Override
                public ToolResult execute(String arguments) {
                    return ToolResult.error("error-id", "工具执行失败");
                }
            };

            ToolCallMockModel model = new ToolCallMockModel(List.of(
                    ToolCallMockModel.MockResponse.withToolCall("调用错误", "error_tool", "{}"),
                    ToolCallMockModel.MockResponse.of("抱歉，出错了")
            ));

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .tool(errorTool)
                    .build();

            AgentRequest request = AgentRequest.of("调用错误工具");
            AgentResponse response = agent.invoke(request);

            assertEquals("抱歉，出错了", response.getOutput());
        }
    }

    @Nested
    @DisplayName("中间件测试")
    class MiddlewareTest {

        @Test
        @DisplayName("日志中间件")
        void testLoggingMiddleware() throws AgentExecutionException {
            SimpleMockModel model = new SimpleMockModel(List.of("响应内容"));

            List<Middleware> middlewares = new ArrayList<>();
            middlewares.add(LoggingMiddleware.builder()
                    .logRequests(true)
                    .logResponses(true)
                    .logToolCalls(true)
                    .build());

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .middlewares(middlewares)
                    .build();

            AgentRequest request = AgentRequest.of("测试日志");
            AgentResponse response = agent.invoke(request);

            assertEquals("响应内容", response.getOutput());
        }

        @Test
        @DisplayName("状态注入中间件")
        void testStateInjectionMiddleware() throws AgentExecutionException {
            SimpleMockModel model = new SimpleMockModel(List.of("你好"));

            List<Middleware> middlewares = new ArrayList<>();
            middlewares.add(StateInjectionMiddleware.builder()
                    .injectionMode(StateInjectionMiddleware.InjectionMode.SUFFIX)
                    .template("用户：${state:userName}")
                    .build());

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .middlewares(middlewares)
                    .build();

            AgentRequest request = AgentRequest.of("你好");
            request.toBuilder()
                    .parameters(Map.of("userName", "张三"))
                    .build();

            AgentResponse response = agent.invoke(request);
            assertNotNull(response.getOutput());
        }

        @Test
        @DisplayName("人机协作中间件 - 白名单模式")
        void testHumanInTheLoopMiddlewareWhitelist() throws AgentExecutionException {
            ToolCallMockModel model = new ToolCallMockModel(List.of(
                    ToolCallMockModel.MockResponse.withToolCall("调用工具", "get_weather", "{\"city\": \"北京\"}"),
                    ToolCallMockModel.MockResponse.of("工具调用已批准")
            ));

            List<Middleware> middlewares = new ArrayList<>();
            middlewares.add(HumanInTheLoopMiddleware.builder()
                    .mode(HumanInTheLoopMiddleware.ReviewMode.WHITELIST)
                    .whitelist(List.of("get_weather"))
                    .callback((toolCall, context) ->
                            HumanInTheLoopMiddleware.ReviewDecision.ALLOW)
                    .build());

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .tool(mockWeatherTool)
                    .middlewares(middlewares)
                    .build();

            AgentRequest request = AgentRequest.of("查询天气");
            AgentResponse response = agent.invoke(request);

            assertEquals("工具调用已批准", response.getOutput());
        }
    }

    @Nested
    @DisplayName("状态持久化测试")
    class CheckpointerTest {

        @Test
        @DisplayName("保存和加载状态")
        void testSaveAndLoadState() throws AgentExecutionException, CheckpointException {
            SimpleMockModel model = new SimpleMockModel(List.of("第一轮响应"));

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .checkpointer(checkpointer)
                    .build();

            String threadId = "thread-test-123";

            // 第一轮调用
            AgentRequest request1 = AgentRequest.of("第一轮消息", threadId);
            AgentResponse response1 = agent.invoke(request1);

            // 验证状态已保存
            Optional<AgentState> loadedState = checkpointer.loadLatest(threadId);
            assertTrue(loadedState.isPresent());
            assertEquals(2, loadedState.get().getMessages().size());
        }

        @Test
        @DisplayName("时间旅行 - 列出所有检查点")
        void testListCheckpoints() throws AgentExecutionException, CheckpointException {
            SimpleMockModel model = new SimpleMockModel(List.of("响应1", "响应2", "响应3"));

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .checkpointer(checkpointer)
                    .build();

            String threadId = "thread-travel-123";

            // 三轮调用
            for (int i = 1; i <= 3; i++) {
                agent.invoke(AgentRequest.of("消息" + i, threadId));
            }

            // 列出所有检查点
            List<CheckpointInfo> checkpoints = checkpointer.list(threadId);
            assertEquals(3, checkpoints.size());
        }
    }

    @Nested
    @DisplayName("批量调用测试")
    class BatchTest {

        @Test
        @DisplayName("批量调用多个请求")
        void testBatchInvoke() throws AgentExecutionException {
            SimpleMockModel model = new SimpleMockModel(List.of("响应1", "响应2", "响应3"));

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .build();

            List<AgentRequest> requests = List.of(
                    AgentRequest.of("请求1"),
                    AgentRequest.of("请求2"),
                    AgentRequest.of("请求3")
            );

            List<AgentResponse> responses = agent.batch(requests);

            assertEquals(3, responses.size());
            assertEquals("响应1", responses.get(0).getOutput());
            assertEquals("响应2", responses.get(1).getOutput());
            assertEquals("响应3", responses.get(2).getOutput());
        }
    }

    @Nested
    @DisplayName("复杂场景测试")
    class ComplexScenarioTest {

        @Test
        @DisplayName("多轮对话带工具调用和状态持久化")
        void testMultiTurnWithToolsAndPersistence() throws AgentExecutionException, CheckpointException {
            ToolCallMockModel model = new ToolCallMockModel(List.of(
                    ToolCallMockModel.MockResponse.of("你好"),
                    ToolCallMockModel.MockResponse.withToolCall("查询天气", "get_weather", "{\"city\": \"深圳\"}"),
                    ToolCallMockModel.MockResponse.of("深圳今天 28°C"),
                    ToolCallMockModel.MockResponse.of("不客气")
            ));

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .tool(mockWeatherTool)
                    .checkpointer(checkpointer)
                    .build();

            String threadId = "thread-complex-123";

            AgentResponse r1 = agent.invoke(AgentRequest.of("你好", threadId));
            assertEquals("你好", r1.getOutput());

            AgentResponse r2 = agent.invoke(AgentRequest.of("深圳天气", threadId));
            assertEquals("深圳今天 28°C", r2.getOutput());

            AgentResponse r3 = agent.invoke(AgentRequest.of("谢谢", threadId));
            assertEquals("不客气", r3.getOutput());

            // 验证状态
            Optional<AgentState> state = checkpointer.loadLatest(threadId);
            assertTrue(state.isPresent());
        }

        @Test
        @DisplayName("多个中间件协同工作")
        void testMultipleMiddlewaresWorkingTogether() throws AgentExecutionException {
            SimpleMockModel model = new SimpleMockModel(List.of("你好"));

            List<Middleware> middlewares = new ArrayList<>();
            middlewares.add(StateInjectionMiddleware.builder()
                    .injectionMode(StateInjectionMiddleware.InjectionMode.SUFFIX)
                    .template("用户：${state:userRole}")
                    .build());
            middlewares.add(LoggingMiddleware.defaults());

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(model)
                    .middlewares(middlewares)
                    .build();

            AgentRequest request = AgentRequest.of("你好");
            request.toBuilder()
                    .parameters(Map.of("userRole", "管理员"))
                    .build();

            AgentResponse response = agent.invoke(request);
            assertNotNull(response.getOutput());
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("模型异常处理")
        void testModelException() {
            ChatModel errorModel = new ChatModel() {
                @Override
                public AIMessage chat(List<Message> messages, ChatOptions options) throws ModelException {
                    throw new ModelException("模拟模型错误");
                }

                @Override
                public void chatStream(List<Message> messages, ChatOptions options, StreamCallback callback) throws ModelException {
                    throw new ModelException("模拟模型错误");
                }

                @Override
                public int countTokens(String text) {
                    return text.length();
                }

                @Override
                public ModelCapabilities getCapabilities() {
                    return ModelCapabilities.builder().build();
                }

                @Override
                public String getModelId() {
                    return "error-model";
                }

                @Override
                public boolean isAvailable() {
                    return true;
                }
            };

            Agent agent = new DefaultAgentFactory().createAgent()
                    .model(errorModel)
                    .build();

            assertThrows(AgentExecutionException.class, () -> {
                agent.invoke(AgentRequest.of("测试"));
            });
        }
    }
}
