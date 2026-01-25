import { WorkflowEditor } from "@/components/workflow/workflow-editor"

interface WorkflowEditorPageProps {
  params: Promise<{ id: string }>
}

export default async function WorkflowEditorPage({ params }: WorkflowEditorPageProps) {
  const { id } = await params
  return <WorkflowEditor id={id} />
}
