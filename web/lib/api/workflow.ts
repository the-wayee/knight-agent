/**
 * 工作流 API
 */

import { apiClient, ApiResponse } from "./client"
import type { Workflow, WorkflowNode, WorkflowEdge } from "../workflow/types"

export interface CreateWorkflowRequest {
  name: string
  description?: string
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
  settings?: Record<string, unknown>
  tags?: string[]
}

export interface UpdateWorkflowRequest {
  name?: string
  description?: string
  nodes?: WorkflowNode[]
  edges?: WorkflowEdge[]
  settings?: Record<string, unknown>
  tags?: string[]
}

export interface WorkflowDTO {
  id: string
  name: string
  description?: string
  version: number
  nodes: NodeDefinitionDTO[]
  edges: EdgeDefinitionDTO[]
  settings?: Record<string, unknown>
  tags?: string[]
  createdAt: string
  updatedAt: string
  createdBy?: string
}

export interface NodeDefinitionDTO {
  id: string
  type: string
  name: string
  position: { x: number; y: number }
  config?: Record<string, unknown>
  isStart?: boolean
  isEnd?: boolean
}

export interface EdgeDefinitionDTO {
  id: string
  source: string
  target: string
  sourceHandle?: string
  targetHandle?: string
  condition?: string
}

function toWorkflow(dto: WorkflowDTO): Workflow {
  return {
    id: dto.id,
    name: dto.name,
    description: dto.description,
    nodes: dto.nodes.map((n) => ({
      id: n.id,
      type: n.type as WorkflowNode["type"],
      name: n.name,
      position: n.position,
      data: n.config || {},
    })),
    edges: dto.edges.map((e) => ({
      id: e.id,
      source: e.source,
      target: e.target,
      sourceHandle: e.sourceHandle,
      targetHandle: e.targetHandle,
    })),
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
    status: "draft", // 后端暂不支持 status 字段
  }
}

function toNodeDefinitionDTO(node: WorkflowNode): NodeDefinitionDTO {
  return {
    id: node.id,
    type: node.type,
    name: node.name,
    position: node.position,
    config: node.data as Record<string, unknown>,
  }
}

function toEdgeDefinitionDTO(edge: WorkflowEdge): EdgeDefinitionDTO {
  return {
    id: edge.id,
    source: edge.source,
    target: edge.target,
    sourceHandle: edge.sourceHandle,
    targetHandle: edge.targetHandle,
  }
}

/**
 * 工作流 API 客户端
 */
export const workflowApi = {
  /**
   * 获取所有工作流
   */
  async listWorkflows(): Promise<Workflow[]> {
    const response = await apiClient.get<ApiResponse<WorkflowDTO[]>>("/api/workflows")
    if (response.success && response.data) {
      return response.data.map(toWorkflow)
    }
    throw new Error(response.error || "Failed to fetch workflows")
  },

  /**
   * 获取工作流详情
   */
  async getWorkflow(id: string): Promise<Workflow> {
    const response = await apiClient.get<ApiResponse<WorkflowDTO>>(`/api/workflows/${id}`)
    if (response.success && response.data) {
      return toWorkflow(response.data)
    }
    throw new Error(response.error || "Failed to fetch workflow")
  },

  /**
   * 创建工作流
   */
  async createWorkflow(request: CreateWorkflowRequest): Promise<Workflow> {
    const body = {
      name: request.name,
      description: request.description,
      nodes: request.nodes.map(toNodeDefinitionDTO),
      edges: request.edges.map(toEdgeDefinitionDTO),
      settings: request.settings,
      tags: request.tags,
    }
    const response = await apiClient.post<ApiResponse<WorkflowDTO>>("/api/workflows", body)
    if (response.success && response.data) {
      return toWorkflow(response.data)
    }
    throw new Error(response.error || "Failed to create workflow")
  },

  /**
   * 更新工作流
   */
  async updateWorkflow(id: string, request: UpdateWorkflowRequest): Promise<Workflow> {
    const body: Record<string, unknown> = {}
    if (request.name !== undefined) body.name = request.name
    if (request.description !== undefined) body.description = request.description
    if (request.nodes !== undefined) body.nodes = request.nodes.map(toNodeDefinitionDTO)
    if (request.edges !== undefined) body.edges = request.edges.map(toEdgeDefinitionDTO)
    if (request.settings !== undefined) body.settings = request.settings
    if (request.tags !== undefined) body.tags = request.tags

    const response = await apiClient.put<ApiResponse<WorkflowDTO>>(`/api/workflows/${id}`, body)
    if (response.success && response.data) {
      return toWorkflow(response.data)
    }
    throw new Error(response.error || "Failed to update workflow")
  },

  /**
   * 删除工作流
   */
  async deleteWorkflow(id: string): Promise<void> {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/workflows/${id}`)
    if (!response.success) {
      throw new Error(response.error || "Failed to delete workflow")
    }
  },
}
