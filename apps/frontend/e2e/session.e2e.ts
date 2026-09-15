import { expect, test } from '@playwright/test'
import type { APIRequestContext } from '@playwright/test'

const password = 'Password123'

async function backendHasProfileApi(request: APIRequestContext): Promise<boolean> {
  const email = `probe-${Date.now()}@example.com`
  const registered = await request.post('http://127.0.0.1:8080/api/v1/auth/register', {
    data: { email, password, name: 'Probe', timezone: 'UTC' },
    headers: { 'Content-Type': 'application/json' },
  })
  if (registered.status() !== 201) {
    return false
  }
  const body = await registered.json() as { accessToken: string }
  const probe = await request.patch('http://127.0.0.1:8080/api/v1/me', {
    data: { displayName: 'Probe User', locale: 'zh-CN', timezone: 'UTC', version: 0 },
    headers: {
      Authorization: `Bearer ${body.accessToken}`,
      'Content-Type': 'application/json',
    },
  })
  return probe.ok()
}

test.describe('real session', () => {
  test.beforeEach(async ({ request }) => {
    const health = await request.get('http://127.0.0.1:8080/actuator/health')
    test.skip(health.status() !== 200, 'Backend is not running')
  })

  test('register restores after reload and logout returns to login', async ({ page }) => {
    const email = `session-${Date.now()}@example.com`

    await page.goto('/register')
    await page.getByLabel('昵称', { exact: true }).fill('会话用户')
    await page.getByLabel('邮箱', { exact: true }).fill(email)
    await page.getByLabel('密码', { exact: true }).fill(password)
    await page.getByLabel('确认密码', { exact: true }).fill(password)
    await page.getByRole('button', { name: '创建并进入工作空间' }).click()
    await expect(page).toHaveURL(/\/app\/dashboard$/, { timeout: 15_000 })

    await page.reload()
    await expect(page).toHaveURL(/\/app\/dashboard$/)
    await expect(page.getByRole('button', { name: '账户菜单' })).toHaveText('会')

    await page.getByRole('button', { name: '账户菜单' }).click()
    await page.getByRole('button', { name: '退出登录' }).click()
    await expect(page).toHaveURL(/\/login$/)

    await page.goto('/app/dashboard')
    await expect(page).toHaveURL(/\/login/)
  })

  test('update profile and isolate workspaces', async ({ page, request }) => {
    test.skip(!(await backendHasProfileApi(request)), 'Restart the Spring Boot process to load PATCH /me')

    const stamp = Date.now()
    const email = `session-profile-${stamp}@example.com`
    const otherEmail = `session-other-${stamp}@example.com`

    await page.goto('/register')
    await page.getByLabel('昵称', { exact: true }).fill('会话用户')
    await page.getByLabel('邮箱', { exact: true }).fill(email)
    await page.getByLabel('密码', { exact: true }).fill(password)
    await page.getByLabel('确认密码', { exact: true }).fill(password)
    await page.getByRole('button', { name: '创建并进入工作空间' }).click()
    await expect(page).toHaveURL(/\/app\/dashboard$/, { timeout: 15_000 })

    await page.goto('/app/settings/profile')
    await expect(page.getByLabel('显示名称')).toHaveValue('会话用户')
    await page.getByLabel('显示名称').fill('已更新用户')
    await page.getByRole('button', { name: '保存资料' }).click()
    await expect(page.getByText('个人资料已保存')).toBeVisible()
    await expect(page.getByRole('button', { name: '账户菜单' })).toHaveText('已')

    const ownerLogin = await request.post('http://127.0.0.1:8080/api/v1/auth/login', {
      data: { email, password },
      headers: { 'Content-Type': 'application/json' },
    })
    expect(ownerLogin.ok()).toBeTruthy()
    const ownerBody = await ownerLogin.json() as { accessToken: string, workspace: { id: string } }

    const other = await request.post('http://127.0.0.1:8080/api/v1/auth/register', {
      data: { email: otherEmail, password, name: '另一用户', timezone: 'UTC' },
      headers: { 'Content-Type': 'application/json' },
    })
    expect(other.status()).toBe(201)
    const otherBody = await other.json() as { accessToken: string }

    const forbidden = await request.get(`http://127.0.0.1:8080/api/v1/workspaces/${ownerBody.workspace.id}`, {
      headers: { Authorization: `Bearer ${otherBody.accessToken}` },
    })
    expect(forbidden.status()).toBe(404)
  })
})
