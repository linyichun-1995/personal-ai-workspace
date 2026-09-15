import { asSearchRecord, readPage } from '@/shared/lib/route-search'
import {
  validateProjectDetailSearch,
  type ProjectListOrigin,
} from '@/features/project/lib/project-search'

export interface NoteListSearch {
  view?: 'favorite' | 'archived'
  q?: string
  page?: number
}

export function validateNoteListSearch(search: Record<string, unknown>): NoteListSearch {
  return {
    view: search.view === 'favorite' || search.view === 'archived' ? search.view : undefined,
    q: typeof search.q === 'string' ? search.q.slice(0, 300) || undefined : undefined,
    page: readPage(search.page),
  }
}

export function validateNoteDetailSearch(search: Record<string, unknown>): {
  from?: NoteListSearch
  project?: { page?: number; from?: ProjectListOrigin }
} {
  const project = asSearchRecord(search.project)
  return {
    from: search.from ? validateNoteListSearch(asSearchRecord(search.from)) : undefined,
    project: search.project
      ? { page: readPage(project.page), from: validateProjectDetailSearch(project).from }
      : undefined,
  }
}
