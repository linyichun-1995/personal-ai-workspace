import { FeaturePlaceholder } from '@/shared/components/feature-placeholder'

export function NotesPage() {
  return (
    <FeaturePlaceholder
      title="笔记"
      description="笔记列表页。自动保存状态机后续放在 note feature 内部，不进入全局 store。"
      route="/app/notes"
    />
  )
}
