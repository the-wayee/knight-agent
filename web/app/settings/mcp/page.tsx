"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { Header } from "@/components/layout/header"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  ArrowLeft,
  Plus,
  Server,
  MoreHorizontal,
  RefreshCw,
  Trash2,
  CheckCircle2,
  XCircle,
  Loader2,
  Wrench,
  ChevronDown,
  ChevronRight,
} from "lucide-react"
import { mcpServersApi, type CreateMcpServerRequest } from "@/lib/api/config/mcp-servers"
import type { McpServer, McpTool } from "@/lib/workflow/types"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { cn } from "@/lib/utils"

export default function McpSettingsPage() {
  const [servers, setServers] = useState<McpServer[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [refreshing, setRefreshing] = useState<string | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [newServer, setNewServer] = useState({ name: "", url: "" })
  const [error, setError] = useState<string | null>(null)
  const [expandedServers, setExpandedServers] = useState<Set<string>>(new Set())

  // Load MCP servers
  useEffect(() => {
    loadServers()
  }, [])

  const loadServers = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await mcpServersApi.getAll()
      setServers(data)
    } catch (err) {
      setError("Failed to load MCP servers")
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleAddServer = async () => {
    if (!newServer.name || !newServer.url) {
      setError("Name and URL are required")
      return
    }

    setSaving(true)
    setError(null)

    try {
      const request: CreateMcpServerRequest = {
        name: newServer.name,
        url: newServer.url,
      }

      await mcpServersApi.create(request)
      setNewServer({ name: "", url: "" })
      setDialogOpen(false)
      await loadServers()
    } catch (err) {
      setError("Failed to create MCP server")
      console.error(err)
    } finally {
      setSaving(false)
    }
  }

  const handleDeleteServer = async (id: string) => {
    if (!confirm("Are you sure you want to delete this MCP server?")) {
      return
    }

    setError(null)
    try {
      await mcpServersApi.delete(id)
      await loadServers()
    } catch (err) {
      setError("Failed to delete MCP server")
      console.error(err)
    }
  }

  const handleRefresh = async (id: string) => {
    setRefreshing(id)
    setError(null)
    try {
      const refreshed = await mcpServersApi.refresh(id)
      // 更新服务器列表中的对应服务器
      setServers(prev => prev.map(s => s.id === id ? refreshed : s))
      // 展开该服务器以显示工具
      setExpandedServers(prev => new Set([...prev, id]))
    } catch (err) {
      setError("Failed to refresh MCP server: " + (err instanceof Error ? err.message : String(err)))
      console.error(err)
    } finally {
      setRefreshing(null)
    }
  }

  const toggleExpanded = (id: string) => {
    setExpandedServers(prev => {
      const newSet = new Set(prev)
      if (newSet.has(id)) {
        newSet.delete(id)
      } else {
        newSet.add(id)
      }
      return newSet
    })
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <main className="container mx-auto px-4 py-8 max-w-4xl">
        {/* Breadcrumb */}
        <div className="flex items-center gap-2 mb-6">
          <Button variant="ghost" size="sm" asChild>
            <Link href="/settings">
              <ArrowLeft className="h-4 w-4 mr-1" />
              Settings
            </Link>
          </Button>
        </div>

        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">MCP Servers</h1>
            <p className="text-muted-foreground mt-1">
              Connect to MCP servers to extend your workflow capabilities
            </p>
          </div>
          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="h-4 w-4 mr-2" />
                Add Server
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Add MCP Server</DialogTitle>
                <DialogDescription>
                  Connect to a new MCP server to access its tools in your workflows.
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-4">
                {error && (
                  <div className="bg-destructive/10 text-destructive text-sm p-2 rounded">
                    {error}
                  </div>
                )}
                <div className="space-y-2">
                  <Label htmlFor="server-name">Server Name</Label>
                  <Input
                    id="server-name"
                    placeholder="My MCP Server"
                    value={newServer.name}
                    onChange={(e) => setNewServer({ ...newServer, name: e.target.value })}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="server-url">Server URL</Label>
                  <Input
                    id="server-url"
                    placeholder="stdio:///path/to/server or http://..."
                    value={newServer.url}
                    onChange={(e) => setNewServer({ ...newServer, url: e.target.value })}
                  />
                  <p className="text-xs text-muted-foreground">
                    Supports stdio:// for local servers or http:// for remote servers
                  </p>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setDialogOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleAddServer} disabled={saving}>
                  {saving && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                  Add Server
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>

        {/* Server List */}
        {loading ? (
          <div className="flex justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <div className="space-y-4">
            {servers.length > 0 ? (
              servers.map((server) => (
                <Card key={server.id}>
                  <Collapsible
                    open={expandedServers.has(server.id)}
                    onOpenChange={() => toggleExpanded(server.id)}
                  >
                    <CollapsibleTrigger asChild>
                      <CardHeader className="pb-3 cursor-pointer">
                        <div className="flex items-start justify-between">
                          <div className="flex items-center gap-3 flex-1">
                            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted">
                              <Server className="h-5 w-5 text-muted-foreground" />
                            </div>
                            <div className="flex-1">
                              <CardTitle className="text-base">{server.name}</CardTitle>
                              <CardDescription className="font-mono text-xs">
                                {server.url}
                              </CardDescription>
                              <div className="flex items-center gap-2 mt-1">
                                <Badge
                                  variant={
                                    server.status === "connected"
                                      ? "default"
                                      : server.status === "error"
                                        ? "destructive"
                                        : "secondary"
                                  }
                                  className="gap-1"
                                >
                                  {server.status === "connected" && (
                                    <CheckCircle2 className="h-3 w-3" />
                                  )}
                                  {server.status}
                                </Badge>
                                <span className="text-xs text-muted-foreground">
                                  {server.tools.length} tools
                                </span>
                              </div>
                            </div>
                            <ChevronDown className={cn(
                              "h-4 w-4 transition-transform",
                              expandedServers.has(server.id) && "rotate-180"
                            )} />
                          </div>
                          <div className="flex items-center gap-2">
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                              onClick={(e) => {
                                e.stopPropagation()
                                handleRefresh(server.id)
                              }}
                              disabled={refreshing === server.id}
                            >
                              {refreshing === server.id ? (
                                <Loader2 className="h-4 w-4 animate-spin" />
                              ) : (
                                <RefreshCw className="h-4 w-4" />
                              )}
                            </Button>
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="h-8 w-8"
                                  onClick={(e) => e.stopPropagation()}
                                >
                                  <MoreHorizontal className="h-4 w-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end">
                                <DropdownMenuItem
                                  onClick={(e) => {
                                    e.stopPropagation()
                                    handleDeleteServer(server.id)
                                  }}
                                  className="text-destructive"
                                >
                                  <Trash2 className="h-4 w-4 mr-2" />
                                  Delete
                                </DropdownMenuItem>
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </div>
                        </div>
                      </CardHeader>
                    </CollapsibleTrigger>
                    <CollapsibleContent>
                      <CardContent className="pt-0">
                        {server.tools.length > 0 ? (
                          <div className="space-y-2">
                            {server.tools.map((tool) => (
                              <div
                                key={tool.name}
                                className="p-3 bg-muted/50 rounded-md border border-border"
                              >
                                <div className="flex items-center gap-2">
                                  <Wrench className="h-4 w-4 text-muted-foreground" />
                                  <span className="font-medium text-sm">{tool.name}</span>
                                </div>
                                <p className="text-xs text-muted-foreground mt-1">
                                  {tool.description}
                                </p>
                              </div>
                            ))}
                          </div>
                        ) : server.status === "connected" ? (
                          <div className="py-4 text-center text-sm text-muted-foreground">
                            No tools found. Try refreshing the connection.
                          </div>
                        ) : server.status === "disconnected" ? (
                          <div className="py-4 text-center text-sm text-muted-foreground">
                            Server disconnected. Click refresh to connect and discover tools.
                          </div>
                        ) : (
                          <div className="py-4 text-center text-sm text-destructive">
                            {server.status === "error" && "Connection error. Check the server URL and try again."}
                          </div>
                        )}
                      </CardContent>
                    </CollapsibleContent>
                  </Collapsible>
                </Card>
              ))
            ) : (
              <Card>
                <CardContent className="flex flex-col items-center justify-center py-12 text-center">
                  <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted mb-4">
                    <Server className="h-8 w-8 text-muted-foreground" />
                  </div>
                  <h3 className="font-semibold mb-1">No MCP servers</h3>
                  <p className="text-sm text-muted-foreground mb-4">
                    Add your first MCP server to extend workflow capabilities
                  </p>
                  <Button onClick={() => setDialogOpen(true)}>
                    <Plus className="h-4 w-4 mr-2" />
                    Add Server
                  </Button>
                </CardContent>
              </Card>
            )}
          </div>
        )}
      </main>
    </div>
  )
}
