import type { QueryParams } from '@/shared/types/api'
import { env } from '@/app/config/env'
import { ApiError } from '@/shared/api/errors'

export interface StreamEvent<T = unknown> {
  event: string
  data: T
  id?: string
}

export interface ConsumeSseOptions<T> {
  query?: QueryParams
  headers?: HeadersInit
  signal?: AbortSignal
  onEvent: (event: StreamEvent<T>) => void
  onOpen?: () => void
}

function toSearch(query?: QueryParams): string {
  if (!query) {
    return ''
  }

  const search = new URLSearchParams()

  for (const [key, value] of Object.entries(query)) {
    if (value === null || value === undefined || value === '') {
      continue
    }

    search.set(key, String(value))
  }

  const serialized = search.toString()
  return serialized.length > 0 ? `?${serialized}` : ''
}

function parseSseBlock<T>(block: string): StreamEvent<T> | null {
  const lines = block.split('\n')
  let event = 'message'
  let id: string | undefined
  const dataLines: string[] = []

  for (const line of lines) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
      continue
    }

    if (line.startsWith('id:')) {
      id = line.slice(3).trim()
      continue
    }

    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }

  if (dataLines.length === 0) {
    return null
  }

  const raw = dataLines.join('\n')
  let data: T

  try {
    data = JSON.parse(raw) as T
  }
  catch {
    data = raw as T
  }

  return { event, data, id }
}

export async function consumeSseStream<T = unknown>(
  path: string,
  options: ConsumeSseOptions<T>,
): Promise<void> {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const url = `${env.apiBaseUrl}${normalizedPath}${toSearch(options.query)}`

  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'text/event-stream',
      ...options.headers,
    },
    signal: options.signal,
  })

  if (!response.ok) {
    throw new ApiError({
      status: response.status,
      code: 'STREAM_ERROR',
      message: response.statusText || 'SSE stream failed',
      traceId: response.headers.get('x-trace-id') ?? undefined,
    })
  }

  if (!response.body) {
    throw new ApiError({
      status: 500,
      code: 'STREAM_EMPTY',
      message: 'SSE response body is empty',
    })
  }

  options.onOpen?.()

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()

    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() ?? ''

    for (const block of blocks) {
      const parsed = parseSseBlock<T>(block)
      if (parsed) {
        options.onEvent(parsed)
      }
    }
  }
}
