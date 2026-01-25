package org.cloudnook.knightagent.workflow.node;

/**
 * 工作流节点接口
 */
public interface WorkflowNode {

    /**
     * 获取节点ID
     */
    String getId();

    /**
     * 获取节点类型
     */
    NodeType getType();

    /**
     * 获取节点名称
     */
    String getName();

    /**
     * 执行节点
     *
     * @param context 执行上下文
     * @return 执行结果
     */
    NodeExecutionResult execute(NodeContext context);

    /**
     * 获取节点配置
     */
    NodeConfig getConfig();

    /**
     * 设置节点配置
     */
    void setConfig(NodeConfig config);
}
