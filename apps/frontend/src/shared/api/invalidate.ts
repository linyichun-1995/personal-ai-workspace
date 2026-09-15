import type { QueryClient } from '@tanstack/react-query'

import { queryKeys } from '@/shared/api/query-keys'

export async function invalidateWorkspaceData(
  queryClient: QueryClient,
  workspaceId: string,
): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: queryKeys.project.all(workspaceId) }),
    queryClient.invalidateQueries({ queryKey: ['project', workspaceId] }),
    queryClient.invalidateQueries({ queryKey: queryKeys.task.all(workspaceId) }),
    queryClient.invalidateQueries({ queryKey: ['task', workspaceId] }),
    queryClient.invalidateQueries({ queryKey: queryKeys.note.all(workspaceId) }),
    queryClient.invalidateQueries({ queryKey: ['note', workspaceId] }),
    queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.summary(workspaceId) }),
  ])
}
