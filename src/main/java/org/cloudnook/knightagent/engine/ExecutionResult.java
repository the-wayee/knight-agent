package org.cloudnook.knightagent.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 执行结果（API层）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    private String executionId;
    private String workflowId;
    private ExecutionStatus status;
    private Map<String, Object> input;
    private Map<String, Object> output;
    private List<NodeExecutionResult> nodeResults;
    private String error;
    private Instant startTime;
    private Instant endTime;
    private Long durationMs;
}
