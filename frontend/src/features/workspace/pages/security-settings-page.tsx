import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'

import { applyAuthFormError } from '@/features/auth/api/form-errors'
import { changePassword } from '@/features/auth/api/session'
import { queryKeys } from '@/shared/api/query-keys'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'

const passwordSchema = z.object({
  currentPassword: z.string().min(1, '请输入当前密码'),
  newPassword: z.string().min(8, '新密码至少需要 8 个字符').max(72, '新密码不能超过 72 个字符').regex(/[a-z]/i, '密码需包含字母').regex(/\d/, '密码需包含数字'),
  confirmPassword: z.string().min(1, '请再次输入新密码'),
}).refine(values => values.newPassword === values.confirmPassword, {
  message: '两次输入的密码不一致',
  path: ['confirmPassword'],
})

type PasswordFormValues = z.infer<typeof passwordSchema>

export function SecuritySettingsPage() {
  const queryClient = useQueryClient()
  const form = useForm<PasswordFormValues>({
    resolver: zodResolver(passwordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  })
  const mutation = useMutation({
    mutationFn: (values: PasswordFormValues) => changePassword({
      currentPassword: values.currentPassword,
      newPassword: values.newPassword,
    }),
    onSuccess: async (session) => {
      queryClient.setQueryData(queryKeys.auth.session(), session)
      toast.success('密码已更新，其他登录会话已失效')
      form.reset()
    },
    onError: error => applyAuthFormError(form.setError, error),
  })

  return (
    <form className="grid max-w-xl gap-5" onSubmit={form.handleSubmit(values => mutation.mutate(values))} noValidate>
      <div>
        <h2 className="text-base font-semibold">安全</h2>
        <p className="mt-1 text-xs text-muted-foreground">修改密码后，其他设备上的登录状态会立即失效。</p>
      </div>
      <div className="grid gap-2">
        <Label htmlFor="currentPassword">当前密码</Label>
        <Input id="currentPassword" type="password" autoComplete="current-password" {...form.register('currentPassword')} />
        {form.formState.errors.currentPassword ? <p className="text-xs text-destructive">{form.formState.errors.currentPassword.message}</p> : null}
      </div>
      <div className="grid gap-2">
        <Label htmlFor="newPassword">新密码</Label>
        <Input id="newPassword" type="password" autoComplete="new-password" {...form.register('newPassword')} />
        {form.formState.errors.newPassword ? <p className="text-xs text-destructive">{form.formState.errors.newPassword.message}</p> : null}
      </div>
      <div className="grid gap-2">
        <Label htmlFor="confirmPassword">确认新密码</Label>
        <Input id="confirmPassword" type="password" autoComplete="new-password" {...form.register('confirmPassword')} />
        {form.formState.errors.confirmPassword ? <p className="text-xs text-destructive">{form.formState.errors.confirmPassword.message}</p> : null}
      </div>
      {form.formState.errors.root?.message ? <p className="text-xs text-destructive" role="alert">{form.formState.errors.root.message}</p> : null}
      <div>
        <Button type="submit" disabled={mutation.isPending}>{mutation.isPending ? '更新中…' : '更新密码'}</Button>
      </div>
    </form>
  )
}
