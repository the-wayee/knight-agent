import type { McpServer, McpTool } from "@/lib/workflow/types"

const API_BASE = "/api/config"

export interface CreateMcpServerRequest {
  name: string
  url: string
}

export interface UpdateMcpServerRequest {
  name?: string
  url?: string
  status?: string
}

export interface McpServerResponse {
  id: string
  name: string
  url: string
  status: string
  toolCount: number
  error?: string
  createdAt: string
  updatedAt: string
  tools?: McpToolResponse[]
}

export interface McpToolResponse {
  id: number
  name: string
  description: string
  inputSchema: string
}

/**
 * MCP Servers API
 */
export const mcpServersApi = {
  /**
   * 获取所有 MCP 服务器
   */
  async getAll(): Promise<McpServer[]> {
    const res = await fetch(`${API_BASE}/mcp-servers`)
    if (!res.ok) throw new Error("Failed to fetch MCP servers")
    const data: McpServerResponse[] = await res.json()
    return data.map(toMcpServer)
  },

  /**
   * 获取单个 MCP 服务器
   */
  async get(id: string): Promise<McpServer | null> {
    const res = await fetch(`${API_BASE}/mcp-servers/${id}`)
    if (!res.ok) return null
    const data: McpServerResponse = await res.json()
    return toMcpServer(data)
  },

  /**
   * 创建 MCP 服务器
   */
  async create(request: CreateMcpServerRequest): Promise<McpServer> {
    const res = await fetch(`${API_BASE}/mcp-servers`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request),
    })
    if (!res.ok) throw new Error("Failed to create MCP server")
    const data: McpServerResponse = await res.json()
    return toMcpServer(data)
  },

  /**
   * 更新 MCP 服务器
   */
  async update(id: string, request: UpdateMcpServerRequest): Promise<McpServer> {
    const res = await fetch(`${API_BASE}/mcp-servers/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request),
    })
    if (!res.ok) throw new Error("Failed to update MCP server")
    const data: McpServerResponse = await res.json()
    return toMcpServer(data)
  },

  /**
   * 删除 MCP 服务器
   */
  async delete(id: string): Promise<void> {
    const res = await fetch(`${API_BASE}/mcp-servers/${id}`, {
      method: "DELETE",
    })
    if (!res.ok) throw new Error("Failed to delete MCP server")
  },

  /**
   * 刷新 MCP 服务器连接（返回包含工具的服务器信息）
   */
  async refresh(id: string): Promise<McpServer> {
    const res = await fetch(`${API_BASE}/mcp-servers/${id}/refresh`, {
      method: "POST",
    })
    if (!res.ok) throw new Error("Failed to refresh MCP server")
    const data: McpServerResponse = await res.json()
    return toMcpServer(data)
  },

  /**
   * 获取服务器的工具列表
   */
  async getTools(id: string): Promise<McpTool[]> {
    const res = await fetch(`${API_BASE}/mcp-servers/${id}/tools`)
    if (!res.ok) throw new Error("Failed to fetch MCP tools")
    const data: McpToolResponse[] = await res.json()
    return data.map(toMcpTool)
  },
}

function toMcpServer(data: McpServerResponse): McpServer {
  return {
    id: data.id,
    name: data.name,
    url: data.url,
    status: data.status as "connected" | "disconnected" | "error",
    tools: data.tools?.map(toMcpTool) ?? [],
  }
}

function toMcpTool(data: McpToolResponse): McpTool {
  let parsedSchema = {}
  if (data.inputSchema) {
    try {
      parsedSchema = JSON.parse(data.inputSchema)
    } catch (e) {
      console.warn(`Failed to parse inputSchema for tool "${data.name}":`, e)
    }
  }
  return {
    name: data.name,
    description: data.description,
    inputSchema: parsedSchema,
  }
}
