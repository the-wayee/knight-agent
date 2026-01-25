package org.cloudnook.knightagent.api.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * MCP 服务器 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerDTO {

    private String id;
    private String name;
    private String url;
    private String status;
    private Integer toolCount;
    private String error;
    private Instant createdAt;
    private Instant updatedAt;
    private List<McpToolDTO> tools;

    /**
     * MCP 工具 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpToolDTO {
        private Long id;
        private String name;
        private String description;
        private String inputSchema;
    }

    /**
     * 创建请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String name;
        private String url;
    }

    /**
     * 更新请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String url;
        private String status;
    }
}
