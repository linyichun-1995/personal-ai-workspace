export interface AiStreamChunk {
  conversationId: string
  delta: string
  done: boolean
}

export interface AiConversation {
  id: string
  title: string
}
