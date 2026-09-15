import type { ReactNode } from 'react'

export interface PageHeaderProps {
  title: string
  description?: string
  eyebrow?: string
  actions?: ReactNode
}

export function PageHeader({ title, description, eyebrow, actions }: PageHeaderProps) {
  return (
    <header className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div className="grid gap-1.5">
        {eyebrow ? <p className="text-xs font-medium text-primary">{eyebrow}</p> : null}
        <h1 className="text-xl font-semibold tracking-tight md:text-2xl">{title}</h1>
        {description
          ? (
              <p className="max-w-2xl text-[13px] leading-relaxed text-muted-foreground">{description}</p>
            )
          : null}
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </header>
  )
}
