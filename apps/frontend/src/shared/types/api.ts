export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export interface ApiErrorBody {
  code: string
  message: string
  details?: unknown
  traceId?: string
}

export interface PaginatedResult<T> {
  items: readonly T[]
  total: number
  page: number
  size: number
  totalPages: number
}

export interface QueryParams {
  [key: string]: string | number | boolean | null | undefined
}
