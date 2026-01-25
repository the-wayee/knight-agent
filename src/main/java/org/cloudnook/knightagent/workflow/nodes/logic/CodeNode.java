package org.cloudnook.knightagent.workflow.nodes.logic;

import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.workflow.node.AbstractNode;
import org.cloudnook.knightagent.workflow.node.ExecutionStatus;
import org.cloudnook.knightagent.workflow.node.NodeContext;
import org.cloudnook.knightagent.workflow.node.NodeExecutionResult;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.HashMap;
import java.util.Map;

/**
 * 代码节点
 * 执行JavaScript代码片段
 */
@Slf4j
public class CodeNode extends AbstractNode<CodeNodeConfig> {

    private final ScriptEngine engine;

    public CodeNode() {
        super(null, null, null);
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("js");
    }

    public CodeNode(String id, String name, CodeNodeConfig config) {
        super(id, name, config);
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("js");
    }

    @Override
    public org.cloudnook.knightagent.workflow.node.NodeType getType() {
        return org.cloudnook.knightagent.workflow.node.NodeType.CODE;
    }

    @Override
    protected NodeExecutionResult doExecute(NodeContext context) {
        CodeNodeConfig config = getConfig();

        try {
            // 准备输入绑定
            SimpleBindings bindings = new SimpleBindings();
            bindings.put("input", context.getInput());
            bindings.put("context", context.getVariables());

            // 添加输入映射
            if (config.getInputMapping() != null) {
                for (Map.Entry<String, String> entry : config.getInputMapping().entrySet()) {
                    String varName = entry.getKey();
                    String varRef = entry.getValue();
                    Object value = resolveVariable(varRef, context);
                    bindings.put(varName, value);
                }
            }

            // 执行代码
            Object result = engine.eval(config.getCode(), bindings);

            // 收集输出
            Map<String, Object> output = new HashMap<>();

            if (config.getOutputMapping() != null) {
                for (Map.Entry<String, String> entry : config.getOutputMapping().entrySet()) {
                    String outputField = entry.getKey();
                    String varName = entry.getValue();
                    output.put(outputField, bindings.get(varName));
                }
            } else if (result != null) {
                // 如果没有配置输出映射，使用执行结果
                output.put("result", result);
            }

            return NodeExecutionResult.builder()
                    .nodeId(context.getNodeId())
                    .status(ExecutionStatus.COMPLETED)
                    .output(output)
                    .build();

        } catch (ScriptException e) {
            log.error("Code execution failed", e);
            return NodeExecutionResult.failure(context.getNodeId(), e.getMessage());
        }
    }

    /**
     * 解析变量引用
     */
    private Object resolveVariable(String ref, NodeContext context) {
        if (ref == null || ref.isBlank()) {
            return null;
        }

        // 简单的变量解析
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
