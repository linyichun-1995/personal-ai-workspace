import type { HttpMethod, QueryParams } from '@/shared/types/api'
import { env } from '@/app/config/env'
import { ApiError, parseApiErrorBody } from '@/shared/api/errors'

export interface ApiRequestOptions extends Omit<RequestInit, 'body' | 'method'> {
  method?: HttpMethod
  body?: unknown
  query?: QueryParams
  parseJson?: boolean
}

type AuthExpiredHandler = () => void

let authExpiredHandler: AuthExpiredHandler | null = null

export function setAuthExpiredHandler(handler: AuthExpiredHandler | null): void {
  authExpiredHandler = handler
}

function joinUrl(baseUrl: string, path: string): string {
  const normalizedBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${normalizedBase}${normalizedPath}`
}

function appendQuery(url: string, query?: QueryParams): string {
  if (!query) {
    return url
  }

  const search = new URLSearchParams()

  for (const [key, value] of Object.entries(query)) {
    if (value === null || value === undefined || value === '') {
      continue
    }

    search.set(key, String(value))
  }

  const serialized = search.toString()
  return serialized.length > 0 ? `${url}?${serialized}` : url
}

function resolveHeaders(body: unknown, headers?: HeadersInit): Headers {
  const resolved = new Headers(headers)

  if (!resolved.has('Accept')) {
    resolved.set('Accept', 'application/json')
  }

  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData

  if (body !== undefined && !isFormData && !resolved.has('Content-Type')) {
    resolved.set('Content-Type', 'application/json')
  }

  return resolved
}

function serializeBody(body: unknown): BodyInit | undefined {
  if (body === undefined) {
    return undefined
  }

  if (typeof FormData !== 'undefined' && body instanceof FormData) {
    return body
  }

  if (typeof body === 'string' || body instanceof Blob) {
    return body
  }

  return JSON.stringify(body)
}

async function readError(response: Response): Promise<ApiError> {
  const traceId = response.headers.get('x-trace-id') ?? undefined

  try {
    const payload: unknown = await response.json()
    const parsed = parseApiErrorBody(payload)

    if (parsed) {
      return new ApiError({
        status: response.status,
        code: parsed.code,
        message: parsed.message,
        details: parsed.details,
        traceId: parsed.traceId ?? traceId,
      })
    }
  }
  catch {
    // Fall through to status text.
  }

  return new ApiError({
    status: response.status,
    code: 'HTTP_ERROR',
    message: response.statusText || 'Request failed',
    traceId,
  })
}

export async function apiClient<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const {
    method = 'GET',
    body,
    query,
    headers,
    parseJson = true,
    ...rest
  } = options

  const response = await fetch(appendQuery(joinUrl(env.apiBaseUrl, path), query), {
    ...rest,
    method,
    headers: resolveHeaders(body, headers),
    body: serializeBody(body),
    credentials: 'include',
  })

  if (response.status === 401) {
    authExpiredHandler?.()
  }

  if (!response.ok) {
    throw await readError(response)
  }

  if (response.status === 204 || !parseJson) {
    return undefined as T
  }

  return await response.json() as T
}

export const api = {
  get: <T>(path: string, options?: Omit<ApiRequestOptions, 'method' | 'body'>) =>
    apiClient<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options?: Omit<ApiRequestOptions, 'method' | 'body'>) =>
    apiClient<T>(path, { ...options, method: 'POST', body }),
  put: <T>(path: string, body?: unknown, options?: Omit<ApiRequestOptions, 'method' | 'body'>) =>
    apiClient<T>(path, { ...options, method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown, options?: Omit<ApiRequestOptions, 'method' | 'body'>) =>
    apiClient<T>(path, { ...options, method: 'PATCH', body }),
  delete: <T>(path: string, options?: Omit<ApiRequestOptions, 'method' | 'body'>) =>
    apiClient<T>(path, { ...options, method: 'DELETE' }),
}
