import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { isApiError } from '@/shared/api/errors'

export function applyAuthFormError<TFieldValues extends FieldValues>(
  setError: UseFormSetError<TFieldValues>,
  error: unknown,
): void {
  if (!isApiError(error)) {
    setError('root', { type: 'server', message: '连接没有完成，请重新尝试' })
    return
  }

  if (error.code === 'AUTH_EMAIL_ALREADY_EXISTS') {
    setError('email' as Path<TFieldValues>, { type: 'server', message: error.message })
    return
  }

  if (error.code === 'VALIDATION_ERROR' && Array.isArray(error.details)) {
    let mapped = false
    for (const detail of error.details) {
      if (!detail || typeof detail !== 'object') {
        continue
      }
      const field = 'field' in detail ? detail.field : undefined
      const message = 'message' in detail ? detail.message : undefined
      if (typeof field === 'string' && typeof message === 'string') {
        setError(field as Path<TFieldValues>, { type: 'server', message })
        mapped = true
      }
    }
    if (mapped) {
      return
    }
  }

  setError('root', { type: 'server', message: error.message })
}

export function resolvePostAuthPath(redirect?: string): string {
  if (redirect && redirect.startsWith('/app')) {
    return redirect
  }
  return '/app/dashboard'
}
