package org.cloudnook.knightagent.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 位置DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionDTO {

    /**
     * X坐标
     */
    private Double x;

    /**
     * Y坐标
     */
    private Double y;
}
