package org.cloudnook.knightagent.core.mcp;

import org.cloudnook.knightagent.core.tool.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP 工具注册器（使用官方 SDK）
 * <p>
 * 负责从 MCP 服务器自动发现并注册工具。
 *
 * @author KnightAgent
 * @since 1.0.0
 */
public class McpToolRegistryWrapper implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(McpToolRegistryWrapper.class);

    private final McpConfig config;
    private final McpClientWrapper client;
    private final List<McpTool> registeredTools = new ArrayList<>();
    private boolean initialized = false;

    /**
     * 构造函数
     *
     * @param config MCP 配置
     */
    public McpToolRegistryWrapper(McpConfig config) {
        this.config = config;
        this.client = new McpClientWrapper(config);
    }

    /**
     * 初始化注册器
     */
    public synchronized void initialize() throws McpException {
        if (initialized) {
            return;
        }

        log.info("初始化 MCP 工具注册器: {}", config.getProtocol());

        try {
            // 初始化客户端
            client.initialize();

            // 自动发现工具
            if (config.isAutoDiscoverTools()) {
                discoverAndRegisterTools();
            }

            // 自动发现资源
            if (config.isAutoDiscoverResources()) {
                discoverResources();
            }

            // 自动发现提示词
            if (config.isAutoDiscoverPrompts()) {
                discoverPrompts();
            }

            initialized = true;
            log.info("MCP 工具注册器初始化完成，注册了 {} 个工具", registeredTools.size());

        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException("MCP 工具注册器初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发现并注册工具
     */
    private void discoverAndRegisterTools() throws McpException {
        log.info("开始发现 MCP 工具...");

        List<McpToolDescription> tools = client.listTools();

        for (McpToolDescription toolDesc : tools) {
            McpTool tool = new McpToolWrapper(client, toolDesc);
            registeredTools.add(tool);
            log.debug("注册 MCP 工具: {} - {}", toolDesc.getName(), toolDesc.getDescription());
        }

        log.info("发现 {} 个 MCP 工具", tools.size());
    }

    /**
     * 发现资源
     */
    private void discoverResources() throws McpException {
        log.info("开始发现 MCP 资源...");

        List<McpResourceDescription> resources = client.listResources();

        for (McpResourceDescription resource : resources) {
            log.debug("发现 MCP 资源: {} - {}", resource.getUri(), resource.getName());
        }

        log.info("发现 {} 个 MCP 资源", resources.size());
    }

    /**
     * 发现提示词
     */
    private void discoverPrompts() throws McpException {
        log.info("开始发现 MCP 提示词...");

        List<McpPromptDescription> prompts = client.listPrompts();

        for (McpPromptDescription prompt : prompts) {
            log.debug("发现 MCP 提示词: {} - {}", prompt.getName(), prompt.getDescription());
        }

        log.info("发现 {} 个 MCP 提示词", prompts.size());
    }

    /**
     * 获取已注册的工具列表
     */
    public List<McpTool> getRegisteredTools() {
        return new ArrayList<>(registeredTools);
    }

    /**
     * 获取 MCP 客户端
     */
    public McpClientWrapper getClient() {
        return client;
    }

    /**
     * 获取 MCP 配置
     */
    public McpConfig getConfig() {
        return config;
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取已注册工具数量
     */
    public int getToolCount() {
        return registeredTools.size();
    }

    /**
     * 关闭注册器
     */
    @Override
    public synchronized void close() {
        if (!initialized) {
            return;
        }

        log.info("关闭 MCP 工具注册器");

        registeredTools.clear();
        client.close();
        initialized = false;
    }

    /**
     * 读取资源内容
     */
    public String readResource(String uri) throws McpException {
        ensureInitialized();
        return client.readResource(uri);
    }

    /**
     * 确保已初始化
     */
    private void ensureInitialized() throws McpException {
        if (!initialized) {
            initialize();
        }
    }

    @Override
    public String toString() {
        return "McpToolRegistryWrapper{" +
                "protocol=" + config.getProtocol() +
                ", entrypoint='" + config.getEntrypoint() + '\'' +
                ", toolCount=" + registeredTools.size() +
                ", initialized=" + initialized +
                '}';
    }
}
