import { Check, Monitor, Moon, RotateCcw, Sun } from 'lucide-react'
import { useTheme } from 'next-themes'

import { accentOptions, densityOptions } from '@/app/config/appearance'
import { ThemePreview } from '@/shared/components/theme-preview'
import { Button } from '@/shared/components/ui/button'
import { cn } from '@/shared/lib/utils'
import { useUiStore } from '@/stores/ui-store'

const themes = [
  { value: 'system', label: '跟随系统', icon: Monitor },
  { value: 'light', label: '浅色', icon: Sun },
  { value: 'dark', label: '深色', icon: Moon },
] as const

export function AppearanceSettingsPage() {
  const { theme = 'system', setTheme } = useTheme()
  const accentColor = useUiStore(state => state.accentColor)
  const density = useUiStore(state => state.density)
  const setAccentColor = useUiStore(state => state.setAccentColor)
  const setDensity = useUiStore(state => state.setDensity)
  const resetAppearance = useUiStore(state => state.resetAppearance)

  return (
    <>
      <div className="mb-7 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold">外观</h2>
          <p className="mt-1 text-xs text-muted-foreground">选择主题、强调色与界面密度。</p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          className="text-xs text-muted-foreground"
          onClick={() => {
            setTheme('system')
            resetAppearance()
          }}
        >
          <RotateCcw className="size-3.5" />
          恢复默认
        </Button>
      </div>

      <div className="grid max-w-3xl gap-7">
        <fieldset>
          <legend className="mb-3 text-[13px] font-medium">主题</legend>
          <div className="grid grid-cols-3 gap-2 sm:gap-4">
            {themes.map(({ value, label, icon: Icon }) => (
              <label key={value} className="group min-w-0 cursor-pointer">
                <input type="radio" name="theme" value={value} checked={theme === value} onChange={() => setTheme(value)} className="peer sr-only" />
                <span className="block rounded-lg border-2 border-transparent p-1 transition-colors group-hover:bg-surface-subtle peer-checked:border-primary peer-focus-visible:ring-2 peer-focus-visible:ring-ring/40 peer-focus-visible:ring-offset-2 peer-focus-visible:ring-offset-card">
                  {value === 'system'
                    ? (
                        <span className="relative block overflow-hidden rounded-md">
                          <ThemePreview theme="light" />
                          <span className="absolute inset-0 [clip-path:polygon(50%_0,100%_0,100%_100%,50%_100%)]"><ThemePreview theme="dark" /></span>
                        </span>
                      )
                    : <ThemePreview theme={value} />}
                </span>
                <span className="mt-2 flex items-center justify-center gap-1.5 text-xs text-muted-foreground peer-checked:font-medium peer-checked:text-foreground">
                  <Icon className="hidden size-3.5 sm:block" />
                  {label}
                </span>
              </label>
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend className="mb-3 text-[13px] font-medium">强调色</legend>
          <div className="grid grid-cols-6 gap-2 sm:max-w-md sm:gap-5">
            {accentOptions.map(option => (
              <label key={option.value} className="flex min-w-0 cursor-pointer flex-col items-center gap-2">
                <input type="radio" name="accent" value={option.value} checked={accentColor === option.value} onChange={() => setAccentColor(option.value)} className="peer sr-only" />
                <span className={cn('grid size-8 place-items-center rounded-full ring-offset-4 ring-offset-card transition-shadow peer-checked:ring-2 peer-checked:ring-primary peer-focus-visible:outline-2 peer-focus-visible:outline-offset-4 peer-focus-visible:outline-ring sm:size-9', option.swatchClass)}>
                  {accentColor === option.value && <Check className="size-4 text-white" strokeWidth={2.5} />}
                </span>
                <span className="text-[11px] text-muted-foreground peer-checked:font-medium peer-checked:text-foreground">{option.label}</span>
              </label>
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend className="mb-3 text-[13px] font-medium">界面密度</legend>
          <div className="grid gap-3 sm:grid-cols-3">
            {densityOptions.map((option, index) => (
              <label key={option.value} className="cursor-pointer">
                <input type="radio" name="density" value={option.value} checked={density === option.value} onChange={() => setDensity(option.value)} className="peer sr-only" />
                <span className="flex h-full flex-col rounded-lg border bg-card p-3 transition-colors hover:border-border-strong peer-checked:border-primary peer-checked:bg-primary-subtle/40 peer-focus-visible:ring-2 peer-focus-visible:ring-ring/40">
                  <span aria-hidden="true" className={cn('mb-3 hidden h-14 flex-col justify-center rounded-md bg-surface-subtle px-3 sm:flex', ['gap-1', 'gap-2', 'gap-3'][index])}>
                    {[0, 1, 2].map(row => (
                      <span key={row} className="flex items-center gap-2">
                        <span className="size-1.5 rounded-sm bg-primary/60" />
                        <span className="h-1 flex-1 rounded bg-border-strong/60" />
                        <span className="h-1 w-4 rounded bg-primary/25" />
                      </span>
                    ))}
                  </span>
                  <span className="flex items-center gap-2 text-xs font-medium">
                    <span className={cn('grid size-3.5 place-items-center rounded-full border', density === option.value ? 'border-primary' : 'border-border-strong')}>{density === option.value && <span className="size-1.5 rounded-full bg-primary" />}</span>
                    {option.label}
                    {option.value === 'comfortable' && <span className="font-normal text-muted-foreground">（默认）</span>}
                  </span>
                  <span className="mt-1.5 text-[11px] text-muted-foreground">{option.description}</span>
                </span>
              </label>
            ))}
          </div>
        </fieldset>

        <p className="flex items-center gap-1.5 border-t border-border-subtle pt-4 text-xs text-muted-foreground" role="status">
          <Check className="size-3.5 text-success" />
          外观偏好已自动保存在此浏览器
        </p>
      </div>
    </>
  )
}
