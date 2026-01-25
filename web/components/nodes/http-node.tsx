"use client"

import { memo } from "react"
import type { NodeProps } from "@xyflow/react"
import { BaseNode, type BaseNodeData } from "./base-node"
import { Globe } from "lucide-react"
import { Badge } from "@/components/ui/badge"

interface HttpNodeData extends BaseNodeData {
  method?: "GET" | "POST" | "PUT" | "DELETE" | "PATCH"
  url?: string
}

export const HttpNode = memo(function HttpNode(props: NodeProps) {
  const data = props.data as HttpNodeData

  const methodColors: Record<string, string> = {
    GET: "bg-emerald-100 text-emerald-700",
    POST: "bg-blue-100 text-blue-700",
    PUT: "bg-amber-100 text-amber-700",
    DELETE: "bg-rose-100 text-rose-700",
    PATCH: "bg-violet-100 text-violet-700",
  }

  return (
    <BaseNode
      {...props}
      data={data}
      icon={Globe}
      iconClassName="bg-blue-100 text-blue-600"
    >
      <div className="space-y-1.5">
        <div className="flex items-center gap-1.5">
          <Badge variant="secondary" className={methodColors[data.method || "GET"]}>
            {data.method || "GET"}
          </Badge>
        </div>
        {data.url && (
          <div className="font-mono text-[10px] truncate max-w-[200px]">
            {data.url}
          </div>
        )}
      </div>
    </BaseNode>
  )
})
