package org.cloudnook.knightagent.workflow.node;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 节点抽象基类
 */
@Slf4j
@Data
public abstract class AbstractNode<T extends NodeConfig> implements WorkflowNode {

    protected String id;
    protected String name;
    protected T config;

    protected AbstractNode(String id, String name, T config) {
        this.id = id;
        this.name = name;
        this.config = config;
    }

    @Override
    public abstract NodeType getType();

    @Override
    public NodeExecutionResult execute(NodeContext context) {
        log.debug("Executing node: {} ({})", name, id);
        try {
            NodeExecutionResult result = doExecute(context);
            log.debug("Node {} completed with status: {}", id, result.getStatus());
            return result;
        } catch (Exception e) {
            log.error("Node {} execution failed", id, e);
            return NodeExecutionResult.failure(id, e.getMessage());
        }
    }

    /**
     * 子类实现具体执行逻辑
     */
    protected abstract NodeExecutionResult doExecute(NodeContext context);

    @Override
    @SuppressWarnings("unchecked")
    public void setConfig(NodeConfig config) {
        this.config = (T) config;
    }
}
