import { FeaturePlaceholder } from '@/shared/components/feature-placeholder'

export function FilesPage() {
  return (
    <FeaturePlaceholder
      title="文件"
      description="文件列表与预览入口。上传进度属于客户端 UI 状态。"
      route="/app/files"
    />
  )
}
