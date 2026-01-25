"use client"

import { useState, useEffect } from "react"
import type { WorkflowNode } from "@/lib/workflow/types"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Slider } from "@/components/ui/slider"
import { Switch } from "@/components/ui/switch"
import { ScrollArea } from "@/components/ui/scroll-area"
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible"
import { X, ChevronDown, Sparkles, Variable, Plus, Trash2, Wrench, Server, Check, Key } from "lucide-react"
import { cn } from "@/lib/utils"
import { useWorkflowStore } from "@/lib/workflow/store"
import { Checkbox } from "@/components/ui/checkbox"
import { Badge } from "@/components/ui/badge"

interface ConfigPanelProps {
  node: WorkflowNode | null
  onClose: () => void
  onUpdate: (updates: Record<string, unknown>) => void
}

const models = [
  { value: "gpt-4o", label: "GPT-4o" },
  { value: "gpt-4o-mini", label: "GPT-4o Mini" },
  { value: "claude-3-5-sonnet", label: "Claude 3.5 Sonnet" },
  { value: "claude-3-5-haiku", label: "Claude 3.5 Haiku" },
  { value: "deepseek-chat", label: "DeepSeek Chat" },
]

export function ConfigPanel({ node, onClose, onUpdate }: ConfigPanelProps) {
  const [basicOpen, setBasicOpen] = useState(true)
  const [advancedOpen, setAdvancedOpen] = useState(false)
  const { initialize } = useWorkflowStore()

  // 初始化配置数据（API Keys 和 MCP Servers）
  useEffect(() => {
    initialize()
  }, [initialize])

  if (!node) {
    return (
      <div className="w-80 border-l border-border bg-card flex flex-col">
        <div className="p-4 border-b border-border flex items-center justify-between">
          <h3 className="font-semibold">Node Configuration</h3>
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>
        <div className="flex-1 flex items-center justify-center p-4">
          <p className="text-sm text-muted-foreground text-center">
            Select a node to configure its settings
          </p>
        </div>
      </div>
    )
  }

  const nodeData = node.data as Record<string, unknown>

  return (
    <div className="w-80 border-l border-border bg-card flex flex-col">
      {/* Header */}
      <div className="p-4 border-b border-border flex items-center justify-between">
        <div className="flex items-center gap-2">
          <NodeTypeIcon type={node.type} />
          <h3 className="font-semibold capitalize">{node.type} Node</h3>
        </div>
        <Button variant="ghost" size="icon" className="h-8 w-8" onClick={onClose}>
          <X className="h-4 w-4" />
        </Button>
      </div>

      {/* Content */}
      <ScrollArea className="flex-1">
        <div className="p-4 space-y-4">
          {/* Node Name */}
          <div className="space-y-2">
            <Label htmlFor="node-name">Node Name</Label>
            <Input
              id="node-name"
              value={(nodeData.label as string) || ""}
              onChange={(e) => onUpdate({ label: e.target.value })}
              placeholder="Enter node name"
            />
          </div>

          {/* Type-specific config */}
          {node.type === "agent" && (
            <AgentConfig data={nodeData} onUpdate={onUpdate} basicOpen={basicOpen} setBasicOpen={setBasicOpen} advancedOpen={advancedOpen} setAdvancedOpen={setAdvancedOpen} />
          )}

          {node.type === "input" && (
            <InputConfig data={nodeData} onUpdate={onUpdate} />
          )}

          {node.type === "output" && (
            <OutputConfig data={nodeData} onUpdate={onUpdate} />
          )}

          {node.type === "code" && (
            <CodeConfig data={nodeData} onUpdate={onUpdate} />
          )}

          {node.type === "http" && (
            <HttpConfig data={nodeData} onUpdate={onUpdate} />
          )}

          {node.type === "condition" && (
            <ConditionConfig data={nodeData} onUpdate={onUpdate} />
          )}
        </div>
      </ScrollArea>

      {/* Footer */}
      <div className="p-4 border-t border-border flex gap-2">
        <Button variant="outline" className="flex-1 bg-transparent" onClick={onClose}>
          Cancel
        </Button>
        <Button className="flex-1" onClick={onClose}>Apply</Button>
      </div>
    </div>
  )
}

function NodeTypeIcon({ type }: { type: string }) {
  const colors: Record<string, string> = {
    input: "bg-emerald-100 text-emerald-600",
    output: "bg-rose-100 text-rose-600",
    agent: "bg-violet-100 text-violet-600",
    code: "bg-amber-100 text-amber-600",
    condition: "bg-sky-100 text-sky-600",
    http: "bg-blue-100 text-blue-600",
    tool: "bg-orange-100 text-orange-600",
  }

  return (
    <div className={cn("h-6 w-6 rounded flex items-center justify-center text-xs font-medium", colors[type] || "bg-muted")}>
      {type.charAt(0).toUpperCase()}
    </div>
  )
}

