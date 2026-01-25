package org.cloudnook.knightagent.workflow.definition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流定义
 */
@Data
@Builder
public class WorkflowDefinition {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 工作流ID
     */
    private String id;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 工作流描述
     */
    private String description;

    /**
     * 版本号
     */
    @Builder.Default
    private Integer version = 1;

    /**
     * 节点列表
     */
    private List<NodeDefinition> nodes;

    /**
     * 连接边列表
     */
    private List<EdgeDefinition> edges;

    /**
     * 全局设置
     */
    private Map<String, Object> settings;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 验证工作流定义
     */
    public ValidationResult validate() {
        ValidationResult result = ValidationResult.builder()
                .valid(true)
                .build();

        // 检查基本字段
        if (name == null || name.isBlank()) {
            result.addError("Workflow name is required");
        }

        if (nodes == null) {
            nodes = List.of();
        }

        if (edges == null) {
            edges = List.of();
        }

        // 检查节点ID唯一性
        if (nodes != null) {
            long duplicateCount = nodes.stream()
                    .map(NodeDefinition::getId)
                    .distinct()
                    .count();
            if (duplicateCount != nodes.size()) {
                result.addError("Duplicate node IDs found");
            }
        }

        // 检查边引用的节点是否存在
        if (edges != null) {
            for (EdgeDefinition edge : edges) {
                if (nodes.stream().noneMatch(n -> n.getId().equals(edge.getSource()))) {
                    result.addError("Edge source node not found: " + edge.getSource());
                }
                if (nodes.stream().noneMatch(n -> n.getId().equals(edge.getTarget()))) {
                    result.addError("Edge target node not found: " + edge.getTarget());
                }
            }
        }

        return result;
    }

    /**
     * 获取起始节点
     */
    public NodeDefinition getStartNode() {
        if (nodes == null) {
            return null;
        }
        return nodes.stream()
                .filter(n -> Boolean.TRUE.equals(n.getIsStart()))
                .findFirst()
                .orElse(nodes.isEmpty() ? null : nodes.get(0));
    }

    /**
     * 获取结束节点
     */
    public List<NodeDefinition> getEndNodes() {
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream()
                .filter(n -> Boolean.TRUE.equals(n.getIsEnd()))
                .toList();
    }

    /**
     * 根据ID获取节点
     */
    public NodeDefinition getNode(String nodeId) {
        if (nodes == null) {
            return null;
        }
        return nodes.stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取节点的输入边
     */
    public List<EdgeDefinition> getInputEdges(String nodeId) {
        if (edges == null) {
            return List.of();
        }
        return edges.stream()
                .filter(e -> e.getTarget().equals(nodeId))
                .toList();
    }

    /**
     * 获取节点的输出边
     */
    public List<EdgeDefinition> getOutputEdges(String nodeId) {
        if (edges == null) {
            return List.of();
        }
        return edges.stream()
                .filter(e -> e.getSource().equals(nodeId))
                .toList();
    }

    /**
     * 转换为JSON字符串
     */
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize workflow definition", e);
        }
    }

    /**
     * 从JSON字符串解析
     */
    public static WorkflowDefinition fromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, WorkflowDefinition.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse workflow definition", e);
        }
    }
}
