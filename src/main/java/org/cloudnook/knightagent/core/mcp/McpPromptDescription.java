package org.cloudnook.knightagent.core.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * MCP 提示词描述
 * <p>
 * 表示 MCP 服务器提供的提示词元数据。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpPromptDescription {

    /**
     * 提示词名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 提示词描述
     */
    @JsonProperty("description")
    private String description;

    /**
     * 参数定义
     */
    @JsonProperty("arguments")
    private List<Argument> arguments;

    /**
     * 参数定义
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Argument {
        /**
         * 参数名称
         */
        @JsonProperty("name")
        private String name;

        /**
         * 参数描述
         */
        @JsonProperty("description")
        private String description;

        /**
         * 是否必需
         */
        @JsonProperty("required")
        private boolean required;
    }
}
