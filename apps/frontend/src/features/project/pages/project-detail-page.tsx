import { OriginLink } from '@/features/search/components/origin-link'
import { FileLibrary } from '@/features/file/pages/files-page'
import { ObjectTags } from '@/features/tag/components/object-tags'
import { Link, getRouteApi, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Archive, ArrowLeft, NotebookPen, Plus, RotateCcw, Trash2 } from 'lucide-react'
import { useRef, useState } from 'react'

import { useSession } from '@/features/auth/hooks/use-session'
import {
  archiveProject,
  deleteProject,
  getProject,
  restoreProject,
  updateProject,
} from '@/features/project/api/projects'
import { ProjectFormDialog } from '@/features/project/components/project-form-dialog'
import { PROJECT_PRIORITY_LABELS, PROJECT_STATUS_LABELS } from '@/features/project/types'
import { createNote, listNotes } from '@/features/note/api/notes'
import {
  createTask,
  deleteTask,
  listTasks,
  updateTask,
  updateTaskStatus,
} from '@/features/task/api/tasks'
import { TaskFormDialog } from '@/features/task/components/task-form-dialog'
import { TaskTable } from '@/features/task/components/task-table'
import type { Task, TaskStatus } from '@/features/task/types'
import { toTaskTableItem } from '@/features/task/types'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { queryKeys } from '@/shared/api/query-keys'
import { ListPagination } from '@/shared/components/list-pagination'
import { PageContainer } from '@/shared/components/page-container'
import { PageHeader } from '@/shared/components/page-header'
import { QueryState } from '@/shared/components/query-state'
import { StatusBadge } from '@/shared/components/status-badge'
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/shared/components/ui/alert-dialog'
import { Button } from '@/shared/components/ui/button'
import { formatDate } from '@/shared/lib/datetime'

const routeApi = getRouteApi('/app/projects/$projectId')
const tabs = [
  { id: 'tasks', label: '任务' },
  { id: 'notes', label: '笔记' },
  { id: 'files', label: '文件' },
  { id: 'overview', label: '项目信息' },
] as const

