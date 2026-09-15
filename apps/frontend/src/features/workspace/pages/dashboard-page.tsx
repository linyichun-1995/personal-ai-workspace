import { Link } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ArrowRight,
  CalendarDays,
  CheckCircle2,
  Clock3,
  FolderKanban,
  ListTodo,
  NotebookPen,
  Plus,
} from 'lucide-react'

import { useSession } from '@/features/auth/hooks/use-session'
import { getDashboard } from '@/features/dashboard/api/dashboard'
import type { DashboardTask } from '@/features/dashboard/types'
import { CreateNoteButton } from '@/features/note/components/create-note-button'
import { updateTaskStatus } from '@/features/task/api/tasks'
import { TASK_PRIORITY_LABELS } from '@/features/task/types'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { queryKeys } from '@/shared/api/query-keys'
import { PageContainer } from '@/shared/components/page-container'
import { QueryState } from '@/shared/components/query-state'
import { StatusBadge } from '@/shared/components/status-badge'
import { Button } from '@/shared/components/ui/button'
import { Checkbox } from '@/shared/components/ui/checkbox'
import { Progress } from '@/shared/components/ui/progress'
import { ViewAllButton, WorkspaceCard } from '@/shared/components/workspace-card'
import { formatDateTime, formatLongDate, greetingFor } from '@/shared/lib/datetime'

