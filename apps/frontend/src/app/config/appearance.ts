export const accentOptions = [
  { value: 'blue', label: '蓝色', swatchClass: 'bg-[oklch(0.56_0.19_252)]' },
  { value: 'indigo', label: '靛蓝', swatchClass: 'bg-[oklch(0.56_0.21_272)]' },
  { value: 'violet', label: '紫色', swatchClass: 'bg-[oklch(0.57_0.22_294)]' },
  { value: 'green', label: '绿色', swatchClass: 'bg-[oklch(0.52_0.145_155)]' },
  { value: 'orange', label: '橙色', swatchClass: 'bg-[oklch(0.64_0.16_55)]' },
  { value: 'rose', label: '玫瑰', swatchClass: 'bg-[oklch(0.58_0.205_10)]' },
] as const

export const densityOptions = [
  { value: 'compact', label: '紧凑', description: '适合表格与高密度工作' },
  { value: 'comfortable', label: '舒适', description: '默认，平衡信息与留白' },
  { value: 'spacious', label: '宽松', description: '更大的行高和页面间距' },
] as const

export type AccentColor = (typeof accentOptions)[number]['value']
export type UiDensity = (typeof densityOptions)[number]['value']

export const defaultAppearance = {
  accentColor: 'indigo' as AccentColor,
  density: 'comfortable' as UiDensity,
}
