package org.cloudnook.knightagent.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.cloudnook.knightagent.api.dto.*;
import org.cloudnook.knightagent.api.service.ExecutionService;
import org.cloudnook.knightagent.api.service.WorkflowService;
import org.cloudnook.knightagent.engine.*;
import org.cloudnook.knightagent.workflow.definition.WorkflowDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 执行服务实现
 */
@Slf4j
@Service
public class ExecutionServiceImpl implements ExecutionService {

    private final WorkflowService workflowService;
    private final org.cloudnook.knightagent.workflow.engine.WorkflowEngine workflowEngine;
    private final Map<String, ExecutionResult> executions = new ConcurrentHashMap<>();

    @Autowired
    public ExecutionServiceImpl(WorkflowService workflowService,
                                org.cloudnook.knightagent.workflow.engine.WorkflowEngine workflowEngine) {
        this.workflowService = workflowService;
        this.workflowEngine = workflowEngine;
    }

    @Override
    public ExecutionDTO execute(String workflowId, ExecuteRequestDTO request) {
        WorkflowDefinition workflow = getWorkflowDefinition(workflowId);
        Map<String, Object> input = request.getInput() != null ? request.getInput() : Map.of();

        org.cloudnook.knightagent.workflow.engine.ExecutionResult result = workflowEngine.execute(workflow, input);
        ExecutionResult apiResult = convertResult(result);
        executions.put(apiResult.getExecutionId(), apiResult);

        return toDTO(apiResult, workflow.getName());
    }

    @Override
    public CompletableFuture<ExecutionDTO> executeAsync(String workflowId, ExecuteRequestDTO request) {
        return CompletableFuture.supplyAsync(() -> execute(workflowId, request));
    }

    @Override
    public ExecutionDTO executeStream(String workflowId, ExecuteRequestDTO request,
                                       Consumer<ExecutionEvent> eventConsumer) {
        WorkflowDefinition workflow = getWorkflowDefinition(workflowId);
        Map<String, Object> input = request.getInput() != null ? request.getInput() : Map.of();

        Consumer<org.cloudnook.knightagent.workflow.engine.ExecutionEvent> consumer =
                event -> eventConsumer.accept(convertEvent(event));

        org.cloudnook.knightagent.workflow.engine.ExecutionResult result =
                workflowEngine.executeStream(workflow, input, consumer);

        ExecutionResult apiResult = convertResult(result);
        executions.put(apiResult.getExecutionId(), apiResult);

        return toDTO(apiResult, workflow.getName());
    }

    @Override
    public ExecutionDTO getExecution(String executionId) {
        ExecutionResult result = executions.get(executionId);
        if (result == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }

        String workflowName = "Unknown";
        try {
            WorkflowDTO workflow = workflowService.getWorkflow(result.getWorkflowId());
            workflowName = workflow.getName();
        } catch (Exception e) {
            log.warn("Failed to get workflow name for execution: {}", executionId);
        }

        return toDTO(result, workflowName);
    }

    @Override
    public List<ExecutionDTO> getExecutionHistory(String workflowId) {
        return executions.values().stream()
                .filter(e -> e.getWorkflowId().equals(workflowId))
                .map(e -> toDTO(e, "Workflow"))
                .collect(Collectors.toList());
    }

    @Override
    public void cancelExecution(String executionId) {
        ExecutionResult result = executions.get(executionId);
        if (result == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }
        // TODO: 实现取消逻辑
    }

    private WorkflowDefinition getWorkflowDefinition(String workflowId) {
        WorkflowDTO dto = workflowService.getWorkflow(workflowId);
        return convertToDefinition(dto);
    }

    private WorkflowDefinition convertToDefinition(WorkflowDTO dto) {
        return WorkflowDefinition.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .version(dto.getVersion())
                .nodes(convertNodes(dto.getNodes()))
                .edges(convertEdges(dto.getEdges()))
                .settings(dto.getSettings())
                .tags(dto.getTags())
                .build();
    }

