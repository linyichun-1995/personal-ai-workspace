import type { LucideIcon } from 'lucide-react'
import { Link } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  BarChart3,
  CalendarDays,
  ChartNoAxesCombined,
  CheckCheck,
  CheckSquare2,
  Clock3,
  FileText,
  FolderKanban,
  ListTodo,
  MessageSquareText,
  NotebookPen,
  Send,
  Sparkles,
  Sun,
} from 'lucide-react'

import { useSession } from '@/features/auth/hooks/use-session'
import { getDashboard } from '@/features/dashboard/api/dashboard'
import type { DashboardTask } from '@/features/dashboard/types'
import { updateTaskStatus } from '@/features/task/api/tasks'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { queryKeys } from '@/shared/api/query-keys'
import { MetricCard } from '@/shared/components/metric-card'
import { PageContainer } from '@/shared/components/page-container'
import { QueryState } from '@/shared/components/query-state'
import { StatusBadge } from '@/shared/components/status-badge'
import { Checkbox } from '@/shared/components/ui/checkbox'
import { Progress } from '@/shared/components/ui/progress'
import { ViewAllButton, WorkspaceCard } from '@/shared/components/workspace-card'
import { formatDateTime, formatLongDate, greetingFor } from '@/shared/lib/datetime'
import { cn } from '@/shared/lib/utils'

const priorityMeta = {
  URGENT: { label: '紧急', tone: 'danger' as const },
  HIGH: { label: '高', tone: 'danger' as const },
  MEDIUM: { label: '中', tone: 'warning' as const },
  LOW: { label: '低', tone: 'success' as const },
}

const taskStatuses = [
  { value: 'todo', label: '待开始', color: 'bg-muted-foreground', key: 'todo' as const },
  { value: 'in-progress', label: '进行中', color: 'bg-info', key: 'inProgress' as const },
  { value: 'review', label: '待评审', color: 'bg-warning', key: 'review' as const },
  { value: 'done', label: '已完成', color: 'bg-success', key: 'done' as const },
] as const

const toneClasses = {
  primary: 'bg-primary-subtle text-primary',
  info: 'bg-info-subtle text-info',
  success: 'bg-success-subtle text-success',
  warning: 'bg-warning-subtle text-warning',
  danger: 'bg-destructive-subtle text-destructive',
}

const projectTones = ['primary', 'info', 'warning', 'success'] as const

function projectTone(index: number): keyof typeof toneClasses {
  return projectTones[index % projectTones.length] ?? 'primary'
}

function IconTile({ icon: Icon, tone }: { icon: LucideIcon, tone: keyof typeof toneClasses }) {
  return (
    <span className={cn('dashboard-icon grid size-8 shrink-0 place-items-center rounded-lg', toneClasses[tone])}>
      <Icon className="size-4" strokeWidth={1.8} />
    </span>
  )
}

function AvatarStack({ name, count }: { name: string, count: number }) {
  return (
    <div className="flex items-center">
      <div className="flex -space-x-1.5" aria-label={`${count} 位成员`}>
        <span className="dashboard-avatar grid size-5 place-items-center rounded-full border-2 border-card bg-secondary text-[8px] font-semibold text-muted-foreground">
          {name.slice(0, 1)}
        </span>
      </div>
      <span className="ml-1.5 text-[11px] text-muted-foreground">{count}</span>
    </div>
  )
}

function EmptyLine({ text }: { text: string }) {
  return <p className="px-4 py-6 text-center text-xs text-muted-foreground">{text}</p>
}

