import { getRouteApi, Link, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useSession } from '@/features/auth/hooks/use-session'
import { fileApi } from '@/features/file/api/files'
import { EXTRACTION_LABELS, formatBytes, type FileSearch } from '@/features/file/lib/file-search'
import { UploadPanel } from '@/features/file/components/upload-panel'
import { OperationError } from '@/features/file/components/operation-error'
import { TagChips } from '@/features/tag/components/object-tags'
import { TagPicker } from '@/features/tag/components/tag-picker'
import { useOrigin } from '@/features/search/components/origin-link'
import { PageContainer } from '@/shared/components/page-container'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'

const route = getRouteApi('/app/files')
export function FilesPage() {
  const filters = route.useSearch()
  const navigate = useNavigate()
  return (
    <PageContainer>
      <div className="grid gap-5">
        <header>
          <h1 className="text-2xl font-semibold">文件资料库</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            收集项目资料，通过标签与正文搜索找回。
          </p>
        </header>
        <FileLibrary
          filters={filters}
          onChange={(next) => void navigate({ to: '/app/files', search: next })}
        />
      </div>
    </PageContainer>
  )
}
export function FileLibrary({
  projectId,
  readOnly = false,
  filters: supplied,
  onChange,
}: {
  projectId?: string
  readOnly?: boolean
  filters?: FileSearch
  onChange?: (next: FileSearch) => void
}) {
  const [local, setLocal] = useState<FileSearch>({})
  const filters = supplied ?? local
  const change = onChange ?? setLocal
  const { data: session } = useSession()
  const origin = useOrigin()
  const [q, setQ] = useState(filters.q ?? '')
  const query = useQuery({
    queryKey: ['content', session?.workspace.id, 'files', filters, projectId],
    enabled: Boolean(session),
    queryFn: ({ signal }) =>
      fileApi.list({ ...filters, projectId: projectId ?? filters.projectId }, signal),
    refetchInterval: 5000,
  })
  return (
    <div className="grid gap-4">
      <UploadPanel projectId={projectId ?? null} readOnly={readOnly} all={!projectId} />
      <div className="flex flex-wrap gap-2" aria-label="文件视图">
        {(['all', 'uncategorized', 'trash'] as const).map((view) => (
          <Button
            key={view}
            variant={(filters.view ?? 'all') === view ? 'default' : 'outline'}
            size="sm"
            onClick={() => change({ ...filters, view, page: 1 })}
          >
            {{ all: '全部文件', uncategorized: '未分类', trash: '回收站' }[view]}
          </Button>
        ))}
      </div>
      <form
        className="flex gap-2"
        onSubmit={(event) => {
          event.preventDefault()
          change({ ...filters, q, page: 1 })
        }}
      >
        <Input
          aria-label="按文件名称查找"
          placeholder="按文件名称查找"
          value={q}
          onChange={(event) => setQ(event.target.value)}
        />
        <Button type="submit">查找</Button>
      </form>
      <details className="rounded border p-3">
        <summary className="cursor-pointer text-sm">标签与排序筛选</summary>
        <div className="mt-3 grid gap-3">
          <TagPicker
            allowCreate={false}
            selected={filters.tagIds ?? []}
            onChange={(tagIds) => change({ ...filters, tagIds, page: 1 })}
          />
          <label className="text-sm">
            标签匹配{' '}
            <select
              aria-label="文件标签匹配"
              value={filters.tagMode ?? 'ALL'}
              onChange={(e) =>
                change({ ...filters, tagMode: e.target.value as 'ALL' | 'ANY', page: 1 })
              }
            >
              <option value="ALL">全部标签</option>
              <option value="ANY">任意标签</option>
            </select>
          </label>
          <label className="text-sm">
            排序{' '}
            <select
              aria-label="文件排序"
              value={filters.sort ?? 'updatedAt,desc'}
              onChange={(e) => change({ ...filters, sort: e.target.value, page: 1 })}
            >
              <option value="updatedAt,desc">最近更新</option>
              <option value="name,asc">名称</option>
              <option value="sizeBytes,desc">大小</option>
            </select>
          </label>
          <label className="text-sm">
            <input
              type="checkbox"
              checked={filters.includeArchived ?? false}
              onChange={(e) => change({ ...filters, includeArchived: e.target.checked, page: 1 })}
            />{' '}
            包含归档项目
          </label>
        </div>
      </details>
      <OperationError error={query.error} />
      {query.isPending && <p role="status">正在加载文件…</p>}
      {query.data && (
        <>
          <p className="text-xs text-muted-foreground">共 {query.data.total} 个文件</p>
          <ul className="grid gap-3">
            {query.data.items.map((file) => (
              <li key={file.id} className="grid gap-2 rounded-lg border bg-card p-4">
                <Link
                  className="break-all font-medium hover:underline"
                  to="/app/files/$fileId"
                  params={{ fileId: file.id }}
                  search={{ origin }}
                >
                  {file.displayName}
                </Link>
                <p className="text-xs text-muted-foreground">
                  {formatBytes(file.sizeBytes)} ·{' '}
                  {file.state === 'DELETED'
                    ? '文件已删除'
                    : (EXTRACTION_LABELS[file.extraction.status] ?? file.extraction.status)}
                </p>
                <TagChips tags={file.tags} />
              </li>
            ))}
          </ul>
          {!query.data.items.length && (
            <p className="rounded-lg border border-dashed p-8 text-center text-muted-foreground">
              没有符合条件的文件
            </p>
          )}
          <div className="flex items-center justify-between">
            <Button
              variant="outline"
              disabled={(filters.page ?? 1) <= 1}
              onClick={() => change({ ...filters, page: (filters.page ?? 1) - 1 })}
            >
              上一页
            </Button>
            <span className="text-sm">
              {query.data.page} / {Math.max(1, query.data.totalPages)}
            </span>
            <Button
              variant="outline"
              disabled={query.data.page >= query.data.totalPages}
              onClick={() => change({ ...filters, page: (filters.page ?? 1) + 1 })}
            >
              下一页
            </Button>
          </div>
        </>
      )}
    </div>
  )
}
