import type { ApiKey } from "@/lib/workflow/types"

const API_BASE = "/api/config"

export interface CreateApiKeyRequest {
  provider: string
  name?: string
  apiKey: string
  baseUrl?: string
  modelId?: string
}

export interface UpdateApiKeyRequest {
  name?: string
  apiKey?: string
  baseUrl?: string
  modelId?: string
  status?: string
}

export interface ApiKeyResponse {
  id: string
  provider: string
  name: string
  apiKey: string
  baseUrl?: string
  modelId?: string
  status: string
  lastUsedAt?: string
  createdAt: string
  updatedAt: string
}

/**
 * API Keys API
 */
export const apiKeysApi = {
  /**
   * 获取所有 API Keys
   */
  async getAll(): Promise<ApiKey[]> {
    const res = await fetch(`${API_BASE}/api-keys`)
    if (!res.ok) throw new Error("Failed to fetch API keys")
    const data: ApiKeyResponse[] = await res.json()
    return data.map(toApiKey)
  },

  /**
   * 获取单个 API Key
   */
  async get(id: string): Promise<ApiKey | null> {
    const res = await fetch(`${API_BASE}/api-keys/${id}`)
    if (!res.ok) return null
    const data: ApiKeyResponse = await res.json()
    return toApiKey(data)
  },

  /**
   * 创建 API Key
   */
  async create(request: CreateApiKeyRequest): Promise<ApiKey> {
    const res = await fetch(`${API_BASE}/api-keys`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request),
    })
    if (!res.ok) throw new Error("Failed to create API key")
    const data: ApiKeyResponse = await res.json()
    return toApiKey(data)
  },

  /**
   * 更新 API Key
   */
  async update(id: string, request: UpdateApiKeyRequest): Promise<ApiKey> {
    const res = await fetch(`${API_BASE}/api-keys/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request),
    })
    if (!res.ok) throw new Error("Failed to update API key")
    const data: ApiKeyResponse = await res.json()
    return toApiKey(data)
  },

  /**
   * 删除 API Key
   */
  async delete(id: string): Promise<void> {
    const res = await fetch(`${API_BASE}/api-keys/${id}`, {
      method: "DELETE",
    })
    if (!res.ok) throw new Error("Failed to delete API key")
  },
}

function toApiKey(data: ApiKeyResponse): ApiKey {
  return {
    id: data.id,
    provider: data.provider,
    name: data.name,
    key: data.apiKey,
    baseUrl: data.baseUrl,
    modelId: data.modelId,
    status: data.status as "valid" | "invalid" | "unknown",
    lastUsed: data.lastUsedAt,
  }
}
