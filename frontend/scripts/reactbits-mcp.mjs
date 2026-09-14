import { spawn } from 'node:child_process'
import process from 'node:process'
import { createInterface } from 'node:readline'

// A small stdio MCP client for reproducible React Bits registry inspection.
const server = spawn(process.execPath, ['node_modules/shadcn/dist/index.js', 'mcp'], {
  cwd: new URL('..', import.meta.url),
  stdio: ['pipe', 'pipe', 'inherit'],
})
const pending = new Map()
let nextId = 0
createInterface({ input: server.stdout }).on('line', (line) => {
  try {
    const message = JSON.parse(line)
    pending.get(message.id)?.(message)
    pending.delete(message.id)
  }
  catch { /* Ignore non-protocol startup output. */ }
})
function request(method, params) {
  const id = ++nextId
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`MCP timeout: ${method}`)), 30000)
    pending.set(id, (message) => {
      clearTimeout(timer)
      if (message.error)
        reject(new Error(JSON.stringify(message.error)))
      else resolve(message.result)
    })
    server.stdin.write(`${JSON.stringify({ jsonrpc: '2.0', id, method, params })}\n`)
  })
}
try {
  await request('initialize', { protocolVersion: '2024-11-05', capabilities: {}, clientInfo: { name: 'workspace-reactbits', version: '1.0.0' } })
  server.stdin.write(`${JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized' })}\n`)
  const result = process.argv[2]
    ? await request('tools/call', { name: process.argv[2], arguments: JSON.parse(process.argv[3] || '{}') })
    : await request('tools/list', {})
  process.stdout.write(`${JSON.stringify(result, null, 2)}\n`)
}
finally {
  server.stdin.end()
  server.kill()
}
