import type { WorkspaceDetail, WorkspaceListResponse } from '@/features/workspace/types'
import { api } from '@/shared/api'

export async function listWorkspaces(): Promise<WorkspaceListResponse> {
  return api.get<WorkspaceListResponse>('/v1/workspaces')
}

export async function getWorkspace(workspaceId: string): Promise<WorkspaceDetail> {
  return api.get<WorkspaceDetail>(`/v1/workspaces/${workspaceId}`)
}

export async function updateWorkspace(
  workspaceId: string,
  input: {
    name: string
    timezone: string
    weekStartsOn: number
    version: number
  },
): Promise<WorkspaceDetail> {
  return api.patch<WorkspaceDetail>(`/v1/workspaces/${workspaceId}`, input)
}
