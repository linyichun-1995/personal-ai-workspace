import { getRouteApi } from '@tanstack/react-router'

import { FeaturePlaceholder } from '@/shared/components/feature-placeholder'

const routeApi = getRouteApi('/app/notes/$noteId')

export function NoteDetailPage() {
  const { noteId } = routeApi.useParams()

  return (
    <FeaturePlaceholder
      title="笔记详情"
      description={`路由参数 noteId=${noteId}。详情与草稿只放在 note feature。`}
      route="/app/notes/$noteId"
    />
  )
}
