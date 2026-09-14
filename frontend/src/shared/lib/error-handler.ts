import { toast } from 'sonner'

import { isApiError, toErrorMessage } from '@/shared/api/errors'

export function handleClientError(error: unknown): void {
  if (isApiError(error) && error.isUnauthorized) {
    return
  }

  toast.error(toErrorMessage(error))
}
