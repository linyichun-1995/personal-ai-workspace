import type { ReactNode } from 'react'
import { ThemeProvider as NextThemesProvider } from 'next-themes'
import { useEffect } from 'react'

import { useUiStore } from '@/stores/ui-store'

function AppearanceSync({ children }: { children: ReactNode }) {
  const accentColor = useUiStore(state => state.accentColor)
  const density = useUiStore(state => state.density)

  useEffect(() => {
    const root = document.documentElement
    root.dataset.accent = accentColor
    root.dataset.density = density
  }, [accentColor, density])

  return children
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  return (
    <NextThemesProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange storageKey="ai-workspace-theme">
      <AppearanceSync>{children}</AppearanceSync>
    </NextThemesProvider>
  )
}
