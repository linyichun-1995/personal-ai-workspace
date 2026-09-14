export type AuthMotionState = 'idle' | 'login' | 'register' | 'email-focus' | 'password-focus' | 'submitting' | 'success' | 'error'
export type AuthMode = 'login' | 'register'

export interface AuthMotionSnapshot {
  state: AuthMotionState
  mode: AuthMode
  step: number
  exiting: boolean
  blink: boolean
  reduced: boolean
  visible: boolean
}

export const reconnectSteps = ['正在连接项目…', '正在恢复任务…', '正在载入笔记…', '正在整理文件…', '正在同步 AI 上下文…'] as const
export const motionTiming = { reconnect: 1400, step: 260, successHold: 650, exit: 300, error: 1700 } as const

/** The only owner of authentication animation state. No credentials or network calls. */
export class AuthMotionController {
  private snapshot: AuthMotionSnapshot
  private listeners = new Set<() => void>()
  private timers = new Set<ReturnType<typeof setTimeout>>()
  private blinkTimer: ReturnType<typeof setTimeout> | undefined
  private blinkEnd: ReturnType<typeof setTimeout> | undefined
  private errorTimer: ReturnType<typeof setTimeout> | undefined
  private running = false
  private focusedField: 'email' | 'password' | null = null

  constructor(mode: AuthMode = 'login') {
    this.snapshot = { state: mode === 'register' ? 'register' : 'idle', mode, step: -1, exiting: false, blink: false, reduced: false, visible: true }
  }

