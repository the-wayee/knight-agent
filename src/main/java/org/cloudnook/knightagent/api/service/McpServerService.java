package org.cloudnook.knightagent.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.api.dto.config.McpServerDTO;
import org.cloudnook.knightagent.api.entity.McpServer;
import org.cloudnook.knightagent.api.entity.McpTool;
import org.cloudnook.knightagent.api.repository.McpServerRepository;
import org.cloudnook.knightagent.api.repository.McpToolRepository;
import org.cloudnook.knightagent.core.mcp.McpClientWrapper;
import org.cloudnook.knightagent.core.mcp.McpConfig;
import org.cloudnook.knightagent.core.mcp.McpException;
import org.cloudnook.knightagent.core.mcp.McpProtocol;
import org.cloudnook.knightagent.core.mcp.McpToolDescription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MCP 服务器服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerService {

    private final McpServerRepository mcpServerRepository;
    private final McpToolRepository mcpToolRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取所有 MCP 服务器（包含工具列表）
     */
    public List<McpServerDTO> getAllServers() {
        return mcpServerRepository.findAll().stream()
                .map(this::toDTOWithTools)
                .collect(Collectors.toList());
    }

    /**
     * 根据 UUID 获取（包含工具列表）
     */
    public McpServerDTO getByUuid(String uuid) {
        return mcpServerRepository.findByUuid(uuid)
                .map(this::toDTOWithTools)
                .orElse(null);
    }

    /**
     * 根据 UUID 获取（不包含工具列表，用于列表展示）
     */
    public McpServerDTO getByUuidWithoutTools(String uuid) {
        return mcpServerRepository.findByUuid(uuid)
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * 创建 MCP 服务器
     */
    @Transactional
    public McpServerDTO createServer(McpServerDTO.CreateRequest request) {
        String uuid = "mcp-" + UUID.randomUUID().toString();

        McpServer server = McpServer.builder()
                .uuid(uuid)
                .name(request.getName())
                .url(request.getUrl())
                .status(McpServer.McpServerStatus.DISCONNECTED)
                .toolCount(0)
                .build();

        server = mcpServerRepository.save(server);
        log.info("Created MCP server: {} with url: {}", uuid, request.getUrl());
        return toDTO(server);
    }

    /**
     * 更新 MCP 服务器
     */
    @Transactional
    public McpServerDTO updateServer(String uuid, McpServerDTO.UpdateRequest request) {
        McpServer server = mcpServerRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + uuid));

        if (request.getName() != null) {
            server.setName(request.getName());
        }
        if (request.getUrl() != null) {
            server.setUrl(request.getUrl());
        }
        if (request.getStatus() != null) {
            server.setStatus(McpServer.McpServerStatus.valueOf(request.getStatus().toUpperCase()));
        }

        server = mcpServerRepository.save(server);
        log.info("Updated MCP server: {}", uuid);
        return toDTO(server);
    }

    /**
     * 删除 MCP 服务器
     */
    @Transactional
    public void deleteServer(String uuid) {
        if (!mcpServerRepository.existsByUuid(uuid)) {
            throw new IllegalArgumentException("MCP Server not found: " + uuid);
        }
        // 删除关联的工具
        mcpToolRepository.deleteByServerId(uuid);
        mcpServerRepository.deleteByUuid(uuid);
        log.info("Deleted MCP server: {}", uuid);
    }

    /**
     * 刷新 MCP 服务器连接状态
     */
    @Transactional
    public McpServerDTO refreshServer(String uuid) {
        McpServer server = mcpServerRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + uuid));

        try {
            // 从 URL 检测协议类型
            McpProtocol protocol = detectProtocol(server.getUrl());
            String entrypoint = extractEntrypoint(server.getUrl());

            // 创建 MCP 配置
            McpConfig config = McpConfig.builder()
                    .protocol(protocol)
                    .entrypoint(entrypoint)
                    .build();

            // 创建 MCP 客户端并连接
            try (McpClientWrapper client = new McpClientWrapper(config)) {
                client.initialize();

                // 获取工具列表
                List<McpToolDescription> tools = client.listTools();

                // 删除旧的工具记录
                mcpToolRepository.deleteByServerId(uuid);

                // 保存新的工具列表
                for (McpToolDescription tool : tools) {
                    McpTool toolEntity = McpTool.builder()
                            .serverId(uuid)
                            .name(tool.getName())
                            .description(tool.getDescription())
                            .inputSchema(serializeInputSchema(tool.getInputSchema()))
                            .build();
                    mcpToolRepository.save(toolEntity);
                }

                // 更新服务器状态
                server.setStatus(McpServer.McpServerStatus.CONNECTED);
                server.setToolCount(tools.size());
                server.setError(null);

                log.info("Successfully connected to MCP server: {}, found {} tools", uuid, tools.size());
            }

        } catch (McpException e) {
            log.error("Failed to connect to MCP server: {}", uuid, e);
            server.setStatus(McpServer.McpServerStatus.ERROR);
            server.setError(e.getMessage());
            server.setToolCount(0);
        } catch (Exception e) {
            log.error("Unexpected error connecting to MCP server: {}", uuid, e);
            server.setStatus(McpServer.McpServerStatus.ERROR);
            server.setError(e.getMessage());
            server.setToolCount(0);
        }

        server = mcpServerRepository.save(server);
        // 返回包含工具列表的 DTO，以便前端显示发现的工具
        return toDTOWithTools(server);
    }

    /**
     * 从 URL 检测协议类型
     */
    private McpProtocol detectProtocol(String url) {
        if (url.startsWith("stdio://")) {
            return McpProtocol.STDIO;
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            // 默认使用 STREAMABLE_HTTP，因为它是 HTTP MCP 服务器的标准协议
            return McpProtocol.STREAMABLE_HTTP;
        } else if (url.startsWith("ws://") || url.startsWith("wss://")) {
            return McpProtocol.WS;
        } else {
            // 如果没有协议前缀，假设是 stdio 命令
            return McpProtocol.STDIO;
        }
    }

    /**
     * 从 URL 提取入口点
     */
    private String extractEntrypoint(String url) {
        if (url.startsWith("stdio://")) {
            // stdio:// 后面的部分是命令
            return url.substring(8);
        }
        // 其他协议直接返回 URL
        return url;
    }

    /**
     * 获取服务器的工具列表
     */
    public List<McpTool> getServerTools(String serverId) {
        return mcpToolRepository.findByServerId(serverId);
    }

    /**
     * 转换为 DTO（不包含工具列表）
     */
    private McpServerDTO toDTO(McpServer entity) {
        return McpServerDTO.builder()
                .id(entity.getUuid())
                .name(entity.getName())
                .url(entity.getUrl())
                .status(entity.getStatus().name().toLowerCase())
                .toolCount(entity.getToolCount())
                .error(entity.getError())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 转换为 DTO（包含工具列表）
     */
    private McpServerDTO toDTOWithTools(McpServer entity) {
        List<McpServerDTO.McpToolDTO> tools = mcpToolRepository.findByServerId(entity.getUuid()).stream()
                .map(tool -> McpServerDTO.McpToolDTO.builder()
                        .id(tool.getId())
                        .name(tool.getName())
                        .description(tool.getDescription())
                        .inputSchema(tool.getInputSchema())
                        .build())
                .collect(Collectors.toList());

        return McpServerDTO.builder()
                .id(entity.getUuid())
                .name(entity.getName())
                .url(entity.getUrl())
                .status(entity.getStatus().name().toLowerCase())
                .toolCount(entity.getToolCount())
                .error(entity.getError())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .tools(tools)
                .build();
    }

    /**
     * 将输入 Schema 序列化为 JSON 字符串
     */
    private String serializeInputSchema(java.util.Map<String, Object> inputSchema) {
        if (inputSchema == null || inputSchema.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(inputSchema);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize input schema, returning empty object", e);
            return "{}";
        }
    }
}
