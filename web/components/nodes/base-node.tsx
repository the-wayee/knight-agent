"use client"

import React from "react"

import { memo } from "react"
import { Handle, Position, type NodeProps } from "@xyflow/react"
import { cn } from "@/lib/utils"
import type { LucideIcon } from "lucide-react"

export interface BaseNodeData {
  label: string
  description?: string
  status?: "idle" | "running" | "success" | "error"
  [key: string]: unknown
}

interface BaseNodeProps extends NodeProps {
  data: BaseNodeData
  icon: LucideIcon
  iconClassName?: string
  showInput?: boolean
  showOutput?: boolean
  headerClassName?: string
  children?: React.ReactNode
}

export const BaseNode = memo(function BaseNode({
  data,
  icon: Icon,
  iconClassName,
  showInput = true,
  showOutput = true,
  headerClassName,
  selected,
  children,
}: BaseNodeProps) {
  const statusColors = {
    idle: "border-gray-200",
    running: "border-primary animate-pulse",
    success: "border-green-500",
    error: "border-red-500",
  }

  return (
    <div
      className={cn(
        "min-w-[180px] max-w-[280px] rounded-lg border-2 bg-card shadow-sm transition-all",
        statusColors[data.status || "idle"],
        selected && "!border-primary shadow-md"
      )}
    >
      {/* Input Handle */}
      {showInput && (
        <Handle
          type="target"
          position={Position.Left}
          className="!w-3 !h-3 !bg-muted-foreground !border-2 !border-card"
        />
      )}

      {/* Header */}
      <div
        className={cn(
          "flex items-center gap-2 px-3 py-2 border-b border-border rounded-t-lg",
          headerClassName
        )}
      >
        <div className={cn("flex h-6 w-6 items-center justify-center rounded", iconClassName)}>
          <Icon className="h-3.5 w-3.5" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium truncate">{data.label}</p>
        </div>
        {data.status === "running" && (
          <div className="w-2 h-2 rounded-full bg-primary animate-pulse" />
        )}
        {data.status === "success" && (
          <div className="w-2 h-2 rounded-full bg-success" />
        )}
        {data.status === "error" && (
          <div className="w-2 h-2 rounded-full bg-destructive" />
        )}
      </div>

      {/* Content */}
      {children && <div className="px-3 py-2 text-xs text-muted-foreground">{children}</div>}

      {/* Output Handle */}
      {showOutput && (
        <Handle
          type="source"
          position={Position.Right}
          className="!w-3 !h-3 !bg-primary !border-2 !border-card"
        />
      )}
    </div>
  )
})
