import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useSession } from '@/features/auth/hooks/use-session'
import { tagApi } from '@/features/tag/api/tags'
import type { Tag } from '@/features/tag/types'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { OperationError } from '@/features/file/components/operation-error'
export function tagNameError(value: string): string | undefined {
  const name = value.normalize('NFKC').trim()
  if (!name || [...name].length > 30) return '标签名称需为 1 至 30 个字符'
  if (/[\p{Cc}\p{Cf}]/u.test(name)) return '标签名称不能包含控制字符'
  return undefined
}
export function TagPicker({
  selected,
  onChange,
  allowCreate = true,
  disabled = false,
}: {
  selected: string[]
  onChange: (ids: string[]) => void
  allowCreate?: boolean
  disabled?: boolean
}) {
  const { data: session } = useSession()
  const workspaceId = session?.workspace.id ?? ''
  const client = useQueryClient()
  const [q, setQ] = useState('')
  const query = useInfiniteQuery({
    queryKey: ['content', workspaceId, 'tags', 'list', q],
    queryFn: ({ pageParam, signal }) => tagApi.list(q, pageParam, signal),
    initialPageParam: 1,
    getNextPageParam: (page) => (page.page < page.totalPages ? page.page + 1 : undefined),
    enabled: Boolean(workspaceId),
  })
  const tags = query.data?.pages.flatMap((page) => page.items) ?? []
  const normalized = q.normalize('NFKC').trim().toLocaleLowerCase()
  const exact = tags.find(
    (tag) => tag.name.normalize('NFKC').trim().toLocaleLowerCase() === normalized,
  )
  const create = useMutation({
    mutationFn: () => tagApi.create(q.trim()),
    onSuccess: (tag: Tag) => {
      onChange([...new Set([...selected, tag.id])])
      setQ('')
      void client.invalidateQueries({ queryKey: ['content', workspaceId, 'tags'] })
    },
    onError: () => {
      void query.refetch()
    },
  })
  return (
    <div className="grid gap-3">
      <Input
        aria-label="搜索标签名称"
        placeholder="搜索标签名称…"
        value={q}
        disabled={disabled}
        onChange={(event) => {
          setQ(event.target.value)
          create.reset()
        }}
      />
      <p className="text-xs text-muted-foreground">已选择 {selected.length} / 20 个标签</p>
      <div className="grid max-h-52 gap-1 overflow-y-auto rounded-md border p-2">
        {query.isPending && (
          <p role="status" className="p-2 text-sm">
            正在加载标签…
          </p>
        )}
        {tags.map((tag) => (
          <label
            key={tag.id}
            className="flex min-h-9 cursor-pointer items-center gap-2 rounded p-2 text-sm hover:bg-muted"
          >
            <input
              type="checkbox"
              checked={selected.includes(tag.id)}
              disabled={disabled || (!selected.includes(tag.id) && selected.length >= 20)}
              onChange={(event) =>
                onChange(
                  event.target.checked
                    ? [...selected, tag.id]
                    : selected.filter((id) => id !== tag.id),
                )
              }
            />
            <span className="break-all">{tag.name}</span>
          </label>
        ))}
        {!query.isPending && !query.isError && !tags.length && (
          <p className="p-2 text-sm text-muted-foreground">没有匹配的标签</p>
        )}
        {query.hasNextPage && (
          <Button
            type="button"
            variant="ghost"
            disabled={query.isFetchingNextPage}
            onClick={() => void query.fetchNextPage()}
          >
            加载更多标签
          </Button>
        )}
      </div>
      <OperationError error={query.error || create.error} />
      {query.isError && (
        <Button type="button" variant="outline" onClick={() => void query.refetch()}>
          重试加载
        </Button>
      )}
      {allowCreate && q.trim() && !exact && (
        <div className="grid gap-1">
          <Button
            type="button"
            variant="outline"
            disabled={
              disabled || create.isPending || selected.length >= 20 || Boolean(tagNameError(q))
            }
            onClick={() => create.mutate()}
          >
            {create.isPending ? '正在创建…' : `创建标签“${q.trim()}”`}
          </Button>
          {tagNameError(q) && <p className="text-xs text-destructive">{tagNameError(q)}</p>}
        </div>
      )}
    </div>
  )
}
