import { expect, test } from '@playwright/test'

const password = 'Password123'

test.describe('workspace business flow', () => {
  test.beforeEach(async ({ request }) => {
    const health = await request.get('http://127.0.0.1:8080/actuator/health')
    test.skip(health.status() !== 200, 'Backend is not running')
  })

  test('create project task and note then see dashboard data', async ({ page }) => {
    const email = `flow-${Date.now()}@example.com`

    await page.goto('/register')
    await page.getByLabel('昵称', { exact: true }).fill('闭环用户')
    await page.getByLabel('邮箱', { exact: true }).fill(email)
    await page.getByLabel('密码', { exact: true }).fill(password)
    await page.getByLabel('确认密码', { exact: true }).fill(password)
    await page.getByRole('button', { name: '创建并进入工作空间' }).click()
    await expect(page).toHaveURL(/\/app\/dashboard$/, { timeout: 15_000 })
    await expect(page.getByText('还没有进行中的项目')).toBeVisible()

    await page.goto('/app/projects')
    await page.getByRole('button', { name: '创建项目' }).first().click()
    await page.getByLabel('项目名称').fill('闭环项目')
    await page.getByRole('dialog').getByRole('button', { name: '创建并添加任务' }).click()
    await expect(page.getByRole('dialog')).toBeHidden()
    await expect(page.getByRole('heading', { name: '闭环项目' })).toBeVisible()
    await page.getByRole('button', { name: /^任务/ }).click()
    await page.getByRole('button', { name: '创建任务' }).click()
    await page.getByLabel('任务标题').fill('推进方案')
    await page.getByRole('dialog').getByRole('button', { name: '创建任务' }).click()
    await expect(page.getByRole('dialog')).toBeHidden()
    await expect(page.getByText('推进方案')).toBeVisible()

    await page.getByText('推进方案', { exact: true }).click()
    await expect(page.getByRole('dialog', { name: '编辑任务' })).toBeVisible()
    await page.getByRole('dialog').getByLabel('状态', { exact: true }).selectOption('IN_PROGRESS')
    await page.getByRole('button', { name: '保存修改', exact: true }).click()
    await expect(page.getByRole('dialog')).toBeHidden()
    await expect(page.getByLabel('推进方案的状态')).toHaveValue('IN_PROGRESS')

    await page.getByText('推进方案', { exact: true }).click()
    await expect(page.getByRole('dialog', { name: '编辑任务' })).toBeVisible()
    await page.getByRole('dialog').getByLabel('状态', { exact: true }).selectOption('DONE')
    await page.getByRole('button', { name: '保存修改', exact: true }).click()
    await expect(page.getByRole('dialog')).toBeHidden()
    await expect(page.getByLabel('推进方案的状态')).toHaveValue('DONE')

    await page.getByRole('button', { name: /^笔记/ }).click()
    await page.getByRole('button', { name: '写笔记' }).click()
    await expect(page).toHaveURL(/\/app\/notes\//)
    await page.getByLabel('笔记标题').fill('项目方案')
    await page.getByLabel('笔记正文').fill('# 背景\n\n自动保存内容')
    await expect(page.getByText('已保存')).toBeVisible({ timeout: 8_000 })

    await page.goto('/app/dashboard')
    await expect(page.getByText('闭环项目').first()).toBeVisible()
    await expect(page.getByText('项目方案').first()).toBeVisible()

    await page.reload()
    await expect(page.getByText('闭环项目').first()).toBeVisible()
    await expect(page.getByText('项目方案').first()).toBeVisible()

    await page.getByRole('button', { name: '账户菜单' }).click()
    await page.getByRole('button', { name: '退出登录' }).click()
    await expect(page).toHaveURL(/\/login$/)

    const otherEmail = `flow-other-${Date.now()}@example.com`
    await page.goto('/register')
    await page.getByLabel('昵称', { exact: true }).fill('隔离用户')
    await page.getByLabel('邮箱', { exact: true }).fill(otherEmail)
    await page.getByLabel('密码', { exact: true }).fill(password)
    await page.getByLabel('确认密码', { exact: true }).fill(password)
    await page.getByRole('button', { name: '创建并进入工作空间' }).click()
    await expect(page).toHaveURL(/\/app\/dashboard$/, { timeout: 15_000 })
    await expect(page.getByText('还没有进行中的项目')).toBeVisible()
    await expect(page.getByText('闭环项目')).toHaveCount(0)
    await expect(page.getByText('项目方案')).toHaveCount(0)
  })
})
