"use client"

import { useState } from "react"
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
  Wrench
} from "lucide-react"

interface McpServer {
  id: string
  name: string
  url: string
  status: "connected" | "disconnected" | "error"
  toolCount: number
}

const demoServers: McpServer[] = [
  {
    id: "1",
    name: "Filesystem Server",
    url: "stdio:///usr/local/bin/mcp-filesystem",
    status: "connected",
    toolCount: 5,
  },
  {
    id: "2",
    name: "PostgreSQL Server",
    url: "stdio:///usr/local/bin/mcp-postgres",
    status: "connected",
    toolCount: 8,
  },
  {
    id: "3",
    name: "Web Search Server",
    url: "http://localhost:3001/mcp",
    status: "disconnected",
    toolCount: 0,
  },
]

export default function McpSettingsPage() {
  const [servers, setServers] = useState<McpServer[]>(demoServers)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [newServer, setNewServer] = useState({ name: "", url: "" })

  const handleAddServer = () => {
    if (newServer.name && newServer.url) {
      setServers([
        ...servers,
        {
          id: Date.now().toString(),
          name: newServer.name,
          url: newServer.url,
          status: "disconnected",
          toolCount: 0,
        },
      ])
      setNewServer({ name: "", url: "" })
      setDialogOpen(false)
    }
  }

  const handleDeleteServer = (id: string) => {
    setServers(servers.filter((s) => s.id !== id))
  }

  const handleRefresh = (id: string) => {
    setServers(
      servers.map((s) =>
        s.id === id ? { ...s, status: "connected" as const, toolCount: Math.floor(Math.random() * 10) + 1 } : s
      )
    )
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
                <Button onClick={handleAddServer}>Add Server</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>

        {/* Server List */}
        <div className="space-y-4">
          {servers.length > 0 ? (
            servers.map((server) => (
              <Card key={server.id}>
                <CardHeader className="pb-3">
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted">
                        <Server className="h-5 w-5 text-muted-foreground" />
                      </div>
                      <div>
                        <CardTitle className="text-base">{server.name}</CardTitle>
                        <CardDescription className="font-mono text-xs">
                          {server.url}
                        </CardDescription>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
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
                        {server.status === "disconnected" && (
                          <XCircle className="h-3 w-3" />
                        )}
                        {server.status === "error" && (
                          <XCircle className="h-3 w-3" />
                        )}
                        {server.status}
                      </Badge>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon" className="h-8 w-8">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => handleRefresh(server.id)}>
                            <RefreshCw className="h-4 w-4 mr-2" />
                            Refresh
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => handleDeleteServer(server.id)}
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
                <CardContent>
                  <div className="flex items-center gap-4 text-sm text-muted-foreground">
                    <div className="flex items-center gap-1">
                      <Wrench className="h-4 w-4" />
                      <span>{server.toolCount} tools available</span>
                    </div>
                  </div>
                </CardContent>
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
      </main>
    </div>
  )
}
