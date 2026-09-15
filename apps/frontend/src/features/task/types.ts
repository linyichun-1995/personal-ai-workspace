import { formatDateTime } from '@/shared/lib/datetime'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type TaskDueFilter = 'TODAY' | 'OVERDUE' | 'UPCOMING'

export interface Task {
  id: string
  workspaceId: string
  projectId: string | null
  projectName: string | null
  parentId: string | null
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  startAt: string | null
  dueAt: string | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
  version: number
}

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: '待处理',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
  CANCELLED: '已取消',
}

export const TASK_PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
  URGENT: '紧急',
}

export interface TaskTableItem {
  id: string
  title: string
  project: string
  status: 'todo' | 'in-progress' | 'review' | 'done'
  priority: 'low' | 'medium' | 'high'
  dueAt: string
  assignee: string
  raw: Task
}

export function toTaskTableItem(task: Task, assignee: string, timeZone?: string): TaskTableItem {
  return {
    id: task.id,
    title: task.title,
    project: task.projectName ?? '未关联项目',
    status: task.status === 'IN_PROGRESS' ? 'in-progress' : task.status === 'DONE' ? 'done' : 'todo',
    priority: task.priority === 'LOW' ? 'low' : task.priority === 'HIGH' || task.priority === 'URGENT' ? 'high' : 'medium',
    dueAt: formatDateTime(task.dueAt, timeZone),
    assignee,
    raw: task,
  }
}
