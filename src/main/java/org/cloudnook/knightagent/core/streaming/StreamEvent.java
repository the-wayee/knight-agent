package org.cloudnook.knightagent.core.streaming;

import lombok.Data;
import org.cloudnook.knightagent.core.message.ToolCall;

/**
 * 流式事件
 * <p>
 * 封装流式输出中的各种事件类型，用于统一的事件传递。
 * 相比直接使用回调接口，事件对象更适合需要异步处理或存储的场景。
 * <p>
 * 事件类型：
 * <ul>
 *   <li>TOKEN - 文本增量</li>
 *   <li>TOOL_CALL - 工具调用</li>
 *   <li>REASONING - 思考过程</li>
 *   <li>START - 流开始</li>
 *   <li>COMPLETE - 流完成</li>
 *   <li>ERROR - 错误</li>
 * </ul>
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
public class StreamEvent {

    /**
     * 事件类型
     */
    private final EventType type;

    /**
     * Token 内容（仅 TOKEN 类型）
     */
    private String token;

    /**
     * 工具调用（仅 TOOL_CALL 类型）
     */
    private ToolCall toolCall;

    /**
     * 思考内容（仅 REASONING 类型）
     */
    private String reasoning;

    /**
     * 错误信息（仅 ERROR 类型）
     */
    private Throwable error;

    /**
     * 事件时间戳
     */
    private final long timestamp;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /**
         * 流开始
         */
        START,
        /**
         * 文本增量
         */
        TOKEN,
        /**
         * 工具调用
         */
        TOOL_CALL,
        /**
         * 思考过程
         */
        REASONING,
        /**
         * 流完成
         */
        COMPLETE,
        /**
         * 错误
         */
        ERROR
    }

    // 私有构造函数
    private StreamEvent(EventType type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建开始事件
     */
    public static StreamEvent start() {
        return new StreamEvent(EventType.START);
    }

    /**
     * 创建 Token 事件
     */
    public static StreamEvent token(String token) {
        StreamEvent event = new StreamEvent(EventType.TOKEN);
        event.token = token;
        return event;
    }

    /**
     * 创建工具调用事件
     */
    public static StreamEvent toolCall(ToolCall toolCall) {
        StreamEvent event = new StreamEvent(EventType.TOOL_CALL);
        event.toolCall = toolCall;
        return event;
    }

    /**
     * 创建思考过程事件
     */
    public static StreamEvent reasoning(String reasoning) {
        StreamEvent event = new StreamEvent(EventType.REASONING);
        event.reasoning = reasoning;
        return event;
    }

    /**
     * 创建完成事件
     */
    public static StreamEvent complete() {
        return new StreamEvent(EventType.COMPLETE);
    }

    /**
     * 创建错误事件
     */
    public static StreamEvent error(Throwable error) {
        StreamEvent event = new StreamEvent(EventType.ERROR);
        event.error = error;
        return event;
    }

    // ==================== 便捷方法 ====================

    /**
     * 是否为开始事件
     */
    public boolean isStart() {
        return type == EventType.START;
    }

    /**
     * 是否为 Token 事件
     */
    public boolean isToken() {
        return type == EventType.TOKEN;
    }

    /**
     * 是否为工具调用事件
     */
    public boolean isToolCall() {
        return type == EventType.TOOL_CALL;
    }

    /**
     * 是否为完成事件
     */
    public boolean isComplete() {
        return type == EventType.COMPLETE;
    }

    /**
     * 是否为错误事件
     */
    public boolean isError() {
        return type == EventType.ERROR;
    }

    /**
     * 是否为终止事件（完成或错误）
     */
    public boolean isTerminal() {
        return type == EventType.COMPLETE || type == EventType.ERROR;
    }
}
