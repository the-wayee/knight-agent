package org.cloudnook.knightagent.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * API Key 实体
 */
@Entity
@Table(name = "api_keys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 唯一标识符（用于前端引用）
     */
    @Column(unique = true, nullable = false, length = 100)
    private String uuid;

    /**
     * 提供商名称（如 OpenAI、Anthropic、DeepSeek）
     */
    @Column(nullable = false, length = 100)
    private String provider;

    /**
     * 自定义名称
     */
    @Column(length = 200)
    private String name;

    /**
     * API 密钥（加密存储）
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String apiKey;

    /**
     * 自定义 Base URL
     */
    @Column(length = 500)
    private String baseUrl;

    /**
     * 默认模型 ID
     */
    @Column(length = 100)
    private String modelId;

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiKeyStatus status;

    /**
     * 最后使用时间
     */
    private Instant lastUsedAt;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = ApiKeyStatus.UNKNOWN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * API Key 状态
     */
    public enum ApiKeyStatus {
        VALID,
        INVALID,
        UNKNOWN
    }
}
