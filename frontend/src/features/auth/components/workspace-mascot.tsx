import { Alignment, Fit, Layout, RuntimeLoader, useRive, useStateMachineInput } from '@rive-app/react-canvas'
import { useEffect, useState } from 'react'
import robotFallback from '@/assets/workspace-mascot/robot.svg'
import { useAuthMotion } from '@/features/auth/motion/auth-motion-context'

RuntimeLoader.setWasmUrl('/mascot/rive.wasm')
RuntimeLoader.setWasmFallbackUrl('/mascot/rive_fallback.wasm')

export function WorkspaceMascot() {
  const { snapshot, visual } = useAuthMotion()
  const [loaded, setLoaded] = useState(false)
  const [failed, setFailed] = useState(false)
  const [machineState, setMachineState] = useState('loading')
  const { rive, setCanvasRef, setContainerRef } = useRive({
    src: '/mascot/workspace-companion.riv',
    stateMachine: 'AuthCompanion',
    autoplay: true,
    layout: new Layout({ fit: Fit.Contain, alignment: Alignment.Center }),
    onLoad: () => setLoaded(true),
    onLoadError: () => setFailed(true),
    onStateChange: event => setMachineState(Array.isArray(event.data) ? event.data.join(',') : String(event.data)),
  })
  const stateInput = useStateMachineInput(rive, 'AuthCompanion', 'authState')
  const blinkInput = useStateMachineInput(rive, 'AuthCompanion', 'blink')
  useEffect(() => {
    if (stateInput)
      stateInput.value = snapshot.reduced ? 8 : visual.riveState
  }, [stateInput, visual.riveState, snapshot.reduced])
  useEffect(() => {
    if (blinkInput)
      blinkInput.value = visual.blink ? 1 : 0
  }, [blinkInput, visual.blink])
  useEffect(() => {
    if (!rive)
      return
    if (snapshot.visible)
      rive.play('AuthCompanion')
    else rive.pause('AuthCompanion')
  }, [rive, snapshot.visible])
  return (
    <div className="workspace-mascot" aria-hidden="true" data-renderer={loaded && !failed ? 'rive' : 'svg'} data-machine-state={machineState}>
      <img src={robotFallback} className="mascot-fallback" alt="" style={{ opacity: loaded && !failed ? 0 : 1 }} />
      <div ref={setContainerRef} className="mascot-rive" style={{ opacity: loaded && !failed ? 1 : 0 }}><canvas ref={setCanvasRef} /></div>
    </div>
  )
}
