export interface WorkspaceDetail {
  id: string
  name: string
  timezone: string
  weekStartsOn: number
  role: 'OWNER' | 'MEMBER'
  version: number
}

export interface WorkspaceListResponse {
  items: WorkspaceDetail[]
}
