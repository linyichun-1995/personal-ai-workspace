import type { LoginFormValues } from '@/features/auth/schemas/login-schema'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from '@tanstack/react-router'
import { LockKeyhole, Mail } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { AuthFootnote, AuthSubmit } from '@/features/auth/components/auth-actions'
import { AuthInput } from '@/features/auth/components/auth-input'
import { useAuthMotion } from '@/features/auth/motion/auth-motion-context'
import { loginSchema } from '@/features/auth/schemas/login-schema'

export function LoginPage() {
  const { controller } = useAuthMotion()
  const navigate = useNavigate()
  const form = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' }, shouldFocusError: true })
  return (
    <>
      <div className="auth-heading">
        <h2>进入工作空间</h2>
        <p>继续未完成的工作，让信息重新归位。</p>
      </div>
      <form id="auth-form" className="auth-form" noValidate onSubmit={form.handleSubmit(() => controller.submit(() => { void navigate({ to: '/app/dashboard' }) }), () => controller.error())}>
        <AuthInput label="邮箱" icon={Mail} type="email" autoComplete="email" placeholder="you@company.com" maxLength={254} {...form.register('email')} error={form.formState.errors.email?.message} />
        <AuthInput label="密码" icon={LockKeyhole} type="password" autoComplete="current-password" placeholder="输入你的密码" {...form.register('password')} error={form.formState.errors.password?.message} labelAction={<Link to="/forgot-password" disabled={controller.busy}>忘记密码?</Link>} />
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
