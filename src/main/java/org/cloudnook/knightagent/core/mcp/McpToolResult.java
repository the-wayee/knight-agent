package org.cloudnook.knightagent.core.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * MCP 工具执行结果
 * <p>
 * 表示 MCP 工具调用的返回结果。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class McpToolResult {

    /**
     * 结果内容列表
     */
    @JsonProperty("content")
    private List<ContentItem> content;

    /**
     * 是否存在错误
     */
    @JsonProperty("isError")
    private boolean isError;

    /**
     * 内容项
     */
    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentItem {
        /**
         * 内容类型
         */
        @JsonProperty("type")
        private String type;

        /**
         * 文本内容（type=text 时）
         */
        @JsonProperty("text")
        private String text;

        /**
         * 数据内容（type=image/resource 时）
         */
        @JsonProperty("data")
        private String data;

        /**
         * MIME 类型
         */
        @JsonProperty("mimeType")
        private String mimeType;
    }
}
