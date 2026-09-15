import { useSyncExternalStore } from 'react'

function subscribeToMediaQuery(query: string, onChange: () => void): () => void {
  const media = window.matchMedia(query)
  media.addEventListener('change', onChange)
  return () => {
    media.removeEventListener('change', onChange)
  }
}

export function useMediaQuery(query: string): boolean {
  return useSyncExternalStore(
    onChange => subscribeToMediaQuery(query, onChange),
    () => window.matchMedia(query).matches,
    () => false,
  )
}

export function useIsDesktop(): boolean {
  return useMediaQuery('(min-width: 768px)')
}

export function useIsWideDesktop(): boolean {
  return useMediaQuery('(min-width: 1024px)')
}
