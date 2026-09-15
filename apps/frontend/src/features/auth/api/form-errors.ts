export { applyApiFormError as applyAuthFormError } from '@/shared/api/form-errors'

export function resolvePostAuthPath(redirect?: string): string {
  if (redirect && redirect.startsWith('/app')) {
    return redirect
  }
  return '/app/dashboard'
}

