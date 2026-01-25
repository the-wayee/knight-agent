package org.cloudnook.knightagent.api.controller;

import lombok.RequiredArgsConstructor;
import org.cloudnook.knightagent.api.dto.*;
import org.cloudnook.knightagent.api.service.ExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 执行控制器
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ExecutionController {

    private final ExecutionService executionService;

    /**
     * 同步执行工作流
     */
    @PostMapping("/workflows/{id}/execute")
    public ResponseEntity<ApiResponse<ExecutionDTO>> executeWorkflow(
            @PathVariable String id,
            @RequestBody ExecuteRequestDTO request) {
        try {
            ExecutionDTO result = executionService.execute(id, request);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 异步执行工作流
     */
    @PostMapping("/workflows/{id}/execute-async")
    public ResponseEntity<ApiResponse<ExecutionDTO>> executeWorkflowAsync(
            @PathVariable String id,
            @RequestBody ExecuteRequestDTO request) {
        try {
            CompletableFuture<ExecutionDTO> future = executionService.executeAsync(id, request);
            ExecutionDTO result = future.get();
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 流式执行工作流（SSE）
     */
    @PostMapping(value = "/workflows/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamWorkflow(
            @PathVariable String id,
            @RequestBody ExecuteRequestDTO request) {
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时

        try {
            Consumer<org.cloudnook.knightagent.engine.ExecutionEvent> eventConsumer = event -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name(event.getType().name().toLowerCase())
                            .data(event));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            };

            ExecutionDTO result = executionService.executeStream(id, request, eventConsumer);

            // 发送完成事件
            emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(result));

            emitter.complete();

        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 获取执行详情
     */
    @GetMapping("/executions/{id}")
    public ResponseEntity<ApiResponse<ExecutionDTO>> getExecution(@PathVariable String id) {
        try {
            ExecutionDTO execution = executionService.getExecution(id);
            return ResponseEntity.ok(ApiResponse.success(execution));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取工作流的执行历史
     */
    @GetMapping("/workflows/{id}/executions")
    public ResponseEntity<ApiResponse<List<ExecutionDTO>>> getExecutionHistory(@PathVariable String id) {
        try {
            List<ExecutionDTO> history = executionService.getExecutionHistory(id);
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 取消执行
     */
    @DeleteMapping("/executions/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelExecution(@PathVariable String id) {
        try {
            executionService.cancelExecution(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
