import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { useState } from 'react'
import { useSession } from '@/features/auth/hooks/use-session'
import { fileApi } from '@/features/file/api/files'
import type { AttachmentTarget } from '@/features/file/types'
import { UploadPanel } from '@/features/file/components/upload-panel'
import { OperationError } from '@/features/file/components/operation-error'
import { useOrigin } from '@/features/search/components/origin-link'
import { Button } from '@/shared/components/ui/button'
export function Attachments({
  target,
  projectId,
  readOnly = false,
}: {
  target: AttachmentTarget
  projectId: string | null
  readOnly?: boolean
}) {
  const { data: session } = useSession()
  const client = useQueryClient()
  const origin = useOrigin()
  const [page, setPage] = useState(1)
  const [choose, setChoose] = useState(false)
  const [candidatePage, setCandidatePage] = useState(1)
  const key = ['content', session?.workspace.id]
  const attached = useQuery({
    queryKey: [...key, 'attachments', target, page],
    queryFn: () => fileApi.attachments(target, page),
    enabled: Boolean(session),
  })
  const candidates = useQuery({
    queryKey: [...key, 'attachmentCandidates', projectId, candidatePage],
    queryFn: ({ signal }) =>
      fileApi.list(
        { projectId: projectId ?? undefined, unassigned: !projectId, page: candidatePage },
        signal,
      ),
    enabled: Boolean(session) && choose && !readOnly,
  })
  const mutate = useMutation({
    mutationFn: ({ id, detach }: { id: string; detach: boolean }) =>
      detach ? fileApi.detach(target, id) : fileApi.attach(target, id),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: key })
    },
  })
  return (
    <section className="grid gap-3 rounded-lg border p-4" aria-label="附件">
      <h3 className="font-medium">附件</h3>
      <p className="text-xs text-muted-foreground">
        附件单独保存。移除关联后，文件仍保留在资料库中。
      </p>
      <OperationError error={attached.error || mutate.error} />
      {attached.isPending && <p role="status">正在加载附件…</p>}
      <ul className="grid gap-2">
        {attached.data?.items.map((file) => (
          <li key={file.id} className="flex items-center justify-between gap-2">
            <Link
              className="break-all text-sm underline"
              to="/app/files/$fileId"
              params={{ fileId: file.id }}
              search={{ origin }}
            >
              {file.displayName}
              {file.state === 'DELETED' ? '（已删除）' : ''}
            </Link>
            <Button
              type="button"
              size="sm"
              variant="ghost"
              disabled={readOnly || mutate.isPending}
              onClick={() => mutate.mutate({ id: file.id, detach: true })}
            >
              移除关联
            </Button>
          </li>
        ))}
      </ul>
      {attached.data?.total === 0 && <p className="text-sm text-muted-foreground">暂无附件</p>}
      {attached.data && attached.data.totalPages > 1 && (
        <div className="flex gap-2">
          <Button type="button" disabled={page <= 1} onClick={() => setPage(page - 1)}>
            上一页
          </Button>
          <Button
            type="button"
            disabled={page >= attached.data.totalPages}
            onClick={() => setPage(page + 1)}
          >
            下一页
          </Button>
        </div>
      )}
      {!readOnly && (
        <>
          <Button type="button" variant="outline" onClick={() => setChoose(!choose)}>
            {choose ? '收起文件选择' : '关联已有文件'}
          </Button>
          {choose && (
            <div className="grid gap-2 rounded border p-3">
              <p className="text-xs text-muted-foreground">
                只能关联同一项目下的文件；无项目内容只能关联未分类文件。
              </p>
              <OperationError error={candidates.error} />
              {candidates.isPending && <p role="status">正在加载可关联文件…</p>}
              {candidates.data?.items.map((file) => (
                <div key={file.id} className="flex items-center justify-between gap-2">
                  <span className="break-all text-sm">{file.displayName}</span>
                  <Button
                    type="button"
                    size="sm"
                    disabled={
                      mutate.isPending || attached.data?.items.some((item) => item.id === file.id)
                    }
                    onClick={() => mutate.mutate({ id: file.id, detach: false })}
                  >
                    关联
                  </Button>
                </div>
              ))}
              {candidates.data && (
                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant="ghost"
                    disabled={candidatePage <= 1}
                    onClick={() => setCandidatePage(candidatePage - 1)}
                  >
                    上一页
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    disabled={candidatePage >= candidates.data.totalPages}
                    onClick={() => setCandidatePage(candidatePage + 1)}
                  >
                    下一页
                  </Button>
                </div>
              )}
            </div>
          )}
        </>
      )}
      <UploadPanel projectId={projectId} target={target} readOnly={readOnly} />
    </section>
  )
}
