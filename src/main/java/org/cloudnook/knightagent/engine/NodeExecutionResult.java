package org.cloudnook.knightagent.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 节点执行结果（API层）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionResult {

    private String nodeId;
    private String nodeName;
    private ExecutionStatus status;
    private Map<String, Object> input;
    private Map<String, Object> output;
    private String error;
    private Instant startTime;
    private Instant endTime;
    private Long durationMs;
}
