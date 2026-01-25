package org.cloudnook.knightagent.api.repository;

import org.cloudnook.knightagent.api.entity.McpServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MCP 服务器仓库
 */
@Repository
public interface McpServerRepository extends JpaRepository<McpServer, Long> {

    /**
     * 根据 UUID 查找
     */
    Optional<McpServer> findByUuid(String uuid);

    /**
     * 检查 UUID 是否存在
     */
    boolean existsByUuid(String uuid);

    /**
     * 获取所有已连接的服务器
     */
    List<McpServer> findByStatus(McpServer.McpServerStatus status);

    /**
     * 根据 UUID 删除
     */
    void deleteByUuid(String uuid);
}
