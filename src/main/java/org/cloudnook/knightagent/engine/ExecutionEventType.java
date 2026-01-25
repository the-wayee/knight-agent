package org.cloudnook.knightagent.engine;

/**
 * 执行事件类型（API层）
 */
public enum ExecutionEventType {
    // 工作流级别
    WORKFLOW_STARTED,
    WORKFLOW_COMPLETED,
    WORKFLOW_FAILED,

    // 节点级别
    NODE_STARTED,
    NODE_COMPLETED,
    NODE_FAILED,

    // 流式输出
    TOKEN,
}
