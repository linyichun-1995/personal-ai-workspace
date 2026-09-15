import { Link, getRouteApi, useNavigate } from '@tanstack/react-router'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Search, Star, X } from 'lucide-react'
import { useEffect, useState } from 'react'

import { useSession } from '@/features/auth/hooks/use-session'
import { listNotes, updateNote } from '@/features/note/api/notes'
import { CreateNoteButton } from '@/features/note/components/create-note-button'
import type { Note } from '@/features/note/types'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { queryKeys } from '@/shared/api/query-keys'
import { ListPagination } from '@/shared/components/list-pagination'
import { PageContainer } from '@/shared/components/page-container'
import { PageHeader } from '@/shared/components/page-header'
import { QueryState } from '@/shared/components/query-state'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { formatDateTime } from '@/shared/lib/datetime'

const routeApi = getRouteApi('/app/notes')

export function NotesPage() {
  const { data: session } = useSession()
  const workspaceId = session?.workspace.id
  const queryClient = useQueryClient()
  const search = routeApi.useSearch()
  const navigate = useNavigate()
  const keyword = search.q ?? ''
  const view = search.view ?? 'all'
  const page = search.page ?? 1
  const [searchTerm, setSearchTerm] = useState(keyword.trim())
  const searchPending = keyword.trim() !== searchTerm
  const setKeyword = (value: string) =>
    void navigate({
      to: '/app/notes',
      search: { ...search, q: value || undefined, page: undefined },
      replace: true,
      resetScroll: false,
    })
  const setPage = (next: number) =>
    void navigate({
      to: '/app/notes',
      search: { ...search, page: next > 1 ? next : undefined },
    })
  useEffect(() => {
    const timer = window.setTimeout(() => {
      setSearchTerm(keyword.trim())
    }, 300)
    return () => window.clearTimeout(timer)
  }, [keyword])
  const query = useQuery({
    queryKey: queryKeys.note.list(workspaceId ?? '', { searchTerm, view, page }),
    enabled: Boolean(workspaceId) && !searchPending,
    queryFn: () =>
      listNotes({
        keyword: searchTerm || undefined,
        archived: view === 'archived',
        favorite: view === 'favorite' ? true : undefined,
        page,
        size: 20,
        sort: 'updatedAt,desc',
      }),
    placeholderData: keepPreviousData,
  })
  const update = useMutation({
    mutationFn: ({
      note,
      patch,
    }: {
      note: Note
      patch: { favorite?: boolean; archived?: boolean }
    }) => updateNote(note.id, { ...patch, projectId: note.projectId, version: note.version }),
    onSuccess: (_note, { patch }) => {
      const leavesView =
        patch.archived !== undefined || (view === 'favorite' && patch.favorite === false)
      if (leavesView && query.data?.items.length === 1 && page > 1) setPage(page - 1)
      return workspaceId ? invalidateWorkspaceData(queryClient, workspaceId) : undefined
    },
  })

  return (
    <PageContainer className="grid gap-5">
      <PageHeader
        title="笔记"
        description="记录想法和工作结论，按最近更新时间排列。重要内容可以收藏。"
        actions={<CreateNoteButton from={search} />}
      />
      <nav className="flex gap-1 border-b border-border-subtle" aria-label="笔记视图">
        {(
          [
            { value: 'all', label: '全部笔记' },
            { value: 'favorite', label: '已收藏' },
            { value: 'archived', label: '已归档' },
          ] as const
        ).map((item) => (
          <Link
            key={item.value}
            to="/app/notes"
            search={{
              ...search,
              view: item.value === 'all' ? undefined : item.value,
              page: undefined,
            }}
            aria-current={view === item.value ? 'page' : undefined}
            className={
              'border-b-2 px-3 py-3 text-sm focus-visible:ring-2 focus-visible:ring-ring/40 ' +
              (view === item.value
                ? 'border-primary font-medium text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground')
            }
          >
            {item.label}
          </Link>
        ))}
      </nav>
      <div className="relative max-w-lg">
        <Search
          className="pointer-events-none absolute left-3 top-2.5 size-4 text-muted-foreground"
          aria-hidden="true"
        />
        <Input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="搜索标题或正文…"
          aria-label="搜索笔记"
          maxLength={300}
          className="pl-9 pr-10"
        />
        {keyword && (
          <Button
            variant="ghost"
            size="icon-sm"
            className="absolute right-1 top-0.5"
            aria-label="清空搜索"
            onClick={() => setKeyword('')}
          >
            <X />
          </Button>
        )}
      </div>
      <div
        aria-busy={query.isFetching || searchPending}
        className={query.isPlaceholderData || searchPending ? 'opacity-60' : ''}
      >
        <QueryState
          query={query}
          isEmpty={(data) => data.items.length === 0}
          empty={{
            title:
              page > 1
                ? '这一页没有笔记'
                : searchTerm
                  ? '没有匹配的笔记'
                  : view === 'favorite'
                    ? '还没有收藏笔记'
                    : view === 'archived'
                      ? '还没有归档笔记'
                      : '把想法留在这里',
            description:
              page > 1
                ? '返回第一页继续查看笔记。'
                : searchTerm
                  ? '换一个关键词，或清空搜索查看当前分区的全部笔记。'
                  : view === 'favorite'
                    ? '点击笔记旁的星标，重要内容会集中在这里。'
                    : view === 'archived'
                      ? '暂时不用的笔记可以归档，需要时随时恢复。'
                      : '从一条想法、一份会议记录或一个方案开始。',
            action:
              page > 1 ? undefined : searchTerm ? (
                <Button variant="outline" onClick={() => setKeyword('')}>
                  清空搜索
                </Button>
              ) : view === 'all' ? (
                <CreateNoteButton from={search} />
              ) : undefined,
          }}
        >
          {(data) => (
            <div className="grid gap-2">
              {data.items.map((note) => (
                <div
                  key={note.id}
                  className="flex items-center gap-2 rounded-lg border border-border-subtle bg-card p-3 hover:border-border"
                >
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    disabled={update.isPending || query.isPlaceholderData || searchPending}
                    aria-pressed={note.favorite}
                    aria-label={(note.favorite ? '取消收藏：' : '收藏：') + note.title}
                    onClick={() => update.mutate({ note, patch: { favorite: !note.favorite } })}
                  >
                    <Star
                      className={
                        note.favorite
                          ? 'size-4 fill-current text-warning'
                          : 'size-4 text-muted-foreground'
                      }
                    />
                  </Button>
                  <Link
                    to="/app/notes/$noteId"
                    params={{ noteId: note.id }}
                    search={{ from: search }}
                    className="min-w-0 flex-1 rounded-sm py-1 focus-visible:ring-2 focus-visible:ring-ring/40"
                  >
                    <p className="truncate font-medium hover:text-primary">{note.title}</p>
                    <p className="mt-1 truncate text-xs text-muted-foreground">
                      {note.summary || '打开笔记，继续记录…'}
                    </p>
                    <p className="mt-1 text-[11px] text-muted-foreground">
                      更新于 {formatDateTime(note.updatedAt)}
                    </p>
                  </Link>
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={update.isPending || query.isPlaceholderData || searchPending}
                    onClick={() => update.mutate({ note, patch: { archived: !note.archived } })}
                  >
                    {note.archived ? '恢复' : '归档'}
                  </Button>
                </div>
              ))}
            </div>
          )}
        </QueryState>
      </div>
      {query.data && (
        <ListPagination
          page={page}
          total={query.data.total}
          totalPages={query.data.totalPages}
          loading={query.isFetching || searchPending}
          onPageChange={setPage}
        />
      )}
    </PageContainer>
  )
}
