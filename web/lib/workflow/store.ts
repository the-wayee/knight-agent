import { create } from "zustand"
import type { Workflow, WorkflowExecution, WorkflowNode, WorkflowEdge, McpServer, McpTool, ApiKey } from "./types"
import { apiKeysApi } from "@/lib/api/config/api-keys"
import { mcpServersApi } from "@/lib/api/config/mcp-servers"

// Demo workflows for showcase
const demoWorkflows: Workflow[] = [
  {
    id: "wf-1",
    name: "Customer Support Agent",
    description: "AI agent that handles customer inquiries using GPT-4",
    nodes: [
      { id: "n1", type: "input", name: "Input", position: { x: 100, y: 100 }, data: {} },
      { id: "n2", type: "agent", name: "Support Agent", position: { x: 300, y: 100 }, data: { model: "gpt-4o" } },
      { id: "n3", type: "output", name: "Output", position: { x: 500, y: 100 }, data: {} },
    ],
    edges: [
      { id: "e1", source: "n1", target: "n2" },
      { id: "e2", source: "n2", target: "n3" },
    ],
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    status: "published",
  },
  {
    id: "wf-2",
    name: "Product Search Pipeline",
    description: "Search and analyze product information from multiple sources",
    nodes: [
      { id: "n1", type: "input", name: "Input", position: { x: 100, y: 100 }, data: {} },
      { id: "n2", type: "agent", name: "Search Agent", position: { x: 300, y: 100 }, data: { model: "gpt-4o-mini" } },
      { id: "n3", type: "http", name: "API Call", position: { x: 500, y: 100 }, data: {} },
      { id: "n4", type: "code", name: "Process", position: { x: 700, y: 100 }, data: {} },
      { id: "n5", type: "output", name: "Output", position: { x: 900, y: 100 }, data: {} },
    ],
    edges: [
      { id: "e1", source: "n1", target: "n2" },
      { id: "e2", source: "n2", target: "n3" },
      { id: "e3", source: "n3", target: "n4" },
      { id: "e4", source: "n4", target: "n5" },
    ],
    createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 12 * 60 * 60 * 1000).toISOString(),
    status: "published",
  },
  {
    id: "wf-3",
    name: "Report Generator",
    description: "Generate comprehensive reports using multi-agent collaboration",
    nodes: [
      { id: "n1", type: "input", name: "Input", position: { x: 100, y: 150 }, data: {} },
      { id: "n2", type: "agent", name: "Data Analyst", position: { x: 300, y: 50 }, data: { model: "gpt-4o" } },
      { id: "n3", type: "agent", name: "Writer", position: { x: 300, y: 250 }, data: { model: "claude-3-5-sonnet" } },
      { id: "n4", type: "condition", name: "Check Quality", position: { x: 500, y: 150 }, data: {} },
      { id: "n5", type: "agent", name: "Editor", position: { x: 700, y: 50 }, data: { model: "gpt-4o" } },
      { id: "n6", type: "code", name: "Format", position: { x: 700, y: 250 }, data: {} },
      { id: "n7", type: "output", name: "Output", position: { x: 900, y: 150 }, data: {} },
    ],
    edges: [
      { id: "e1", source: "n1", target: "n2" },
      { id: "e2", source: "n1", target: "n3" },
      { id: "e3", source: "n2", target: "n4" },
      { id: "e4", source: "n3", target: "n4" },
      { id: "e5", source: "n4", target: "n5" },
      { id: "e6", source: "n4", target: "n6" },
      { id: "e7", source: "n5", target: "n7" },
      { id: "e8", source: "n6", target: "n7" },
    ],
    createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
    status: "draft",
  },
]

// Load API keys and MCP servers from backend
const loadApiKeys = async (): Promise<ApiKey[]> => {
  try {
    return await apiKeysApi.getAll()
  } catch {
    return []
  }
}

const loadMcpServers = async (): Promise<McpServer[]> => {
  try {
    return await mcpServersApi.getAll()
  } catch {
    return []
  }
}

