package org.cloudnook.knightagent.workflow.nodes.agent;

import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.api.dto.config.McpServerDTO;
import org.cloudnook.knightagent.api.service.ApiKeyService;
import org.cloudnook.knightagent.api.service.McpServerService;
import org.cloudnook.knightagent.api.entity.ApiKey;
import org.cloudnook.knightagent.core.agent.Agent;
import org.cloudnook.knightagent.core.agent.AgentConfig;
import org.cloudnook.knightagent.core.agent.AgentRequest;
import org.cloudnook.knightagent.core.agent.AgentResponse;
import org.cloudnook.knightagent.core.agent.factory.AgentBuilder;
import org.cloudnook.knightagent.core.agent.factory.AgentFactory;
import org.cloudnook.knightagent.core.agent.strategy.ReActStrategy;
import org.cloudnook.knightagent.core.mcp.McpConfig;
import org.cloudnook.knightagent.core.mcp.McpProtocol;
import org.cloudnook.knightagent.core.mcp.McpToolRegistryWrapper;
import org.cloudnook.knightagent.core.model.ChatModel;
import org.cloudnook.knightagent.core.model.OpenAIChatModel;
import org.cloudnook.knightagent.core.tool.McpTool;
import org.cloudnook.knightagent.core.streaming.StreamChunk;
import org.cloudnook.knightagent.workflow.node.AbstractNode;
import org.cloudnook.knightagent.workflow.node.NodeContext;
import org.cloudnook.knightagent.workflow.node.NodeExecutionResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent节点
 * 执行AI Agent
 */
@Slf4j
public class AgentNode extends AbstractNode<AgentNodeConfig> {

    private final AgentFactory agentFactory;
    private final ApiKeyService apiKeyService;
    private final McpServerService mcpServerService;
    private final Map<String, ChatModel> modelRegistry;

    public AgentNode(AgentFactory agentFactory) {
        super(null, null, null);
        this.agentFactory = agentFactory;
        this.apiKeyService = null;
        this.mcpServerService = null;
        this.modelRegistry = new HashMap<>();
    }

    public AgentNode(String id, String name, AgentNodeConfig config, AgentFactory agentFactory) {
        super(id, name, config);
        this.agentFactory = agentFactory;
        this.apiKeyService = null;
        this.mcpServerService = null;
        this.modelRegistry = new HashMap<>();
    }

    public AgentNode(String id, String name, AgentNodeConfig config, AgentFactory agentFactory, ApiKeyService apiKeyService) {
        super(id, name, config);
        this.agentFactory = agentFactory;
        this.apiKeyService = apiKeyService;
        this.mcpServerService = null;
        this.modelRegistry = new HashMap<>();
    }

    public AgentNode(String id, String name, AgentNodeConfig config,
                     AgentFactory agentFactory, ApiKeyService apiKeyService,
                     McpServerService mcpServerService) {
        super(id, name, config);
        this.agentFactory = agentFactory;
        this.apiKeyService = apiKeyService;
        this.mcpServerService = mcpServerService;
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

        // 收集需要清理的 MCP 注册表
        List<McpToolRegistryWrapper> tempRegistries = new ArrayList<>();

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

            // 创建 Agent 构建器
            AgentBuilder builder = agentFactory.createAgent()
                    .model(model)
                    .config(agentConfigBuilder.build());

            // 设置策略 (默认 ReAct)
            if ("REACT".equalsIgnoreCase(config.getStrategy())) {
                builder.strategy(new ReActStrategy());
            } else {
                builder.strategy(new ReActStrategy());
            }

            // 处理 MCP 工具
            List<AgentNodeConfig.McpToolRef> mcpToolRefs = normalizeMcpTools(config);
            if (!mcpToolRefs.isEmpty() && mcpServerService != null) {
                List<McpTool> allMcpTools = new ArrayList<>();

                for (AgentNodeConfig.McpToolRef toolRef : mcpToolRefs) {
                    try {
                        // 获取服务器配置
                        McpServerDTO serverDTO = mcpServerService.getByUuid(toolRef.getServerId());
                        if (serverDTO == null) {
                            log.warn("MCP Server not found: {}", toolRef.getServerId());
                            continue;
                        }

                        // 连接到 MCP 服务器
                        McpConfig mcpConfig = McpConfig.builder()
                                .protocol(detectProtocol(serverDTO.getUrl()))
                                .entrypoint(extractEntrypoint(serverDTO.getUrl()))
                                .build();

                        McpToolRegistryWrapper registry = new McpToolRegistryWrapper(mcpConfig);
                        registry.initialize();
                        tempRegistries.add(registry);

                        // 获取并过滤工具
                        List<McpTool> availableTools = registry.getRegisteredTools();
                        if (toolRef.getTools() != null && !toolRef.getTools().isEmpty()) {
                            List<McpTool> selectedTools = availableTools.stream()
                                    .filter(t -> toolRef.getTools().contains(t.getName()))
                                    .collect(Collectors.toList());
                            allMcpTools.addAll(selectedTools);
                        } else {
                            // 如果没有指定具体工具，则加载全部
                            allMcpTools.addAll(availableTools);
                        }

                    } catch (Exception e) {
                        log.error("Failed to load MCP tools for server: {}", toolRef.getServerId(), e);
                    }
                }

                builder.tools(allMcpTools);
            }

            // 创建Agent
            Agent agent = builder.build();

            // 准备输入
            String input = buildInput(context);
            AgentRequest request = AgentRequest.of(input);

            // 使用流式执行（即使是同步等待，也通过 callback 获取事件）
            AgentResponse response = agent.stream(request, new org.cloudnook.knightagent.core.streaming.StreamCallback() {
                @Override
                public void onToken(StreamChunk chunk) {
                    if (context.getEventConsumer() != null) {
                        try {
                            String token = chunk.getContent() != null ? chunk.getContent() : "";
                            context.getEventConsumer().accept(
                                org.cloudnook.knightagent.workflow.engine.ExecutionEvent.token(
                                    context.getExecutionId(), context.getNodeId(), token
                                )
                            );
                        } catch (Exception e) {
                            log.warn("Failed to send token event", e);
                        }
                    }
                }

                @Override
                public void onToolCall(StreamChunk chunk, org.cloudnook.knightagent.core.message.ToolCall toolCall) {
                    if (context.getEventConsumer() != null) {
                        try {
                            // 将参数 Map 转换为 JSON 字符串
                            String argsJson = "{}";
                            if (toolCall.getArguments() != null) {
                                try {
                                    argsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(toolCall.getArguments());
                                } catch (Exception e) {
                                    log.warn("Failed to serialize tool arguments", e);
                                    argsJson = toolCall.getArguments().toString();
                                }
                            }

                            context.getEventConsumer().accept(
                                org.cloudnook.knightagent.workflow.engine.ExecutionEvent.toolCall(
                                    context.getExecutionId(), context.getNodeId(),
                                    toolCall.getName(), argsJson
                                )
                            );
                        } catch (Exception e) {
                            log.warn("Failed to send tool call event", e);
                        }
                    }
                }

                @Override
                public void onReasoning(StreamChunk chunk, String reasoning) {
                     if (context.getEventConsumer() != null) {
                        try {
                            context.getEventConsumer().accept(
                                org.cloudnook.knightagent.workflow.engine.ExecutionEvent.reasoning(
                                    context.getExecutionId(), context.getNodeId(), reasoning
                                )
                            );
                        } catch (Exception e) {
                            log.warn("Failed to send reasoning event", e);
                        }
                    }
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Agent stream error", error);
                }
            });

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
        } finally {
            // 清理临时创建的 MCP 连接
            for (McpToolRegistryWrapper registry : tempRegistries) {
                try {
                    registry.close();
                } catch (Exception e) {
                    log.warn("Failed to close MCP registry", e);
                }
            }
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
            return extractStringValue(userInput);
        }

