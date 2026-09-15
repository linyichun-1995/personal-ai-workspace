import { Link } from '@tanstack/react-router'
import { ArrowUpRight, FolderPlus, ListPlus, Search } from 'lucide-react'
import { useEffect, useState } from 'react'

import { commandNavigationItems } from '@/app/config/navigation'
import { CreateNoteButton } from '@/features/note/components/create-note-button'
import { Button } from '@/shared/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/shared/components/ui/dialog'
import { Input } from '@/shared/components/ui/input'
import { useUiStore } from '@/stores/ui-store'

export function CommandPalette() {
  const open = useUiStore((state) => state.commandPaletteOpen)
  const setOpen = useUiStore((state) => state.setCommandPaletteOpen)
  const [query, setQuery] = useState('')
  function close() {
    setOpen(false)
    setQuery('')
  }

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        setOpen(!open)
        setQuery('')
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [open, setOpen])

  const results = commandNavigationItems.filter((item) => item.label.includes(query.trim()))
  return (
    <Dialog
      open={open}
      onOpenChange={(value) => {
        setOpen(value)
        if (!value) setQuery('')
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>快捷导航</DialogTitle>
          <DialogDescription>跳转到工作页面，或直接新建任务、项目和笔记。</DialogDescription>
        </DialogHeader>
        <div className="relative">
          <Search
            className="pointer-events-none absolute left-3 top-2.5 size-4 text-muted-foreground"
            aria-hidden="true"
          />
          <Input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="查找页面…"
            aria-label="查找页面"
            className="pl-9"
          />
        </div>
        {!query.trim() && (
          <div className="grid gap-1 border-b border-border-subtle pb-3">
            <p className="mb-1 text-xs text-muted-foreground">新建内容</p>
            <Button variant="ghost" className="justify-start" asChild>
              <Link to="/app/tasks" search={{ create: true }} onClick={close}>
                <ListPlus />
                新建任务
              </Link>
            </Button>
            <Button variant="ghost" className="justify-start" asChild>
              <Link to="/app/projects" search={{ create: true }} onClick={close}>
                <FolderPlus />
                新建项目
              </Link>
            </Button>
            <CreateNoteButton variant="ghost" className="justify-start" onCreated={close} />
          </div>
        )}
        <nav className="grid gap-1" aria-label="页面导航">
          {results.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              onClick={close}
              className="flex min-h-10 items-center gap-3 rounded-md px-3 text-sm hover:bg-accent focus-visible:ring-2 focus-visible:ring-ring/40"
            >
              <item.icon className="size-4 text-muted-foreground" aria-hidden="true" />
              <span className="flex-1">{item.label}</span>
              <ArrowUpRight className="size-3.5" aria-hidden="true" />
            </Link>
          ))}
          {!results.length && (
            <p className="py-6 text-center text-sm text-muted-foreground">
              没有匹配的页面，试试“项目”“任务”或“笔记”。
            </p>
          )}
        </nav>
      </DialogContent>
    </Dialog>
  )
}
