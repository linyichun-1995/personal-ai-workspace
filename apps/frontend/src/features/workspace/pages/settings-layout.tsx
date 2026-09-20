import type { LucideIcon } from 'lucide-react'
import { Link, Outlet, useRouterState } from '@tanstack/react-router'
import {
  Bell,
  CircleHelp,
  CircleUserRound,
  House,
  Keyboard,
  Palette,
  Plug,
  Shield,
} from 'lucide-react'

import { PageContainer } from '@/shared/components/page-container'
import { cn } from '@/shared/lib/utils'

const settingsNav = [
  { label: '个人资料', icon: CircleUserRound, to: '/app/settings/profile' },
  { label: '工作空间', icon: House, to: '/app/settings/workspace' },
  { label: '标签', icon: House, to: '/app/settings/tags' },
  { label: '安全', icon: Shield, to: '/app/settings/security' },
  { label: '外观', icon: Palette, to: '/app/settings/appearance' },
  { label: '通知', icon: Bell },
  { label: '集成', icon: Plug },
  { label: '快捷键', icon: Keyboard },
  { label: '关于', icon: CircleHelp },
] as const

type SettingsLink = Extract<(typeof settingsNav)[number], { to: string }>

function isSettingsLink(item: (typeof settingsNav)[number]): item is SettingsLink {
  return 'to' in item
}

export function SettingsLayout() {
  const pathname = useRouterState({ select: (state) => state.location.pathname })

  return (
    <PageContainer width="form" className="grid gap-5 lg:ml-0">
      <header>
        <h1 className="text-xl font-semibold tracking-tight">设置</h1>
        <p className="mt-1 text-[13px] text-muted-foreground">让工作空间更适合你的习惯。</p>
      </header>
      <div className="grid min-w-0 overflow-hidden rounded-xl border border-border-subtle bg-card lg:grid-cols-[180px_minmax(0,1fr)]">
        <nav
          aria-label="设置导航"
          className="flex gap-1 overflow-x-auto border-b border-border-subtle bg-surface/60 p-3 lg:flex-col lg:border-b-0 lg:border-r lg:py-5"
        >
          {settingsNav.map((item) => {
            const Icon = item.icon as LucideIcon
            if (!isSettingsLink(item)) {
              return (
                <button
                  key={item.label}
                  type="button"
                  disabled
                  className="flex min-h-10 shrink-0 items-center gap-2.5 rounded-md px-3 text-left text-xs text-muted-foreground disabled:cursor-default"
                >
                  <Icon className="size-4" strokeWidth={1.7} />
                  {item.label}
                </button>
              )
            }
            const active = pathname === item.to
            return (
              <Link
                key={item.to}
                to={item.to}
                aria-current={active ? 'page' : undefined}
                className={cn(
                  'flex min-h-10 shrink-0 items-center gap-2.5 rounded-md px-3 text-left text-xs text-muted-foreground hover:bg-surface-subtle hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring/40',
                  active && 'bg-primary-subtle font-medium text-primary',
                )}
              >
                <Icon className="size-4" strokeWidth={1.7} />
                {item.label}
              </Link>
            )
          })}
        </nav>
        <div className="min-w-0 p-5 sm:p-7 lg:p-8">
          <Outlet />
        </div>
      </div>
    </PageContainer>
  )
}
