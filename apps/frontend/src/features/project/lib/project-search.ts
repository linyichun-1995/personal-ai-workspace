import { safeOrigin } from '@/features/search/lib/search-params'
import { asSearchRecord, readPage } from '@/shared/lib/route-search'

export function validateProjectListSearch(search: Record<string, unknown>): {
  create?: boolean
  page?: number
} {
  return { create: search.create === true ? true : undefined, page: readPage(search.page) }
}

export interface ProjectListOrigin {
  view?: 'active' | 'archived'
  page?: number
}

export function validateProjectDetailSearch(search: Record<string, unknown>): {
  origin?: string
  tab?: 'tasks' | 'notes' | 'overview' | 'files'
  taskPage?: number
  notePage?: number
  from?: ProjectListOrigin
} {
  const from = asSearchRecord(search.from)
  return {
    origin: safeOrigin(search.origin),
    tab:
      search.tab === 'files' ||
      search.tab === 'notes' ||
      search.tab === 'overview' ||
      search.tab === 'tasks'
        ? search.tab
        : undefined,
    taskPage: readPage(search.taskPage),
    notePage: readPage(search.notePage),
    from: search.from
      ? {
          view: from.view === 'active' || from.view === 'archived' ? from.view : undefined,
          page: readPage(from.page),
        }
      : undefined,
  }
}
