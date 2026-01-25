package org.cloudnook.knightagent.workflow.definition;

import lombok.Builder;
import lombok.Data;

/**
 * 连接边定义
 */
@Data
@Builder
public class EdgeDefinition {

    /**
     * 边ID
     */
    private String id;

    /**
     * 源节点ID
     */
    private String source;

    /**
     * 目标节点ID
     */
    private String target;

    /**
     * 源锚点（可选）
     */
    private String sourceHandle;

    /**
     * 目标锚点（可选）
     */
    private String targetHandle;

    /**
     * 条件表达式（可选，用于条件节点）
     */
    private String condition;
}
