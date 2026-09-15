import type { CSSProperties } from 'react'
import { FileText, Folder, ListChecks, NotebookPen, Sparkles } from 'lucide-react'
import { motion } from 'motion/react'
import { lazy, Suspense } from 'react'
import robotFallback from '@/assets/workspace-mascot/robot.svg'
import { useAuthMotion } from '@/features/auth/motion/auth-motion-context'

const WorkspaceMascot = lazy(() => import('./workspace-mascot').then(module => ({ default: module.WorkspaceMascot })))

const cards = [
  { id: 'projects', label: '项目', Icon: Folder, x: 200, y: 163, rotate: -8, dx: -1, dy: -1, period: 4.5 },
  { id: 'tasks', label: '任务', Icon: ListChecks, x: 543, y: 157, rotate: 8, dx: 1, dy: -1, period: 5.2 },
  { id: 'notes', label: '笔记', Icon: NotebookPen, x: 642, y: 360, rotate: 7, dx: 1, dy: 0, period: 6.1 },
  { id: 'files', label: '文件', Icon: FileText, x: 125, y: 549, rotate: 3, dx: -1, dy: 1, period: 5.8 },
  { id: 'ai', label: 'AI', Icon: Sparkles, x: 569, y: 593, rotate: 5, dx: 1, dy: 1, period: 6.6 },
]
const paths = ['M200 180C217 210 255 240 330 320', 'M543 172C506 225 453 250 399 330', 'M642 378C537 367 472 361 410 369', 'M125 550C200 487 255 453 325 407', 'M569 593C468 510 445 466 393 412']
const nodes = [{ x: 318, y: 153 }, { x: 88, y: 395 }, { x: 557, y: 469 }, { x: 440, y: 237 }, { x: 288, y: 555 }, { x: 613, y: 275 }, { x: 187, y: 287 }, { x: 449, y: 557 }]

