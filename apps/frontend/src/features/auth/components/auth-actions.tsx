import type { ReactNode } from 'react'
import { ArrowRight, Check, ShieldCheck } from 'lucide-react'
import { motion } from 'motion/react'
import { useAuthMotion } from '@/features/auth/motion/auth-motion-context'

export function AuthSubmit({ children }: { children: ReactNode }) {
  const { controller, visual } = useAuthMotion()
  return (
    <button className="auth-submit" type="submit" disabled={controller.busy} aria-busy={visual.submitting}>
      <motion.span className="auth-submit-progress" aria-hidden="true" initial={false} animate={{ scaleX: visual.progress }} style={{ originX: 0 }} transition={{ duration: 0.26 }} />
      <span>{visual.success ? '工作空间已准备完成' : visual.submitting ? '正在连接工作空间' : children}</span>
      {visual.success ? <Check size={22} /> : <ArrowRight size={22} />}
    </button>
  )
}

export function AuthFootnote() {
  return (
    <p className="auth-footnote">
      <ShieldCheck size={29} strokeWidth={1.4} />
      <span>你的数据保持私密与安全。</span>
    </p>
  )
}
