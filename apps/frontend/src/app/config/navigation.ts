import type { LucideIcon } from 'lucide-react'
import { FolderKanban, LayoutDashboard, ListTodo, Settings, StickyNote } from 'lucide-react'

export type AppRoute =
  | '/app/dashboard'
  | '/app/projects'
  | '/app/projects/active'
  | '/app/projects/archived'
  | '/app/projects/templates'
  | '/app/tasks'
  | '/app/tasks/today'
  | '/app/tasks/overdue'
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
  | '/app/settings/appearance'
  | '/app/settings/profile'
  | '/app/settings/workspace'
  | '/app/settings/security'

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
      { to: '/app/projects', label: '项目', icon: FolderKanban },
      { to: '/app/tasks', label: '任务', icon: ListTodo },
      { to: '/app/notes', label: '笔记', icon: StickyNote },
    ],
  },
]

export const secondaryNavItems: readonly AppNavItem[] = [
  { to: '/app/settings', label: '设置', icon: Settings },
]

export const commandNavigationItems: readonly AppNavChild[] = primaryNavSections.flatMap(
  (section) =>
    section.items.flatMap(
      (item) =>
        item.children ?? (item.to ? [{ label: item.label, icon: item.icon, to: item.to }] : []),
    ),
)
