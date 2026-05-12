const TOKEN_KEY = 'mall_token'
const REFRESH_TOKEN_KEY = 'mall_refresh_token'

export function getAccessToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function getRefreshToken(): string {
  return uni.getStorageSync(REFRESH_TOKEN_KEY) || ''
}

export function hasLoginState(): boolean {
  return Boolean(getAccessToken() || getRefreshToken())
}

export function saveLoginTokens(accessToken?: string, refreshToken?: string) {
  if (accessToken) {
    uni.setStorageSync(TOKEN_KEY, accessToken)
  }
  if (refreshToken) {
    uni.setStorageSync(REFRESH_TOKEN_KEY, refreshToken)
  }
  uni.$emit('mall:auth-changed', { loggedIn: hasLoginState() })
}

export function clearLoginState(reason = 'expired') {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(REFRESH_TOKEN_KEY)
  uni.$emit('mall:auth-changed', { loggedIn: false, reason })
}
