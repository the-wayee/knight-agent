package org.cloudnook.knightagent.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 节点定义DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeDefinitionDTO {

    /**
     * 节点ID
     */
    private String id;

    /**
     * 节点类型
     */
    private String type;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 画布位置
     */
    private PositionDTO position;

    /**
     * 节点配置
     */
    private Map<String, Object> config;

    /**
     * 是否为起始节点
     */
    private Boolean isStart;

    /**
     * 是否为结束节点
     */
    private Boolean isEnd;
}
