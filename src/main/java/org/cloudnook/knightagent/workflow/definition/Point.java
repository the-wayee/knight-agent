package org.cloudnook.knightagent.workflow.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 画布位置坐标
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Point {

    /**
     * X坐标
     */
    private Double x;

    /**
     * Y坐标
     */
    private Double y;
}
