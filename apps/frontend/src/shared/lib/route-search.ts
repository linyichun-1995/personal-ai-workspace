export function readPage(value: unknown): number | undefined {
  const page = typeof value === 'string' ? Number(value) : value
  return typeof page === 'number' && Number.isSafeInteger(page) && page > 1 && page <= 1_000_000
    ? page
    : undefined
}

export function asSearchRecord(value: unknown): Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {}
}
