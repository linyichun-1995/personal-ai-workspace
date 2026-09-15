export const queryKeys = {
  auth: {
    all: () => ['auth'] as const,
    session: () => ['auth', 'session'] as const,
    me: () => ['auth', 'me'] as const,
  },
  workspace: {
    all: () => ['workspaces'] as const,
    current: () => ['workspaces', 'current'] as const,
    detail: (workspaceId: string) => ['workspace', workspaceId] as const,
  },
  project: {
    all: (workspaceId: string) => ['projects', workspaceId] as const,
    list: (workspaceId: string, filters?: unknown) =>
      ['projects', workspaceId, filters] as const,
    detail: (workspaceId: string, projectId: string) =>
      ['project', workspaceId, projectId] as const,
  },
  task: {
    all: (workspaceId: string) => ['tasks', workspaceId] as const,
    list: (workspaceId: string, filters?: unknown) =>
      ['tasks', workspaceId, filters] as const,
    detail: (workspaceId: string, taskId: string) =>
      ['task', workspaceId, taskId] as const,
  },
  note: {
    all: (workspaceId: string) => ['notes', workspaceId] as const,
    list: (workspaceId: string, filters?: unknown) =>
      ['notes', workspaceId, filters] as const,
    detail: (workspaceId: string, noteId: string) =>
      ['note', workspaceId, noteId] as const,
  },
  file: {
    all: (workspaceId: string) => ['files', workspaceId] as const,
    list: (workspaceId: string, filters?: unknown) =>
      ['files', workspaceId, filters] as const,
    detail: (workspaceId: string, fileId: string) =>
      ['file', workspaceId, fileId] as const,
  },
  search: {
    all: (workspaceId: string) => ['search', workspaceId] as const,
    query: (workspaceId: string, term: string) =>
      ['search', workspaceId, term] as const,
  },
  ai: {
    all: (workspaceId: string) => ['ai', workspaceId] as const,
    conversations: (workspaceId: string) =>
      ['ai', workspaceId, 'conversations'] as const,
    conversation: (workspaceId: string, conversationId: string) =>
      ['ai', workspaceId, 'conversation', conversationId] as const,
  },
  integration: {
    all: (workspaceId: string) => ['integrations', workspaceId] as const,
  },
  dashboard: {
    summary: (workspaceId: string) => ['dashboard', workspaceId] as const,
  },
} as const
