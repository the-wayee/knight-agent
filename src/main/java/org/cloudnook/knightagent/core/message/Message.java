package org.cloudnook.knightagent.core.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息基类
 * <p>
 * 所有消息类型的抽象基类，定义了消息的公共属性。
 * 消息是 Agent 与 LLM 之间传递的基本单位。
 * <p>
 * 使用 Jackson 多态序列化支持，子类通过 @JsonSubTypes 注解注册。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SystemMessage.class, name = "SYSTEM"),
        @JsonSubTypes.Type(value = HumanMessage.class, name = "HUMAN"),
        @JsonSubTypes.Type(value = AIMessage.class, name = "AI"),
        @JsonSubTypes.Type(value = ToolMessage.class, name = "TOOL")
})
public abstract class Message {

    /**
     * 消息内容
     * <p>
     * 对于文本消息，这是消息的主体内容。
     * 对于包含工具调用的消息，这可能是可选的。
     */
    protected String content;

    /**
     * 附加数据
     * <p>
     * 用于存储消息的额外元数据或扩展信息。
     * 例如：消息 ID、来源、优先级等。
     */
    protected Map<String, Object> additionalData = new HashMap<>();

    /**
     * 消息时间戳
     * <p>
     * 记录消息创建的时间，用于排序和追踪。
     */
    protected Instant timestamp = Instant.now();

    /**
     * 获取消息类型
     * <p>
     * 用于序列化/反序列化时的类型识别。
     *
     * @return 消息类型枚举
     */
    public abstract MessageType getType();

    /**
     * 添加附加数据
     *
     * @param key   键
     * @param value 值
     * @return this，支持链式调用
     */
    public Message addAdditionalData(String key, Object value) {
        if (this.additionalData == null) {
            this.additionalData = new HashMap<>();
        }
        this.additionalData.put(key, value);
        return this;
    }

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        /**
         * 系统消息
         */
        SYSTEM,
        /**
         * 用户消息
         */
        HUMAN,
        /**
         * AI 消息
         */
        AI,
        /**
         * 工具消息
         */
        TOOL
    }
}
