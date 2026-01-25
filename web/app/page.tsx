"use client"

import Link from "next/link"
import { Header } from "@/components/layout/header"
import { WorkflowCard } from "@/components/workflow/workflow-card"
import { useWorkflowStore } from "@/lib/workflow/store"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { 
  Plus, 
  ArrowRight, 
  CheckCircle2, 
  Loader2, 
  XCircle,
  Zap,
  Workflow,
  LayoutTemplate
} from "lucide-react"
import { formatDistanceToNow } from "@/lib/utils/date"

export default function DashboardPage() {
  const { workflows, executions } = useWorkflowStore()
  
  const recentWorkflows = workflows.slice(0, 3)
  const recentExecutions = executions.slice(0, 5)

  return (
    <div className="min-h-screen bg-background">
      <Header />
      
      <main className="container mx-auto px-4 py-8 max-w-7xl">
        {/* Welcome Section */}
        <Card className="mb-8 bg-gradient-to-r from-primary/5 via-primary/3 to-background border-primary/10">
          <CardContent className="p-6 md:p-8">
            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
              <div className="space-y-2">
                <h1 className="text-2xl md:text-3xl font-bold tracking-tight">
                  Welcome to KnightAgent
                </h1>
                <p className="text-muted-foreground max-w-lg">
                  Create AI-powered workflows without coding. Drag and drop nodes to build intelligent agents.
                </p>
              </div>
              <div className="flex flex-wrap gap-3">
                <Button asChild>
                  <Link href="/workflow/new">
                    <Plus className="mr-2 h-4 w-4" />
                    Create Workflow
                  </Link>
                </Button>
                <Button variant="outline" asChild>
                  <Link href="/templates">
                    <LayoutTemplate className="mr-2 h-4 w-4" />
                    Browse Templates
                  </Link>
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Stats Cards */}
        <div className="grid gap-4 md:grid-cols-3 mb-8">
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <Workflow className="h-6 w-6 text-primary" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Total Workflows</p>
                  <p className="text-2xl font-bold">{workflows.length}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-success/10">
                  <CheckCircle2 className="h-6 w-6 text-success" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Successful Runs</p>
                  <p className="text-2xl font-bold">
                    {executions.filter((e) => e.status === "completed").length}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <Zap className="h-6 w-6 text-primary" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Running Now</p>
                  <p className="text-2xl font-bold">
                    {executions.filter((e) => e.status === "running").length}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Workflows Section */}
        <section className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">Your Workflows</h2>
            <Button variant="ghost" size="sm" asChild>
              <Link href="/workflow" className="gap-1">
                View All
                <ArrowRight className="h-4 w-4" />
              </Link>
            </Button>
          </div>
          
          {recentWorkflows.length > 0 ? (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {recentWorkflows.map((workflow) => (
                <WorkflowCard 
                  key={workflow.id} 
                  workflow={workflow}
                  onRun={() => console.log("Run workflow:", workflow.id)}
                  onDelete={() => console.log("Delete workflow:", workflow.id)}
                  onDuplicate={() => console.log("Duplicate workflow:", workflow.id)}
                />
              ))}
            </div>
          ) : (
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-12 text-center">
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted mb-4">
                  <Workflow className="h-8 w-8 text-muted-foreground" />
                </div>
                <h3 className="font-semibold mb-1">No workflows yet</h3>
                <p className="text-sm text-muted-foreground mb-4">
                  Create your first workflow to get started
                </p>
                <Button asChild>
                  <Link href="/workflow/new">
                    <Plus className="mr-2 h-4 w-4" />
                    Create Workflow
                  </Link>
                </Button>
              </CardContent>
            </Card>
          )}
        </section>

        {/* Recent Executions */}
        <section>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">Recent Executions</h2>
            <Button variant="ghost" size="sm" asChild>
              <Link href="/executions" className="gap-1">
                View All
                <ArrowRight className="h-4 w-4" />
              </Link>
            </Button>
          </div>
          
          <Card>
            <CardContent className="p-0">
              {recentExecutions.length > 0 ? (
                <div className="divide-y divide-border">
                  {recentExecutions.map((execution) => (
                    <div
                      key={execution.id}
                      className="flex items-center gap-4 px-4 py-3 hover:bg-muted/50 transition-colors"
                    >
                      <div className="flex-shrink-0">
                        {execution.status === "completed" && (
                          <CheckCircle2 className="h-5 w-5 text-success" />
                        )}
                        {execution.status === "running" && (
                          <Loader2 className="h-5 w-5 text-primary animate-spin" />
                        )}
                        {execution.status === "failed" && (
                          <XCircle className="h-5 w-5 text-destructive" />
                        )}
                        {execution.status === "pending" && (
                          <div className="h-5 w-5 rounded-full border-2 border-muted-foreground" />
                        )}
                      </div>
                      
                      <div className="flex-1 min-w-0">
                        <p className="font-medium truncate">{execution.workflowName}</p>
                        <p className="text-sm text-muted-foreground">
                          {formatDistanceToNow(execution.startedAt)}
                        </p>
                      </div>
                      
                      <Badge
                        variant={
                          execution.status === "completed"
                            ? "default"
                            : execution.status === "running"
                              ? "secondary"
                              : execution.status === "failed"
                                ? "destructive"
                                : "outline"
                        }
                      >
                        {execution.status === "running" ? "Running..." : execution.status}
                      </Badge>
                      
                      {execution.status === "failed" && (
                        <Button variant="ghost" size="sm">
                          View Error
                        </Button>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center py-12 text-center">
                  <p className="text-sm text-muted-foreground">No executions yet</p>
                </div>
              )}
            </CardContent>
          </Card>
        </section>
      </main>
    </div>
  )
}
