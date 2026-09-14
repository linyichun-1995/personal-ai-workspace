import type { AiStreamChunk } from '@/features/ai/types'
import type { StreamEvent } from '@/shared/api'
import { consumeSseStream } from '@/shared/api'

export interface SubscribeChatStreamOptions {
  conversationId: string
  signal?: AbortSignal
  onChunk: (chunk: AiStreamChunk) => void
}

export function subscribeChatStream({
  conversationId,
  signal,
  onChunk,
}: SubscribeChatStreamOptions): Promise<void> {
  return consumeSseStream<AiStreamChunk>(`/ai/conversations/${conversationId}/stream`, {
    signal,
    onEvent: (event: StreamEvent<AiStreamChunk>) => {
      onChunk(event.data)
    },
  })
}