const demoMcpServers: McpServer[] = [
  {
    id: "mcp-1",
    name: "Filesystem Server",
    url: "stdio:///usr/local/bin/mcp-filesystem",
    status: "connected",
    tools: [
      { name: "read_file", description: "Read contents of a file", inputSchema: { type: "object", properties: { path: { type: "string" } } } },
      { name: "write_file", description: "Write content to a file", inputSchema: { type: "object", properties: { path: { type: "string" }, content: { type: "string" } } } },
      { name: "list_directory", description: "List contents of a directory", inputSchema: { type: "object", properties: { path: { type: "string" } } } },
      { name: "create_directory", description: "Create a new directory", inputSchema: { type: "object", properties: { path: { type: "string" } } } },
      { name: "delete_file", description: "Delete a file or directory", inputSchema: { type: "object", properties: { path: { type: "string" } } } },
    ],
  },
  {
    id: "mcp-2",
    name: "PostgreSQL Server",
    url: "stdio:///usr/local/bin/mcp-postgres",
    status: "connected",
    tools: [
      { name: "query", description: "Execute a SQL query", inputSchema: { type: "object", properties: { sql: { type: "string" } } } },
      { name: "list_tables", description: "List all tables in database", inputSchema: { type: "object" } },
      { name: "describe_table", description: "Get table schema", inputSchema: { type: "object", properties: { table: { type: "string" } } } },
      { name: "insert", description: "Insert data into a table", inputSchema: { type: "object", properties: { table: { type: "string" }, data: { type: "object" } } } },
      { name: "update", description: "Update records in a table", inputSchema: { type: "object", properties: { table: { type: "string" }, where: { type: "object" }, data: { type: "object" } } } },
      { name: "delete", description: "Delete records from a table", inputSchema: { type: "object", properties: { table: { type: "string" }, where: { type: "object" } } } },
      { name: "create_table", description: "Create a new table", inputSchema: { type: "object", properties: { name: { type: "string" }, columns: { type: "array" } } } },
      { name: "drop_table", description: "Drop a table", inputSchema: { type: "object", properties: { table: { type: "string" } } } },
    ],
  },
  {
    id: "mcp-3",
    name: "Web Search Server",
    url: "http://localhost:3001/mcp",
    status: "connected",
    tools: [
      { name: "web_search", description: "Search the web for information", inputSchema: { type: "object", properties: { query: { type: "string" }, limit: { type: "number" } } } },
      { name: "fetch_page", description: "Fetch content from a URL", inputSchema: { type: "object", properties: { url: { type: "string" } } } },
      { name: "extract_links", description: "Extract links from a webpage", inputSchema: { type: "object", properties: { url: { type: "string" } } } },
    ],
  },
]

const demoExecutions: WorkflowExecution[] = [
  {
    id: "exec-1",
    workflowId: "wf-1",
    workflowName: "Customer Support Agent",
    status: "completed",
    startedAt: new Date(Date.now() - 2 * 60 * 1000).toISOString(),
    completedAt: new Date(Date.now() - 1 * 60 * 1000).toISOString(),
    logs: [],
  },
  {
    id: "exec-2",
    workflowId: "wf-2",
    workflowName: "Product Search Pipeline",
    status: "running",
    startedAt: new Date(Date.now() - 30 * 1000).toISOString(),
    logs: [],
  },
  {
    id: "exec-3",
    workflowId: "wf-3",
    workflowName: "Report Generator",
    status: "failed",
    startedAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
    completedAt: new Date(Date.now() - 59 * 60 * 1000).toISOString(),
    error: "Token limit exceeded",
    logs: [],
  },
]

interface WorkflowState {
  workflows: Workflow[]
  executions: WorkflowExecution[]
  mcpServers: McpServer[]
  apiKeys: ApiKey[]
  selectedWorkflow: Workflow | null
  selectedNode: WorkflowNode | null

  // Initialize state (load from backend)
  initialize: () => Promise<void>
  refreshConfig: () => Promise<void>
  
  // MCP actions
  getMcpServers: () => McpServer[]
  getMcpTools: () => McpTool[]

  // API Key actions
  getApiKeys: () => ApiKey[]
  addApiKey: (key: ApiKey) => void
  updateApiKey: (id: string, updates: Partial<ApiKey>) => void
  deleteApiKey: (id: string) => void
  getApiKeyById: (id: string) => ApiKey | undefined

  // Workflow actions
  setWorkflows: (workflows: Workflow[]) => void
  addWorkflow: (workflow: Workflow) => void
  updateWorkflow: (id: string, updates: Partial<Workflow>) => void
  deleteWorkflow: (id: string) => void
  selectWorkflow: (workflow: Workflow | null) => void
  
  // Node actions
  selectNode: (node: WorkflowNode | null) => void
  addNode: (workflowId: string, node: WorkflowNode) => void
  updateNode: (workflowId: string, nodeId: string, updates: Partial<WorkflowNode>) => void
  deleteNode: (workflowId: string, nodeId: string) => void
  
  // Edge actions
  addEdge: (workflowId: string, edge: WorkflowEdge) => void
  deleteEdge: (workflowId: string, edgeId: string) => void
  
  // Execution actions
  addExecution: (execution: WorkflowExecution) => void
  updateExecution: (id: string, updates: Partial<WorkflowExecution>) => void
}

