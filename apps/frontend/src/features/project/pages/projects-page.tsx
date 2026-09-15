import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useState } from 'react'

import { createProject, listProjects } from '@/features/project/api/projects'
import { ProjectCard } from '@/features/project/components/project-card'
import { ProjectFormDialog } from '@/features/project/components/project-form-dialog'
import { FeaturePlaceholder } from '@/shared/components/feature-placeholder'
import { PageContainer } from '@/shared/components/page-container'
import { PageHeader } from '@/shared/components/page-header'
import { QueryState } from '@/shared/components/query-state'
import { Button } from '@/shared/components/ui/button'
import { useSession } from '@/features/auth/hooks/use-session'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { queryKeys } from '@/shared/api/query-keys'

export function ProjectsPage({ view = 'all' }: { view?: 'all' | 'active' | 'archived' | 'templates' }) {
  const session = useSession()
  const workspaceId = session.data?.workspace.id
  const queryClient = useQueryClient()
  const [createOpen, setCreateOpen] = useState(false)

  const titles = { all: '全部项目', active: '进行中的项目', archived: '已归档项目', templates: '项目模板' }

  const query = useQuery({
    queryKey: queryKeys.project.list(workspaceId ?? '', { view }),
    enabled: Boolean(workspaceId) && view !== 'templates',
    queryFn: () => listProjects({
      archived: view === 'archived',
      status: view === 'active' ? 'ACTIVE,PLANNED' : undefined,
      size: 50,
      sort: 'updatedAt,desc',
    }),
  })

  const createMutation = useMutation({
    mutationFn: createProject,
    onSuccess: async () => {
      if (workspaceId) {
        invalidateWorkspaceData(queryClient, workspaceId)
      }
    },
  })

  if (view === 'templates') {
    return (
      <FeaturePlaceholder
        title={titles.templates}
        description="整理项目与计划，让每一步进展清晰可见。"
        route="/app/projects/templates"
      />
    )
  }

  return (
    <PageContainer className="grid gap-5">
      <PageHeader
        eyebrow="工作台 / 项目"
        title={titles[view]}
        description="用项目组织任务和笔记，跟踪进度与截止日期。"
        actions={(
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="size-4" />
            创建项目
          </Button>
        )}
      />
      <QueryState
        query={query}
        isEmpty={data => data.items.length === 0}
        empty={{
          title: view === 'archived' ? '还没有归档项目' : '还没有项目',
          description: view === 'archived' ? '归档后的项目会出现在这里。' : '创建一个项目，开始安排任务和笔记。',
          action: view === 'archived'
            ? undefined
            : <Button onClick={() => setCreateOpen(true)}>创建第一个项目</Button>,
        }}
      >
        {data => (
          <div className="grid gap-3">
            {data.items.map(project => <ProjectCard key={project.id} project={project} />)}
          </div>
        )}
      </QueryState>
      <ProjectFormDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        submitting={createMutation.isPending}
        onSubmit={async (values) => {
          await createMutation.mutateAsync({
            name: values.name,
            description: values.description || null,
            status: values.status,
            priority: values.priority,
            startDate: values.startDate || null,
            dueDate: values.dueDate || null,
          })
        }}
      />
    </PageContainer>
  )
}