    private List<org.cloudnook.knightagent.workflow.definition.NodeDefinition> convertNodes(List<NodeDefinitionDTO> nodes) {
        return nodes.stream()
                .map(dto -> org.cloudnook.knightagent.workflow.definition.NodeDefinition.builder()
                        .id(dto.getId())
                        .type(dto.getType())
                        .name(dto.getName())
                        .position(org.cloudnook.knightagent.workflow.definition.Point.builder()
                                .x(dto.getPosition() != null ? dto.getPosition().getX() : 0)
                                .y(dto.getPosition() != null ? dto.getPosition().getY() : 0)
                                .build())
                        .config(dto.getConfig())
                        .isStart(dto.getIsStart())
                        .isEnd(dto.getIsEnd())
                        .build())
                .collect(Collectors.toList());
    }

    private List<org.cloudnook.knightagent.workflow.definition.EdgeDefinition> convertEdges(List<EdgeDefinitionDTO> edges) {
        return edges.stream()
                .map(dto -> org.cloudnook.knightagent.workflow.definition.EdgeDefinition.builder()
                        .id(dto.getId())
                        .source(dto.getSource())
                        .target(dto.getTarget())
                        .sourceHandle(dto.getSourceHandle())
                        .targetHandle(dto.getTargetHandle())
                        .condition(dto.getCondition())
                        .build())
                .collect(Collectors.toList());
    }

    private ExecutionResult convertResult(org.cloudnook.knightagent.workflow.engine.ExecutionResult result) {
        return ExecutionResult.builder()
                .executionId(result.getExecutionId())
                .workflowId(result.getWorkflowId())
                .status(ExecutionStatus.valueOf(result.getStatus().name()))
                .input(result.getInput())
                .output(result.getOutput())
                .nodeResults(convertNodeResults(result.getNodeResults()))
                .error(result.getError())
                .startTime(result.getStartTime())
                .endTime(result.getEndTime())
                .durationMs(result.getDurationMs())
                .build();
    }

    private List<NodeExecutionResult> convertNodeResults(List<org.cloudnook.knightagent.workflow.engine.NodeExecutionResult> results) {
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .map(r -> NodeExecutionResult.builder()
                        .nodeId(r.getNodeId())
                        .nodeName(r.getNodeName())
                        .status(ExecutionStatus.valueOf(r.getStatus().name()))
                        .input(r.getInput())
                        .output(r.getOutput())
                        .error(r.getError())
                        .startTime(r.getStartTime())
                        .endTime(r.getEndTime())
                        .durationMs(r.getDurationMs())
                        .build())
                .collect(Collectors.toList());
    }

    private ExecutionEvent convertEvent(org.cloudnook.knightagent.workflow.engine.ExecutionEvent event) {
        return ExecutionEvent.builder()
                .type(ExecutionEventType.valueOf(event.getType().name()))
                .executionId(event.getExecutionId())
                .nodeId(event.getNodeId())
                .nodeName(event.getNodeName())
                .nodeType(event.getNodeType())
                .data(event.getData())
                .timestamp(event.getTimestamp())
                .error(event.getError())
                .build();
    }

    private ExecutionDTO toDTO(ExecutionResult result, String workflowName) {
        return ExecutionDTO.builder()
                .id(result.getExecutionId())
                .workflowId(result.getWorkflowId())
                .workflowName(workflowName)
                .status(result.getStatus().getCode())
                .input(result.getInput())
                .output(result.getOutput())
                .nodeResults(convertNodeResultsToDTO(result.getNodeResults()))
                .error(result.getError())
                .startTime(result.getStartTime())
                .endTime(result.getEndTime())
                .durationMs(result.getDurationMs())
                .build();
    }

    private List<NodeExecutionResultDTO> convertNodeResultsToDTO(List<NodeExecutionResult> results) {
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .map(r -> NodeExecutionResultDTO.builder()
                        .nodeId(r.getNodeId())
                        .nodeName(r.getNodeName())
                        .status(r.getStatus().getCode())
                        .input(r.getInput())
                        .output(r.getOutput())
                        .error(r.getError())
                        .startTime(r.getStartTime())
                        .endTime(r.getEndTime())
                        .durationMs(r.getDurationMs())
                        .build())
                .collect(Collectors.toList());
    }
}
