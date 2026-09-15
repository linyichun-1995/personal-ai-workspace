import { FeaturePlaceholder } from '@/shared/components/feature-placeholder'

export function AiPage({ view = 'chat' }: { view?: 'chat' | 'knowledge' | 'agents' }) {
  const titles = { chat: 'AI 对话', knowledge: '知识库', agents: '智能体' }
  return (
    <FeaturePlaceholder
      title={titles[view]}
      description="连接你的知识与工作，让 AI 帮你推进下一步。"
      route={view === 'chat' ? '/app/ai' : `/app/ai/${view}`}
    />
  )
}
