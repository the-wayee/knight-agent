"use client"

import { memo } from "react"
import { Handle, Position, type NodeProps } from "@xyflow/react"
import { cn } from "@/lib/utils"
import { GitBranch } from "lucide-react"
import type { BaseNodeData } from "./base-node"

interface ConditionNodeData extends BaseNodeData {
  conditions?: Array<{ field: string; operator: string; value: string }>
}

export const ConditionNode = memo(function ConditionNode(props: NodeProps) {
  const { data, selected } = props
  const nodeData = data as ConditionNodeData

  return (
    <div
      className={cn(
        "min-w-[180px] max-w-[280px] rounded-lg border-2 bg-card shadow-sm transition-all border-border",
        selected && "ring-2 ring-primary ring-offset-2 ring-offset-background"
      )}
    >
      {/* Input Handle */}
      <Handle
        type="target"
        position={Position.Left}
        className="!w-3 !h-3 !bg-muted-foreground !border-2 !border-card"
      />

      {/* Header */}
      <div className="flex items-center gap-2 px-3 py-2 border-b border-border rounded-t-lg">
        <div className="flex h-6 w-6 items-center justify-center rounded bg-sky-100 text-sky-600">
          <GitBranch className="h-3.5 w-3.5" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium truncate">{nodeData.label}</p>
        </div>
      </div>

      {/* Content */}
      <div className="px-3 py-2 text-xs text-muted-foreground">
        {nodeData.conditions && nodeData.conditions.length > 0 ? (
          <div className="space-y-1">
            {nodeData.conditions.slice(0, 2).map((condition, i) => (
              <div key={i} className="truncate">
                {condition.field} {condition.operator} {condition.value}
              </div>
            ))}
          </div>
        ) : (
          <span>Configure conditions</span>
        )}
      </div>

      {/* Output Handles */}
      <div className="flex flex-col gap-2 absolute right-0 top-1/2 -translate-y-1/2 translate-x-1/2">
        <div className="relative">
          <Handle
            type="source"
            position={Position.Right}
            id="true"
            className="!w-3 !h-3 !bg-success !border-2 !border-card !relative !transform-none"
          />
          <span className="absolute left-4 top-1/2 -translate-y-1/2 text-[10px] text-success font-medium whitespace-nowrap">
            True
          </span>
        </div>
        <div className="relative">
          <Handle
            type="source"
            position={Position.Right}
            id="false"
            className="!w-3 !h-3 !bg-destructive !border-2 !border-card !relative !transform-none"
          />
          <span className="absolute left-4 top-1/2 -translate-y-1/2 text-[10px] text-destructive font-medium whitespace-nowrap">
            False
          </span>
        </div>
      </div>
    </div>
  )
})
