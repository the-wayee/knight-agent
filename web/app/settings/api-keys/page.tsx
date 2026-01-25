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
  ArrowLeft,
  Key,
  Eye,
  EyeOff,
  CheckCircle2,
  AlertCircle,
  Pencil,
  Trash2,
  Loader2
} from "lucide-react"
import { apiKeysApi, type CreateApiKeyRequest } from "@/lib/api/config/api-keys"
import type { ApiKey } from "@/lib/workflow/types"

const providers = [
  { value: "openai", label: "OpenAI", baseUrl: "https://api.openai.com/v1" },
  { value: "anthropic", label: "Anthropic", baseUrl: "https://api.anthropic.com/v1" },
  { value: "deepseek", label: "DeepSeek", baseUrl: "https://api.deepseek.com/v1" },
  { value: "groq", label: "Groq", baseUrl: "https://api.groq.com/openai/v1" },
  { value: "azure", label: "Azure OpenAI", baseUrl: "" },
]

export default function ApiKeysPage() {
  const [keys, setKeys] = useState<ApiKey[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [showKey, setShowKey] = useState<string | null>(null)
  const [editingKeyId, setEditingKeyId] = useState<string | null>(null)
  const [newKey, setNewKey] = useState({ provider: "", name: "", key: "", baseUrl: "" })
  const [error, setError] = useState<string | null>(null)

  // 加载 API Keys
  useEffect(() => {
    loadKeys()
  }, [])

  const loadKeys = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await apiKeysApi.getAll()
      setKeys(data)
    } catch (err) {
      setError("Failed to load API keys")
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleSaveKey = async () => {
    if (!newKey.provider || (!newKey.key && !editingKeyId)) {
      setError("Provider and API Key are required")
      return
    }

    setSaving(true)
    setError(null)

    try {
      if (editingKeyId) {
        // Update existing key
        await apiKeysApi.update(editingKeyId, {
          name: newKey.name || undefined,
          apiKey: newKey.key || undefined,
          baseUrl: newKey.baseUrl || undefined,
        })
      } else {
        // Create new key
        const request: CreateApiKeyRequest = {
          provider: newKey.provider,
          name: newKey.name || undefined,
          apiKey: newKey.key,
          baseUrl: newKey.baseUrl || undefined,
        }
        await apiKeysApi.create(request)
      }

      setNewKey({ provider: "", name: "", key: "", baseUrl: "" })
      setEditingKeyId(null)
      setDialogOpen(false)
      await loadKeys()
    } catch (err) {
      setError("Failed to save API key")
      console.error(err)
    } finally {
      setSaving(false)
    }
  }

  const handleEditKey = (key: ApiKey) => {
    setEditingKeyId(key.id)
    setNewKey({
      provider: key.provider,
      name: key.name,
      key: "", // Don't show existing key for security, require new one only if changing
      baseUrl: key.baseUrl || "",
    })
    setDialogOpen(true)
  }

  const handleDeleteKey = async (id: string) => {
    if (!confirm("Are you sure you want to delete this API key?")) {
      return
    }

    setError(null)
    try {
      await apiKeysApi.delete(id)
      await loadKeys()
    } catch (err) {
      setError("Failed to delete API key")
      console.error(err)
    }
  }

  const handleProviderChange = (value: string) => {
    const providerConfig = providers.find((p) => p.value === value)
    setNewKey({
      ...newKey,
      provider: value,
      baseUrl: providerConfig?.baseUrl || "",
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
            <h1 className="text-2xl font-bold tracking-tight">API Keys</h1>
            <p className="text-muted-foreground mt-1">
              Manage API keys for different LLM providers
            </p>
          </div>
          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button onClick={() => {
                setEditingKeyId(null)
                setNewKey({ provider: "", name: "", key: "", baseUrl: "" })
              }}>
                <Key className="h-4 w-4 mr-2" />
                Add API Key
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>{editingKeyId ? "Edit API Key" : "Add API Key"}</DialogTitle>
                <DialogDescription>
                  {editingKeyId ? "Update existing API key configuration." : "Add an API key for an LLM provider to use in your workflows."}
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-4">
                {error && (
                  <div className="bg-destructive/10 text-destructive text-sm p-2 rounded">
                    {error}
                  </div>
                )}
                <div className="space-y-2">
                  <Label htmlFor="provider">Provider</Label>
                  <select
                    id="provider"
                    className="w-full h-10 px-3 rounded-md border border-input bg-background"
                    value={newKey.provider}
                    onChange={(e) => handleProviderChange(e.target.value)}
                    disabled={!!editingKeyId}
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
                    placeholder={editingKeyId ? "Leave empty to keep current key" : "sk-..."}
                    value={newKey.key}
                    onChange={(e) => setNewKey({ ...newKey, key: e.target.value })}
                  />
                  <p className="text-xs text-muted-foreground">
                    Your API key is encrypted and stored securely
                  </p>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="base-url">Base URL (optional)</Label>
                  <Input
                    id="base-url"
                    placeholder="https://api.openai.com/v1"
                    value={newKey.baseUrl}
                    onChange={(e) => setNewKey({ ...newKey, baseUrl: e.target.value })}
                  />
                  <p className="text-xs text-muted-foreground">
                    Custom API endpoint. Leave empty to use provider default.
                  </p>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setDialogOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleSaveKey} disabled={saving}>
                  {saving && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                  {editingKeyId ? "Update Key" : "Add Key"}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>

        {/* Keys List */}
        {loading ? (
          <div className="flex justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : (
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
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8"
                          onClick={() => handleEditKey(apiKey)}
                        >
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
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <code className="text-sm bg-muted px-2 py-1 rounded font-mono">
                            {apiKey.key}
                          </code>
                        </div>
                        {apiKey.lastUsed && (
                          <span className="text-xs text-muted-foreground">
                            Last used: {apiKey.lastUsed}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-4 text-xs text-muted-foreground">
                        {apiKey.baseUrl && (
                          <div className="flex items-center gap-1">
                            <span className="font-medium">Base URL:</span>
                            <span className="font-mono">{apiKey.baseUrl}</span>
                          </div>
                        )}

                      </div>
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
        )}
      </main>
    </div>
  )
}
