import { api } from '@/shared/api'
import type { PaginatedResult } from '@/shared/types/api'
import type { Tag, TagCollection, TagResource } from '@/features/tag/types'
export const tagApi = {
  list: (q = '', page = 1, signal?: AbortSignal) =>
    api.get<PaginatedResult<Tag>>('/v1/tags', { query: { q, page, size: 100 }, signal }),
  create: (name: string, color: Tag['color'] = 'GRAY') =>
    api.post<Tag>('/v1/tags', { name, color }),
  update: (tag: Tag, input: { name: string; color: Tag['color'] }) =>
    api.patch<Tag>(`/v1/tags/${tag.id}`, { ...input, version: tag.version }),
  remove: (tag: Tag) =>
    api.delete<void>(`/v1/tags/${tag.id}`, { headers: { 'If-Match': `"${tag.version}"` } }),
  collection: (resource: TagResource, id: string) =>
    api.get<TagCollection>(`/v1/${resource}/${id}/tags`),
  replace: (resource: TagResource, id: string, tagIds: string[], tagVersion: number) =>
    api.put<TagCollection>(`/v1/${resource}/${id}/tags`, { tagIds, tagVersion }),
}
