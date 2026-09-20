import { expect, test } from '@playwright/test'

test('phase two: label a note, search its body and return with filters intact', async ({
  page,
  request,
}) => {
  const api = 'http://127.0.0.1:8080/api/v1'
  const email = `phase2-${Date.now()}@example.com`
  const password = 'Password123'
  const registered = await request.post(`${api}/auth/register`, {
    data: { email, password, name: '二期验收' },
  })
  expect(registered.status()).toBe(201)
  const { accessToken } = await registered.json()
  const headers = { Authorization: `Bearer ${accessToken}` }
  const created = await request.post(`${api}/notes`, {
    headers,
    data: { title: '二期检索样本', content: '中文资料检索内容，保留来源筛选。' },
  })
  expect(created.status()).toBe(201)
  const note = await created.json()
  await page.goto('/login')
  await page.getByLabel('邮箱', { exact: true }).fill(email)
  await page.getByLabel('密码', { exact: true }).fill(password)
  await page.getByRole('button', { name: '进入工作空间', exact: true }).click()
  await expect(page).toHaveURL(/\/app\/dashboard$/, { timeout: 15000 })
  await page.goto(`/app/notes/${note.id}`)
  await expect(page.getByLabel('笔记标题')).toHaveValue('二期检索样本')
  await expect(page.getByRole('heading', { name: '附件', exact: true })).toBeVisible()
  await page.getByRole('button', { name: '管理标签' }).click()
  await page.getByRole('dialog').getByLabel('搜索标签名称').fill('二期资料')
  await page.getByRole('button', { name: '创建标签“二期资料”' }).click()
  await expect(page.getByText('已选择 1 / 20 个标签')).toBeVisible()
  await page.getByRole('button', { name: '保存标签', exact: true }).click()
  await expect(page.getByRole('dialog')).toBeHidden()
  await page.getByRole('link', { name: '二期资料', exact: true }).click()
  await expect(page).toHaveURL(/\/app\/search\?/)
  await page.getByLabel('搜索内容', { exact: true }).fill('中文资料')
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await expect(page.getByRole('link', { name: '二期检索样本', exact: true })).toBeVisible({
    timeout: 10000,
  })
  const searchUrl = new URL(page.url())
  // Return links keep filters while dropping nested navigation history.
  searchUrl.searchParams.delete('origin')
  await page.getByRole('link', { name: '二期检索样本', exact: true }).click()
  await expect(page.getByLabel('笔记标题')).toHaveValue('二期检索样本')
  await page.getByRole('link', { name: '返回来源', exact: true }).click()
  await expect(page).toHaveURL(searchUrl.toString())
  await expect(page.getByLabel('搜索内容', { exact: true })).toHaveValue('中文资料')
})
