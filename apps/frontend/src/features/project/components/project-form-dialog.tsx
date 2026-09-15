import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useCallback, useRef } from 'react'
import { Loader2 } from 'lucide-react'

import type { Project } from '@/features/project/types'
import { PROJECT_PRIORITY_LABELS, PROJECT_STATUS_LABELS } from '@/features/project/types'
import {
  projectFormSchema,
  type ProjectFormValues,
} from '@/features/project/schemas/project-schema'
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
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'
import { Textarea } from '@/shared/components/ui/textarea'

const selectClassName =
  'h-9 w-full rounded-md border bg-background px-3 text-sm text-foreground shadow-xs focus-visible:ring-2 focus-visible:ring-ring/30'

export function ProjectFormDialog({
  open,
  onOpenChange,
  project,
  submitting,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean, navigating?: boolean) => void
  project?: Project | null
  submitting: boolean
  onSubmit: (values: ProjectFormValues) => Promise<void>
}) {
  const allowNavigation = useRef(false)
  const setNavigationPermission = useCallback((allowed: boolean) => {
    allowNavigation.current = allowed
  }, [])
  const form = useForm<ProjectFormValues>({
    resolver: zodResolver(projectFormSchema),
    resetOptions: { keepDirtyValues: true },
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
    <DraftDialog
      open={open}
      onOpenChange={onOpenChange}
      dirty={form.formState.isDirty}
      busy={submitting || form.formState.isSubmitting}
      onDiscard={() => form.reset()}
      canNavigate={() => allowNavigation.current}
      onNavigationPermissionChange={setNavigationPermission}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{project ? '编辑项目' : '创建项目'}</DialogTitle>
          <DialogDescription>
            先给目标起个名字。创建后直接添加任务，日期和描述可以稍后完善。
          </DialogDescription>
        </DialogHeader>
        <form
          className="grid gap-4"
          onSubmit={(event) => {
            void form.handleSubmit(async (values) => {
              try {
                await onSubmit(values)
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
              <Label htmlFor="project-name">项目名称</Label>
              <Input
                id="project-name"
                maxLength={200}
                placeholder="例如：个人网站改版…"
                aria-invalid={Boolean(form.formState.errors.name)}
                {...form.register('name')}
              />
              {form.formState.errors.name ? (
                <p className="text-xs text-destructive">{form.formState.errors.name.message}</p>
              ) : null}
            </div>
            <div className="grid gap-1.5">
              <Label htmlFor="project-description">描述</Label>
              <Textarea id="project-description" rows={4} {...form.register('description')} />
              {form.formState.errors.description && (
                <p role="alert" className="text-xs text-destructive">
                  {form.formState.errors.description.message}
                </p>
              )}
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="grid gap-1.5">
                <Label htmlFor="project-status">状态</Label>
                <select
                  id="project-status"
                  className={selectClassName}
                  {...form.register('status')}
                >
                  {Object.entries(PROJECT_STATUS_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid gap-1.5">
                <Label htmlFor="project-priority">优先级</Label>
                <select
                  id="project-priority"
                  className={selectClassName}
                  {...form.register('priority')}
                >
                  {Object.entries(PROJECT_PRIORITY_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
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
                {form.formState.errors.dueDate ? (
                  <p className="text-xs text-destructive">
                    {form.formState.errors.dueDate.message}
                  </p>
                ) : null}
              </div>
            </div>
            {form.formState.errors.root ? (
              <p className="text-sm text-destructive">{form.formState.errors.root.message}</p>
            ) : null}
            <DialogFooter>
              <DialogClose asChild>
                <Button type="button" variant="outline" disabled={submitting}>
                  取消
                </Button>
              </DialogClose>
              <Button type="submit" disabled={submitting || form.formState.isSubmitting}>
                {submitting && <Loader2 className="size-4 animate-spin" aria-hidden="true" />}
                {submitting ? '正在保存…' : project ? '保存修改' : '创建并添加任务'}
              </Button>
            </DialogFooter>
          </fieldset>
        </form>
      </DialogContent>
    </DraftDialog>
  )
}
