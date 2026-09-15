import type { TaskStatus, TaskTableItem } from '@/features/task/types'
import { Pencil } from 'lucide-react'

import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS } from '@/features/task/types'
import { DataTable } from '@/shared/components/data-table'
import { createDataTableColumnHelper } from '@/shared/components/data-table-config'
import { StatusBadge } from '@/shared/components/status-badge'
import { Button } from '@/shared/components/ui/button'
import { Checkbox } from '@/shared/components/ui/checkbox'

const columnHelper = createDataTableColumnHelper<TaskTableItem>()
type TaskTableProps = {
  data: TaskTableItem[]
  loading?: boolean
  busy?: boolean
  pagination?: boolean
  searchPlaceholder?: string
  onRowClick?: (item: TaskTableItem) => void
  onToggleComplete?: (item: TaskTableItem, completed: boolean) => void
  onStatusChange?: (item: TaskTableItem, status: TaskStatus) => void
}

function createTaskColumns({ onToggleComplete, onRowClick, onStatusChange, busy }: TaskTableProps) {
  return columnHelper.columns([
    columnHelper.accessor('title', {
      header: '任务',
      enableHiding: false,
      cell: ({ row, getValue }) => (
        <div className="flex min-w-52 items-center gap-3">
          {onToggleComplete && (
            <Checkbox
              aria-label={
                (row.original.status === 'DONE' ? '重新打开任务：' : '完成任务：') + getValue()
              }
              checked={row.original.status === 'DONE'}
              disabled={busy || row.original.status === 'CANCELLED'}
              onChange={(event) => onToggleComplete(row.original, event.target.checked)}
            />
          )}
          {onRowClick ? (
            <button
              type="button"
              onClick={() => onRowClick(row.original)}
              className="max-w-sm truncate rounded-sm text-left font-medium hover:text-primary focus-visible:ring-2 focus-visible:ring-ring/40"
            >
              {getValue()}
            </button>
          ) : (
            <span className="max-w-sm truncate font-medium">{getValue()}</span>
          )}
        </div>
      ),
      sortFn: 'alphanumeric',
    }),
    columnHelper.accessor('project', {
      header: '所属项目',
      cell: ({ getValue }) => (
        <span className="block max-w-44 truncate text-muted-foreground">{getValue()}</span>
      ),
    }),
    columnHelper.accessor('status', {
      header: '状态',
      cell: ({ row, getValue }) =>
        onStatusChange ? (
          <select
            aria-label={row.original.title + '的状态'}
            value={getValue()}
            disabled={busy}
            onChange={(event) => onStatusChange(row.original, event.target.value as TaskStatus)}
            className="h-8 rounded-md border border-border-subtle bg-card px-2 text-xs text-foreground focus-visible:ring-2 focus-visible:ring-ring/40 disabled:opacity-50"
          >
            {Object.entries(TASK_STATUS_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        ) : (
          <StatusBadge
            tone={
              getValue() === 'DONE' ? 'success' : getValue() === 'IN_PROGRESS' ? 'info' : 'neutral'
            }
          >
            {TASK_STATUS_LABELS[getValue()]}
          </StatusBadge>
        ),
    }),
    columnHelper.accessor('priority', {
      header: '优先级',
      cell: ({ getValue }) => (
        <StatusBadge
          tone={
            getValue() === 'URGENT' || getValue() === 'HIGH'
              ? 'danger'
              : getValue() === 'MEDIUM'
                ? 'warning'
                : 'neutral'
          }
        >
          {TASK_PRIORITY_LABELS[getValue()]}
        </StatusBadge>
      ),
      sortFn: (a, b) =>
        ['LOW', 'MEDIUM', 'HIGH', 'URGENT'].indexOf(a.original.priority) -
        ['LOW', 'MEDIUM', 'HIGH', 'URGENT'].indexOf(b.original.priority),
    }),
    columnHelper.accessor('dueAt', {
      header: '截止时间',
      cell: ({ row, getValue }) => {
        const task = row.original.raw
        const overdue =
          task.dueAt &&
          new Date(task.dueAt).getTime() < Date.now() &&
          task.status !== 'DONE' &&
          task.status !== 'CANCELLED'
        return (
          <span className={overdue ? 'text-destructive' : 'text-muted-foreground'}>
            {getValue()}
            {overdue ? ' · 已到期' : ''}
          </span>
        )
      },
      sortFn: (a, b) =>
        (a.original.raw.dueAt ? Date.parse(a.original.raw.dueAt) : Number.MAX_SAFE_INTEGER) -
        (b.original.raw.dueAt ? Date.parse(b.original.raw.dueAt) : Number.MAX_SAFE_INTEGER),
    }),
    columnHelper.display({
      id: 'actions',
      header: '操作',
      enableHiding: false,
      enableSorting: false,
      cell: ({ row }) =>
        onRowClick ? (
          <Button variant="ghost" size="sm" onClick={() => onRowClick(row.original)}>
            <Pencil className="size-3.5" />
            编辑
          </Button>
        ) : null,
    }),
  ])
}

export function TaskTable(props: TaskTableProps) {
  return (
    <DataTable
      columns={createTaskColumns(props)}
      data={props.data}
      getRowId={(row) => row.id}
      selectable={false}
      searchPlaceholder={props.searchPlaceholder ?? '搜索已加载的任务或项目…'}
      pagination={props.pagination}
      emptyText="没有匹配的任务"
      emptyDescription="换一个关键词，或清空搜索内容。"
      columnLabels={{
        title: '任务',
        project: '所属项目',
        status: '状态',
        priority: '优先级',
        dueAt: '截止时间',
      }}
      pageSize={10}
      loading={props.loading}
    />
  )
}
