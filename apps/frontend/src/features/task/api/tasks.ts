import type { Task, TaskDueFilter, TaskPriority, TaskStatus } from '@/features/task/types'
import type { PaginatedResult } from '@/shared/types/api'
import { api } from '@/shared/api'

export interface TaskListQuery {
  projectId?: string
  status?: string
  priority?: TaskPriority
  due?: TaskDueFilter
  page?: number
  size?: number
  sort?: string
}

export interface CreateTaskInput {
  projectId?: string | null
  parentId?: string | null
  title: string
  description?: string | null
  status?: TaskStatus
  priority?: TaskPriority
  startAt?: string | null
  dueAt?: string | null
}

export interface UpdateTaskInput {
  projectId?: string | null
  parentId?: string | null
  title: string
  description?: string | null
  status: TaskStatus
  priority: TaskPriority
  startAt?: string | null
  dueAt?: string | null
  version: number
}

export function listTasks(query: TaskListQuery = {}): Promise<PaginatedResult<Task>> {
  return api.get<PaginatedResult<Task>>('/v1/tasks', {
    query: {
      projectId: query.projectId,
      status: query.status,
      priority: query.priority,
      due: query.due,
      page: query.page,
      size: query.size,
      sort: query.sort,
    },
  })
}

export function getTask(taskId: string): Promise<Task> {
  return api.get<Task>(`/v1/tasks/${taskId}`)
}

export function createTask(input: CreateTaskInput): Promise<Task> {
  return api.post<Task>('/v1/tasks', input)
}

export function updateTask(taskId: string, input: UpdateTaskInput): Promise<Task> {
  return api.put<Task>(`/v1/tasks/${taskId}`, input)
}

export function updateTaskStatus(taskId: string, status: TaskStatus, version: number): Promise<Task> {
  return api.patch<Task>(`/v1/tasks/${taskId}/status`, { status, version })
}

export function deleteTask(taskId: string): Promise<void> {
  return api.delete(`/v1/tasks/${taskId}`)
}
