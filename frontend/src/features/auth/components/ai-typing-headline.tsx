import { AnimatePresence, motion } from 'motion/react'
import { useEffect, useState } from 'react'
import { useAuthMotion } from '@/features/auth/motion/auth-motion-context'

function TypedLine({ text, instant }: { text: string, instant: boolean }) {
  const [count, setCount] = useState(0)
  useEffect(() => {
    if (instant)
      return undefined
    let nextCount = 0
    const timer = setInterval(() => {
      nextCount++
      setCount(nextCount)
      if (nextCount >= text.length)
        clearInterval(timer)
    }, 55)
    return () => clearInterval(timer)
  }, [text, instant])
  return (
    <span aria-hidden="true">
      {text.slice(0, instant ? text.length : count)}
      <span className="ai-cursor" />
    </span>
  )
}

export function AITypingHeadline() {
  const { visual, snapshot } = useAuthMotion()
  return (
    <div className="workspace-copy">
      <h1 className="ai-headline" aria-label={`${visual.headline.first}${visual.headline.second}`}>
        {visual.headline.first && <span className="ai-headline-first" aria-hidden="true">{visual.headline.first}</span>}
        <span className="ai-headline-second">
          <AnimatePresence mode="wait" initial={false}>
            <motion.span className="ai-typed-line" key={snapshot.state} initial={{ opacity: 0, y: snapshot.reduced ? 0 : 4 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: snapshot.reduced ? 0 : -4 }} transition={{ duration: 0.2 }}>
              <TypedLine text={visual.headline.second} instant={snapshot.reduced || visual.success} />
            </motion.span>
          </AnimatePresence>
        </span>
      </h1>
      <p className="workspace-description">
        项目、任务、笔记、文件与 AI，
        <br />
        在这里重新连接。
      </p>
      <div className="workspace-status" role="status" aria-live="polite">
        <span>{visual.status}</span>
        <svg width="107" height="46" viewBox="0 0 107 46" aria-hidden="true">
          <path d="M5 41H16C46 41 38 5 68 5H102" fill="none" stroke="#7c98c3" strokeOpacity=".65" />
          <circle cx="5" cy="41" r="3.5" fill="#739dff" />
          <circle cx="102" cy="5" r="2" fill="#99b5e7" />
        </svg>
      </div>
    </div>
  )
}
