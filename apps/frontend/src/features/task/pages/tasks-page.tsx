import { Link } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ListTodo, Plus } from 'lucide-react'
import { useState } from 'react'

import { useSession } from '@/features/auth/hooks/use-session'
import { listProjects } from '@/features/project/api/projects'
import { createTask, listTasks, updateTask, updateTaskStatus } from '@/features/task/api/tasks'
import { TaskFormDialog } from '@/features/task/components/task-form-dialog'
import { TaskTable } from '@/features/task/components/task-table'
import type { Task, TaskDueFilter } from '@/features/task/types'
import { toTaskTableItem } from '@/features/task/types'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { queryKeys } from '@/shared/api/query-keys'
import { PageContainer } from '@/shared/components/page-container'
import { PageHeader } from '@/shared/components/page-header'
import { QueryState } from '@/shared/components/query-state'
import { Button } from '@/shared/components/ui/button'

const views = [
  { value: 'today', label: '今天', to: '/app/tasks/today', due: 'TODAY' as TaskDueFilter },
  { value: 'upcoming', label: '即将到期', to: '/app/tasks/upcoming', due: 'UPCOMING' as TaskDueFilter },
  { value: 'all', label: '我的任务', to: '/app/tasks' },
  { value: 'completed', label: '已完成', to: '/app/tasks/completed', status: 'DONE' },
] as const

export function TasksPage({ activeView = 'all' }: { activeView?: (typeof views)[number]['value'] }) {
  const session = useSession()
  const workspaceId = session.data?.workspace.id
  const assignee = session.data?.user.name ?? '我'
  const queryClient = useQueryClient()
  const [createOpen, setCreateOpen] = useState(false)
  const [editing, setEditing] = useState<Task | null>(null)
  const view = views.find(item => item.value === activeView) ?? views[2]

  const query = useQuery({
    queryKey: queryKeys.task.list(workspaceId ?? '', { view: activeView }),
    enabled: Boolean(workspaceId),
    queryFn: () => listTasks({
      due: 'due' in view ? view.due : undefined,
      status: 'status' in view ? view.status : undefined,
      size: 100,
      sort: activeView === 'completed' ? 'updatedAt,desc' : 'dueAt,asc',
    }),
  })

  const projectsQuery = useQuery({
    queryKey: queryKeys.project.list(workspaceId ?? '', { archived: false }),
    enabled: Boolean(workspaceId),
    queryFn: () => listProjects({ archived: false, size: 100 }),
  })

  const createMutation = useMutation({
    mutationFn: createTask,
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, ...input }: Parameters<typeof updateTask>[1] & { id: string }) => updateTask(id, input),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const completeMutation = useMutation({
    mutationFn: ({ id, status, version }: { id: string, status: 'DONE' | 'TODO', version: number }) =>
      updateTaskStatus(id, status, version),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })

  const title = view.label

  return (
    <PageContainer width="wide" className="grid gap-5">
      <PageHeader
        eyebrow="工作台 / 任务"
        title="任务"
        description="集中查看、筛选和推进工作空间中的任务。"
        actions={(
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="size-4" />
            创建任务
          </Button>
        )}
      />

      <div className="flex flex-col gap-3 border-b border-border-subtle sm:flex-row sm:items-center sm:justify-between">
        <nav className="flex items-center gap-1 overflow-x-auto" aria-label="任务视图">
          {views.map(item => (
            <Link
              key={item.value}
              to={item.to}
              activeOptions={{ exact: true }}
              data-active={item.value === activeView}
              className="relative flex h-11 shrink-0 items-center px-3 text-[13px] text-muted-foreground transition-colors hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring/40 data-[active=true]:font-medium data-[active=true]:text-foreground data-[active=true]:after:absolute data-[active=true]:after:inset-x-2 data-[active=true]:after:bottom-0 data-[active=true]:after:h-0.5 data-[active=true]:after:rounded-full data-[active=true]:after:bg-primary"
            >
              {item.label}
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
        <QueryState
          query={query}
          isEmpty={data => data.items.length === 0}
          empty={{
            title: '还没有任务',
            description: '创建一条任务，或从项目详情里添加。',
            action: <Button onClick={() => setCreateOpen(true)}>创建任务</Button>,
          }}
        >
          {data => (
            <TaskTable
              key={activeView}
              data={data.items.map(task => toTaskTableItem(task, assignee))}
              onRowClick={item => setEditing(item.raw)}
              onToggleComplete={(item, completed) => completeMutation.mutate({
                id: item.raw.id,
                status: completed ? 'DONE' : 'TODO',
                version: item.raw.version,
              })}
            />
          )}
        </QueryState>
      </section>

      <TaskFormDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        projects={projectsQuery.data?.items ? [...projectsQuery.data.items] : []}
        submitting={createMutation.isPending}
        onSubmit={async (values) => {
          await createMutation.mutateAsync({
            title: values.title,
            description: values.description || null,
            projectId: values.projectId || null,
            status: values.status,
            priority: values.priority,
            dueAt: values.dueAt || null,
          })
        }}
      />
      <TaskFormDialog
        open={Boolean(editing)}
        onOpenChange={open => !open && setEditing(null)}
        task={editing}
        projects={projectsQuery.data?.items ? [...projectsQuery.data.items] : []}
        submitting={updateMutation.isPending}
        onSubmit={async (values) => {
          if (!editing) {
            return
          }
          await updateMutation.mutateAsync({
            id: editing.id,
            title: values.title,
            description: values.description || null,
            projectId: values.projectId || null,
            status: values.status,
            priority: values.priority,
            dueAt: values.dueAt || null,
            version: editing.version,
          })
        }}
      />
    </PageContainer>
  )
}
