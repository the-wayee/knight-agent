package org.cloudnook.knightagent.workflow.nodes.agent;

import lombok.Data;
import org.cloudnook.knightagent.workflow.node.NodeConfig;

import java.util.List;
import java.util.Map;

/**
 * Agent节点配置
 */
@Data
public class AgentNodeConfig extends NodeConfig {

    /**
     * 模型ID
     */
    private String model;

    /**
     * Agent 策略 (如: "REACT")
     */
    private String strategy;

    /**
     * API Key ID (引用配置的 API Key)
     */
    private String apiKeyId;

    /**
     * API Base URL (用于自定义 API 端点，如 OpenAI 代理)
     */
    private String baseUrl;

    /**
     * API Key (兼容旧配置，直接存储的密钥)
     */
    private String apiKey;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * MCP工具引用列表
     */
    /**
     * MCP工具引用列表 (旧格式)
     */
    private List<McpToolRef> mcpTools;

    /**
     * 工具列表 (新格式: "serverId/toolName")
     */
    private List<String> tools;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 最大Token数
     */
    private Integer maxTokens;

    /**
     * 最大迭代次数
     */
    private Integer maxIterations;

    /**
     * 中间件配置
     */
    private List<String> middleware;

    /**
     * MCP工具引用
     */
    @Data
    public static class McpToolRef {
        /**
         * MCP服务器ID
         */
        private String serverId;

        /**
         * 工具名称列表
         */
        private List<String> tools;
    }
}
