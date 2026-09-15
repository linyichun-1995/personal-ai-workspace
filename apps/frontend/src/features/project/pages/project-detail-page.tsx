import { Link, getRouteApi, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Archive, NotebookPen, Plus, RotateCcw, Trash2 } from 'lucide-react'
import { useState } from 'react'

import { useSession } from '@/features/auth/hooks/use-session'
import { archiveProject, deleteProject, getProject, restoreProject, updateProject } from '@/features/project/api/projects'
import { ProjectFormDialog } from '@/features/project/components/project-form-dialog'
import { PROJECT_PRIORITY_LABELS, PROJECT_STATUS_LABELS } from '@/features/project/types'
import { createNote, listNotes } from '@/features/note/api/notes'
import { createTask, listTasks, updateTask, updateTaskStatus } from '@/features/task/api/tasks'
import { TaskFormDialog } from '@/features/task/components/task-form-dialog'
import { TaskTable } from '@/features/task/components/task-table'
import type { Task } from '@/features/task/types'
import { toTaskTableItem } from '@/features/task/types'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { queryKeys } from '@/shared/api/query-keys'
import { PageContainer } from '@/shared/components/page-container'
import { PageHeader } from '@/shared/components/page-header'
import { QueryState } from '@/shared/components/query-state'
import { StatusBadge } from '@/shared/components/status-badge'
import {
  AlertDialog,
  AlertDialogAction,
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
  { id: 'overview', label: 'Overview' },
  { id: 'tasks', label: 'Tasks' },
  { id: 'notes', label: 'Notes' },
] as const

