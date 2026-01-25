package org.cloudnook.knightagent.core.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MCP 资源描述
 * <p>
 * 表示 MCP 服务器提供的资源元数据。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpResourceDescription {

    /**
     * 资源 URI（唯一标识符）
     */
    @JsonProperty("uri")
    private String uri;

    /**
     * 资源名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 资源描述
     */
    @JsonProperty("description")
    private String description;

    /**
     * 资源 MIME 类型
     */
    @JsonProperty("mimeType")
    private String mimeType;
}
