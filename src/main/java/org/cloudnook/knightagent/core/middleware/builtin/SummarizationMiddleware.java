package org.cloudnook.knightagent.core.middleware.builtin;

import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.message.Message;
import org.cloudnook.knightagent.core.message.Message.MessageType;
import org.cloudnook.knightagent.core.message.HumanMessage;
import org.cloudnook.knightagent.core.message.SystemMessage;
import org.cloudnook.knightagent.core.message.AIMessage;
import org.cloudnook.knightagent.core.message.ToolMessage;
import org.cloudnook.knightagent.core.message.ToolCall;
import org.cloudnook.knightagent.core.message.ToolResult;
import org.cloudnook.knightagent.core.middleware.AgentContext;
import org.cloudnook.knightagent.core.middleware.Middleware;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.ChatOptions;
import org.cloudnook.knightagent.core.model.ModelException;
import org.cloudnook.knightagent.core.state.AgentState;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话摘要中间件
 * <p>
 * 当对话历史超过指定 Token 限制时，自动对历史消息进行摘要，
 * 保留最近的消息和摘要，避免超过模型的上下文窗口。
 * <p>
 * 工作原理：
 * <ol>
 *   <li>在调用 LLM 前检查历史消息的 Token 数量</li>
 *   <li>如果超过阈值，将早期消息合并为摘要</li>
   * <li>保留最近的消息和系统提示词</li>
 * </ol>
 * <p>
 * 使用示例：
 * <pre>{@code
 * Agent agent = AgentBuilder.builder()
 *     .model(chatModel)
 *     .middleware(SummarizationMiddleware.builder()
 *         .maxTokens(4000)
 *         .summaryModel(summaryModel)
 *         .build())
 *     .build();
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Slf4j
public class SummarizationMiddleware implements Middleware {

    /**
     * Token 计数器接口
     */
    @FunctionalInterface
    public interface TokenCounter {
        int countTokens(List<Message> messages);
    }

    private final int maxTokens;
    private final int summaryTargetTokens;
    private final TokenCounter tokenCounter;
    private final ChatModel summaryModel;
    private final String summaryPrompt;
    private final int recentMessagesCount;

    private SummarizationMiddleware(Builder builder) {
        this.maxTokens = builder.maxTokens;
        this.summaryTargetTokens = builder.summaryTargetTokens;
        this.tokenCounter = builder.tokenCounter;
        this.summaryModel = builder.summaryModel;
        this.summaryPrompt = builder.summaryPrompt;
        this.recentMessagesCount = builder.recentMessagesCount;
    }

    @Override
    public AgentState onStateUpdate(AgentState state, AgentContext context) {
        if (summaryModel == null) {
            return state;
        }

        List<Message> messages = state.getMessages();
        if (messages.isEmpty()) {
            return state;
        }

        int currentTokens = tokenCounter.countTokens(messages);
        if (currentTokens <= maxTokens) {
            // 未超过阈值，不需要摘要
            return state;
        }

        log.info("对话历史超过 Token 限制（{}/{}），开始摘要...",
                currentTokens, maxTokens);

        try {
            List<Message> summarized = summarizeMessages(messages);
            return new AgentState.Builder(state)
                    .messages(summarized)
                    .build();
        } catch (ModelException e) {
            log.warn("摘要失败，保留原状态: {}", e.getMessage());
            return state;
        }
    }

    /**
     * 对消息列表进行摘要
     *
     * @param messages 原始消息列表
     * @return 摘要后的消息列表
     */
    private List<Message> summarizeMessages(List<Message> messages) throws ModelException {
        List<Message> result = new ArrayList<>();

        // 保留系统提示词（如果有）
        for (Message message : messages) {
            if (message.getType() == MessageType.SYSTEM) {
                result.add(message);
            }
        }

        // 分离需要摘要的消息和最近的消息
        int summarizeCount = Math.max(0, messages.size() - recentMessagesCount);
        List<Message> toSummarize = new ArrayList<>();
        List<Message> recentMessages = new ArrayList<>();

        int i = 0;
        for (Message message : messages) {
            if (message.getType() == MessageType.SYSTEM) {
                continue; // 系统消息已保留
            }
            if (i < summarizeCount) {
                toSummarize.add(message);
            } else {
                recentMessages.add(message);
            }
            i++;
        }

        // 执行摘要
        if (!toSummarize.isEmpty()) {
            String summary = doSummarize(toSummarize);
            result.add(SystemMessage.of(summaryPrompt + "\n\n" + summary));
        }

        // 添加最近的消息
        result.addAll(recentMessages);

        log.info("摘要完成: {} 条消息 → {} 条消息",
                toSummarize.size(), result.size());

        return result;
    }

    /**
     * 执行实际的摘要操作
     *
     * @param messages 要摘要的消息
     * @return 摘要文本
     */
    private String doSummarize(List<Message> messages) throws ModelException {
        // 将消息转换为文本
        StringBuilder textBuilder = new StringBuilder();
        for (Message message : messages) {
            textBuilder.append(formatMessage(message));
            textBuilder.append("\n\n");
        }

        String conversationText = textBuilder.toString();

        // 调用模型生成摘要
        ChatOptions options = ChatOptions.builder()
                .maxTokens(summaryTargetTokens)
                .temperature(0.3)
                .build();

        AIMessage summary = summaryModel.chat(
                List.of(SystemMessage.of(summaryPrompt), HumanMessage.of(conversationText)),
                options
        );

        return summary.getContent();
    }

    /**
     * 格式化消息为文本
     */
    private String formatMessage(Message message) {
        return switch (message.getType()) {
            case HUMAN -> "用户: " + message.getContent();
            case AI -> "助手: " + message.getContent();
            case TOOL -> "工具[" + ((ToolMessage) message).getToolCallId() + "]: " + message.getContent();
            default -> message.getContent();
        };
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器
     */
    public static class Builder {

        private int maxTokens = 4000;
        private int summaryTargetTokens = 500;
        private TokenCounter tokenCounter;
        private ChatModel summaryModel;
        private String summaryPrompt = "请简要总结以下对话内容，保留关键信息：";
        private int recentMessagesCount = 10;

        /**
         * 设置最大 Token 数（默认 4000）
         */
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * 设置摘要的目标 Token 数（默认 500）
         */
        public Builder summaryTargetTokens(int summaryTargetTokens) {
            this.summaryTargetTokens = summaryTargetTokens;
            return this;
        }

        /**
         * 设置 Token 计数器
         */
        public Builder tokenCounter(TokenCounter tokenCounter) {
            this.tokenCounter = tokenCounter;
            return this;
        }

        /**
         * 设置用于摘要的模型
         */
        public Builder summaryModel(ChatModel summaryModel) {
            this.summaryModel = summaryModel;
            return this;
        }

        /**
         * 设置摘要提示词
         */
        public Builder summaryPrompt(String summaryPrompt) {
            this.summaryPrompt = summaryPrompt;
            return this;
        }

        /**
         * 设置保留的最近消息数量（默认 10）
         */
        public Builder recentMessagesCount(int recentMessagesCount) {
            this.recentMessagesCount = recentMessagesCount;
            return this;
        }

        /**
         * 构建中间件实例
         */
        public SummarizationMiddleware build() {
            if (tokenCounter == null) {
                throw new IllegalStateException("tokenCounter is required");
            }
            if (summaryModel == null) {
                throw new IllegalStateException("summaryModel is required");
            }
            return new SummarizationMiddleware(this);
        }
    }
}
