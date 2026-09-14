import type { LoginFormValues } from '@/features/auth/schemas/login-schema'
import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useSearch } from '@tanstack/react-router'
import { LockKeyhole, Mail } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { applyAuthFormError, resolvePostAuthPath } from '@/features/auth/api/form-errors'
import { login } from '@/features/auth/api/session'
import { AuthFootnote, AuthSubmit } from '@/features/auth/components/auth-actions'
import { AuthInput } from '@/features/auth/components/auth-input'
import { useAuthMotion } from '@/features/auth/motion/auth-motion-context'
import { loginSchema } from '@/features/auth/schemas/login-schema'
import { queryKeys } from '@/shared/api/query-keys'

export function LoginPage() {
  const { controller } = useAuthMotion()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const search = useSearch({ strict: false })
  const redirect = typeof search.redirect === 'string' ? search.redirect : undefined
  const form = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' }, shouldFocusError: true })
  return (
    <>
      <div className="auth-heading">
        <h2>进入工作空间</h2>
        <p>继续未完成的工作，让信息重新归位。</p>
      </div>
      <form
        id="auth-form"
        className="auth-form"
        noValidate
        onSubmit={form.handleSubmit(async (values) => {
          if (!controller.beginSubmit())
            return
          try {
            const session = await login(values)
            queryClient.setQueryData(queryKeys.auth.session(), session)
            controller.succeed(() => {
              void navigate({ href: resolvePostAuthPath(redirect) })
            })
          }
          catch (error) {
            applyAuthFormError(form.setError, error)
            controller.fail()
          }
        }, () => controller.error())}
      >
        <AuthInput label="邮箱" icon={Mail} type="email" autoComplete="email" placeholder="you@company.com" maxLength={254} {...form.register('email')} error={form.formState.errors.email?.message} />
        <AuthInput label="密码" icon={LockKeyhole} type="password" autoComplete="current-password" placeholder="输入你的密码" {...form.register('password')} error={form.formState.errors.password?.message} labelAction={<Link to="/forgot-password" disabled={controller.busy}>忘记密码?</Link>} />
        {form.formState.errors.root?.message ? <p className="auth-field-error" role="alert">{form.formState.errors.root.message}</p> : null}
        <AuthSubmit>进入工作空间</AuthSubmit>
      </form>
      <div className="auth-divider"><span>或</span></div>
      <div className="auth-switch-page">
        <Link to="/register" disabled={controller.busy}>创建工作空间</Link>
        <p>第一次使用？几分钟内建立你的个人空间。</p>
      </div>
      <AuthFootnote />
    </>
  )
}
