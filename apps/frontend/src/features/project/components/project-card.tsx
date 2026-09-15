import { Link } from '@tanstack/react-router'
import { FolderKanban } from 'lucide-react'

import type { Project } from '@/features/project/types'
import { PROJECT_PRIORITY_LABELS, PROJECT_STATUS_LABELS } from '@/features/project/types'
import { StatusBadge } from '@/shared/components/status-badge'
import { Progress } from '@/shared/components/ui/progress'
import { formatDate } from '@/shared/lib/datetime'

const statusTone = {
  PLANNED: 'neutral',
  ACTIVE: 'info',
  PAUSED: 'warning',
  COMPLETED: 'success',
} as const

const priorityTone = {
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'danger',
} as const

export function ProjectCard({ project }: { project: Project }) {
  const progress = project.stats.taskCount === 0
    ? 0
    : Math.round((project.stats.completedTaskCount * 100) / project.stats.taskCount)

  return (
    <Link
      to="/app/projects/$projectId"
      params={{ projectId: project.id }}
      className="flex items-center gap-3 rounded-lg border border-border-subtle bg-card p-4 shadow-xs transition-colors hover:border-border hover:bg-card-hover"
    >
      <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-primary-subtle text-primary">
        <FolderKanban className="size-4" />
      </span>
      <div className="min-w-0 flex-1">
        <p className="truncate font-medium">{project.name}</p>
        <p className="truncate text-xs text-muted-foreground">
          {project.description || '暂无描述'}
          {' · 截止 '}
          {formatDate(project.dueDate)}
        </p>
        <div className="mt-2 flex max-w-56 items-center gap-2">
          <Progress value={progress} aria-label={`${project.name}任务进度`} />
          <span className="w-8 shrink-0 text-right text-[11px] text-muted-foreground">{progress}%</span>
        </div>
      </div>
      <div className="flex shrink-0 flex-col items-end gap-1">
        <StatusBadge tone={statusTone[project.status]}>{PROJECT_STATUS_LABELS[project.status]}</StatusBadge>
        <StatusBadge tone={priorityTone[project.priority]}>{PROJECT_PRIORITY_LABELS[project.priority]}</StatusBadge>
      </div>
    </Link>
  )
}