export function WorkspaceCore() {
  const { snapshot, visual } = useAuthMotion()
  return (
    <div className="workspace-core" data-state={snapshot.state} data-continuous={visual.continuous} aria-label="AI 助手与项目、任务、笔记、文件和 AI 工作流连接" role="img">
      <svg className="workspace-orbits" viewBox="0 0 800 800" fill="none" aria-hidden="true">
        <defs>
          <radialGradient id="node-fill">
            <stop stopColor="#e6f5ff" />
            <stop offset=".55" stopColor="#a1c7ff" />
            <stop offset="1" stopColor="#557ed3" />
          </radialGradient>
          <linearGradient id="orbit-stroke" x1="100" y1="200" x2="650" y2="570" gradientUnits="userSpaceOnUse">
            <stop stopColor="#82acff" stopOpacity=".65" />
            <stop offset=".5" stopColor="#759bee" stopOpacity=".22" />
            <stop offset="1" stopColor="#a0caff" stopOpacity=".85" />
          </linearGradient>
          <radialGradient id="platform-light">
            <stop stopColor="#a4d3ff" stopOpacity=".9" />
            <stop offset=".4" stopColor="#4e85e7" stopOpacity=".5" />
            <stop offset="1" stopColor="#578ae2" stopOpacity="0" />
          </radialGradient>
        </defs>
        <g className="orbit-rings" opacity={visual.orbitOpacity}>
          <ellipse className="orbit-main" cx="354" cy="365" rx="265" ry="210" transform="rotate(-15 354 365)" stroke="url(#orbit-stroke)" strokeWidth="1.3" />
          <ellipse className="orbit-secondary" cx="356" cy="375" rx="345" ry="126" transform="rotate(-24 356 375)" stroke="url(#orbit-stroke)" strokeWidth="1.1" />
          <ellipse className="orbit-tertiary" cx="353" cy="373" rx="242" ry="268" transform="rotate(30 353 373)" stroke="#7ba0eb" strokeOpacity=".18" />
          <ellipse className="orbit-flow orbit-main" cx="354" cy="365" rx="265" ry="210" transform="rotate(-15 354 365)" stroke="#b2d5ff" strokeWidth="1.5" strokeDasharray="65 1435" pathLength="1500" style={{ animationDuration: `${visual.orbitDuration}s` }} />
          <ellipse className="orbit-flow orbit-secondary" cx="356" cy="375" rx="345" ry="126" transform="rotate(-24 356 375)" stroke="#a5ccff" strokeWidth="1.2" strokeDasharray="40 1460" pathLength="1500" style={{ animationDuration: '52s', animationDirection: 'reverse' }} />
        </g>
        <g className="workspace-connections">
          {paths.map((d, index) => <motion.path key={`${d}-${snapshot.state === 'submitting' ? 'reconnect' : snapshot.state === 'email-focus' ? 'identify' : 'rest'}`} className={`connection connection-${index}`} d={d} stroke="#95bdff" strokeWidth="1.4" initial={{ pathLength: visual.drawConnections ? 0 : 1 }} animate={{ pathLength: index < visual.activeLinks ? 1 : 0, opacity: visual.error && index === 0 ? [1, 0.4, 1] : visual.drawConnections ? 0.6 : 0.07 }} transition={{ duration: snapshot.reduced ? 0.15 : 0.42, opacity: { duration: visual.error ? 0.38 : 0.3 } }} />)}
        </g>
        <motion.ellipse cx="354" cy="360" rx="152" ry="174" stroke="#91baff" strokeWidth="1.1" initial={false} animate={{ opacity: visual.protection ? 0.3 : 0 }} transition={{ duration: 0.35 }} />
        {nodes.map((node, index) => (
          <g className={`workspace-node workspace-node-${index}`} key={`${node.x}-${node.y}`} style={{ '--node-delay': `${-index * 0.8}s` } as CSSProperties}>
            <circle cx={node.x} cy={node.y} r={index < 3 ? 15 : 9} fill="url(#platform-light)" opacity=".48" />
            <circle cx={node.x} cy={node.y} r={index < 3 ? 9 : 4} fill="url(#node-fill)" />
          </g>
        ))}
        {visual.activeNode >= 0 && visual.animated && <motion.circle key={`${snapshot.state}-${visual.activeNode}`} r="5" fill="#c3e4ff" style={{ offsetPath: `path('${paths[visual.activeNode] ?? paths[0]}')` }} initial={{ offsetDistance: '0%', opacity: 0 }} animate={{ offsetDistance: '100%', opacity: [0, 1, 1, 0] }} transition={{ duration: visual.submitting ? 0.24 : 0.95, ease: 'easeInOut' }} />}
        <g className="workspace-platform">
          <ellipse cx="345" cy="669" rx="300" ry="55" fill="url(#platform-light)" opacity=".17" />
          <ellipse cx="345" cy="664" rx="252" ry="45" stroke="#77a7ef" strokeOpacity=".13" />
          <ellipse cx="348" cy="651" rx="171" ry="25" fill="#2a416a" fillOpacity=".2" stroke="#77aaff" strokeOpacity=".23" />
          <ellipse cx="348" cy="640" rx="94" ry="17" fill="url(#platform-light)" opacity=".8" />
          <motion.ellipse cx="348" cy="602" rx="67" ry="7" fill="url(#platform-light)" initial={false} animate={{ opacity: visual.success ? [0.5, 1, 0.65] : 0.55, scale: visual.success && visual.animated ? [1, 1.04, 1] : 1 }} transition={{ duration: 0.65 }} />
        </g>
      </svg>
      {cards.map((card, index) => (
        <motion.div key={card.id} className={`workspace-card-position workspace-card-${card.id}`} style={{ left: `${card.x / 8}%`, top: `${card.y / 8}%` }} animate={{ x: card.dx * visual.cardSpread, y: card.dy * visual.cardSpread }} transition={{ duration: 0.55, ease: 'easeInOut', delay: visual.submitting ? index * 0.035 : 0 }}>
          <motion.div className="workspace-floating-card" animate={{ y: visual.float ? [-2, 2, -2] : 0 }} transition={visual.float ? { duration: card.period, repeat: Infinity, ease: 'easeInOut', delay: index * 0.25 } : { duration: 0.3 }}>
            <motion.div className="workspace-card-content" style={{ rotate: card.rotate }} whileHover={snapshot.reduced ? {} : { y: -3 }} data-active={visual.drawConnections && index < visual.activeLinks}>
              <span className={`workspace-card-icon icon-${card.id}`}><card.Icon strokeWidth={1.5} fill={card.id === 'projects' ? 'currentColor' : 'none'} /></span>
              <div>
                <span className="workspace-card-label">{card.label}</span>
                <i />
                <i />
              </div>
            </motion.div>
          </motion.div>
        </motion.div>
      ))}
      <Suspense fallback={<div className="workspace-mascot" aria-hidden="true"><img className="mascot-fallback" src={robotFallback} alt="" /></div>}>
        <WorkspaceMascot />
      </Suspense>
    </div>
  )
}