interface ConfigSectionProps {
  data: Record<string, unknown>
  onUpdate: (updates: Record<string, unknown>) => void
  basicOpen?: boolean
  setBasicOpen?: (open: boolean) => void
  advancedOpen?: boolean
  setAdvancedOpen?: (open: boolean) => void
}

function AgentConfig({ data, onUpdate, basicOpen, setBasicOpen, advancedOpen, setAdvancedOpen }: ConfigSectionProps) {
  const [toolsOpen, setToolsOpen] = useState(false)
  const { mcpServers, apiKeys } = useWorkflowStore()
  const connectedServers = mcpServers.filter((s) => s.status === "connected")
  const selectedTools = (data.tools as string[]) || []
  const selectedApiKeyId = (data.apiKeyId as string) || ""

  const toggleTool = (toolName: string) => {
    if (selectedTools.includes(toolName)) {
      onUpdate({ tools: selectedTools.filter((t) => t !== toolName) })
    } else {
      onUpdate({ tools: [...selectedTools, toolName] })
    }
  }

  return (
    <>
      <Collapsible open={basicOpen} onOpenChange={setBasicOpen}>
        <CollapsibleTrigger className="flex items-center justify-between w-full py-2 text-sm font-medium">
          Basic Settings
          <ChevronDown className={cn("h-4 w-4 transition-transform", basicOpen && "rotate-180")} />
        </CollapsibleTrigger>
        <CollapsibleContent className="space-y-4 pt-2">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>API Configuration</Label>
              <Button
                variant="ghost"
                size="sm"
                className="h-6 px-2 text-xs"
                onClick={() => window.open("/settings/api-keys", "_blank")}
              >
                <Key className="h-3 w-3 mr-1" />
                Manage Keys
              </Button>
            </div>
            <Select
              value={selectedApiKeyId}
              onValueChange={(value) => {
                onUpdate({ apiKeyId: value })
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select API Key" />
              </SelectTrigger>
              <SelectContent>
                {apiKeys.length === 0 ? (
                  <div className="p-2 text-sm text-muted-foreground text-center">
                    No API keys configured
                  </div>
                ) : (
                  apiKeys.map((key) => (
                    <SelectItem key={key.id} value={key.id}>
                      <div className="flex items-center gap-2">
                        <span>{key.provider}</span>
                        <span className="text-muted-foreground">·</span>
                        <span className="text-muted-foreground">{key.name}</span>
                      </div>
                    </SelectItem>
                  ))
                )}
              </SelectContent>
            </Select>

            <div className="space-y-2">
              <Label>Model Name</Label>
              <Input
                value={(data.model as string) || ""}
                onChange={(e) => onUpdate({ model: e.target.value })}
                placeholder="e.g. gpt-4, claude-3-opus"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label>Agent Strategy</Label>
            <Select
              value={(data.strategy as string) || "REACT"}
              onValueChange={(value) => onUpdate({ strategy: value })}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="REACT">ReAct Agent</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>System Prompt</Label>
              <div className="flex gap-1">
                <Button variant="ghost" size="sm" className="h-6 px-2 text-xs">
                  <Sparkles className="h-3 w-3 mr-1" />
                  AI
                </Button>
                <Button variant="ghost" size="sm" className="h-6 px-2 text-xs">
                  <Variable className="h-3 w-3 mr-1" />
                  Var
                </Button>
              </div>
            </div>
            <Textarea
              value={(data.systemPrompt as string) || ""}
              onChange={(e) => onUpdate({ systemPrompt: e.target.value })}
              placeholder="You are a helpful assistant..."
              rows={4}
            />
          </div>
        </CollapsibleContent>
      </Collapsible>

      <Collapsible open={advancedOpen} onOpenChange={setAdvancedOpen}>
        <CollapsibleTrigger className="flex items-center justify-between w-full py-2 text-sm font-medium">
          Model Parameters
          <ChevronDown className={cn("h-4 w-4 transition-transform", advancedOpen && "rotate-180")} />
        </CollapsibleTrigger>
        <CollapsibleContent className="space-y-4 pt-2">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>Temperature</Label>
              <span className="text-sm text-muted-foreground">{data.temperature ?? 0.7}</span>
            </div>
            <Slider
              value={[Number(data.temperature) || 0.7]}
              onValueChange={([value]) => onUpdate({ temperature: value })}
              min={0}
              max={2}
              step={0.1}
            />
          </div>

          <div className="space-y-2">
            <Label>Max Tokens</Label>
            <Input
              type="number"
              value={(data.maxTokens as number) || 4096}
              onChange={(e) => onUpdate({ maxTokens: Number(e.target.value) })}
            />
          </div>

          <div className="flex items-center justify-between">
            <Label>Stream Response</Label>
            <Switch
              checked={(data.stream as boolean) ?? true}
              onCheckedChange={(checked) => onUpdate({ stream: checked })}
            />
          </div>
        </CollapsibleContent>
      </Collapsible>

      {/* MCP Tools Section */}
      <Collapsible open={toolsOpen} onOpenChange={setToolsOpen}>
        <CollapsibleTrigger className="flex items-center justify-between w-full py-2 text-sm font-medium">
          <div className="flex items-center gap-2">
            <Wrench className="h-4 w-4" />
            MCP Tools
            {selectedTools.length > 0 && (
              <Badge variant="secondary" className="ml-1 h-5 px-1.5 text-xs">
                {selectedTools.length}
              </Badge>
            )}
          </div>
          <ChevronDown className={cn("h-4 w-4 transition-transform", toolsOpen && "rotate-180")} />
        </CollapsibleTrigger>
        <CollapsibleContent className="space-y-3 pt-2">
          {connectedServers.length === 0 ? (
            <div className="text-sm text-muted-foreground text-center py-4">
              <Server className="h-8 w-8 mx-auto mb-2 opacity-50" />
              <p>No MCP servers connected</p>
              <p className="text-xs mt-1">Configure servers in Settings</p>
            </div>
          ) : (
            connectedServers.map((server) => (
              <div key={server.id} className="space-y-2">
                <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                  <Server className="h-3 w-3" />
                  {server.name}
                </div>
                <div className="space-y-1 pl-2">
                  {server.tools.map((tool) => {
                    const fullToolName = `${server.id}/${tool.name}`
                    const isSelected = selectedTools.includes(fullToolName)
                    return (
                      <div
                        key={tool.name}
                        className={cn(
                          "flex items-start gap-2 p-2 rounded-md border cursor-pointer transition-colors",
                          isSelected ? "border-primary bg-primary/5" : "border-transparent hover:bg-muted"
                        )}
                        onClick={() => toggleTool(fullToolName)}
                      >
                        <Checkbox
                          checked={isSelected}
                          className="mt-0.5"
                        />
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-1">
                            <span className="text-sm font-medium">{tool.name}</span>
                            {isSelected && <Check className="h-3 w-3 text-primary" />}
                          </div>
                          <p className="text-xs text-muted-foreground line-clamp-2">
                            {tool.description}
                          </p>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            ))
          )}

          {selectedTools.length > 0 && (
            <div className="pt-2 border-t">
              <div className="flex items-center justify-between mb-2">
                <Label className="text-xs text-muted-foreground">Selected Tools</Label>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-6 px-2 text-xs"
                  onClick={() => onUpdate({ tools: [] })}
                >
                  Clear All
                </Button>
              </div>
              <div className="flex flex-wrap gap-1">
                {selectedTools.map((tool) => {
                  const toolName = tool.split("/").pop()
                  return (
                    <Badge
                      key={tool}
                      variant="secondary"
                      className="text-xs cursor-pointer hover:bg-destructive/20"
                      onClick={() => toggleTool(tool)}
                    >
                      {toolName}
                      <X className="h-2 w-2 ml-1" />
                    </Badge>
                  )
                })}
              </div>
            </div>
          )}
        </CollapsibleContent>
      </Collapsible>
    </>
  )
}

function InputConfig({ data, onUpdate }: ConfigSectionProps) {
  const fields = (data.fields as Array<{ name: string; type: string; required: boolean }>) || []

  const addField = () => {
    onUpdate({
      fields: [...fields, { name: "", type: "text", required: false }],
    })
  }

  const updateField = (index: number, updates: Partial<{ name: string; type: string; required: boolean }>) => {
    const newFields = [...fields]
    newFields[index] = { ...newFields[index], ...updates }
    onUpdate({ fields: newFields })
  }

  const removeField = (index: number) => {
    onUpdate({ fields: fields.filter((_, i) => i !== index) })
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <Label>Input Fields</Label>
        <Button variant="outline" size="sm" onClick={addField}>
          <Plus className="h-3 w-3 mr-1" />
          Add
        </Button>
      </div>

      <div className="space-y-2">
        {fields.map((field, index) => (
          <div key={index} className="flex items-center gap-2">
            <Input
              value={field.name}
              onChange={(e) => updateField(index, { name: e.target.value })}
              placeholder="Field name"
              className="flex-1"
            />
            <Select
              value={field.type}
              onValueChange={(value) => updateField(index, { type: value })}
            >
              <SelectTrigger className="w-24">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="text">Text</SelectItem>
                <SelectItem value="number">Number</SelectItem>
                <SelectItem value="boolean">Boolean</SelectItem>
                <SelectItem value="json">JSON</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => removeField(index)}>
              <Trash2 className="h-3 w-3" />
            </Button>
          </div>
        ))}
      </div>
    </div>
  )
}

function OutputConfig({ data, onUpdate }: ConfigSectionProps) {
  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label>Output Format</Label>
        <Select
          value={(data.format as string) || "json"}
          onValueChange={(value) => onUpdate({ format: value })}
        >
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="json">JSON</SelectItem>
            <SelectItem value="text">Plain Text</SelectItem>
            <SelectItem value="template">Custom Template</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {data.format === "template" && (
        <div className="space-y-2">
          <Label>Template</Label>
          <Textarea
            value={(data.template as string) || ""}
            onChange={(e) => onUpdate({ template: e.target.value })}
            placeholder="Enter your template..."
            rows={4}
          />
        </div>
      )}
    </div>
  )
}

function CodeConfig({ data, onUpdate }: ConfigSectionProps) {
  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label>Language</Label>
        <Select
          value={(data.language as string) || "javascript"}
          onValueChange={(value) => onUpdate({ language: value })}
        >
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="javascript">JavaScript</SelectItem>
            <SelectItem value="python">Python</SelectItem>
            <SelectItem value="java">Java</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label>Code</Label>
        <Textarea
          value={(data.code as string) || ""}
          onChange={(e) => onUpdate({ code: e.target.value })}
          placeholder="// Write your code here..."
          rows={8}
          className="font-mono text-sm"
        />
      </div>
    </div>
  )
}

function HttpConfig({ data, onUpdate }: ConfigSectionProps) {
  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label>Method</Label>
        <Select
          value={(data.method as string) || "GET"}
          onValueChange={(value) => onUpdate({ method: value })}
        >
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="GET">GET</SelectItem>
            <SelectItem value="POST">POST</SelectItem>
            <SelectItem value="PUT">PUT</SelectItem>
            <SelectItem value="DELETE">DELETE</SelectItem>
            <SelectItem value="PATCH">PATCH</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label>URL</Label>
        <Input
          value={(data.url as string) || ""}
          onChange={(e) => onUpdate({ url: e.target.value })}
          placeholder="https://api.example.com/..."
        />
      </div>

      <div className="space-y-2">
        <Label>Headers (JSON)</Label>
        <Textarea
          value={(data.headers as string) || "{}"}
          onChange={(e) => onUpdate({ headers: e.target.value })}
          placeholder="{}"
          rows={3}
          className="font-mono text-sm"
        />
      </div>

      {["POST", "PUT", "PATCH"].includes((data.method as string) || "") && (
        <div className="space-y-2">
          <Label>Body</Label>
          <Textarea
            value={(data.body as string) || ""}
            onChange={(e) => onUpdate({ body: e.target.value })}
            placeholder="{}"
            rows={4}
            className="font-mono text-sm"
          />
        </div>
      )}
    </div>
  )
}

function ConditionConfig({ data, onUpdate }: ConfigSectionProps) {
  const conditions = (data.conditions as Array<{ field: string; operator: string; value: string }>) || []

  const addCondition = () => {
    onUpdate({
      conditions: [...conditions, { field: "", operator: "==", value: "" }],
    })
  }

  const updateCondition = (index: number, updates: Partial<{ field: string; operator: string; value: string }>) => {
    const newConditions = [...conditions]
    newConditions[index] = { ...newConditions[index], ...updates }
    onUpdate({ conditions: newConditions })
  }

  const removeCondition = (index: number) => {
    onUpdate({ conditions: conditions.filter((_, i) => i !== index) })
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <Label>Conditions</Label>
        <Button variant="outline" size="sm" onClick={addCondition}>
          <Plus className="h-3 w-3 mr-1" />
          Add
        </Button>
      </div>

      <div className="space-y-3">
        {conditions.map((condition, index) => (
          <div key={index} className="space-y-2 p-3 border border-border rounded-md">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium">Condition {index + 1}</span>
              <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => removeCondition(index)}>
                <Trash2 className="h-3 w-3" />
              </Button>
            </div>
            <Input
              value={condition.field}
              onChange={(e) => updateCondition(index, { field: e.target.value })}
              placeholder="Field (e.g. input.score)"
            />
            <div className="flex gap-2">
              <Select
                value={condition.operator}
                onValueChange={(value) => updateCondition(index, { operator: value })}
              >
                <SelectTrigger className="w-24">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="==">==</SelectItem>
                  <SelectItem value="!=">!=</SelectItem>
                  <SelectItem value=">">&gt;</SelectItem>
                  <SelectItem value="<">&lt;</SelectItem>
                  <SelectItem value=">=">&gt;=</SelectItem>
                  <SelectItem value="<=">&lt;=</SelectItem>
                </SelectContent>
              </Select>
              <Input
                value={condition.value}
                onChange={(e) => updateCondition(index, { value: e.target.value })}
                placeholder="Value"
                className="flex-1"
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
