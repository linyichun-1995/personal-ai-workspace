import { Bell, Check, CircleHelp, CircleUserRound, Keyboard, Monitor, Moon, Palette, Plug, RotateCcw, Shield, Sun } from 'lucide-react'
import { useTheme } from 'next-themes'

import { accentOptions, densityOptions } from '@/app/config/appearance'
import { PageContainer } from '@/shared/components/page-container'
import { ThemePreview } from '@/shared/components/theme-preview'
import { Button } from '@/shared/components/ui/button'
import { cn } from '@/shared/lib/utils'
import { useUiStore } from '@/stores/ui-store'

const themes = [
  { value: 'system', label: '跟随系统', icon: Monitor },
  { value: 'light', label: '浅色', icon: Sun },
  { value: 'dark', label: '深色', icon: Moon },
] as const

const settingsItems = [
  { label: '个人资料', icon: CircleUserRound },
  { label: '外观', icon: Palette },
  { label: '通知', icon: Bell },
  { label: '集成', icon: Plug },
  { label: '隐私与权限', icon: Shield },
  { label: '快捷键', icon: Keyboard },
  { label: '关于', icon: CircleHelp },
]

export function SettingsPage() {
  const { theme = 'system', setTheme } = useTheme()
  const accentColor = useUiStore(state => state.accentColor)
  const density = useUiStore(state => state.density)
  const setAccentColor = useUiStore(state => state.setAccentColor)
  const setDensity = useUiStore(state => state.setDensity)
  const resetAppearance = useUiStore(state => state.resetAppearance)

  return (
    <PageContainer width="form" className="grid gap-5 lg:ml-0">
      <header>
        <h1 className="text-xl font-semibold tracking-tight">设置</h1>
        <p className="mt-1 text-[13px] text-muted-foreground">让工作空间更适合你的习惯。</p>
      </header>
      <div className="grid min-w-0 overflow-hidden rounded-xl border border-border-subtle bg-card lg:grid-cols-[180px_minmax(0,1fr)]">
        <nav aria-label="设置导航" className="flex gap-1 overflow-x-auto border-b border-border-subtle bg-surface/60 p-3 lg:flex-col lg:border-b-0 lg:border-r lg:py-5">
          {settingsItems.map(({ label, icon: Icon }) => (
            <button key={label} type="button" disabled={label !== '外观'} aria-current={label === '外观' ? 'page' : undefined} className={cn('flex min-h-10 shrink-0 items-center gap-2.5 rounded-md px-3 text-left text-xs text-muted-foreground disabled:cursor-default', label === '外观' && 'bg-primary-subtle font-medium text-primary focus-visible:ring-2 focus-visible:ring-ring/40')}>
              <Icon className="size-4" strokeWidth={1.7} />
              {label}
            </button>
          ))}
        </nav>

        <div className="min-w-0 p-5 sm:p-7 lg:p-8">
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
        </div>
      </div>
    </PageContainer>
  )
}
