export const TAG_COLORS = {
  GRAY: '灰色',
  RED: '红色',
  ORANGE: '橙色',
  YELLOW: '黄色',
  GREEN: '绿色',
  BLUE: '蓝色',
  PURPLE: '紫色',
  PINK: '粉色',
} as const
export interface Tag {
  id: string
  name: string
  color: keyof typeof TAG_COLORS
  version: number
  referenceCount: number
}
export type TagResource = 'projects' | 'tasks' | 'notes' | 'files'
export interface TagCollection {
  tags: Tag[]
  tagVersion: number
}
