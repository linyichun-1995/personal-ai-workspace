import type { LucideIcon } from 'lucide-react'
import { Link } from '@tanstack/react-router'
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

import { useState } from 'react'
import { demoTasks } from '@/features/task/data/demo-tasks'
import { MetricCard } from '@/shared/components/metric-card'
import { PageContainer } from '@/shared/components/page-container'
import { StatusBadge } from '@/shared/components/status-badge'
import { Checkbox } from '@/shared/components/ui/checkbox'
import { Progress } from '@/shared/components/ui/progress'
import { ViewAllButton, WorkspaceCard } from '@/shared/components/workspace-card'
import { cn } from '@/shared/lib/utils'

const focusTasks = demoTasks.filter((task) => task.dueAt.startsWith('今天'))
const nextTasks = demoTasks.filter(
  (task) => !task.dueAt.startsWith('今天') && task.status !== 'done',
)
const priorityMeta = {
  high: { label: '高', tone: 'danger' },
  medium: { label: '中', tone: 'warning' },
  low: { label: '低', tone: 'success' },
} as const
const taskStatuses = [
  { value: 'todo', label: '待开始', color: 'bg-muted-foreground' },
  { value: 'in-progress', label: '进行中', color: 'bg-info' },
  { value: 'review', label: '待评审', color: 'bg-warning' },
  { value: 'done', label: '已完成', color: 'bg-success' },
] as const

const activeProjects = [
  {
    name: 'AI 知识库',
    description: '构建基于 RAG 的个人知识中枢',
    progress: 72,
    members: 3,
    tone: 'primary',
  },
  {
    name: 'Workspace Platform',
    description: '核心工作台与组件体系',
    progress: 45,
    members: 5,
    tone: 'info',
  },
  {
    name: '飞书集成',
    description: '同步笔记、任务与文件',
    progress: 28,
    members: 2,
    tone: 'warning',
  },
  {
    name: 'Personal AI Assistant',
    description: '长期陪伴的智能生产力伙伴',
    progress: 60,
    members: 4,
    tone: 'success',
  },
] as const

const recentNotes = [
  { title: '会议纪要：产品路线图', meta: '今天 10:24 · 项目', tone: 'primary' },
  { title: '想法：AI Agent 能力边界', meta: '昨天 16:20 · 个人', tone: 'success' },
  { title: '技术设计：RAG Pipeline', meta: '9月 11日 · 工程', tone: 'warning' },
] as const

const upcoming = [
  { title: '产品评审会议', meta: '今天 14:00 – 15:00', tone: 'danger' },
  { title: 'Sprint Planning', meta: '明天 10:00 – 11:00', tone: 'success' },
  { title: '候选人技术面试', meta: '9月 15日 14:00 – 16:00', tone: 'info' },
] as const

const toneClasses = {
  primary: 'bg-primary-subtle text-primary',
  info: 'bg-info-subtle text-info',
  success: 'bg-success-subtle text-success',
  warning: 'bg-warning-subtle text-warning',
  danger: 'bg-destructive-subtle text-destructive',
}

function IconTile({ icon: Icon, tone }: { icon: LucideIcon; tone: keyof typeof toneClasses }) {
  return (
    <span
      className={cn(
        'dashboard-icon grid size-8 shrink-0 place-items-center rounded-lg',
        toneClasses[tone],
      )}
    >
      <Icon className="size-4" strokeWidth={1.8} />
    </span>
  )
}

function AvatarStack({ count }: { count: number }) {
  const initials = ['A', 'M', 'J']
  return (
    <div className="flex items-center">
      <div className="flex -space-x-1.5" aria-label={`${count} 位成员`}>
        {initials.slice(0, Math.min(count, 3)).map((initial) => (
          <span
            key={initial}
            className="dashboard-avatar grid size-5 place-items-center rounded-full border-2 border-card bg-secondary text-[8px] font-semibold text-muted-foreground"
          >
            {initial}
          </span>
        ))}
      </div>
      <span className="ml-1.5 text-[11px] text-muted-foreground">{count}</span>
    </div>
  )
}

