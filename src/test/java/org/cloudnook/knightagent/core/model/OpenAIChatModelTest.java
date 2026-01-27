package org.cloudnook.knightagent.core.model;

import org.cloudnook.knightagent.core.message.AIMessage;
import org.cloudnook.knightagent.core.message.HumanMessage;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.streaming.StreamCallbackAdapter;
import org.cloudnook.knightagent.core.streaming.StreamChunk;
import org.cloudnook.knightagent.core.streaming.StreamCompleteResponse;
import org.cloudnook.knightagent.core.streaming.StreamCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAIChatModel 测试类
 * <p>
 * 测试非流式调用和流式调用功能。
 * <p>
 * 运行测试前需要设置环境变量：
 * <ul>
 *   <li>OPENAI_API_KEY - OpenAI API 密钥</li>
 *   <li>OPENAI_BASE_URL - (可选) API 基础 URL，默认为 https://api.openai.com/v1</li>
 *   <li>OPENAI_MODEL - (可选) 模型名称，默认为 gpt-3.5-turbo</li>
 * </ul>
 * <p>
 * 运行测试：
 * <pre>{@code
 * # 使用真实 API
 * ./mvnw test -Dtest=OpenAIChatModelTest
 *
 * # 跳过测试（需要 API 密钥）
 * ./mvnw test -DskipTests
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
class OpenAIChatModelTest {

    private String apiKey;
    private String baseUrl;
    private String modelId;

    @BeforeEach
    void setUp() {
        apiKey = System.getenv("OPENAI_API_KEY");
        baseUrl = System.getenv("OPENAI_BASE_URL");
        modelId = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-3.5-turbo");

        // 如果没有 API 密钥，跳过测试
        org.junit.jupiter.api.Assumptions.assumeTrue(
                apiKey != null && !apiKey.isEmpty(),
                "OPENAI_API_KEY environment variable not set, skipping tests"
        );
    }

    /**
     * 测试非流式调用 - 基础对话
     */
    @Test
    void testChat_BasicConversation() {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelId(modelId)
                .build();

        List<Message> messages = List.of(
                HumanMessage.of("你好，请用一句话介绍你自己。")
        );

        AIMessage response = model.chat(messages);

        assertNotNull(response, "响应不应为 null");
        assertNotNull(response.getContent(), "内容不应为 null");
        assertFalse(response.getContent().isEmpty(), "内容不应为空");

        System.out.println("=== 非流式调用 - 基础对话 ===");
        System.out.println("响应: " + response.getContent());
    }

    /**
     * 测试非流式调用 - 带选项
     */
    @Test
    void testChat_WithOptions() {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelId(modelId)
                .build();

        ChatOptions options = ChatOptions.builder()
                .temperature(0.7)
                .maxTokens(100)
                .build();

        List<Message> messages = List.of(
                HumanMessage.of("什么是 Java？")
        );

        AIMessage response = model.chat(messages, options);

        assertNotNull(response);
        assertTrue(response.getContent().length() > 0);

        System.out.println("=== 非流式调用 - 带选项 ===");
        System.out.println("响应: " + response.getContent());
    }

    /**
     * 测试流式调用 - 基础流式输出
     */
    @Test
    void testChatStream_BasicStreaming() throws InterruptedException {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelId(modelId)
                .build();

        List<Message> messages = List.of(
                HumanMessage.of("请用三个词描述春天。")
        );

        // 收集流式输出
        StringBuilder fullContent = new StringBuilder();
        AtomicInteger tokenCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        model.chatStream(messages, ChatOptions.defaults(), new StreamCallbackAdapter() {
            @Override
            public void onToken(StreamChunk chunk) {
                String token = chunk.getContent();
                if (token != null) {
                    fullContent.append(token);
                    System.out.print(token);  // 实时输出
                    tokenCount.incrementAndGet();
                }
            }

            @Override
            public void onCompletion(StreamCompleteResponse response) {
                System.out.println("full response: " + response.getFullContent());
                System.out.println("full usage: " + response.getUsage());
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
                latch.countDown();
            }
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS), "流式调用应在 30 秒内完成");

        assertTrue(fullContent.length() > 0, "应收到完整内容");
        assertTrue(tokenCount.get() > 0, "应收到至少一个 token");

        System.out.println("\n=== 流式调用 - 基础流式输出 ===");
        System.out.println("完整内容: " + fullContent);
        System.out.println("Token 数量: " + tokenCount.get());
    }

    /**
     * 测试流式调用 - 获取完整响应
     */
    @Test
    void testChatStream_CompleteResponse() throws InterruptedException {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelId(modelId)
                .build();

        List<Message> messages = List.of(
                HumanMessage.of("2 + 2 = ?")
        );

        AtomicReference<StreamCompleteResponse> responseRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        model.chatStream(messages, ChatOptions.defaults(), new StreamCallbackAdapter() {
            @Override
            public void onToken(StreamChunk chunk) {
                System.out.print(chunk.getContent());
            }

            @Override
            public void onCompletion(StreamCompleteResponse response) {
                responseRef.set(response);
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
                latch.countDown();
            }
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS));

        StreamCompleteResponse response = responseRef.get();
        assertNotNull(response);
        assertNotNull(response.getFullContent());
        assertTrue(response.getFullContent().contains("4"), "答案应包含 4");

        System.out.println("\n=== 流式调用 - 完整响应 ===");
        System.out.println("完整内容: " + response.getFullContent());
        System.out.println("结束原因: " + response.getFinishReason());

        if (response.hasUsage()) {
            System.out.println("Token 用量:");
            System.out.println("  - 输入: " + response.getUsage().getPromptTokens());
            System.out.println("  - 输出: " + response.getUsage().getCompletionTokens());
            System.out.println("  - 总计: " + response.getUsage().getTotalTokens());
        }
    }

    /**
     * 测试流式调用 - 多轮对话
     */
    @Test
    void testChatStream_MultiTurnConversation() throws InterruptedException {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelId(modelId)
                .build();

        // 对话历史
        List<Message> conversationHistory = new java.util.ArrayList<>();
        StringBuilder firstResponse = new StringBuilder();
        StringBuilder secondResponse = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(2);

        // 第一轮：用户介绍自己
        HumanMessage userMsg1 = HumanMessage.of("我叫小明，请记住我的名字。");
        conversationHistory.add(userMsg1);

        model.chatStream(
                conversationHistory,
                ChatOptions.defaults(),
                new StreamCallbackAdapter() {
                    @Override
                    public void onToken(StreamChunk chunk) {
                        String token = chunk.getContent();
                        if (token != null) {
                            firstResponse.append(token);
                            System.out.print("第一轮 AI: " + token);
                        }
                    }

                    @Override
                    public void onCompletion(StreamCompleteResponse response) {
                        // 将 AI 回复加入对话历史
                        conversationHistory.add(org.cloudnook.knightagent.core.message.AIMessage.of(
                                response.getFullContent()
                        ));
                        latch.countDown();
                    }
                }
        );

        // 第二轮：询问名字
        HumanMessage userMsg2 = HumanMessage.of("我叫什么名字？");
        conversationHistory.add(userMsg2);

        model.chatStream(
                conversationHistory,  // 传入完整的对话历史
                ChatOptions.defaults(),
                new StreamCallbackAdapter() {
                    @Override
                    public void onToken(StreamChunk chunk) {
                        String token = chunk.getContent();
                        if (token != null) {
                            secondResponse.append(token);
                            System.out.print("第二轮 AI: " + token);
                        }
                    }

                    @Override
                    public void onCompletion(StreamCompleteResponse response) {
                        latch.countDown();
                    }
                }
        );

        assertTrue(latch.await(60, TimeUnit.SECONDS));
        assertTrue(secondResponse.toString().contains("小明"), "响应应包含用户名字");

        System.out.println("\n=== 流式调用 - 多轮对话 ===");
        System.out.println("第一轮响应: " + firstResponse);
        System.out.println("第二轮响应: " + secondResponse);
    }

    /**
     * 测试流式调用 - 空响应处理
     */
    @Test
    void testChatStream_EmptyResponse() throws InterruptedException {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelId(modelId)
                .build();

        // 发送空消息
        List<Message> messages = List.of(
                HumanMessage.of("")
        );

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> completed = new AtomicReference<>(false);

        model.chatStream(messages, ChatOptions.defaults(), new StreamCallbackAdapter() {
            @Override
            public void onToken(StreamChunk chunk) {
                System.out.print(chunk.getContent());
            }

            @Override
            public void onCompletion(StreamCompleteResponse response) {
                completed.set(true);
                assertNotNull(response);
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("错误: " + error.getMessage());
                latch.countDown();
            }
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertTrue(completed.get(), "应该完成调用");
    }

    /**
     * 测试模型配置
     */
    @Test
    void testModelConfiguration() {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelId(modelId)
                .build();

        assertEquals(modelId, model.getModelId());
        assertTrue(model.isAvailable());

        System.out.println("=== 模型配置 ===");
        System.out.println("模型 ID: " + model.getModelId());
        System.out.println("可用: " + model.isAvailable());
    }

    /**
     * 测试异常处理 - 无效 API 密钥
     */
    @Test
    void testInvalidApiKey() {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey("sk-invalid-key-12345")
                .baseUrl(baseUrl)
                .modelId(modelId)
                .build();

        List<Message> messages = List.of(
                HumanMessage.of("测试")
        );

        assertThrows(ModelException.class, () -> {
            model.chat(messages);
        });

        System.out.println("=== 异常处理 - 无效 API 密钥 ===");
        System.out.println("正确抛出 ModelException");
    }
}
