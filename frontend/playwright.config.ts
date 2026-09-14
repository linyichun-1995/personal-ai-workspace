import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  fullyParallel: false,
  workers: 1,
  use: { baseURL: 'http://127.0.0.1:5176', channel: 'msedge', viewport: { width: 1672, height: 941 }, screenshot: 'only-on-failure' },
  webServer: { command: 'pnpm dev --host 127.0.0.1 --port 5176', url: 'http://127.0.0.1:5176/login', reuseExistingServer: true },
})
