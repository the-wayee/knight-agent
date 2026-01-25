"use client"

import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
  TooltipProvider,
} from "@/components/ui/tooltip"
import type { NodeType } from "@/lib/workflow/types"
import {
  FileInput,
  FileOutput,
  Bot,
  Code,
  GitBranch,
  Globe,
  Wrench,
} from "lucide-react"

interface NodePaletteProps {
  onAddNode: (type: NodeType) => void
}

const nodeCategories = [
  {
    name: "Basic",
    nodes: [
      { type: "input" as NodeType, label: "Input", icon: FileInput, description: "Define workflow input parameters" },
      { type: "output" as NodeType, label: "Output", icon: FileOutput, description: "Define workflow output format" },
    ],
  },
  {
    name: "AI",
    nodes: [
      { type: "agent" as NodeType, label: "Agent", icon: Bot, description: "AI agent with LLM capabilities" },
    ],
  },
  {
    name: "Logic",
    nodes: [
      { type: "code" as NodeType, label: "Code", icon: Code, description: "Execute custom code" },
      { type: "condition" as NodeType, label: "Condition", icon: GitBranch, description: "Branch based on conditions" },
    ],
  },
  {
    name: "Integration",
    nodes: [
      { type: "http" as NodeType, label: "HTTP", icon: Globe, description: "Make HTTP requests" },
      { type: "tool" as NodeType, label: "Tool", icon: Wrench, description: "Use MCP tools" },
    ],
  },
]

export function NodePalette({ onAddNode }: NodePaletteProps) {
  return (
    <TooltipProvider>
      <div className="w-52 border-r border-border bg-card flex flex-col">
        <div className="p-3 border-b border-border">
          <h3 className="font-semibold text-sm">Nodes</h3>
          <p className="text-xs text-muted-foreground mt-0.5">
            Drag or click to add
          </p>
        </div>
        
        <ScrollArea className="flex-1">
          <div className="p-2">
            {nodeCategories.map((category) => (
              <div key={category.name} className="mb-4">
                <h4 className="text-xs font-medium text-muted-foreground px-2 mb-2">
                  {category.name}
                </h4>
                <div className="space-y-1">
                  {category.nodes.map((node) => (
                    <Tooltip key={node.type}>
                      <TooltipTrigger asChild>
                        <Button
                          variant="ghost"
                          className="w-full justify-start h-auto py-2 px-2"
                          onClick={() => onAddNode(node.type)}
                        >
                          <div className="flex h-8 w-8 items-center justify-center rounded-md bg-muted mr-2 flex-shrink-0">
                            <node.icon className="h-4 w-4 text-muted-foreground" />
                          </div>
                          <span className="text-sm">{node.label}</span>
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent side="right" className="max-w-[200px]">
                        <p className="font-medium">{node.label}</p>
                        <p className="text-xs text-muted-foreground">{node.description}</p>
                      </TooltipContent>
                    </Tooltip>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </ScrollArea>
      </div>
    </TooltipProvider>
  )
}
