package org.cloudnook.knightagent.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 执行事件（API层）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionEvent {

    /**
     * 事件类型
     */
    private ExecutionEventType type;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 节点ID
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
    private Instant timestamp;

    /**
     * 错误信息
     */
    private String error;
}
