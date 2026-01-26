package org.cloudnook.knightagent.core.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudnook.knightagent.core.message.ToolResult;
import org.cloudnook.knightagent.core.tool.AbstractTool;
import org.cloudnook.knightagent.core.tool.ToolExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP 工具适配器（使用官方 SDK）
 * <p>
 * 将 MCP 工具适配为 KnightAgent 的 Tool 接口。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class McpToolWrapper extends AbstractTool {

    private static final Logger log = LoggerFactory.getLogger(McpToolWrapper.class);

    private final McpClientWrapper client;
    private final McpToolDescription description;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造函数
     *
     * @param client      MCP 客户端包装器
     * @param description 工具描述
     */
    public McpToolWrapper(McpClientWrapper client, McpToolDescription description) {
        this.client = client;
        this.description = description;
    }

    @Override
    public String getName() {
        return description.getName();
    }

    @Override
    public String getDescription() {
        String desc = description.getDescription();
        return desc != null ? desc : "MCP 工具: " + description.getName();
    }

    @Override
    public java.util.Map<String, Object> getParameters() {
        if (description.getInputSchema() == null || description.getInputSchema().isEmpty()) {
            return java.util.Map.of("type", "object", "properties", java.util.Map.of());
        }
        // InputSchema 已经是 Map 结构，直接返回
        return description.getInputSchema();
    }

    @Override
    protected ToolResult executeInternal(java.util.Map<String, Object> arguments) throws Exception {
        log.debug("执行 MCP 工具: {} 参数: {}", description.getName(), arguments);

        McpToolResult mcpResult = client.callTool(description.getName(), arguments);

        if (mcpResult.isError()) {
            String errorMessage = extractTextContent(mcpResult);
            return ToolResult.error(generateCallId(), errorMessage);
        }

        String result = extractTextContent(mcpResult);
        return ToolResult.success(generateCallId(), result);
    }

    /**
     * 从 MCP 结果中提取文本内容
     */
    private String extractTextContent(McpToolResult mcpResult) {
        if (mcpResult.getContent() == null || mcpResult.getContent().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (McpToolResult.ContentItem item : mcpResult.getContent()) {
            if ("text".equals(item.getType())) {
                if (result.length() > 0) {
                    result.append("\n");
                }
                result.append(item.getText());
            } else if ("resource".equals(item.getType()) || "image".equals(item.getType())) {
                if (result.length() > 0) {
                    result.append("\n");
                }
                result.append("[Resource: ").append(item.getData() != null ? item.getData() : "data").append("]");
            }
        }

        return result.toString();
    }

    /**
     * 获取 MCP 工具描述
     */
    public McpToolDescription getToolDescription() {
        return description;
    }

    /**
     * 获取 MCP 客户端
     */
    public McpClientWrapper getClient() {
        return client;
    }

    @Override
    public String getCategory() {
        return "mcp";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String toString() {
        return "McpToolWrapper{" +
                "name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                '}';
    }
}
