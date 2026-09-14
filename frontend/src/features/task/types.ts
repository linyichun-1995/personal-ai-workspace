export type TaskStatus = 'todo' | 'in-progress' | 'review' | 'done'
export type TaskPriority = 'low' | 'medium' | 'high'

export interface TaskItem {
  id: string
  title: string
  project: string
  status: TaskStatus
  priority: TaskPriority
  dueAt: string
  assignee: string
}
