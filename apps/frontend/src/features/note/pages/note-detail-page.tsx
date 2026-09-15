import { getRouteApi, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

import { useSession } from '@/features/auth/hooks/use-session'
import { deleteNote, getNote, updateNote } from '@/features/note/api/notes'
import type { Note } from '@/features/note/types'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { queryKeys } from '@/shared/api/query-keys'
import { PageContainer } from '@/shared/components/page-container'
import { QueryState } from '@/shared/components/query-state'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/shared/components/ui/alert-dialog'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Textarea } from '@/shared/components/ui/textarea'

const routeApi = getRouteApi('/app/notes/$noteId')

type SaveState = 'saved' | 'saving' | 'dirty' | 'error'

export function NoteDetailPage() {
  const { noteId } = routeApi.useParams()
  const session = useSession()
  const workspaceId = session.data?.workspace.id
  const query = useQuery({
    queryKey: queryKeys.note.detail(workspaceId ?? '', noteId),
    enabled: Boolean(workspaceId),
    queryFn: () => getNote(noteId),
  })

  return (
    <PageContainer width="reading">
      <QueryState query={query}>
        {note => <NoteEditor key={note.id} note={note} />}
      </QueryState>
    </PageContainer>
  )
}

function NoteEditor({ note }: { note: Note }) {
  const session = useSession()
  const workspaceId = session.data?.workspace.id
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [title, setTitle] = useState(note.title)
  const [content, setContent] = useState(note.content ?? '')
  const [favorite, setFavorite] = useState(note.favorite)
  const [archived, setArchived] = useState(note.archived)
  const [preview, setPreview] = useState(false)
  const [saveState, setSaveState] = useState<SaveState>('saved')
  const [deleteOpen, setDeleteOpen] = useState(false)
  const versionRef = useRef(note.version)
  const skipFirstSave = useRef(true)
  const persistRef = useRef<(nextTitle: string, nextContent: string, nextFavorite: boolean, nextArchived: boolean) => Promise<void>>(async () => {})

  useEffect(() => {
    persistRef.current = async (nextTitle, nextContent, nextFavorite, nextArchived) => {
      setSaveState('saving')
      try {
        const updated = await updateNote(note.id, {
          title: nextTitle,
          content: nextContent,
          favorite: nextFavorite,
          archived: nextArchived,
          projectId: note.projectId,
          version: versionRef.current,
        })
        versionRef.current = updated.version
        setSaveState('saved')
        if (workspaceId) {
          queryClient.setQueryData(queryKeys.note.detail(workspaceId, note.id), updated)
          void queryClient.invalidateQueries({ queryKey: queryKeys.note.all(workspaceId) })
          void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.summary(workspaceId) })
          void queryClient.invalidateQueries({ queryKey: ['project', workspaceId] })
        }
      }
      catch {
        setSaveState('error')
      }
    }
  })

  useEffect(() => {
    if (skipFirstSave.current) {
      skipFirstSave.current = false
      return
    }
    const timer = window.setTimeout(() => {
      void persistRef.current(title, content, favorite, archived)
    }, 800)
    return () => window.clearTimeout(timer)
  }, [title, content, favorite, archived])

  const deleteMutation = useMutation({
    mutationFn: () => deleteNote(note.id),
    onSuccess: async () => {
      if (workspaceId) {
        invalidateWorkspaceData(queryClient, workspaceId)
      }
      await navigate({ to: '/app/notes' })
    },
  })

  const saveLabel = saveState === 'saving' ? '正在保存...' : saveState === 'error' ? '保存失败' : saveState === 'dirty' ? '未保存' : '已保存'

  return (
    <div className="grid gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-xs text-muted-foreground" aria-live="polite">{saveLabel}</p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => setPreview(current => !current)}>
            {preview ? '编辑' : '预览'}
          </Button>
          <Button variant="outline" size="sm" onClick={() => {
            setFavorite(current => !current)
            setSaveState('dirty')
          }}>
            {favorite ? '取消收藏' : '收藏'}
          </Button>
          <Button variant="outline" size="sm" onClick={() => {
            setArchived(current => !current)
            setSaveState('dirty')
          }}>
            {archived ? '取消归档' : '归档'}
          </Button>
          <Button variant="destructive" size="sm" onClick={() => setDeleteOpen(true)}>删除</Button>
        </div>
      </div>
      <Input
        value={title}
        onChange={(event) => {
          setTitle(event.target.value)
          setSaveState('dirty')
        }}
        aria-label="笔记标题"
        className="text-lg font-semibold"
      />
      {preview
        ? (
            <div className="prose prose-neutral dark:prose-invert max-w-none rounded-lg border border-border-subtle bg-card px-4 py-3 text-sm">
              <Markdown remarkPlugins={[remarkGfm]}>{content || '*暂无内容*'}</Markdown>
            </div>
          )
        : (
            <Textarea
              value={content}
              onChange={(event) => {
                setContent(event.target.value)
                setSaveState('dirty')
              }}
              aria-label="笔记正文"
              className="min-h-[28rem] font-mono text-sm"
            />
          )}
      {saveState === 'error'
        ? (
            <Button variant="outline" onClick={() => void persistRef.current(title, content, favorite, archived)}>重试保存</Button>
          )
        : null}
      <AlertDialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>删除这篇笔记？</AlertDialogTitle>
            <AlertDialogDescription>删除后不会出现在列表中。</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={() => deleteMutation.mutate()}>确认删除</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