export function DashboardPage() {
  const session = useSession()
  const workspaceId = session.data?.workspace.id
  const userName = session.data?.user.name ?? '你'
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: queryKeys.dashboard.summary(workspaceId ?? ''),
    enabled: Boolean(workspaceId),
    queryFn: getDashboard,
  })
  const completeMutation = useMutation({
    mutationFn: ({ id, status, version }: { id: string, status: 'DONE' | 'TODO', version: number }) =>
      updateTaskStatus(id, status, version),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })

  return (
    <div className="min-h-full bg-background">
      <PageContainer className="dashboard-page @container/dashboard grid content-start gap-4 py-5 md:py-5">
        <QueryState query={query}>
          {(data) => {
            const remainingToday = data.todayTasks.filter(task => task.status !== 'DONE').length
            const totalTasks = Math.max(data.overview.totalTasks, 1)
            const taskStatusCounts = taskStatuses.map(status => ({
              ...status,
              count: data.taskStatusCounts[status.key],
            }))
            return (
              <>
                <header className="dashboard-greeting flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex items-start gap-3">
                    <span className="grid size-9 shrink-0 place-items-center text-warning">
                      <Sun className="size-6" />
                    </span>
                    <div>
                      <h1 className="text-xl font-semibold tracking-tight">
                        {greetingFor()}，{userName}
                      </h1>
                      <p className="mt-0.5 text-[13px] text-muted-foreground">
                        这是今天工作空间里最值得关注的内容。
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 text-xs text-muted-foreground">
                    <span className="hidden sm:inline">{formatLongDate()}</span>
                    <select
                      className="h-9 rounded-md border bg-card px-3 text-xs text-foreground shadow-xs focus-visible:ring-2 focus-visible:ring-ring/30"
                      aria-label="统计周期"
                      defaultValue="week"
                    >
                      <option value="week">本周</option>
                      <option value="today">今天</option>
                      <option value="month">本月</option>
                    </select>
                  </div>
                </header>

                <section className="dashboard-metrics grid gap-4" aria-label="工作空间指标">
                  <MetricCard
                    label="活跃项目"
                    value={data.overview.activeProjects}
                    trend={`${data.overview.totalTasks} 个关联任务`}
                    trendDirection="neutral"
                    icon={FolderKanban}
                    tone="info"
                    chart={[]}
                  />
                  <MetricCard
                    label="今日待完成"
                    value={data.overview.todayTasks}
                    trend={`逾期 ${data.overview.overdueTasks} 项`}
                    trendDirection="neutral"
                    icon={CheckSquare2}
                    tone="danger"
                    chart={[]}
                  />
                  <MetricCard
                    label="本周新增笔记"
                    value={data.overview.notesThisWeek}
                    trend={`全部 ${data.overview.notes} 篇`}
                    trendDirection="neutral"
                    icon={NotebookPen}
                    tone="primary"
                    chart={[]}
                  />
                  <MetricCard
                    label="AI 对话"
                    value={data.overview.aiConversations}
                    trend="后续版本提供"
                    trendDirection="neutral"
                    icon={MessageSquareText}
                    tone="info"
                    chart={[]}
                  />
                  <MetricCard
                    label="任务完成率"
                    value={`${data.overview.completionRate}%`}
                    trend={`已完成 ${data.overview.completedTasks} / ${data.overview.totalTasks}`}
                    trendDirection="neutral"
                    icon={CheckCheck}
                    tone="success"
                    chart={[]}
                  />
                  <MetricCard
                    label="待评审任务"
                    value={data.overview.reviewTasks}
                    trend="等待评审"
                    trendDirection="neutral"
                    icon={ListTodo}
                    tone="warning"
                    chart={[]}
                  />
                </section>

                <div className="dashboard-panels grid gap-4">
                  <WorkspaceCard
                    title="今日任务"
                    description={`${remainingToday} 项待完成 · ${data.todayTasks.length - remainingToday} 项已完成`}
                    icon={CheckSquare2}
                    action={<ViewAllButton to="/app/tasks/today" />}
                  >
                    {data.todayTasks.length === 0
                      ? <EmptyLine text="今天没有到期任务" />
                      : (
                          <div className="flex flex-col divide-y divide-border-subtle px-4 pb-3">
                            {data.todayTasks.map(task => (
                              <TodayTaskRow
                                key={task.id}
                                task={task}
                                userName={userName}
                                disabled={completeMutation.isPending}
                                onToggle={checked => completeMutation.mutate({
                                  id: task.id,
                                  status: checked ? 'DONE' : 'TODO',
                                  version: task.version,
                                })}
                              />
                            ))}
                          </div>
                        )}
                  </WorkspaceCard>

                  <WorkspaceCard
                    title="进行中的项目"
                    description={`${data.activeProjects.length} 个项目 · 进度与协作成员`}
                    icon={FolderKanban}
                    action={<ViewAllButton to="/app/projects/active" />}
                  >
                    {data.activeProjects.length === 0
                      ? <EmptyLine text="还没有进行中的项目" />
                      : (
                          <div className="flex flex-col divide-y divide-border-subtle px-4 pb-3">
                            {data.activeProjects.map((project, index) => (
                              <Link
                                key={project.id}
                                to="/app/projects/$projectId"
                                params={{ projectId: project.id }}
                                className="dashboard-project-row flex h-[70px] items-center gap-3"
                              >
                                <IconTile icon={FolderKanban} tone={projectTone(index)} />
                                <div className="min-w-0 flex-1">
                                  <p title={project.name} className="truncate text-[13px] font-medium">{project.name}</p>
                                  <p title={project.description ?? ''} className="truncate text-[11px] text-muted-foreground">
                                    {project.description || '暂无描述'}
                                  </p>
                                  <div className="mt-1 flex max-w-48 items-center gap-2">
                                    <Progress value={project.progress} aria-label={`${project.name}项目进度`} />
                                    <span className="w-7 shrink-0 text-right text-[11px] text-muted-foreground">{project.progress}%</span>
                                  </div>
                                </div>
                                <AvatarStack name={userName} count={project.memberCount} />
                              </Link>
                            ))}
                          </div>
                        )}
                  </WorkspaceCard>

                  <WorkspaceCard
                    title="任务状态"
                    description={`全部 ${data.overview.totalTasks} 项任务`}
                    icon={BarChart3}
                    action={<ViewAllButton to="/app/tasks" />}
                  >
                    <div className="grid gap-4 px-4 pb-4">
                      <div className="flex items-baseline gap-2">
                        <span className="text-2xl font-semibold">{data.overview.completionRate}%</span>
                        <span className="text-xs text-muted-foreground">整体完成率</span>
                      </div>
                      <div className="flex h-2 overflow-hidden rounded-full bg-secondary" aria-hidden="true">
                        {taskStatusCounts.map(status => (
                          <span
                            key={status.value}
                            className={status.color}
                            style={{ width: `${(status.count / totalTasks) * 100}%` }}
                          />
                        ))}
                      </div>
                      <dl className="grid gap-3">
                        {taskStatusCounts.map(status => (
                          <div key={status.value} className="flex items-center justify-between text-xs">
                            <dt className="flex items-center gap-2 text-muted-foreground">
                              <span className={cn('size-2 rounded-full', status.color)} />
                              {status.label}
                            </dt>
                            <dd className="font-medium">
                              {status.count}
                              <span className="ml-1 font-normal text-muted-foreground">项</span>
                            </dd>
                          </div>
                        ))}
                      </dl>
                      <p className="border-t border-border-subtle pt-3 text-xs text-muted-foreground">
                        今日进度：已完成 {data.todayTasks.length - remainingToday} / {data.todayTasks.length}
                      </p>
                    </div>
                  </WorkspaceCard>

                  <WorkspaceCard
                    title="AI 简报"
                    description="工作空间重点摘要"
                    icon={Sparkles}
                    action={<ViewAllButton to="/app/ai" />}
                  >
                    <div className="flex h-full flex-col gap-4 px-4 pb-4">
                      <p className="text-xs leading-6 text-muted-foreground">AI 能力将在后续版本提供。</p>
                      <Link
                        to="/app/ai"
                        className="mt-auto flex min-h-9 items-center gap-2 rounded-md border border-border-subtle bg-surface-sunken/60 px-3 text-muted-foreground hover:bg-surface-subtle focus-visible:ring-2 focus-visible:ring-ring/30"
                      >
                        <span className="flex-1 text-xs">问问你的工作空间…</span>
                        <Send className="size-3.5 text-primary" />
                      </Link>
                    </div>
                  </WorkspaceCard>

                  <WorkspaceCard
                    title="最近笔记"
                    description="继续阅读与整理"
                    icon={FileText}
                    action={<ViewAllButton to="/app/notes" />}
                  >
                    {data.recentNotes.length === 0
                      ? <EmptyLine text="还没有笔记" />
                      : (
                          <div className="flex flex-col px-2 pb-3">
                            {data.recentNotes.map((note, index) => (
                              <Link
                                key={note.id}
                                to="/app/notes/$noteId"
                                params={{ noteId: note.id }}
                                className="dashboard-note-row flex h-14 w-full items-center gap-3 rounded-md px-2 text-left hover:bg-surface-subtle focus-visible:ring-2 focus-visible:ring-ring/30"
                              >
                                <IconTile icon={FileText} tone={projectTone(index)} />
                                <span className="min-w-0">
                                  <span title={note.title} className="block truncate text-[13px] font-medium">{note.title}</span>
                                  <span className="block truncate text-[11px] text-muted-foreground">
                                    {formatDateTime(note.updatedAt)}
                                  </span>
                                </span>
                              </Link>
                            ))}
                          </div>
                        )}
                  </WorkspaceCard>

                  <WorkspaceCard
                    title="即将开始"
                    description="即将到期的任务"
                    icon={CalendarDays}
                    action={<ViewAllButton to="/app/tasks/upcoming" />}
                  >
                    {data.upcomingTasks.length === 0
                      ? <EmptyLine text="近期没有即将到期的任务" />
                      : (
                          <div className="flex flex-col px-2 pb-3">
                            {data.upcomingTasks.map((item, index) => (
                              <div key={item.id} className="dashboard-note-row flex h-14 items-center gap-3 rounded-md px-2">
                                <IconTile icon={Clock3} tone={index === 0 ? 'danger' : projectTone(index)} />
                                <div className="min-w-0">
                                  <p title={item.title} className="truncate text-[13px] font-medium">{item.title}</p>
                                  <p className="truncate text-[11px] text-muted-foreground">{formatDateTime(item.dueAt)}</p>
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                  </WorkspaceCard>

                  <WorkspaceCard
                    title="后续待办"
                    description={`${data.nextTasks.length} 项任务 · 按到期时间排列`}
                    icon={ListTodo}
                    action={<ViewAllButton to="/app/tasks/upcoming" />}
                  >
                    {data.nextTasks.length === 0
                      ? <EmptyLine text="没有后续待办" />
                      : (
                          <div className="divide-y divide-border-subtle px-4 pb-3">
                            {data.nextTasks.map(task => (
                              <Link
                                key={task.id}
                                to="/app/tasks/upcoming"
                                className="dashboard-next-task-row flex h-14 items-center gap-3 rounded-sm hover:bg-surface-subtle focus-visible:ring-2 focus-visible:ring-ring/30"
                              >
                                <div className="min-w-0 flex-1">
                                  <p title={task.title} className="truncate text-[13px]">{task.title}</p>
                                  <p className="truncate text-[11px] text-muted-foreground">
                                    {task.projectName ?? '未关联项目'}
                                    {' · '}
                                    {userName}
                                  </p>
                                </div>
                                <span className="shrink-0 text-[11px] text-muted-foreground">{formatDateTime(task.dueAt)}</span>
                              </Link>
                            ))}
                          </div>
                        )}
                  </WorkspaceCard>

                  <WorkspaceCard
                    title="项目任务分布"
                    description="各项目的任务量与完成情况"
                    icon={ChartNoAxesCombined}
                    action={<ViewAllButton to="/app/projects" />}
                  >
                    {data.projectTaskStats.length === 0
                      ? <EmptyLine text="还没有项目任务" />
                      : (
                          <div className="divide-y divide-border-subtle px-4 pb-3">
                            {data.projectTaskStats.map((project) => {
                              const percent = project.taskCount ? (project.completedTaskCount / project.taskCount) * 100 : 0
                              return (
                                <div key={project.projectId} className="grid h-[70px] content-center gap-2">
                                  <div className="flex items-center justify-between gap-2 text-xs">
                                    <p title={project.name} className="min-w-0 truncate font-medium">{project.name}</p>
                                    <span className="shrink-0 text-muted-foreground">
                                      {project.taskCount} 项 · 已完成 {project.completedTaskCount}
                                    </span>
                                  </div>
                                  <Progress value={percent} indicatorClassName="bg-success" aria-label={`${project.name}任务完成率`} />
                                </div>
                              )
                            })}
                          </div>
                        )}
                  </WorkspaceCard>
                </div>
              </>
            )
          }}
        </QueryState>
      </PageContainer>
    </div>
  )
}

function TodayTaskRow({
  task,
  userName,
  disabled,
  onToggle,
}: {
  task: DashboardTask
  userName: string
  disabled: boolean
  onToggle: (checked: boolean) => void
}) {
  const checked = task.status === 'DONE'
  return (
    <div className="dashboard-task-row flex h-14 items-center gap-2.5">
      <Checkbox
        aria-label={`完成任务：${task.title}`}
        checked={checked}
        disabled={disabled}
        onChange={event => onToggle(event.target.checked)}
      />
      <div className="min-w-0 flex-1">
        <p title={task.title} className={cn('truncate text-[13px]', checked && 'text-muted-foreground line-through')}>
          {task.title}
        </p>
        <p className="truncate text-[11px] text-muted-foreground">
          {task.projectName ?? '未关联项目'}
          {' · '}
          {userName}
        </p>
      </div>
      <StatusBadge tone={priorityMeta[task.priority].tone}>{priorityMeta[task.priority].label}</StatusBadge>
      <time className="w-16 shrink-0 text-right text-xs text-muted-foreground">{formatDateTime(task.dueAt)}</time>
    </div>
  )
}
