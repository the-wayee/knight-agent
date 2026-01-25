package org.cloudnook.knightagent.workflow.definition;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 节点定义
 */
@Data
@Builder
public class NodeDefinition {

    /**
     * 节点ID（唯一标识）
     */
    private String id;

    /**
     * 节点类型
     */
    private String type;

    /**
     * 节点名称（显示名称）
     */
    private String name;

    /**
     * 画布位置
     */
    private Point position;

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
