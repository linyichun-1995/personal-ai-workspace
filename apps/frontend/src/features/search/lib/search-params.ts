import { readPage } from '@/shared/lib/route-search'

export const SOURCE_TYPES = { PROJECT: '项目', TASK: '任务', NOTE: '笔记', FILE: '文件' } as const
export interface ContentFilters {
  q?: string
  projectId?: string
  tagIds?: string[]
  tagMode?: 'ALL' | 'ANY'
  updatedFrom?: string
  updatedTo?: string
  includeArchived?: boolean
  sort?: string
  page?: number
}
export interface GlobalSearch extends ContentFilters {
  types?: (keyof typeof SOURCE_TYPES)[]
  origin?: string
}
const uuid = /^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i
export function stringArray(value: unknown): string[] {
  if (Array.isArray(value)) return value.filter((item): item is string => typeof item === 'string')
  return typeof value === 'string' && value ? value.split(',') : []
}
export function validateContentFilters(search: Record<string, unknown>): ContentFilters {
  const date = (value: unknown) =>
    typeof value === 'string' && Number.isFinite(Date.parse(value))
      ? new Date(value).toISOString()
      : undefined
  return {
    q: typeof search.q === 'string' ? [...search.q].slice(0, 100).join('') || undefined : undefined,
    projectId:
      typeof search.projectId === 'string' && uuid.test(search.projectId)
        ? search.projectId
        : undefined,
    tagIds: stringArray(search.tagIds)
      .filter((id) => uuid.test(id))
      .slice(0, 20),
    tagMode: search.tagMode === 'ANY' ? 'ANY' : 'ALL',
    updatedFrom: date(search.updatedFrom),
    updatedTo: date(search.updatedTo),
    includeArchived:
      search.includeArchived === true || search.includeArchived === 'true' ? true : undefined,
    page: readPage(search.page),
  }
}
export function validateGlobalSearch(search: Record<string, unknown>): GlobalSearch {
  const filters = validateContentFilters(search)
  return {
    ...filters,
    page: filters.page ? Math.min(100, filters.page) : undefined,
    types: stringArray(search.types).filter((type): type is keyof typeof SOURCE_TYPES =>
      Object.hasOwn(SOURCE_TYPES, type),
    ),
    sort: search.sort === 'updatedAt,desc' ? 'updatedAt,desc' : 'relevance',
    origin: safeOrigin(search.origin),
  }
}
/** Only known application destinations and context parameters can survive a return link. */
export function safeOrigin(value: unknown): string | undefined {
  if (
    typeof value !== 'string' ||
    !value.startsWith('/app/') ||
    value.includes('\\') ||
    value.length > 5000
  )
    return
  try {
    const url = new URL(value, 'https://workspace.invalid')
    if (
      url.origin !== 'https://workspace.invalid' ||
      !/^\/app\/(?:search|files(?:\/[\da-f-]{36})?|notes(?:\/[\da-f-]{36})?|projects(?:\/(?:active|archived|[\da-f-]{36}))?|tasks(?:\/(?:today|upcoming|completed|overdue))?)$/.test(
        url.pathname,
      )
    )
      return
    const allowed = new Set([
      'q',
      'view',
      'page',
      'types',
      'projectId',
      'tagIds',
      'tagMode',
      'updatedFrom',
      'updatedTo',
      'includeArchived',
      'sort',
      'mediaTypes',
      'extractionStatus',
      'tab',
      'taskPage',
      'notePage',
      'filePage',
      'taskId',
      'from',
      'project',
    ])
    for (const key of [...url.searchParams.keys()])
      if (!allowed.has(key)) url.searchParams.delete(key)
    return url.pathname + url.search
  } catch {
    return
  }
}
export function queryValidation(q?: string): string | undefined {
  const terms = (q ?? '').normalize('NFKC').trim().split(/\s+/).filter(Boolean)
  if (terms.length > 8) return '最多输入 8 个查询项'
  if (terms.some((term) => [...term].length < 2)) return '每个关键词至少输入 2 个字符后搜索'
  return undefined
}
