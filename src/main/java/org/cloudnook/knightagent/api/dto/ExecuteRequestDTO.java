package org.cloudnook.knightagent.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 执行请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteRequestDTO {

    /**
     * 输入数据
     */
    private Map<String, Object> input;

    /**
     * 是否异步执行
     */
    private Boolean async;

    /**
     * 是否流式执行
     */
    private Boolean stream;
}
