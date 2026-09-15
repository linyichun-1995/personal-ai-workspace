import type { AppNavChild, AppNavItem } from '@/app/config/navigation'
import { Link, useRouterState } from '@tanstack/react-router'
import { ChevronDown, PanelLeftClose, PanelLeftOpen } from 'lucide-react'
import { Popover } from 'radix-ui'
import { useId, useState } from 'react'

import { primaryNavSections, secondaryNavItems } from '@/app/config/navigation'
import { AppLogo } from '@/shared/components/app-logo'
import { Button } from '@/shared/components/ui/button'
import { ScrollArea } from '@/shared/components/ui/scroll-area'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/shared/components/ui/tooltip'
import { useIsWideDesktop } from '@/shared/hooks/use-media-query'
import { cn } from '@/shared/lib/utils'
import { useUiStore } from '@/stores/ui-store'

const itemClassName =
  'flex min-h-9 w-full min-w-0 items-center gap-2.5 rounded-md px-3 text-[13px] text-sidebar-foreground transition-colors hover:bg-sidebar-hover focus-visible:ring-2 focus-visible:ring-sidebar-ring/40 [&_svg]:text-sidebar-icon max-md:min-h-11'
const activeClassName =
  'bg-sidebar-active font-medium text-primary [&_svg]:text-sidebar-icon-active'

function ChildLinks({
  items,
  onNavigate,
}: {
  items: readonly AppNavChild[]
  onNavigate?: () => void
}) {
  return items.map((child) => {
    const Icon = child.icon
    return (
      <Link
        key={child.to}
        to={child.to}
        onClick={onNavigate}
        activeOptions={{ exact: true }}
        className="flex min-h-7 items-center gap-2 rounded-md px-2 text-xs text-sidebar-muted-foreground transition-colors hover:bg-sidebar-hover hover:text-sidebar-foreground focus-visible:ring-2 focus-visible:ring-sidebar-ring/40 max-md:min-h-11"
        activeProps={{ className: activeClassName }}
      >
        <Icon className="size-3.5 shrink-0" strokeWidth={1.7} />
        <span className="truncate">{child.label}</span>
      </Link>
    )
  })
}

function SidebarItem({
  item,
  collapsed,
  onNavigate,
}: {
  item: AppNavItem
  collapsed: boolean
  onNavigate?: () => void
}) {
  const pathname = useRouterState({ select: (state) => state.location.pathname })
  const [expansion, setExpansion] = useState({ expanded: true, pathname })
  const [flyoutOpen, setFlyoutOpen] = useState(false)
  const contentId = useId()
  const Icon = item.icon
  const hasActiveChild = item.children?.some(
    (child) => pathname === child.to || pathname.startsWith(`${child.to}/`),
  )
  const expanded = expansion.expanded || Boolean(hasActiveChild && expansion.pathname !== pathname)

  if (item.children?.length) {
    if (collapsed) {
      return (
        <Popover.Root open={flyoutOpen} onOpenChange={setFlyoutOpen}>
          <Tooltip>
            <TooltipTrigger asChild>
              <Popover.Trigger asChild>
                <button
                  type="button"
                  className={cn(
                    itemClassName,
                    'mx-auto size-9 justify-center px-0',
                    hasActiveChild && activeClassName,
                  )}
                  aria-label={`${item.label}菜单`}
                >
                  <Icon className="size-4.5" strokeWidth={1.75} />
                </button>
              </Popover.Trigger>
            </TooltipTrigger>
            {!flyoutOpen && <TooltipContent side="right">{item.label}</TooltipContent>}
          </Tooltip>
          <Popover.Portal>
            <Popover.Content
              side="right"
              align="start"
              sideOffset={12}
              collisionPadding={12}
              aria-label={`${item.label}子导航`}
              className="z-60 w-60 rounded-lg border bg-popover p-2 shadow-(--shadow-md) outline-none"
            >
              <p className="px-2 pb-2 pt-1 text-xs font-semibold">{item.label}</p>
              <nav aria-label={`${item.label}子导航`}>
                <ChildLinks
                  items={item.children}
                  onNavigate={() => {
                    setFlyoutOpen(false)
                    onNavigate?.()
                  }}
                />
              </nav>
            </Popover.Content>
          </Popover.Portal>
        </Popover.Root>
      )
    }

    return (
      <div>
        <button
          type="button"
          className={itemClassName}
          onClick={() => setExpansion({ expanded: !expanded, pathname })}
          aria-expanded={expanded}
          aria-controls={contentId}
        >
          <Icon className="size-4.5 shrink-0" strokeWidth={1.75} />
          <span className="flex-1 truncate text-left">{item.label}</span>
          <ChevronDown
            className={cn('size-3.5 transition-transform duration-180', expanded && 'rotate-180')}
          />
        </button>
        <div id={contentId} hidden={!expanded} className="pb-1 pl-8 pr-1">
          <ChildLinks items={item.children} onNavigate={onNavigate} />
        </div>
      </div>
    )
  }

  if (!item.to) return null

  const link = (
    <Link
      to={item.to}
      onClick={onNavigate}
      activeOptions={{ exact: item.to === '/app/dashboard', includeSearch: false }}
      className={cn(itemClassName, collapsed && 'mx-auto size-9 justify-center px-0')}
      activeProps={{ className: activeClassName }}
    >
      <Icon className="size-4.5 shrink-0" strokeWidth={1.75} />
      <span className={collapsed ? 'sr-only' : 'truncate'}>{item.label}</span>
    </Link>
  )

  return collapsed ? (
    <Tooltip>
      <TooltipTrigger asChild>{link}</TooltipTrigger>
      <TooltipContent side="right">{item.label}</TooltipContent>
    </Tooltip>
  ) : (
    link
  )
}

