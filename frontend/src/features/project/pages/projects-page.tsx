import { FeaturePlaceholder } from '@/shared/components/feature-placeholder'

export function ProjectsPage({ view = 'all' }: { view?: 'all' | 'active' | 'archived' | 'templates' }) {
  const titles = { all: '全部项目', active: '进行中的项目', archived: '已归档项目', templates: '项目模板' }
  return (
    <FeaturePlaceholder
      title={titles[view]}
      description="整理项目与计划，让每一步进展清晰可见。"
      route={view === 'all' ? '/app/projects' : `/app/projects/${view}`}
    />
  )
}