export function DashboardPage() {
  const [completedTasks, setCompletedTasks] = useState<string[]>(() =>
    demoTasks.filter((task) => task.status === 'done').map((task) => task.id),
  )
  const remainingToday = focusTasks.filter((task) => !completedTasks.includes(task.id)).length
  const completionRate = Math.round((completedTasks.length / demoTasks.length) * 100)
  const taskStatusCounts = taskStatuses.map((status) => ({
    ...status,
    count: demoTasks.filter(
      (task) => (completedTasks.includes(task.id) ? 'done' : task.status) === status.value,
    ).length,
  }))
  const reviewCount = taskStatusCounts.find((status) => status.value === 'review')?.count ?? 0

  return (
    <div className="min-h-full bg-background">
      <PageContainer className="dashboard-page @container/dashboard grid content-start gap-4 py-5 md:py-5">
        <header className="dashboard-greeting flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-start gap-3">
            <span className="grid size-9 shrink-0 place-items-center text-warning">
              <Sun className="size-6" />
            </span>
            <div>
              <h1 className="text-xl font-semibold tracking-tight">早上好，Alex</h1>
              <p className="mt-0.5 text-[13px] text-muted-foreground">
                这是今天工作空间里最值得关注的内容。<span className="ml-2 text-xs">示例数据</span>
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3 text-xs text-muted-foreground">
            <span className="hidden sm:inline">2026年9月13日 · 星期日</span>
            <select
              className="h-9 rounded-md border bg-card px-3 text-xs text-foreground shadow-xs focus-visible:ring-2 focus-visible:ring-ring/30"
              aria-label="统计周期"
            >
              <option>本周</option>
              <option>今天</option>
              <option>本月</option>
            </select>
          </div>
        </header>

        <section className="dashboard-metrics grid gap-4" aria-label="工作空间指标">
          <MetricCard
            label="活跃项目"
            value={activeProjects.length}
            trend={`${demoTasks.length} 个关联任务`}
            trendDirection="neutral"
            icon={FolderKanban}
            tone="info"
            chart={[]}
          />
          <MetricCard
            label="今日待完成"
            value={remainingToday}
            trend={`今日共 ${focusTasks.length} 项任务`}
            trendDirection="neutral"
            icon={CheckSquare2}
            tone="danger"
            chart={[]}
          />
          <MetricCard
            label="本周新增笔记"
            value={12}
            trend="较上周 +6"
            icon={NotebookPen}
            tone="primary"
            chart={[20, 34, 48, 58, 70, 86]}
          />
          <MetricCard
            label="AI 对话"
            value={28}
            trend="较上周 +40%"
            icon={MessageSquareText}
            tone="info"
            chart={[22, 30, 45, 50, 72, 90]}
          />
          <MetricCard
            label="任务完成率"
            value={`${completionRate}%`}
            trend={`已完成 ${completedTasks.length} / ${demoTasks.length}`}
            trendDirection="neutral"
            icon={CheckCheck}
            tone="success"
            chart={[]}
          />
          <MetricCard
            label="待评审任务"
            value={reviewCount}
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
            description={`${remainingToday} 项待完成 · ${focusTasks.length - remainingToday} 项已完成`}
            icon={CheckSquare2}
            action={<ViewAllButton to="/app/tasks/today" />}
          >
            <div className="flex flex-col divide-y divide-border-subtle px-4 pb-3">
              {focusTasks.map((task) => (
                <div key={task.id} className="dashboard-task-row flex h-14 items-center gap-2.5">
                  <Checkbox
                    aria-label={`完成任务：${task.title}`}
                    checked={completedTasks.includes(task.id)}
                    onChange={(event) =>
                      setCompletedTasks((current) =>
                        event.target.checked
                          ? [...current, task.id]
                          : current.filter((id) => id !== task.id),
                      )
                    }
                  />
                  <div className="min-w-0 flex-1">
                    <p
                      title={task.title}
                      className={cn(
                        'truncate text-[13px]',
                        completedTasks.includes(task.id) && 'text-muted-foreground line-through',
                      )}
                    >
                      {task.title}
                    </p>
                    <p className="truncate text-[11px] text-muted-foreground">
                      {task.project} · {task.assignee}
                    </p>
                  </div>
                  <StatusBadge tone={priorityMeta[task.priority].tone}>
                    {priorityMeta[task.priority].label}
                  </StatusBadge>
                  <time className="w-10 shrink-0 text-right text-xs text-muted-foreground">
                    {task.dueAt.replace('今天 ', '')}
                  </time>
                </div>
              ))}
            </div>
          </WorkspaceCard>

          <WorkspaceCard
            title="进行中的项目"
            description={`${activeProjects.length} 个项目 · 进度与协作成员`}
            icon={FolderKanban}
            action={<ViewAllButton to="/app/projects/active" />}
          >
            <div className="flex flex-col divide-y divide-border-subtle px-4 pb-3">
              {activeProjects.map((project) => (
                <div
                  key={project.name}
                  className="dashboard-project-row flex h-[70px] items-center gap-3"
                >
                  <IconTile icon={FolderKanban} tone={project.tone} />
                  <div className="min-w-0 flex-1">
                    <p title={project.name} className="truncate text-[13px] font-medium">
                      {project.name}
                    </p>
                    <p
                      title={project.description}
                      className="truncate text-[11px] text-muted-foreground"
                    >
                      {project.description}
                    </p>
                    <div className="mt-1 flex max-w-48 items-center gap-2">
                      <Progress value={project.progress} aria-label={`${project.name}项目进度`} />
                      <span className="w-7 shrink-0 text-right text-[11px] text-muted-foreground">
                        {project.progress}%
                      </span>
                    </div>
                  </div>
                  <AvatarStack count={project.members} />
                </div>
              ))}
            </div>
          </WorkspaceCard>

          <WorkspaceCard
            title="任务状态"
            description={`全部 ${demoTasks.length} 项任务`}
            icon={BarChart3}
            action={<ViewAllButton to="/app/tasks" />}
          >
            <div className="grid gap-4 px-4 pb-4">
              <div className="flex items-baseline gap-2">
                <span className="text-2xl font-semibold">{completionRate}%</span>
                <span className="text-xs text-muted-foreground">整体完成率</span>
              </div>
              <div
                className="flex h-2 overflow-hidden rounded-full bg-secondary"
                aria-hidden="true"
              >
                {taskStatusCounts.map((status) => (
                  <span
                    key={status.value}
                    className={status.color}
                    style={{ width: `${(status.count / demoTasks.length) * 100}%` }}
                  />
                ))}
              </div>
              <dl className="grid gap-3">
                {taskStatusCounts.map((status) => (
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
                今日进度：已完成 {focusTasks.length - remainingToday} / {focusTasks.length}
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
              <p className="text-xs leading-6 text-muted-foreground">
                {remainingToday > 0
                  ? `今天还有 ${remainingToday} 项任务待完成，建议优先处理高优先级任务，再推进项目评审。`
                  : '今日任务已全部完成，可以查看后续待办，安排下一步工作。'}
              </p>
              <ul className="grid gap-3 text-xs text-muted-foreground">
                <li className="flex gap-2">
                  <span className="mt-1.5 size-1 shrink-0 rounded-full bg-primary" />
                  {reviewCount} 项任务等待评审与反馈
                </li>
                <li className="flex gap-2">
                  <span className="mt-1.5 size-1 shrink-0 rounded-full bg-primary" />
                  {nextTasks.length} 项后续待办需要安排
                </li>
                <li className="flex gap-2">
                  <span className="mt-1.5 size-1 shrink-0 rounded-full bg-primary" />
                  最近关注 RAG、API 设计与飞书集成
                </li>
              </ul>
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
            <div className="flex flex-col px-2 pb-3">
              {recentNotes.map((note) => (
                <Link
                  key={note.title}
                  to="/app/notes"
                  className="dashboard-note-row flex h-14 w-full items-center gap-3 rounded-md px-2 text-left hover:bg-surface-subtle focus-visible:ring-2 focus-visible:ring-ring/30"
                >
                  <IconTile icon={FileText} tone={note.tone} />
                  <span className="min-w-0">
                    <span title={note.title} className="block truncate text-[13px] font-medium">
                      {note.title}
                    </span>
                    <span className="block truncate text-[11px] text-muted-foreground">
                      {note.meta}
                    </span>
                  </span>
                </Link>
              ))}
            </div>
          </WorkspaceCard>

          <WorkspaceCard
            title="即将开始"
            description="会议与日程安排"
            icon={CalendarDays}
            action={<ViewAllButton to="/app/tasks/upcoming" />}
          >
            <div className="flex flex-col px-2 pb-3">
              {upcoming.map((item) => (
                <div
                  key={item.title}
                  className="dashboard-note-row flex h-14 items-center gap-3 rounded-md px-2"
                >
                  <IconTile icon={Clock3} tone={item.tone} />
                  <div className="min-w-0">
                    <p title={item.title} className="truncate text-[13px] font-medium">
                      {item.title}
                    </p>
                    <p className="truncate text-[11px] text-muted-foreground">{item.meta}</p>
                  </div>
                </div>
              ))}
            </div>
          </WorkspaceCard>

          <WorkspaceCard
            title="后续待办"
            description={`${nextTasks.length} 项任务 · 按到期时间排列`}
            icon={ListTodo}
            action={<ViewAllButton to="/app/tasks/upcoming" />}
          >
            <div className="divide-y divide-border-subtle px-4 pb-3">
              {nextTasks.map((task) => (
                <Link
                  key={task.id}
                  to="/app/tasks/upcoming"
                  className="dashboard-next-task-row flex h-14 items-center gap-3 rounded-sm hover:bg-surface-subtle focus-visible:ring-2 focus-visible:ring-ring/30"
                >
                  <div className="min-w-0 flex-1">
                    <p title={task.title} className="truncate text-[13px]">
                      {task.title}
                    </p>
                    <p className="truncate text-[11px] text-muted-foreground">
                      {task.project} · {task.assignee}
                    </p>
                  </div>
                  <span className="shrink-0 text-[11px] text-muted-foreground">{task.dueAt}</span>
                </Link>
              ))}
            </div>
          </WorkspaceCard>

          <WorkspaceCard
            title="项目任务分布"
            description="各项目的任务量与完成情况"
            icon={ChartNoAxesCombined}
            action={<ViewAllButton to="/app/projects" />}
          >
            <div className="divide-y divide-border-subtle px-4 pb-3">
              {activeProjects.map((project) => {
                const tasks = demoTasks.filter((task) => task.project === project.name)
                const doneCount = tasks.filter((task) => completedTasks.includes(task.id)).length
                return (
                  <div key={project.name} className="grid h-[70px] content-center gap-2">
                    <div className="flex items-center justify-between gap-2 text-xs">
                      <p title={project.name} className="min-w-0 truncate font-medium">
                        {project.name}
                      </p>
                      <span className="shrink-0 text-muted-foreground">
                        {tasks.length} 项 · 已完成 {doneCount}
                      </span>
                    </div>
                    <Progress
                      value={tasks.length ? (doneCount / tasks.length) * 100 : 0}
                      indicatorClassName="bg-success"
                      aria-label={`${project.name}任务完成率`}
                    />
                  </div>
                )
              })}
            </div>
          </WorkspaceCard>
        </div>
      </PageContainer>
    </div>
  )
}
