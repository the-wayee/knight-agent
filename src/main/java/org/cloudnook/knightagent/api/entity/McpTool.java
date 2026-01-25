package org.cloudnook.knightagent.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * MCP 工具实体
 */
@Entity
@Table(name = "mcp_tools")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属服务器 ID
     */
    @Column(name = "server_id", nullable = false)
    private String serverId;

    /**
     * 工具名称
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 工具描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 输入参数 Schema（JSON）
     */
    @Column(columnDefinition = "TEXT")
    private String inputSchema;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
