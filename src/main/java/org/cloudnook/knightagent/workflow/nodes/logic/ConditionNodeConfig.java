package org.cloudnook.knightagent.workflow.nodes.logic;

import lombok.Data;
import org.cloudnook.knightagent.workflow.node.NodeConfig;

import java.util.Map;

/**
 * 条件节点配置
 */
@Data
public class ConditionNodeConfig extends NodeConfig {

    /**
     * 条件表达式
     * 支持简单的变量比较，如: {{input.value}} > 10
     */
    private String condition;

    /**
     * 变量引用（用于解析条件表达式）
     */
    private Map<String, String> variableMapping;
}
