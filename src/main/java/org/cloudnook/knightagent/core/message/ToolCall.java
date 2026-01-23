package org.cloudnook.knightagent.core.message;

import lombok.Builder;
import lombok.Data;

/**
 * 工具调用
 * <p>
 * 表示 LLM 请求调用某个工具的请求。
 * 包含工具名称、参数以及唯一的调用标识。
 * <p>
 * 这个类用于在 AIMessage 中携带工具调用信息，
 * 与 {@link ToolResult} 配合使用，形成工具调用的请求-响应模式。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@Builder
public class ToolCall {

    /**
     * 工具调用的唯一标识
     * <p>
     * 用于关联工具调用请求和执行结果。
     * 格式通常为 "call_" + UUID，例如：call_abc123xyz
     */
    private String id;

    /**
     * 工具名称
     * <p>
     * 要调用的工具的名称，对应 {@link org.cloudnook.knightagent.core.tool.Tool#getName()}
     */
    private String name;

    /**
     * 工具参数（JSON 格式）
     * <p>
     * 调用工具时传递的参数，以 JSON 字符串形式存储。
     * 例如：{"city": "北京", "unit": "celsius"}
     */
    private String arguments;

    /**
     * 创建工具调用的便捷方法
     *
     * @param id        工具调用 ID
     * @param name      工具名称
     * @param arguments 工具参数（JSON 格式）
     * @return 工具调用实例
     */
    public static ToolCall of(String id, String name, String arguments) {
        return ToolCall.builder()
                .id(id)
                .name(name)
                .arguments(arguments)
                .build();
    }

    /**
     * 创建工具调用的便捷方法（自动生成 ID）
     *
     * @param name      工具名称
     * @param arguments 工具参数（JSON 格式）
     * @return 工具调用实例
     */
    public static ToolCall of(String name, String arguments) {
        return ToolCall.builder()
                .id("call_" + java.util.UUID.randomUUID().toString().substring(0, 8))
                .name(name)
                .arguments(arguments)
                .build();
    }
}
