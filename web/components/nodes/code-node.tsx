"use client"

import { memo } from "react"
import type { NodeProps } from "@xyflow/react"
import { BaseNode, type BaseNodeData } from "./base-node"
import { Code } from "lucide-react"

interface CodeNodeData extends BaseNodeData {
  language?: string
  code?: string
}

export const CodeNode = memo(function CodeNode(props: NodeProps) {
  const data = props.data as CodeNodeData

  return (
    <BaseNode
      {...props}
      data={data}
      icon={Code}
      iconClassName="bg-amber-100 text-amber-600"
    >
      <div className="space-y-1">
        <div className="flex items-center gap-1.5">
          <span>Language:</span>
          <span className="text-foreground font-medium">{data.language || "JavaScript"}</span>
        </div>
        {data.code && (
          <div className="font-mono text-[10px] bg-muted px-1.5 py-1 rounded truncate max-w-[200px]">
            {data.code.slice(0, 50)}...
          </div>
        )}
      </div>
    </BaseNode>
  )
})
