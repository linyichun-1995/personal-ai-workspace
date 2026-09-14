import { Link } from '@tanstack/react-router'
import { ArrowUpRight, FilePlus2, ListPlus, Search, Sparkles, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'

import { commandNavigationItems } from '@/app/config/navigation'
import { AppLogo } from '@/shared/components/app-logo'
import { Button } from '@/shared/components/ui/button'
import { cn } from '@/shared/lib/utils'
import { useUiStore } from '@/stores/ui-store'

const quickActions = [
  { label: '新建任务', hint: 'T', icon: ListPlus },
  { label: '新建笔记', hint: 'N', icon: FilePlus2 },
  { label: '与 AI 对话', hint: 'I', icon: Sparkles },
]

export function CommandPalette() {
  const open = useUiStore(state => state.commandPaletteOpen)
  const setOpen = useUiStore(state => state.setCommandPaletteOpen)
  const [query, setQuery] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        setOpen(!open)
        if (open) {
          setQuery('')
        }
      }
      if (event.key === 'Escape') {
        setOpen(false)
        setQuery('')
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [open, setOpen])

  useEffect(() => {
    if (open) {
      const frameId = window.requestAnimationFrame(() => inputRef.current?.focus())
      return () => window.cancelAnimationFrame(frameId)
    }
    return undefined
  }, [open])

  const results = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase()
    if (!normalizedQuery) {
      return commandNavigationItems
    }
    return commandNavigationItems.filter(item => item.label.toLocaleLowerCase().includes(normalizedQuery))
  }, [query])

  if (!open) {
    return null
  }

  return (
    <div
      className="fixed inset-0 z-80 flex items-start justify-center bg-foreground/25 px-4 pt-[12vh] backdrop-blur-[2px]"
      onMouseDown={() => {
        setOpen(false)
        setQuery('')
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label="命令面板"
        className="w-full max-w-[40rem] overflow-hidden rounded-xl border bg-popover text-popover-foreground shadow-[var(--shadow-md)]"
        onMouseDown={event => event.stopPropagation()}
      >
        <div className="flex h-14 items-center gap-3 border-b px-4">
          <Search className="size-4 text-muted-foreground" />
          <input
            ref={inputRef}
            value={query}
            onChange={event => setQuery(event.target.value)}
            placeholder="搜索页面，或输入一个命令…"
            className="h-full min-w-0 flex-1 bg-transparent text-sm placeholder:text-muted-foreground"
          />
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={() => {
              setOpen(false)
              setQuery('')
            }}
            aria-label="关闭命令面板"
          >
            <X />
          </Button>
        </div>

        <div className="scrollbar-subtle max-h-[58vh] overflow-y-auto p-2">
          {!query
            ? (
                <div className="mb-2">
                  <p className="px-2 py-1.5 text-[11px] font-medium text-muted-foreground">快捷操作</p>
                  {quickActions.map((action) => {
                    const Icon = action.icon
                    return (
                      <button
                        key={action.label}
                        type="button"
                        className="flex h-10 w-full items-center gap-3 rounded-md px-2 text-left text-sm hover:bg-accent"
                      >
                        <span className="grid size-7 place-items-center rounded-md bg-primary-subtle text-primary"><Icon className="size-3.5" /></span>
                        <span className="flex-1">{action.label}</span>
                        <kbd className="rounded border px-1.5 py-0.5 font-sans text-[10px] text-muted-foreground">
                          Ctrl
                          {action.hint}
                        </kbd>
                      </button>
                    )
                  })}
                </div>
              )
            : null}

          <p className="px-2 py-1.5 text-[11px] font-medium text-muted-foreground">导航</p>
          {results.length > 0
            ? results.map((item) => {
                const Icon = item.icon
                return (
                  <Link
                    key={item.to}
                    to={item.to}
                    onClick={() => {
                      setOpen(false)
                      setQuery('')
                    }}
                    className={cn('flex h-10 items-center gap-3 rounded-md px-2 text-sm hover:bg-accent')}
                  >
                    <span className="grid size-7 place-items-center rounded-md bg-secondary text-muted-foreground"><Icon className="size-3.5" /></span>
                    <span className="flex-1">{item.label}</span>
                    <ArrowUpRight className="size-3.5 text-muted-foreground" />
                  </Link>
                )
              })
            : (
                <div className="flex flex-col items-center gap-2 px-6 py-10 text-center">
                  <AppLogo className="size-9 opacity-70" />
                  <p className="text-sm font-medium">没有匹配结果</p>
                  <p className="text-xs text-muted-foreground">换一个关键词，或直接询问 AI。</p>
                </div>
              )}
        </div>
      </div>
    </div>
  )
}
