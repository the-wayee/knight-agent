package org.cloudnook.knightagent.core.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 消息
 * <p>
 * 代表 LLM 返回的响应消息。
 * 可以包含文本内容，也可以包含工具调用请求。
 * <p>
 * 示例（纯文本响应）：
 * <pre>{@code
 * AIMessage message = AIMessage.builder()
 *     .content("今天北京天气晴朗，温度25°C。")
 *     .build();
 * }</pre>
 * <p>
 * 示例（包含工具调用）：
 * <pre>{@code
 * AIMessage message = AIMessage.builder()
 *     .content("我来帮你查询北京的天气。")
 *     .toolCall(List.of(
 *         ToolCall.builder()
 *             .id("call_123")
 *             .name("get_weather")
 *             .arguments("{\"city\": \"北京\"}")
 *             .build()
 *     ))
 *     .build();
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AIMessage extends Message {

    /**
     * 工具调用列表
     * <p>
     * 当 LLM 决定调用工具时，会包含一个或多个工具调用请求。
     * 每个工具调用包含工具名称、参数和唯一标识。
     */
    private List<ToolCall> toolCalls = new ArrayList<>();

    /**
     * 思考过程
     * <p>
     * 某些模型（如 Claude 的 thinking 模式）会输出思考过程。
     * 这部分内容不会传递给用户，但可以用于调试和分析。
     */
    private String reasoning;

    /**
     * 使用的 Token 数量
     * <p>
     * 记录生成此消息所消耗的 Token 数量，
     * 用于成本计算和 Token 预算管理。
     */
    private Integer usageTokens;

    /**
     * 是否包含工具调用
     *
     * @return 如果包含工具调用返回 true
     */
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    /**
     * 添加工具调用
     *
     * @param toolCall 工具调用
     * @return this，支持链式调用
     */
    public AIMessage addToolCall(ToolCall toolCall) {
        if (this.toolCalls == null) {
            this.toolCalls = new ArrayList<>();
        }
        this.toolCalls.add(toolCall);
        return this;
    }

    @Override
    public MessageType getType() {
        return MessageType.AI;
    }

    /**
     * 创建纯文本 AI 消息
     *
     * @param content AI 响应内容
     * @return AI 消息实例
     */
    public static AIMessage of(String content) {
        return AIMessage.builder()
                .content(content)
                .build();
    }

    /**
     * 创建包含工具调用的 AI 消息
     *
     * @param content   AI 响应内容
     * @param toolCalls 工具调用列表
     * @return AI 消息实例
     */
    public static AIMessage of(String content, List<ToolCall> toolCalls) {
        return AIMessage.builder()
                .content(content)
                .toolCalls(toolCalls)
                .build();
    }

    /**
     * 获取工具调用的不可变视图
     *
     * @return 工具调用列表的不可变视图
     */
    public List<ToolCall> getToolCalls() {
        return toolCalls != null ? Collections.unmodifiableList(toolCalls) : Collections.emptyList();
    }
}
