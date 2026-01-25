package org.cloudnook.knightagent.api.service;

import org.cloudnook.knightagent.api.dto.ExecutionDTO;
import org.cloudnook.knightagent.api.dto.ExecuteRequestDTO;
import org.cloudnook.knightagent.engine.ExecutionResult;
import org.cloudnook.knightagent.workflow.definition.WorkflowDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 执行服务接口
 */
public interface ExecutionService {

    /**
     * 同步执行工作流
     */
    ExecutionDTO execute(String workflowId, ExecuteRequestDTO request);

    /**
     * 异步执行工作流
     */
    CompletableFuture<ExecutionDTO> executeAsync(String workflowId, ExecuteRequestDTO request);

    /**
     * 流式执行工作流
     */
    ExecutionDTO executeStream(String workflowId, ExecuteRequestDTO request, Consumer<org.cloudnook.knightagent.engine.ExecutionEvent> eventConsumer);

    /**
     * 获取执行详情
     */
    ExecutionDTO getExecution(String executionId);

    /**
     * 获取工作流的执行历史
     */
    List<ExecutionDTO> getExecutionHistory(String workflowId);

    /**
     * 取消执行
     */
    void cancelExecution(String executionId);
}