        // 尝试查找常见的消息字段
        if (context.getInput().containsKey("message")) {
            return extractStringValue(context.getInput().get("message"));
        }

        // 如果没有明确的input字段，将整个输入转为字符串
        if (context.getInput() != null && !context.getInput().isEmpty()) {
            // 如果 input 只有一个条目，直接返回该条目的值
            if (context.getInput().size() == 1) {
                return extractStringValue(context.getInput().values().iterator().next());
            }
            return context.getInput().toString();
        }

        return "";
    }

    private String extractStringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            // 如果 Map 包含 message 字段，提取它
            if (map.containsKey("message")) {
                return extractStringValue(map.get("message"));
            }
            // 否则尝试 input 字段
            if (map.containsKey("input")) {
                return extractStringValue(map.get("input"));
            }
        }
        return value.toString();
    }

    private McpProtocol detectProtocol(String url) {
        if (url.startsWith("stdio://")) {
            return McpProtocol.STDIO;
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            return McpProtocol.STREAMABLE_HTTP;
        } else if (url.startsWith("ws://") || url.startsWith("wss://")) {
            return McpProtocol.WS;
        } else {
            return McpProtocol.STDIO;
        }
    }

    private String extractEntrypoint(String url) {
        if (url.startsWith("stdio://")) {
            return url.substring(8);
        }
        return url;
    }

    /**
     * 标准化 MCP 工具配置
     * 将新旧格式统一转换为 McpToolRef 列表
     */
    private List<AgentNodeConfig.McpToolRef> normalizeMcpTools(AgentNodeConfig config) {
        List<AgentNodeConfig.McpToolRef> result = new ArrayList<>();

        // 1. 处理旧格式 (mcpTools)
        if (config.getMcpTools() != null) {
            result.addAll(config.getMcpTools());
        }

        // 2. 处理新格式 (tools: ["serverId/toolName", ...])
        if (config.getTools() != null && !config.getTools().isEmpty()) {
            Map<String, List<String>> serverToolsMap = new HashMap<>();

            for (String toolStr : config.getTools()) {
                int slashIndex = toolStr.indexOf('/');
                if (slashIndex > 0) {
                    String serverId = toolStr.substring(0, slashIndex);
                    String toolName = toolStr.substring(slashIndex + 1);
                    serverToolsMap.computeIfAbsent(serverId, k -> new ArrayList<>()).add(toolName);
                }
            }

            for (Map.Entry<String, List<String>> entry : serverToolsMap.entrySet()) {
                AgentNodeConfig.McpToolRef ref = new AgentNodeConfig.McpToolRef();
                ref.setServerId(entry.getKey());
                ref.setTools(entry.getValue());
                result.add(ref);
            }
        }

        return result;
    }
}
