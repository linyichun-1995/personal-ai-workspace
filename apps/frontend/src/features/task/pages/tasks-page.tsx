import { Link, useNavigate, useSearch } from '@tanstack/react-router'
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'

import { useSession } from '@/features/auth/hooks/use-session'
import { listProjects } from '@/features/project/api/projects'
import {
  createTask,
  deleteTask,
  getTask,
  listTasks,
  updateTask,
  updateTaskStatus,
} from '@/features/task/api/tasks'
import { TaskFormDialog } from '@/features/task/components/task-form-dialog'
import { TaskTable } from '@/features/task/components/task-table'
import type { TaskDueFilter, TaskStatus } from '@/features/task/types'
import { toTaskTableItem } from '@/features/task/types'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { queryKeys } from '@/shared/api/query-keys'
import { PageContainer } from '@/shared/components/page-container'
import { PageHeader } from '@/shared/components/page-header'
import { QueryState } from '@/shared/components/query-state'
import { Button } from '@/shared/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/shared/components/ui/dialog'

const views = [
  {
    value: 'all',
    label: '全部任务',
    to: '/app/tasks',
    empty: '还没有任务',
    description: '把下一步要做的事写下来，也可以稍后关联项目。',
  },
  {
    value: 'today',
    label: '今天',
    to: '/app/tasks/today',
    due: 'TODAY' as TaskDueFilter,
    empty: '今天没有到期任务',
    description: '可以新建待办，或在全部任务里安排截止时间。',
  },
  {
    value: 'overdue',
    label: '已逾期',
    to: '/app/tasks/overdue',
    due: 'OVERDUE' as TaskDueFilter,
    empty: '没有逾期任务',
    description: '当前安排都在掌控中，继续推进下一项工作。',
  },
  {
    value: 'upcoming',
    label: '即将到期',
    to: '/app/tasks/upcoming',
    due: 'UPCOMING' as TaskDueFilter,
    empty: '近期没有到期任务',
    description: '给任务设置截止时间，就能在这里提前安排。',
  },
  {
    value: 'completed',
    label: '已完成',
    to: '/app/tasks/completed',
    status: 'DONE',
    empty: '还没有已完成任务',
    description: '完成一项任务后，可以在这里回顾，或重新打开。',
  },
] as const

