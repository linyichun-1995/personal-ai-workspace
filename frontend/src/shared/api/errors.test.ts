import { describe, expect, it } from 'vitest'

import { ApiError, parseApiErrorBody, toErrorMessage } from '@/shared/api/errors'

describe('apiError', () => {
  it('exposes status helpers', () => {
    const error = new ApiError({
      status: 401,
      code: 'UNAUTHORIZED',
      message: 'Session expired',
    })

    expect(error.isUnauthorized).toBe(true)
    expect(error.isForbidden).toBe(false)
  })
})

describe('parseApiErrorBody', () => {
  it('reads a nested error payload', () => {
    const parsed = parseApiErrorBody({
      error: {
        code: 'VALIDATION',
        message: 'Invalid input',
        traceId: 'abc',
      },
    })

    expect(parsed).toEqual({
      code: 'VALIDATION',
      message: 'Invalid input',
      details: undefined,
      traceId: 'abc',
    })
  })

  it('reads requestId from the backend error payload', () => {
    const parsed = parseApiErrorBody({
      code: 'AUTH_INVALID_CREDENTIALS',
      message: '邮箱或密码错误',
      requestId: 'req-1',
    })

    expect(parsed?.traceId).toBe('req-1')
  })
})

describe('toErrorMessage', () => {
  it('falls back for unknown values', () => {
    expect(toErrorMessage('nope')).toBe('发生未知错误')
  })
})
