import type { RowData, TableOptions } from '@tanstack/react-table'
import type { CSSProperties, ReactNode } from 'react'
import type { DataTableColumnDef } from '@/shared/components/data-table-config'
import { useTable } from '@tanstack/react-table'

import {
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  ChevronsUpDown,
  ChevronUp,
  Columns3,
  Search,
  SearchX,
} from 'lucide-react'
import { dataTableFeatures } from '@/shared/components/data-table-config'
import { Button } from '@/shared/components/ui/button'
import { Checkbox } from '@/shared/components/ui/checkbox'
import { Input } from '@/shared/components/ui/input'
import { Skeleton } from '@/shared/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/components/ui/table'
import { cn } from '@/shared/lib/utils'

export interface DataTableProps<TData extends RowData> {
  columns: Array<DataTableColumnDef<TData>>
  data: TData[]
  getRowId?: TableOptions<typeof dataTableFeatures, TData>['getRowId']
  emptyText?: string
  emptyDescription?: string
  searchPlaceholder?: string
  columnLabels?: Partial<Record<string, string>>
  toolbarActions?: ReactNode
  loading?: boolean
  selectable?: boolean
  pagination?: boolean
  pageSize?: number
  pageSizes?: number[]
  stickyHeader?: boolean
  density?: 'compact' | 'comfortable' | 'spacious'
  onRowClick?: (data: TData) => void
}

const densityStyles: Record<NonNullable<DataTableProps<RowData>['density']>, CSSProperties> = {
  compact: {
    '--row-height': '2.25rem',
    '--table-cell-padding-x': '0.625rem',
    '--table-cell-padding-y': '0.45rem',
  } as CSSProperties,
  comfortable: {
    '--row-height': '2.75rem',
    '--table-cell-padding-x': '0.75rem',
    '--table-cell-padding-y': '0.625rem',
  } as CSSProperties,
  spacious: {
    '--row-height': '3.25rem',
    '--table-cell-padding-x': '1rem',
    '--table-cell-padding-y': '0.875rem',
  } as CSSProperties,
}

function SortIndicator({ direction }: { direction: false | 'asc' | 'desc' }) {
  if (direction === 'asc') {
    return <ChevronUp className="size-3.5 text-primary" aria-hidden="true" />
  }

  if (direction === 'desc') {
    return <ChevronDown className="size-3.5 text-primary" aria-hidden="true" />
  }

  return <ChevronsUpDown className="size-3.5 opacity-45" aria-hidden="true" />
}

function LoadingRows({ columnCount, selectable }: { columnCount: number; selectable: boolean }) {
  return Array.from({ length: 5 }, (_, rowIndex) => (
    <TableRow key={rowIndex}>
      {selectable ? (
        <TableCell className="w-10">
          <Skeleton className="size-4 rounded" />
        </TableCell>
      ) : null}
      {Array.from({ length: columnCount }, (__, cellIndex) => (
        <TableCell key={cellIndex}>
          <Skeleton className={cn('h-3.5', cellIndex === 0 ? 'w-40' : 'w-20')} />
        </TableCell>
      ))}
    </TableRow>
  ))
}

