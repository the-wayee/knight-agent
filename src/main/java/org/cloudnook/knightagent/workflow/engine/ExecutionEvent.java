package org.cloudnook.knightagent.workflow.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 执行事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionEvent {

    /**
     * 事件类型
     */
    private EventType type;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 节点ID（节点级别事件）
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 事件数据
     */
    private Map<String, Object> data;

    /**
     * 时间戳
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * 错误信息（错误事件）
     */
    private String error;

    /**
     * 创建工作流开始事件
     */
    public static ExecutionEvent workflowStarted(String executionId) {
        return ExecutionEvent.builder()
                .type(EventType.WORKFLOW_STARTED)
                .executionId(executionId)
                .build();
    }

    /**
     * 创建工作流完成事件
     */
    public static ExecutionEvent workflowCompleted(String executionId, Map<String, Object> output) {
        return ExecutionEvent.builder()
                .type(EventType.WORKFLOW_COMPLETED)
                .executionId(executionId)
                .data(output)
                .build();
    }

    /**
     * 创建工作流失败事件
     */
    public static ExecutionEvent workflowFailed(String executionId, String error) {
        return ExecutionEvent.builder()
                .type(EventType.WORKFLOW_FAILED)
                .executionId(executionId)
                .error(error)
                .build();
    }

    /**
     * 创建节点开始事件
     */
    public static ExecutionEvent nodeStarted(String executionId, String nodeId, String nodeName, String nodeType, Map<String, Object> input) {
        return ExecutionEvent.builder()
                .type(EventType.NODE_STARTED)
                .executionId(executionId)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .nodeType(nodeType)
                .data(input)
                .build();
    }

    /**
     * 创建节点完成事件
     */
    public static ExecutionEvent nodeCompleted(String executionId, String nodeId, String nodeName, Map<String, Object> output) {
        return ExecutionEvent.builder()
                .type(EventType.NODE_COMPLETED)
                .executionId(executionId)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .data(output)
                .build();
    }

    /**
     * 创建节点失败事件
     */
    public static ExecutionEvent nodeFailed(String executionId, String nodeId, String nodeName, String error) {
        return ExecutionEvent.builder()
                .type(EventType.NODE_FAILED)
                .executionId(executionId)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .error(error)
                .build();
    }

    /**
     * 创建Token事件（流式输出）
     */
    public static ExecutionEvent token(String executionId, String nodeId, String text) {
        return ExecutionEvent.builder()
                .type(EventType.TOKEN)
                .executionId(executionId)
                .nodeId(nodeId)
                .data(Map.of("text", text))
                .build();
    }

    /**
     * 事件类型枚举
     */
    public enum EventType {
        // 工作流级别
        WORKFLOW_STARTED,
        WORKFLOW_COMPLETED,
        WORKFLOW_FAILED,

        // 节点级别
        NODE_STARTED,
        NODE_COMPLETED,
        NODE_FAILED,

        // 流式输出
        TOKEN,
    }
}
