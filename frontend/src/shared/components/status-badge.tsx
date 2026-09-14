import type { ComponentProps } from 'react'

import { cn } from '@/shared/lib/utils'

export type StatusTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger'

const toneClasses: Record<StatusTone, string> = {
  neutral: 'border-border bg-muted text-muted-foreground',
  info: 'border-info/15 bg-info-subtle text-info',
  success: 'border-success/15 bg-success-subtle text-success',
  warning: 'border-warning/15 bg-warning-subtle text-warning-foreground',
  danger: 'border-destructive/15 bg-destructive-subtle text-destructive',
}

export interface StatusBadgeProps extends ComponentProps<'span'> {
  tone?: StatusTone
  dot?: boolean
}

export function StatusBadge({ tone = 'neutral', dot = false, className, children, ...props }: StatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex h-5 w-fit items-center gap-1.5 rounded-md border px-1.5 text-[11px] font-medium whitespace-nowrap',
        toneClasses[tone],
        className,
      )}
      {...props}
    >
      {dot ? <span className="size-1.5 rounded-full bg-current opacity-70" aria-hidden="true" /> : null}
      {children}
    </span>
  )
}
