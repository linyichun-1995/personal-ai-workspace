import { defaultParseSearch, Link, useRouterState } from '@tanstack/react-router'
import { ArrowLeft } from 'lucide-react'
import { safeOrigin } from '@/features/search/lib/search-params'
export function useOrigin() {
  return useRouterState({ select: (state) => safeOrigin(state.location.href) })
}
export function OriginLink({
  origin,
  fallback = '/app/files',
  label = '返回来源',
}: {
  origin?: string
  fallback?: string
  label?: string
}) {
  const url = new URL(safeOrigin(origin) ?? fallback, 'https://workspace.invalid')
  return (
    <Link
      to={url.pathname as '/app/search'}
      search={defaultParseSearch(url.search)}
      className="flex w-fit items-center gap-2 rounded-sm text-sm text-muted-foreground hover:text-foreground"
    >
      <ArrowLeft className="size-4" />
      {label}
    </Link>
  )
}
