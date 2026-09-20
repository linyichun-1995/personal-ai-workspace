import {
  safeOrigin,
  stringArray,
  validateContentFilters,
  type ContentFilters,
} from '@/features/search/lib/search-params'
export interface FileSearch extends ContentFilters {
  view?: 'all' | 'uncategorized' | 'trash'
  mediaTypes?: string[]
  extractionStatus?: string
}
export const FILE_MEDIA = {
  'application/pdf': 'PDF',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'DOCX',
  'text/plain': 'TXT',
  'text/markdown': 'MD',
  'image/png': 'PNG',
  'image/jpeg': 'JPEG',
  'image/webp': 'WebP',
}
export const EXTRACTION_LABELS: Record<string, string> = {
  QUEUED: '正文待处理',
  PROCESSING: '正文处理中',
  READY: '正文可搜索',
  EMPTY: '未提取到文本',
  SKIPPED: '仅名称可搜',
  FAILED: '正文解析失败',
}
export function validateFileSearch(search: Record<string, unknown>): FileSearch {
  return {
    ...validateContentFilters(search),
    view: search.view === 'uncategorized' || search.view === 'trash' ? search.view : 'all',
    mediaTypes: stringArray(search.mediaTypes).filter((type) => Object.hasOwn(FILE_MEDIA, type)),
    extractionStatus:
      typeof search.extractionStatus === 'string' &&
      Object.hasOwn(EXTRACTION_LABELS, search.extractionStatus)
        ? search.extractionStatus
        : undefined,
    sort:
      search.sort === 'name,asc' || search.sort === 'sizeBytes,desc'
        ? search.sort
        : 'updatedAt,desc',
  }
}
export function validateFileDetailSearch(search: Record<string, unknown>): { origin?: string } {
  return { origin: safeOrigin(search.origin ?? search.from) }
}
export function formatBytes(bytes: number) {
  return bytes < 1024
    ? `${bytes} B`
    : bytes < 1048576
      ? `${(bytes / 1024).toFixed(1)} KiB`
      : `${(bytes / 1048576).toFixed(1)} MiB`
}
