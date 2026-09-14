import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'

import { applyAuthFormError } from '@/features/auth/api/form-errors'
import { useSession } from '@/features/auth/hooks/use-session'
import type { Session } from '@/features/auth/types'
import { getWorkspace, updateWorkspace } from '@/features/workspace/api/workspaces'
import { isValidTimeZone, timezoneOptions, WEEK_START_OPTIONS } from '@/features/workspace/lib/timezones'
import { queryKeys } from '@/shared/api/query-keys'
import { QueryState } from '@/shared/components/query-state'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'

const workspaceSchema = z.object({
  name: z.string().trim().min(1, '请输入工作空间名称').max(120, '名称不能超过 120 个字符'),
  timezone: z.string().min(1, '请选择时区').refine(isValidTimeZone, '时区无效'),
  weekStartsOn: z.enum(['1', '2', '3', '4', '5', '6', '7']),
})

type WorkspaceFormValues = z.infer<typeof workspaceSchema>

const selectClassName = 'h-9 w-full rounded-md border bg-background px-3 text-sm text-foreground shadow-xs focus-visible:ring-2 focus-visible:ring-ring/30'

export function WorkspaceSettingsPage() {
  const sessionQuery = useSession()
  const workspaceId = sessionQuery.data?.workspace.id
  const query = useQuery({
    queryKey: queryKeys.workspace.detail(workspaceId ?? ''),
    queryFn: () => getWorkspace(workspaceId!),
    enabled: Boolean(workspaceId),
  })

  if (sessionQuery.isPending || !workspaceId) {
    return (
      <QueryState query={sessionQuery}>
        {() => null}
      </QueryState>
    )
  }

  return (
    <QueryState query={query}>
      {workspace => (
        <WorkspaceForm
          key={`${workspace.id}-${workspace.version}`}
          workspace={workspace}
        />
      )}
    </QueryState>
  )
}

function WorkspaceForm({ workspace }: { workspace: Awaited<ReturnType<typeof getWorkspace>> }) {
  const queryClient = useQueryClient()
  const form = useForm<WorkspaceFormValues>({
    resolver: zodResolver(workspaceSchema),
    defaultValues: {
      name: workspace.name,
      timezone: workspace.timezone,
      weekStartsOn: String(workspace.weekStartsOn) as WorkspaceFormValues['weekStartsOn'],
    },
  })
  const mutation = useMutation({
    mutationFn: (values: WorkspaceFormValues) => updateWorkspace(workspace.id, {
      name: values.name,
      timezone: values.timezone,
      weekStartsOn: Number(values.weekStartsOn),
      version: workspace.version,
    }),
    onSuccess: (updated) => {
      toast.success('工作空间设置已保存')
      queryClient.setQueryData(queryKeys.workspace.detail(updated.id), updated)
      queryClient.setQueryData(queryKeys.auth.session(), (current: Session | null | undefined) => (
        current
          ? { ...current, workspace: { ...current.workspace, name: updated.name } }
          : current
      ))
    },
    onError: error => applyAuthFormError(form.setError, error),
  })

  useEffect(() => {
    form.reset({
      name: workspace.name,
      timezone: workspace.timezone,
      weekStartsOn: String(workspace.weekStartsOn) as WorkspaceFormValues['weekStartsOn'],
    })
  }, [form, workspace])

  const readOnly = workspace.role !== 'OWNER'
  const timezones = timezoneOptions(workspace.timezone)

  return (
    <form className="grid max-w-xl gap-5" onSubmit={form.handleSubmit(values => mutation.mutate(values))} noValidate>
      <div>
        <h2 className="text-base font-semibold">工作空间</h2>
        <p className="mt-1 text-xs text-muted-foreground">名称、时区和一周起始日会影响后续任务的“今日”计算。</p>
      </div>
      <div className="grid gap-2">
        <Label htmlFor="workspace-name">名称</Label>
        <Input id="workspace-name" maxLength={120} disabled={readOnly} {...form.register('name')} />
        {form.formState.errors.name ? <p className="text-xs text-destructive">{form.formState.errors.name.message}</p> : null}
      </div>
      <div className="grid gap-2">
        <Label htmlFor="workspace-timezone">时区</Label>
        <select id="workspace-timezone" className={selectClassName} disabled={readOnly} {...form.register('timezone')}>
          {timezones.map(zone => (
            <option key={zone} value={zone}>{zone}</option>
          ))}
        </select>
        {form.formState.errors.timezone ? <p className="text-xs text-destructive">{form.formState.errors.timezone.message}</p> : null}
      </div>
      <div className="grid gap-2">
        <Label htmlFor="weekStartsOn">一周从哪天开始</Label>
        <select id="weekStartsOn" className={selectClassName} disabled={readOnly} {...form.register('weekStartsOn')}>
          {WEEK_START_OPTIONS.map(option => (
            <option key={option.value} value={String(option.value)}>{option.label}</option>
          ))}
        </select>
      </div>
      {readOnly ? <p className="text-xs text-muted-foreground">只有所有者可以修改工作空间设置。</p> : null}
      {form.formState.errors.root?.message ? <p className="text-xs text-destructive" role="alert">{form.formState.errors.root.message}</p> : null}
      <div>
        <Button type="submit" disabled={mutation.isPending || readOnly}>{mutation.isPending ? '保存中…' : '保存设置'}</Button>
      </div>
    </form>
  )
}
