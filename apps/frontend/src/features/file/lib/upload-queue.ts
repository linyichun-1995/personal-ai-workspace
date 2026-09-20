import { create } from 'zustand'
import { fileApi } from '@/features/file/api/files'
import type { AttachmentTarget, FileLimits, WorkspaceFile } from '@/features/file/types'
import { uploadBytes } from '@/shared/api/client'
import { toErrorMessage } from '@/shared/api/errors'
export type UploadState =
  | 'WAITING'
  | 'CREATING'
  | 'UPLOADING'
  | 'VERIFYING'
  | 'CHECKING'
  | 'COMPLETE'
  | 'ERROR'
  | 'LINK_ERROR'
export interface UploadItem {
  id: string
  name: string
  size: number
  mediaType: string
  projectId: string | null
  target?: AttachmentTarget
  uploadId?: string
  fileId?: string
  key: string
  state: UploadState
  progress: number
  error?: string
  file?: File
}
interface QueueStore {
  workspaceId: string
  items: UploadItem[]
  revision: number
}
export const useUploadQueue = create<QueueStore>(() => ({
  workspaceId: '',
  items: [],
  revision: 0,
}))
const controllers = new Map<string, AbortController>()
const checking = new Set<string>()
const active = new Set<string>()
let concurrency = 2
function patch(id: string, changes: Partial<UploadItem>) {
  useUploadQueue.setState((state) => ({
    items: state.items.map((item) => (item.id === id ? { ...item, ...changes } : item)),
  }))
  persist()
}
function persist() {
  const { workspaceId, items } = useUploadQueue.getState()
  if (!workspaceId) return
  try {
    sessionStorage.setItem(
      `workspace-uploads:${workspaceId}`,
      JSON.stringify(items.map(({ file: _file, ...metadata }) => metadata).slice(-50)),
    )
  } catch {
    /* status recovery is best effort when browser storage is unavailable */
  }
}
function current(id: string) {
  return useUploadQueue.getState().items.find((item) => item.id === id)
}
function changed() {
  useUploadQueue.setState((state) => ({ revision: state.revision + 1 }))
}
export function activateQueue(workspaceId: string) {
  if (useUploadQueue.getState().workspaceId === workspaceId) return
  controllers.forEach((controller) => controller.abort())
  controllers.clear()
  active.clear()
  checking.clear()
  let items: UploadItem[] = []
  if (workspaceId) {
    try {
      const saved: unknown = JSON.parse(
        sessionStorage.getItem(`workspace-uploads:${workspaceId}`) ?? '[]',
      )
      if (Array.isArray(saved))
        items = saved
          .filter(
            (item) =>
              item &&
              typeof item.id === 'string' &&
              typeof item.name === 'string' &&
              typeof item.key === 'string',
          )
          .slice(-50)
          .map((item) => ({
            ...item,
            file: undefined,
            state:
              item.state === 'COMPLETE'
                ? 'COMPLETE'
                : item.state === 'LINK_ERROR'
                  ? 'LINK_ERROR'
                  : item.uploadId
                    ? 'CHECKING'
                    : 'ERROR',
            error:
              !item.uploadId && item.state !== 'COMPLETE'
                ? '本地文件字节无法恢复，请重新选择文件'
                : item.error,
          }))
    } catch {
      /* invalid saved state is ignored */
    }
  }
  useUploadQueue.setState({ workspaceId, items, revision: 0 })
  void checkUploads()
}
export function addUploads(
  files: File[],
  limits: FileLimits,
  projectId: string | null,
  target?: AttachmentTarget,
): string | undefined {
  if (files.length > Math.min(10, limits.maxSelection))
    return `每次最多选择 ${Math.min(10, limits.maxSelection)} 个文件`
  if (!limits.storageAvailable) return '文件存储暂时不可用，请稍后重试'
  concurrency = Math.max(1, Math.min(2, limits.maxConcurrent))
  const remaining = limits.quotaBytes - limits.usedBytes - limits.reservedBytes
  if (files.reduce((sum, file) => sum + file.size, 0) > remaining)
    return '容量不足，回收站中的文件也占用空间'
  const accepted = new Set(['pdf', 'docx', 'txt', 'md', 'png', 'jpg', 'jpeg', 'webp'])
  const items = files.map((file): UploadItem => {
    const extension = file.name.split('.').at(-1)?.toLowerCase() ?? ''
    const error =
      file.size > limits.maxSize
        ? '超过单文件大小限制'
        : !accepted.has(extension)
          ? '格式不受支持，请选择 PDF、DOCX、TXT、MD、PNG、JPEG 或 WebP'
          : undefined
    const mediaType =
      extension === 'md'
        ? 'text/markdown'
        : extension === 'txt'
          ? 'text/plain'
          : file.type || limits.formats[extension] || 'application/octet-stream'
    return {
      id: crypto.randomUUID(),
      key: crypto.randomUUID(),
      name: file.name,
      size: file.size,
      mediaType,
      projectId,
      target,
      file,
      state: error ? 'ERROR' : 'WAITING',
      progress: 0,
      error,
    }
  })
  useUploadQueue.setState((state) => ({ items: [...state.items, ...items] }))
  persist()
  drain()
  return undefined
}
function drain() {
  while (active.size < concurrency) {
    const item = useUploadQueue
      .getState()
      .items.find((candidate) => candidate.state === 'WAITING' && !active.has(candidate.id))
    if (!item) break
    active.add(item.id)
    void transmit(item.id).finally(() => {
      active.delete(item.id)
      drain()
    })
  }
}
async function finish(item: UploadItem, file: WorkspaceFile) {
  if (!current(item.id)) return
  patch(item.id, { fileId: file.id, progress: 100 })
  if (item.target) {
    try {
      await fileApi.attach(item.target, file.id)
    } catch (error) {
      patch(item.id, {
        state: 'LINK_ERROR',
        file: undefined,
        error: `文件已上传，关联未完成：${toErrorMessage(error)}`,
      })
      changed()
      return
    }
  }
  patch(item.id, { state: 'COMPLETE', file: undefined, error: undefined })
  changed()
}
async function transmit(id: string) {
  let item = current(id)
  if (!item?.file) {
    patch(id, { state: 'ERROR', error: '请重新选择本地文件，无法自动恢复文件字节' })
    return
  }
  const controller = new AbortController()
  controllers.set(id, controller)
  try {
    if (!item.uploadId) {
      patch(id, { state: 'CREATING', error: undefined })
      const session = await fileApi.createUpload(
        { name: item.name, size: item.size, mediaType: item.mediaType, projectId: item.projectId },
        item.key,
      )
      if (!current(id)) return
      patch(id, { uploadId: session.uploadId, fileId: session.fileId })
      item = current(id)!
      if (session.state === 'COMPLETED') {
        await finish(item, await fileApi.get(session.fileId))
        return
      }
      if (session.state !== 'CREATED') {
        patch(id, { state: 'CHECKING' })
        await checkOne(id)
        return
      }
    }
    if (controller.signal.aborted) {
      patch(id, { state: 'CHECKING' })
      return
    }
    patch(id, { state: 'UPLOADING', progress: 0, error: undefined })
    const file = await uploadBytes<WorkspaceFile>(
      `/v1/files/uploads/${item.uploadId}/content`,
      item.file!,
      {
        signal: controller.signal,
        mediaType: item.mediaType,
        onProgress: (progress) =>
          patch(id, { progress, state: progress === 100 ? 'VERIFYING' : 'UPLOADING' }),
      },
    )
    await finish(item, file)
  } catch (error) {
    if (current(id)) {
      patch(id, {
        state: current(id)?.uploadId ? 'CHECKING' : 'ERROR',
        error: toErrorMessage(error),
      })
      await checkOne(id)
    }
  } finally {
    controllers.delete(id)
  }
}
async function checkOne(id: string) {
  const item = current(id)
  if (!item?.uploadId || checking.has(id)) return
  checking.add(id)
  try {
    const session = await fileApi.uploadStatus(item.uploadId)
    if (!current(id)) return
    if (session.state === 'COMPLETED') await finish(item, await fileApi.get(session.fileId))
    else if (session.state === 'FAILED' || session.state === 'EXPIRED')
      patch(id, {
        state: 'ERROR',
        uploadId: undefined,
        key: crypto.randomUUID(),
        error:
          session.state === 'EXPIRED' ? '上传会话已过期，请重新上传' : '上传未保存成功，请重试',
      })
    else if (session.state === 'CREATED')
      patch(id, {
        state: 'ERROR',
        error: item.file ? '上传尚未完成，可重试发送文件' : '本地文件字节无法恢复，请重新选择文件',
      })
    else
      patch(id, {
        state: 'CHECKING',
        error: '服务端正在核对状态，确认后会自动更新。请勿重复创建上传。',
      })
  } catch (error) {
    patch(id, { state: 'CHECKING', error: `暂时无法确认结果：${toErrorMessage(error)}` })
  } finally {
    checking.delete(id)
  }
}
export async function checkUploads() {
  await Promise.all(
    useUploadQueue
      .getState()
      .items.filter((item) => item.state === 'CHECKING')
      .map((item) => checkOne(item.id)),
  )
}
export function cancelUpload(id: string) {
  const item = current(id)
  if (!item) return
  if (item.state === 'WAITING') {
    removeUpload(id)
    return
  }
  controllers.get(id)?.abort()
  patch(id, { state: 'CHECKING', error: '已取消客户端传输，服务端正在核对状态' })
}
export function removeUpload(id: string) {
  useUploadQueue.setState((state) => ({ items: state.items.filter((item) => item.id !== id) }))
  persist()
}
export async function retryUpload(id: string) {
  let item = current(id)
  if (!item) return
  if (item.state === 'LINK_ERROR' && item.fileId) {
    patch(id, { state: 'CHECKING' })
    await finish(item, await fileApi.get(item.fileId))
    return
  }
  if (item.uploadId) {
    await checkOne(id)
    item = current(id)
  }
  if (item?.file && item.state === 'ERROR') {
    patch(id, { state: 'WAITING', error: undefined })
    drain()
  }
}
