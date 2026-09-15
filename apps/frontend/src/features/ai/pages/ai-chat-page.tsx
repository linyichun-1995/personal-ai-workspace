import { getRouteApi } from '@tanstack/react-router'

import { FeaturePlaceholder } from '@/shared/components/feature-placeholder'

const routeApi = getRouteApi('/app/ai/chat/$conversationId')

export function AiChatPage() {
  const { conversationId } = routeApi.useParams()

  return (
    <FeaturePlaceholder
      title="AI 会话"
      description={`conversationId=${conversationId}。后续使用 subscribeChatStream 消费 SSE。`}
      route="/app/ai/chat/$conversationId"
    />
  )
}
