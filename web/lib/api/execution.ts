/**
 * 执行 API
 */

import { apiClient, ApiResponse } from "./client"
import type { WorkflowExecution, ExecutionLog } from "../workflow/types"

export interface ExecuteRequest {
  input?: Record<string, unknown>
  async?: boolean
  stream?: boolean
}

export interface ExecutionDTO {
  id: string
  workflowId: string
  workflowName: string
  status: string
  input?: Record<string, unknown>
  output?: Record<string, unknown>
  nodeResults?: NodeExecutionResultDTO[]
  error?: string
  startTime: string
  endTime?: string
  durationMs?: number
}

export interface NodeExecutionResultDTO {
  nodeId: string
  nodeName: string
  status: string
  input?: Record<string, unknown>
  output?: Record<string, unknown>
  error?: string
  startTime: string
  endTime?: string
  durationMs?: number
}

export interface StreamInfoDTO {
  executionId: string
  wsUrl: string
}

export interface ExecutionEvent {
  type: ExecutionEventType
  executionId: string
  nodeId?: string
  nodeName?: string
  nodeType?: string
  data?: Record<string, unknown>
  timestamp: string
  error?: string
}

export type ExecutionEventType =
  | "workflow_started"
  | "workflow_completed"
  | "workflow_failed"
  | "node_started"
  | "node_completed"
  | "node_failed"
  | "token"
  | "tool_call"
  | "reasoning"

function toExecutionStatus(status: string): WorkflowExecution["status"] {
  switch (status) {
    case "pending":
      return "pending"
    case "running":
      return "running"
    case "completed":
      return "completed"
    case "failed":
      return "failed"
    default:
      return "pending"
  }
}

function toWorkflowExecution(dto: ExecutionDTO): WorkflowExecution {
  return {
    id: dto.id,
    workflowId: dto.workflowId,
    workflowName: dto.workflowName,
    status: toExecutionStatus(dto.status),
    startedAt: dto.startTime,
    completedAt: dto.endTime,
    input: dto.input,
    output: dto.output,
    error: dto.error,
    logs: [], // 后端暂不返回详细日志
  }
}

/**
 * 执行 API 客户端
 */
export const executionApi = {
  /**
   * 同步执行工作流
   */
  async execute(workflowId: string, request: ExecuteRequest): Promise<WorkflowExecution> {
    const response = await apiClient.post<ApiResponse<ExecutionDTO>>(
      `/api/workflows/${workflowId}/execute`,
      request
    )
    if (response.success && response.data) {
      return toWorkflowExecution(response.data)
    }
    throw new Error(response.error || "Failed to execute workflow")
  },

  /**
   * 异步执行工作流
   */
  async executeAsync(workflowId: string, request: ExecuteRequest): Promise<WorkflowExecution> {
    const response = await apiClient.post<ApiResponse<ExecutionDTO>>(
      `/api/workflows/${workflowId}/execute-async`,
      request
    )
    if (response.success && response.data) {
      return toWorkflowExecution(response.data)
    }
    throw new Error(response.error || "Failed to execute workflow")
  },

  /**
   * 获取执行详情
   */
  async getExecution(executionId: string): Promise<WorkflowExecution> {
    const response = await apiClient.get<ApiResponse<ExecutionDTO>>(`/api/executions/${executionId}`)
    if (response.success && response.data) {
      return toWorkflowExecution(response.data)
    }
    throw new Error(response.error || "Failed to fetch execution")
  },

  /**
   * 获取工作流的执行历史
   */
  async getExecutionHistory(workflowId: string): Promise<WorkflowExecution[]> {
    const response = await apiClient.get<ApiResponse<ExecutionDTO[]>>(
      `/api/workflows/${workflowId}/executions`
    )
    if (response.success && response.data) {
      return response.data.map(toWorkflowExecution)
    }
    throw new Error(response.error || "Failed to fetch execution history")
  },

  /**
   * 取消执行
   */
  async cancelExecution(executionId: string): Promise<void> {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/executions/${executionId}`)
    if (!response.success) {
      throw new Error(response.error || "Failed to cancel execution")
    }
  },

  /**
   * 流式执行工作流 (SSE)
   * 返回 EventSource 和初始执行信息
   */
  executeStream(
    workflowId: string,
    request: ExecuteRequest,
    onEvent: (event: ExecutionEvent) => void
  ): () => void {
    const baseUrl = apiClient["baseUrl"] || "http://localhost:8080"
    const url = `${baseUrl}/api/workflows/${workflowId}/stream`

    // 使用 fetch API 处理 SSE
    const controller = new AbortController()
    const signal = controller.signal

    fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "text/event-stream",
      },
      body: JSON.stringify(request),
      signal,
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const reader = response.body?.getReader()
        if (!reader) {
          throw new Error("No reader available")
        }

        const decoder = new TextDecoder()
        let buffer = ""
        let currentData = ""
        let currentEventType = ""

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })

          // 处理 SSE 格式（按行分割）
          const lines = buffer.split("\n")
          buffer = lines.pop() || ""

          for (const line of lines) {
            if (line === "") {
              // 空行表示事件结束，处理当前事件
              if (currentData.trim()) {
                try {
                  const data = JSON.parse(currentData)

                  // 检查是否是特殊的 complete 事件（有 id 和 workflowId 字段）
                  if (data.id && data.workflowId && !data.type) {
                    // 这是 ExecutionDTO，表示执行完成，但不发送事件
                    // 因为 workflow_completed 事件已经被处理了
                  } else if (data.type) {
                    // 普通的 ExecutionEvent
                    const eventType = data.type.toLowerCase() as ExecutionEventType
                    onEvent({ ...data, type: eventType })
                  } else if (currentEventType === "complete") {
                    // 处理后端明确标记为 complete 的事件，但 data 中没有 type
                    // 这里可以选择触发一个完成回调，或者什么都不做
                  }
                } catch (e) {
                  console.error("Failed to parse SSE event:", currentData, e)
                }
              }
              currentData = ""
              currentEventType = ""
            } else if (line.startsWith("event:")) {
              currentEventType = line.slice(7).trim()
            } else if (line.startsWith("data:")) {
              const data = line.slice(5).trim()
              if (data) {
                currentData = currentData ? currentData + "\n" + data : data
              }
            }
          }
        }
      })
      .catch((error) => {
        if (error.name !== "AbortError") {
          console.error("SSE error:", error)
        }
      })

    // 返回取消函数
    return () => controller.abort()
  },
}
