package org.cloudnook.knightagent.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 执行DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionDTO {

    /**
     * 执行ID
     */
    private String id;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 工作流名称
     */
    private String workflowName;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 输入数据
     */
    private Map<String, Object> input;

    /**
     * 输出数据
     */
    private Map<String, Object> output;

    /**
     * 节点执行结果
     */
    private List<NodeExecutionResultDTO> nodeResults;

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
