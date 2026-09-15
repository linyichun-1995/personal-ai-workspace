export interface Note {
  id: string
  workspaceId: string
  projectId: string | null
  title: string
  content: string | null
  summary: string
  favorite: boolean
  archived: boolean
  createdAt: string
  updatedAt: string
  version: number
}
