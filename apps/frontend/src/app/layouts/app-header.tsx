import { Link, useNavigate } from '@tanstack/react-router'
import { useQueryClient } from '@tanstack/react-query'
import { CircleUserRound, LogOut, Menu, Moon, Palette, Search, Sun } from 'lucide-react'
import { useTheme } from 'next-themes'
import { Popover } from 'radix-ui'
import { useState } from 'react'

import { logout } from '@/features/auth/api/session'
import { useSession } from '@/features/auth/hooks/use-session'
import { queryKeys } from '@/shared/api/query-keys'
import { Button } from '@/shared/components/ui/button'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/shared/components/ui/tooltip'
import { useIsDesktop } from '@/shared/hooks/use-media-query'
import { useUiStore } from '@/stores/ui-store'

function userInitial(name?: string): string {
  const trimmed = name?.trim()
  return trimmed && trimmed.length > 0 ? trimmed.charAt(0).toUpperCase() : 'A'
}

export function AppHeader({ title }: { title?: string }) {
  const isDesktop = useIsDesktop()
  const setMobileNavOpen = useUiStore((state) => state.setMobileNavOpen)
  const setCommandPaletteOpen = useUiStore((state) => state.setCommandPaletteOpen)
  const { resolvedTheme, setTheme } = useTheme()
  const { data: session } = useSession()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  async function handleLogout() {
    setMenuOpen(false)
    try {
      await logout()
    } finally {
      queryClient.setQueryData(queryKeys.auth.session(), null)
      queryClient.removeQueries({ queryKey: queryKeys.auth.all() })
      await navigate({ to: '/login' })
    }
  }

  return (
    <header className="sticky top-0 z-30 flex h-(--header-height) shrink-0 items-center gap-3 border-b border-border-subtle bg-background/90 px-4 backdrop-blur-lg md:px-5">
      {!isDesktop ? (
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setMobileNavOpen(true)}
          aria-label="打开导航"
        >
          <Menu />
        </Button>
      ) : null}

      {!isDesktop && (
        <p className="min-w-0 truncate text-sm font-medium sm:hidden">{title ?? 'AI Workspace'}</p>
      )}

      <button
        type="button"
        onClick={() => setCommandPaletteOpen(true)}
        className="hidden h-9 w-full max-w-136 items-center gap-2 rounded-md border border-transparent bg-surface-sunken/65 px-3 text-xs text-muted-foreground transition-colors hover:border-border hover:bg-surface-subtle focus-visible:ring-2 focus-visible:ring-ring/30 sm:flex"
      >
        <Search className="size-4" />
        <span className="flex-1 truncate text-left">跳转到页面或新建内容…</span>
        <kbd className="rounded border bg-background px-1.5 py-0.5 font-sans text-[10px] shadow-xs">
          Ctrl K
        </kbd>
      </button>

      <div className="ml-auto flex shrink-0 items-center gap-1 sm:gap-3">
        <Button
          variant="ghost"
          size="icon-sm"
          className="sm:hidden"
          aria-label="快捷导航"
          onClick={() => setCommandPaletteOpen(true)}
        >
          <Search />
        </Button>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button variant="ghost" size="icon-sm" asChild>
              <Link to="/app/settings/appearance" aria-label="外观设置">
                <Palette />
              </Link>
            </Button>
          </TooltipTrigger>
          <TooltipContent>外观设置</TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon-sm"
              onClick={() => setTheme(resolvedTheme === 'dark' ? 'light' : 'dark')}
              aria-label={resolvedTheme === 'dark' ? '切换到浅色主题' : '切换到深色主题'}
            >
              {resolvedTheme === 'dark' ? <Sun /> : <Moon />}
            </Button>
          </TooltipTrigger>
          <TooltipContent>{resolvedTheme === 'dark' ? '浅色主题' : '深色主题'}</TooltipContent>
        </Tooltip>
        <Popover.Root open={menuOpen} onOpenChange={setMenuOpen}>
          <Popover.Trigger asChild>
            <button
              type="button"
              aria-label="账户菜单"
              className="ml-1 grid size-8 place-items-center rounded-full border bg-primary-subtle text-xs font-semibold text-primary focus-visible:ring-2 focus-visible:ring-ring/30"
            >
              {userInitial(session?.user.name)}
            </button>
          </Popover.Trigger>
          <Popover.Portal>
            <Popover.Content
              align="end"
              sideOffset={8}
              className="z-50 w-56 rounded-lg border bg-popover p-1.5 shadow-(--shadow-md) outline-none"
            >
              <div className="border-b border-border-subtle px-2.5 py-2">
                <p className="truncate text-sm font-medium">{session?.user.name ?? '未登录'}</p>
                <p className="truncate text-xs text-muted-foreground">{session?.user.email}</p>
              </div>
              <Link
                to="/app/settings/profile"
                onClick={() => setMenuOpen(false)}
                className="mt-1 flex min-h-9 items-center gap-2 rounded-md px-2.5 text-xs hover:bg-surface-subtle focus-visible:ring-2 focus-visible:ring-ring/30"
              >
                <CircleUserRound className="size-3.5" />
                个人资料
              </Link>
              <Link
                to="/app/settings/appearance"
                onClick={() => setMenuOpen(false)}
                className="flex min-h-9 items-center gap-2 rounded-md px-2.5 text-xs hover:bg-surface-subtle focus-visible:ring-2 focus-visible:ring-ring/30"
              >
                <Palette className="size-3.5" />
                设置
              </Link>
              <button
                type="button"
                onClick={() => void handleLogout()}
                className="flex min-h-9 w-full items-center gap-2 rounded-md px-2.5 text-left text-xs text-destructive hover:bg-destructive/8 focus-visible:ring-2 focus-visible:ring-ring/30"
              >
                <LogOut className="size-3.5" />
                退出登录
              </button>
            </Popover.Content>
          </Popover.Portal>
        </Popover.Root>
      </div>
    </header>
  )
}
