/**
 * 工作流 API Hooks
 */

"use client"

import { useEffect, useCallback, useState } from "react"
import { workflowApi, type CreateWorkflowRequest, type UpdateWorkflowRequest } from "@/lib/api/workflow"
import type { Workflow } from "@/lib/workflow/types"

export function useWorkflows() {
  const [workflows, setWorkflows] = useState<Workflow[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchWorkflows = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await workflowApi.listWorkflows()
      setWorkflows(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to fetch workflows")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchWorkflows()
  }, [fetchWorkflows])

  const createWorkflow = useCallback(async (request: CreateWorkflowRequest) => {
    setError(null)
    try {
      const newWorkflow = await workflowApi.createWorkflow(request)
      setWorkflows((prev) => [...prev, newWorkflow])
      return newWorkflow
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Failed to create workflow"
      setError(errorMessage)
      throw err
    }
  }, [])

  const deleteWorkflow = useCallback(async (id: string) => {
    setError(null)
    try {
      await workflowApi.deleteWorkflow(id)
      setWorkflows((prev) => prev.filter((wf) => wf.id !== id))
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete workflow")
      throw err
    }
  }, [])

  return {
    workflows,
    loading,
    error,
    fetchWorkflows,
    createWorkflow,
    deleteWorkflow,
  }
}

export function useWorkflow(id: string) {
  const [workflow, setWorkflow] = useState<Workflow | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchWorkflow = useCallback(async () => {
    if (!id) return

    setLoading(true)
    setError(null)
    try {
      const data = await workflowApi.getWorkflow(id)
      setWorkflow(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to fetch workflow")
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    fetchWorkflow()
  }, [fetchWorkflow])

  const updateWorkflow = useCallback(async (request: UpdateWorkflowRequest) => {
    if (!id) throw new Error("Workflow ID is required")

    setError(null)
    try {
      const updated = await workflowApi.updateWorkflow(id, request)
      setWorkflow(updated)
      return updated
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Failed to update workflow"
      setError(errorMessage)
      throw err
    }
  }, [id])

  return {
    workflow,
    loading,
    error,
    fetchWorkflow,
    updateWorkflow,
  }
}
