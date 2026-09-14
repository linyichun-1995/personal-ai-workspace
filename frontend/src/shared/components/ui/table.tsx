import type { ComponentProps } from 'react'

import { cn } from '@/shared/lib/utils'

function Table({ className, ...props }: ComponentProps<'table'>) {
  return (
    <div data-slot="table-container" className="scrollbar-subtle relative w-full overflow-x-auto">
      <table
        data-slot="table"
        className={cn('w-full caption-bottom text-[13px]', className)}
        {...props}
      />
    </div>
  )
}

function TableHeader({ className, ...props }: ComponentProps<'thead'>) {
  return (
    <thead
      data-slot="table-header"
      className={cn('[&_tr]:border-b', className)}
      {...props}
    />
  )
}

function TableBody({ className, ...props }: ComponentProps<'tbody'>) {
  return (
    <tbody
      data-slot="table-body"
      className={cn('[&_tr:last-child]:border-0', className)}
      {...props}
    />
  )
}

function TableFooter({ className, ...props }: ComponentProps<'tfoot'>) {
  return (
    <tfoot
      data-slot="table-footer"
      className={cn('border-t bg-muted/50 font-medium [&>tr]:last:border-b-0', className)}
      {...props}
    />
  )
}

function TableRow({ className, ...props }: ComponentProps<'tr'>) {
  return (
    <tr
      data-slot="table-row"
      className={cn(
        'h-(--row-height) border-b border-border-subtle transition-colors duration-(--duration-fast) hover:bg-surface-subtle data-[state=selected]:bg-primary-subtle',
        className,
      )}
      {...props}
    />
  )
}

function TableHead({ className, ...props }: ComponentProps<'th'>) {
  return (
    <th
      data-slot="table-head"
      className={cn(
        'h-10 px-(--table-cell-padding-x) text-left align-middle text-xs font-medium whitespace-nowrap text-muted-foreground',
        className,
      )}
      {...props}
    />
  )
}

function TableCell({ className, ...props }: ComponentProps<'td'>) {
  return (
    <td
      data-slot="table-cell"
      className={cn('px-(--table-cell-padding-x) py-(--table-cell-padding-y) align-middle whitespace-nowrap', className)}
      {...props}
    />
  )
}

export {
  Table,
  TableBody,
  TableCell,
  TableFooter,
  TableHead,
  TableHeader,
  TableRow,
}
