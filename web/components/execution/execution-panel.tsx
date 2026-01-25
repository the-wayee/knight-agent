"use client"

import { useState, useCallback } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Badge } from "@/components/ui/badge"
import { X, CheckCircle2, Loader2, AlertCircle, ChevronRight, Play, RotateCcw } from "lucide-react"
import { cn } from "@/lib/utils"
import { executionApi, type ExecutionEvent } from "@/lib/api/execution"
import type { WorkflowNode, WorkflowEdge } from "@/lib/workflow/types"

interface ExecutionPanelProps {
  workflowId: string
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
  onClose: () => void
  onSaveBeforeExecute?: () => Promise<string>
}

interface LogEntry {
  id: string
  timestamp: string
  nodeId?: string
  nodeName?: string
  type: "start" | "progress" | "success" | "error" | "output" | "info"
  message: string
  data?: unknown
  expanded?: boolean
  icon?: React.ReactNode // 自定义图标
}

export function ExecutionPanel({ workflowId, nodes, edges, onClose, onSaveBeforeExecute }: ExecutionPanelProps) {
  const [isRunning, setIsRunning] = useState(false)
  const [logs, setLogs] = useState<LogEntry[]>([])
  const [inputData, setInputData] = useState<Record<string, string>>({})
  const [outputData, setOutputData] = useState<Record<string, unknown> | null>(null)
  const [expandedLogs, setExpandedLogs] = useState<Set<string>>(new Set())
  const [nodeStates, setNodeStates] = useState<Map<string, 'running' | 'completed' | 'failed'>>(new Map())

  // Get input node to determine input fields
  const inputNode = nodes.find((n) => n.type === "input")
  const inputFields = (inputNode?.data.fields as Array<{ name: string; type: string; required: boolean }>) || []

  // Add log entry
  const addLog = useCallback((entry: Omit<LogEntry, "id" | "timestamp">) => {
    const now = new Date()
    const timestamp = now.toTimeString().split(" ")[0]
    const id = `${Date.now()}-${Math.random()}`
    setLogs((prev) => [...prev, { ...entry, id, timestamp }])
  }, [])

  // Update log icon for a specific node
  const updateLogIcon = useCallback((logId: string, icon: React.ReactNode) => {
    setLogs((prev) => prev.map((log) => log.id === logId ? { ...log, icon } : log))
  }, [])

  // Clear logs
  const clearLogs = useCallback(() => {
    setLogs([])
    setOutputData(null)
  }, [])

  // Toggle log expansion
  const toggleExpand = useCallback((id: string) => {
    setExpandedLogs((prev) => {
      const newExpanded = new Set(prev)
      if (newExpanded.has(id)) {
        newExpanded.delete(id)
      } else {
        newExpanded.add(id)
      }
      return newExpanded
    })
  }, [])

  // Execute workflow
  const executeWorkflow = useCallback(async () => {
    if (isRunning) return

    // Check if workflow needs to be saved first
    let actualWorkflowId = workflowId
    if (workflowId === "temp") {
      if (!onSaveBeforeExecute) {
        addLog({
          type: "error",
          message: "Please save the workflow first before executing",
        })
        return
      }
      addLog({
        type: "info",
        message: "Saving workflow...",
      })
      actualWorkflowId = await onSaveBeforeExecute()
      addLog({
        type: "success",
        message: "Workflow saved",
      })
    }

    setIsRunning(true)
    clearLogs()
    setNodeStates(new Map()) // 重置节点状态

    addLog({
      type: "info",
      message: "Starting workflow execution...",
    })

    // Prepare input data
    const input: Record<string, unknown> = {}
    for (const field of inputFields) {
      const value = inputData[field.name]
      if (field.required && !value) {
        addLog({
          type: "error",
          message: `Missing required field: ${field.name}`,
        })
        setIsRunning(false)
        return
      }
      // Convert value based on type
      switch (field.type) {
        case "number":
          input[field.name] = value ? Number(value) : 0
          break
        case "boolean":
          input[field.name] = value === "true"
          break
        case "json":
          try {
            input[field.name] = value ? JSON.parse(value) : {}
          } catch {
            input[field.name] = {}
          }
          break
        default:
          input[field.name] = value || ""
      }
    }

    // If no input fields defined, use raw input data
    if (inputFields.length === 0 && Object.keys(inputData).length > 0) {
      try {
        // Try to parse as JSON first
        Object.assign(input, JSON.parse(inputData.raw || "{}"))
      } catch {
        // If not JSON, use as plain text
        input._input = inputData.raw || ""
      }
    }

    let completed = false

    try {
      const cleanup = executionApi.executeStream(
        actualWorkflowId,
        { input, async: false, stream: true },
        (event: ExecutionEvent) => {
          switch (event.type) {
            case "workflow_started":
              addLog({
                type: "start",
                message: "Workflow execution started",
                icon: <Loader2 className="h-4 w-4 text-blue-500 animate-spin" />,
              })
              break
            case "node_started":
              if (event.nodeId) {
                setNodeStates((prev) => new Map(prev).set(event.nodeId, "running"))
              }
              addLog({
                nodeId: event.nodeId,
                nodeName: event.nodeName,
                type: "start",
                message: `Starting node: ${event.nodeName}`,
              })
              break
            case "node_completed":
              if (event.nodeId) {
                setNodeStates((prev) => new Map(prev).set(event.nodeId, "completed"))
              }
              addLog({
                nodeId: event.nodeId,
                nodeName: event.nodeName,
                type: "success",
                message: `Completed: ${event.nodeName}`,
                data: event.data,
              })
              // Check if this is an output node
              const node = nodes.find((n) => n.id === event.nodeId)
              if (node?.type === "output" && event.data) {
                setOutputData(event.data as Record<string, unknown>)
              }
              break
            case "node_failed":
              if (event.nodeId) {
                setNodeStates((prev) => new Map(prev).set(event.nodeId, "failed"))
              }
              addLog({
                nodeId: event.nodeId,
                nodeName: event.nodeName,
                type: "error",
                message: `Failed: ${event.nodeName}`,
                data: event.error,
              })
              break
            case "token":
              addLog({
                nodeId: event.nodeId,
                nodeName: event.nodeName,
                type: "progress",
                message: (event.data?.token as string) || "",
              })
              break
            case "workflow_completed":
              addLog({
                type: "success",
                message: "Workflow completed successfully",
                data: event.data,
                icon: <CheckCircle2 className="h-4 w-4 text-green-500" />,
              })
              if (event.data?.output) {
                setOutputData(event.data.output as Record<string, unknown>)
              }
              completed = true
              setIsRunning(false)
              cleanup() // 立即清理连接
              break
            case "workflow_failed":
              addLog({
                type: "error",
                message: `Workflow failed: ${event.error || "Unknown error"}`,
                icon: <AlertCircle className="h-4 w-4 text-red-500" />,
              })
              completed = true
              setIsRunning(false)
              cleanup() // 立即清理连接
              break
          }
        }
      )

      // 30秒超时（作为备用机制）
      setTimeout(() => {
        if (!completed) {
          addLog({
            type: "error",
            message: "Execution timeout",
          })
          setIsRunning(false)
          cleanup()
        }
      }, 30000)
    } catch (error) {
      addLog({
        type: "error",
        message: error instanceof Error ? error.message : "Failed to execute workflow",
      })
      setIsRunning(false)
    }
  }, [workflowId, inputData, inputFields, nodes, isRunning, addLog, clearLogs, onSaveBeforeExecute, nodeStates])

  // Get log icon
  const getLogIcon = (type: LogEntry["type"]) => {
    switch (type) {
      case "success":
        return <CheckCircle2 className="h-4 w-4 text-green-500" />
      case "error":
        return <AlertCircle className="h-4 w-4 text-red-500" />
      case "start":
      case "progress":
        return <Loader2 className="h-4 w-4 text-blue-500 animate-spin" />
      case "output":
      case "info":
        return <ChevronRight className="h-4 w-4 text-muted-foreground" />
      default:
        return null
    }
  }

  return (
    <div className="absolute bottom-0 left-0 right-0 h-96 border-t border-border bg-card flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-2 border-b border-border">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-semibold">Execution Panel</h3>
          {isRunning && (
            <Badge variant="secondary" className="gap-1">
              <Loader2 className="h-3 w-3 animate-spin" />
              Running
            </Badge>
          )}
        </div>
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" onClick={clearLogs} disabled={isRunning}>
            <RotateCcw className="h-3 w-3 mr-1" />
            Clear
          </Button>
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 flex overflow-hidden min-h-0">
        {/* Input Section */}
        <div className="w-80 border-r border-border p-4 overflow-y-auto flex-shrink-0">
          <h4 className="text-sm font-medium mb-3">Input Data</h4>

          {inputFields.length > 0 ? (
            <div className="space-y-3">
              {inputFields.map((field) => (
                <div key={field.name} className="space-y-1">
                  <Label htmlFor={`input-${field.name}`} className="text-xs">
                    {field.name}
                    {field.required && <span className="text-red-500 ml-1">*</span>}
                  </Label>
                  {field.type === "text" || field.type === "json" ? (
                    <Textarea
                      id={`input-${field.name}`}
                      value={inputData[field.name] || ""}
                      onChange={(e) => setInputData((prev) => ({ ...prev, [field.name]: e.target.value }))}
                      placeholder={`Enter ${field.name}...`}
                      rows={field.type === "json" ? 4 : 2}
                      className="text-sm"
                      disabled={isRunning}
                    />
                  ) : (
                    <Input
                      id={`input-${field.name}`}
                      type={field.type === "number" ? "number" : "text"}
                      value={inputData[field.name] || ""}
                      onChange={(e) => setInputData((prev) => ({ ...prev, [field.name]: e.target.value }))}
                      placeholder={`Enter ${field.name}...`}
                      className="text-sm"
                      disabled={isRunning}
                    />
                  )}
                  <span className="text-xs text-muted-foreground">{field.type}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="space-y-3">
              <div className="space-y-1">
                <Label htmlFor="raw-input" className="text-xs">
                  Input (JSON)
                </Label>
                <Textarea
                  id="raw-input"
                  value={inputData.raw || '{"message": "Hello, World!"}'}
                  onChange={(e) => setInputData((prev) => ({ ...prev, raw: e.target.value }))}
                  placeholder='{"key": "value"}'
                  rows={6}
                  className="text-sm font-mono"
                  disabled={isRunning}
                />
              </div>
              <p className="text-xs text-muted-foreground">
                Add an Input node to define custom input fields
              </p>
            </div>
          )}

          <Button
            className="w-full mt-4"
            onClick={executeWorkflow}
            disabled={isRunning}
          >
            {isRunning ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Running...
              </>
            ) : (
              <>
                <Play className="h-4 w-4 mr-2" />
                Execute
              </>
            )}
          </Button>
        </div>

        {/* Logs Section */}
        <div className="flex-1 flex flex-col min-h-0 overflow-hidden">
          {/* Output Display */}
          {outputData && (
            <div className="border-b border-border p-3 bg-muted/30 flex-shrink-0">
              <div className="flex items-center justify-between mb-2">
                <h4 className="text-sm font-medium">Output</h4>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-6 px-2 text-xs"
                  onClick={() => setOutputData(null)}
                >
                  Clear
                </Button>
              </div>
              <div className="h-24 overflow-auto">
                <pre className="text-xs bg-background p-2 rounded">
                  {JSON.stringify(outputData, null, 2)}
                </pre>
              </div>
            </div>
          )}

          {/* Logs */}
          <ScrollArea className="flex-1 min-h-0">
            <div className="p-2 space-y-1">
              {logs.length === 0 ? (
                <div className="flex items-center justify-center h-full text-sm text-muted-foreground">
                  <p>Click Execute to run the workflow</p>
                </div>
              ) : (
                logs.map((log) => {
                  // 使用自定义图标或根据节点状态确定图标
                  let icon = log.icon || getLogIcon(log.type)
                  if (log.nodeId && nodeStates.has(log.nodeId)) {
                    const state = nodeStates.get(log.nodeId)
                    if (state === "running") {
                      icon = <Loader2 className="h-4 w-4 text-blue-500 animate-spin" />
                    } else if (state === "completed") {
                      icon = <CheckCircle2 className="h-4 w-4 text-green-500" />
                    } else if (state === "failed") {
                      icon = <AlertCircle className="h-4 w-4 text-red-500" />
                    }
                  }

                  return (
                    <div
                      key={log.id}
                      className={cn(
                        "flex items-start gap-2 px-2 py-1.5 rounded text-sm hover:bg-muted/50 cursor-pointer",
                        log.type === "error" && "bg-destructive/5"
                      )}
                      onClick={() => (log.data || log.type === "output") && toggleExpand(log.id)}
                    >
                      <span className="text-xs text-muted-foreground font-mono w-16 flex-shrink-0">
                        {log.timestamp}
                      </span>
                      <div className="flex-shrink-0 mt-0.5">{icon}</div>
                      <div className="flex-1 min-w-0">
                        {log.nodeName && (
                          <span className="text-xs text-muted-foreground">[{log.nodeName}]</span>
                        )}{" "}
                        <span
                          className={cn(
                            log.type === "output" && "text-foreground font-medium",
                            log.type === "progress" && "text-foreground"
                          )}
                        >
                          {log.message}
                        </span>
                        {log.data && expandedLogs.has(log.id) && (
                          <pre className="mt-2 p-2 bg-muted rounded text-xs overflow-auto">
                            {typeof log.data === "string"
                              ? log.data
                              : JSON.stringify(log.data, null, 2)}
                          </pre>
                        )}
                      </div>
                    </div>
                  )
                })
              )}
            </div>
          </ScrollArea>
        </div>
      </div>
    </div>
  )
}
