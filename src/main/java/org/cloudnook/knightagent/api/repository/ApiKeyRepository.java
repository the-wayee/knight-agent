package org.cloudnook.knightagent.api.repository;

import org.cloudnook.knightagent.api.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * API Key 仓库
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /**
     * 根据 UUID 查找
     */
    Optional<ApiKey> findByUuid(String uuid);

    /**
     * 检查 UUID 是否存在
     */
    boolean existsByUuid(String uuid);

    /**
     * 获取所有有效的 API Key
     */
    List<ApiKey> findByStatus(ApiKey.ApiKeyStatus status);

    /**
     * 根据提供商查找
     */
    List<ApiKey> findByProvider(String provider);

    /**
     * 根据 UUID 删除
     */
    void deleteByUuid(String uuid);
}
