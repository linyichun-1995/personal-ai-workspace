import { Buffer } from 'node:buffer'
import { copyFile, mkdir, writeFile } from 'node:fs/promises'
import { createRequire } from 'node:module'
import path from 'node:path'
import { Resvg } from '@resvg/resvg-js'

// Editable layered artwork, reconstructed from the supplied reference. No remote assets.
const definitions = `<defs>
  <linearGradient id="shell" x1=".24" y1="0" x2=".76" y2="1" gradientUnits="objectBoundingBox"><stop stop-color="#ffffff"/><stop offset=".22" stop-color="#dbe6ff"/><stop offset=".52" stop-color="#9baedc"/><stop offset=".77" stop-color="#c1d1fa"/><stop offset="1" stop-color="#f3f7ff"/></linearGradient>
  <linearGradient id="body" x1=".18" y1="0" x2=".8" y2="1"><stop stop-color="#edf3ff"/><stop offset=".4" stop-color="#c2d3f6"/><stop offset=".75" stop-color="#8ca6e2"/><stop offset="1" stop-color="#b0daff"/></linearGradient>
  <linearGradient id="arm" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#f4f8ff"/><stop offset=".4" stop-color="#c5d7ff"/><stop offset=".8" stop-color="#8dabe6"/><stop offset="1" stop-color="#e0edff"/></linearGradient>
  <radialGradient id="visor" cx=".68" cy=".12" r=".95"><stop stop-color="#3b4a6f"/><stop offset=".35" stop-color="#202c49"/><stop offset=".72" stop-color="#111c33"/><stop offset="1" stop-color="#253555"/></radialGradient>
  <linearGradient id="rim"><stop stop-color="#6d97ee"/><stop offset=".5" stop-color="#c2e4ff"/><stop offset="1" stop-color="#769dff"/></linearGradient>
  <radialGradient id="core"><stop stop-color="#e2f7ff"/><stop offset=".55" stop-color="#74bdff"/><stop offset="1" stop-color="#6985ee" stop-opacity="0"/></radialGradient>
  <filter id="soft"><feGaussianBlur stdDeviation="3"/></filter>
  <filter id="light"><feGaussianBlur stdDeviation="1.5"/></filter>
</defs>`

const layers = {
  rightArm: `<path d="M244 258C259 241 289 223 299 226C311 232 286 278 263 290C249 298 241 277 244 258Z" fill="url(#arm)" stroke="#e7f2ff" stroke-width="1.4"/><path d="M252 259C267 242 286 234 296 232" fill="none" stroke="#fff" opacity=".7"/>`,
  body: `<path d="M134 245C154 230 212 229 237 244C254 257 255 291 242 316C230 340 211 350 190 348C163 345 146 323 137 298C130 279 128 257 134 245Z" fill="url(#body)" stroke="#d6e8ff" stroke-width="1.3"/><path d="M151 252C174 243 211 243 233 252" stroke="#edf6ff" stroke-width="2" opacity=".7" fill="none"/><path d="M163 331C181 345 211 348 232 331" stroke="#99d9ff" stroke-width="4" opacity=".65" fill="none"/>`,
  core: `<ellipse cx="211" cy="292" rx="25" ry="30" fill="url(#core)" opacity=".7"/><ellipse cx="211" cy="292" rx="15" ry="18" transform="rotate(20 211 292)" fill="#67aaff" stroke="#d3f6ff" stroke-width="4"/><ellipse cx="209" cy="290" rx="11" ry="13" fill="#7296ee" opacity=".6"/>`,
  leftArm: `<path d="M132 261C115 244 100 249 102 267C106 299 128 329 149 334C168 334 151 283 132 261Z" fill="url(#arm)" stroke="#d8e9ff" stroke-width="1.4"/><path d="M109 261C112 278 125 308 144 322" stroke="#f1f7ff" opacity=".5" fill="none"/>`,
  head: `
    <path d="M77 177C75 123 108 83 160 72C213 60 255 82 272 125C291 172 272 221 230 239C191 256 129 255 101 232C85 219 77 198 77 177Z" fill="url(#shell)" stroke="#d8e8ff" stroke-width="1.7"/>
    <path d="M91 128C108 94 135 79 166 75C204 68 231 77 248 92" fill="none" stroke="#fff" stroke-width="3" opacity=".8"/>
    <path d="M128 126C151 109 186 101 219 104C251 107 269 136 267 169C267 198 249 219 217 226C187 233 150 235 133 222C112 208 108 185 112 162C114 146 119 134 128 126Z" fill="#687eab" stroke="#f0f6ff" stroke-width="1.3"/>
    <path d="M131 130C153 113 186 106 219 109C248 112 263 137 262 168C262 194 246 214 216 221C190 228 153 230 137 218C118 205 114 184 118 163C120 149 123 138 131 130Z" fill="url(#visor)" stroke="#142440" stroke-width="1.5"/>
    <path d="M150 126C178 112 211 112 229 119" fill="none" stroke="#96acd2" stroke-width="2" opacity=".3"/>
    <ellipse cx="82" cy="184" rx="17" ry="37" transform="rotate(-10 82 184)" fill="#7498e3" stroke="#c2e6ff" stroke-width="2"/>
    <ellipse cx="80" cy="185" rx="11" ry="30" transform="rotate(-10 80 185)" fill="#98b6ed" stroke="#edf8ff" stroke-width="1.5"/>
    <path d="M99 231C127 252 181 256 227 241" fill="none" stroke="#adc9ff" stroke-width="2" opacity=".6"/>`,
  eyes: `<path d="M151 185C153 167 170 165 176 179M216 170C217 151 234 150 239 165" stroke="#7b9dff" stroke-width="12" fill="none" stroke-linecap="round" filter="url(#soft)" opacity=".8"/><path d="M151 185C153 167 170 165 176 179M216 170C217 151 234 150 239 165" stroke="#d6e5ff" stroke-width="6.5" fill="none" stroke-linecap="round"/>`,
}
const svg = body => `<svg xmlns="http://www.w3.org/2000/svg" width="360" height="400" viewBox="0 0 360 400">${definitions}${body}</svg>`
const assetDir = new URL('../src/assets/workspace-mascot/', import.meta.url)
const publicDir = new URL('../public/mascot/', import.meta.url)
await mkdir(assetDir, { recursive: true })
await mkdir(publicDir, { recursive: true })
await writeFile(new URL('robot.svg', assetDir), svg(Object.values(layers).join('')))

