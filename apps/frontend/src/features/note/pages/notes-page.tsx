import { Link, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Star } from 'lucide-react'
import { useState } from 'react'

import { useSession } from '@/features/auth/hooks/use-session'
import { createNote, listNotes, updateNote } from '@/features/note/api/notes'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { queryKeys } from '@/shared/api/query-keys'
import { PageContainer } from '@/shared/components/page-container'
import { PageHeader } from '@/shared/components/page-header'
import { QueryState } from '@/shared/components/query-state'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { formatDateTime } from '@/shared/lib/datetime'

export function NotesPage() {
  const session = useSession()
  const workspaceId = session.data?.workspace.id
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [keyword, setKeyword] = useState('')
  const [archived, setArchived] = useState(false)

  const query = useQuery({
    queryKey: queryKeys.note.list(workspaceId ?? '', { keyword, archived }),
    enabled: Boolean(workspaceId),
    queryFn: () => listNotes({
      keyword: keyword || undefined,
      archived,
      size: 50,
      sort: 'updatedAt,desc',
    }),
  })

  const createMutation = useMutation({
    mutationFn: createNote,
    onSuccess: async (note) => {
      if (workspaceId) {
        invalidateWorkspaceData(queryClient, workspaceId)
      }
      await navigate({ to: '/app/notes/$noteId', params: { noteId: note.id } })
    },
  })

  const favoriteMutation = useMutation({
    mutationFn: ({ id, favorite, version, title, projectId }: { id: string, favorite: boolean, version: number, title: string, projectId: string | null }) =>
      updateNote(id, { favorite, version, title, projectId }),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })
  const archiveMutation = useMutation({
    mutationFn: ({ id, archived: nextArchived, version, title, projectId }: { id: string, archived: boolean, version: number, title: string, projectId: string | null }) =>
      updateNote(id, { archived: nextArchived, version, title, projectId }),
    onSuccess: () => workspaceId && invalidateWorkspaceData(queryClient, workspaceId),
  })

  return (
    <PageContainer className="grid gap-5">
      <PageHeader
        eyebrow="工作台 / 笔记"
        title="笔记"
        description="用 Markdown 记录项目背景、方案和会议内容，停止输入后会自动保存。"
        actions={(
          <Button onClick={() => createMutation.mutate({ title: '无标题' })}>
            <Plus className="size-4" />
            新建笔记
          </Button>
        )}
      />
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <Input
          value={keyword}
          onChange={event => setKeyword(event.target.value)}
          placeholder="搜索标题或正文"
          aria-label="搜索笔记"
        />
        <Button variant={archived ? 'default' : 'outline'} onClick={() => setArchived(current => !current)}>
          {archived ? '查看未归档' : '查看已归档'}
        </Button>
      </div>
      <QueryState
        query={query}
        isEmpty={data => data.items.length === 0}
        empty={{
          title: archived ? '还没有归档笔记' : '还没有笔记',
          description: archived ? '归档后的笔记会出现在这里。' : '写一篇笔记，记录当前工作上下文。',
          action: archived
            ? undefined
            : <Button onClick={() => createMutation.mutate({ title: '无标题' })}>新建笔记</Button>,
        }}
      >
        {data => (
          <div className="grid gap-2">
            {data.items.map(note => (
              <div key={note.id} className="flex items-center gap-2 rounded-lg border border-border-subtle bg-card px-3 py-3">
                <button
                  type="button"
                  aria-label={note.favorite ? '取消收藏' : '收藏'}
                  className="text-muted-foreground hover:text-warning"
                  onClick={() => favoriteMutation.mutate({
                    id: note.id,
                    favorite: !note.favorite,
                    version: note.version,
                    title: note.title,
                    projectId: note.projectId,
                  })}
                >
                  <Star className={note.favorite ? 'size-4 fill-current text-warning' : 'size-4'} />
                </button>
                <Link to="/app/notes/$noteId" params={{ noteId: note.id }} className="min-w-0 flex-1">
                  <p className="truncate font-medium">{note.title}</p>
                  <p className="truncate text-xs text-muted-foreground">
                    {note.summary || '暂无摘要'}
                    {' · '}
                    {formatDateTime(note.updatedAt)}
                  </p>
                </Link>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => archiveMutation.mutate({
                    id: note.id,
                    archived: !note.archived,
                    version: note.version,
                    title: note.title,
                    projectId: note.projectId,
                  })}
                >
                  {note.archived ? '恢复' : '归档'}
                </Button>
              </div>
            ))}
          </div>
        )}
      </QueryState>
    </PageContainer>
  )
}
