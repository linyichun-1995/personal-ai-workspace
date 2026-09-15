import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'

import type { Project } from '@/features/project/types'
import { PROJECT_PRIORITY_LABELS, PROJECT_STATUS_LABELS } from '@/features/project/types'
import { projectFormSchema, type ProjectFormValues } from '@/features/project/schemas/project-schema'
import { applyApiFormError } from '@/shared/api/form-errors'
import { Button } from '@/shared/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/shared/components/ui/dialog'
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'
import { Textarea } from '@/shared/components/ui/textarea'

const selectClassName = 'h-9 w-full rounded-md border bg-background px-3 text-sm text-foreground shadow-xs focus-visible:ring-2 focus-visible:ring-ring/30'

export function ProjectFormDialog({
  open,
  onOpenChange,
  project,
  submitting,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  project?: Project | null
  submitting: boolean
  onSubmit: (values: ProjectFormValues) => Promise<void>
}) {
  const form = useForm<ProjectFormValues>({
    resolver: zodResolver(projectFormSchema),
    values: {
      name: project?.name ?? '',
      description: project?.description ?? '',
      status: project?.status ?? 'ACTIVE',
      priority: project?.priority ?? 'MEDIUM',
      startDate: project?.startDate ?? '',
      dueDate: project?.dueDate ?? '',
    },
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{project ? '编辑项目' : '创建项目'}</DialogTitle>
          <DialogDescription>设置名称、状态和计划时间，随后可以在项目里管理任务与笔记。</DialogDescription>
        </DialogHeader>
        <form
          className="grid gap-4"
          onSubmit={form.handleSubmit(async (values) => {
            try {
              await onSubmit(values)
              onOpenChange(false)
            }
            catch (error) {
              applyApiFormError(form.setError, error)
            }
          })}
        >
          <div className="grid gap-1.5">
            <Label htmlFor="project-name">项目名称</Label>
            <Input id="project-name" {...form.register('name')} />
            {form.formState.errors.name ? <p className="text-xs text-destructive">{form.formState.errors.name.message}</p> : null}
          </div>
          <div className="grid gap-1.5">
            <Label htmlFor="project-description">描述</Label>
            <Textarea id="project-description" rows={4} {...form.register('description')} />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="grid gap-1.5">
              <Label htmlFor="project-status">状态</Label>
              <select id="project-status" className={selectClassName} {...form.register('status')}>
                {Object.entries(PROJECT_STATUS_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </div>
            <div className="grid gap-1.5">
              <Label htmlFor="project-priority">优先级</Label>
              <select id="project-priority" className={selectClassName} {...form.register('priority')}>
                {Object.entries(PROJECT_PRIORITY_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </div>
            <div className="grid gap-1.5">
              <Label htmlFor="project-start">开始日期</Label>
              <Input id="project-start" type="date" {...form.register('startDate')} />
            </div>
            <div className="grid gap-1.5">
              <Label htmlFor="project-due">截止日期</Label>
              <Input id="project-due" type="date" {...form.register('dueDate')} />
              {form.formState.errors.dueDate ? <p className="text-xs text-destructive">{form.formState.errors.dueDate.message}</p> : null}
            </div>
          </div>
          {form.formState.errors.root ? <p className="text-sm text-destructive">{form.formState.errors.root.message}</p> : null}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>取消</Button>
            <Button type="submit" disabled={submitting}>{project ? '保存' : '创建项目'}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
