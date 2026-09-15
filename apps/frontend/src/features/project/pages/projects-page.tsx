import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useRouter, useSearch } from '@tanstack/react-router'
import { Plus } from 'lucide-react'
import { useRef, useState } from 'react'

import { createProject, listProjects } from '@/features/project/api/projects'
import { ProjectCard } from '@/features/project/components/project-card'
import { ProjectFormDialog } from '@/features/project/components/project-form-dialog'
import { ListPagination } from '@/shared/components/list-pagination'
import { FeaturePlaceholder } from '@/shared/components/feature-placeholder'
import { PageContainer } from '@/shared/components/page-container'
import { PageHeader } from '@/shared/components/page-header'
import { QueryState } from '@/shared/components/query-state'
import { Button } from '@/shared/components/ui/button'
import { useSession } from '@/features/auth/hooks/use-session'
import { invalidateWorkspaceData } from '@/shared/api/invalidate'
import { queryKeys } from '@/shared/api/query-keys'

export function ProjectsPage({
  view = 'all',
}: {
  view?: 'all' | 'active' | 'archived' | 'templates'
}) {
  const session = useSession()
  const workspaceId = session.data?.workspace.id
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const router = useRouter()
  const search = useSearch({ strict: false })
  const [createOpen, setCreateOpen] = useState(false)
  const page = search.page ?? 1
  const listRoute =
    view === 'active'
      ? '/app/projects/active'
      : view === 'archived'
        ? '/app/projects/archived'
        : '/app/projects'
  const origin = {
    view: view === 'active' || view === 'archived' ? view : undefined,
    page: search.page,
  }
  const setPage = (next: number) =>
    void navigate({ to: listRoute, search: { page: next > 1 ? next : undefined } })
  const createdProjectId = useRef<string | null>(null)

  const titles = {
    all: '全部项目',
    active: '进行中的项目',
    archived: '已归档项目',
    templates: '项目模板',
  }

  const query = useQuery({
    queryKey: queryKeys.project.list(workspaceId ?? '', { view, page }),
    enabled: Boolean(workspaceId) && view !== 'templates',
    queryFn: () =>
      listProjects({
        archived: view === 'archived',
        status: view === 'active' ? 'ACTIVE' : undefined,
        page,
        size: 20,
        sort: 'updatedAt,desc',
      }),
  })

  const createMutation = useMutation({
    mutationFn: createProject,
    onSuccess: async () => {
      if (workspaceId) {
        await invalidateWorkspaceData(queryClient, workspaceId)
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
        title="项目"
        description="用项目组织任务和笔记，跟踪进度与截止日期。"
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="size-4" />
            创建项目
          </Button>
        }
      />
      <nav className="flex gap-1 border-b border-border-subtle" aria-label="项目视图">
        {(
          [
            { value: 'all', to: '/app/projects', label: '全部项目' },
            { value: 'active', to: '/app/projects/active', label: '进行中' },
            { value: 'archived', to: '/app/projects/archived', label: '已归档' },
          ] as const
        ).map((item) => (
          <Link
            key={item.value}
            to={item.to}
            search={{}}
            aria-current={view === item.value ? 'page' : undefined}
            className={
              'border-b-2 px-3 py-3 text-sm focus-visible:ring-2 focus-visible:ring-ring/40 ' +
              (view === item.value
                ? 'border-primary font-medium text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground')
            }
          >
            {item.label}
          </Link>
        ))}
      </nav>
      <QueryState
        query={query}
        isEmpty={(data) => data.items.length === 0}
        empty={{
          title:
            page > 1
              ? '这一页没有项目'
              : view === 'archived'
                ? '还没有归档项目'
                : view === 'active'
                  ? '没有进行中的项目'
                  : '还没有项目',
          description:
            page > 1
              ? '返回第一页继续查看项目。'
              : view === 'archived'
                ? '归档后的项目会出现在这里。'
                : '创建一个项目，开始安排任务和笔记。',
          action:
            view === 'archived' || page > 1 ? undefined : (
              <Button onClick={() => setCreateOpen(true)}>创建第一个项目</Button>
            ),
        }}
      >
        {(data) => (
          <div className="grid gap-3">
            {data.items.map((project) => (
              <ProjectCard key={project.id} project={project} origin={origin} />
            ))}
          </div>
        )}
      </QueryState>
      {query.data && (
        <ListPagination
          page={page}
          total={query.data.total}
          totalPages={query.data.totalPages}
          loading={query.isFetching}
          onPageChange={setPage}
        />
      )}
      <ProjectFormDialog
        open={createOpen || (view === 'all' && Boolean(search.create))}
        onOpenChange={(open, navigating) => {
          setCreateOpen(open)
          if (navigating) return
          if (!open && createdProjectId.current) {
            const projectId = createdProjectId.current
            createdProjectId.current = null
            void navigate({
              to: '/app/projects/$projectId',
              params: { projectId },
              search: { tab: 'tasks', from: origin },
            })
            return
          }
          if (!open && search.create && router.state.location.pathname === '/app/projects') {
            void navigate({ to: '/app/projects', search: { page: search.page }, replace: true })
          }
        }}
        submitting={createMutation.isPending}
        onSubmit={async (values) => {
          const project = await createMutation.mutateAsync({
            name: values.name,
            description: values.description || null,
            status: values.status,
            priority: values.priority,
            startDate: values.startDate || null,
            dueDate: values.dueDate || null,
          })
          createdProjectId.current = project.id
        }}
      />
    </PageContainer>
  )
}
