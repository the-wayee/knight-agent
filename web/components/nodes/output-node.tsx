"use client"

import { memo } from "react"
import type { NodeProps } from "@xyflow/react"
import { BaseNode, type BaseNodeData } from "./base-node"
import { FileOutput } from "lucide-react"

interface OutputNodeData extends BaseNodeData {
  format?: "json" | "text" | "template"
  mappings?: Array<{ field: string; source: string }>
}

export const OutputNode = memo(function OutputNode(props: NodeProps) {
  const data = props.data as OutputNodeData

  return (
    <BaseNode
      {...props}
      data={data}
      icon={FileOutput}
      iconClassName="bg-rose-100 text-rose-600"
      showOutput={false}
    >
      <div className="space-y-1">
        <div className="flex items-center gap-1.5">
          <span>Format:</span>
          <span className="text-foreground capitalize">{data.format || "JSON"}</span>
        </div>
        {data.mappings && data.mappings.length > 0 && (
          <div className="text-muted-foreground">
            {data.mappings.length} field{data.mappings.length > 1 ? "s" : ""} mapped
          </div>
        )}
      </div>
    </BaseNode>
  )
})
