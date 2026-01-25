package org.cloudnook.knightagent.api.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * API Key DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyDTO {

    private String id;
    private String provider;
    private String name;
    private String apiKey;
    private String baseUrl;
    private String modelId;
    private String status;
    private Instant lastUsedAt;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 创建请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String provider;
        private String name;
        private String apiKey;
        private String baseUrl;
        private String modelId;
    }

    /**
     * 更新请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String apiKey;
        private String baseUrl;
        private String modelId;
        private String status;
    }
}
