package org.cloudnook.knightagent.workflow.node;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * 节点执行结果
 */
@Data
@Builder
public class NodeExecutionResult {

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 输出数据
     */
    private Map<String, Object> output;

    /**
     * 错误信息（执行失败时）
     */
    private String error;

    /**
     * 开始时间
     */
    private Instant startTime;

    /**
     * 结束时间
     */
    private Instant endTime;

    /**
     * 执行时长（毫秒）
     */
    private Long durationMs;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 创建成功结果
     */
    public static NodeExecutionResult success(String nodeId, Map<String, Object> output) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(ExecutionStatus.COMPLETED)
                .output(output)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static NodeExecutionResult failure(String nodeId, String error) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(ExecutionStatus.FAILED)
                .error(error)
                .build();
    }

    /**
     * 创建跳过结果
     */
    public static NodeExecutionResult skipped(String nodeId) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(ExecutionStatus.SKIPPED)
                .build();
    }
}
