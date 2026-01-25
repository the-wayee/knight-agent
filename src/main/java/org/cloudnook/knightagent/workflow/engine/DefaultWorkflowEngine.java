package org.cloudnook.knightagent.workflow.engine;

import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.workflow.definition.WorkflowDefinition;
import org.cloudnook.knightagent.workflow.definition.NodeDefinition;
import org.cloudnook.knightagent.workflow.definition.ValidationResult;
import org.cloudnook.knightagent.workflow.node.*;
import org.cloudnook.knightagent.workflow.nodes.io.InputNode;
import org.cloudnook.knightagent.workflow.nodes.io.OutputNode;
import org.cloudnook.knightagent.workflow.nodes.agent.AgentNode;
import org.cloudnook.knightagent.workflow.nodes.logic.CodeNode;
import org.cloudnook.knightagent.workflow.nodes.logic.ConditionNode;
import org.cloudnook.knightagent.workflow.nodes.external.HttpNode;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 默认工作流执行引擎
 */
@Slf4j
@Component
public class DefaultWorkflowEngine implements WorkflowEngine {

    private final Map<String, WorkflowNode> nodeRegistry = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private org.cloudnook.knightagent.core.agent.factory.AgentFactory agentFactory;

    @Autowired(required = false)
    private org.cloudnook.knightagent.api.service.ApiKeyService apiKeyService;

    /**
     * 注册节点
     */
    public void registerNode(WorkflowNode node) {
        nodeRegistry.put(node.getType().getCode(), node);
        log.info("Registered node: {} ({})", node.getName(), node.getType());
    }

    /**
     * 获取节点实例
     */
    @SuppressWarnings("unchecked")
    public <T extends WorkflowNode> T getNodeInstance(NodeDefinition nodeDef, Function<NodeDefinition, T> factory) {
        return factory.apply(nodeDef);
    }

    @Override
    public ExecutionResult execute(WorkflowDefinition workflow, Map<String, Object> input) {
        return executeStream(workflow, input, null);
    }

    @Override
    public CompletableFuture<ExecutionResult> executeAsync(WorkflowDefinition workflow, Map<String, Object> input) {
        return CompletableFuture.supplyAsync(() -> execute(workflow, input));
    }

    @Override
    public ExecutionResult executeStream(WorkflowDefinition workflow, Map<String, Object> input,
                                         Consumer<ExecutionEvent> eventConsumer) {
        // 生成执行ID
        String executionId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        // 发送开始事件
        publishEvent(eventConsumer, ExecutionEvent.workflowStarted(executionId));

        // 验证工作流
        ValidationResult validation = workflow.validate();
        if (!validation.isValid()) {
            ExecutionEvent event = ExecutionEvent.workflowFailed(executionId,
                    "Workflow validation failed: " + String.join(", ", validation.getErrors()));
            publishEvent(eventConsumer, event);
            return ExecutionResult.failure(executionId, workflow.getId(), input, event.getError());
        }

        // 构建执行上下文
        ExecutionContext context = ExecutionContext.builder()
                .executionId(executionId)
                .workflowId(workflow.getId())
                .input(input)
                .globalConfig(workflow.getSettings())
                .build();

        List<NodeExecutionResult> nodeResults = new ArrayList<>();

        try {
            // 拓扑排序获取执行顺序
            List<String> executionOrder = topologicalSort(workflow);
            log.info("Execution order: {} for workflow: {}", executionOrder, workflow.getId());

            // 按顺序执行节点
            for (String nodeId : executionOrder) {
                // 检查是否取消
                if (!context.shouldContinue()) {
                    throw new IllegalStateException("Execution was cancelled");
                }

                // 获取节点定义
                NodeDefinition nodeDef = workflow.getNode(nodeId);
                if (nodeDef == null) {
                    throw new IllegalStateException("Node not found: " + nodeId);
                }

                // 执行节点
                NodeExecutionResult result = executeNode(workflow, nodeDef, context, eventConsumer);
                nodeResults.add(result);

                // 检查节点是否执行成功
                if (result.getStatus() != org.cloudnook.knightagent.workflow.node.ExecutionStatus.COMPLETED) {
                    throw new IllegalStateException("Node execution failed: " + result.getError());
                }
            }

            // 获取最终输出
            Map<String, Object> output = collectOutput(workflow, context);

            Instant endTime = Instant.now();
            ExecutionResult result = ExecutionResult.success(executionId, workflow.getId(), input, output, nodeResults);
            result.setStartTime(startTime);
            result.setEndTime(endTime);
            result.calculateDuration();

            publishEvent(eventConsumer, ExecutionEvent.workflowCompleted(executionId, output));

            return result;

        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            ExecutionEvent event = ExecutionEvent.workflowFailed(executionId, e.getMessage());
            publishEvent(eventConsumer, event);

            ExecutionResult result = ExecutionResult.failure(executionId, workflow.getId(), input, e.getMessage());
            result.setStartTime(startTime);
            result.setEndTime(Instant.now());
            result.calculateDuration();
            result.setNodeResults(nodeResults);
            return result;
        }
    }

