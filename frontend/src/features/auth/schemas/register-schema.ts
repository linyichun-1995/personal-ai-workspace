import { z } from 'zod'

export const registerSchema = z.object({
  name: z.string().trim().min(2, '昵称至少需要 2 个字符').max(30, '昵称不能超过 30 个字符'),
  email: z.string().trim().pipe(z.email('请输入有效邮箱')),
  password: z.string().min(8, '密码至少需要 8 个字符').max(72, '密码不能超过 72 个字符').regex(/[a-z]/i, '密码需包含字母').regex(/\d/, '密码需包含数字'),
  confirmPassword: z.string().min(1, '请再次输入密码'),
}).refine(values => values.password === values.confirmPassword, { message: '两次输入的密码不一致', path: ['confirmPassword'] })

export type RegisterFormValues = z.infer<typeof registerSchema>
