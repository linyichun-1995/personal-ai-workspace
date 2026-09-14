import type { RegisterFormValues } from '@/features/auth/schemas/register-schema'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from '@tanstack/react-router'
import { LockKeyhole, Mail, UserRound } from 'lucide-react'
import { useForm, useWatch } from 'react-hook-form'
import { AuthFootnote, AuthSubmit } from '@/features/auth/components/auth-actions'
import { AuthInput } from '@/features/auth/components/auth-input'
import { useAuthMotion } from '@/features/auth/motion/auth-motion-context'
import { registerSchema } from '@/features/auth/schemas/register-schema'

export function RegisterPage() {
  const { controller } = useAuthMotion()
  const navigate = useNavigate()
  const form = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema), defaultValues: { name: '', email: '', password: '', confirmPassword: '' } })
  const password = useWatch({ control: form.control, name: 'password' })
  const strong = password.length >= 8 && /[a-z]/i.test(password) && /\d/.test(password)
  return (
    <div className="auth-register">
      <div className="auth-heading">
        <h2>创建工作空间</h2>
        <p>让项目、任务与灵感，从这里开始连接。</p>
      </div>
      <form id="auth-form" className="auth-form" noValidate onSubmit={form.handleSubmit(() => controller.submit(() => { void navigate({ to: '/app/dashboard' }) }), () => controller.error())}>
        <AuthInput label="昵称" icon={UserRound} autoComplete="nickname" placeholder="怎么称呼你" maxLength={30} {...form.register('name')} error={form.formState.errors.name?.message} />
        <AuthInput label="邮箱" icon={Mail} type="email" autoComplete="email" placeholder="you@company.com" maxLength={254} {...form.register('email')} error={form.formState.errors.email?.message} />
        <AuthInput label="密码" icon={LockKeyhole} type="password" autoComplete="new-password" placeholder="至少 8 位，包含字母和数字" maxLength={72} {...form.register('password')} error={form.formState.errors.password?.message} hint={password ? strong ? '密码强度良好' : '请使用至少 8 位字母与数字组合' : undefined} />
        <AuthInput label="确认密码" icon={LockKeyhole} type="password" autoComplete="new-password" placeholder="再次输入你的密码" maxLength={72} {...form.register('confirmPassword')} error={form.formState.errors.confirmPassword?.message} />
        <AuthSubmit>创建并进入工作空间</AuthSubmit>
      </form>
      <div className="auth-divider"><span>或</span></div>
      <div className="auth-switch-page">
        <Link to="/login" disabled={controller.busy}>已有工作空间，返回登录</Link>
        <p>让你的工作，回到熟悉的节奏。</p>
      </div>
      <AuthFootnote />
    </div>
  )
}
