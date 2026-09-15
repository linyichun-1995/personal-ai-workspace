import { AITypingHeadline } from './ai-typing-headline'
import { WorkspaceCore } from './workspace-core'

export function AuthScene() {
  return (
    <section className="auth-scene" aria-label="你的 Personal Workspace">
      <AITypingHeadline />
      <WorkspaceCore />
    </section>
  )
}
