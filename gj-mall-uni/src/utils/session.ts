import { ensureSession } from './request'

export function syncSession() {
  return ensureSession({ showToast: false, redirect: false })
}

export async function requireSession(message = '请先登录'): Promise<boolean> {
  const ok = await ensureSession({ showToast: false, redirect: false })
  if (ok) return true
  if (message) {
    uni.showToast({ title: message, icon: 'none' })
  }
  setTimeout(() => {
    uni.switchTab({ url: '/pages/user/user' })
  }, 300)
  return false
}
