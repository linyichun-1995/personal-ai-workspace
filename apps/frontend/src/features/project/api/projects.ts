import type { Project, ProjectPriority, ProjectStatus } from '@/features/project/types'
import type { PaginatedResult } from '@/shared/types/api'
import { api } from '@/shared/api'

export interface ProjectListQuery {
  status?: string
  priority?: ProjectPriority
  archived?: boolean
  page?: number
  size?: number
  sort?: string
}

export interface CreateProjectInput {
  name: string
  description?: string | null
  status: ProjectStatus
  priority: ProjectPriority
  startDate?: string | null
  dueDate?: string | null
}

export interface UpdateProjectInput extends CreateProjectInput {
  version: number
}

export function listProjects(query: ProjectListQuery = {}): Promise<PaginatedResult<Project>> {
  return api.get<PaginatedResult<Project>>('/v1/projects', {
    query: {
      status: query.status,
      priority: query.priority,
      archived: query.archived,
      page: query.page,
      size: query.size,
      sort: query.sort,
    },
  })
}

export function getProject(projectId: string): Promise<Project> {
  return api.get<Project>(`/v1/projects/${projectId}`)
}

export function createProject(input: CreateProjectInput): Promise<Project> {
  return api.post<Project>('/v1/projects', input)
}

export function updateProject(projectId: string, input: UpdateProjectInput): Promise<Project> {
  return api.put<Project>(`/v1/projects/${projectId}`, input)
}

export function updateProjectStatus(projectId: string, status: ProjectStatus, version: number): Promise<Project> {
  return api.patch<Project>(`/v1/projects/${projectId}/status`, { status, version })
}

export function archiveProject(projectId: string, version: number): Promise<Project> {
  return api.post<Project>(`/v1/projects/${projectId}/archive`, { version })
}

export function restoreProject(projectId: string, version: number): Promise<Project> {
  return api.post<Project>(`/v1/projects/${projectId}/restore`, { version })
}

export function deleteProject(projectId: string): Promise<void> {
  return api.delete(`/v1/projects/${projectId}`)
}
