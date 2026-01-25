"use client"

import React from "react"

import Link from "next/link"
import type { Workflow } from "@/lib/workflow/types"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { 
  Play, 
  Pencil, 
  MoreHorizontal, 
  Copy, 
  Trash2,
  Bot,
  Code,
  GitBranch,
  Globe,
  FileInput,
  FileOutput,
  Wrench
} from "lucide-react"
import { formatDistanceToNow } from "@/lib/utils/date"

interface WorkflowCardProps {
  workflow: Workflow
  onRun?: () => void
  onDelete?: () => void
  onDuplicate?: () => void
}

const nodeTypeIcons: Record<string, React.ElementType> = {
  input: FileInput,
  output: FileOutput,
  agent: Bot,
  code: Code,
  condition: GitBranch,
  http: Globe,
  tool: Wrench,
}

export function WorkflowCard({ workflow, onRun, onDelete, onDuplicate }: WorkflowCardProps) {
  const nodeTypes = [...new Set(workflow.nodes.map((n) => n.type))]

  return (
    <Card className="group relative overflow-hidden transition-all hover:shadow-md hover:border-primary/20">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between gap-2">
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold text-base truncate">{workflow.name}</h3>
            {workflow.description && (
              <p className="text-sm text-muted-foreground line-clamp-2 mt-1">
                {workflow.description}
              </p>
            )}
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity"
              >
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={onDuplicate}>
                <Copy className="mr-2 h-4 w-4" />
                Duplicate
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={onDelete} className="text-destructive">
                <Trash2 className="mr-2 h-4 w-4" />
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </CardHeader>
      
      <CardContent className="pb-3">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs text-muted-foreground">{workflow.nodes.length} nodes</span>
          <span className="text-muted-foreground">·</span>
          <div className="flex items-center gap-1">
            {nodeTypes.slice(0, 4).map((type) => {
              const Icon = nodeTypeIcons[type] || Bot
              return (
                <div
                  key={type}
                  className="flex items-center justify-center h-5 w-5 rounded bg-muted"
                  title={type}
                >
                  <Icon className="h-3 w-3 text-muted-foreground" />
                </div>
              )
            })}
            {nodeTypes.length > 4 && (
              <span className="text-xs text-muted-foreground">+{nodeTypes.length - 4}</span>
            )}
          </div>
        </div>
        
        <div className="flex items-center gap-2 mt-3">
          <Badge variant={workflow.status === "published" ? "default" : "secondary"} className="text-xs">
            {workflow.status === "published" ? "Published" : "Draft"}
          </Badge>
          <span className="text-xs text-muted-foreground">
            {formatDistanceToNow(workflow.updatedAt)}
          </span>
        </div>
      </CardContent>
      
      <CardFooter className="pt-0 gap-2">
        <Button size="sm" variant="outline" className="flex-1 bg-transparent" onClick={onRun}>
          <Play className="mr-1.5 h-3.5 w-3.5" />
          Run
        </Button>
        <Button size="sm" className="flex-1" asChild>
          <Link href={`/workflow/${workflow.id}`}>
            <Pencil className="mr-1.5 h-3.5 w-3.5" />
            Edit
          </Link>
        </Button>
      </CardFooter>
    </Card>
  )
}
