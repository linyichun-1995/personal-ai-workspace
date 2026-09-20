import { describe, expect, it } from 'vitest'
import { queryValidation, safeOrigin, validateGlobalSearch } from './search-params'

describe('content search URL boundaries', () => {
  it('keeps supported filters across navigation and caps the result window', () => {
    const id = '00000000-0000-0000-0000-000000000001'
    const result = validateGlobalSearch({
      q: '中文 文档',
      types: ['FILE', 'NOTE', 'UNKNOWN'],
      tagIds: [id, 'bad'],
      tagMode: 'ANY',
      page: 500,
      includeArchived: 'true',
      sort: 'updatedAt,desc',
    })
    expect(result).toMatchObject({
      q: '中文 文档',
      types: ['FILE', 'NOTE'],
      tagIds: [id],
      tagMode: 'ANY',
      page: 100,
      includeArchived: true,
      sort: 'updatedAt,desc',
    })
  })
  it('allows filter-only search and rejects one-character terms', () => {
    expect(queryValidation('')).toBeUndefined()
    expect(queryValidation('中文')).toBeUndefined()
    expect(queryValidation('中')).toBeDefined()
    expect(queryValidation('aa '.repeat(9))).toBeDefined()
  })
  it('rejects external and unexpected return destinations', () => {
    for (const value of [
      'https://evil.invalid',
      '//evil.invalid',
      '/app/../admin',
      '/app/\\evil.invalid',
      '/app/settings',
    ])
      expect(safeOrigin(value)).toBeUndefined()
    expect(safeOrigin('/app/search?q=test&page=2&token=private')).toBe('/app/search?q=test&page=2')
  })
  it('preserves a serialized tag selection on return', () => {
    const value = '/app/search?tagIds=%5B%2200000000-0000-0000-0000-000000000001%22%5D&tagMode=ALL'
    expect(safeOrigin(value)).toBe(value)
  })
})