// Rive runtime format v7, serialized from the public rive-runtime schema.
// Sources: github.com/rive-app/rive-runtime/include/rive/generated and runtime_header.hpp.
const chunks = []
function uint(value) {
  const bytes = []
  do {
    const byte = value & 127
    value >>>= 7
    bytes.push(byte | (value ? 128 : 0))
  } while (value)
  return Buffer.from(bytes)
}
function number(value) {
  const buffer = Buffer.alloc(4)
  buffer.writeFloatLE(value)
  return buffer
}
function string(value) {
  const buffer = Buffer.from(value)
  return Buffer.concat([uint(buffer.length), buffer])
}
function object(type, props = []) {
  chunks.push(uint(type))
  props.forEach(([key, value]) => chunks.push(uint(key), value))
  chunks.push(uint(0))
}
const U = (key, value) => [key, uint(value)]
const F = (key, value) => [key, number(value)]
const S = (key, value) => [key, string(value)]
const bytes = (key, data) => [key, Buffer.concat([uint(data.length), data])]
const pathIds = (...values) => Buffer.concat(values.map(value => uint(value)))
chunks.push(Buffer.from('RIVE'), uint(7), uint(0), uint(0), uint(0))
object(23) // Backboard
const entries = Object.entries(layers)
for (const [index, [name, content]] of entries.entries()) {
  const png = new Resvg(svg(content)).render().asPng()
  await writeFile(new URL(`${name}.svg`, assetDir), svg(content))
  object(105, [S(203, name), U(204, index), F(207, 400), F(208, 360)])
  object(106, [[212, Buffer.concat([uint(png.length), png])]])
}
object(435, [S(557, 'AuthCompanion')])
object(431, [S(557, 'authState')])
object(431, [S(557, 'blink')])
object(437, [S(4, 'Default'), U(566, 0)])
object(442, [U(554, 0), F(575, 0)])
object(442, [U(554, 1), F(575, 0)])
object(1, [S(4, 'Workspace Companion'), F(7, 360), F(8, 400), U(236, 0), U(583, 0)]) // Artboard id 0
object(2, [S(4, 'robot'), U(5, 0)]) // root id 1
const ids = {}
let objectId = 2
for (const [name] of entries) {
  const pivot = name === 'head' ? [180, 230] : name === 'eyes' ? [195, 174] : name === 'core' ? [211, 292] : name === 'leftArm' ? [128, 261] : name === 'rightArm' ? [247, 266] : [180, 260]
  ids[name] = objectId++
  const parent = name === 'eyes' ? ids.head : 1
  const parentPivot = name === 'eyes' ? [180, 230] : [0, 0]
  object(2, [S(4, name), U(5, parent), F(13, pivot[0] - parentPivot[0]), F(14, pivot[1] - parentPivot[1])])
}
// Rive's drawable list paints in reverse component order. Keep face/core above shells.
for (const [index, [name]] of [...entries.entries()].reverse()) {
  const pivot = name === 'head' ? [180, 230] : name === 'eyes' ? [195, 174] : name === 'core' ? [211, 292] : name === 'leftArm' ? [128, 261] : name === 'rightArm' ? [247, 266] : [180, 260]
  object(100, [S(4, `${name}-image`), U(5, ids[name]), U(206, index), F(13, -pivot[0]), F(14, -pivot[1]), F(380, 0), F(381, 0)])
}
function keyed(id, property, frames) {
  object(25, [U(51, id)])
  object(26, [U(53, property)])
  frames.forEach(([frame, value]) => object(30, [U(67, frame), U(68, 1), F(70, value)]))
}
const names = ['idle', 'login', 'register', 'email', 'password', 'submitting', 'success', 'error', 'rest']
for (const [state, name] of names.entries()) {
  const duration = state < 4 ? 288 : state === 5 ? 84 : state === 7 ? 24 : 60
  object(31, [S(55, name), U(56, 60), U(57, duration), U(59, state < 4 ? 1 : 0)])
  const floating = Array.from({ length: 17 }, (_, i) => [i * 18, 0.5 - 2.5 * Math.cos(i * Math.PI / 8)])
  keyed(1, 14, state < 4 ? floating : state === 5 ? [[0, 0], [35, -5], [84, -5]] : state === 6 ? [[0, -5], [30, -8], [60, -8]] : [[0, 0]])
  keyed(ids.head, 15, state === 7 ? [[0, 0], [6, -0.035], [14, 0.035], [24, 0]] : state === 6 ? [[0, 0], [18, 0.025], [40, 0]] : [[0, state === 3 ? 0.07 : 0]])
  keyed(ids.leftArm, 15, [[0, state === 2 ? 0.06 : 0]])
  keyed(ids.rightArm, 15, [[0, state === 2 ? -0.06 : 0]])
  keyed(ids.core, 18, state < 4 ? [[0, 0.86], [144, 0.96], [288, 0.86]] : [[0, state === 5 || state === 6 ? 1 : 0.9]])
  for (const prop of [16, 17]) keyed(ids.core, prop, state === 6 ? [[0, 1], [20, 1.04], [60, 1]] : state === 7 ? [[0, 1], [10, 0.98], [24, 1]] : [[0, 1]])
}
// Independent eye layer so natural blinks never interrupt the current auth state.
for (const [name, scale] of [['eyes-open', 1], ['eyes-closed', 0.12]]) {
  object(31, [S(55, name), U(56, 60), U(57, 8)])
  keyed(ids.eyes, 17, [[0, scale]])
}
object(53, [S(55, 'AuthCompanion')])
function bindNumber(propertyId) {
  object(473)
  object(447, [U(586, 636), U(587, 0), bytes(588, pathIds(0, propertyId))])
  object(479)
}
function stateLayer(name, propertyId, animationIds) {
  object(57, [S(138, name)])
  object(63) // entry id 0
  object(65, [U(151, 3), U(158, 0)])
  object(64) // exit id 1
  object(62) // any id 2
  animationIds.forEach((_, state) => {
    object(65, [U(151, state + 3), U(158, name === 'eyes' ? 65 : 220)])
    object(482, [U(650, 0)])
    bindNumber(propertyId)
    object(484, [F(652, state)])
  })
  animationIds.forEach(id => object(61, [U(149, id)]))
}
stateLayer('authentication', 0, names.map((_, i) => i))
stateLayer('eyes', 1, [9, 10])
await writeFile(new URL('workspace-companion.riv', publicDir), Buffer.concat(chunks))
const requireRive = createRequire(import.meta.resolve('@rive-app/react-canvas'))
const runtimeDir = path.dirname(requireRive.resolve('@rive-app/canvas'))
await copyFile(path.join(runtimeDir, 'rive.wasm'), new URL('rive.wasm', publicDir))
await copyFile(path.join(runtimeDir, 'rive_fallback.wasm'), new URL('rive_fallback.wasm', publicDir))
console.log(`Built workspace-companion.riv (${Buffer.concat(chunks).length} bytes), view model AuthCompanion, 9 auth states, 2 eye states, local WASM.`)
