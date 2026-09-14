import type { ComponentProps } from 'react'

import { cn } from '@/shared/lib/utils'

export interface ProgressProps extends ComponentProps<'div'> {
  value: number
  indicatorClassName?: string
}

export function Progress({ value, className, indicatorClassName, ...props }: ProgressProps) {
  const normalizedValue = Math.min(100, Math.max(0, value))

  return (
    <div
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={normalizedValue}
      className={cn('h-1.5 w-full overflow-hidden rounded-full bg-secondary', className)}
      {...props}
    >
      <div
        className={cn('h-full rounded-full bg-primary transition-[width] duration-300', indicatorClassName)}
        style={{ width: `${normalizedValue}%` }}
      />
    </div>
  )
}
