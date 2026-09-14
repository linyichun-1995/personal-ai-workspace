import { Link } from '@tanstack/react-router'
import { Bell, Menu, Moon, Palette, Search, Sun } from 'lucide-react'
import { useTheme } from 'next-themes'

import { Button } from '@/shared/components/ui/button'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/shared/components/ui/tooltip'
import { useIsDesktop } from '@/shared/hooks/use-media-query'
import { useUiStore } from '@/stores/ui-store'

export function AppHeader({ title }: { title?: string }) {
  const isDesktop = useIsDesktop()
  const setMobileNavOpen = useUiStore(state => state.setMobileNavOpen)
  const setCommandPaletteOpen = useUiStore(state => state.setCommandPaletteOpen)
  const { resolvedTheme, setTheme } = useTheme()

  return (
    <header className="sticky top-0 z-30 flex h-(--header-height) shrink-0 items-center gap-3 border-b border-border-subtle bg-background/90 px-4 backdrop-blur-lg md:px-5">
      {!isDesktop
        ? (
            <Button variant="ghost" size="icon" onClick={() => setMobileNavOpen(true)} aria-label="打开导航">
              <Menu />
            </Button>
          )
        : null}

      {!isDesktop && <p className="min-w-0 truncate text-sm font-medium sm:hidden">{title ?? 'AI Workspace'}</p>}

      <button
        type="button"
        onClick={() => setCommandPaletteOpen(true)}
        className="hidden h-9 w-full max-w-[34rem] items-center gap-2 rounded-md border border-transparent bg-surface-sunken/65 px-3 text-xs text-muted-foreground transition-colors hover:border-border hover:bg-surface-subtle focus-visible:ring-2 focus-visible:ring-ring/30 sm:flex"
      >
        <Search className="size-4" />
        <span className="flex-1 truncate text-left">搜索项目、任务、笔记或询问 AI…</span>
        <kbd className="rounded border bg-background px-1.5 py-0.5 font-sans text-[10px] shadow-xs">Ctrl K</kbd>
      </button>

      <div className="ml-auto flex shrink-0 items-center gap-1 sm:gap-3">
        <Button variant="ghost" size="icon-sm" className="sm:hidden" aria-label="全局搜索" onClick={() => setCommandPaletteOpen(true)}><Search /></Button>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button variant="ghost" size="icon-sm" asChild>
              <Link to="/app/settings" aria-label="外观设置"><Palette /></Link>
            </Button>
          </TooltipTrigger>
          <TooltipContent>外观设置</TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button variant="ghost" size="icon-sm" aria-label="通知" className="relative hidden sm:inline-flex">
              <Bell />
              <span className="absolute right-1.5 top-1.5 size-1.5 rounded-full bg-destructive" />
            </Button>
          </TooltipTrigger>
          <TooltipContent>通知</TooltipContent>
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
        <span className="ml-1 grid size-8 place-items-center rounded-full border bg-primary-subtle text-xs font-semibold text-primary">A</span>
      </div>
    </header>
  )
}
