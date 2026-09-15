import type { LucideIcon } from 'lucide-react'
import type { ComponentProps, ReactNode } from 'react'
import type { AppRoute } from '@/app/config/navigation'
import { Link } from '@tanstack/react-router'
import { ArrowRight } from 'lucide-react'

import { Button } from '@/shared/components/ui/button'
import { cn } from '@/shared/lib/utils'

export interface WorkspaceCardProps extends ComponentProps<'section'> {
  title?: string
  description?: string
  icon?: LucideIcon
  action?: ReactNode
  contentClassName?: string
}

export function WorkspaceCard({
  title,
  description,
  icon: Icon,
  action,
  className,
  contentClassName,
  children,
  ...props
}: WorkspaceCardProps) {
  const hasHeader = title || description || Icon || action

  return (
    <section className={cn('workspace-card flex min-w-0 flex-col overflow-hidden rounded-lg border border-border-subtle bg-card text-card-foreground shadow-xs', className)} {...props}>
      {hasHeader
        ? (
            <div className="workspace-card-header flex min-h-12 shrink-0 items-center justify-between gap-3 px-4 py-3">
              <div className="flex min-w-0 items-center gap-2.5">
                {Icon
                  ? (
                      <span className="grid size-5 shrink-0 place-items-center text-foreground">
                        <Icon className="size-4" />
                      </span>
                    )
                  : null}
                <div className="min-w-0">
                  {title ? <h2 className="truncate text-sm font-semibold">{title}</h2> : null}
                  {description ? <p className="mt-0.5 text-xs text-muted-foreground">{description}</p> : null}
                </div>
              </div>
              {action}
            </div>
          )
        : null}
      <div className={cn('min-h-0 flex-1', contentClassName)}>{children}</div>
    </section>
  )
}

export function ViewAllButton({ label = '查看全部', to }: { label?: string, to: AppRoute }) {
  return (
    <Button variant="ghost" size="xs" className="text-[11px] text-primary" asChild>
      <Link to={to}>
        {label}
        <ArrowRight className="size-3" />
      </Link>
    </Button>
  )
}
