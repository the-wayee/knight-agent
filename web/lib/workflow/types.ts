export type NodeType = 
  | "input" 
  | "output" 
  | "agent" 
  | "code" 
  | "condition" 
  | "tool" 
  | "http"

export interface WorkflowNode {
  id: string
  type: NodeType
  name: string
  position: { x: number; y: number }
  data: Record<string, unknown>
}

export interface WorkflowEdge {
  id: string
  source: string
  target: string
  sourceHandle?: string
  targetHandle?: string
}

export interface Workflow {
  id: string
  name: string
  description?: string
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
  createdAt: string
  updatedAt: string
  status: "draft" | "published"
}

export interface WorkflowExecution {
  id: string
  workflowId: string
  workflowName: string
  status: "pending" | "running" | "completed" | "failed"
  startedAt: string
  completedAt?: string
  input?: Record<string, unknown>
  output?: Record<string, unknown>
  error?: string
  logs: ExecutionLog[]
}

export interface ExecutionLog {
  id: string
  nodeId: string
  nodeName: string
  timestamp: string
  level: "info" | "warning" | "error"
  message: string
  data?: unknown
}

export interface McpServer {
  id: string
  name: string
  url: string
  status: "connected" | "disconnected" | "error"
  tools: McpTool[]
}

export interface McpTool {
  name: string
  description: string
  inputSchema: Record<string, unknown>
}
