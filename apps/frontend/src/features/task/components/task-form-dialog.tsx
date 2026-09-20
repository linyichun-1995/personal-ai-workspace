import { Attachments } from '@/features/file/components/attachments'
import { ObjectTags } from '@/features/tag/components/object-tags'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useCallback, useRef, useState } from 'react'
import { Loader2 } from 'lucide-react'

import type { Project } from '@/features/project/types'
import type { Task } from '@/features/task/types'
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS } from '@/features/task/types'
import { taskFormSchema, type TaskFormValues } from '@/features/task/schemas/task-schema'
import { applyApiFormError } from '@/shared/api/form-errors'
import { Button } from '@/shared/components/ui/button'
import { DraftDialog } from '@/shared/components/draft-dialog'
import {
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/shared/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/shared/components/ui/alert-dialog'
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'
import { Textarea } from '@/shared/components/ui/textarea'
import { fromDateTimeLocal, toDateTimeLocal } from '@/shared/lib/datetime'

const selectClassName =
  'h-9 w-full rounded-md border bg-background px-3 text-sm text-foreground shadow-xs focus-visible:ring-2 focus-visible:ring-ring/30'

export function TaskFormDialog({
  open,
  onOpenChange,
  task,
  projects,
  projectsLoading,
  defaultProjectId,
  submitting,
  onSubmit,
  onDelete,
}: {
  open: boolean
  onOpenChange: (open: boolean, navigating?: boolean) => void
  task?: Task | null
  projects: Project[]
  projectsLoading?: boolean
  defaultProjectId?: string
  submitting: boolean
  onSubmit: (values: TaskFormValues) => Promise<void>
  onDelete?: () => Promise<void>
}) {
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const allowNavigation = useRef(false)
  const setNavigationPermission = useCallback((allowed: boolean) => {
    allowNavigation.current = allowed
  }, [])
  const form = useForm<TaskFormValues>({
    resolver: zodResolver(taskFormSchema),
    resetOptions: { keepDirtyValues: true },
    values: {
      title: task?.title ?? '',
      description: task?.description ?? '',
      projectId: task?.projectId ?? defaultProjectId ?? '',
      status: task?.status ?? 'TODO',
      priority: task?.priority ?? 'MEDIUM',
      dueAt: toDateTimeLocal(task?.dueAt),
    },
  })

  return (
    <DraftDialog
      open={open}
      onOpenChange={onOpenChange}
      dirty={form.formState.isDirty}
      busy={submitting || form.formState.isSubmitting || deleting}
      onDiscard={() => form.reset()}
      canNavigate={() => allowNavigation.current}
      onNavigationPermissionChange={setNavigationPermission}
    >
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{task ? '编辑任务' : '创建任务'}</DialogTitle>
          <DialogDescription>
            {task
              ? '更新任务内容，或调整状态和截止时间。'
              : '先写清楚要做什么，其余信息可以稍后补充。'}
          </DialogDescription>
        </DialogHeader>
        <form
          className="grid gap-4"
          onSubmit={(event) => {
            void form.handleSubmit(async (values) => {
              try {
                await onSubmit({
                  ...values,
                  projectId: defaultProjectId ?? values.projectId ?? task?.projectId ?? '',
                  dueAt: fromDateTimeLocal(values.dueAt ?? '') ?? '',
                })
                allowNavigation.current = true
                form.reset()
                onOpenChange(false)
              } catch (error) {
                applyApiFormError(form.setError, error)
              }
            })(event)
          }}
        >
          <fieldset disabled={submitting || form.formState.isSubmitting} className="contents">
            <div className="grid gap-1.5">
              <Label htmlFor="task-title">任务标题</Label>
              <Input
                id="task-title"
                placeholder="例如：整理周五会议的行动项…"
                maxLength={300}
                aria-invalid={Boolean(form.formState.errors.title)}
                aria-describedby={form.formState.errors.title ? 'task-title-error' : undefined}
                {...form.register('title')}
              />
              {form.formState.errors.title ? (
                <p id="task-title-error" role="alert" className="text-xs text-destructive">
                  {form.formState.errors.title.message}
                </p>
              ) : null}
            </div>
            <div className="grid gap-1.5">
              <Label htmlFor="task-description">补充说明（选填）</Label>
              <Textarea id="task-description" rows={3} {...form.register('description')} />
              {form.formState.errors.description && (
                <p role="alert" className="text-xs text-destructive">
                  {form.formState.errors.description.message}
                </p>
              )}
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="grid gap-1.5">
                <Label htmlFor="task-project">所属项目</Label>
                <select
                  id="task-project"
                  className={selectClassName}
                  aria-busy={projectsLoading}
                  disabled={Boolean(defaultProjectId) || projectsLoading}
                  {...form.register('projectId')}
                >
                  <option value="">
                    {projectsLoading ? '正在加载项目…' : '独立任务（不关联项目）'}
                  </option>
                  {task?.projectId &&
                    !projects.some((project) => project.id === task.projectId) && (
                      <option value={task.projectId}>{task.projectName ?? '当前关联项目'}</option>
                    )}
                  {projects.map((project) => (
                    <option key={project.id} value={project.id}>
                      {project.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid gap-1.5">
                <Label htmlFor="task-status">状态</Label>
                <select id="task-status" className={selectClassName} {...form.register('status')}>
                  {Object.entries(TASK_STATUS_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid gap-1.5">
                <Label htmlFor="task-priority">优先级</Label>
                <select
                  id="task-priority"
                  className={selectClassName}
                  {...form.register('priority')}
                >
                  {Object.entries(TASK_PRIORITY_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid gap-1.5">
                <Label htmlFor="task-due">截止时间</Label>
                <Input id="task-due" type="datetime-local" {...form.register('dueAt')} />
              </div>
            </div>
            {form.formState.errors.root ? (
              <p className="text-sm text-destructive">{form.formState.errors.root.message}</p>
            ) : null}
            <DialogFooter>
              {task && onDelete && (
                <Button
                  type="button"
                  variant="ghost"
                  className="text-destructive sm:mr-auto"
                  disabled={submitting || deleting}
                  onClick={() => setDeleteOpen(true)}
                >
                  删除任务
                </Button>
              )}
              <DialogClose asChild>
                <Button type="button" variant="outline" disabled={submitting}>
                  取消
                </Button>
              </DialogClose>
              <Button type="submit" disabled={submitting || form.formState.isSubmitting}>
                {submitting && <Loader2 className="size-4 animate-spin" aria-hidden="true" />}
                {submitting ? '正在保存…' : task ? '保存修改' : '创建任务'}
              </Button>
            </DialogFooter>
          </fieldset>
        </form>
        {task && (
          <>
            <ObjectTags
              resource="tasks"
              id={task.id}
              readOnly={Boolean(
                task.projectId &&
                (!projects.find((p) => p.id === task.projectId) ||
                  projects.find((p) => p.id === task.projectId)?.archivedAt),
              )}
            />
            <Attachments
              target={{ resource: 'tasks', id: task.id }}
              projectId={task.projectId}
              readOnly={Boolean(
                task.projectId &&
                (!projects.find((p) => p.id === task.projectId) ||
                  projects.find((p) => p.id === task.projectId)?.archivedAt),
              )}
            />
          </>
        )}
      </DialogContent>
      <AlertDialog open={deleteOpen} onOpenChange={(value) => !deleting && setDeleteOpen(value)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>删除“{task?.title}”？</AlertDialogTitle>
            <AlertDialogDescription>任务及其子任务将从列表中移除。</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleting}>取消</AlertDialogCancel>
            <Button
              variant="destructive"
              disabled={deleting}
              onClick={async () => {
                setDeleting(true)
                try {
                  await onDelete?.()
                  allowNavigation.current = true
                  form.reset()
                  setDeleteOpen(false)
                  onOpenChange(false)
                } catch (error) {
                  applyApiFormError(form.setError, error)
                  setDeleteOpen(false)
                } finally {
                  setDeleting(false)
                }
              }}
            >
              {deleting ? '正在删除…' : '删除任务'}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </DraftDialog>
  )
}
