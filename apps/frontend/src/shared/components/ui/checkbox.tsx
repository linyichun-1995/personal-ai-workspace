import type { ComponentProps } from 'react'
import { Check, Minus } from 'lucide-react'
import { useEffect, useRef } from 'react'

import { cn } from '@/shared/lib/utils'

export interface CheckboxProps extends Omit<ComponentProps<'input'>, 'type'> {
  indeterminate?: boolean
}

export function Checkbox({ className, indeterminate = false, ...props }: CheckboxProps) {
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.indeterminate = indeterminate
    }
  }, [indeterminate])

  return (
    <label className={cn('group/checkbox relative inline-grid size-4 shrink-0 cursor-pointer place-items-center', className)}>
      <input
        ref={inputRef}
        type="checkbox"
        className="peer absolute inset-0 size-4 cursor-pointer appearance-none rounded-[4px] border border-input bg-background shadow-xs transition-colors checked:border-primary checked:bg-primary focus-visible:ring-2 focus-visible:ring-ring/30 disabled:cursor-not-allowed disabled:opacity-50"
        {...props}
      />
      {indeterminate
        ? <Minus className="pointer-events-none relative size-3 text-primary-foreground" strokeWidth={2.5} />
        : <Check className="pointer-events-none relative size-3 text-primary-foreground opacity-0 peer-checked:opacity-100" strokeWidth={2.5} />}
    </label>
  )
}
