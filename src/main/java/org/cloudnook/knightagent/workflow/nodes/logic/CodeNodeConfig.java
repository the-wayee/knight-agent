package org.cloudnook.knightagent.workflow.nodes.logic;

import lombok.Data;
import org.cloudnook.knightagent.workflow.node.NodeConfig;

import java.util.List;
import java.util.Map;

/**
 * 代码节点配置
 */
@Data
public class CodeNodeConfig extends NodeConfig {

    /**
     * JavaScript代码
     */
    private String code;

    /**
     * 输入变量映射
     * key: 代码中的变量名
     * value: 工作流变量引用 (如 {{input.field}}, {{nodeId.field}})
     */
    private Map<String, String> inputMapping;

    /**
     * 输出变量映射
     * key: 工作流输出字段名
     * value: 代码中的变量名
     */
    private Map<String, String> outputMapping;
}