export function DataTable<TData extends RowData>({
  columns,
  data,
  getRowId,
  emptyText = '暂无数据',
  emptyDescription = '调整筛选条件，或创建一条新记录。',
  searchPlaceholder = '搜索表格内容…',
  columnLabels = {},
  toolbarActions,
  loading = false,
  selectable = true,
  pagination = true,
  pageSize = 10,
  pageSizes = [5, 10, 20, 50],
  stickyHeader = true,
  density,
  onRowClick,
}: DataTableProps<TData>) {
  const table = useTable({
    features: dataTableFeatures,
    columns,
    data,
    getRowId,
    enableRowSelection: selectable,
    enableMultiRowSelection: true,
    globalFilterFn: 'includesString',
    initialState: {
      pagination: { pageIndex: 0, pageSize: pagination ? pageSize : Number.MAX_SAFE_INTEGER },
    },
  })

  const rows = table.getRowModel().rows
  const selectedCount = table.getSelectedRowIds().length
  const filteredCount = table.getFilteredRowModel().rows.length
  const pageCount = table.getPageCount()
  const pageIndex = table.state.pagination.pageIndex

  return (
    <div className="grid gap-3" style={density ? densityStyles[density] : undefined}>
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative w-full sm:max-w-xs">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={String(table.state.globalFilter ?? '')}
            onChange={(event) => table.setGlobalFilter(event.target.value)}
            placeholder={searchPlaceholder}
            aria-label="搜索表格"
            className="bg-card pl-9"
          />
        </div>

        <div className="flex items-center justify-end gap-2">
          {toolbarActions}
          <details className="group/columns relative">
            <summary className="flex h-9 cursor-pointer list-none items-center gap-2 rounded-md border bg-card px-3 text-sm font-medium shadow-xs transition-colors hover:bg-accent [&::-webkit-details-marker]:hidden">
              <Columns3 className="size-4" />
              <span className="hidden sm:inline">显示列</span>
              <ChevronDown className="size-3.5 transition-transform group-open/columns:rotate-180" />
            </summary>
            <div className="absolute right-0 z-50 mt-2 min-w-44 rounded-lg border bg-popover p-1.5 text-popover-foreground shadow-[var(--shadow-md)]">
              <p className="px-2 py-1.5 text-xs font-medium text-muted-foreground">切换字段</p>
              {table
                .getAllLeafColumns()
                .filter((column) => column.getCanHide())
                .map((column) => (
                  <label
                    key={column.id}
                    className="flex h-8 cursor-pointer items-center gap-2 rounded-md px-2 text-sm hover:bg-accent"
                  >
                    <Checkbox
                      checked={column.getIsVisible()}
                      onChange={column.getToggleVisibilityHandler()}
                      aria-label={`显示${columnLabels[column.id] ?? column.id}列`}
                    />
                    <span>{columnLabels[column.id] ?? column.id}</span>
                  </label>
                ))}
            </div>
          </details>
        </div>
      </div>

      <div className="overflow-hidden rounded-lg border bg-card">
        <Table>
          <TableHeader className={cn(stickyHeader && 'sticky top-0 z-10 bg-card')}>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id} className="hover:bg-transparent">
                {selectable ? (
                  <TableHead className="w-10">
                    <Checkbox
                      checked={table.getIsAllPageRowsSelected()}
                      indeterminate={
                        table.getIsSomePageRowsSelected() && !table.getIsAllPageRowsSelected()
                      }
                      onChange={table.getToggleAllPageRowsSelectedHandler()}
                      aria-label="选择当前页全部行"
                    />
                  </TableHead>
                ) : null}
                {headerGroup.headers.map((header) => {
                  const sorted = header.column.getIsSorted()
                  return (
                    <TableHead
                      key={header.id}
                      aria-sort={
                        sorted === 'asc' ? 'ascending' : sorted === 'desc' ? 'descending' : 'none'
                      }
                    >
                      {header.isPlaceholder ? null : header.column.getCanSort() ? (
                        <button
                          type="button"
                          className="flex h-8 items-center gap-1.5 rounded-md text-left transition-colors hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring/30"
                          onClick={header.column.getToggleSortingHandler()}
                        >
                          <table.FlexRender header={header} />
                          <SortIndicator direction={sorted} />
                        </button>
                      ) : (
                        <table.FlexRender header={header} />
                      )}
                    </TableHead>
                  )
                })}
              </TableRow>
            ))}
          </TableHeader>
          <TableBody>
            {loading ? (
              <LoadingRows
                columnCount={table.getVisibleLeafColumns().length}
                selectable={selectable}
              />
            ) : rows.length > 0 ? (
              rows.map((row) => (
                <TableRow
                  key={row.id}
                  data-state={row.getIsSelected() ? 'selected' : undefined}
                  className={cn(onRowClick && 'cursor-pointer')}
                  onClick={onRowClick ? () => onRowClick(row.original) : undefined}
                >
                  {selectable ? (
                    <TableCell className="w-10" onClick={(event) => event.stopPropagation()}>
                      <Checkbox
                        checked={row.getIsSelected()}
                        onChange={row.getToggleSelectedHandler()}
                        aria-label={`选择第 ${row.index + 1} 行`}
                      />
                    </TableCell>
                  ) : null}
                  {row.getVisibleCells().map((cell) => (
                    <TableCell key={cell.id}>
                      <table.FlexRender cell={cell} />
                    </TableCell>
                  ))}
                </TableRow>
              ))
            ) : (
              <TableRow className="hover:bg-transparent">
                <TableCell
                  colSpan={table.getVisibleLeafColumns().length + (selectable ? 1 : 0)}
                  className="h-44 text-center"
                >
                  <div className="mx-auto flex max-w-sm flex-col items-center gap-2 whitespace-normal">
                    <span className="grid size-9 place-items-center rounded-lg bg-secondary text-muted-foreground">
                      <SearchX className="size-4" />
                    </span>
                    <p className="font-medium text-foreground">{emptyText}</p>
                    <p className="text-xs leading-relaxed text-muted-foreground">
                      {emptyDescription}
                    </p>
                  </div>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex flex-col gap-2 text-xs text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
        <p aria-live="polite">
          {selectedCount > 0 ? `已选择 ${selectedCount} 项 · ` : ''}共 {filteredCount} 项
        </p>
        {pagination && pageCount > 0 ? (
          <div className="flex items-center justify-between gap-3 sm:justify-end">
            <label className="flex items-center gap-2">
              <span className="hidden md:inline">每页</span>
              <select
                value={table.state.pagination.pageSize}
                onChange={(event) => table.setPageSize(Number(event.target.value))}
                className="h-8 rounded-md border bg-card px-2 text-xs text-foreground shadow-xs focus-visible:ring-2 focus-visible:ring-ring/30"
                aria-label="每页显示行数"
              >
                {pageSizes.map((size) => (
                  <option key={size} value={size}>
                    {size}
                  </option>
                ))}
              </select>
            </label>
            <span>
              第{pageIndex + 1} /{pageCount} 页
            </span>
            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="icon-sm"
                onClick={() => table.firstPage()}
                disabled={!table.getCanPreviousPage()}
                aria-label="第一页"
              >
                <ChevronsLeft />
              </Button>
              <Button
                variant="outline"
                size="icon-sm"
                onClick={() => table.previousPage()}
                disabled={!table.getCanPreviousPage()}
                aria-label="上一页"
              >
                <ChevronLeft />
              </Button>
              <Button
                variant="outline"
                size="icon-sm"
                onClick={() => table.nextPage()}
                disabled={!table.getCanNextPage()}
                aria-label="下一页"
              >
                <ChevronRight />
              </Button>
              <Button
                variant="outline"
                size="icon-sm"
                onClick={() => table.lastPage()}
                disabled={!table.getCanNextPage()}
                aria-label="最后一页"
              >
                <ChevronsRight />
              </Button>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  )
}
