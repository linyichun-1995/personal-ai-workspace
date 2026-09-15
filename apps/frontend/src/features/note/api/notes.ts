import type { Note } from '@/features/note/types'
import type { PaginatedResult } from '@/shared/types/api'
import { api } from '@/shared/api'

export interface NoteListQuery {
  projectId?: string
  favorite?: boolean
  archived?: boolean
  keyword?: string
  page?: number
  size?: number
  sort?: string
}

export function listNotes(query: NoteListQuery = {}): Promise<PaginatedResult<Note>> {
  return api.get<PaginatedResult<Note>>('/v1/notes', {
    query: {
      projectId: query.projectId,
      favorite: query.favorite,
      archived: query.archived,
      keyword: query.keyword,
      page: query.page,
      size: query.size,
      sort: query.sort,
    },
  })
}

export function getNote(noteId: string): Promise<Note> {
  return api.get<Note>(`/v1/notes/${noteId}`)
}

export function createNote(input: { projectId?: string | null, title?: string, content?: string }): Promise<Note> {
  return api.post<Note>('/v1/notes', input)
}

export function updateNote(noteId: string, input: {
  projectId?: string | null
  title?: string
  content?: string
  favorite?: boolean
  archived?: boolean
  version: number
}): Promise<Note> {
  return api.put<Note>(`/v1/notes/${noteId}`, input)
}

export function deleteNote(noteId: string): Promise<void> {
  return api.delete(`/v1/notes/${noteId}`)
}
