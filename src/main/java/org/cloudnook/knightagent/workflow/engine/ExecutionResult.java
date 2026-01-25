package org.cloudnook.knightagent.workflow.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 输入数据
     */
    private Map<String, Object> input;

    /**
     * 输出数据
     */
    private Map<String, Object> output;

    /**
     * 节点执行结果列表
     */
    private List<NodeExecutionResult> nodeResults;

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
     * 创建成功结果
     */
    public static ExecutionResult success(String executionId, String workflowId,
                                          Map<String, Object> input, Map<String, Object> output,
                                          List<NodeExecutionResult> nodeResults) {
        return ExecutionResult.builder()
                .executionId(executionId)
                .workflowId(workflowId)
                .status(ExecutionStatus.COMPLETED)
                .input(input)
                .output(output)
                .nodeResults(nodeResults)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ExecutionResult failure(String executionId, String workflowId,
                                          Map<String, Object> input, String error) {
        return ExecutionResult.builder()
                .executionId(executionId)
                .workflowId(workflowId)
                .status(ExecutionStatus.FAILED)
                .input(input)
                .error(error)
                .build();
    }

    /**
     * 计算执行时长
     */
    public void calculateDuration() {
        if (startTime != null && endTime != null) {
            durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }
}
