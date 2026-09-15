import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAccessToken, getAccessToken, setAccessToken } from '@/features/auth/api/token-store'
import { api, setAuthExpiredHandler } from '@/shared/api/client'

function jsonResponse(body: unknown, status = 200, headers?: HeadersInit): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  })
}

function requestInit(calls: unknown[][], index: number): RequestInit {
  const init = calls[index]?.[1]
  if (!init || typeof init !== 'object') {
    throw new Error(`Missing fetch init at ${index}`)
  }
  return init as RequestInit
}

describe('apiClient auth refresh', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
    clearAccessToken()
    setAuthExpiredHandler(null)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    clearAccessToken()
    setAuthExpiredHandler(null)
  })

  it('keeps the access token in memory only', async () => {
    setAccessToken('access-1')
    fetchMock.mockResolvedValueOnce(jsonResponse({ id: 'u1' }))

    await api.get('/v1/me')

    expect(fetchMock).toHaveBeenCalledOnce()
    const headers = new Headers(requestInit(fetchMock.mock.calls, 0).headers)
    expect(headers.get('Authorization')).toBe('Bearer access-1')
    expect(getAccessToken()).toBe('access-1')
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(sessionStorage.getItem('accessToken')).toBeNull()
  })

  it('refreshes once and retries the original request after 401', async () => {
    setAccessToken('expired')
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ code: 'AUTH_TOKEN_EXPIRED', message: '令牌已过期' }, 401))
      .mockResolvedValueOnce(jsonResponse({ accessToken: 'fresh' }))
      .mockResolvedValueOnce(jsonResponse({ id: 'u1' }))

    const result = await api.get<{ id: string }>('/v1/me')

    expect(result).toEqual({ id: 'u1' })
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(String(fetchMock.mock.calls[1]?.[0])).toContain('/v1/auth/refresh')
    expect(fetchMock.mock.calls[1]?.[1]).toMatchObject({ credentials: 'include', method: 'POST' })
    expect(getAccessToken()).toBe('fresh')
    const retriedHeaders = new Headers(requestInit(fetchMock.mock.calls, 2).headers)
    expect(retriedHeaders.get('Authorization')).toBe('Bearer fresh')
  })

  it('shares one refresh across concurrent 401s', async () => {
    setAccessToken('expired')
    let refreshCalls = 0
    fetchMock.mockImplementation(async (input: RequestInfo | URL) => {
      const url = String(input)
      if (url.includes('/v1/auth/refresh')) {
        refreshCalls += 1
        await new Promise(resolve => setTimeout(resolve, 20))
        return jsonResponse({ accessToken: 'fresh' })
      }
      if (getAccessToken() === 'fresh') {
        return jsonResponse({ ok: true })
      }
      return jsonResponse({ code: 'AUTH_TOKEN_EXPIRED', message: '令牌已过期' }, 401)
    })

    const [first, second] = await Promise.all([
      api.get('/v1/me'),
      api.get('/v1/notes'),
    ])

    expect(first).toEqual({ ok: true })
    expect(second).toEqual({ ok: true })
    expect(refreshCalls).toBe(1)
  })

  it('does not refresh login failures and keeps the session handler idle', async () => {
    const expired = vi.fn()
    setAuthExpiredHandler(expired)
    fetchMock.mockResolvedValueOnce(jsonResponse({ code: 'AUTH_INVALID_CREDENTIALS', message: '邮箱或密码错误' }, 401))

    await expect(api.post('/v1/auth/login', { email: 'a@b.c', password: 'x' })).rejects.toMatchObject({
      code: 'AUTH_INVALID_CREDENTIALS',
    })
    expect(expired).not.toHaveBeenCalled()
    expect(fetchMock).toHaveBeenCalledOnce()
  })

  it('clears memory token and reports expiry when refresh fails', async () => {
    const expired = vi.fn()
    setAuthExpiredHandler(expired)
    setAccessToken('expired')
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ code: 'AUTH_TOKEN_EXPIRED', message: '令牌已过期' }, 401))
      .mockResolvedValueOnce(jsonResponse({ code: 'AUTH_REFRESH_TOKEN_INVALID', message: '刷新令牌无效' }, 401))

    await expect(api.get('/v1/me')).rejects.toMatchObject({ code: 'AUTH_TOKEN_EXPIRED' })
    expect(expired).toHaveBeenCalledOnce()
    expect(getAccessToken()).toBeNull()
  })
})
