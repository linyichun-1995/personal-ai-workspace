import { api } from '@/shared/api'
import type { PaginatedResult } from '@/shared/types/api'
import type {
  AttachmentTarget,
  FileLimits,
  UploadSession,
  WorkspaceFile,
} from '@/features/file/types'
import type { FileSearch } from '@/features/file/lib/file-search'
export const fileApi = {
  limits: () => api.get<FileLimits>('/v1/files/capabilities'),
  list: (
    filters: FileSearch & { unassigned?: boolean; size?: number } = {},
    signal?: AbortSignal,
  ) =>
    api.get<PaginatedResult<WorkspaceFile>>('/v1/files', {
      signal,
      query: {
        ...filters,
        view: filters.view === 'trash' ? 'trash' : 'active',
        uncategorized: filters.view === 'uncategorized',
        size: filters.size ?? 20,
      },
    }),
  get: (id: string) => api.get<WorkspaceFile>(`/v1/files/${id}`),
  createUpload: (
    input: { name: string; size: number; mediaType: string; projectId: string | null },
    key: string,
  ) => api.post<UploadSession>('/v1/files/uploads', input, { headers: { 'Idempotency-Key': key } }),
  uploadStatus: (id: string) => api.get<UploadSession>(`/v1/files/uploads/${id}`),
  update: (
    id: string,
    input: { version: number; displayName?: string; projectId?: string | null },
  ) => api.patch<WorkspaceFile>(`/v1/files/${id}`, input),
  remove: (file: WorkspaceFile) =>
    api.delete<void>(`/v1/files/${file.id}`, { headers: { 'If-Match': `"${file.version}"` } }),
  restore: (file: WorkspaceFile) =>
    api.post<WorkspaceFile>(`/v1/files/${file.id}/restore`, { version: file.version }),
  text: (id: string) =>
    api.get<{ status: string; text: string | null; truncated: boolean }>(`/v1/files/${id}/text`),
  retryExtraction: (id: string) => api.post<WorkspaceFile>(`/v1/files/${id}/extraction-retries`),
  blob: (id: string) =>
    api.get<Blob>(`/v1/files/${id}/content`, {
      responseType: 'blob',
      headers: { Accept: 'application/octet-stream' },
    }),
  attachments: (target: AttachmentTarget, page = 1) =>
    api.get<PaginatedResult<WorkspaceFile>>(`/v1/${target.resource}/${target.id}/files`, {
      query: { page, size: 20 },
    }),
  attach: (target: AttachmentTarget, fileId: string) =>
    api.put<void>(`/v1/${target.resource}/${target.id}/files/${fileId}`),
  detach: (target: AttachmentTarget, fileId: string) =>
    api.delete<void>(`/v1/${target.resource}/${target.id}/files/${fileId}`),
}
export async function downloadFile(file: WorkspaceFile) {
  const url = URL.createObjectURL(await fileApi.blob(file.id))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = file.displayName
  anchor.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
