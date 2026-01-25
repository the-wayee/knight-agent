package org.cloudnook.knightagent.workflow.node;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点类型枚举
 */
@Getter
@AllArgsConstructor
public enum NodeType {

    // I/O 节点
    INPUT("input", "输入节点", "工作流输入定义"),
    OUTPUT("output", "输出节点", "工作流输出定义"),

    // Agent 节点
    AGENT("agent", "Agent节点", "AI Agent执行"),

    // 逻辑节点
    CODE("code", "代码节点", "JavaScript代码执行"),
    CONDITION("condition", "条件节点", "条件分支"),

    // 外部节点
    HTTP("http", "HTTP节点", "HTTP请求"),
    TOOL("tool", "工具节点", "单独工具调用");

    private final String code;
    private final String name;
    private final String description;

    /**
     * 根据代码获取节点类型
     */
    public static NodeType fromCode(String code) {
        for (NodeType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown node type: " + code);
    }
}
