import type { AuthResponse, CurrentUserResponse, Session } from '@/features/auth/types'
import { api } from '@/shared/api'
import { queryKeys } from '@/shared/api/query-keys'
import { clearAccessToken, setAccessToken } from '@/features/auth/api/token-store'

export const sessionQueryOptions = {
  queryKey: queryKeys.auth.session(),
  queryFn: restoreSession,
  retry: false as const,
  staleTime: Infinity,
  gcTime: Infinity,
}

export async function login(input: { email: string, password: string }): Promise<Session> {
  return storeSession(await api.post<AuthResponse>('/v1/auth/login', input))
}

export async function register(input: { name: string, email: string, password: string }): Promise<Session> {
  return storeSession(await api.post<AuthResponse>('/v1/auth/register', input))
}

export async function logout(): Promise<void> {
  try {
    await api.post<void>('/v1/auth/logout')
  }
  finally {
    clearAccessToken()
  }
}

export async function getCurrentUser(): Promise<CurrentUserResponse> {
  return api.get<CurrentUserResponse>('/v1/me')
}

export async function restoreSession(): Promise<Session | null> {
  try {
    const refreshed = await api.post<AuthResponse>('/v1/auth/refresh', undefined, { skipAuthRefresh: true })
    setAccessToken(refreshed.accessToken)
    const me = await getCurrentUser()
    return {
      user: {
        id: me.id,
        email: me.email,
        name: me.name,
        avatarUrl: me.avatarUrl,
      },
      workspace: me.currentWorkspace,
    }
  }
  catch {
    clearAccessToken()
    return null
  }
}

function storeSession(response: AuthResponse): Session {
  setAccessToken(response.accessToken)
  return {
    user: response.user,
    workspace: response.workspace,
  }
}
