export type ProjectStatus = 'PLANNED' | 'ACTIVE' | 'PAUSED' | 'COMPLETED'
export type ProjectPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export interface ProjectStats {
  taskCount: number
  completedTaskCount: number
  inProgressTaskCount: number
  noteCount: number
}

export interface Project {
  id: string
  workspaceId: string
  name: string
  description: string | null
  status: ProjectStatus
  priority: ProjectPriority
  startDate: string | null
  dueDate: string | null
  archivedAt: string | null
  createdAt: string
  updatedAt: string
  version: number
  stats: ProjectStats
}

export const PROJECT_STATUS_LABELS: Record<ProjectStatus, string> = {
  PLANNED: '已计划',
  ACTIVE: '进行中',
  PAUSED: '已暂停',
  COMPLETED: '已完成',
}

export const PROJECT_PRIORITY_LABELS: Record<ProjectPriority, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
}