export function ProjectDetailPage() {
  const { projectId } = routeApi.useParams()
  const session = useSession()
  const workspaceId = session.data?.workspace.id
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const search = routeApi.useSearch()
  const tab = search.tab ?? 'tasks'
  const taskPage = search.taskPage ?? 1
  const notePage = search.notePage ?? 1
  const listRoute =
    search.from?.view === 'active'
      ? '/app/projects/active'
      : search.from?.view === 'archived'
        ? '/app/projects/archived'
        : '/app/projects'
  const setPage = (kind: 'taskPage' | 'notePage', page: number) =>
    void navigate({
      to: '/app/projects/$projectId',
      params: { projectId },
      search: { ...search, [kind]: page > 1 ? page : undefined },
    })
  const setTab = (next: (typeof tabs)[number]['id']) =>
    void navigate({
      to: '/app/projects/$projectId',
      params: { projectId },
      search: { ...search, tab: next },
      replace: true,
    })
  const [editOpen, setEditOpen] = useState(false)
  const [taskOpen, setTaskOpen] = useState(false)
  const createdTask = useRef(false)
  const [editingTask, setEditingTask] = useState<Task | null>(null)
  const [deleteOpen, setDeleteOpen] = useState(false)

  const query = useQuery({
    queryKey: queryKeys.project.detail(workspaceId ?? '', projectId),
    enabled: Boolean(workspaceId),
    queryFn: () => getProject(projectId),
  })
  const tasksQuery = useQuery({
    queryKey: queryKeys.task.list(workspaceId ?? '', {
      projectId,
      page: taskPage,
      size: 20,
      sort: 'createdAt,desc',
    }),
    enabled: Boolean(workspaceId) && tab === 'tasks',
    queryFn: () => listTasks({ projectId, page: taskPage, size: 20, sort: 'createdAt,desc' }),
  })
  const notesQuery = useQuery({
    queryKey: queryKeys.note.list(workspaceId ?? '', { projectId, page: notePage, size: 20 }),
    enabled: Boolean(workspaceId) && tab === 'notes',
    queryFn: () => listNotes({ projectId, page: notePage, size: 20, sort: 'updatedAt,desc' }),
  })

  const updateMutation = useMutation({
    mutationFn: ({
      projectId,
      ...input
    }: Parameters<typeof updateProject>[1] & { projectId: string }) =>
      updateProject(projectId, input),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const archiveMutation = useMutation({
    mutationFn: ({ projectId, version }: { projectId: string; version: number }) =>
      archiveProject(projectId, version),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const restoreMutation = useMutation({
    mutationFn: ({ projectId, version }: { projectId: string; version: number }) =>
      restoreProject(projectId, version),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const deleteMutation = useMutation({
    mutationFn: deleteProject,
    onSuccess: async () => {
      if (workspaceId) {
        invalidateWorkspaceData(queryClient, workspaceId)
      }
      await navigate({ to: listRoute, search: { page: search.from?.page } })
    },
  })
  const createTaskMutation = useMutation({
    mutationFn: createTask,
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const updateTaskMutation = useMutation({
    mutationFn: ({ id, ...input }: Parameters<typeof updateTask>[1] & { id: string }) =>
      updateTask(id, input),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const deleteTaskMutation = useMutation({
    mutationFn: deleteTask,
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const completeTaskMutation = useMutation({
    mutationFn: ({ id, status, version }: { id: string; status: TaskStatus; version: number }) =>
      updateTaskStatus(id, status, version),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const createNoteMutation = useMutation({
    mutationFn: createNote,
    onSuccess: async (note) => {
      if (workspaceId) {
        invalidateWorkspaceData(queryClient, workspaceId)
      }
      await navigate({
        to: '/app/notes/$noteId',
        params: { noteId: note.id },
        search: { project: { from: search.from } },
      })
    },
  })

  return (
    <PageContainer className="grid gap-5">
      <Link
        to={listRoute}
        search={{ page: search.from?.page }}
        className="flex w-fit items-center gap-2 rounded-sm text-sm text-muted-foreground hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring/40"
      >
        <ArrowLeft className="size-4" />
        返回项目
      </Link>
      <QueryState query={query}>
        {(project) => {
          const archived = Boolean(project.archivedAt)
          return (
            <>
              <PageHeader
                eyebrow="工作台 / 项目"
                title={project.name}
                description={project.description || '拆解任务、推进进度，并把相关记录整理成笔记。'}
                actions={
                  <div className="flex flex-wrap gap-2">
                    {!archived ? (
                      <Button variant="outline" onClick={() => setEditOpen(true)}>
                        编辑
                      </Button>
                    ) : null}
                    {archived ? (
                      <Button
                        variant="outline"
                        disabled={restoreMutation.isPending}
                        onClick={() =>
                          restoreMutation.mutate({
                            projectId: project.id,
                            version: project.version,
                          })
                        }
                      >
                        <RotateCcw className="size-4" />
                        恢复项目
                      </Button>
                    ) : (
                      <Button
                        variant="outline"
                        disabled={archiveMutation.isPending}
                        onClick={() =>
                          archiveMutation.mutate({
                            projectId: project.id,
                            version: project.version,
                          })
                        }
                      >
                        <Archive className="size-4" />
                        归档
                      </Button>
                    )}
                    <Button
                      variant="ghost"
                      className="text-destructive"
                      onClick={() => setDeleteOpen(true)}
                    >
                      <Trash2 className="size-4" />
                      删除
                    </Button>
                  </div>
                }
              />

              {archived && (
                <p
                  role="status"
                  className="rounded-lg border bg-muted px-4 py-3 text-sm text-muted-foreground"
                >
                  项目已归档。恢复项目后可继续添加和修改内容。
                </p>
              )}
              <nav
                className="flex items-center gap-1 border-b border-border-subtle"
                aria-label="项目分区"
              >
                {tabs.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    data-active={tab === item.id}
                    aria-current={tab === item.id ? 'page' : undefined}
                    onClick={() => setTab(item.id)}
                    className="relative flex h-11 shrink-0 items-center px-3 text-[13px] text-muted-foreground transition-colors hover:text-foreground data-[active=true]:font-medium data-[active=true]:text-foreground data-[active=true]:after:absolute data-[active=true]:after:inset-x-2 data-[active=true]:after:bottom-0 data-[active=true]:after:h-0.5 data-[active=true]:after:rounded-full data-[active=true]:after:bg-primary"
                  >
                    {item.label}
                    {item.id === 'tasks'
                      ? ' (' + project.stats.taskCount + ')'
                      : item.id === 'notes'
                        ? ' (' + project.stats.noteCount + ')'
                        : ''}
                  </button>
                ))}
              </nav>

              {search.origin && <OriginLink origin={search.origin} />}
              <ObjectTags resource="projects" id={projectId} readOnly={archived} />
              {tab === 'files' && <FileLibrary projectId={projectId} readOnly={archived} />}
              {tab === 'overview' ? (
                <div className="grid gap-4">
                  <dl className="grid gap-3 rounded-lg border border-border-subtle bg-card p-4 sm:grid-cols-2 lg:grid-cols-3">
                    <div>
                      <dt className="text-xs text-muted-foreground">状态</dt>
                      <dd className="mt-1">
                        <StatusBadge>{PROJECT_STATUS_LABELS[project.status]}</StatusBadge>
                      </dd>
                    </div>
                    <div>
                      <dt className="text-xs text-muted-foreground">优先级</dt>
                      <dd className="mt-1">
                        <StatusBadge>{PROJECT_PRIORITY_LABELS[project.priority]}</StatusBadge>
                      </dd>
                    </div>
                    <div>
                      <dt className="text-xs text-muted-foreground">开始时间</dt>
                      <dd className="mt-1 text-sm">{formatDate(project.startDate)}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-muted-foreground">截止时间</dt>
                      <dd className="mt-1 text-sm">{formatDate(project.dueDate)}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-muted-foreground">任务总数</dt>
                      <dd className="mt-1 text-sm">{project.stats.taskCount}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-muted-foreground">已完成任务</dt>
                      <dd className="mt-1 text-sm">{project.stats.completedTaskCount}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-muted-foreground">进行中任务</dt>
                      <dd className="mt-1 text-sm">{project.stats.inProgressTaskCount}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-muted-foreground">笔记数量</dt>
                      <dd className="mt-1 text-sm">{project.stats.noteCount}</dd>
                    </div>
                  </dl>
                  {archived ? (
                    <p className="text-sm text-muted-foreground">
                      项目已归档，内容只读。恢复后可继续编辑。
                    </p>
                  ) : null}
                </div>
              ) : null}

              {tab === 'tasks' ? (
                <div className="grid gap-3">
                  {!archived ? (
                    <div className="flex justify-end">
                      <Button onClick={() => setTaskOpen(true)}>
                        <Plus className="size-4" />
                        创建任务
                      </Button>
                    </div>
                  ) : null}
                  <QueryState
                    query={tasksQuery}
                    isEmpty={(data) => data.items.length === 0}
                    empty={{
                      title:
                        taskPage > 1
                          ? '这一页没有任务'
                          : archived
                            ? '项目中还没有任务'
                            : '先写下这个项目的第一步',
                      description:
                        taskPage > 1
                          ? '返回第一页继续查看任务。'
                          : archived
                            ? '恢复项目后可以添加任务。'
                            : '例如：明确目标、收集资料，或安排一次讨论。',
                      action:
                        !archived && taskPage === 1 ? (
                          <Button onClick={() => setTaskOpen(true)}>
                            <Plus />
                            添加第一个任务
                          </Button>
                        ) : undefined,
                    }}
                  >
                    {(data) => (
                      <TaskTable
                        key={taskPage}
                        pagination={false}
                        searchPlaceholder="搜索本页任务或项目…"
                        data={data.items.map((task) =>
                          toTaskTableItem(task, session.data?.user.name ?? '我'),
                        )}
                        busy={completeTaskMutation.isPending}
                        onRowClick={archived ? undefined : (item) => setEditingTask(item.raw)}
                        onStatusChange={
                          archived
                            ? undefined
                            : (item, status) =>
                                completeTaskMutation.mutate({
                                  id: item.id,
                                  status,
                                  version: item.raw.version,
                                })
                        }
                        onToggleComplete={
                          archived
                            ? undefined
                            : (item, completed) =>
                                completeTaskMutation.mutate({
                                  id: item.raw.id,
                                  status: completed ? 'DONE' : 'TODO',
                                  version: item.raw.version,
                                })
                        }
                      />
                    )}
                  </QueryState>
                  {tasksQuery.data && (
                    <ListPagination
                      page={taskPage}
                      total={tasksQuery.data.total}
                      totalPages={tasksQuery.data.totalPages}
                      loading={tasksQuery.isFetching}
                      onPageChange={(page) => setPage('taskPage', page)}
                    />
                  )}
                </div>
              ) : null}

              {tab === 'notes' ? (
                <div className="grid gap-3">
                  {!archived ? (
                    <div className="flex justify-end">
                      <Button
                        disabled={createNoteMutation.isPending}
                        onClick={() =>
                          createNoteMutation.mutate({ projectId, title: `${project.name} 笔记` })
                        }
                      >
                        <NotebookPen className="size-4" />
                        写笔记
                      </Button>
                    </div>
                  ) : null}
                  <QueryState
                    query={notesQuery}
                    isEmpty={(data) => data.items.length === 0}
                    empty={{
                      title: notePage > 1 ? '这一页没有笔记' : '这个项目还没有笔记',
                      description:
                        notePage > 1 ? '返回第一页继续查看笔记。' : '记录背景、方案或会议内容。',
                    }}
                  >
                    {(data) => (
                      <div className="grid gap-2">
                        {data.items.map((note) => (
                          <Link
                            key={note.id}
                            to="/app/notes/$noteId"
                            params={{ noteId: note.id }}
                            search={{ project: { page: search.notePage, from: search.from } }}
                            className="rounded-lg border border-border-subtle bg-card px-4 py-3 hover:bg-card-hover"
                          >
                            <p className="font-medium">{note.title}</p>
                            <p className="truncate text-xs text-muted-foreground">
                              {note.summary || '暂无摘要'}
                            </p>
                          </Link>
                        ))}
                      </div>
                    )}
                  </QueryState>
                  {notesQuery.data && (
                    <ListPagination
                      page={notePage}
                      total={notesQuery.data.total}
                      totalPages={notesQuery.data.totalPages}
                      loading={notesQuery.isFetching}
                      onPageChange={(page) => setPage('notePage', page)}
                    />
                  )}
                </div>
              ) : null}

              <ProjectFormDialog
                open={editOpen}
                onOpenChange={setEditOpen}
                project={project}
                submitting={updateMutation.isPending}
                onSubmit={async (values) => {
                  await updateMutation.mutateAsync({
                    projectId: project.id,
                    name: values.name,
                    description: values.description || null,
                    status: values.status,
                    priority: values.priority,
                    startDate: values.startDate || null,
                    dueDate: values.dueDate || null,
                    version: project.version,
                  })
                }}
              />
              <TaskFormDialog
                open={taskOpen}
                onOpenChange={(open) => {
                  setTaskOpen(open)
                  if (!open && createdTask.current) {
                    createdTask.current = false
                    setPage('taskPage', 1)
                  }
                }}
                projects={[project]}
                defaultProjectId={project.id}
                submitting={createTaskMutation.isPending}
                onSubmit={async (values) => {
                  await createTaskMutation.mutateAsync({
                    title: values.title,
                    description: values.description || null,
                    projectId: project.id,
                    status: values.status,
                    priority: values.priority,
                    dueAt: values.dueAt || null,
                  })
                  createdTask.current = true
                }}
              />
              <TaskFormDialog
                open={Boolean(editingTask)}
                onOpenChange={(open) => !open && setEditingTask(null)}
                task={editingTask}
                onDelete={async () => {
                  if (editingTask) {
                    await deleteTaskMutation.mutateAsync(editingTask.id)
                  }
                }}
                projects={[project]}
                defaultProjectId={project.id}
                submitting={updateTaskMutation.isPending}
                onSubmit={async (values) => {
                  if (!editingTask) {
                    return
                  }
                  await updateTaskMutation.mutateAsync({
                    id: editingTask.id,
                    title: values.title,
                    description: values.description || null,
                    projectId: project.id,
                    status: values.status,
                    priority: values.priority,
                    dueAt: values.dueAt || null,
                    version: editingTask.version,
                    parentId: editingTask.parentId,
                    startAt: editingTask.startAt,
                  })
                }}
              />
              <AlertDialog
                open={deleteOpen}
                onOpenChange={(open) => !deleteMutation.isPending && setDeleteOpen(open)}
              >
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>删除项目？</AlertDialogTitle>
                    <AlertDialogDescription>
                      将同时删除这个项目下的任务和笔记。更稳妥的方式是先归档。
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel disabled={deleteMutation.isPending}>取消</AlertDialogCancel>
                    <Button
                      variant="destructive"
                      disabled={deleteMutation.isPending}
                      onClick={() => deleteMutation.mutate(project.id)}
                    >
                      {deleteMutation.isPending ? '正在删除…' : '删除项目及其内容'}
                    </Button>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            </>
          )
        }}
      </QueryState>
    </PageContainer>
  )
}
