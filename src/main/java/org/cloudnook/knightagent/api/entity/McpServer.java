package org.cloudnook.knightagent.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * MCP 服务器实体
 */
@Entity
@Table(name = "mcp_servers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 唯一标识符（用于前端引用）
     */
    @Column(unique = true, nullable = false, length = 100)
    private String uuid;

    /**
     * 服务器名称
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 服务器 URL（stdio:// 或 http://）
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private McpServerStatus status;

    /**
     * 工具数量
     */
    @Column(name = "tool_count")
    private Integer toolCount;

    /**
     * 错误信息（如果连接失败）
     */
    @Column(columnDefinition = "TEXT")
    private String error;

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
            status = McpServerStatus.DISCONNECTED;
        }
        if (toolCount == null) {
            toolCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * MCP 服务器状态
     */
    public enum McpServerStatus {
        CONNECTED,
        DISCONNECTED,
        ERROR
    }
}
