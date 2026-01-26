package org.cloudnook.knightagent.core.model;

import org.cloudnook.knightagent.core.message.AIMessage;
import org.cloudnook.knightagent.core.message.HumanMessage;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.streaming.StreamCallbackAdapter;
import org.cloudnook.knightagent.core.streaming.StreamChunk;

import java.util.List;
import java.util.Scanner;

/**
 * OpenAI 聊天模型手动测试
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class OpenAIManualTest {

    // ==================== 配置区域 - 请修改这里 ====================
    // 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
    private static final String API_KEY = "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";
    private static final String BASE_URL = "https://api.openai.com/v1";
    private static final String MODEL_ID = "gpt-3.5-turbo";
    // ===========================================================

    public static void main(String[] args) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("请设置环境变量 OPENAI_API_KEY");
            System.exit(1);
        }

        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .modelId(MODEL_ID)
                .build();

        runChat(model);
    }

    private static void runChat(ChatModel model) {
        System.out.println("=== KnightAgent OpenAI 测试 ===");
        System.out.println("模型: " + model.getModelId());
        System.out.println("Base URL: " + BASE_URL);
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("你> ");
            String userInput = scanner.nextLine();

            if (userInput.isEmpty()) {
                continue;
            }

            if ("exit".equals(userInput) || "quit".equals(userInput)) {
                System.out.println("再见！");
                break;
            }

            if ("stream".equals(userInput)) {
                testStreaming(model, scanner);
                continue;
            }

            if ("info".equals(userInput)) {
                System.out.println("模型信息:");
                System.out.println("  ID: " + model.getModelId());
                System.out.println("  可用: " + model.isAvailable());
                continue;
            }

            // 同步调用
            try {
                List<Message> messages = List.of(HumanMessage.of(userInput));
                AIMessage response = model.chat(messages);
                System.out.println("AI> " + response.getContent());
                System.out.println();
            } catch (ModelException e) {
                System.err.println("错误: " + e.getMessage());
                e.printStackTrace();
            }
        }

        scanner.close();
    }

    private static void testStreaming(ChatModel model, Scanner scanner) {
        System.out.println("请输入测试问题:");
        String userInput = scanner.nextLine();

        System.out.print("AI> ");
        try {
            model.chatStream(List.of(HumanMessage.of(userInput)), new StreamCallbackAdapter() {
                @Override
                public void onToken(StreamChunk chunk) {
                    String token = chunk.getContent() != null ? chunk.getContent() : "";
                    System.out.print(token);
                }

                @Override
                public void onComplete(StreamChunk finalChunk) {
                    System.out.println();
                    System.out.println("[流式输出完成]");
                }

                @Override
                public void onError(Throwable error) {
                    System.err.println("\n[错误] " + error.getMessage());
                }
            });
        } catch (ModelException e) {
            System.err.println("错误: " + e.getMessage());
        }
    }
}
