import type { Tag } from '@/features/tag/types'

export interface WorkspaceFile {
  id: string
  displayName: string
  originalName: string
  mediaType: string
  sizeBytes: number
  projectId: string | null
  state: 'UPLOADING' | 'STORED' | 'FAILED' | 'DELETED' | 'PURGED'
  extraction: { status: string; truncated: boolean; retryable: boolean; errorCode?: string }
  version: number
  tagVersion: number
  tags: Tag[]
  references: { type: string; id: string; title: string }[]
  capabilities: { canEdit: boolean; canDownload: boolean; canRestore: boolean }
  createdAt: string
  updatedAt: string
  deletedAt?: string
  purgeAfter?: string
  deletionReason?: string
}
export interface FileLimits {
  formats: Record<string, string>
  maxSize: number
  maxSelection: number
  maxConcurrent: number
  quotaBytes: number
  usedBytes: number
  reservedBytes: number
  storageAvailable: boolean
}
export interface UploadSession {
  uploadId: string
  fileId: string
  state: 'CREATED' | 'RECEIVING' | 'VERIFYING' | 'COMPLETED' | 'FAILED' | 'EXPIRED'
  expiresAt: string
  contentPath: string
  errorCode?: string
}
export type AttachmentTarget = { resource: 'tasks' | 'notes'; id: string }
