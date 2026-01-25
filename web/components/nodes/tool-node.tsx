"use client"

import { memo } from "react"
import type { NodeProps } from "@xyflow/react"
import { BaseNode, type BaseNodeData } from "./base-node"
import { Wrench } from "lucide-react"

interface ToolNodeData extends BaseNodeData {
  server?: string
  tool?: string
}

export const ToolNode = memo(function ToolNode(props: NodeProps) {
  const data = props.data as ToolNodeData

  return (
    <BaseNode
      {...props}
      data={data}
      icon={Wrench}
      iconClassName="bg-orange-100 text-orange-600"
    >
      <div className="space-y-1">
        {data.server && (
          <div className="flex items-center gap-1.5">
            <span>Server:</span>
            <span className="text-foreground font-medium">{data.server}</span>
          </div>
        )}
        {data.tool && (
          <div className="flex items-center gap-1.5">
            <span>Tool:</span>
            <span className="text-foreground">{data.tool}</span>
          </div>
        )}
        {!data.server && !data.tool && (
          <span>Select MCP tool</span>
        )}
      </div>
    </BaseNode>
  )
})
