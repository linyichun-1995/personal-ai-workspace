import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from '@tanstack/react-router'
import { ArrowLeft, Mail } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { AuthFootnote, AuthSubmit } from '@/features/auth/components/auth-actions'
import { AuthInput } from '@/features/auth/components/auth-input'
import { useAuthMotion } from '@/features/auth/motion/auth-motion-context'
import { loginSchema } from '@/features/auth/schemas/login-schema'

const resetSchema = loginSchema.pick({ email: true })
export function ForgotPasswordPage() {
  const [submitted, setSubmitted] = useState(false)
  const { controller } = useAuthMotion()
  const form = useForm<{ email: string }>({ resolver: zodResolver(resetSchema), defaultValues: { email: '' } })
  return (
    <>
      <div className="auth-heading">
        <h2>找回工作空间</h2>
        <p>输入注册邮箱，重新连接你的个人空间。</p>
      </div>
      <form id="auth-form" className="auth-form" noValidate onChange={() => setSubmitted(false)} onSubmit={form.handleSubmit(() => setSubmitted(true), () => controller.error())}>
        <AuthInput label="邮箱" type="email" icon={Mail} autoComplete="email" placeholder="you@company.com" maxLength={254} {...form.register('email')} error={form.formState.errors.email?.message} />
        <AuthSubmit>发送重置链接</AuthSubmit>
        {submitted && <p className="auth-reset-result" role="status">邮箱格式已验证。当前为前端体验，未发送重置邮件。</p>}
      </form>
      <div className="auth-divider"><span>或</span></div>
      <div className="auth-switch-page">
        <Link to="/login">
          <ArrowLeft size={18} />
          返回登录
        </Link>
      </div>
      <AuthFootnote />
    </>
  )
}
