import { FeaturePlaceholder } from '@/shared/components/feature-placeholder'

export function SearchPage() {
  return (
    <FeaturePlaceholder
      title="搜索"
      description="关键词 / 语义检索结果页。查询词应同步到 Search Params。"
      route="/app/search"
    />
  )
}
