package org.cloudnook.knightagent.workflow.nodes.io;

import org.cloudnook.knightagent.workflow.node.*;

import java.util.Map;

/**
 * 输入节点
 * 用于定义工作流的输入参数
 */
public class InputNode extends AbstractNode<InputNodeConfig> {

    public InputNode() {
        super(null, null, null);
    }

    public InputNode(String id, String name, InputNodeConfig config) {
        super(id, name, config);
    }

    @Override
    public NodeType getType() {
        return NodeType.INPUT;
    }

    @Override
    protected NodeExecutionResult doExecute(NodeContext context) {
        // 输入节点直接传递工作流输入
        Map<String, Object> output = context.getWorkflowInput();

        return NodeExecutionResult.builder()
                .nodeId(context.getNodeId())
                .status(ExecutionStatus.COMPLETED)
                .output(output)
                .build();
    }
}
