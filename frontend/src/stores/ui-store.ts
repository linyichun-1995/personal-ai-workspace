import type { AccentColor, UiDensity } from '@/app/config/appearance'
import { create } from 'zustand'

import { persist } from 'zustand/middleware'
import { defaultAppearance } from '@/app/config/appearance'

interface UiState {
  sidebarCollapsed: boolean
  mobileNavOpen: boolean
  commandPaletteOpen: boolean
  accentColor: AccentColor
  density: UiDensity
  toggleSidebar: () => void
  setSidebarCollapsed: (collapsed: boolean) => void
  setMobileNavOpen: (open: boolean) => void
  setCommandPaletteOpen: (open: boolean) => void
  setAccentColor: (accentColor: AccentColor) => void
  setDensity: (density: UiDensity) => void
  resetAppearance: () => void
}

export const useUiStore = create<UiState>()(
  persist(
    set => ({
      sidebarCollapsed: false,
      mobileNavOpen: false,
      commandPaletteOpen: false,
      accentColor: defaultAppearance.accentColor,
      density: defaultAppearance.density,
      toggleSidebar: () => {
        set(state => ({ sidebarCollapsed: !state.sidebarCollapsed }))
      },
      setSidebarCollapsed: (collapsed) => {
        set({ sidebarCollapsed: collapsed })
      },
      setMobileNavOpen: (open) => {
        set({ mobileNavOpen: open })
      },
      setCommandPaletteOpen: (open) => {
        set({ commandPaletteOpen: open })
      },
      setAccentColor: (accentColor) => {
        set({ accentColor })
      },
      setDensity: (density) => {
        set({ density })
      },
      resetAppearance: () => {
        set(defaultAppearance)
      },
    }),
    {
      name: 'ai-workspace-ui',
      partialize: state => ({
        sidebarCollapsed: state.sidebarCollapsed,
        accentColor: state.accentColor,
        density: state.density,
      }),
    },
  ),
)
