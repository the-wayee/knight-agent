package org.cloudnook.knightagent.workflow.engine;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 执行状态枚举
 */
@Getter
@AllArgsConstructor
public enum ExecutionStatus {

    PENDING("pending", "等待执行"),
    RUNNING("running", "执行中"),
    PAUSED("paused", "已暂停"),
    COMPLETED("completed", "执行完成"),
    FAILED("failed", "执行失败"),
    CANCELLED("cancelled", "已取消");

    private final String code;
    private final String description;

    public static ExecutionStatus fromCode(String code) {
        for (ExecutionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown execution status: " + code);
    }
}