export function AppSidebar({
  className,
  onNavigate,
  forceExpanded = false,
}: {
  className?: string
  onNavigate?: () => void
  forceExpanded?: boolean
}) {
  const isWideDesktop = useIsWideDesktop()
  const sidebarCollapsed = useUiStore((state) => state.sidebarCollapsed)
  const toggleSidebar = useUiStore((state) => state.toggleSidebar)
  const collapsed = !forceExpanded && (!isWideDesktop || sidebarCollapsed)

  return (
    <aside
      className={cn(
        'flex h-full shrink-0 flex-col border-r border-sidebar-border/60 bg-sidebar text-sidebar-foreground transition-[width] duration-(--duration-slow) ease-(--ease-standard)',
        collapsed ? 'w-(--sidebar-width-collapsed)' : 'w-(--sidebar-width)',
        className,
      )}
    >
      <div
        className={cn(
          'flex h-(--header-height) shrink-0 items-center gap-2 px-4',
          collapsed && 'justify-center px-2',
        )}
      >
        <AppLogo className="size-7" />
        {!collapsed && (
          <p className="truncate text-[13px] font-semibold tracking-tight text-foreground">
            AI Workspace
          </p>
        )}
      </div>

      <ScrollArea className="min-h-0 flex-1">
        <nav className="grid gap-3 px-3 pb-4 pt-3" aria-label="主导航">
          {primaryNavSections.map((section) => (
            <div key={section.id} className="grid gap-1">
              {section.items.map((item) => (
                <SidebarItem
                  key={item.label}
                  item={item}
                  collapsed={collapsed}
                  onNavigate={onNavigate}
                />
              ))}
            </div>
          ))}
          <div className="grid gap-1">
            {secondaryNavItems.map((item) => (
              <SidebarItem
                key={item.label}
                item={item}
                collapsed={collapsed}
                onNavigate={onNavigate}
              />
            ))}
          </div>
        </nav>
      </ScrollArea>

      {isWideDesktop && !forceExpanded && (
        <div
          className={cn(
            'flex shrink-0 items-center px-3 py-3',
            collapsed ? 'justify-center' : 'justify-between',
          )}
        >
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="icon-sm"
                onClick={toggleSidebar}
                aria-label={collapsed ? '展开侧栏' : '收起侧栏'}
                className="text-sidebar-muted-foreground"
              >
                {collapsed ? <PanelLeftOpen /> : <PanelLeftClose />}
              </Button>
            </TooltipTrigger>
            <TooltipContent side="right">
              {collapsed ? '展开侧栏' : '收起侧栏'} · Ctrl B
            </TooltipContent>
          </Tooltip>
          {!collapsed && <kbd className="text-[10px] text-sidebar-muted-foreground">Ctrl + B</kbd>}
        </div>
      )}
    </aside>
  )
}
