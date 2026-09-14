import { getRouteApi } from '@tanstack/react-router'

import { FeaturePlaceholder } from '@/shared/components/feature-placeholder'

const routeApi = getRouteApi('/app/projects/$projectId')

export function ProjectDetailPage() {
  const { projectId } = routeApi.useParams()

  return (
    <FeaturePlaceholder
      title="项目详情"
      description={`路由参数 projectId=${projectId}。详情缓存使用 queryKeys.project.detail。`}
      route="/app/projects/$projectId"
    />
  )
}
