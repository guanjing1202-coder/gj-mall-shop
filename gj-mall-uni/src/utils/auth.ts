const TOKEN_KEY = 'mall_token'
const REFRESH_TOKEN_KEY = 'mall_refresh_token'
const TOKEN_EXPIRE_LEEWAY_SECONDS = 30

export function getAccessToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function getRefreshToken(): string {
  return uni.getStorageSync(REFRESH_TOKEN_KEY) || ''
}

export function hasLoginState(): boolean {
  const accessToken = getAccessToken()
  return Boolean(accessToken && !isTokenExpired(accessToken))
}

export function hasRecoverableLoginState(): boolean {
  const accessToken = getAccessToken()
  const refreshToken = getRefreshToken()
  return Boolean(
    (accessToken && !isTokenExpired(accessToken)) ||
    (refreshToken && !isTokenExpired(refreshToken, 0)),
  )
}

export function isTokenExpired(token: string, leewaySeconds = TOKEN_EXPIRE_LEEWAY_SECONDS): boolean {
  if (!token) return true
  const payload = parseJwtPayload(token)
  const exp = Number(payload?.exp || 0)
  if (!exp) return false
  return Math.floor(Date.now() / 1000) >= exp - leewaySeconds
}

export function saveLoginTokens(accessToken?: string, refreshToken?: string) {
  if (accessToken) {
    uni.setStorageSync(TOKEN_KEY, accessToken)
  }
  if (refreshToken) {
    uni.setStorageSync(REFRESH_TOKEN_KEY, refreshToken)
  }
  uni.$emit('mall:auth-changed', { loggedIn: hasRecoverableLoginState() })
}

export function clearLoginState(reason = 'expired') {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(REFRESH_TOKEN_KEY)
  uni.$emit('mall:auth-changed', { loggedIn: false, reason })
}

function parseJwtPayload(token: string): Record<string, any> | null {
  try {
    const part = token.split('.')[1]
    if (!part) return null
    return JSON.parse(decodeURIComponent(escape(base64UrlDecode(part))))
  } catch (error) {
    return null
  }
}

function base64UrlDecode(input: string): string {
  const normalized = input.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  // #ifdef H5
  return atob(padded)
  // #endif
  // #ifdef MP-WEIXIN
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/='
  let output = ''
  let buffer = 0
  let bits = 0
  for (let i = 0; i < padded.length; i += 1) {
    const value = chars.indexOf(padded.charAt(i))
    if (value < 0 || value === 64) continue
    buffer = (buffer << 6) | value
    bits += 6
    if (bits >= 8) {
      bits -= 8
      output += String.fromCharCode((buffer >> bits) & 0xff)
    }
  }
  return output
  // #endif
  return ''
}
