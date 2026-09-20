import { getRouteApi } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useSession } from '@/features/auth/hooks/use-session'
import { fileApi, downloadFile } from '@/features/file/api/files'
import { formatBytes, EXTRACTION_LABELS } from '@/features/file/lib/file-search'
import type { WorkspaceFile } from '@/features/file/types'
import { ObjectTags, TagChips } from '@/features/tag/components/object-tags'
import { OriginLink } from '@/features/search/components/origin-link'
import { OperationError } from '@/features/file/components/operation-error'
import { PageContainer } from '@/shared/components/page-container'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { api } from '@/shared/api'
const route = getRouteApi('/app/files/$fileId')
export function FileDetailPage() {
  const { fileId } = route.useParams()
  const { origin } = route.useSearch()
  const { data: session } = useSession()
  const query = useQuery({
    queryKey: ['content', session?.workspace.id, 'files', fileId],
    queryFn: () => fileApi.get(fileId),
    enabled: Boolean(session),
    refetchInterval: 5000,
  })
  return (
    <PageContainer width="reading">
      <div className="grid gap-5">
        <OriginLink origin={origin} />
        <OperationError error={query.error} />
        {query.isPending && <p>正在加载文件…</p>}
        {query.data && <FileDetail key={fileId} file={query.data} />}
      </div>
    </PageContainer>
  )
}
function FileDetail({ file }: { file: WorkspaceFile }) {
  const client = useQueryClient()
  const [name, setName] = useState(file.displayName)
  const [error, setError] = useState<unknown>()
  const [preview, setPreview] = useState(false)
  const [image, setImage] = useState<string>()
  const change = useMutation({
    mutationFn: (action: () => Promise<unknown>) => action(),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['content'] })
    },
  })
  const text = useQuery({
    queryKey: ['content', 'text', file.id, file.extraction.status],
    queryFn: () => fileApi.text(file.id),
    enabled: preview && file.state === 'STORED',
  })
  const visual = ['application/pdf', 'image/png', 'image/jpeg', 'image/webp'].includes(
    file.mediaType,
  )
  useEffect(() => {
    if (!preview || !visual || file.state !== 'STORED') return undefined
    let disposed = false
    let url: string | undefined
    void api
      .get<Blob>(`/v1/files/${file.id}/preview`, {
        responseType: 'blob',
        headers: { Accept: 'image/png' },
      })
      .then((blob) => {
        if (!disposed) {
          url = URL.createObjectURL(blob)
          setImage(url)
        }
      })
      .catch(setError)
    return () => {
      disposed = true
      if (url) URL.revokeObjectURL(url)
      setImage(undefined)
    }
  }, [preview, visual, file.id, file.state])
  return (
    <>
      <header>
        <h1 className="break-all text-2xl font-semibold">{file.displayName}</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          {formatBytes(file.sizeBytes)} · {file.mediaType}
        </p>
        <p className="mt-1 text-sm">
          {file.state === 'DELETED'
            ? '文件已删除，回收站保留 30 天'
            : (EXTRACTION_LABELS[file.extraction.status] ?? file.extraction.status)}
        </p>
        {file.deletionReason === 'PROJECT_DELETED' && (
          <p className="text-sm">所属项目已删除，无法单独恢复此文件。</p>
        )}
        {file.extraction.truncated && <p className="text-sm">仅检索前 200000 个字符。</p>}
      </header>
      <div className="flex flex-wrap gap-2">
        <Button
          disabled={!file.capabilities.canDownload}
          onClick={() => void downloadFile(file).catch(setError)}
        >
          下载原文件
        </Button>
        <Button
          variant="outline"
          disabled={!file.capabilities.canDownload}
          onClick={() => setPreview(!preview)}
        >
          {preview ? '关闭预览' : '预览'}
        </Button>
        {file.capabilities.canEdit && (
          <Button
            variant="destructive"
            disabled={change.isPending}
            onClick={() => {
              if (window.confirm(`将“${file.displayName}”放入回收站？`))
                change.mutate(() => fileApi.remove(file))
            }}
          >
            删除
          </Button>
        )}
        {file.capabilities.canRestore && (
          <Button
            disabled={change.isPending}
            onClick={() => change.mutate(() => fileApi.restore(file))}
          >
            恢复文件
          </Button>
        )}
        {file.capabilities.canEdit && file.extraction.retryable && (
          <Button
            variant="outline"
            disabled={change.isPending}
            onClick={() => change.mutate(() => fileApi.retryExtraction(file.id))}
          >
            重新处理正文
          </Button>
        )}
      </div>
      <OperationError error={error ?? change.error} />
      {preview && file.state === 'STORED' && (
        <section className="grid gap-3 rounded-lg border p-4" aria-label="文件预览">
          {visual &&
            (image ? (
              <img
                className="max-w-full rounded"
                src={image}
                alt={file.mediaType === 'application/pdf' ? 'PDF 首页预览' : file.displayName}
              />
            ) : (
              <p>正在生成安全预览…</p>
            ))}
          {file.mediaType === 'application/pdf' && (
            <p className="text-xs text-muted-foreground">
              图片预览展示首页，完整内容请下载原文件。
            </p>
          )}
          <OperationError error={text.error} />
          {text.data?.text ? (
            <pre className="max-h-128 overflow-auto whitespace-pre-wrap break-words text-sm">
              {text.data.text}
            </pre>
          ) : (
            <p className="text-sm text-muted-foreground">
              {text.isPending ? '正在加载正文…' : '当前没有可预览的正文，可下载原文件。'}
            </p>
          )}
        </section>
      )}
      {file.capabilities.canEdit && (
        <form
          className="flex gap-2"
          onSubmit={(event) => {
            event.preventDefault()
            change.mutate(() =>
              fileApi.update(file.id, { version: file.version, displayName: name }),
            )
          }}
        >
          <Input aria-label="文件显示名称" value={name} onChange={(e) => setName(e.target.value)} />
          <Button type="submit" variant="outline" disabled={change.isPending}>
            重命名
          </Button>
        </form>
      )}
      {file.state === 'STORED' ? (
        <ObjectTags resource="files" id={file.id} readOnly={!file.capabilities.canEdit} />
      ) : (
        <TagChips tags={file.tags} />
      )}
      <section className="grid gap-2">
        <h2 className="font-medium">引用位置</h2>
        {file.references.length ? (
          file.references.map((ref) => (
            <p key={`${ref.type}:${ref.id}`} className="text-sm">
              {ref.type === 'NOTE' ? '笔记' : '任务'} · {ref.title}
            </p>
          ))
        ) : (
          <p className="text-sm text-muted-foreground">暂无附件引用</p>
        )}
      </section>
    </>
  )
}
