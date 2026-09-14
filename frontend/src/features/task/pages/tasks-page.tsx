import { Link } from '@tanstack/react-router'
import { ListTodo } from 'lucide-react'

import { TaskTable } from '@/features/task/components/task-table'
import { demoTasks } from '@/features/task/data/demo-tasks'
import { PageContainer } from '@/shared/components/page-container'
import { PageHeader } from '@/shared/components/page-header'

const views = [
  { value: 'today', label: '今天', to: '/app/tasks/today' },
  { value: 'upcoming', label: '即将到期', to: '/app/tasks/upcoming' },
  { value: 'all', label: '我的任务', to: '/app/tasks' },
  { value: 'completed', label: '已完成', to: '/app/tasks/completed' },
] as const

export function TasksPage({ activeView = 'all' }: { activeView?: (typeof views)[number]['value'] }) {
  const tasks = demoTasks.filter((task) => {
    if (activeView === 'today')
      return task.dueAt.startsWith('今天') && task.status !== 'done'
    if (activeView === 'upcoming')
      return !task.dueAt.startsWith('今天') && task.status !== 'done'
    if (activeView === 'completed')
      return task.status === 'done'
    return true
  })
  const title = views.find(view => view.value === activeView)?.label ?? '我的任务'
  return (
    <PageContainer width="wide" className="grid gap-5">
      <PageHeader
        eyebrow="工作台 / 任务"
        title="任务"
        description="集中查看、筛选和推进工作空间中的任务。"
      />

      <div className="flex flex-col gap-3 border-b border-border-subtle sm:flex-row sm:items-center sm:justify-between">
        <nav className="flex items-center gap-1 overflow-x-auto" aria-label="任务视图">
          {views.map(view => (
            <Link
              key={view.value}
              to={view.to}
              activeOptions={{ exact: true }}
              data-active={view.value === activeView}
              className="relative flex h-11 shrink-0 items-center px-3 text-[13px] text-muted-foreground transition-colors hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring/40 data-[active=true]:font-medium data-[active=true]:text-foreground data-[active=true]:after:absolute data-[active=true]:after:inset-x-2 data-[active=true]:after:bottom-0 data-[active=true]:after:h-0.5 data-[active=true]:after:rounded-full data-[active=true]:after:bg-primary"
            >
              {view.label}
            </Link>
          ))}
        </nav>
      </div>

      <section aria-labelledby="task-list-heading">
        <div className="mb-3 flex items-center gap-2">
          <span className="grid size-7 place-items-center rounded-md bg-primary-subtle text-primary">
            <ListTodo className="size-4" />
          </span>
          <div>
            <h2 id="task-list-heading" className="text-sm font-semibold">{title}</h2>
            <p className="text-xs text-muted-foreground">支持排序、搜索、分页、选择和列显隐</p>
          </div>
        </div>
        <TaskTable key={activeView} data={tasks} />
      </section>
    </PageContainer>
  )
}
