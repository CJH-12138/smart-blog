export function normalizeBaseApi(baseApi) {
  if (!baseApi) {
    return '/api'
  }

  const normalized = String(baseApi).trim()
  if (!normalized) {
    return '/api'
  }

  if (/^https?:\/\//.test(normalized)) {
    return normalized.replace(/\/+$/, '')
  }

  return `/${normalized.replace(/^\/+|\/+$/g, '')}`
}

export function buildApiUrl(baseApi, path) {
  const normalizedBaseApi = normalizeBaseApi(baseApi)
  const normalizedPath = String(path || '').replace(/^\/+/, '')
  return `${normalizedBaseApi}/${normalizedPath}`
}
