package org.cloudnook.knightagent.workflow.nodes.io;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cloudnook.knightagent.workflow.node.NodeConfig;

import java.util.Map;

/**
 * 输出节点配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OutputNodeConfig extends NodeConfig {

    /**
     * 输出映射配置
     * key: 输出字段名
     * value: 变量引用 (如 {{input.field}}, {{nodeId.field}})
     */
    private Map<String, String> outputMapping;

    /**
     * 输出Schema定义
     */
    private String schema;
}