    @Override
    public Publisher<ExecutionEvent> executeStreamPublisher(WorkflowDefinition workflow, Map<String, Object> input) {
        return subscriber -> {
            List<ExecutionEvent> events = new ArrayList<>();
            Consumer<ExecutionEvent> collector = events::add;

            ExecutionResult result = executeStream(workflow, input, collector);

            events.forEach(subscriber::onNext);
            subscriber.onComplete();
        };
    }

    /**
     * 执行单个节点
     */
    private NodeExecutionResult executeNode(WorkflowDefinition workflow,
                                            NodeDefinition nodeDef,
                                            ExecutionContext context,
                                            Consumer<ExecutionEvent> eventConsumer) {
        String nodeId = nodeDef.getId();
        Instant startTime = Instant.now();

        // 获取节点输入
        Map<String, Object> nodeInput = prepareNodeInput(workflow, nodeDef, context);

        // 发送节点开始事件
        publishEvent(eventConsumer, ExecutionEvent.nodeStarted(
                context.getExecutionId(), nodeId, nodeDef.getName(), nodeDef.getType(), nodeInput));

        // 构建节点上下文
        NodeContext nodeContext = NodeContext.builder()
                .executionId(context.getExecutionId())
                .nodeId(nodeId)
                .workflowInput(context.getInput())
                .input(nodeInput)
                .variables(new HashMap<>(context.getNodeOutputs()))
                .globalConfig(context.getGlobalConfig())
                .build();

        // 执行节点
        org.cloudnook.knightagent.workflow.node.NodeExecutionResult nodeResult;
        try {
            WorkflowNode node = createNode(nodeDef);
            nodeResult = node.execute(nodeContext);

            // 存储节点输出
            if (nodeResult.getOutput() != null) {
                context.setNodeOutput(nodeId, nodeResult.getOutput());
            }

        } catch (Exception e) {
            log.error("Node execution failed: {}", nodeId, e);
            nodeResult = org.cloudnook.knightagent.workflow.node.NodeExecutionResult.builder()
                    .nodeId(nodeId)
                    .status(org.cloudnook.knightagent.workflow.node.ExecutionStatus.FAILED)
                    .error(e.getMessage())
                    .build();
        }

        Instant endTime = Instant.now();

        // 构建节点执行结果
        NodeExecutionResult result = NodeExecutionResult.builder()
                .nodeId(nodeId)
                .nodeName(nodeDef.getName())
                .status(nodeResult.getStatus())
                .input(nodeInput)
                .output(nodeResult.getOutput())
                .error(nodeResult.getError())
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
                .build();

        // 发送节点完成事件
        if (result.getStatus() == org.cloudnook.knightagent.workflow.node.ExecutionStatus.COMPLETED) {
            publishEvent(eventConsumer, ExecutionEvent.nodeCompleted(
                    context.getExecutionId(), nodeId, nodeDef.getName(), result.getOutput()));
        } else {
            publishEvent(eventConsumer, ExecutionEvent.nodeFailed(
                    context.getExecutionId(), nodeId, nodeDef.getName(), result.getError()));
        }

        return result;
    }

