package org.cloudnook.knightagent.api.repository;

import org.cloudnook.knightagent.api.entity.McpTool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MCP 工具仓库
 */
@Repository
public interface McpToolRepository extends JpaRepository<McpTool, Long> {

    /**
     * 根据服务器 ID 删除所有工具
     */
    void deleteByServerId(String serverId);

    /**
     * 根据服务器 ID 获取所有工具
     */
    List<McpTool> findByServerId(String serverId);
}
