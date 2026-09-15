import type { ComponentProps } from 'react'

import { cn } from '@/shared/lib/utils'

export interface PageContainerProps extends ComponentProps<'div'> {
  width?: 'dashboard' | 'wide' | 'reading' | 'form' | 'fluid'
}

const widthClasses: Record<NonNullable<PageContainerProps['width']>, string> = {
  dashboard: 'max-w-none',
  wide: 'max-w-none',
  reading: 'max-w-[52rem]',
  form: 'max-w-[60rem]',
  fluid: 'max-w-none',
}

export function PageContainer({ width = 'dashboard', className, ...props }: PageContainerProps) {
  return (
    <div
      className={cn(
        'mx-auto w-full px-4 py-5 md:px-(--page-padding-x) md:py-6',
        widthClasses[width],
        className,
      )}
      {...props}
    />
  )
}
