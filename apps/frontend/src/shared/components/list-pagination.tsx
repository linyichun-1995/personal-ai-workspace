import { Button } from '@/shared/components/ui/button'

export function ListPagination({
  page,
  total,
  totalPages,
  loading,
  onPageChange,
}: {
  page: number
  total: number
  totalPages: number
  loading?: boolean
  onPageChange: (page: number) => void
}) {
  if (totalPages <= 1 && page === 1) return null
  return (
    <nav
      aria-label="列表分页"
      className="flex flex-wrap items-center justify-end gap-3 text-xs text-muted-foreground"
    >
      {page > Math.max(1, totalPages) ? (
        <>
          <span>当前页已没有内容</span>
          <Button variant="outline" size="sm" onClick={() => onPageChange(1)} disabled={loading}>
            返回第一页
          </Button>
        </>
      ) : (
        <>
          <span aria-live="polite">
            共 {total} 项 · 第 {page} / {Math.max(1, totalPages)} 页
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page <= 1 || loading}
            onClick={() => onPageChange(page - 1)}
          >
            上一页
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages || loading}
            onClick={() => onPageChange(page + 1)}
          >
            下一页
          </Button>
        </>
      )}
    </nav>
  )
}
