package org.cloudnook.knightagent.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 工作流DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDTO {

    /**
     * 工作流ID
     */
    private String id;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 工作流描述
     */
    private String description;

    /**
     * 版本号
     */
    private Integer version;

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

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    /**
     * 创建者
     */
    private String createdBy;
}
