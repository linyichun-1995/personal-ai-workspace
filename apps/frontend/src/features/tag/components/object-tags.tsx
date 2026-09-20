import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { useState } from 'react'
import { useSession } from '@/features/auth/hooks/use-session'
import { tagApi } from '@/features/tag/api/tags'
import { TagPicker } from '@/features/tag/components/tag-picker'
import type { Tag, TagResource } from '@/features/tag/types'
import { useOrigin } from '@/features/search/components/origin-link'
import { OperationError } from '@/features/file/components/operation-error'
import { isApiError } from '@/shared/api/errors'
import { Button } from '@/shared/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/shared/components/ui/dialog'

const colors: Record<Tag['color'], string> = {
  GRAY: 'bg-muted text-foreground',
  RED: 'bg-destructive/10 text-destructive',
  ORANGE: 'bg-warning/10 text-foreground',
  YELLOW: 'bg-warning/10 text-foreground',
  GREEN: 'bg-success/10 text-foreground',
  BLUE: 'bg-primary/10 text-primary',
  PURPLE: 'bg-primary/10 text-foreground',
  PINK: 'bg-destructive/10 text-foreground',
}
export function TagChips({ tags }: { tags: readonly Tag[] }) {
  const origin = useOrigin()
  return (
    <div className="flex flex-wrap gap-1.5">
      {tags.map((tag) => (
        <Link
          key={tag.id}
          to="/app/search"
          search={{ tagIds: [tag.id], tagMode: 'ALL', origin }}
          className={`max-w-full break-all rounded border border-current/10 px-2 py-0.5 text-xs ${colors[tag.color] ?? colors.GRAY}`}
        >
          {tag.name}
        </Link>
      ))}
    </div>
  )
}
export function ObjectTags({
  resource,
  id,
  readOnly = false,
}: {
  resource: TagResource
  id: string
  readOnly?: boolean
}) {
  const { data: session } = useSession()
  const workspaceId = session?.workspace.id ?? ''
  const client = useQueryClient()
  const key = ['content', workspaceId, 'tags', resource, id]
  const query = useQuery({
    queryKey: key,
    queryFn: () => tagApi.collection(resource, id),
    enabled: Boolean(workspaceId),
  })
  const [open, setOpen] = useState(false)
  const [selection, setSelection] = useState<string[]>([])
  const [version, setVersion] = useState(0)
  const save = useMutation({
    mutationFn: () => tagApi.replace(resource, id, selection, version),
    onSuccess: (collection) => {
      client.setQueryData(key, collection)
      void client.invalidateQueries({ queryKey: ['content', workspaceId] })
      setOpen(false)
    },
  })
  const conflict = isApiError(save.error) && save.error.code === 'TAG_VERSION_CONFLICT'
  return (
    <section className="grid gap-2" aria-label="内容标签">
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-sm font-medium">标签</h3>
        <Button
          size="sm"
          type="button"
          variant="ghost"
          disabled={readOnly || !query.data}
          title={readOnly ? '归档内容只读' : undefined}
          onClick={() => {
            setSelection(query.data!.tags.map((tag) => tag.id))
            setVersion(query.data!.tagVersion)
            save.reset()
            setOpen(true)
          }}
        >
          管理标签
        </Button>
      </div>
      {query.isPending ? (
        <p className="text-xs text-muted-foreground">正在加载标签…</p>
      ) : query.data?.tags.length ? (
        <TagChips tags={query.data.tags} />
      ) : (
        <p className="text-xs text-muted-foreground">暂无标签</p>
      )}
      <OperationError error={query.error} />
      <Dialog open={open} onOpenChange={(value) => !save.isPending && setOpen(value)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>管理标签</DialogTitle>
            <DialogDescription>标签单独保存，不影响正文草稿。</DialogDescription>
          </DialogHeader>
          <TagPicker
            selected={selection}
            onChange={setSelection}
            disabled={save.isPending || conflict}
          />
          <OperationError error={save.error} />
          {conflict && (
            <div className="grid gap-2">
              <p className="text-sm">标签已在其他位置修改，请重新加载后再次选择。</p>
              <Button
                type="button"
                variant="outline"
                onClick={async () => {
                  const result = await query.refetch()
                  if (result.data) {
                    setSelection(result.data.tags.map((tag) => tag.id))
                    setVersion(result.data.tagVersion)
                    save.reset()
                  }
                }}
              >
                重新加载标签
              </Button>
            </div>
          )}
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              disabled={save.isPending}
              onClick={() => setOpen(false)}
            >
              取消
            </Button>
            <Button
              type="button"
              disabled={save.isPending || conflict}
              onClick={() => save.mutate()}
            >
              {save.isPending ? '正在保存…' : '保存标签'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </section>
  )
}
