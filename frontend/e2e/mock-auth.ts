import type { Page } from '@playwright/test'

const session = {
  user: {
    id: '11111111-1111-1111-1111-111111111111',
    email: 'preview@example.com',
    name: '体验用户',
    avatarUrl: null,
  },
  workspace: {
    id: '22222222-2222-2222-2222-222222222222',
    name: '我的工作空间',
  },
}

export async function mockAuthApi(page: Page): Promise<void> {
  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url())
    const path = url.pathname
    const method = route.request().method()

    if (method === 'POST' && path.endsWith('/auth/refresh')) {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'AUTH_REFRESH_TOKEN_INVALID', message: '刷新令牌无效', details: [] }),
      })
      return
    }

    if (method === 'POST' && (path.endsWith('/auth/login') || path.endsWith('/auth/register'))) {
      await route.fulfill({
        status: path.endsWith('/register') ? 201 : 200,
        contentType: 'application/json',
        body: JSON.stringify({
          ...session,
          accessToken: 'test-access-token',
          tokenType: 'Bearer',
          expiresIn: 900,
        }),
      })
      return
    }

    if (method === 'GET' && path.endsWith('/me')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          ...session.user,
          locale: 'zh-CN',
          timezone: 'UTC',
          version: 0,
          currentWorkspace: session.workspace,
        }),
      })
      return
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: '{}',
    })
  })
}
