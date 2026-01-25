package org.cloudnook.knightagent.workflow.engine;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行上下文
 */
@Data
@Builder
public class ExecutionContext {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 工作流定义ID
     */
    private String workflowId;

    /**
     * 工作流输入
     */
    private Map<String, Object> input;

    /**
     * 全局配置
     */
    private Map<String, Object> globalConfig;

    /**
     * 节点输出存储（key: nodeId, value: output）
     */
    @Builder.Default
    private Map<String, Map<String, Object>> nodeOutputs = new ConcurrentHashMap<>();

    /**
     * 上下文变量
     */
    @Builder.Default
    private Map<String, Object> variables = new ConcurrentHashMap<>();

    /**
     * 当前执行的节点ID
     */
    private String currentNodeId;

    /**
     * 是否暂停
     */
    @Builder.Default
    private boolean paused = false;

    /**
     * 是否取消
     */
    @Builder.Default
    private boolean cancelled = false;

    /**
     * 获取节点输出
     */
    public Map<String, Object> getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }

    /**
     * 设置节点输出
     */
    public void setNodeOutput(String nodeId, Map<String, Object> output) {
        nodeOutputs.put(nodeId, output);
    }

    /**
     * 获取变量
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 暂停执行
     */
    public void pause() {
        this.paused = true;
    }

    /**
     * 恢复执行
     */
    public void resume() {
        this.paused = false;
    }

    /**
     * 取消执行
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * 检查是否应该继续执行
     */
    public boolean shouldContinue() {
        return !paused && !cancelled;
    }
}
