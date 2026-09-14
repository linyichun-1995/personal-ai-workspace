import type { ApiErrorBody } from '@/shared/types/api'

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details?: unknown
  readonly traceId?: string

  constructor(input: {
    status: number
    code: string
    message: string
    details?: unknown
    traceId?: string
  }) {
    super(input.message)
    this.name = 'ApiError'
    this.status = input.status
    this.code = input.code
    this.details = input.details
    this.traceId = input.traceId
  }

  get isUnauthorized(): boolean {
    return this.status === 401
  }

  get isForbidden(): boolean {
    return this.status === 403
  }

  get isNotFound(): boolean {
    return this.status === 404
  }

  get isNotImplemented(): boolean {
    return this.status === 501 || this.code === 'NOT_IMPLEMENTED'
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

export function parseApiErrorBody(value: unknown): ApiErrorBody | null {
  if (!isRecord(value)) {
    return null
  }

  const nested = isRecord(value.error) ? value.error : value
  const code = nested.code
  const message = nested.message

  if (typeof code !== 'string' || typeof message !== 'string') {
    return null
  }

  return {
    code,
    message,
    details: nested.details,
    traceId: typeof nested.traceId === 'string'
      ? nested.traceId
      : typeof nested.requestId === 'string'
        ? nested.requestId
        : undefined,
  }
}

export function toErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    return error.message
  }

  if (error instanceof Error) {
    return error.message
  }

  return '发生未知错误'
}
