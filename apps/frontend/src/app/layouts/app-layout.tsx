import { Outlet } from '@tanstack/react-router'
import { motion, useReducedMotion } from 'motion/react'
import { useEffect } from 'react'

import { AppHeader } from '@/app/layouts/app-header'
import { AppSidebar } from '@/app/layouts/app-sidebar'
import { CommandPalette } from '@/shared/components/command-palette'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/shared/components/ui/sheet'
import { useIsDesktop, useIsWideDesktop } from '@/shared/hooks/use-media-query'
import { useUiStore } from '@/stores/ui-store'

export function AppLayout() {
  const reducedMotion = useReducedMotion()
  const isDesktop = useIsDesktop()
  const isWideDesktop = useIsWideDesktop()
  const mobileNavOpen = useUiStore(state => state.mobileNavOpen)
  const setMobileNavOpen = useUiStore(state => state.setMobileNavOpen)
  const toggleSidebar = useUiStore(state => state.toggleSidebar)

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      const target = event.target
      const isEditing = target instanceof HTMLElement
        && (target.matches('input, textarea, select') || target.isContentEditable)

      if (!isEditing && isWideDesktop && (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'b') {
        event.preventDefault()
        toggleSidebar()
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [isWideDesktop, toggleSidebar])

  return (
    <motion.div className="flex min-h-dvh bg-background" initial={{ opacity: 0, scale: reducedMotion ? 1 : 1.01 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.35, ease: 'easeOut' }}>
      {isDesktop
        ? (
            <div className="sticky top-0 hidden h-dvh md:block">
              <AppSidebar />
            </div>
          )
        : (
            <Sheet open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
              <SheetContent side="left" className="w-72 p-0">
                <SheetHeader className="sr-only">
                  <SheetTitle>导航</SheetTitle>
                  <SheetDescription>应用主导航</SheetDescription>
                </SheetHeader>
                <AppSidebar
                  forceExpanded
                  className="h-full w-full border-0"
                  onNavigate={() => {
                    setMobileNavOpen(false)
                  }}
                />
              </SheetContent>
            </Sheet>
          )}

      <div className="flex min-w-0 flex-1 flex-col">
        <AppHeader />
        <main className="min-h-0 flex-1">
          <Outlet />
        </main>
      </div>
      <CommandPalette />
    </motion.div>
  )
}
