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
  tab?: 'tasks' | 'notes' | 'overview'
  taskPage?: number
  notePage?: number
  from?: ProjectListOrigin
} {
  const from = asSearchRecord(search.from)
  return {
    tab:
      search.tab === 'notes' || search.tab === 'overview' || search.tab === 'tasks'
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
