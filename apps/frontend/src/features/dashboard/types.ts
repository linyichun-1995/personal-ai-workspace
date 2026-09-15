export interface DashboardResponse {
  overview: {
    activeProjects: number
    todayTasks: number
    overdueTasks: number
    notes: number
    notesThisWeek: number
    aiConversations: number
    completionRate: number
    reviewTasks: number
    totalTasks: number
    completedTasks: number
  }
  todayTasks: DashboardTask[]
  nextTasks: DashboardTask[]
  activeProjects: DashboardProject[]
  recentNotes: DashboardNote[]
  upcomingTasks: DashboardTask[]
  taskStatusCounts: {
    todo: number
    inProgress: number
    review: number
    done: number
  }
  projectTaskStats: Array<{
    projectId: string
    name: string
    taskCount: number
    completedTaskCount: number
  }>
}

export interface DashboardTask {
  id: string
  title: string
  projectId: string | null
  projectName: string | null
  status: 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  dueAt: string | null
  version: number
}

export interface DashboardProject {
  id: string
  name: string
  description: string | null
  status: string
  priority: string
  dueDate: string | null
  progress: number
  memberCount: number
  taskCount: number
  completedTaskCount: number
}

export interface DashboardNote {
  id: string
  title: string
  summary: string
  updatedAt: string
  projectId: string | null
}