export function DashboardPage() {
  const { data: session } = useSession()
  const workspaceId = session?.workspace.id
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: queryKeys.dashboard.summary(workspaceId ?? ''),
    enabled: Boolean(workspaceId),
    queryFn: getDashboard,
  })
  const complete = useMutation({
    mutationFn: (task: DashboardTask) => updateTaskStatus(task.id, 'DONE', task.version),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })

  return (
    <PageContainer className="grid max-w-[100rem] gap-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="mb-1 text-xs text-muted-foreground">{formatLongDate()}</p>
          <h1 className="text-2xl font-semibold tracking-tight">
            {greetingFor()}，{session?.user.name ?? '你'}
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            先推进眼前的任务，再回到项目继续工作。
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <CreateNoteButton variant="outline" />
          <Button asChild>
            <Link to="/app/tasks" search={{ create: true }}>
              <Plus />
              新建任务
            </Link>
          </Button>
        </div>
      </header>
      <QueryState query={query}>
        {(data) => {
          const today = data.todayTasks.filter(
            (task) => task.status !== 'DONE' && task.status !== 'CANCELLED',
          )
          const isNew =
            data.overview.totalTasks === 0 &&
            data.overview.activeProjects === 0 &&
            data.overview.notes === 0
          const metrics = [
            {
              label: '今日待办',
              value: data.overview.todayTasks,
              to: '/app/tasks/today' as const,
              icon: CalendarDays,
            },
            {
              label: '已逾期',
              value: data.overview.overdueTasks,
              to: '/app/tasks/overdue' as const,
              icon: Clock3,
            },
            {
              label: '进行中项目',
              value: data.overview.activeProjects,
              to: '/app/projects/active' as const,
              icon: FolderKanban,
            },
            {
              label: '笔记',
              value: data.overview.notes,
              to: '/app/notes' as const,
              icon: NotebookPen,
            },
          ]
          return (
            <>
              {isNew && (
                <section
                  className="rounded-xl border border-primary/20 bg-primary-subtle p-5 sm:p-6"
                  aria-labelledby="getting-started"
                >
                  <p className="text-xs font-medium text-primary">从一件具体的事开始</p>
                  <h2 id="getting-started" className="mt-2 text-lg font-semibold">
                    建立项目，写下第一步
                  </h2>
                  <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">
                    把正在推进的目标建成项目，再拆成任务；相关想法和资料写进项目笔记。零散事项也可以直接新建任务。
                  </p>
                  <Button className="mt-4" asChild>
                    <Link to="/app/projects" search={{ create: true }}>
                      创建一个项目
                      <ArrowRight />
                    </Link>
                  </Button>
                </section>
              )}
              <div className="grid grid-cols-2 gap-3 xl:grid-cols-4">
                {metrics.map((metric) => (
                  <Link
                    key={metric.label}
                    to={metric.to}
                    className="rounded-lg border border-border-subtle bg-card p-4 transition-colors hover:border-primary/40 hover:bg-card-hover focus-visible:ring-2 focus-visible:ring-ring/40"
                  >
                    <div className="flex items-center justify-between text-xs text-muted-foreground">
                      <span>{metric.label}</span>
                      <metric.icon className="size-4" aria-hidden="true" />
                    </div>
                    <p className="mt-2 text-2xl font-semibold tabular-nums">{metric.value}</p>
                  </Link>
                ))}
              </div>
              {data.overview.overdueTasks > 0 && (
                <Link
                  to="/app/tasks/overdue"
                  className="flex items-center gap-3 rounded-lg border border-warning/30 bg-warning/5 px-4 py-3 text-sm hover:bg-warning/10 focus-visible:ring-2 focus-visible:ring-ring/40"
                >
                  <Clock3 className="size-4 shrink-0 text-warning" aria-hidden="true" />
                  <span className="flex-1">
                    <strong>{data.overview.overdueTasks} 项任务已逾期</strong>
                    <span className="ml-2 text-muted-foreground">优先完成，或调整截止时间。</span>
                  </span>
                  <ArrowRight className="size-4 shrink-0" aria-hidden="true" />
                </Link>
              )}
              <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
                <div className="grid gap-5">
                  <WorkspaceCard
                    title="今天先做这些"
                    description={'今日还有 ' + data.overview.todayTasks + ' 项待办'}
                    icon={ListTodo}
                    action={<ViewAllButton to="/app/tasks/today" />}
                  >
                    {today.length ? (
                      <div className="divide-y divide-border-subtle px-4 pb-3">
                        {today.map((task) => (
                          <FocusTaskRow
                            key={task.id}
                            task={task}
                            disabled={complete.isPending}
                            onComplete={() => complete.mutate(task)}
                          />
                        ))}
                      </div>
                    ) : (
                      <div className="grid justify-items-start gap-3 px-4 pb-5">
                        <CheckCircle2 className="size-6 text-success" aria-hidden="true" />
                        <p className="text-sm font-medium">今天没有待处理的到期任务</p>
                        <p className="text-xs text-muted-foreground">
                          可以提前安排接下来的工作，或留下一条新的待办。
                        </p>
                        <Button variant="outline" size="sm" asChild>
                          <Link to="/app/tasks" search={{ create: true }}>
                            <Plus />
                            新建任务
                          </Link>
                        </Button>
                      </div>
                    )}
                  </WorkspaceCard>
                  <WorkspaceCard
                    title="继续推进项目"
                    description="任务和笔记集中在项目里"
                    icon={FolderKanban}
                    action={<ViewAllButton to="/app/projects" />}
                  >
                    <div className="grid gap-1 px-3 pb-3">
                      {data.activeProjects.slice(0, 5).map((project) => (
                        <Link
                          key={project.id}
                          to="/app/projects/$projectId"
                          params={{ projectId: project.id }}
                          search={{ tab: 'tasks' }}
                          className="rounded-md p-3 hover:bg-accent focus-visible:ring-2 focus-visible:ring-ring/40"
                        >
                          <div className="flex items-center justify-between gap-3">
                            <p className="truncate text-sm font-medium">{project.name}</p>
                            <ArrowRight
                              className="size-3.5 shrink-0 text-muted-foreground"
                              aria-hidden="true"
                            />
                          </div>
                          <div className="mt-2 flex items-center gap-3">
                            <Progress
                              value={project.progress}
                              aria-label={project.name + '任务进度'}
                            />
                            <span className="shrink-0 text-xs tabular-nums text-muted-foreground">
                              {project.completedTaskCount} / {project.taskCount} 项完成
                            </span>
                          </div>
                        </Link>
                      ))}
                      {!data.activeProjects.length && (
                        <p className="px-1 py-3 text-sm text-muted-foreground">
                          还没有进行中的项目。把一个目标拆成可以执行的小任务。
                        </p>
                      )}
                      <Button variant="ghost" className="justify-start text-primary" asChild>
                        <Link to="/app/projects" search={{ create: true }}>
                          <Plus />
                          新建项目
                        </Link>
                      </Button>
                    </div>
                  </WorkspaceCard>
                </div>
                <div className="grid gap-5">
                  <WorkspaceCard
                    title="接下来的安排"
                    description="未来 7 天到期的任务"
                    icon={CalendarDays}
                    action={<ViewAllButton to="/app/tasks/upcoming" />}
                  >
                    <div className="divide-y divide-border-subtle px-4 pb-3">
                      {data.upcomingTasks.slice(0, 5).map((task) => (
                        <FocusTaskRow key={task.id} task={task} />
                      ))}
                      {!data.upcomingTasks.length && (
                        <p className="py-3 text-sm text-muted-foreground">
                          近期没有安排，给任务设置截止时间后会显示在这里。
                        </p>
                      )}
                    </div>
                  </WorkspaceCard>
                  <WorkspaceCard
                    title="继续写笔记"
                    description="最近更新的工作记录"
                    icon={NotebookPen}
                    action={<ViewAllButton to="/app/notes" />}
                  >
                    <div className="grid gap-1 px-3 pb-3">
                      {data.recentNotes.slice(0, 5).map((note) => (
                        <Link
                          key={note.id}
                          to="/app/notes/$noteId"
                          params={{ noteId: note.id }}
                          className="min-w-0 rounded-md p-3 hover:bg-accent focus-visible:ring-2 focus-visible:ring-ring/40"
                        >
                          <p className="truncate text-sm font-medium">{note.title}</p>
                          <p className="mt-1 truncate text-xs text-muted-foreground">
                            {note.summary || '继续记录想法…'}
                          </p>
                          <p className="mt-1 text-[11px] text-muted-foreground">
                            {formatDateTime(note.updatedAt)}
                          </p>
                        </Link>
                      ))}
                      {!data.recentNotes.length && (
                        <p className="px-1 py-3 text-sm text-muted-foreground">
                          记下想法、会议结论或下一步计划。
                        </p>
                      )}
                      <CreateNoteButton variant="ghost" className="justify-start text-primary" />
                    </div>
                  </WorkspaceCard>
                </div>
              </div>
            </>
          )
        }}
      </QueryState>
    </PageContainer>
  )
}

function FocusTaskRow({
  task,
  disabled,
  onComplete,
}: {
  task: DashboardTask
  disabled?: boolean
  onComplete?: () => void
}) {
  return (
    <div className="flex items-start gap-3 py-3">
      {onComplete && (
        <Checkbox
          className="mt-1"
          aria-label={'完成任务：' + task.title}
          disabled={disabled}
          checked={false}
          onChange={onComplete}
        />
      )}
      <Link
        to="/app/tasks"
        search={{ taskId: task.id }}
        className="min-w-0 flex-1 rounded-sm focus-visible:ring-2 focus-visible:ring-ring/40"
      >
        <p className="break-words text-sm font-medium hover:text-primary">{task.title}</p>
        <p className="mt-1 text-xs text-muted-foreground">
          {task.projectName ?? '独立任务'} · {formatDateTime(task.dueAt)}
        </p>
      </Link>
      {(task.priority === 'URGENT' || task.priority === 'HIGH') && (
        <StatusBadge tone="danger">{TASK_PRIORITY_LABELS[task.priority]}</StatusBadge>
      )}
    </div>
  )
}
