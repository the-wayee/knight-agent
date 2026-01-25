"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { Header } from "@/components/layout/header"
import { WorkflowCard } from "@/components/workflow/workflow-card"
import { CreateWorkflowDialog } from "@/components/workflow/create-workflow-dialog"
import { useWorkflows } from "@/lib/hooks"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Plus,
  Search,
  Workflow,
  Grid3X3,
  List,
  Loader2,
} from "lucide-react"

export default function WorkflowListPage() {
  const router = useRouter()
  const { workflows, loading, error, createWorkflow, deleteWorkflow, fetchWorkflows } = useWorkflows()
  const [searchQuery, setSearchQuery] = useState("")
  const [statusFilter, setStatusFilter] = useState<string>("all")
  const [viewMode, setViewMode] = useState<"grid" | "list">("grid")
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [creating, setCreating] = useState(false)

  const filteredWorkflows = workflows.filter((wf) => {
    const matchesSearch = wf.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      wf.description?.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesStatus = statusFilter === "all" || wf.status === statusFilter
    return matchesSearch && matchesStatus
  })

  const handleCreateWorkflow = async (name: string, description: string) => {
    setCreating(true)
    try {
      await createWorkflow({
        name,
        description,
        nodes: [],
        edges: [],
      })
      setCreateDialogOpen(false)
    } catch (err) {
      console.error("Failed to create workflow:", err)
    } finally {
      setCreating(false)
    }
  }

  const handleDeleteWorkflow = async (id: string) => {
    try {
      await deleteWorkflow(id)
    } catch (err) {
      console.error("Failed to delete workflow:", err)
    }
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <main className="container mx-auto px-4 py-8 max-w-7xl">
        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">Workflows</h1>
            <p className="text-muted-foreground mt-1">
              Manage and run your AI workflows
            </p>
          </div>
          <Button onClick={() => setCreateDialogOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            New Workflow
          </Button>
        </div>

        {/* Filters */}
        <div className="flex flex-col sm:flex-row gap-3 mb-6">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search workflows..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9"
            />
          </div>

          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-[140px]">
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Status</SelectItem>
              <SelectItem value="draft">Draft</SelectItem>
              <SelectItem value="published">Published</SelectItem>
            </SelectContent>
          </Select>

          <div className="flex border border-input rounded-md">
            <Button
              variant={viewMode === "grid" ? "secondary" : "ghost"}
              size="icon"
              className="rounded-r-none"
              onClick={() => setViewMode("grid")}
            >
              <Grid3X3 className="h-4 w-4" />
            </Button>
            <Button
              variant={viewMode === "list" ? "secondary" : "ghost"}
              size="icon"
              className="rounded-l-none"
              onClick={() => setViewMode("list")}
            >
              <List className="h-4 w-4" />
            </Button>
          </div>
        </div>

        {/* Error State */}
        {error && (
          <div className="mb-6 p-4 bg-destructive/10 text-destructive rounded-lg">
            <p className="font-medium">Error loading workflows</p>
            <p className="text-sm mt-1">{error}</p>
            <Button
              variant="outline"
              size="sm"
              className="mt-3"
              onClick={fetchWorkflows}
            >
              Retry
            </Button>
          </div>
        )}

        {/* Loading State */}
        {loading && (
          <div className="flex items-center justify-center py-16">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {/* Workflow Grid/List */}
        {!loading && filteredWorkflows.length > 0 ? (
          <div className={
            viewMode === "grid"
              ? "grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
              : "space-y-3"
          }>
            {filteredWorkflows.map((workflow) => (
              <WorkflowCard
                key={workflow.id}
                workflow={workflow}
                onRun={() => router.push(`/workflow/${workflow.id}`)}
                onDelete={() => handleDeleteWorkflow(workflow.id)}
                onDuplicate={() => {
                  const duplicate = {
                    name: `${workflow.name} (Copy)`,
                    description: workflow.description,
                    nodes: workflow.nodes,
                    edges: workflow.edges,
                  }
                  createWorkflow(duplicate).catch(console.error)
                }}
              />
            ))}
          </div>
        ) : !loading && (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <div className="flex h-20 w-20 items-center justify-center rounded-full bg-muted mb-4">
              <Workflow className="h-10 w-10 text-muted-foreground" />
            </div>
            {searchQuery || statusFilter !== "all" ? (
              <>
                <h3 className="font-semibold mb-1">No workflows found</h3>
                <p className="text-sm text-muted-foreground mb-4">
                  Try adjusting your search or filters
                </p>
                <Button variant="outline" onClick={() => {
                  setSearchQuery("")
                  setStatusFilter("all")
                }}>
                  Clear Filters
                </Button>
              </>
            ) : (
              <>
                <h3 className="font-semibold mb-1">No workflows yet</h3>
                <p className="text-sm text-muted-foreground mb-4">
                  Create your first workflow to get started
                </p>
                <Button onClick={() => setCreateDialogOpen(true)}>
                  <Plus className="mr-2 h-4 w-4" />
                  Create Workflow
                </Button>
              </>
            )}
          </div>
        )}
      </main>

      <CreateWorkflowDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
        onCreate={handleCreateWorkflow}
        loading={creating}
      />
    </div>
  )
}
