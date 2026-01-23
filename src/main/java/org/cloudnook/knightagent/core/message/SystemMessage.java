package org.cloudnook.knightagent.core.message;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 系统消息
 * <p>
 * 用于向 LLM 发送系统级指令或上下文信息。
 * 系统消息通常用于设置 AI 的行为、角色或约束条件。
 * <p>
 * 示例：
 * <pre>{@code
 * SystemMessage message = SystemMessage.builder()
 *     .content("你是一个专业的Java开发助手，专注于Spring Boot应用的咨询。")
 *     .build();
 * }</pre>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SystemMessage extends Message {

    /**
     * 系统提示词的优先级
     * <p>
     * 较高的优先级意味着系统提示词在对话中更不容易被覆盖或忽略。
     */
    private Integer priority;

    @Override
    public MessageType getType() {
        return MessageType.SYSTEM;
    }

    /**
     * 创建系统消息的便捷方法
     *
     * @param content 系统提示词内容
     * @return 系统消息实例
     */
    public static SystemMessage of(String content) {
        return SystemMessage.builder()
                .content(content)
                .build();
    }

    /**
     * 创建高优先级系统消息
     *
     * @param content   系统提示词内容
     * @param priority 优先级（值越大优先级越高）
     * @return 系统消息实例
     */
    public static SystemMessage of(String content, int priority) {
        return SystemMessage.builder()
                .content(content)
                .priority(priority)
                .build();
    }
}
