package org.cloudnook.knightagent.workflow.node;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点执行上下文
 */
@Data
@Builder
public class NodeContext {

    /**
     * 工作流执行ID
     */
    private String executionId;

    /**
     * 当前节点ID
     */
    private String nodeId;

    /**
     * 工作流输入数据
     */
    private Map<String, Object> workflowInput;

    /**
     * 节点输入数据
     */
    private Map<String, Object> input;

    /**
     * 上下文变量（存储所有节点的输出）
     */
    @Builder.Default
    private Map<String, Map<String, Object>> variables = new ConcurrentHashMap<>();

    /**
     * 全局配置
     */
    private Map<String, Object> globalConfig;

    /**
     * 获取上游节点输出
     */
    public Map<String, Object> getNodeOutput(String nodeId) {
        return variables.get(nodeId);
    }

    /**
     * 获取上游节点的特定字段
     */
    public Object getFieldValue(String nodeId, String field) {
        Map<String, Object> output = getNodeOutput(nodeId);
        return output != null ? output.get(field) : null;
    }

    /**
     * 存储当前节点输出
     */
    public void putNodeOutput(String nodeId, Map<String, Object> output) {
        variables.put(nodeId, output);
    }

    /**
     * 获取上下文变量
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 设置上下文变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value instanceof Map ? (Map<String, Object>) value : Map.of("value", value));
    }
}
