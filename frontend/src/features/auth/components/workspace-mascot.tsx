import type { Rive } from '@rive-app/react-canvas'
import { Alignment, Fit, Layout, RuntimeLoader, useRive } from '@rive-app/react-canvas'
import { useEffect, useState } from 'react'
import robotFallback from '@/assets/workspace-mascot/robot.svg'
import { useAuthMotion } from '@/features/auth/motion/auth-motion-context'

RuntimeLoader.setWasmUrl('/mascot/rive.wasm')
RuntimeLoader.setWasmFallbackUrl('/mascot/rive_fallback.wasm')

function setViewModelNumber(rive: Rive, name: string, value: number) {
  const property = rive.viewModelInstance?.number(name)
  if (property)
    property.value = value
}

export function WorkspaceMascot() {
  const { snapshot, visual } = useAuthMotion()
  const [loaded, setLoaded] = useState(false)
  const [failed, setFailed] = useState(false)
  const [machineState, setMachineState] = useState('loading')
  const { rive, setCanvasRef, setContainerRef } = useRive({
    src: '/mascot/workspace-companion.riv',
    stateMachine: 'AuthCompanion',
    autoplay: true,
    autoBind: true,
    layout: new Layout({ fit: Fit.Contain, alignment: Alignment.Center }),
    onLoad: () => setLoaded(true),
    onLoadError: () => setFailed(true),
    onStateChange: event => setMachineState(Array.isArray(event.data) ? event.data.join(',') : String(event.data)),
  })
  useEffect(() => {
    if (!rive)
      return
    setViewModelNumber(rive, 'authState', snapshot.reduced ? 8 : visual.riveState)
    setViewModelNumber(rive, 'blink', visual.blink ? 1 : 0)
  }, [rive, snapshot.reduced, visual.riveState, visual.blink])
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
