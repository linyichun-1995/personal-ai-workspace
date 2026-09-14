import type { LucideIcon } from 'lucide-react'
import { TrendingDown, TrendingUp } from 'lucide-react'

import { cn } from '@/shared/lib/utils'

export interface MetricCardProps {
  label: string
  value: string | number
  trend: string
  trendDirection?: 'up' | 'down' | 'neutral'
  icon: LucideIcon
  tone?: 'primary' | 'info' | 'success' | 'warning' | 'danger'
  chart?: readonly number[]
}

const toneClasses = {
  primary: 'bg-primary-subtle text-primary',
  info: 'bg-info-subtle text-info',
  success: 'bg-success-subtle text-success',
  warning: 'bg-warning-subtle text-warning',
  danger: 'bg-destructive-subtle text-destructive',
}

const chartClasses = {
  primary: 'bg-primary',
  info: 'bg-info',
  success: 'bg-success',
  warning: 'bg-warning',
  danger: 'bg-primary',
}

export function MetricCard({
  label,
  value,
  trend,
  trendDirection = 'up',
  icon: Icon,
  tone = 'primary',
  chart = [25, 42, 35, 66, 58, 84],
}: MetricCardProps) {
  const TrendIcon = trendDirection === 'up' ? TrendingUp : TrendingDown

  return (
    <article className="metric-card group flex min-h-24 items-start gap-3 rounded-lg border border-border-subtle bg-card p-4 shadow-xs transition-colors hover:border-border hover:bg-card-hover">
      <span
        className={cn(
          'metric-icon grid size-9 shrink-0 place-items-center rounded-lg',
          toneClasses[tone],
        )}
      >
        <Icon className="size-[18px]" />
      </span>
      <div className="min-w-0 flex-1">
        <p className="metric-label truncate text-[11px] text-muted-foreground">{label}</p>
        <p className="metric-value mt-0.5 text-xl font-semibold tracking-tight">{value}</p>
        <p
          className={cn(
            'metric-trend mt-0.5 flex items-center gap-1 whitespace-nowrap text-[10px]',
            trendDirection === 'neutral'
              ? 'text-muted-foreground'
              : trendDirection === 'up'
                ? 'text-success'
                : 'text-destructive',
          )}
        >
          {trendDirection !== 'neutral' && <TrendIcon className="size-3 shrink-0" />}
          {trend}
        </p>
      </div>
      {chart.length > 0 && (
        <div
          className="metric-chart flex h-9 w-10 shrink-0 items-end gap-1 self-end pb-1"
          aria-hidden="true"
        >
          {chart.map((height) => (
            <span
              key={`${label}-${height}`}
              className={cn('w-1 flex-1 rounded-t-sm opacity-70', chartClasses[tone])}
              style={{ height: `${height}%` }}
            />
          ))}
        </div>
      )}
    </article>
  )
}
