import { InputNode } from "./input-node"
import { OutputNode } from "./output-node"
import { AgentNode } from "./agent-node"
import { CodeNode } from "./code-node"
import { ConditionNode } from "./condition-node"
import { HttpNode } from "./http-node"
import { ToolNode } from "./tool-node"

export const nodeTypes = {
  input: InputNode,
  output: OutputNode,
  agent: AgentNode,
  code: CodeNode,
  condition: ConditionNode,
  http: HttpNode,
  tool: ToolNode,
}

export { InputNode, OutputNode, AgentNode, CodeNode, ConditionNode, HttpNode, ToolNode }
