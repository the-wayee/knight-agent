package org.cloudnook.knightagent.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式执行信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamInfoDTO {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * WebSocket URL
     */
    private String wsUrl;
}
