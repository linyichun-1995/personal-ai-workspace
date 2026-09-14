import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthMotionController, getAuthPresentation, motionTiming } from './auth-motion-controller'

describe('authMotionController', () => {
  let controller: AuthMotionController
  beforeEach(() => {
    vi.useFakeTimers()
    controller = new AuthMotionController()
    controller.start()
  })
  afterEach(() => {
    controller.dispose()
    vi.useRealTimers()
  })

  it('coordinates focus and restores the current mode on blur', () => {
    expect(controller.getSnapshot().state).toBe('idle')
    controller.focus('email')
    expect(getAuthPresentation(controller.getSnapshot())).toMatchObject({ activeNode: 0, drawConnections: true, protection: false })
    controller.focus('password')
    expect(getAuthPresentation(controller.getSnapshot())).toMatchObject({ protection: true, float: false, activeNode: -1 })
    controller.setMode('register')
    controller.focus('email')
    controller.blur()
    expect(controller.getSnapshot().state).toBe('register')
  })

  it('reconnects sequentially, locks focus and repeat submits, then leaves after the success hold', () => {
    const enter = vi.fn()
    const repeated = vi.fn()
    controller.submit(enter)
    controller.submit(repeated)
    controller.focus('email')
    expect(controller.getSnapshot()).toMatchObject({ state: 'submitting', step: 0 })
    for (let step = 1; step < 5; step++) {
      vi.advanceTimersByTime(motionTiming.step)
      expect(controller.getSnapshot().step).toBe(step)
    }
    vi.advanceTimersByTime(motionTiming.reconnect - 4 * motionTiming.step)
    expect(controller.getSnapshot().state).toBe('success')
    expect(enter).not.toHaveBeenCalled()
    vi.advanceTimersByTime(motionTiming.successHold)
    expect(controller.getSnapshot().exiting).toBe(true)
    vi.advanceTimersByTime(motionTiming.exit)
    expect(enter).toHaveBeenCalledOnce()
    expect(repeated).not.toHaveBeenCalled()
  })

  it('moves from submitting to error without losing the chance to retry', () => {
    expect(controller.beginSubmit()).toBe(true)
    expect(controller.beginSubmit()).toBe(false)
    controller.fail()
    expect(controller.getSnapshot().state).toBe('error')
    vi.advanceTimersByTime(motionTiming.error)
    expect(controller.getSnapshot().state).toBe('login')
  })

  it('holds an error through validation focus and cancels recovery when the mode changes', () => {
    controller.error()
    controller.focus('email')
    expect(controller.getSnapshot().state).toBe('error')
    controller.setMode('register')
    vi.advanceTimersByTime(2000)
    expect(controller.getSnapshot().state).toBe('register')
  })

  it('recovers from errors and still allows editing before recovery', () => {
    controller.error()
    vi.advanceTimersByTime(motionTiming.error)
    expect(controller.getSnapshot().state).toBe('login')
    controller.error()
    controller.edited()
    controller.focus('password')
    vi.advanceTimersByTime(motionTiming.error)
    expect(controller.getSnapshot().state).toBe('password-focus')
  })

  it('stops continuous motion and blinks for reduced motion and hidden tabs', () => {
    controller.setEnvironment(true, true)
    vi.advanceTimersByTime(10000)
    expect(controller.getSnapshot().blink).toBe(false)
    expect(getAuthPresentation(controller.getSnapshot())).toMatchObject({ animated: false, float: false, continuous: false })
    controller.setEnvironment(false, false)
    expect(getAuthPresentation(controller.getSnapshot()).animated).toBe(false)
  })

  it('cancels all pending work on disposal', () => {
    const enter = vi.fn()
    controller.submit(enter)
    controller.dispose()
    vi.advanceTimersByTime(10000)
    expect(enter).not.toHaveBeenCalled()
    expect(vi.getTimerCount()).toBe(0)
  })
})
