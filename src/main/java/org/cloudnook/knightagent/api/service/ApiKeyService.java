package org.cloudnook.knightagent.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.api.dto.config.ApiKeyDTO;
import org.cloudnook.knightagent.api.entity.ApiKey;
import org.cloudnook.knightagent.api.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * API Key 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    /**
     * 获取所有 API Key
     */
    public List<ApiKeyDTO> getAllApiKeys() {
        return apiKeyRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据 UUID 获取
     */
    public ApiKeyDTO getByUuid(String uuid) {
        return apiKeyRepository.findByUuid(uuid)
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * 根据 UUID 获取实体（内部使用）
     */
    public ApiKey getEntityByUuid(String uuid) {
        return apiKeyRepository.findByUuid(uuid).orElse(null);
    }

    /**
     * 创建 API Key
     */
    @Transactional
    public ApiKeyDTO createApiKey(ApiKeyDTO.CreateRequest request) {
        String uuid = "api-" + UUID.randomUUID().toString();

        ApiKey apiKey = ApiKey.builder()
                .uuid(uuid)
                .provider(request.getProvider())
                .name(request.getName())
                .apiKey(request.getApiKey())
                .baseUrl(request.getBaseUrl())
                .modelId(request.getModelId())
                .status(ApiKey.ApiKeyStatus.UNKNOWN)
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        log.info("Created API key: {} for provider: {}", uuid, request.getProvider());
        return toDTO(apiKey);
    }

    /**
     * 更新 API Key
     */
    @Transactional
    public ApiKeyDTO updateApiKey(String uuid, ApiKeyDTO.UpdateRequest request) {
        ApiKey apiKey = apiKeyRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("API Key not found: " + uuid));

        if (request.getName() != null) {
            apiKey.setName(request.getName());
        }
        if (request.getApiKey() != null) {
            apiKey.setApiKey(request.getApiKey());
        }
        if (request.getBaseUrl() != null) {
            apiKey.setBaseUrl(request.getBaseUrl());
        }
        if (request.getModelId() != null) {
            apiKey.setModelId(request.getModelId());
        }
        if (request.getStatus() != null) {
            apiKey.setStatus(ApiKey.ApiKeyStatus.valueOf(request.getStatus()));
        }

        apiKey = apiKeyRepository.save(apiKey);
        log.info("Updated API key: {}", uuid);
        return toDTO(apiKey);
    }

    /**
     * 删除 API Key
     */
    @Transactional
    public void deleteApiKey(String uuid) {
        if (!apiKeyRepository.existsByUuid(uuid)) {
            throw new IllegalArgumentException("API Key not found: " + uuid);
        }
        apiKeyRepository.deleteByUuid(uuid);
        log.info("Deleted API key: {}", uuid);
    }

    /**
     * 更新最后使用时间
     */
    @Transactional
    public void updateLastUsed(String uuid) {
        apiKeyRepository.findByUuid(uuid).ifPresent(key -> {
            key.setLastUsedAt(java.time.Instant.now());
            apiKeyRepository.save(key);
        });
    }

    /**
     * 转换为 DTO
     */
    private ApiKeyDTO toDTO(ApiKey entity) {
        return ApiKeyDTO.builder()
                .id(entity.getUuid())
                .provider(entity.getProvider())
                .name(entity.getName())
                .apiKey(maskApiKey(entity.getApiKey()))
                .baseUrl(entity.getBaseUrl())
                .modelId(entity.getModelId())
                .status(entity.getStatus().name().toLowerCase())
                .lastUsedAt(entity.getLastUsedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 掩码 API Key
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 11) {
            return "sk-****...";
        }
        return apiKey.substring(0, 7) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 获取实际的 API Key（用于内部调用）
     */
    public String getActualApiKey(String uuid) {
        return apiKeyRepository.findByUuid(uuid)
                .map(ApiKey::getApiKey)
                .orElseThrow(() -> new IllegalArgumentException("API Key not found: " + uuid));
    }
}
