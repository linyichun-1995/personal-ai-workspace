import { getRouteApi, Link, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '@/shared/api'
import { useSession } from '@/features/auth/hooks/use-session'
import {
  SOURCE_TYPES,
  queryValidation,
  type GlobalSearch,
} from '@/features/search/lib/search-params'
import { OriginLink, useOrigin } from '@/features/search/components/origin-link'
import { TagPicker } from '@/features/tag/components/tag-picker'
import { TagChips } from '@/features/tag/components/object-tags'
import type { Tag } from '@/features/tag/types'
import { OperationError } from '@/features/file/components/operation-error'
import { PageContainer } from '@/shared/components/page-container'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'
interface Result {
  type: keyof typeof SOURCE_TYPES
  id: string
  title: string
  project: { id: string; name: string } | null
  tags: Tag[]
  snippet: { text: string; matched: boolean }[]
  archived: boolean
}
interface Results {
  items: Result[]
  page: number
  total: number
  totalPages: number
  meta: { indexingPending: boolean }
}
const route = getRouteApi('/app/search')
export function SearchPage() {
  const filters = route.useSearch()
  const navigate = useNavigate()
  const origin = useOrigin()
  const { data: session } = useSession()
  const [draft, setDraft] = useState({ source: filters.q, value: filters.q ?? '' })
  const q = draft.source === filters.q ? draft.value : (filters.q ?? '')
  const setQ = (value: string) => setDraft({ source: filters.q, value })
  const invalid = queryValidation(filters.q)
  const change = (next: GlobalSearch) => void navigate({ to: '/app/search', search: next })
  const query = useQuery({
    queryKey: ['content', session?.workspace.id, 'search', filters],
    enabled: Boolean(session) && !invalid,
    queryFn: ({ signal }) =>
      api.get<Results>('/v1/search', {
        signal,
        query: { ...filters, size: 20, origin: undefined },
      }),
  })
  return (
    <PageContainer>
      <div className="grid gap-5">
        <header className="grid gap-2">
          {filters.origin && <OriginLink origin={filters.origin} />}
          <h1 className="text-2xl font-semibold">搜索</h1>
          <p className="text-sm text-muted-foreground">查找项目、任务、笔记和文件正文。</p>
        </header>
        <form
          className="flex gap-2"
          onSubmit={(event) => {
            event.preventDefault()
            change({ ...filters, q, page: 1 })
          }}
        >
          <Input
            aria-label="搜索内容"
            placeholder="输入关键词，每项至少 2 个字符"
            value={q}
            maxLength={200}
            onChange={(event) => setQ(event.target.value)}
          />
          <Button type="submit">搜索</Button>
        </form>
        <div className="flex flex-wrap gap-3">
          {Object.entries(SOURCE_TYPES).map(([type, label]) => (
            <label key={type} className="text-sm">
              <input
                type="checkbox"
                checked={filters.types?.includes(type as Result['type']) ?? false}
                onChange={(event) =>
                  change({
                    ...filters,
                    page: 1,
                    types: event.target.checked
                      ? [...(filters.types ?? []), type as Result['type']]
                      : filters.types?.filter((value) => value !== type),
                  })
                }
              />{' '}
              {label}
            </label>
          ))}
        </div>
        <details className="rounded border p-3">
          <summary>筛选与排序</summary>
          <div className="mt-3 grid gap-3">
            <TagPicker
              allowCreate={false}
              selected={filters.tagIds ?? []}
              onChange={(tagIds) => change({ ...filters, tagIds, page: 1 })}
            />
            <label>
              标签匹配{' '}
              <select
                value={filters.tagMode}
                onChange={(e) =>
                  change({ ...filters, tagMode: e.target.value as 'ALL' | 'ANY', page: 1 })
                }
              >
                <option value="ALL">全部标签</option>
                <option value="ANY">任意标签</option>
              </select>
            </label>
            <label>
              排序{' '}
              <select
                value={filters.sort}
                onChange={(e) => change({ ...filters, sort: e.target.value, page: 1 })}
              >
                <option value="relevance">相关度</option>
                <option value="updatedAt,desc">最近更新</option>
              </select>
            </label>
            <label>
              <input
                type="checkbox"
                checked={Boolean(filters.includeArchived)}
                onChange={(e) => change({ ...filters, includeArchived: e.target.checked, page: 1 })}
              />{' '}
              包含归档内容
            </label>
            <Button
              variant="outline"
              onClick={() => change({ q: filters.q, origin: filters.origin })}
            >
              清除筛选
            </Button>
          </div>
        </details>
        {invalid && <p role="alert">{invalid}</p>}
        <OperationError error={query.error} />
        {query.isError && (
          <Button variant="outline" onClick={() => void query.refetch()}>
            重试搜索
          </Button>
        )}
        {query.isFetching && <p role="status">正在搜索…</p>}
        {!invalid && query.data && (
          <>
            {query.data.meta.indexingPending && (
              <p role="status" className="text-sm text-muted-foreground">
                部分内容正在建立索引，稍后搜索可获得更新的结果。
              </p>
            )}
            <p className="text-sm">共 {query.data.total} 条结果</p>
            <ul className="grid gap-3">
              {query.data.items.map((item) => (
                <li
                  key={`${item.type}:${item.id}`}
                  className="grid gap-2 rounded-lg border bg-card p-4"
                >
                  <span className="text-xs text-muted-foreground">
                    {SOURCE_TYPES[item.type]}
                    {item.archived ? ' · 已归档' : ''}
                    {item.project ? ` · ${item.project.name}` : ''}
                  </span>
                  <div className="font-medium">
                    {item.type === 'FILE' ? (
                      <Link
                        to="/app/files/$fileId"
                        params={{ fileId: item.id }}
                        search={{ origin }}
                      >
                        {item.title}
                      </Link>
                    ) : item.type === 'PROJECT' ? (
                      <Link
                        to="/app/projects/$projectId"
                        params={{ projectId: item.id }}
                        search={{ origin }}
                      >
                        {item.title}
                      </Link>
                    ) : item.type === 'NOTE' ? (
                      <Link
                        to="/app/notes/$noteId"
                        params={{ noteId: item.id }}
                        search={{ origin }}
                      >
                        {item.title}
                      </Link>
                    ) : (
                      <Link to="/app/tasks" search={{ taskId: item.id }}>
                        {item.title}
                      </Link>
                    )}
                  </div>
                  <p className="whitespace-pre-wrap break-words text-sm">
                    {item.snippet.map((part, index) =>
                      part.matched ? (
                        <mark key={index}>{part.text}</mark>
                      ) : (
                        <span key={index}>{part.text}</span>
                      ),
                    )}
                  </p>
                  <TagChips tags={item.tags} />
                </li>
              ))}
            </ul>
            {!query.data.items.length && (
              <p className="p-8 text-center text-muted-foreground">
                没有符合条件的内容，试试其他关键词或减少筛选。
              </p>
            )}
            <div className="flex items-center justify-between">
              <Button
                variant="outline"
                disabled={query.data.page <= 1}
                onClick={() => change({ ...filters, page: query.data.page - 1 })}
              >
                上一页
              </Button>
              <span>
                {query.data.page} / {Math.max(1, Math.min(100, query.data.totalPages))}
              </span>
              <Button
                variant="outline"
                disabled={query.data.page >= Math.min(100, query.data.totalPages)}
                onClick={() => change({ ...filters, page: query.data.page + 1 })}
              >
                下一页
              </Button>
            </div>
          </>
        )}
      </div>
    </PageContainer>
  )
}
