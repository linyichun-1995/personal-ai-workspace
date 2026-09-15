import type { QueryClient } from '@tanstack/react-query'

import { queryKeys } from '@/shared/api/query-keys'

export function invalidateWorkspaceData(queryClient: QueryClient, workspaceId: string): void {
  void queryClient.invalidateQueries({ queryKey: queryKeys.project.all(workspaceId) })
  void queryClient.invalidateQueries({ queryKey: ['project', workspaceId] })
  void queryClient.invalidateQueries({ queryKey: queryKeys.task.all(workspaceId) })
  void queryClient.invalidateQueries({ queryKey: ['task', workspaceId] })
  void queryClient.invalidateQueries({ queryKey: queryKeys.note.all(workspaceId) })
  void queryClient.invalidateQueries({ queryKey: ['note', workspaceId] })
  void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.summary(workspaceId) })
}
