package org.cloudnook.knightagent.workflow.engine;

import org.cloudnook.knightagent.workflow.definition.WorkflowDefinition;
import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 工作流执行引擎接口
 */
public interface WorkflowEngine {

    /**
     * 同步执行工作流
     *
     * @param workflow 工作流定义
     * @param input    输入数据
     * @return 执行结果
     */
    ExecutionResult execute(WorkflowDefinition workflow, Map<String, Object> input);

    /**
     * 异步执行工作流
     *
     * @param workflow 工作流定义
     * @param input    输入数据
     * @return 异步执行结果
     */
    CompletableFuture<ExecutionResult> executeAsync(WorkflowDefinition workflow, Map<String, Object> input);

    /**
     * 流式执行工作流（通过事件回调）
     *
     * @param workflow 工作流定义
     * @param input    输入数据
     * @param eventConsumer 事件消费者
     * @return 执行结果
     */
    ExecutionResult executeStream(WorkflowDefinition workflow, Map<String, Object> input,
                                   Consumer<ExecutionEvent> eventConsumer);

    /**
     * 流式执行工作流（响应式）
     *
     * @param workflow 工作流定义
     * @param input    输入数据
     * @return 事件发布器
     */
    Publisher<ExecutionEvent> executeStreamPublisher(WorkflowDefinition workflow, Map<String, Object> input);
}
