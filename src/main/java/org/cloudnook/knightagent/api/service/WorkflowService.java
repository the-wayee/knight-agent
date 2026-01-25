package org.cloudnook.knightagent.api.service;

import org.cloudnook.knightagent.api.dto.CreateWorkflowDTO;
import org.cloudnook.knightagent.api.dto.UpdateWorkflowDTO;
import org.cloudnook.knightagent.api.dto.WorkflowDTO;
import org.cloudnook.knightagent.workflow.definition.WorkflowDefinition;

import java.util.List;

/**
 * 工作流服务接口
 */
public interface WorkflowService {

    /**
     * 创建工作流
     */
    WorkflowDTO createWorkflow(CreateWorkflowDTO dto);

    /**
     * 更新工作流
     */
    WorkflowDTO updateWorkflow(String id, UpdateWorkflowDTO dto);

    /**
     * 删除工作流
     */
    void deleteWorkflow(String id);

    /**
     * 获取工作流详情
     */
    WorkflowDTO getWorkflow(String id);

    /**
     * 列出所有工作流
     */
    List<WorkflowDTO> listWorkflows();

    /**
     * 验证工作流定义
     */
    WorkflowDefinition validateWorkflow(WorkflowDefinition definition);
}
