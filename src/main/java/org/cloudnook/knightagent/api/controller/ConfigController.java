package org.cloudnook.knightagent.api.controller;

import lombok.RequiredArgsConstructor;
import org.cloudnook.knightagent.api.dto.config.ApiKeyDTO;
import org.cloudnook.knightagent.api.dto.config.McpServerDTO;
import org.cloudnook.knightagent.api.service.ApiKeyService;
import org.cloudnook.knightagent.api.service.McpServerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 配置管理控制器
 * 提供 API Keys 和 MCP Servers 的 CRUD 操作
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConfigController {

    private final ApiKeyService apiKeyService;
    private final McpServerService mcpServerService;

    // ==================== API Keys ====================

    /**
     * 获取所有 API Keys
     */
    @GetMapping("/api-keys")
    public ResponseEntity<List<ApiKeyDTO>> getApiKeys() {
        return ResponseEntity.ok(apiKeyService.getAllApiKeys());
    }

    /**
     * 获取单个 API Key
     */
    @GetMapping("/api-keys/{uuid}")
    public ResponseEntity<ApiKeyDTO> getApiKey(@PathVariable String uuid) {
        ApiKeyDTO dto = apiKeyService.getByUuid(uuid);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * 创建 API Key
     */
    @PostMapping("/api-keys")
    public ResponseEntity<ApiKeyDTO> createApiKey(@RequestBody ApiKeyDTO.CreateRequest request) {
        ApiKeyDTO created = apiKeyService.createApiKey(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 更新 API Key
     */
    @PutMapping("/api-keys/{uuid}")
    public ResponseEntity<ApiKeyDTO> updateApiKey(
            @PathVariable String uuid,
            @RequestBody ApiKeyDTO.UpdateRequest request) {
        try {
            ApiKeyDTO updated = apiKeyService.updateApiKey(uuid, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除 API Key
     */
    @DeleteMapping("/api-keys/{uuid}")
    public ResponseEntity<Void> deleteApiKey(@PathVariable String uuid) {
        try {
            apiKeyService.deleteApiKey(uuid);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== MCP Servers ====================

    /**
     * 获取所有 MCP 服务器
     */
    @GetMapping("/mcp-servers")
    public ResponseEntity<List<McpServerDTO>> getMcpServers() {
        return ResponseEntity.ok(mcpServerService.getAllServers());
    }

    /**
     * 获取单个 MCP 服务器
     */
    @GetMapping("/mcp-servers/{uuid}")
    public ResponseEntity<McpServerDTO> getMcpServer(@PathVariable String uuid) {
        McpServerDTO dto = mcpServerService.getByUuid(uuid);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * 创建 MCP 服务器
     */
    @PostMapping("/mcp-servers")
    public ResponseEntity<McpServerDTO> createMcpServer(@RequestBody McpServerDTO.CreateRequest request) {
        McpServerDTO created = mcpServerService.createServer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 更新 MCP 服务器
     */
    @PutMapping("/mcp-servers/{uuid}")
    public ResponseEntity<McpServerDTO> updateMcpServer(
            @PathVariable String uuid,
            @RequestBody McpServerDTO.UpdateRequest request) {
        try {
            McpServerDTO updated = mcpServerService.updateServer(uuid, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除 MCP 服务器
     */
    @DeleteMapping("/mcp-servers/{uuid}")
    public ResponseEntity<Void> deleteMcpServer(@PathVariable String uuid) {
        try {
            mcpServerService.deleteServer(uuid);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 刷新 MCP 服务器连接
     */
    @PostMapping("/mcp-servers/{uuid}/refresh")
    public ResponseEntity<McpServerDTO> refreshMcpServer(@PathVariable String uuid) {
        try {
            McpServerDTO refreshed = mcpServerService.refreshServer(uuid);
            return ResponseEntity.ok(refreshed);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取 MCP 服务器的工具列表
     */
    @GetMapping("/mcp-servers/{uuid}/tools")
    public ResponseEntity<?> getMcpServerTools(@PathVariable String uuid) {
        try {
            var tools = mcpServerService.getServerTools(uuid);
            return ResponseEntity.ok(tools.stream()
                    .map(tool -> Map.of(
                            "id", tool.getId(),
                            "name", tool.getName(),
                            "description", tool.getDescription(),
                            "inputSchema", tool.getInputSchema()
                    ))
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
