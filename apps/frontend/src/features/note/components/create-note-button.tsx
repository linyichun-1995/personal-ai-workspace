import type { ComponentProps } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { Loader2, NotebookPen } from 'lucide-react'

import { useSession } from '@/features/auth/hooks/use-session'
import { createNote } from '@/features/note/api/notes'
import type { NoteListSearch } from '@/features/note/lib/note-search'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { Button } from '@/shared/components/ui/button'

export function CreateNoteButton({
  onCreated,
  from,
  children,
  ...props
}: Omit<ComponentProps<typeof Button>, 'onClick' | 'asChild'> & {
  onCreated?: () => void
  from?: NoteListSearch
}) {
  const { data: session } = useSession()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const mutation = useMutation({
    mutationFn: () => createNote({ title: '无标题' }),
    onSuccess: async (note) => {
      if (session) invalidateWorkspaceData(queryClient, session.workspace.id)
      onCreated?.()
      await navigate({ to: '/app/notes/$noteId', params: { noteId: note.id }, search: { from } })
    },
  })
  return (
    <Button
      {...props}
      disabled={props.disabled || mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      {mutation.isPending ? (
        <Loader2 className="size-4 animate-spin" aria-hidden="true" />
      ) : (
        <NotebookPen className="size-4" aria-hidden="true" />
      )}
      {mutation.isPending ? '正在创建…' : (children ?? '新建笔记')}
    </Button>
  )
}