export const useWorkflowStore = create<WorkflowState>((set, get) => ({
  workflows: demoWorkflows,
  executions: demoExecutions,
  mcpServers: [],
  apiKeys: [],
  selectedWorkflow: null,
  selectedNode: null,

  // Initialize and load config from backend
  initialize: async () => {
    const [apiKeys, mcpServers] = await Promise.all([
      loadApiKeys(),
      loadMcpServers(),
    ])
    set({ apiKeys, mcpServers })
  },

  refreshConfig: async () => {
    const [apiKeys, mcpServers] = await Promise.all([
      loadApiKeys(),
      loadMcpServers(),
    ])
    set({ apiKeys, mcpServers })
  },

  getMcpServers: () => get().mcpServers.filter((s) => s.status === "connected"),

  getMcpTools: () => {
    const servers = get().mcpServers.filter((s) => s.status === "connected")
    return servers.flatMap((server) =>
      server.tools.map((tool) => ({
        ...tool,
        name: `${server.name}/${tool.name}`,
      }))
    )
  },

  getApiKeys: () => get().apiKeys,

  addApiKey: (key) =>
    set((state) => ({ apiKeys: [...state.apiKeys, key] })),

  updateApiKey: (id, updates) =>
    set((state) => ({
      apiKeys: state.apiKeys.map((k) => (k.id === id ? { ...k, ...updates } : k)),
    })),

  deleteApiKey: (id) =>
    set((state) => ({ apiKeys: state.apiKeys.filter((k) => k.id !== id) })),

  getApiKeyById: (id) => get().apiKeys.find((k) => k.id === id),

  setWorkflows: (workflows) => set({ workflows }),
  
  addWorkflow: (workflow) =>
    set((state) => ({ workflows: [...state.workflows, workflow] })),
    
  updateWorkflow: (id, updates) =>
    set((state) => ({
      workflows: state.workflows.map((wf) =>
        wf.id === id ? { ...wf, ...updates, updatedAt: new Date().toISOString() } : wf
      ),
      selectedWorkflow:
        state.selectedWorkflow?.id === id
          ? { ...state.selectedWorkflow, ...updates }
          : state.selectedWorkflow,
    })),
    
  deleteWorkflow: (id) =>
    set((state) => ({
      workflows: state.workflows.filter((wf) => wf.id !== id),
      selectedWorkflow: state.selectedWorkflow?.id === id ? null : state.selectedWorkflow,
    })),
    
  selectWorkflow: (workflow) => set({ selectedWorkflow: workflow, selectedNode: null }),
  
  selectNode: (node) => set({ selectedNode: node }),
  
  addNode: (workflowId, node) =>
    set((state) => ({
      workflows: state.workflows.map((wf) =>
        wf.id === workflowId
          ? { ...wf, nodes: [...wf.nodes, node], updatedAt: new Date().toISOString() }
          : wf
      ),
    })),
    
  updateNode: (workflowId, nodeId, updates) =>
    set((state) => ({
      workflows: state.workflows.map((wf) =>
        wf.id === workflowId
          ? {
              ...wf,
              nodes: wf.nodes.map((n) => (n.id === nodeId ? { ...n, ...updates } : n)),
              updatedAt: new Date().toISOString(),
            }
          : wf
      ),
      selectedNode:
        state.selectedNode?.id === nodeId
          ? { ...state.selectedNode, ...updates }
          : state.selectedNode,
    })),
    
  deleteNode: (workflowId, nodeId) =>
    set((state) => ({
      workflows: state.workflows.map((wf) =>
        wf.id === workflowId
          ? {
              ...wf,
              nodes: wf.nodes.filter((n) => n.id !== nodeId),
              edges: wf.edges.filter((e) => e.source !== nodeId && e.target !== nodeId),
              updatedAt: new Date().toISOString(),
            }
          : wf
      ),
      selectedNode: state.selectedNode?.id === nodeId ? null : state.selectedNode,
    })),
    
  addEdge: (workflowId, edge) =>
    set((state) => ({
      workflows: state.workflows.map((wf) =>
        wf.id === workflowId
          ? { ...wf, edges: [...wf.edges, edge], updatedAt: new Date().toISOString() }
          : wf
      ),
    })),
    
  deleteEdge: (workflowId, edgeId) =>
    set((state) => ({
      workflows: state.workflows.map((wf) =>
        wf.id === workflowId
          ? {
              ...wf,
              edges: wf.edges.filter((e) => e.id !== edgeId),
              updatedAt: new Date().toISOString(),
            }
          : wf
      ),
    })),
    
  addExecution: (execution) =>
    set((state) => ({ executions: [execution, ...state.executions] })),
    
  updateExecution: (id, updates) =>
    set((state) => ({
      executions: state.executions.map((exec) =>
        exec.id === id ? { ...exec, ...updates } : exec
      ),
    })),
}))
