import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'

import type { Project } from '@/features/project/types'
import type { Task } from '@/features/task/types'
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS } from '@/features/task/types'
import { taskFormSchema, type TaskFormValues } from '@/features/task/schemas/task-schema'
import { applyApiFormError } from '@/shared/api/form-errors'
import { Button } from '@/shared/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/shared/components/ui/dialog'
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'
import { Textarea } from '@/shared/components/ui/textarea'
import { fromDateTimeLocal, toDateTimeLocal } from '@/shared/lib/datetime'

const selectClassName = 'h-9 w-full rounded-md border bg-background px-3 text-sm text-foreground shadow-xs focus-visible:ring-2 focus-visible:ring-ring/30'

export function TaskFormDialog({
  open,
  onOpenChange,
  task,
  projects,
  defaultProjectId,
  submitting,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  task?: Task | null
  projects: Project[]
  defaultProjectId?: string
  submitting: boolean
  onSubmit: (values: TaskFormValues) => Promise<void>
}) {
  const form = useForm<TaskFormValues>({
    resolver: zodResolver(taskFormSchema),
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
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{task ? '编辑任务' : '创建任务'}</DialogTitle>
          <DialogDescription>设置标题、优先级和截止时间，然后按状态推进。</DialogDescription>
        </DialogHeader>
        <form
          className="grid gap-4"
          onSubmit={form.handleSubmit(async (values) => {
            try {
              await onSubmit({ ...values, dueAt: fromDateTimeLocal(values.dueAt ?? '') ?? '' })
              onOpenChange(false)
            }
            catch (error) {
              applyApiFormError(form.setError, error)
            }
          })}
        >
          <div className="grid gap-1.5">
            <Label htmlFor="task-title">任务标题</Label>
            <Input id="task-title" {...form.register('title')} />
            {form.formState.errors.title ? <p className="text-xs text-destructive">{form.formState.errors.title.message}</p> : null}
          </div>
          <div className="grid gap-1.5">
            <Label htmlFor="task-description">描述</Label>
            <Textarea id="task-description" rows={3} {...form.register('description')} />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="grid gap-1.5">
              <Label htmlFor="task-project">所属项目</Label>
              <select id="task-project" className={selectClassName} {...form.register('projectId')}>
                <option value="">未关联项目</option>
                {projects.map(project => (
                  <option key={project.id} value={project.id}>{project.name}</option>
                ))}
              </select>
            </div>
            <div className="grid gap-1.5">
              <Label htmlFor="task-status">状态</Label>
              <select id="task-status" className={selectClassName} {...form.register('status')}>
                {Object.entries(TASK_STATUS_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </div>
            <div className="grid gap-1.5">
              <Label htmlFor="task-priority">优先级</Label>
              <select id="task-priority" className={selectClassName} {...form.register('priority')}>
                {Object.entries(TASK_PRIORITY_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </div>
            <div className="grid gap-1.5">
              <Label htmlFor="task-due">截止时间</Label>
              <Input id="task-due" type="datetime-local" {...form.register('dueAt')} />
            </div>
          </div>
          {form.formState.errors.root ? <p className="text-sm text-destructive">{form.formState.errors.root.message}</p> : null}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>取消</Button>
            <Button type="submit" disabled={submitting}>{task ? '保存' : '创建任务'}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
