import type { TaskItem } from '@/features/task/types'

export const demoTasks: TaskItem[] = [
  { id: 'TASK-1024', title: '评审身份认证模块 PR', project: 'Workspace Platform', status: 'review', priority: 'high', dueAt: '今天 10:00', assignee: 'Alex' },
  { id: 'TASK-1025', title: '完善 AI 助手使用文档', project: 'Personal AI Assistant', status: 'in-progress', priority: 'medium', dueAt: '今天 11:30', assignee: 'Mia' },
  { id: 'TASK-1026', title: '更新第三季度产品路线图', project: 'AI 知识库', status: 'todo', priority: 'medium', dueAt: '今天 14:00', assignee: 'Alex' },
  { id: 'TASK-1027', title: '准备项目演示材料', project: '飞书集成', status: 'in-progress', priority: 'high', dueAt: '今天 15:30', assignee: 'Jay' },
  { id: 'TASK-1028', title: '规划下一次产品迭代', project: 'Workspace Platform', status: 'todo', priority: 'low', dueAt: '今天 17:00', assignee: 'Nina' },
  { id: 'TASK-1029', title: '梳理 RAG 检索评估指标', project: 'AI 知识库', status: 'review', priority: 'medium', dueAt: '明天 10:00', assignee: 'Mia' },
  { id: 'TASK-1030', title: '同步飞书文档权限模型', project: '飞书集成', status: 'done', priority: 'high', dueAt: '9月 12日', assignee: 'Jay' },
  { id: 'TASK-1031', title: '实现项目活动时间线', project: 'Workspace Platform', status: 'in-progress', priority: 'medium', dueAt: '9月 15日', assignee: 'Alex' },
  { id: 'TASK-1032', title: '补充文件上传异常状态', project: 'Workspace Platform', status: 'todo', priority: 'low', dueAt: '9月 16日', assignee: 'Nina' },
  { id: 'TASK-1033', title: '设计 AI Tool Call 展示组件', project: 'Personal AI Assistant', status: 'review', priority: 'medium', dueAt: '9月 17日', assignee: 'Mia' },
  { id: 'TASK-1034', title: '整理知识库数据源清单', project: 'AI 知识库', status: 'done', priority: 'low', dueAt: '9月 11日', assignee: 'Alex' },
  { id: 'TASK-1035', title: '编写组件库使用说明', project: 'Workspace Platform', status: 'in-progress', priority: 'medium', dueAt: '9月 18日', assignee: 'Jay' },
]
