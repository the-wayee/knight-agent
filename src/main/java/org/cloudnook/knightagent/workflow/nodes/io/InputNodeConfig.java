package org.cloudnook.knightagent.workflow.nodes.io;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cloudnook.knightagent.workflow.node.NodeConfig;

/**
 * 输入节点配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InputNodeConfig extends NodeConfig {

    /**
     * 输入Schema定义（JSON Schema格式）
     */
    private String schema;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 输入描述
     */
    private String description;
}
