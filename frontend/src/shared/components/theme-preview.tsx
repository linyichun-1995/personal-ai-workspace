import type { CSSProperties } from 'react'

import { cn } from '@/shared/lib/utils'

const previewThemes = {
  light: {
    '--preview-bg': 'oklch(0.982 0.005 255)',
    '--preview-sidebar': 'oklch(0.96 0.006 255)',
    '--preview-card': 'oklch(0.998 0.001 255)',
    '--preview-line': 'oklch(0.91 0.009 255)',
    '--preview-text': 'oklch(0.35 0.02 255)',
  },
  dark: {
    '--preview-bg': 'oklch(0.17 0.01 255)',
    '--preview-sidebar': 'oklch(0.15 0.009 255)',
    '--preview-card': 'oklch(0.23 0.011 255)',
    '--preview-line': 'oklch(0.32 0.013 255)',
    '--preview-text': 'oklch(0.87 0.006 255)',
  },
} satisfies Record<string, CSSProperties & Record<string, string>>

export function ThemePreview({ theme, className }: { theme: 'light' | 'dark', className?: string }) {
  return (
    <span aria-hidden="true" style={previewThemes[theme] as CSSProperties} className={cn('flex h-24 w-full overflow-hidden rounded-md border border-(--preview-line) bg-(--preview-bg)', className)}>
      <span className="flex w-[24%] flex-col gap-2 border-r border-(--preview-line) bg-(--preview-sidebar) p-2">
        <span className="mb-1 size-2 rounded-sm bg-primary" />
        <span className="h-1.5 rounded-sm bg-primary/30" />
        <span className="h-1 rounded-sm bg-(--preview-text) opacity-25" />
        <span className="h-1 w-3/4 rounded-sm bg-(--preview-text) opacity-25" />
      </span>
      <span className="flex min-w-0 flex-1 flex-col gap-2 p-2">
        <span className="h-1.5 w-2/3 rounded-sm bg-(--preview-text)" />
        <span className="flex gap-1.5">
          <span className="h-6 flex-1 rounded-sm border border-(--preview-line) bg-(--preview-card) p-1"><span className="block h-1 w-1/2 rounded bg-primary" /></span>
          <span className="h-6 flex-1 rounded-sm border border-(--preview-line) bg-(--preview-card) p-1"><span className="block h-1 w-1/2 rounded bg-(--preview-text) opacity-30" /></span>
        </span>
        <span className="flex flex-1 items-center justify-between rounded-sm border border-(--preview-line) bg-(--preview-card) px-1.5">
          <span className="h-1 w-1/3 rounded bg-(--preview-text) opacity-35" />
          <span className="flex gap-1">
            <span className="size-1.5 rounded-full bg-success" />
            <span className="size-1.5 rounded-full bg-warning" />
            <span className="size-1.5 rounded-full bg-destructive" />
          </span>
        </span>
      </span>
    </span>
  )
}
