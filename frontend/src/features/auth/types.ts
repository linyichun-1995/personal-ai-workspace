export interface AuthUser {
  id: string
  email: string
  name: string
  avatarUrl: string | null
}

export interface AuthWorkspace {
  id: string
  name: string
}

export interface Session {
  user: AuthUser
  workspace: AuthWorkspace
}

export interface AuthResponse {
  user: AuthUser
  workspace: AuthWorkspace
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface CurrentUserResponse {
  id: string
  email: string
  name: string
  avatarUrl: string | null
  currentWorkspace: AuthWorkspace
}
