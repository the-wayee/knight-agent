package org.cloudnook.knightagent.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.api.dto.*;
import org.cloudnook.knightagent.api.service.WorkflowService;
import org.cloudnook.knightagent.workflow.definition.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流服务实现
 */
@Slf4j
@Service
public class WorkflowServiceImpl implements WorkflowService {

    // 内存存储（生产环境应该使用数据库）
    private final Map<String, WorkflowDefinition> workflows = new ConcurrentHashMap<>();

    @Override
    public WorkflowDTO createWorkflow(CreateWorkflowDTO dto) {
        String id = UUID.randomUUID().toString();

        WorkflowDefinition definition = WorkflowDefinition.builder()
                .id(id)
                .name(dto.getName())
                .description(dto.getDescription())
                .nodes(convertNodes(dto.getNodes()))
                .edges(convertEdges(dto.getEdges()))
                .settings(dto.getSettings())
                .tags(dto.getTags())
                .version(1)
                .build();

        // 验证工作流
        ValidationResult validation = definition.validate();
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Invalid workflow definition: " + String.join(", ", validation.getErrors()));
        }

        workflows.put(id, definition);

        return toDTO(definition);
    }

    @Override
    public WorkflowDTO updateWorkflow(String id, UpdateWorkflowDTO dto) {
        WorkflowDefinition existing = workflows.get(id);
        if (existing == null) {
            throw new IllegalArgumentException("Workflow not found: " + id);
        }

        WorkflowDefinition updated = WorkflowDefinition.builder()
                .id(id)
                .name(dto.getName() != null ? dto.getName() : existing.getName())
                .description(dto.getDescription() != null ? dto.getDescription() : existing.getDescription())
                .nodes(dto.getNodes() != null ? convertNodes(dto.getNodes()) : existing.getNodes())
                .edges(dto.getEdges() != null ? convertEdges(dto.getEdges()) : existing.getEdges())
                .settings(dto.getSettings() != null ? dto.getSettings() : existing.getSettings())
                .tags(dto.getTags() != null ? dto.getTags() : existing.getTags())
                .version(existing.getVersion() + 1)
                .build();

        ValidationResult validation = updated.validate();
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Invalid workflow definition: " + String.join(", ", validation.getErrors()));
        }

        workflows.put(id, updated);

        return toDTO(updated);
    }

    @Override
    public void deleteWorkflow(String id) {
        if (!workflows.containsKey(id)) {
            throw new IllegalArgumentException("Workflow not found: " + id);
        }
        workflows.remove(id);
    }

    @Override
    public WorkflowDTO getWorkflow(String id) {
        WorkflowDefinition definition = workflows.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Workflow not found: " + id);
        }
        return toDTO(definition);
    }

    @Override
    public List<WorkflowDTO> listWorkflows() {
        return workflows.values().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public WorkflowDefinition validateWorkflow(WorkflowDefinition definition) {
        ValidationResult validation = definition.validate();
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Invalid workflow definition: " + String.join(", ", validation.getErrors()));
        }
        return definition;
    }

    /**
     * 转换为DTO
     */
    private WorkflowDTO toDTO(WorkflowDefinition definition) {
        return WorkflowDTO.builder()
                .id(definition.getId())
                .name(definition.getName())
                .description(definition.getDescription())
                .version(definition.getVersion())
                .nodes(convertNodesToDTO(definition.getNodes()))
                .edges(convertEdgesToDTO(definition.getEdges()))
                .settings(definition.getSettings())
                .tags(definition.getTags())
                .createdAt(Instant.now()) // 内存存储无法跟踪时间
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * 转换节点列表
     */
    private List<NodeDefinition> convertNodes(List<NodeDefinitionDTO> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(this::convertNode)
                .toList();
    }

    /**
     * 转换单个节点
     */
    private NodeDefinition convertNode(NodeDefinitionDTO dto) {
        return NodeDefinition.builder()
                .id(dto.getId())
                .type(dto.getType())
                .name(dto.getName())
                .position(convertPosition(dto.getPosition()))
                .config(dto.getConfig())
                .isStart(dto.getIsStart())
                .isEnd(dto.getIsEnd())
                .build();
    }

    /**
     * 转换位置
     */
    private Point convertPosition(PositionDTO dto) {
        if (dto == null) {
            return null;
        }
        return Point.builder()
                .x(dto.getX())
                .y(dto.getY())
                .build();
    }

    /**
     * 转换边列表
     */
    private List<EdgeDefinition> convertEdges(List<EdgeDefinitionDTO> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(this::convertEdge)
                .toList();
    }

    /**
     * 转换单个边
     */
    private EdgeDefinition convertEdge(EdgeDefinitionDTO dto) {
        return EdgeDefinition.builder()
                .id(dto.getId())
                .source(dto.getSource())
                .target(dto.getTarget())
                .sourceHandle(dto.getSourceHandle())
                .targetHandle(dto.getTargetHandle())
                .condition(dto.getCondition())
                .build();
    }

    /**
     * 转换节点列表为DTO
     */
    private List<NodeDefinitionDTO> convertNodesToDTO(List<NodeDefinition> nodes) {
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream()
                .map(this::convertNodeToDTO)
                .toList();
    }

    /**
     * 转换单个节点为DTO
     */
    private NodeDefinitionDTO convertNodeToDTO(NodeDefinition node) {
        return NodeDefinitionDTO.builder()
                .id(node.getId())
                .type(node.getType())
                .name(node.getName())
                .position(convertPositionToDTO(node.getPosition()))
                .config(node.getConfig())
                .isStart(node.getIsStart())
                .isEnd(node.getIsEnd())
                .build();
    }

    /**
     * 转换位置为DTO
     */
    private PositionDTO convertPositionToDTO(Point point) {
        if (point == null) {
            return null;
        }
        return PositionDTO.builder()
                .x(point.getX())
                .y(point.getY())
                .build();
    }

    /**
     * 转换边列表为DTO
     */
    private List<EdgeDefinitionDTO> convertEdgesToDTO(List<EdgeDefinition> edges) {
        if (edges == null) {
            return List.of();
        }
        return edges.stream()
                .map(this::convertEdgeToDTO)
                .toList();
    }

    /**
     * 转换单个边为DTO
     */
    private EdgeDefinitionDTO convertEdgeToDTO(EdgeDefinition edge) {
        return EdgeDefinitionDTO.builder()
                .id(edge.getId())
                .source(edge.getSource())
                .target(edge.getTarget())
                .sourceHandle(edge.getSourceHandle())
                .targetHandle(edge.getTargetHandle())
                .condition(edge.getCondition())
                .build();
    }
}