  getSnapshot = () => this.snapshot
  subscribe = (listener: () => void) => {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  private update(patch: Partial<AuthMotionSnapshot>) {
    this.snapshot = { ...this.snapshot, ...patch }
    this.listeners.forEach(listener => listener())
  }

  private schedule(callback: () => void, delay: number) {
    const timer = setTimeout(() => {
      this.timers.delete(timer)
      callback()
    }, delay)
    this.timers.add(timer)
    return timer
  }

  private clearSequence() {
    this.timers.forEach(clearTimeout)
    this.timers.clear()
    this.errorTimer = undefined
  }

  start() {
    this.running = true
    this.scheduleBlink()
  }

  dispose() {
    this.running = false
    this.clearSequence()
    clearTimeout(this.blinkTimer)
    clearTimeout(this.blinkEnd)
  }

  private scheduleBlink() {
    clearTimeout(this.blinkTimer)
    clearTimeout(this.blinkEnd)
    if (!this.running || this.snapshot.reduced || !this.snapshot.visible)
      return
    this.blinkTimer = setTimeout(() => {
      this.update({ blink: true })
      this.blinkEnd = setTimeout(() => {
        this.update({ blink: false })
        this.scheduleBlink()
      }, 140)
    }, 4000 + Math.random() * 4000)
  }

  setEnvironment(reduced: boolean, visible: boolean) {
    if (reduced === this.snapshot.reduced && visible === this.snapshot.visible)
      return
    this.update({ reduced, visible, blink: false })
    this.scheduleBlink()
  }

  setMode(mode: AuthMode) {
    if (mode === this.snapshot.mode)
      return
    this.clearSequence()
    this.update({ state: mode, mode, step: -1, exiting: false })
  }

  focus(field: 'email' | 'password') {
    this.focusedField = field
    if (this.busy || this.snapshot.state === 'error')
      return
    this.update({ state: field === 'email' ? 'email-focus' : 'password-focus' })
  }

  blur() {
    this.focusedField = null
    if (this.busy || this.snapshot.state === 'error')
      return
    this.update({ state: this.snapshot.mode })
  }

  get busy() { return this.snapshot.state === 'submitting' || this.snapshot.state === 'success' }

  beginSubmit() {
    if (this.busy)
      return false
    this.clearSequence()
    this.update({ state: 'submitting', step: 0, exiting: false })
    for (let step = 1; step < reconnectSteps.length; step++) {
      this.schedule(() => this.update({ step }), step * motionTiming.step)
    }
    return true
  }

  succeed(onComplete: () => void) {
    this.clearSequence()
    this.update({ state: 'success', step: 5, exiting: false })
    this.schedule(() => this.update({ exiting: true }), motionTiming.successHold)
    this.schedule(onComplete, motionTiming.successHold + motionTiming.exit)
  }

  fail() {
    this.clearSequence()
    this.update({ state: 'error', step: -1, exiting: false })
    this.errorTimer = this.schedule(() => this.update({ state: this.snapshot.mode }), motionTiming.error)
  }

  error() {
    if (this.busy)
      return
    this.fail()
  }

  edited() {
    if (this.snapshot.state !== 'error')
      return
    if (this.errorTimer) {
      clearTimeout(this.errorTimer)
      this.timers.delete(this.errorTimer)
    }
    this.update({ state: this.focusedField === 'email' ? 'email-focus' : this.focusedField === 'password' ? 'password-focus' : this.snapshot.mode })
  }

  /** A deterministic local preview, not authentication or simulated server data. */
  submit(onComplete: () => void) {
    if (!this.beginSubmit())
      return
    this.schedule(() => this.succeed(onComplete), motionTiming.reconnect)
  }
}

const headlines: Record<AuthMotionState, { first: string, second: string }> = {
  'idle': { first: '你的工作空间，', second: '正在等待连接' },
  'login': { first: '欢迎回来，', second: '正在等待你进入工作空间' },
  'register': { first: '新的工作空间，', second: '正在开始建立' },
  'email-focus': { first: '', second: '正在识别你的工作空间' },
  'password-focus': { first: '', second: '正在进行安全验证' },
  'submitting': { first: '', second: '正在重新连接你的工作空间' },
  'success': { first: '', second: '工作空间已准备完成' },
  'error': { first: '', second: '连接没有完成，请重新尝试' },
}

/** All visual components consume this presentation, not independent business logic. */
export function getAuthPresentation(snapshot: AuthMotionSnapshot) {
  const { state, mode, reduced, visible, step } = snapshot
  const success = state === 'success'
  const submitting = state === 'submitting'
  const secure = state === 'password-focus'
  const email = state === 'email-focus'
  const error = state === 'error'
  const animated = !reduced && visible
  return {
    headline: headlines[state],
    status: submitting ? reconnectSteps[Math.min(step, 4)] : success ? '工作空间已就绪' : error ? '请检查输入信息' : email ? '正在识别' : secure ? '安全验证中' : mode === 'register' ? '正在建立' : '准备就绪',
    progress: submitting ? (step + 1) / 5 : success ? 1 : 0,
    continuous: animated && !submitting && !success && !error,
    float: animated && !secure && !submitting && !success && !error,
    headRotation: !animated ? 0 : error ? [0, -2, 2, 0] : email ? 4 : success ? [0, 2, 0] : 0,
    robotY: !animated ? 0 : success ? -8 : submitting ? -5 : 0,
    coreScale: !animated ? 1 : success ? [1, 1.04, 1] : error ? [1, 0.98, 1] : submitting ? [1, 1.035, 1] : 1,
    coreIntensity: submitting || success ? 1 : secure ? 0.94 : 0.86,
    cardSpread: !animated ? 0 : submitting ? -14 : mode === 'register' ? 12 : -2,
    armOpen: animated && mode === 'register' ? 4 : 0,
    orbitDuration: secure ? 56 : email ? 30 : 38,
    orbitOpacity: email || submitting || success ? 0.72 : secure ? 0.35 : 0.48,
    protection: secure,
    activeLinks: success ? 5 : submitting ? step + 1 : email ? 1 : mode === 'register' ? 2 : 5,
    drawConnections: email || submitting || success,
    activeNode: submitting ? step : email ? 0 : -1,
    error,
    success,
    submitting,
    animated,
    blink: snapshot.blink,
    riveState: ['idle', 'login', 'register', 'email-focus', 'password-focus', 'submitting', 'success', 'error'].indexOf(state),
  }
}
