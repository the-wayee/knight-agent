package org.cloudnook.knightagent.core.model;

import org.cloudnook.knightagent.core.message.AIMessage;
import org.cloudnook.knightagent.core.message.HumanMessage;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.message.SystemMessage;
import org.cloudnook.knightagent.core.streaming.StreamCallbackAdapter;
import org.cloudnook.knightagent.core.streaming.StreamChunk;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAI 聊天模型测试
 * <p>
 * 注意：这些测试需要真实的 API Key，默认被禁用。
 * 运行测试前请设置环境变量 OPENAI_API_KEY。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
class OpenAIChatModelTest {

    private static final String API_KEY = System.getenv("OPENAI_API_KEY");

    /**
     * 获取配置好的模型实例
     */
    private OpenAIChatModel createModel() {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("Please set OPENAI_API_KEY environment variable");
        }
        return OpenAIChatModel.builder()
                .apiKey(API_KEY)
                .modelId("gpt-3.5-turbo")
                .build();
    }

    @Test
    @Disabled("Requires API key")
    void testBasicChat() throws ModelException {
        OpenAIChatModel model = createModel();

        List<Message> messages = List.of(
                HumanMessage.of("你好，请用一句话介绍一下你自己")
        );

        AIMessage response = model.chat(messages);

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertFalse(response.getContent().isEmpty());
        System.out.println("Response: " + response.getContent());
    }

    @Test
    @Disabled("Requires API key")
    void testChatWithSystemPrompt() throws ModelException {
        OpenAIChatModel model = createModel();

        List<Message> messages = List.of(
                SystemMessage.of("你是一个专业的 Java 开发助手，只回答 Java 相关的问题。"),
                HumanMessage.of("Python 怎么样？")
        );

        AIMessage response = model.chat(messages);

        assertNotNull(response);
        assertNotNull(response.getContent());
        System.out.println("Response: " + response.getContent());
    }

    @Test
    @Disabled("Requires API key")
    void testChatWithOptions() throws ModelException {
        OpenAIChatModel model = createModel();

        ChatOptions options = ChatOptions.builder()
                .temperature(0.1)
                .maxTokens(100)
                .build();

        List<Message> messages = List.of(
                HumanMessage.of("用一句话说明什么是设计模式")
        );

        AIMessage response = model.chat(messages, options);

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertTrue(response.getContent().length() < 500, "Response should be limited");
        System.out.println("Response: " + response.getContent());
    }

    @Test
    @Disabled("Requires API key")
    void testStreamingChat() throws ModelException {
        OpenAIChatModel model = createModel();

        List<Message> messages = List.of(
                HumanMessage.of("请用 3 个词描述 Spring 框架")
        );

        StringBuilder fullResponse = new StringBuilder();

        model.chatStream(messages, new StreamCallbackAdapter() {
            @Override
            public void onToken(StreamChunk chunk) {
                String token = chunk.getContent() != null ? chunk.getContent() : "";
                System.out.print(token);
                fullResponse.append(token);
            }

            @Override
            public void onComplete(StreamChunk finalChunk) {
                System.out.println("\n[Stream completed]");
            }
        });

        assertTrue(fullResponse.length() > 0);
        System.out.println("\nFull response: " + fullResponse);
    }

    @Test
    @Disabled("Requires API key")
    void testMultiTurnConversation() throws ModelException {
        OpenAIChatModel model = createModel();

        List<Message> messages = List.of(
                HumanMessage.of("我的名字叫张三"),
                AIMessage.of("你好张三，很高兴认识你。"),
                HumanMessage.of("我叫什么名字？")
        );

        AIMessage response = model.chat(messages);

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("张三"), "Should remember the name");
        System.out.println("Response: " + response.getContent());
    }

    @Test
    @Disabled("Requires API key")
    void testIsAvailable() throws ModelException {
        OpenAIChatModel model = createModel();

        boolean available = model.isAvailable();

        assertTrue(available, "Model should be available with valid API key");
        System.out.println("Model is available: " + available);
    }

    @Test
    void testBuilderDefaults() {
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey("test-key")
                .build();

        assertEquals("gpt-3.5-turbo", model.getModelId());
    }
}