    /**
     * 创建节点实例
     */
    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * 创建节点实例
     */
    private WorkflowNode createNode(NodeDefinition nodeDef) {
        NodeType nodeType = NodeType.fromCode(nodeDef.getType());
        Map<String, Object> configMap = nodeDef.getConfig();

        return switch (nodeType) {
            case INPUT -> {
                 org.cloudnook.knightagent.workflow.nodes.io.InputNodeConfig config = convertConfig(configMap, org.cloudnook.knightagent.workflow.nodes.io.InputNodeConfig.class);
                 yield new InputNode(nodeDef.getId(), nodeDef.getName(), config);
            }
            case OUTPUT -> {
                org.cloudnook.knightagent.workflow.nodes.io.OutputNodeConfig config = convertConfig(configMap, org.cloudnook.knightagent.workflow.nodes.io.OutputNodeConfig.class);
                yield new OutputNode(nodeDef.getId(), nodeDef.getName(), config);
            }
            case AGENT -> {
                org.cloudnook.knightagent.workflow.nodes.agent.AgentNodeConfig config = convertConfig(configMap, org.cloudnook.knightagent.workflow.nodes.agent.AgentNodeConfig.class);
                yield new AgentNode(nodeDef.getId(), nodeDef.getName(), config, agentFactory, apiKeyService);
            }
            case CODE -> {
                org.cloudnook.knightagent.workflow.nodes.logic.CodeNodeConfig config = convertConfig(configMap, org.cloudnook.knightagent.workflow.nodes.logic.CodeNodeConfig.class);
                yield new CodeNode(nodeDef.getId(), nodeDef.getName(), config);
            }
            case CONDITION -> {
                org.cloudnook.knightagent.workflow.nodes.logic.ConditionNodeConfig config = convertConfig(configMap, org.cloudnook.knightagent.workflow.nodes.logic.ConditionNodeConfig.class);
                yield new ConditionNode(nodeDef.getId(), nodeDef.getName(), config);
            }
            case HTTP -> {
                org.cloudnook.knightagent.workflow.nodes.external.HttpNodeConfig config = convertConfig(configMap, org.cloudnook.knightagent.workflow.nodes.external.HttpNodeConfig.class);
                yield new HttpNode(nodeDef.getId(), nodeDef.getName(), config);
            }
            case TOOL -> {
                log.warn("Tool node not yet implemented, using passthrough");
                yield new InputNode(nodeDef.getId(), nodeDef.getName(), null);
            }
        };
    }

    private <T> T convertConfig(Map<String, Object> configMap, Class<T> targetType) {
        if (configMap == null) {
            return null;
        }
        return objectMapper.convertValue(configMap, targetType);
    }

    /**
     * 准备节点输入
     */
    private Map<String, Object> prepareNodeInput(WorkflowDefinition workflow, NodeDefinition nodeDef, ExecutionContext context) {
        Map<String, Object> input = new HashMap<>();

        // 添加工作流输入
        if (context.getInput() != null) {
            input.putAll(context.getInput());
        }

        // 添加上游节点的输出
        List<org.cloudnook.knightagent.workflow.definition.EdgeDefinition> inputEdges = workflow.getInputEdges(nodeDef.getId());
        for (org.cloudnook.knightagent.workflow.definition.EdgeDefinition edge : inputEdges) {
            Map<String, Object> sourceOutput = context.getNodeOutput(edge.getSource());
            if (sourceOutput != null) {
                input.putAll(sourceOutput);
            }
        }

        return input;
    }

    /**
     * 收集工作流输出
     */
    private Map<String, Object> collectOutput(WorkflowDefinition workflow, ExecutionContext context) {
        // 获取所有结束节点
        List<NodeDefinition> endNodes = workflow.getEndNodes();

        if (endNodes.isEmpty()) {
            // 如果没有结束节点，返回最后一个节点的输出
            return context.getNodeOutputs().values().stream()
                    .reduce((a, b) -> b)
                    .orElse(Map.of());
        }

        // 合并所有结束节点的输出
        Map<String, Object> output = new HashMap<>();
        for (NodeDefinition endNode : endNodes) {
            Map<String, Object> nodeOutput = context.getNodeOutput(endNode.getId());
            if (nodeOutput != null) {
                output.putAll(nodeOutput);
            }
        }
        return output;
    }

    /**
     * 拓扑排序
     */
    private List<String> topologicalSort(WorkflowDefinition workflow) {
        Map<String, Set<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // 初始化
        for (NodeDefinition node : workflow.getNodes()) {
            graph.put(node.getId(), new HashSet<>());
            inDegree.put(node.getId(), 0);
        }

        // 构建图
        for (org.cloudnook.knightagent.workflow.definition.EdgeDefinition edge : workflow.getEdges()) {
            graph.get(edge.getSource()).add(edge.getTarget());
            inDegree.put(edge.getTarget(), inDegree.get(edge.getTarget()) + 1);
        }

        // Kahn算法
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String node = queue.poll();
            result.add(node);

            for (String neighbor : graph.get(node)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // 检查循环
        if (result.size() != workflow.getNodes().size()) {
            throw new IllegalStateException("Workflow contains cycles");
        }

        return result;
    }

    /**
     * 发布事件
     */
    private void publishEvent(Consumer<ExecutionEvent> eventConsumer, ExecutionEvent event) {
        if (eventConsumer != null) {
            eventConsumer.accept(event);
        }
    }
}
