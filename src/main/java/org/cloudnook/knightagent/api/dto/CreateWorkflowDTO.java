package org.cloudnook.knightagent.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 创建工作流DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowDTO {

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 工作流描述
     */
    private String description;

    /**
     * 节点列表
     */
    private List<NodeDefinitionDTO> nodes;

    /**
     * 连接边列表
     */
    private List<EdgeDefinitionDTO> edges;

    /**
     * 全局设置
     */
    private Map<String, Object> settings;

    /**
     * 标签
     */
    private List<String> tags;
}
