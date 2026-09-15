import type { ReactNode } from 'react'
import type { AuthMode } from '@/features/auth/motion/auth-motion-controller'
import { MotionConfig } from 'motion/react'
import { createContext, use, useEffect, useState, useSyncExternalStore } from 'react'
import { AuthMotionController, getAuthPresentation } from '@/features/auth/motion/auth-motion-controller'
import { useMediaQuery } from '@/shared/hooks/use-media-query'

const AuthMotionContext = createContext<AuthMotionController | null>(null)

export function AuthMotionProvider({ children, mode }: { children: ReactNode, mode: AuthMode }) {
  const [controller] = useState(() => new AuthMotionController(mode))
  const reduced = useMediaQuery('(prefers-reduced-motion: reduce)')
  useEffect(() => {
    controller.start()
    return () => controller.dispose()
  }, [controller])
  useEffect(() => {
    controller.setMode(mode)
  }, [controller, mode])
  useEffect(() => {
    const sync = () => controller.setEnvironment(reduced, document.visibilityState !== 'hidden')
    sync()
    document.addEventListener('visibilitychange', sync)
    return () => document.removeEventListener('visibilitychange', sync)
  }, [controller, reduced])
  return <AuthMotionContext value={controller}><MotionConfig reducedMotion="user" transition={{ duration: 0.24, ease: 'easeInOut' }}>{children}</MotionConfig></AuthMotionContext>
}

export function useAuthMotion() {
  const controller = use(AuthMotionContext)
  if (!controller)
    throw new Error('Authentication components require AuthMotionProvider')
  const snapshot = useSyncExternalStore(controller.subscribe, controller.getSnapshot, controller.getSnapshot)
  return { controller, snapshot, visual: getAuthPresentation(snapshot) }
}
