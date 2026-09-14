import { AsyncState } from '@/shared/components/async-state'
import { PageHeader } from '@/shared/components/page-header'
import { Badge } from '@/shared/components/ui/badge'

export interface FeaturePlaceholderProps {
  title: string
  description: string
  route?: string
}

export function FeaturePlaceholder({ title, description, route }: FeaturePlaceholderProps) {
  return (
    <div className="grid gap-6">
      <PageHeader
        title={title}
        description={description}
        actions={route ? <Badge variant="outline">{route}</Badge> : undefined}
      />
      <AsyncState
        status="empty"
        empty={{
          title: '业务尚未接入',
          description: '当前仅为架构占位页，后续在对应 feature 中接入 Query 与表单即可。',
        }}
      />
    </div>
  )
}
