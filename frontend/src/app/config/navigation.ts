import type { LucideIcon } from 'lucide-react'
import {
  Archive,
  Blocks,
  Bot,
  CalendarDays,
  CheckCircle2,
  FolderKanban,
  LayoutDashboard,
  ListTodo,
  MessageSquareText,
  Paperclip,
  Settings,
  Sparkles,
  StickyNote,
} from 'lucide-react'

export type AppRoute
  = | '/app/dashboard'
    | '/app/projects'
    | '/app/projects/active'
    | '/app/projects/archived'
    | '/app/projects/templates'
    | '/app/tasks'
    | '/app/tasks/today'
    | '/app/tasks/upcoming'
    | '/app/tasks/completed'
    | '/app/notes'
    | '/app/files'
    | '/app/search'
    | '/app/ai'
    | '/app/ai/knowledge'
    | '/app/ai/agents'
    | '/app/integrations'
    | '/app/settings'

export interface AppNavChild {
  label: string
  icon: LucideIcon
  to: AppRoute
  children?: never
}

export interface AppNavGroup {
  to?: never
  label: string
  icon: LucideIcon
  children: readonly AppNavChild[]
}

export type AppNavItem = AppNavChild | AppNavGroup

export interface AppNavSection {
  id: string
  label: string
  items: readonly AppNavItem[]
}

export const primaryNavSections: readonly AppNavSection[] = [
  {
    id: 'workspace',
    label: '',
    items: [
      { to: '/app/dashboard', label: '概览', icon: LayoutDashboard },
      {
        label: '项目',
        icon: FolderKanban,
        children: [
          { to: '/app/projects', label: '全部项目', icon: FolderKanban },
          { to: '/app/projects/active', label: '进行中', icon: Sparkles },
          { to: '/app/projects/archived', label: '已归档', icon: Archive },
          { to: '/app/projects/templates', label: '模板', icon: StickyNote },
        ],
      },
      {
        label: '任务',
        icon: ListTodo,
        children: [
          { to: '/app/tasks/today', label: '今天', icon: CalendarDays },
          { to: '/app/tasks/upcoming', label: '即将到期', icon: ListTodo },
          { to: '/app/tasks/completed', label: '已完成', icon: CheckCircle2 },
          { to: '/app/tasks', label: '我的任务', icon: ListTodo },
        ],
      },
      { to: '/app/notes', label: '笔记', icon: StickyNote },
      { to: '/app/files', label: '文件', icon: Paperclip },
    ],
  },
  {
    id: 'tools',
    label: '',
    items: [
      {
        label: 'AI 助手',
        icon: Bot,
        children: [
          { to: '/app/ai', label: '对话', icon: MessageSquareText },
          { to: '/app/ai/knowledge', label: '知识库', icon: StickyNote },
          { to: '/app/ai/agents', label: '智能体', icon: Sparkles },
        ],
      },
      { to: '/app/integrations', label: '集成', icon: Blocks },
    ],
  },
]

export const secondaryNavItems: readonly AppNavItem[] = [
  { to: '/app/settings', label: '设置', icon: Settings },
]

export const commandNavigationItems: readonly AppNavChild[] = primaryNavSections.flatMap(section => (
  section.items.flatMap(item => item.children ?? (item.to ? [{ label: item.label, icon: item.icon, to: item.to }] : []))
))
