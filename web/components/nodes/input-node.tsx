"use client"

import { memo } from "react"
import type { NodeProps } from "@xyflow/react"
import { BaseNode, type BaseNodeData } from "./base-node"
import { FileInput } from "lucide-react"

interface InputNodeData extends BaseNodeData {
  fields?: Array<{ name: string; type: string; required: boolean }>
}

export const InputNode = memo(function InputNode(props: NodeProps) {
  const data = props.data as InputNodeData
  const fields = data.fields || []

  return (
    <BaseNode
      {...props}
      data={data}
      icon={FileInput}
      iconClassName="bg-emerald-100 text-emerald-600"
      showInput={false}
    >
      {fields.length > 0 ? (
        <div className="space-y-1">
          {fields.slice(0, 3).map((field) => (
            <div key={field.name} className="flex items-center gap-1.5">
              <span className="text-foreground">{field.name}</span>
              <span className="text-muted-foreground">({field.type})</span>
              {field.required && <span className="text-destructive">*</span>}
            </div>
          ))}
          {fields.length > 3 && (
            <span className="text-muted-foreground">+{fields.length - 3} more</span>
          )}
        </div>
      ) : (
        <span>Define input fields</span>
      )}
    </BaseNode>
  )
})
