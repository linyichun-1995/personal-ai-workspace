export function validateTaskSearch(search: Record<string, unknown>): {
  create?: boolean
  taskId?: string
} {
  return {
    create: search.create === true ? true : undefined,
    taskId:
      typeof search.taskId === 'string' && /^[\da-f-]{36}$/i.test(search.taskId)
        ? search.taskId
        : undefined,
  }
}
