import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'

import { applyAuthFormError } from '@/features/auth/api/form-errors'
import { getCurrentUser, updateProfile } from '@/features/auth/api/session'
import type { Session } from '@/features/auth/types'
import { isValidTimeZone, LOCALE_OPTIONS, timezoneOptions } from '@/features/workspace/lib/timezones'
import { queryKeys } from '@/shared/api/query-keys'
import { QueryState } from '@/shared/components/query-state'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'

const profileSchema = z.object({
  displayName: z.string().trim().min(1, '请输入显示名称').max(100, '显示名称不能超过 100 个字符'),
  avatarUrl: z.string().trim().max(1000, '头像地址过长').refine(value => value.length === 0 || /^https?:\/\//.test(value), '头像地址必须是 http 或 https URL'),
  locale: z.string().min(1, '请选择语言'),
  timezone: z.string().min(1, '请选择时区').refine(isValidTimeZone, '时区无效'),
})

type ProfileFormValues = z.infer<typeof profileSchema>

const selectClassName = 'h-9 w-full rounded-md border bg-background px-3 text-sm text-foreground shadow-xs focus-visible:ring-2 focus-visible:ring-ring/30'

export function ProfileSettingsPage() {
  const query = useQuery({
    queryKey: queryKeys.auth.me(),
    queryFn: getCurrentUser,
  })

  return (
    <QueryState query={query}>
      {profile => (
        <ProfileForm
          key={`${profile.id}-${profile.version}`}
          profile={profile}
        />
      )}
    </QueryState>
  )
}

function ProfileForm({
  profile,
}: {
  profile: Awaited<ReturnType<typeof getCurrentUser>>
}) {
  const queryClient = useQueryClient()
  const form = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      displayName: profile.name,
      avatarUrl: profile.avatarUrl ?? '',
      locale: profile.locale,
      timezone: profile.timezone,
    },
  })
  const mutation = useMutation({
    mutationFn: (values: ProfileFormValues) => updateProfile({
      displayName: values.displayName,
      avatarUrl: values.avatarUrl.length > 0 ? values.avatarUrl : null,
      locale: values.locale,
      timezone: values.timezone,
      version: profile.version,
    }),
    onSuccess: (updated) => {
      toast.success('个人资料已保存')
      queryClient.setQueryData(queryKeys.auth.me(), updated)
      queryClient.setQueryData(queryKeys.auth.session(), (current: Session | null | undefined) => (
        current
          ? {
              ...current,
              user: {
                ...current.user,
                name: updated.name,
                avatarUrl: updated.avatarUrl,
              },
            }
          : current
      ))
    },
    onError: error => applyAuthFormError(form.setError, error),
  })

  useEffect(() => {
    form.reset({
      displayName: profile.name,
      avatarUrl: profile.avatarUrl ?? '',
      locale: profile.locale,
      timezone: profile.timezone,
    })
  }, [form, profile])

  const timezones = timezoneOptions(profile.timezone)

  return (
    <form className="grid max-w-xl gap-5" onSubmit={form.handleSubmit(values => mutation.mutate(values))} noValidate>
      <div>
        <h2 className="text-base font-semibold">个人资料</h2>
        <p className="mt-1 text-xs text-muted-foreground">更新显示名称、头像、语言和时区。</p>
      </div>
      <div className="grid gap-2">
        <Label htmlFor="displayName">显示名称</Label>
        <Input id="displayName" maxLength={100} {...form.register('displayName')} />
        {form.formState.errors.displayName ? <p className="text-xs text-destructive">{form.formState.errors.displayName.message}</p> : null}
      </div>
      <div className="grid gap-2">
        <Label htmlFor="avatarUrl">头像 URL</Label>
        <Input id="avatarUrl" placeholder="https://" maxLength={1000} {...form.register('avatarUrl')} />
        {form.formState.errors.avatarUrl ? <p className="text-xs text-destructive">{form.formState.errors.avatarUrl.message}</p> : null}
      </div>
      <div className="grid gap-2">
        <Label htmlFor="locale">语言</Label>
        <select id="locale" className={selectClassName} {...form.register('locale')}>
          {LOCALE_OPTIONS.map(option => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
        {form.formState.errors.locale ? <p className="text-xs text-destructive">{form.formState.errors.locale.message}</p> : null}
      </div>
      <div className="grid gap-2">
        <Label htmlFor="timezone">时区</Label>
        <select id="timezone" className={selectClassName} {...form.register('timezone')}>
          {timezones.map(zone => (
            <option key={zone} value={zone}>{zone}</option>
          ))}
        </select>
        {form.formState.errors.timezone ? <p className="text-xs text-destructive">{form.formState.errors.timezone.message}</p> : null}
      </div>
      {form.formState.errors.root?.message ? <p className="text-xs text-destructive" role="alert">{form.formState.errors.root.message}</p> : null}
      <div>
        <Button type="submit" disabled={mutation.isPending}>{mutation.isPending ? '保存中…' : '保存资料'}</Button>
      </div>
    </form>
  )
}
