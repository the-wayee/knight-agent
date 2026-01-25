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
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * MCP工具引用列表
     */
    private List<McpToolRef> mcpTools;

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
