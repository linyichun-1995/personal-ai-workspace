import { z } from 'zod'

export const taskFormSchema = z.object({
  title: z.string().trim().min(1, '请输入任务标题').max(300, '标题不能超过 300 个字符'),
  description: z.string().max(20_000, '描述过长').optional(),
  projectId: z.string().optional(),
  status: z.enum(['TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED']),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']),
  dueAt: z.string().optional(),
})

export type TaskFormValues = z.infer<typeof taskFormSchema>
