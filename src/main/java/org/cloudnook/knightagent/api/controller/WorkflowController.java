package org.cloudnook.knightagent.api.controller;

import lombok.RequiredArgsConstructor;
import org.cloudnook.knightagent.api.dto.ApiResponse;
import org.cloudnook.knightagent.api.dto.CreateWorkflowDTO;
import org.cloudnook.knightagent.api.dto.UpdateWorkflowDTO;
import org.cloudnook.knightagent.api.dto.WorkflowDTO;
import org.cloudnook.knightagent.api.service.WorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

/**
 * 工作流控制器
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * 列出所有工作流
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkflowDTO>>> listWorkflows() {
        List<WorkflowDTO> workflows = workflowService.listWorkflows();
        return ResponseEntity.ok(ApiResponse.success(workflows));
    }

    /**
     * 获取工作流详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowDTO>> getWorkflow(@PathVariable String id) {
        try {
            WorkflowDTO workflow = workflowService.getWorkflow(id);
            return ResponseEntity.ok(ApiResponse.success(workflow));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 创建工作流
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WorkflowDTO>> createWorkflow(@RequestBody CreateWorkflowDTO dto) {
        try {
            WorkflowDTO workflow = workflowService.createWorkflow(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(workflow));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 更新工作流
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowDTO>> updateWorkflow(
            @PathVariable String id,
            @RequestBody UpdateWorkflowDTO dto) {
        try {
            WorkflowDTO workflow = workflowService.updateWorkflow(id, dto);
            return ResponseEntity.ok(ApiResponse.success(workflow));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 删除工作流
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkflow(@PathVariable String id) {
        try {
            workflowService.deleteWorkflow(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
