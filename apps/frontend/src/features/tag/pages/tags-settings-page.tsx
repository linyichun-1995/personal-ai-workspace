import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useSession } from '@/features/auth/hooks/use-session'
import { tagApi } from '@/features/tag/api/tags'
import { TAG_COLORS, type Tag } from '@/features/tag/types'
import { tagNameError } from '@/features/tag/components/tag-picker'
import { OperationError } from '@/features/file/components/operation-error'
import { ListPagination } from '@/shared/components/list-pagination'
import { QueryState } from '@/shared/components/query-state'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/shared/components/ui/dialog'
export function TagsSettingsPage() {
  const { data: session } = useSession()
  const ws = session?.workspace.id ?? ''
  const client = useQueryClient()
  const [q, setQ] = useState('')
  const [page, setPage] = useState(1)
  const [editing, setEditing] = useState<Tag | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Tag | null>(null)
  const [name, setName] = useState('')
  const [color, setColor] = useState<Tag['color']>('GRAY')
  const query = useQuery({
    queryKey: ['content', ws, 'tags', 'manage', q, page],
    queryFn: ({ signal }) => tagApi.list(q, page, signal),
    enabled: Boolean(ws),
  })
  const refresh = () => client.invalidateQueries({ queryKey: ['content', ws] })
  const save = useMutation({
    mutationFn: () =>
      editing === 'new' ? tagApi.create(name, color) : tagApi.update(editing!, { name, color }),
    onSuccess: () => {
      setEditing(null)
      void refresh()
    },
  })
  const remove = useMutation({
    mutationFn: () => tagApi.remove(deleting!),
    onSuccess: () => {
      setDeleting(null)
      void refresh()
    },
  })
  return (
    <div className="grid gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">标签管理</h2>
        <Button
          onClick={() => {
            setEditing('new')
            setName('')
            setColor('GRAY')
            save.reset()
          }}
        >
          创建标签
        </Button>
      </div>
      <p className="text-sm text-muted-foreground">
        四类内容共用标签。使用数量包含可访问的归档内容；删除标签会解除关联，内容保留。
      </p>
      <Input
        aria-label="筛选标签"
        placeholder="按名称筛选…"
        value={q}
        onChange={(event) => {
          setQ(event.target.value)
          setPage(1)
        }}
      />
      <QueryState
        query={query}
        isEmpty={(data) => !data.items.length}
        empty={{ title: '暂无匹配的标签', description: '创建标签以整理项目、任务、笔记和文件。' }}
      >
        {(data) => (
          <div className="divide-y rounded-lg border">
            {data.items.map((tag) => (
              <div key={tag.id} className="flex flex-wrap items-center justify-between gap-2 p-3">
                <div className="min-w-0">
                  <p className="break-all font-medium">{tag.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {TAG_COLORS[tag.color]} · {tag.referenceCount} 处使用
                  </p>
                </div>
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setEditing(tag)
                      setName(tag.name)
                      setColor(tag.color)
                      save.reset()
                    }}
                  >
                    编辑
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setDeleting(tag)
                      remove.reset()
                    }}
                  >
                    删除
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </QueryState>
      {query.data && (
        <ListPagination
          page={page}
          total={query.data.total}
          totalPages={query.data.totalPages}
          onPageChange={setPage}
        />
      )}
      <Dialog
        open={Boolean(editing)}
        onOpenChange={(open) => !open && !save.isPending && setEditing(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing === 'new' ? '创建标签' : '编辑标签'}</DialogTitle>
            <DialogDescription>标签名称在工作空间内唯一，最多 30 个字符。</DialogDescription>
          </DialogHeader>
          <label className="grid gap-2 text-sm">
            名称
            <Input value={name} onChange={(event) => setName(event.target.value)} />
          </label>
          <label className="grid gap-2 text-sm">
            颜色
            <select
              className="h-9 rounded border bg-background px-2"
              value={color}
              onChange={(event) => setColor(event.target.value as Tag['color'])}
            >
              {Object.entries(TAG_COLORS).map(([id, label]) => (
                <option value={id} key={id}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          {name && tagNameError(name) && (
            <p role="alert" className="text-sm text-destructive">
              {tagNameError(name)}
            </p>
          )}
          <OperationError error={save.error} />
          <DialogFooter>
            <Button variant="outline" disabled={save.isPending} onClick={() => setEditing(null)}>
              取消
            </Button>
            <Button
              disabled={save.isPending || Boolean(tagNameError(name))}
              onClick={() => save.mutate()}
            >
              {save.isPending ? '正在保存…' : '保存'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <Dialog
        open={Boolean(deleting)}
        onOpenChange={(open) => !open && !remove.isPending && setDeleting(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>删除“{deleting?.name}”？</DialogTitle>
            <DialogDescription>
              将解除 {deleting?.referenceCount ?? 0} 处内容关联，项目、任务、笔记和文件不会被删除。
            </DialogDescription>
          </DialogHeader>
          <OperationError error={remove.error} />
          <DialogFooter>
            <Button variant="outline" disabled={remove.isPending} onClick={() => setDeleting(null)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={remove.isPending}
              onClick={() => remove.mutate()}
            >
              {remove.isPending ? '正在删除…' : '删除标签'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
