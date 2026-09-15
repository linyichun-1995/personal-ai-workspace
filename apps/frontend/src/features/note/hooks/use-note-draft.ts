import { useCallback, useEffect, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { updateNote } from '@/features/note/api/notes'
import type { Note } from '@/features/note/types'
import { isApiError, toErrorMessage } from '@/shared/api/errors'
import { queryKeys } from '@/shared/api/query-keys'

type Draft = Pick<Note, 'title' | 'favorite' | 'archived'> & { content: string }
export type SaveState = 'saved' | 'dirty' | 'saving' | 'error'

export function useNoteDraft(note: Note, workspaceId: string) {
  const queryClient = useQueryClient()
  const [draft, setDraft] = useState<Draft>({
    title: note.title,
    content: note.content ?? '',
    favorite: note.favorite,
    archived: note.archived,
  })
  const [state, setState] = useState<SaveState>('saved')
  const [error, setError] = useState('')
  const [conflict, setConflict] = useState(false)
  const draftRef = useRef(draft)
  const version = useRef(note.version)
  const revision = useRef(0)
  const savedRevision = useRef(0)
  const inFlight = useRef<Promise<boolean> | null>(null)
  const mounted = useRef(true)
  const hasConflict = useRef(false)

  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  const hasChanges = useCallback(() => revision.current !== savedRevision.current, [])
  const change = useCallback((patch: Partial<Draft>) => {
    draftRef.current = { ...draftRef.current, ...patch }
    revision.current += 1
    setDraft(draftRef.current)
    setState(hasConflict.current ? 'error' : 'dirty')
    if (!hasConflict.current) setError('')
  }, [])

  const save = useCallback((): Promise<boolean> => {
    if (inFlight.current) return inFlight.current
    if (hasConflict.current) return Promise.resolve(false)
    if (!hasChanges()) return Promise.resolve(true)
    const run = async () => {
      while (hasChanges() && mounted.current) {
        const input = { ...draftRef.current }
        const savingRevision = revision.current
        setState('saving')
        setError('')
        try {
          const updated = await updateNote(note.id, {
            ...input,
            projectId: note.projectId,
            version: version.current,
          })
          version.current = updated.version
          savedRevision.current = savingRevision
          queryClient.setQueryData(queryKeys.note.detail(workspaceId, note.id), updated)
          void queryClient.invalidateQueries({ queryKey: queryKeys.note.all(workspaceId) })
          void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.summary(workspaceId) })
          void queryClient.invalidateQueries({ queryKey: queryKeys.project.all(workspaceId) })
          void queryClient.invalidateQueries({ queryKey: ['project', workspaceId] })
        } catch (cause) {
          if (mounted.current) {
            hasConflict.current = isApiError(cause) && cause.code === 'VERSION_CONFLICT'
            setConflict(hasConflict.current)
            setError(
              hasConflict.current
                ? '这篇笔记已在其他位置更新。你的输入仍在这里，请先复制内容，再重新打开笔记核对。'
                : toErrorMessage(cause),
            )
            setState('error')
          }
          return false
        }
      }
      if (mounted.current && !hasChanges()) setState('saved')
      return !hasChanges()
    }
    const request = run()
    inFlight.current = request
    void request.finally(() => {
      inFlight.current = null
    })
    return request
  }, [hasChanges, note.id, note.projectId, queryClient, workspaceId])

  useEffect(() => {
    if (!hasChanges() || conflict) return
    const timer = window.setTimeout(() => {
      void save()
    }, 800)
    return () => window.clearTimeout(timer)
  }, [draft, conflict, hasChanges, save])

  return { draft, change, state, error, conflict, save, hasChanges }
}
