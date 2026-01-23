package org.cloudnook.knightagent.core.message;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 用户消息
 * <p>
 * 代表用户发送给 Agent 的消息。
 * 这是最常见的消息类型，用于承载用户的输入、问题或指令。
 * <p>
 * 示例：
 * <pre>{@code
 * HumanMessage message = HumanMessage.builder()
 *     .content("今天北京的天气怎么样？")
 *     .build();
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class HumanMessage extends Message {

    /**
     * 用户标识
     * <p>
     * 用于标识发送消息的用户，在多用户场景中很有用。
     */
    private String userId;

    /**
     * 消息来源
     * <p>
     * 标识消息的来源渠道，如：web、api、mobile 等。
     */
    private String source;

    @Override
    public MessageType getType() {
        return MessageType.HUMAN;
    }

    /**
     * 创建用户消息的便捷方法
     *
     * @param content 用户输入内容
     * @return 用户消息实例
     */
    public static HumanMessage of(String content) {
        return HumanMessage.builder()
                .content(content)
                .build();
    }

    /**
     * 创建带用户标识的消息
     *
     * @param content 用户输入内容
     * @param userId  用户标识
     * @return 用户消息实例
     */
    public static HumanMessage of(String content, String userId) {
        return HumanMessage.builder()
                .content(content)
                .userId(userId)
                .build();
    }
}
