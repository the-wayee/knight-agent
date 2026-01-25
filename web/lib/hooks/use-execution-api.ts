/**
 * 执行 API Hooks
 */

"use client"

import { useCallback, useState, useRef } from "react"
import { executionApi, type ExecuteRequest, type ExecutionEvent } from "@/lib/api/execution"
import type { WorkflowExecution } from "@/lib/workflow/types"

export function useExecution(workflowId: string) {
  const [execution, setExecution] = useState<WorkflowExecution | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [events, setEvents] = useState<ExecutionEvent[]>([])
  const abortControllerRef = useRef<(() => void) | null>(null)

  const execute = useCallback(async (request: ExecuteRequest) => {
    setLoading(true)
    setError(null)
    setEvents([])

    try {
      if (request.stream) {
        // 流式执行
        return new Promise<WorkflowExecution>((resolve, reject) => {
          let lastEventData: ExecutionEvent | null = null

          const cleanup = executionApi.executeStream(
            workflowId,
            request,
            (event) => {
              setEvents((prev) => [...prev, event])
              lastEventData = event

              // 处理完成事件
              if (event.type === "workflow_completed") {
                executionApi.getExecution(event.executionId).then((result) => {
                  setExecution(result)
                  setLoading(false)
                  resolve(result)
                }).catch(reject)
              } else if (event.type === "workflow_failed") {
                setLoading(false)
                reject(new Error(event.error || "Execution failed"))
              }
            }
          )

          abortControllerRef.current = cleanup
        })
      } else if (request.async) {
        // 异步执行
        const result = await executionApi.executeAsync(workflowId, request)
        setExecution(result)
        setLoading(false)
        return result
      } else {
        // 同步执行
        const result = await executionApi.execute(workflowId, request)
        setExecution(result)
        setLoading(false)
        return result
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Execution failed"
      setError(errorMessage)
      setLoading(false)
      throw err
    }
  }, [workflowId])

  const cancel = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current()
      abortControllerRef.current = null
    }
    setLoading(false)
  }, [])

  const reset = useCallback(() => {
    setExecution(null)
    setError(null)
    setEvents([])
    if (abortControllerRef.current) {
      abortControllerRef.current()
      abortControllerRef.current = null
    }
  }, [])

  return {
    execution,
    loading,
    error,
    events,
    execute,
    cancel,
    reset,
  }
}

export function useExecutionHistory(workflowId: string) {
  const [executions, setExecutions] = useState<WorkflowExecution[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchHistory = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await executionApi.getExecutionHistory(workflowId)
      setExecutions(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to fetch execution history")
    } finally {
      setLoading(false)
    }
  }, [workflowId])

  return {
    executions,
    loading,
    error,
    fetchHistory,
  }
}
