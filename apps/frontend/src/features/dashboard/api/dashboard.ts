import type { DashboardNote, DashboardProject, DashboardResponse, DashboardTask } from '@/features/dashboard/types'
import { api } from '@/shared/api'

export type { DashboardNote, DashboardProject, DashboardResponse, DashboardTask }

export function getDashboard(): Promise<DashboardResponse> {
  return api.get<DashboardResponse>('/v1/dashboard')
}
