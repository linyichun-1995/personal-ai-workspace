import type { TaskItem, TaskPriority, TaskStatus } from '@/features/task/types'
import { MoreHorizontal } from 'lucide-react'

import { DataTable } from '@/shared/components/data-table'
import { createDataTableColumnHelper } from '@/shared/components/data-table-config'
import { StatusBadge } from '@/shared/components/status-badge'
import { Button } from '@/shared/components/ui/button'

const columnHelper = createDataTableColumnHelper<TaskItem>()

const statusMap: Record<TaskStatus, { label: string, tone: 'neutral' | 'info' | 'warning' | 'success' }> = {
  'todo': { label: '待处理', tone: 'neutral' },
  'in-progress': { label: '进行中', tone: 'info' },
  'review': { label: '待评审', tone: 'warning' },
  'done': { label: '已完成', tone: 'success' },
}

const priorityMap: Record<TaskPriority, { label: string, tone: 'success' | 'warning' | 'danger' }> = {
  low: { label: '低', tone: 'success' },
  medium: { label: '中', tone: 'warning' },
  high: { label: '高', tone: 'danger' },
}

const taskColumns = columnHelper.columns([
  columnHelper.accessor('title', {
    header: '任务',
    cell: ({ row, getValue }) => (
      <div className="min-w-52">
        <p className="truncate font-medium">{getValue()}</p>
        <p className="mt-0.5 text-[11px] text-muted-foreground">{row.original.id}</p>
      </div>
    ),
    sortFn: 'alphanumeric',
  }),
  columnHelper.accessor('project', {
    header: '项目',
    cell: ({ getValue }) => (
      <div className="flex items-center gap-2">
        <span className="size-2 rounded-sm bg-primary/65" />
        <span className="max-w-44 truncate">{getValue()}</span>
      </div>
    ),
    sortFn: 'alphanumeric',
  }),
  columnHelper.accessor('status', {
    header: '状态',
    cell: ({ getValue }) => {
      const status = statusMap[getValue()]
      return <StatusBadge tone={status.tone} dot>{status.label}</StatusBadge>
    },
    sortFn: 'alphanumeric',
  }),
  columnHelper.accessor('priority', {
    header: '优先级',
    cell: ({ getValue }) => {
      const priority = priorityMap[getValue()]
      return <StatusBadge tone={priority.tone}>{priority.label}</StatusBadge>
    },
    sortFn: 'alphanumeric',
  }),
  columnHelper.accessor('dueAt', {
    header: '截止时间',
    cell: ({ getValue }) => <span className="text-muted-foreground">{getValue()}</span>,
    sortFn: 'alphanumeric',
  }),
  columnHelper.accessor('assignee', {
    header: '负责人',
    cell: ({ getValue }) => (
      <div className="flex items-center gap-2">
        <span className="grid size-6 place-items-center rounded-full bg-secondary text-[10px] font-semibold text-secondary-foreground">
          {getValue().slice(0, 1)}
        </span>
        <span>{getValue()}</span>
      </div>
    ),
    sortFn: 'alphanumeric',
  }),
  columnHelper.display({
    id: 'actions',
    header: '',
    enableHiding: false,
    enableSorting: false,
    cell: ({ row }) => (
      <Button
        variant="ghost"
        size="icon-sm"
        aria-label={`打开任务 ${row.original.title} 的更多操作`}
        onClick={event => event.stopPropagation()}
      >
        <MoreHorizontal />
      </Button>
    ),
  }),
])

const columnLabels = {
  title: '任务',
  project: '项目',
  status: '状态',
  priority: '优先级',
  dueAt: '截止时间',
  assignee: '负责人',
}

export function TaskTable({ data }: { data: TaskItem[] }) {
  return (
    <DataTable
      columns={taskColumns}
      data={data}
      getRowId={row => row.id}
      searchPlaceholder="搜索任务、项目或负责人…"
      emptyText="没有找到任务"
      emptyDescription="试试其他关键词，或清除当前筛选条件。"
      columnLabels={columnLabels}
      pageSize={5}
    />
  )
}
