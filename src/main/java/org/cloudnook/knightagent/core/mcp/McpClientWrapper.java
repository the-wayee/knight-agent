package org.cloudnook.knightagent.core.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP 客户端（使用官方 SDK）
 * <p>
 * 基于 Model Context Protocol 官方 SDK 实现。
 * 支持多种传输协议：STDIO、Streamable-HTTP、SSE。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
@Slf4j
public class McpClientWrapper implements AutoCloseable {

    private final McpConfig config;
    private McpSyncClient mcpClient;
    private boolean initialized = false;

    /**
     * 构造函数
     *
     * @param config MCP 配置
     */
    public McpClientWrapper(McpConfig config) {
        this.config = config;
    }

    /**
     * 初始化 MCP 连接
     */
    public synchronized void initialize() throws McpException {
        if (initialized) {
            return;
        }

        config.validate();

        try {
            // 使用 McpClient.sync() 创建同步客户端
            McpClient.SyncSpec syncSpec = McpClient.sync(createTransport())
                    .requestTimeout(Duration.ofSeconds(30))
                    .initializationTimeout(Duration.ofSeconds(30));

            mcpClient = syncSpec.build();

            // 初始化连接
            mcpClient.initialize();

            log.info("MCP 客户端初始化成功: {}", config.getProtocol());

            initialized = true;

        } catch (Exception e) {
            throw new McpException("MCP 客户端初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建传输层
     */
    private McpClientTransport createTransport() throws McpException {
        return switch (config.getProtocol()) {
            case STDIO -> {
                // 解析命令行
                String[] command = parseCommandLine(config.getEntrypoint());
                ServerParameters params = ServerParameters.builder(command[0])
                        .args(java.util.Arrays.copyOfRange(command, 1, command.length))
                        .build();
                yield new StdioClientTransport(params, null);
            }
            case STREAMABLE_HTTP -> HttpClientStreamableHttpTransport.builder(config.getEntrypoint()).build();
            case SSE -> HttpClientSseClientTransport.builder(config.getEntrypoint()).build();
            default -> throw new McpException("不支持的协议: " + config.getProtocol());
        };
    }

    /**
     * 获取可用工具列表
     */
    public List<McpToolDescription> listTools() throws McpException {
        ensureInitialized();

        try {
            McpSchema.ListToolsResult toolsResult = mcpClient.listTools();

            if (toolsResult.tools() == null || toolsResult.tools().isEmpty()) {
                log.debug("没有发现工具");
                return List.of();
            }

            List<McpToolDescription> result = new ArrayList<>();
            for (McpSchema.Tool tool : toolsResult.tools()) {
                McpToolDescription desc = new McpToolDescription();
                desc.setName(tool.name());
                desc.setDescription(tool.description());
                if (tool.inputSchema() != null) {
                    desc.setInputSchema(tool.inputSchema().properties());
                }
                result.add(desc);
            }

            log.info("获取到 {} 个工具", result.size());
            return result;

        } catch (Exception e) {
            throw new McpException("获取工具列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用工具
     */
    public McpToolResult callTool(String toolName, Map<String, Object> arguments) throws McpException {
        ensureInitialized();

        try {
            McpSchema.CallToolResult callResult = mcpClient.callTool(
                    new McpSchema.CallToolRequest(toolName, arguments)
            );

            McpToolResult result = new McpToolResult();
            result.setError(callResult.isError() != null && callResult.isError());

            if (callResult.content() != null && !callResult.content().isEmpty()) {
                result.setContent(new ArrayList<>());

                for (McpSchema.Content content : callResult.content()) {
                    McpToolResult.ContentItem item = new McpToolResult.ContentItem();
                    item.setType(content.type());

                    // 根据类型处理不同的内容
                    if (content instanceof McpSchema.TextContent textContent) {
                        item.setText(textContent.text());
                    } else if (content instanceof McpSchema.ImageContent imageContent) {
                        item.setData(imageContent.data());
                        item.setMimeType(imageContent.mimeType());
                    } else if (content instanceof McpSchema.ResourceContent resourceContent) {
                        item.setData(resourceContent.uri());
                        item.setMimeType(resourceContent.mimeType());
                    }

                    result.getContent().add(item);
                }
            }

            return result;

        } catch (Exception e) {
            throw new McpException("工具调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取可用资源列表
     */
    public List<McpResourceDescription> listResources() throws McpException {
        ensureInitialized();

        try {
            McpSchema.ListResourcesResult resourcesResult = mcpClient.listResources();

            if (resourcesResult.resources() == null || resourcesResult.resources().isEmpty()) {
                log.debug("没有发现资源");
                return List.of();
            }

            List<McpResourceDescription> result = new ArrayList<>();
            for (McpSchema.Resource resource : resourcesResult.resources()) {
                McpResourceDescription desc = new McpResourceDescription();
                desc.setUri(resource.uri());
                desc.setName(resource.name());
                desc.setDescription(resource.description());
                if (resource.mimeType() != null) {
                    desc.setMimeType(resource.mimeType());
                }
                result.add(desc);
            }

            log.info("获取到 {} 个资源", result.size());
            return result;

        } catch (Exception e) {
            throw new McpException("获取资源列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取资源内容
     */
    public String readResource(String uri) throws McpException {
        ensureInitialized();

        try {
            McpSchema.ReadResourceResult readResult = mcpClient.readResource(
                    new McpSchema.ReadResourceRequest(uri)
            );

            if (readResult.contents() != null && !readResult.contents().isEmpty()) {
                McpSchema.ResourceContents first = readResult.contents().get(0);
                // 如果是文本资源内容
                if (first instanceof McpSchema.TextResourceContents textContents) {
                    return textContents.text();
                }
                // 如果是 Blob 资源内容，返回 base64 编码的数据
                else if (first instanceof McpSchema.BlobResourceContents blobContents) {
                    return blobContents.blob();
                }
            }

            return null;

        } catch (Exception e) {
            throw new McpException("读取资源失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取可用提示词列表
     */
    public List<McpPromptDescription> listPrompts() throws McpException {
        ensureInitialized();

        try {
            McpSchema.ListPromptsResult promptsResult = mcpClient.listPrompts();

            if (promptsResult.prompts() == null || promptsResult.prompts().isEmpty()) {
                log.debug("没有发现提示词");
                return List.of();
            }

            List<McpPromptDescription> result = new ArrayList<>();
            for (McpSchema.Prompt prompt : promptsResult.prompts()) {
                McpPromptDescription desc = new McpPromptDescription();
                desc.setName(prompt.name());
                desc.setDescription(prompt.description());

                if (prompt.arguments() != null && !prompt.arguments().isEmpty()) {
                    List<McpPromptDescription.Argument> args = new ArrayList<>();
                    for (McpSchema.PromptArgument arg : prompt.arguments()) {
                        McpPromptDescription.Argument argument = new McpPromptDescription.Argument();
                        argument.setName(arg.name());
                        argument.setDescription(arg.description());
                        argument.setRequired(arg.required() != null && arg.required());
                        args.add(argument);
                    }
                    desc.setArguments(args);
                }

                result.add(desc);
            }

            log.info("获取到 {} 个提示词", result.size());
            return result;

        } catch (Exception e) {
            throw new McpException("获取提示词列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取配置
     */
    public McpConfig getConfig() {
        return config;
    }

    /**
     * 关闭客户端
     */
    @Override
    public synchronized void close() {
        if (!initialized) {
            return;
        }

        log.info("关闭 MCP 客户端");

        try {
            if (mcpClient != null) {
                mcpClient.close();
            }
        } catch (Exception e) {
            log.error("关闭 MCP 客户端失败", e);
        }

        initialized = false;
    }

    /**
     * 确保已初始化
     */
    private void ensureInitialized() throws McpException {
        if (!initialized) {
            initialize();
        }
    }

    /**
     * 解析命令行字符串
     */
    private String[] parseCommandLine(String command) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts.toArray(new String[0]);
    }

    /**
     * 获取原始 MCP 客户端（用于高级操作）
     */
    public McpSyncClient getRawClient() {
        return mcpClient;
    }
}
