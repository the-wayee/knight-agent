package org.cloudnook.knightagent.workflow.nodes.io;

import org.cloudnook.knightagent.workflow.node.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 输出节点
 * 用于定义工作流的输出参数
 */
public class OutputNode extends AbstractNode<OutputNodeConfig> {

    public OutputNode() {
        super(null, null, null);
    }

    public OutputNode(String id, String name, OutputNodeConfig config) {
        super(id, name, config);
    }

    @Override
    public NodeType getType() {
        return NodeType.OUTPUT;
    }

    @Override
    protected NodeExecutionResult doExecute(NodeContext context) {
        OutputNodeConfig config = getConfig();
        Map<String, Object> output = new HashMap<>();

        // 如果 config 为 null，直接传递输入
        if (config == null || config.getOutputMapping() == null) {
            output.putAll(context.getInput());
        } else {
            for (Map.Entry<String, String> entry : config.getOutputMapping().entrySet()) {
                String outputKey = entry.getKey();
                String variableRef = entry.getValue();

                // 解析变量引用
                Object value = resolveVariable(variableRef, context);
                output.put(outputKey, value);
            }
        }

        return NodeExecutionResult.builder()
                .nodeId(context.getNodeId())
                .status(ExecutionStatus.COMPLETED)
                .output(output)
                .build();
    }

    /**
     * 解析变量引用
     */
    private Object resolveVariable(String ref, NodeContext context) {
        if (ref == null || ref.isBlank()) {
            return null;
        }

        // 简单的变量解析
        // 支持: input.field, nodeId.field
        if (ref.startsWith("{{") && ref.endsWith("}}")) {
            ref = ref.substring(2, ref.length() - 2).trim();
        }

        String[] parts = ref.split("\\.");
        if (parts.length < 2) {
            return context.getInput().get(ref);
        }

        String source = parts[0];
        String field = parts[1];

        return switch (source) {
            case "input" -> context.getWorkflowInput().get(field);
            case "context" -> context.getVariable(field);
            default -> {
                Map<String, Object> nodeOutput = context.getNodeOutput(source);
                yield nodeOutput != null ? nodeOutput.get(field) : null;
            }
        };
    }
}
