"use client"

import { memo } from "react"
import type { NodeProps } from "@xyflow/react"
import { BaseNode, type BaseNodeData } from "./base-node"
import { Bot } from "lucide-react"

interface AgentNodeData extends BaseNodeData {
  model?: string
  temperature?: number
  tools?: string[]
  strategy?: string
}

export const AgentNode = memo(function AgentNode(props: NodeProps) {
  const data = props.data as AgentNodeData

  return (
    <BaseNode
      {...props}
      data={data}
      icon={Bot}
      iconClassName="bg-violet-100 text-violet-600"
    >
      <div className="space-y-1">
        {data.model && (
          <div className="flex items-center gap-1.5">
            <span>Model:</span>
            <span className="text-foreground font-medium">{data.model}</span>
          </div>
        )}
        {data.temperature !== undefined && (
          <div className="flex items-center gap-1.5">
            <span>Temperature:</span>
            <span className="text-foreground">{data.temperature}</span>
          </div>
        )}
        {data.tools && data.tools.length > 0 && (
          <div className="text-muted-foreground">
            {data.tools.length} tool{data.tools.length > 1 ? "s" : ""} enabled
          </div>
        )}
      </div>
    </BaseNode>
  )
})
