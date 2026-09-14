import { useId } from 'react'

import { cn } from '@/shared/lib/utils'

export function AppLogo({ className }: { className?: string }) {
  const gradientId = useId()
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 48 48"
      fill="none"
      className={cn('size-8 shrink-0', className)}
    >
      <defs>
        <linearGradient id={gradientId} x1="15" y1="5" x2="29" y2="44" gradientUnits="userSpaceOnUse">
          <stop stopColor="#B9C8F7" />
          <stop offset="0.5" stopColor="#9790EF" />
          <stop offset="1" stopColor="#805AF3" />
        </linearGradient>
      </defs>
      <path d="M19.3 8.2c2-4.2 7.4-4.2 9.4 0l15.1 31.3c1.7 3.5-.9 7-4.6 7-2 0-3.8-1.2-4.7-3L19.3 12.3a4.8 4.8 0 0 1 0-4.1Z" fill={`url(#${gradientId})`} />
      <path d="M19.3 8.2c1.2-2.6 4.6-3.6 7.1-2.2a5.1 5.1 0 0 1 2.3 6.9L13.5 43.5a5.3 5.3 0 0 1-4.7 3c-3.7 0-6.3-3.5-4.6-7L19.3 8.2Z" fill={`url(#${gradientId})`} opacity="0.88" />
      <path d="m15.4 31 5-10.3L29.5 40H24a9.5 9.5 0 0 1-8.6-9Z" fill="#C2BCFF" opacity="0.72" />
    </svg>
  )
}
