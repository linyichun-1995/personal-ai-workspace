import { Link, getRouteApi, useBlocker, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Check, Copy, Eye, Loader2, Pencil, Save, Star } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { toast } from 'sonner'

import { useSession } from '@/features/auth/hooks/use-session'
import { deleteNote, getNote } from '@/features/note/api/notes'
import { getProject } from '@/features/project/api/projects'
import { useNoteDraft } from '@/features/note/hooks/use-note-draft'
import type { Note } from '@/features/note/types'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { queryKeys } from '@/shared/api/query-keys'
import { PageContainer } from '@/shared/components/page-container'
import { QueryState } from '@/shared/components/query-state'
import {
  AlertDialog,
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

export function NoteDetailPage() {
  const { noteId } = routeApi.useParams()
  const { data: session } = useSession()
  const workspaceId = session?.workspace.id
  const query = useQuery({
    queryKey: queryKeys.note.detail(workspaceId ?? '', noteId),
    enabled: Boolean(workspaceId),
    queryFn: () => getNote(noteId),
  })
  return (
    <PageContainer width="reading">
      <QueryState query={query}>
        {(note) => <NoteEditor key={note.id} note={note} workspaceId={workspaceId!} />}
      </QueryState>
    </PageContainer>
  )
}

function NoteEditor({ note, workspaceId }: { note: Note; workspaceId: string }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const search = routeApi.useSearch()
  const editor = useNoteDraft(note, workspaceId)
  const { save } = editor
  const project = useQuery({
    queryKey: queryKeys.project.detail(workspaceId, note.projectId ?? ''),
    enabled: Boolean(note.projectId),
    queryFn: () => getProject(note.projectId!),
  })
  const readOnly = Boolean(note.projectId && (!project.data || project.data.archivedAt))
  const [preview, setPreview] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const allowLeave = useRef(false)
  const blocker = useBlocker({
    shouldBlockFn: async () => !allowLeave.current && editor.hasChanges() && !(await editor.save()),
    enableBeforeUnload: () => !allowLeave.current && editor.hasChanges(),
    withResolver: true,
  })

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
        event.preventDefault()
        void save()
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [save])

  const remove = useMutation({
    mutationFn: () => deleteNote(note.id),
    onSuccess: async () => {
      allowLeave.current = true
      invalidateWorkspaceData(queryClient, workspaceId)
      toast.success('笔记已删除')
      if (search.project && note.projectId) {
        await navigate({
          to: '/app/projects/$projectId',
          params: { projectId: note.projectId },
          search: { tab: 'notes', notePage: search.project.page, from: search.project.from },
        })
      } else {
        await navigate({ to: '/app/notes', search: search.from ?? {} })
      }
    },
  })
  const labels = { saved: '已保存', saving: '正在保存…', dirty: '等待保存…', error: '尚未保存成功' }
  async function copyDraft() {
    try {
      await navigator.clipboard.writeText(editor.draft.title + '\n\n' + editor.draft.content)
      toast.success('已复制标题和正文')
    } catch {
      toast.error('复制失败，请在编辑区手动选择并复制内容')
    }
  }

  return (
    <div className="grid gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        {search.project && note.projectId ? (
          <Link
            to="/app/projects/$projectId"
            params={{ projectId: note.projectId }}
            search={{ tab: 'notes', notePage: search.project.page, from: search.project.from }}
            className="flex items-center gap-2 rounded-sm text-sm text-muted-foreground hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring/40"
          >
            <ArrowLeft className="size-4" /> 返回所属项目
          </Link>
        ) : (
          <Link
            to="/app/notes"
            search={search.from ?? {}}
            className="flex items-center gap-2 rounded-sm text-sm text-muted-foreground hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring/40"
          >
            <ArrowLeft className="size-4" /> 返回笔记
          </Link>
        )}
        {note.projectId && !search.project && (
          <Button variant="link" size="sm" asChild>
            <Link
              to="/app/projects/$projectId"
              params={{ projectId: note.projectId }}
              search={{ tab: 'notes' }}
            >
              查看所属项目
            </Link>
          </Button>
        )}
      </div>
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border-subtle bg-card px-3 py-2">
        <span
          className={
            'flex items-center gap-1.5 text-xs ' +
            (editor.state === 'error' ? 'text-destructive' : 'text-muted-foreground')
          }
          role="status"
          aria-live="polite"
        >
          {editor.state === 'saving' ? (
            <Loader2 className="size-3.5 animate-spin" aria-hidden="true" />
          ) : editor.state === 'saved' ? (
            <Check className="size-3.5 text-success" aria-hidden="true" />
          ) : null}
          {labels[editor.state]}
        </span>
        <div className="flex flex-wrap gap-1">
          <Button
            variant={preview ? 'secondary' : 'ghost'}
            size="sm"
            aria-pressed={preview}
            onClick={() => setPreview((current) => !current)}
          >
            {preview ? <Pencil /> : <Eye />}
            {preview ? '继续编辑' : '预览'}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={editor.state === 'saved' || editor.state === 'saving' || editor.conflict}
            onClick={() => void editor.save()}
          >
            <Save />
            保存
          </Button>
          <Button
            variant="ghost"
            size="sm"
            aria-pressed={editor.draft.favorite}
            disabled={readOnly}
            onClick={() => editor.change({ favorite: !editor.draft.favorite })}
          >
            <Star className={editor.draft.favorite ? 'fill-current text-warning' : ''} />
            {editor.draft.favorite ? '已收藏' : '收藏'}
          </Button>
        </div>
      </div>
      {editor.error && (
        <div
          role="alert"
          className="grid gap-3 rounded-lg border border-destructive/30 bg-destructive/5 p-4"
        >
          <p className="text-sm">{editor.error}</p>
          <p className="text-xs text-muted-foreground">
            本次输入仍保留在当前页面。请保存成功后再关闭浏览器。
          </p>
          <div className="flex flex-wrap gap-2">
            {!editor.conflict && (
              <Button variant="outline" size="sm" onClick={() => void editor.save()}>
                重试保存
              </Button>
            )}
            <Button variant="outline" size="sm" onClick={() => void copyDraft()}>
              <Copy />
              复制我的内容
            </Button>
          </div>
        </div>
      )}
      {editor.draft.archived && !readOnly && (
        <p className="rounded-lg bg-muted px-4 py-3 text-sm text-muted-foreground">
          这篇笔记已归档。可以继续阅读和编辑，或在下方恢复到笔记列表。
        </p>
      )}
      {readOnly && (
        <p
          role="status"
          className="rounded-lg border bg-muted px-4 py-3 text-sm text-muted-foreground"
        >
          {project.data?.archivedAt
            ? '所属项目已归档，笔记只读。返回项目并恢复后可继续编辑。'
            : project.isError
              ? '暂时无法读取所属项目状态。请重试后继续编辑。'
              : '正在读取所属项目状态…'}
          {project.isError && (
            <Button variant="link" size="sm" onClick={() => void project.refetch()}>
              重试
            </Button>
          )}
        </p>
      )}
      <Input
        value={editor.draft.title}
        disabled={readOnly}
        onFocus={(event) => {
          if (event.target.value === '无标题') event.target.select()
        }}
        onChange={(event) => editor.change({ title: event.target.value })}
        placeholder="无标题"
        aria-label="笔记标题"
        maxLength={300}
        className="h-12 border-transparent bg-transparent px-0 text-xl font-semibold shadow-none focus-visible:px-3"
      />
      {preview || readOnly ? (
        <article className="note-markdown min-h-96 break-words rounded-lg border border-border-subtle bg-card p-5 text-sm leading-7">
          <Markdown remarkPlugins={[remarkGfm]}>
            {editor.draft.content || '*这里还没有内容，切换到编辑开始记录。*'}
          </Markdown>
        </article>
      ) : (
        <Textarea
          value={editor.draft.content}
          onChange={(event) => editor.change({ content: event.target.value })}
          aria-label="笔记正文"
          placeholder="写下想法、会议结论或下一步计划…支持 Markdown。"
          maxLength={200_000}
          className="min-h-[55dvh] resize-y p-4 font-mono text-sm leading-7"
        />
      )}
      <footer className="flex flex-wrap items-center justify-between gap-3 border-t border-border-subtle pt-3 text-xs text-muted-foreground">
        <span>
          {editor.draft.content.length.toLocaleString()} 字符 · 停止输入后自动保存 · Ctrl / ⌘ S 保存
        </span>
        <div className="flex gap-1">
          <Button
            variant="ghost"
            size="sm"
            disabled={readOnly}
            onClick={() => editor.change({ archived: !editor.draft.archived })}
          >
            {editor.draft.archived ? '恢复到笔记列表' : '归档笔记'}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="text-destructive"
            disabled={editor.state !== 'saved' || readOnly}
            onClick={() => setDeleteOpen(true)}
          >
            删除笔记
          </Button>
        </div>
      </footer>
      <AlertDialog
        open={deleteOpen}
        onOpenChange={(value) => !remove.isPending && setDeleteOpen(value)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>删除“{editor.draft.title || '无标题'}”？</AlertDialogTitle>
            <AlertDialogDescription>
              笔记将从列表中移除。只是暂时不用，可以选择归档。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={remove.isPending}>取消</AlertDialogCancel>
            <Button
              variant="destructive"
              disabled={remove.isPending}
              onClick={() => remove.mutate()}
            >
              {remove.isPending ? '正在删除…' : '删除笔记'}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
      <AlertDialog
        open={blocker.status === 'blocked'}
        onOpenChange={(value) => {
          if (!value) blocker.reset?.()
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>修改还没有保存成功</AlertDialogTitle>
            <AlertDialogDescription>
              你的输入仍保留在页面中。继续编辑后重试，或放弃未保存的修改并离开。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => blocker.reset?.()}>继续编辑</AlertDialogCancel>
            <Button
              variant="outline"
              onClick={() => {
                allowLeave.current = true
                blocker.proceed?.()
              }}
            >
              放弃并离开
            </Button>
            <Button
              disabled={editor.state === 'saving' || editor.conflict}
              onClick={async () => {
                if (await editor.save()) blocker.proceed?.()
              }}
            >
              保存后离开
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
