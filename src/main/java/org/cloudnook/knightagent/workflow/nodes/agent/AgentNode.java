package org.cloudnook.knightagent.workflow.nodes.agent;

import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.core.agent.Agent;
import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.factory.AgentFactory;
import org.cloudnook.knightagent.api.service.ApiKeyService;
import org.cloudnook.knightagent.api.entity.ApiKey;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.OpenAIChatModel;
import org.cloudnook.knightagent.workflow.node.AbstractNode;
import org.cloudnook.knightagent.workflow.node.NodeContext;
import org.cloudnook.knightagent.workflow.node.NodeExecutionResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent节点
 * 执行AI Agent
 */
@Slf4j
public class AgentNode extends AbstractNode<AgentNodeConfig> {

    private final AgentFactory agentFactory;
    private final ApiKeyService apiKeyService;
    private final Map<String, ChatModel> modelRegistry;

    public AgentNode(AgentFactory agentFactory) {
        super(null, null, null);
        this.agentFactory = agentFactory;
        this.apiKeyService = null;
        this.modelRegistry = new HashMap<>();
    }

    public AgentNode(String id, String name, AgentNodeConfig config, AgentFactory agentFactory) {
        super(id, name, config);
        this.agentFactory = agentFactory;
        this.apiKeyService = null;
        this.modelRegistry = new HashMap<>();
    }

    public AgentNode(String id, String name, AgentNodeConfig config, AgentFactory agentFactory, ApiKeyService apiKeyService) {
        super(id, name, config);
        this.agentFactory = agentFactory;
        this.apiKeyService = apiKeyService;
        this.modelRegistry = new HashMap<>();
    }

    /**
     * 注册模型
     */
    public void registerModel(String modelId, ChatModel model) {
        modelRegistry.put(modelId, model);
    }

    @Override
    public org.cloudnook.knightagent.workflow.node.NodeType getType() {
        return org.cloudnook.knightagent.workflow.node.NodeType.AGENT;
    }

    @Override
    protected NodeExecutionResult doExecute(NodeContext context) {
        AgentNodeConfig config = getConfig();

        try {
            // 获取或创建模型
            ChatModel model = getOrCreateModel(config);

            // 构建Agent配置
            AgentConfig.Builder agentConfigBuilder = AgentConfig.builder();
            if (config.getSystemPrompt() != null) {
                agentConfigBuilder.systemPrompt(config.getSystemPrompt());
            }
            if (config.getMaxIterations() != null) {
                agentConfigBuilder.maxIterations(config.getMaxIterations());
            }

            // 创建Agent
            Agent agent = agentFactory.createAgent()
                    .model(model)
                    .config(agentConfigBuilder.build())
                    .build();

            // 准备输入
            String input = buildInput(context);

            // 执行Agent
            AgentRequest request = AgentRequest.of(input);
            AgentResponse response = agent.invoke(request);

            // 构建输出
            Map<String, Object> output = new HashMap<>();
            output.put("output", response.getOutput());
            output.put("messages", response.getMessages());
            if (response.getState() != null) {
                output.put("state", response.getState().getData());
            }
            if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
                output.put("toolCalls", response.getToolCalls());
            }

            return NodeExecutionResult.builder()
                    .nodeId(context.getNodeId())
                    .status(org.cloudnook.knightagent.workflow.node.ExecutionStatus.COMPLETED)
                    .output(output)
                    .build();

        } catch (Exception e) {
            log.error("Agent node execution failed", e);
            return NodeExecutionResult.failure(context.getNodeId(), e.getMessage());
        }
    }

    /**
     * 获取或创建模型实例
     */
    private ChatModel getOrCreateModel(AgentNodeConfig config) {
        // config 可能为 null，需要检查
        if (config == null) {
            throw new IllegalArgumentException("Agent node config is required. Please configure the node with an API Key.");
        }

        // 优先使用 apiKeyId 从数据库获取配置
        if (config.getApiKeyId() != null && !config.getApiKeyId().isEmpty() && apiKeyService != null) {
            ApiKey apiKeyEntity = apiKeyService.getEntityByUuid(config.getApiKeyId());
            if (apiKeyEntity != null) {
                OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                        .apiKey(apiKeyEntity.getApiKey())
                        .modelId(config.getModel() != null ? config.getModel() : apiKeyEntity.getModelId());

                if (apiKeyEntity.getBaseUrl() != null && !apiKeyEntity.getBaseUrl().isEmpty()) {
                    builder.baseUrl(apiKeyEntity.getBaseUrl());
                }

                return builder.build();
            }
            throw new IllegalArgumentException("API Key not found: " + config.getApiKeyId());
        }

        // 兼容旧配置：如果配置了直接的 API 信息
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelId(config.getModel() != null ? config.getModel() : "gpt-3.5-turbo");

            if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
                builder.baseUrl(config.getBaseUrl());
            }

            return builder.build();
        }

        // 否则从注册表中获取模型
        ChatModel model = modelRegistry.get(config.getModel());
        if (model == null) {
            throw new IllegalArgumentException(
                "Model not found: " + config.getModel() + ". " +
                "Please configure apiKey in the node settings."
            );
        }
        return model;
    }

    /**
     * 构建Agent输入
     */
    private String buildInput(NodeContext context) {
        // 从输入中获取用户消息
        Object userInput = context.getInput().get("input");
        if (userInput != null) {
            return userInput.toString();
        }

        // 如果没有明确的input字段，将整个输入转为字符串
        if (context.getInput() != null && !context.getInput().isEmpty()) {
            return context.getInput().toString();
        }

        return "";
    }
}
