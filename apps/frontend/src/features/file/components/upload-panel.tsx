import { Link } from '@tanstack/react-router'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { Upload } from 'lucide-react'
import { useSession } from '@/features/auth/hooks/use-session'
import { fileApi } from '@/features/file/api/files'
import type { AttachmentTarget } from '@/features/file/types'
import {
  activateQueue,
  addUploads,
  cancelUpload,
  checkUploads,
  removeUpload,
  retryUpload,
  useUploadQueue,
  type UploadState,
} from '@/features/file/lib/upload-queue'
import { formatBytes } from '@/features/file/lib/file-search'
import { useOrigin } from '@/features/search/components/origin-link'
import { Button } from '@/shared/components/ui/button'
import { OperationError } from '@/features/file/components/operation-error'

export function UploadCoordinator() {
  const { data: session } = useSession()
  const client = useQueryClient()
  const workspaceId = session?.workspace.id ?? ''
  const revision = useUploadQueue((state) => state.revision)
  useEffect(() => {
    activateQueue(workspaceId)
  }, [workspaceId])
  useEffect(() => {
    if (workspaceId && revision)
      void client.invalidateQueries({ queryKey: ['content', workspaceId] })
  }, [client, workspaceId, revision])
  useEffect(() => {
    const interval = setInterval(() => void checkUploads(), 3000)
    return () => clearInterval(interval)
  }, [])
  return null
}
const labels: Record<UploadState, string> = {
  WAITING: '等待上传',
  CREATING: '准备上传',
  UPLOADING: '正在传输',
  VERIFYING: '正在确认文件',
  CHECKING: '正在核对上传结果',
  COMPLETE: '上传成功',
  ERROR: '上传未完成',
  LINK_ERROR: '文件已上传，关联未完成',
}
export function UploadPanel({
  projectId = null,
  target,
  readOnly = false,
  all = false,
}: {
  projectId?: string | null
  target?: AttachmentTarget
  readOnly?: boolean
  all?: boolean
}) {
  const { data: session } = useSession()
  const workspaceId = session?.workspace.id ?? ''
  const limits = useQuery({
    queryKey: ['content', workspaceId, 'files', 'limits'],
    queryFn: fileApi.limits,
    enabled: Boolean(workspaceId),
  })
  const items = useUploadQueue((state) => state.items)
  const input = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string>()
  const origin = useOrigin()
  const visible = items.filter(
    (item) => all || (item.projectId === projectId && item.target?.id === target?.id),
  )
  function choose(files: FileList | null) {
    if (!files || !limits.data || readOnly) return
    setError(addUploads(Array.from(files), limits.data, projectId, target))
    if (input.current) input.current.value = ''
  }
  return (
    <section
      className="grid gap-3 rounded-lg border border-dashed bg-card p-4"
      aria-label="文件上传"
      onDragOver={(event) => {
        event.preventDefault()
      }}
      onDrop={(event) => {
        event.preventDefault()
        choose(event.dataTransfer.files)
      }}
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0 text-xs text-muted-foreground">
          <p>PDF、DOCX、TXT、MD、PNG、JPEG、WebP</p>
          <p className="mt-1">
            单文件 {formatBytes(limits.data?.maxSize ?? 20971520)} · 每次最多 10 个 · 同时上传 2 个
          </p>
          {limits.data && (
            <p className="mt-1">
              可用{' '}
              {formatBytes(
                Math.max(
                  0,
                  limits.data.quotaBytes - limits.data.usedBytes - limits.data.reservedBytes,
                ),
              )}{' '}
              / {formatBytes(limits.data.quotaBytes)}，回收站文件也占用容量。
            </p>
          )}
          {readOnly && <p className="mt-1">归档内容只读，恢复后可上传。</p>}
        </div>
        <Button
          type="button"
          disabled={readOnly || !limits.data || !limits.data.storageAvailable}
          onClick={() => input.current?.click()}
        >
          <Upload />
          上传文件
        </Button>
      </div>
      <input
        ref={input}
        type="file"
        className="sr-only"
        aria-label="选择上传文件"
        tabIndex={-1}
        multiple
        accept=".pdf,.docx,.txt,.md,.png,.jpg,.jpeg,.webp"
        onChange={(event) => choose(event.target.files)}
      />
      {!readOnly && (
        <p className="text-xs text-muted-foreground">
          也可拖入文件。刷新后会核对上传结果，本地文件字节不会自动恢复。
        </p>
      )}
      <OperationError error={error ? new Error(error) : limits.error} />
      {limits.data && !limits.data.storageAvailable && (
        <p role="alert" className="text-sm">
          文件存储暂时不可用，请稍后重试。
        </p>
      )}
      {visible.length > 0 && (
        <ol className="grid gap-2" aria-label="上传队列">
          {visible.map((item) => (
            <li key={item.id} className="grid gap-2 rounded-md border p-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="min-w-0 break-all text-sm font-medium">{item.name}</span>
                <span className="text-xs text-muted-foreground">{formatBytes(item.size)}</span>
              </div>
              <div role="status" aria-live="polite" className="text-xs">
                {labels[item.state]}
                {['UPLOADING', 'VERIFYING'].includes(item.state)
                  ? ` · 已传输 ${item.progress}%`
                  : ''}
                {item.state === 'COMPLETE' && '；正文处理状态见文件详情'}
              </div>
              {['UPLOADING', 'VERIFYING'].includes(item.state) && (
                <progress
                  className="h-2 w-full accent-primary"
                  value={item.progress}
                  max={100}
                  aria-label={`${item.name} 字节传输进度`}
                />
              )}
              {item.error && (
                <p className="break-words text-xs text-muted-foreground">{item.error}</p>
              )}
              <div className="flex flex-wrap gap-2">
                {item.fileId && ['COMPLETE', 'LINK_ERROR'].includes(item.state) && (
                  <Button type="button" variant="outline" size="sm" asChild>
                    <Link
                      to="/app/files/$fileId"
                      params={{ fileId: item.fileId }}
                      search={{ origin }}
                    >
                      查看文件
                    </Link>
                  </Button>
                )}
                {['ERROR', 'LINK_ERROR'].includes(item.state) &&
                  (item.file || item.state === 'LINK_ERROR') && (
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={readOnly}
                      onClick={() =>
                        void retryUpload(item.id).catch((reason) => setError(String(reason)))
                      }
                    >
                      {item.state === 'LINK_ERROR' ? '重试关联' : '重试上传'}
                    </Button>
                  )}
                {['WAITING', 'CREATING', 'UPLOADING'].includes(item.state) && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => cancelUpload(item.id)}
                  >
                    {item.state === 'WAITING' ? '移除' : '取消传输'}
                  </Button>
                )}
                {['COMPLETE', 'ERROR', 'LINK_ERROR'].includes(item.state) && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => removeUpload(item.id)}
                  >
                    关闭记录
                  </Button>
                )}
              </div>
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}