export function TasksPage({
  activeView = 'all',
}: {
  activeView?: (typeof views)[number]['value']
}) {
  const { data: session } = useSession()
  const workspaceId = session?.workspace.id
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const search = useSearch({ strict: false })
  const [localCreateOpen, setLocalCreateOpen] = useState(false)
  const view = views.find((item) => item.value === activeView) ?? views[0]
  const editingId = search.taskId
  const invalidate = () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId)
  const query = useInfiniteQuery({
    queryKey: queryKeys.task.list(workspaceId ?? '', { view: activeView, mode: 'infinite' }),
    enabled: Boolean(workspaceId),
    initialPageParam: 1,
    queryFn: ({ pageParam }) =>
      listTasks({
        due: 'due' in view ? view.due : undefined,
        status: 'status' in view ? view.status : undefined,
        page: pageParam,
        size: 100,
        sort: activeView === 'completed' ? 'updatedAt,desc' : 'dueAt,asc',
      }),
    getNextPageParam: (page) => (page.page < page.totalPages ? page.page + 1 : undefined),
  })
  const detail = useQuery({
    queryKey: queryKeys.task.detail(workspaceId ?? '', editingId ?? ''),
    enabled: Boolean(workspaceId && editingId),
    queryFn: () => getTask(editingId!),
  })
  const projects = useQuery({
    queryKey: queryKeys.project.list(workspaceId ?? '', { archived: false, size: 100 }),
    enabled: Boolean(workspaceId && (localCreateOpen || search.create || editingId)),
    queryFn: () => listProjects({ archived: false, size: 100 }),
  })
  const create = useMutation({
    mutationFn: createTask,
    onSuccess: (task) => {
      invalidate()
      toast.success('任务已创建', {
        action: {
          label: '查看任务',
          onClick: () => void navigate({ to: view.to, search: { taskId: task.id } }),
        },
      })
    },
  })
  const update = useMutation({
    mutationFn: ({ id, ...input }: Parameters<typeof updateTask>[1] & { id: string }) =>
      updateTask(id, input),
    onSuccess: () => {
      invalidate()
      toast.success('任务已保存')
    },
  })
  const remove = useMutation({
    mutationFn: deleteTask,
    onSuccess: () => {
      invalidate()
      toast.success('任务已删除')
    },
  })
  const status = useMutation({
    mutationFn: ({
      id,
      status: nextStatus,
      version,
    }: {
      id: string
      status: TaskStatus
      version: number
    }) => updateTaskStatus(id, nextStatus, version),
    onSuccess: invalidate,
  })
  function closeEditor() {
    void navigate({ to: view.to, search: {}, replace: true })
  }
  function setCreateOpen(open: boolean, navigating = false) {
    setLocalCreateOpen(open)
    if (!open && search.create && !navigating)
      void navigate({ to: view.to, search: {}, replace: true })
  }
  return (
    <PageContainer className="grid gap-5">
      <PageHeader
        title="任务"
        description="写下下一步，设置截止时间，完成后勾选。点击标题可查看详情。"
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus />
            新建任务
          </Button>
        }
      />
      <nav
        className="flex gap-1 overflow-x-auto border-b border-border-subtle"
        aria-label="任务视图"
      >
        {views.map((item) => (
          <Link
            key={item.value}
            to={item.to}
            aria-current={activeView === item.value ? 'page' : undefined}
            className={
              'shrink-0 border-b-2 px-3 py-3 text-sm focus-visible:ring-2 focus-visible:ring-ring/40 ' +
              (activeView === item.value
                ? 'border-primary font-medium text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground')
            }
          >
            {item.label}
          </Link>
        ))}
      </nav>
      <QueryState
        query={query}
        isEmpty={(data) => data.pages[0]?.total === 0}
        empty={{
          title: view.empty,
          description: view.description,
          action:
            activeView === 'all' || activeView === 'today' ? (
              <Button onClick={() => setCreateOpen(true)}>新建任务</Button>
            ) : (
              <Button variant="outline" asChild>
                <Link to="/app/tasks">查看全部任务</Link>
              </Button>
            ),
        }}
      >
        {(data) => (
          <div className="grid gap-3">
            <TaskTable
              key={activeView}
              data={data.pages
                .flatMap((page) => page.items)
                .map((task) => toTaskTableItem(task, session?.user.name ?? '我'))}
              busy={status.isPending}
              onRowClick={(item) => {
                if (workspaceId)
                  queryClient.setQueryData(queryKeys.task.detail(workspaceId, item.id), item.raw)
                void navigate({ to: view.to, search: { taskId: item.id } })
              }}
              onToggleComplete={(item, completed) =>
                status.mutate({
                  id: item.id,
                  status: completed ? 'DONE' : 'TODO',
                  version: item.raw.version,
                })
              }
              onStatusChange={(item, nextStatus) =>
                status.mutate({ id: item.id, status: nextStatus, version: item.raw.version })
              }
            />
            {query.hasNextPage && (
              <Button
                variant="outline"
                disabled={query.isFetchingNextPage}
                onClick={() => void query.fetchNextPage()}
              >
                {query.isFetchingNextPage
                  ? '正在加载…'
                  : '加载更多任务（共 ' + (data.pages[0]?.total ?? 0) + ' 项）'}
              </Button>
            )}
          </div>
        )}
      </QueryState>
      <TaskFormDialog
        open={localCreateOpen || Boolean(search.create)}
        onOpenChange={setCreateOpen}
        projects={[...(projects.data?.items ?? [])]}
        projectsLoading={projects.isPending}
        submitting={create.isPending}
        onSubmit={async (values) => {
          await create.mutateAsync({
            ...values,
            description: values.description || null,
            projectId: values.projectId || null,
            dueAt: values.dueAt || null,
          })
        }}
      />
      {editingId &&
        (detail.data ? (
          <TaskFormDialog
            key={editingId}
            open
            onOpenChange={(open, navigating) => !open && !navigating && closeEditor()}
            task={detail.data}
            projects={[...(projects.data?.items ?? [])]}
            projectsLoading={projects.isPending}
            submitting={update.isPending}
            onDelete={async () => {
              await remove.mutateAsync(editingId)
            }}
            onSubmit={async (values) => {
              if (!detail.data) return
              await update.mutateAsync({
                ...values,
                id: editingId,
                description: values.description || null,
                projectId: values.projectId || null,
                dueAt: values.dueAt || null,
                startAt: detail.data.startAt,
                parentId: detail.data.parentId,
                version: detail.data.version,
              })
            }}
          />
        ) : (
          <Dialog open onOpenChange={(open) => !open && closeEditor()}>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>任务详情</DialogTitle>
              </DialogHeader>
              <QueryState query={detail}>{() => null}</QueryState>
            </DialogContent>
          </Dialog>
        ))}
    </PageContainer>
  )
}
