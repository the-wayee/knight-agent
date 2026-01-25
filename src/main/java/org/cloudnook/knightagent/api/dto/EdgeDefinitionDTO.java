package org.cloudnook.knightagent.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 连接边定义DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeDefinitionDTO {

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
     * 源锚点
     */
    private String sourceHandle;

    /**
     * 目标锚点
     */
    private String targetHandle;

    /**
     * 条件表达式
     */
    private String condition;
}