export function ProjectDetailPage() {
  const { projectId } = routeApi.useParams()
  const session = useSession()
  const workspaceId = session.data?.workspace.id
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [tab, setTab] = useState<(typeof tabs)[number]['id']>('overview')
  const [editOpen, setEditOpen] = useState(false)
  const [taskOpen, setTaskOpen] = useState(false)
  const [editingTask, setEditingTask] = useState<Task | null>(null)
  const [deleteOpen, setDeleteOpen] = useState(false)

  const query = useQuery({
    queryKey: queryKeys.project.detail(workspaceId ?? '', projectId),
    enabled: Boolean(workspaceId),
    queryFn: () => getProject(projectId),
  })
  const tasksQuery = useQuery({
    queryKey: queryKeys.task.list(workspaceId ?? '', { projectId }),
    enabled: Boolean(workspaceId),
    queryFn: () => listTasks({ projectId, size: 100 }),
  })
  const notesQuery = useQuery({
    queryKey: queryKeys.note.list(workspaceId ?? '', { projectId }),
    enabled: Boolean(workspaceId),
    queryFn: () => listNotes({ projectId, size: 50 }),
  })

  const updateMutation = useMutation({
    mutationFn: ({ projectId, ...input }: Parameters<typeof updateProject>[1] & { projectId: string }) =>
      updateProject(projectId, input),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const archiveMutation = useMutation({
    mutationFn: ({ projectId, version }: { projectId: string, version: number }) => archiveProject(projectId, version),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const restoreMutation = useMutation({
    mutationFn: ({ projectId, version }: { projectId: string, version: number }) => restoreProject(projectId, version),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const deleteMutation = useMutation({
    mutationFn: deleteProject,
    onSuccess: async () => {
      if (workspaceId) {
        invalidateWorkspaceData(queryClient, workspaceId)
      }
      await navigate({ to: '/app/projects' })
    },
  })
  const createTaskMutation = useMutation({
    mutationFn: createTask,
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const updateTaskMutation = useMutation({
    mutationFn: ({ id, ...input }: Parameters<typeof updateTask>[1] & { id: string }) => updateTask(id, input),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const completeTaskMutation = useMutation({
    mutationFn: ({ id, status, version }: { id: string, status: 'DONE' | 'TODO', version: number }) =>
      updateTaskStatus(id, status, version),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const createNoteMutation = useMutation({
    mutationFn: createNote,
    onSuccess: async (note) => {
      if (workspaceId) {
        invalidateWorkspaceData(queryClient, workspaceId)
      }
      await navigate({ to: '/app/notes/$noteId', params: { noteId: note.id } })
    },
  })

  return (
    <PageContainer className="grid gap-5">
      <QueryState query={query}>
        {(project) => {
          const archived = Boolean(project.archivedAt)
          return (
            <>
              <PageHeader
                eyebrow="工作台 / 项目"
                title={project.name}
                description={project.description || '暂无描述'}
                actions={(
                  <div className="flex flex-wrap gap-2">
                    {!archived ? <Button variant="outline" onClick={() => setEditOpen(true)}>编辑</Button> : null}
                    {archived
                      ? (
                          <Button variant="outline" onClick={() => restoreMutation.mutate({ projectId: project.id, version: project.version })}>
                            <RotateCcw className="size-4" />
                            恢复项目
                          </Button>
                        )
                      : (
                          <Button variant="outline" onClick={() => archiveMutation.mutate({ projectId: project.id, version: project.version })}>
                            <Archive className="size-4" />
                            归档
                          </Button>
                        )}
                    <Button variant="destructive" onClick={() => setDeleteOpen(true)}>
                      <Trash2 className="size-4" />
                      删除
                    </Button>
                  </div>
                )}
              />

              <nav className="flex items-center gap-1 border-b border-border-subtle" aria-label="项目分区">
                {tabs.map(item => (
                  <button
                    key={item.id}
                    type="button"
                    data-active={tab === item.id}
                    onClick={() => setTab(item.id)}
                    className="relative flex h-11 shrink-0 items-center px-3 text-[13px] text-muted-foreground transition-colors hover:text-foreground data-[active=true]:font-medium data-[active=true]:text-foreground data-[active=true]:after:absolute data-[active=true]:after:inset-x-2 data-[active=true]:after:bottom-0 data-[active=true]:after:h-0.5 data-[active=true]:after:rounded-full data-[active=true]:after:bg-primary"
                  >
                    {item.label}
                  </button>
                ))}
              </nav>

              {tab === 'overview'
                ? (
                    <div className="grid gap-4">
                      <dl className="grid gap-3 rounded-lg border border-border-subtle bg-card p-4 sm:grid-cols-2 lg:grid-cols-3">
                        <div><dt className="text-xs text-muted-foreground">状态</dt><dd className="mt-1"><StatusBadge>{PROJECT_STATUS_LABELS[project.status]}</StatusBadge></dd></div>
                        <div><dt className="text-xs text-muted-foreground">优先级</dt><dd className="mt-1"><StatusBadge>{PROJECT_PRIORITY_LABELS[project.priority]}</StatusBadge></dd></div>
                        <div><dt className="text-xs text-muted-foreground">开始时间</dt><dd className="mt-1 text-sm">{formatDate(project.startDate)}</dd></div>
                        <div><dt className="text-xs text-muted-foreground">截止时间</dt><dd className="mt-1 text-sm">{formatDate(project.dueDate)}</dd></div>
                        <div><dt className="text-xs text-muted-foreground">任务总数</dt><dd className="mt-1 text-sm">{project.stats.taskCount}</dd></div>
                        <div><dt className="text-xs text-muted-foreground">已完成任务</dt><dd className="mt-1 text-sm">{project.stats.completedTaskCount}</dd></div>
                        <div><dt className="text-xs text-muted-foreground">进行中任务</dt><dd className="mt-1 text-sm">{project.stats.inProgressTaskCount}</dd></div>
                        <div><dt className="text-xs text-muted-foreground">笔记数量</dt><dd className="mt-1 text-sm">{project.stats.noteCount}</dd></div>
                      </dl>
                      {archived ? <p className="text-sm text-muted-foreground">项目已归档，内容只读。恢复后可继续编辑。</p> : null}
                    </div>
                  )
                : null}

              {tab === 'tasks'
                ? (
                    <div className="grid gap-3">
                      {!archived
                        ? (
                            <div className="flex justify-end">
                              <Button onClick={() => setTaskOpen(true)}><Plus className="size-4" />创建任务</Button>
                            </div>
                          )
                        : null}
                      <QueryState
                        query={tasksQuery}
                        isEmpty={data => data.items.length === 0}
                        empty={{ title: '这个项目还没有任务', description: '创建任务后会显示在这里。' }}
                      >
                        {data => (
                          <TaskTable
                            data={data.items.map(task => toTaskTableItem(task, session.data?.user.name ?? '我'))}
                            onRowClick={item => setEditingTask(item.raw)}
                            onToggleComplete={(item, completed) => completeTaskMutation.mutate({
                              id: item.raw.id,
                              status: completed ? 'DONE' : 'TODO',
                              version: item.raw.version,
                            })}
                          />
                        )}
                      </QueryState>
                    </div>
                  )
                : null}

              {tab === 'notes'
                ? (
                    <div className="grid gap-3">
                      {!archived
                        ? (
                            <div className="flex justify-end">
                              <Button onClick={() => createNoteMutation.mutate({ projectId, title: `${project.name} 笔记` })}>
                                <NotebookPen className="size-4" />
                                写笔记
                              </Button>
                            </div>
                          )
                        : null}
                      <QueryState
                        query={notesQuery}
                        isEmpty={data => data.items.length === 0}
                        empty={{ title: '这个项目还没有笔记', description: '记录背景、方案或会议内容。' }}
                      >
                        {data => (
                          <div className="grid gap-2">
                            {data.items.map(note => (
                              <Link
                                key={note.id}
                                to="/app/notes/$noteId"
                                params={{ noteId: note.id }}
                                className="rounded-lg border border-border-subtle bg-card px-4 py-3 hover:bg-card-hover"
                              >
                                <p className="font-medium">{note.title}</p>
                                <p className="truncate text-xs text-muted-foreground">{note.summary || '暂无摘要'}</p>
                              </Link>
                            ))}
                          </div>
                        )}
                      </QueryState>
                    </div>
                  )
                : null}

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
                onOpenChange={setTaskOpen}
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
                }}
              />
              <TaskFormDialog
                open={Boolean(editingTask)}
                onOpenChange={open => !open && setEditingTask(null)}
                task={editingTask}
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
                  })
                }}
              />
              <AlertDialog open={deleteOpen} onOpenChange={setDeleteOpen}>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>删除项目？</AlertDialogTitle>
                    <AlertDialogDescription>
                      将同时删除这个项目下的任务和笔记。更稳妥的方式是先归档。
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>取消</AlertDialogCancel>
                    <AlertDialogAction onClick={() => deleteMutation.mutate(project.id)}>确认删除</AlertDialogAction>
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
