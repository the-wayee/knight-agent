"use client"

import React, { useEffect, useCallback, useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  addEdge,
  useNodesState,
  useEdgesState,
  type Connection,
  type Node,
  type Edge,
  BackgroundVariant,
  Panel,
} from "@xyflow/react"
import "@xyflow/react/dist/style.css"

import { useWorkflow } from "@/lib/hooks"
import { workflowApi } from "@/lib/api/workflow"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { useToast } from "@/hooks/use-toast"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { NodePalette } from "@/components/canvas/node-palette"
import { ConfigPanel } from "@/components/panels/config-panel"
import { ExecutionPanel } from "@/components/execution/execution-panel"
import { nodeTypes } from "@/components/nodes"
import type { NodeType, WorkflowNode, WorkflowEdge } from "@/lib/workflow/types"
import {
  ArrowLeft,
  Save,
  Play,
  Undo2,
  Redo2,
  ZoomIn,
  ZoomOut,
  PanelRightClose,
  PanelRightOpen,
  ChevronDown,
  ChevronUp,
  Loader2,
} from "lucide-react"

interface WorkflowEditorProps {
  id: string
}

export function WorkflowEditor({ id }: WorkflowEditorProps) {
  const router = useRouter()
  const { toast } = useToast()
  const { workflow, loading: workflowLoading, error: workflowError, updateWorkflow: apiUpdateWorkflow } = useWorkflow(id)

  // Suppress ResizeObserver loop error (benign warning from ReactFlow)
  useEffect(() => {
    const resizeObserverError = (e: ErrorEvent) => {
      if (e.message === "ResizeObserver loop completed with undelivered notifications.") {
        e.stopImmediatePropagation()
      }
    }
    window.addEventListener("error", resizeObserverError)
    return () => window.removeEventListener("error", resizeObserverError)
  }, [])

  const [nodes, setNodes, onNodesChange] = useNodesState([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const [workflowName, setWorkflowName] = useState("")
  const [showConfigPanel, setShowConfigPanel] = useState(true)
  const [showExecutionPanel, setShowExecutionPanel] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [selectedNode, setSelectedNode] = useState<WorkflowNode | null>(null)

  // Initialize nodes and edges from workflow
  useEffect(() => {
    if (workflow) {
      setWorkflowName(workflow.name)
      setNodes(
        workflow.nodes.map((n) => ({
          id: n.id,
          type: n.type,
          position: n.position,
          data: { ...n.data, label: n.name },
        }))
      )
      setEdges(
        workflow.edges.map((e) => ({
          id: e.id,
          source: e.source,
          target: e.target,
          sourceHandle: e.sourceHandle,
          targetHandle: e.targetHandle,
        }))
      )
    }
  }, [workflow, setNodes, setEdges])

  const onConnect = useCallback(
    (connection: Connection) => {
      setEdges((eds) => addEdge({ ...connection, id: `e-${Date.now()}` }, eds))
    },
    [setEdges]
  )

  const onNodeClick = useCallback(
    (_: React.MouseEvent, node: Node) => {
      // Use ReactFlow nodes instead of workflow nodes to support newly added nodes
      const workflowNode: WorkflowNode = {
        id: node.id,
        type: node.type as NodeType,
        name: (node.data.label as string) || node.type,
        position: node.position,
        data: node.data as Record<string, unknown>,
      }
      setSelectedNode(workflowNode)
    },
    []
  )

  const onPaneClick = useCallback(() => {
    setSelectedNode(null)
  }, [])

  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault()
    event.dataTransfer.dropEffect = "move"
  }, [])

  const onDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault()

      const type = event.dataTransfer.getData("application/reactflow") as NodeType
      if (!type) return

      const position = {
        x: event.clientX - 250,
        y: event.clientY - 100,
      }

      const newNodeId = `${type}-${Date.now()}`
      const newNode: Node = {
        id: newNodeId,
        type,
        position,
        data: { label: `${type.charAt(0).toUpperCase() + type.slice(1)} Node` },
      }

      setNodes((nds) => nds.concat(newNode))
    },
    [setNodes]
  )

  const handleAddNode = useCallback(
    (type: NodeType) => {
      const position = {
        x: 300 + Math.random() * 100,
        y: 200 + Math.random() * 100,
      }

      const newNodeId = `${type}-${Date.now()}`
      const newNode: Node = {
        id: newNodeId,
        type,
        position,
        data: { label: `${type.charAt(0).toUpperCase() + type.slice(1)} Node` },
      }

      setNodes((nds) => nds.concat(newNode))
    },
    [setNodes]
  )

  const handleSave = useCallback(async () => {
    if (!workflow) return

    setIsSaving(true)
    try {
      const updatedNodes: WorkflowNode[] = nodes.map((node) => ({
        id: node.id,
        type: node.type as NodeType,
        name: node.data.label || node.type,
        position: node.position,
        data: node.data,
      }))

      const updatedEdges: WorkflowEdge[] = edges.map((edge) => ({
        id: edge.id,
        source: edge.source,
        target: edge.target,
        sourceHandle: edge.sourceHandle,
        targetHandle: edge.targetHandle,
      }))

      await apiUpdateWorkflow({
        name: workflowName,
        nodes: updatedNodes,
        edges: updatedEdges,
      })

      toast({
        title: "Workflow saved",
        description: "Your workflow has been saved successfully.",
      })
    } catch (error) {
      toast({
        title: "Failed to save",
        description: error instanceof Error ? error.message : "An error occurred while saving.",
        variant: "destructive",
      })
    } finally {
      setIsSaving(false)
    }
  }, [workflow, nodes, edges, workflowName, apiUpdateWorkflow, toast])

  // Use selectedNode directly since onNodeClick already constructs it properly
  const currentNode: WorkflowNode | undefined = selectedNode ?? undefined

  // Handle new workflow creation
  useEffect(() => {
    if (id === "new" && !workflowLoading && !workflow) {
      // Create a new workflow via API
      workflowApi.createWorkflow({
        name: "New Workflow",
        description: "",
        nodes: [],
        edges: [],
      }).then((newWorkflow) => {
        router.replace(`/workflow/${newWorkflow.id}`)
      }).catch((error) => {
        toast({
          title: "Failed to create workflow",
          description: error instanceof Error ? error.message : "An error occurred",
          variant: "destructive",
        })
      })
    }
  }, [id, workflow, workflowLoading, router, toast])

  // Loading state
  if (workflowLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin text-primary mx-auto" />
          <p className="mt-4 text-muted-foreground">Loading workflow...</p>
        </div>
      </div>
    )
  }

  // Error state
  if (workflowError && !workflow) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="text-center max-w-md">
          <h2 className="text-xl font-semibold text-foreground mb-2">Error loading workflow</h2>
          <p className="text-muted-foreground mb-4">{workflowError}</p>
          <Button asChild>
            <Link href="/workflow">Back to Workflows</Link>
          </Button>
        </div>
      </div>
    )
  }

  // Workflow not found (but not loading)
  if (!workflow && id !== "new") {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="text-center">
          <h2 className="text-xl font-semibold text-foreground">Workflow not found</h2>
          <p className="mt-2 text-muted-foreground">The workflow you are looking for does not exist.</p>
          <Button asChild className="mt-4">
            <Link href="/workflow">Back to Workflows</Link>
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-screen flex-col bg-background">
      {/* Top Toolbar */}
      <div className="flex h-14 items-center justify-between border-b bg-card px-4">
        <div className="flex items-center gap-4">
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button variant="ghost" size="icon" asChild>
                  <Link href="/workflow">
                    <ArrowLeft className="h-4 w-4" />
                  </Link>
                </Button>
              </TooltipTrigger>
              <TooltipContent>Back to Workflows</TooltipContent>
            </Tooltip>
          </TooltipProvider>

          <div className="flex items-center gap-2">
            <Input
              value={workflowName}
              onChange={(e) => setWorkflowName(e.target.value)}
              className="h-8 w-48 font-medium"
            />
            <Badge variant="secondary" className="text-xs">
              {workflow?.status || "draft"}
            </Badge>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button variant="ghost" size="icon">
                  <Undo2 className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Undo</TooltipContent>
            </Tooltip>
          </TooltipProvider>

          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button variant="ghost" size="icon">
                  <Redo2 className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Redo</TooltipContent>
            </Tooltip>
          </TooltipProvider>

          <div className="mx-2 h-6 w-px bg-border" />

          <Button variant="outline" size="sm" onClick={handleSave} disabled={isSaving}>
            <Save className="mr-2 h-4 w-4" />
            {isSaving ? "Saving..." : "Save"}
          </Button>

          <Button size="sm" onClick={() => setShowExecutionPanel(true)}>
            <Play className="mr-2 h-4 w-4" />
            Run
          </Button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex flex-1 overflow-hidden">
        {/* Node Palette */}
        <NodePalette onAddNode={handleAddNode} />

        {/* Canvas */}
        <div className="flex-1 relative">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeClick={onNodeClick}
            onPaneClick={onPaneClick}
            onDragOver={onDragOver}
            onDrop={onDrop}
            nodeTypes={nodeTypes}
            fitView
            snapToGrid
            snapGrid={[15, 15]}
            defaultEdgeOptions={{
              type: "smoothstep",
              style: { strokeWidth: 2 },
            }}
            className="focus:outline-none [&_.react-flow__node:focus]:outline-none"
          >
            <Background variant={BackgroundVariant.Dots} gap={20} size={1} color="#e5e7eb" />
            <Controls showInteractive={false}>
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <button className="react-flow__controls-button">
                      <ZoomIn className="h-4 w-4" />
                    </button>
                  </TooltipTrigger>
                  <TooltipContent side="right">Zoom In</TooltipContent>
                </Tooltip>
              </TooltipProvider>
            </Controls>
            <MiniMap
              nodeColor={(node) => {
                switch (node.type) {
                  case "input":
                    return "#22c55e"
                  case "output":
                    return "#ef4444"
                  case "agent":
                    return "#8b5cf6"
                  case "code":
                    return "#f59e0b"
                  case "condition":
                    return "#06b6d4"
                  case "http":
                    return "#3b82f6"
                  case "tool":
                    return "#ec4899"
                  default:
                    return "#94a3b8"
                }
              }}
              maskColor="rgba(0, 0, 0, 0.1)"
              className="!bg-card !border !border-border"
            />

            {/* Canvas Controls Panel */}
            <Panel position="top-right" className="flex gap-2">
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-8 w-8 bg-card"
                      onClick={() => setShowConfigPanel(!showConfigPanel)}
                    >
                      {showConfigPanel ? (
                        <PanelRightClose className="h-4 w-4" />
                      ) : (
                        <PanelRightOpen className="h-4 w-4" />
                      )}
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>{showConfigPanel ? "Hide Config" : "Show Config"}</TooltipContent>
                </Tooltip>
              </TooltipProvider>
            </Panel>
          </ReactFlow>

          {/* Execution Panel Toggle */}
          <div className="absolute bottom-0 left-0 right-0 z-20">
            <button
              onClick={() => setShowExecutionPanel(!showExecutionPanel)}
              className="mx-auto flex h-6 w-32 items-center justify-center rounded-t-lg bg-card border border-b-0 border-border hover:bg-muted shadow-lg"
            >
              {showExecutionPanel ? (
                <ChevronDown className="h-4 w-4" />
              ) : (
                <ChevronUp className="h-4 w-4" />
              )}
            </button>
            {showExecutionPanel && (
              <ExecutionPanel
                workflowId={workflow?.id || "temp"}
                nodes={nodes.map((n) => ({
                  id: n.id,
                  type: n.type as NodeType,
                  name: (n.data.label as string) || n.type,
                  position: n.position,
                  data: n.data as Record<string, unknown>,
                }))}
                edges={edges.map((e) => ({
                  id: e.id,
                  source: e.source,
                  target: e.target,
                  sourceHandle: e.sourceHandle,
                  targetHandle: e.targetHandle,
                }))}
                onClose={() => setShowExecutionPanel(false)}
                onSaveBeforeExecute={async () => {
                  // Save workflow before execution
                  const updatedNodes: WorkflowNode[] = nodes.map((node) => ({
                    id: node.id,
                    type: node.type as NodeType,
                    name: node.data.label || node.type,
                    position: node.position,
                    data: node.data,
                  }))
                  const updatedEdges: WorkflowEdge[] = edges.map((edge) => ({
                    id: edge.id,
                    source: edge.source,
                    target: edge.target,
                    sourceHandle: edge.sourceHandle,
                    targetHandle: edge.targetHandle,
                  }))
                  const saved = await apiUpdateWorkflow({
                    name: workflowName,
                    nodes: updatedNodes,
                    edges: updatedEdges,
                  })
                  return saved.id
                }}
              />
            )}
          </div>
        </div>

        {/* Config Panel */}
        {showConfigPanel && (
          <ConfigPanel
            node={currentNode || null}
            onUpdate={(updates) => {
              if (currentNode) {
                // Update ReactFlow nodes for immediate feedback
                setNodes((nds) =>
                  nds.map((n) =>
                    n.id === currentNode.id
                      ? {
                          ...n,
                          data: { ...n.data, ...updates },
                        }
                      : n
                  )
                )
                // Also update selectedNode to reflect changes in the config panel
                setSelectedNode({
                  ...currentNode,
                  data: { ...currentNode.data, ...updates },
                  name: (updates.label as string) || currentNode.name,
                })
              }
            }}
            onClose={() => setShowConfigPanel(false)}
          />
        )}
      </div>
    </div>
  )
}
