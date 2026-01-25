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
  ArrowLeft,
  Key,
  Eye,
  EyeOff,
  CheckCircle2,
  AlertCircle,
  Pencil,
  Trash2
} from "lucide-react"

interface ApiKey {
  id: string
  provider: string
  name: string
  key: string
  status: "valid" | "invalid" | "unknown"
  lastUsed?: string
}

const demoKeys: ApiKey[] = [
  {
    id: "1",
    provider: "OpenAI",
    name: "Production Key",
    key: "sk-proj-xxxx...xxxx",
    status: "valid",
    lastUsed: "2 hours ago",
  },
  {
    id: "2",
    provider: "Anthropic",
    name: "Claude API",
    key: "sk-ant-xxxx...xxxx",
    status: "valid",
    lastUsed: "1 day ago",
  },
  {
    id: "3",
    provider: "DeepSeek",
    name: "DeepSeek Chat",
    key: "sk-xxxx...xxxx",
    status: "unknown",
  },
]

const providers = [
  { value: "openai", label: "OpenAI" },
  { value: "anthropic", label: "Anthropic" },
  { value: "deepseek", label: "DeepSeek" },
  { value: "groq", label: "Groq" },
  { value: "azure", label: "Azure OpenAI" },
]

export default function ApiKeysPage() {
  const [keys, setKeys] = useState<ApiKey[]>(demoKeys)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [showKey, setShowKey] = useState<string | null>(null)
  const [newKey, setNewKey] = useState({ provider: "", name: "", key: "" })

  const handleAddKey = () => {
    if (newKey.provider && newKey.key) {
      const maskedKey = `${newKey.key.slice(0, 7)}...${newKey.key.slice(-4)}`
      setKeys([
        ...keys,
        {
          id: Date.now().toString(),
          provider: providers.find((p) => p.value === newKey.provider)?.label || newKey.provider,
          name: newKey.name || "API Key",
          key: maskedKey,
          status: "unknown",
        },
      ])
      setNewKey({ provider: "", name: "", key: "" })
      setDialogOpen(false)
    }
  }

  const handleDeleteKey = (id: string) => {
    setKeys(keys.filter((k) => k.id !== id))
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
            <h1 className="text-2xl font-bold tracking-tight">API Keys</h1>
            <p className="text-muted-foreground mt-1">
              Manage API keys for different LLM providers
            </p>
          </div>
          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button>
                <Key className="h-4 w-4 mr-2" />
                Add API Key
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Add API Key</DialogTitle>
                <DialogDescription>
                  Add an API key for an LLM provider to use in your workflows.
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <Label htmlFor="provider">Provider</Label>
                  <select
                    id="provider"
                    className="w-full h-10 px-3 rounded-md border border-input bg-background"
                    value={newKey.provider}
                    onChange={(e) => setNewKey({ ...newKey, provider: e.target.value })}
                  >
                    <option value="">Select provider...</option>
                    {providers.map((p) => (
                      <option key={p.value} value={p.value}>
                        {p.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="key-name">Name (optional)</Label>
                  <Input
                    id="key-name"
                    placeholder="Production Key"
                    value={newKey.name}
                    onChange={(e) => setNewKey({ ...newKey, name: e.target.value })}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="api-key">API Key</Label>
                  <Input
                    id="api-key"
                    type="password"
                    placeholder="sk-..."
                    value={newKey.key}
                    onChange={(e) => setNewKey({ ...newKey, key: e.target.value })}
                  />
                  <p className="text-xs text-muted-foreground">
                    Your API key is encrypted and stored securely
                  </p>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setDialogOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleAddKey}>Add Key</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>

        {/* Keys List */}
        <div className="space-y-4">
          {keys.length > 0 ? (
            keys.map((apiKey) => (
              <Card key={apiKey.id}>
                <CardHeader className="pb-3">
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted">
                        <Key className="h-5 w-5 text-muted-foreground" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <CardTitle className="text-base">{apiKey.provider}</CardTitle>
                          <Badge
                            variant={
                              apiKey.status === "valid"
                                ? "default"
                                : apiKey.status === "invalid"
                                  ? "destructive"
                                  : "secondary"
                            }
                            className="gap-1"
                          >
                            {apiKey.status === "valid" && (
                              <CheckCircle2 className="h-3 w-3" />
                            )}
                            {apiKey.status === "invalid" && (
                              <AlertCircle className="h-3 w-3" />
                            )}
                            {apiKey.status}
                          </Badge>
                        </div>
                        <CardDescription>{apiKey.name}</CardDescription>
                      </div>
                    </div>
                    <div className="flex items-center gap-1">
                      <Button variant="ghost" size="icon" className="h-8 w-8">
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 text-destructive"
                        onClick={() => handleDeleteKey(apiKey.id)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <code className="text-sm bg-muted px-2 py-1 rounded font-mono">
                        {showKey === apiKey.id ? apiKey.key : apiKey.key}
                      </code>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-6 w-6"
                        onClick={() => setShowKey(showKey === apiKey.id ? null : apiKey.id)}
                      >
                        {showKey === apiKey.id ? (
                          <EyeOff className="h-3 w-3" />
                        ) : (
                          <Eye className="h-3 w-3" />
                        )}
                      </Button>
                    </div>
                    {apiKey.lastUsed && (
                      <span className="text-xs text-muted-foreground">
                        Last used: {apiKey.lastUsed}
                      </span>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))
          ) : (
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-12 text-center">
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted mb-4">
                  <Key className="h-8 w-8 text-muted-foreground" />
                </div>
                <h3 className="font-semibold mb-1">No API keys</h3>
                <p className="text-sm text-muted-foreground mb-4">
                  Add API keys for LLM providers to use in your workflows
                </p>
                <Button onClick={() => setDialogOpen(true)}>
                  <Key className="h-4 w-4 mr-2" />
                  Add API Key
                </Button>
              </CardContent>
            </Card>
          )}
        </div>
      </main>
    </div>
  )
}
