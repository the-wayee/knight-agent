package org.cloudnook.knightagent.workflow.engine;

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
     * 节点名称
     */
    private String nodeName;

    /**
     * 执行状态
     */
    private org.cloudnook.knightagent.workflow.node.ExecutionStatus status;

    /**
     * 输入数据
     */
    private Map<String, Object> input;

    /**
     * 输出数据
     */
    private Map<String, Object> output;

    /**
     * 错误信息
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
}
