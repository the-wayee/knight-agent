package org.cloudnook.knightagent.core.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * MCP 工具描述
 * <p>
 * 表示 MCP 服务器提供的工具元数据。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略未知字段，便于兼容不同版本的 MCP
public class McpToolDescription {

    /**
     * 工具名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 工具描述
     */
    @JsonProperty("description")
    private String description;

    /**
     * 输入参数的 JSON Schema
     */
    @JsonProperty("inputSchema")
    private Map<String, Object> inputSchema;

    /**
     * 输出结果的 JSON Schema（可选）
     */
    @JsonProperty("outputSchema")
    private Map<String, Object> outputSchema;
}
