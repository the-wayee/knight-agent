/**
 * API 客户端配置
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

export class ApiError extends Error {
  constructor(
    public message: string,
    public status: number,
    public data?: unknown
  ) {
    super(message)
    this.name = "ApiError"
  }
}

export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: string
  code?: string
}

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

/**
 * 基础 API 客户端
 */
export class ApiClient {
  private baseUrl: string

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl
  }

  private async request<T>(
    path: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseUrl}${path}`
    const token = this.getToken()

    const headers: HeadersInit = {
      "Content-Type": "application/json",
      ...options.headers,
    }

    if (token) {
      headers["Authorization"] = `Bearer ${token}`
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers,
      })

      const data = await response.json()

      if (!response.ok) {
        throw new ApiError(
          data.error || data.message || "Request failed",
          response.status,
          data
        )
      }

      return data
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      throw new ApiError(
        error instanceof Error ? error.message : "Network error",
        0
      )
    }
  }

  async get<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: "GET" })
  }

  async post<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, {
      method: "POST",
      body: body ? JSON.stringify(body) : undefined,
    })
  }

  async put<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, {
      method: "PUT",
      body: body ? JSON.stringify(body) : undefined,
    })
  }

  async delete<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: "DELETE" })
  }

  private getToken(): string | null {
    if (typeof window === "undefined") return null
    return localStorage.getItem("auth_token")
  }

  public setToken(token: string): void {
    if (typeof window === "undefined") return
    localStorage.setItem("auth_token", token)
  }

  public clearToken(): void {
    if (typeof window === "undefined") return
    localStorage.removeItem("auth_token")
  }
}

export const apiClient = new ApiClient()
