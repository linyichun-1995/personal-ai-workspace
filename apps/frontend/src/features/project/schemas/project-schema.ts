import { z } from 'zod'

export const projectFormSchema = z.object({
  name: z.string().trim().min(1, '请输入项目名称').max(200, '项目名称不能超过 200 个字符'),
  description: z.string().max(20_000, '描述过长').optional(),
  status: z.enum(['PLANNED', 'ACTIVE', 'PAUSED', 'COMPLETED']),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH']),
  startDate: z.string().optional(),
  dueDate: z.string().optional(),
}).superRefine((value, ctx) => {
  if (value.startDate && value.dueDate && value.dueDate < value.startDate) {
    ctx.addIssue({ code: 'custom', path: ['dueDate'], message: '截止日期不能早于开始日期' })
  }
})

export type ProjectFormValues = z.infer<typeof projectFormSchema>
