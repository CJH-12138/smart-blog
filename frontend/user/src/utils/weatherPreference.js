const WEATHER_PREFERENCE_KEY = 'weatherPreference'

export function normalizeWeatherPreference(value) {
  if (!value || value.mode !== 'manual') {
    return { mode: 'auto' }
  }

  const city = String(value.city || '').trim()
  if (!city) {
    return { mode: 'auto' }
  }

  return {
    mode: 'manual',
    city
  }
}

export function loadWeatherPreference(storage = globalThis.localStorage) {
  if (!storage) {
    return { mode: 'auto' }
  }

  try {
    const raw = storage.getItem(WEATHER_PREFERENCE_KEY)
    if (!raw) {
      return { mode: 'auto' }
    }
    return normalizeWeatherPreference(JSON.parse(raw))
  } catch (error) {
    console.warn('读取天气偏好失败:', error)
    return { mode: 'auto' }
  }
}

export function saveManualWeatherPreference(storage = globalThis.localStorage, city) {
  if (!storage) {
    return
  }

  const preference = normalizeWeatherPreference({ mode: 'manual', city })
  storage.setItem(WEATHER_PREFERENCE_KEY, JSON.stringify(preference))
}

export function clearWeatherPreference(storage = globalThis.localStorage) {
  if (!storage) {
    return
  }

  storage.removeItem(WEATHER_PREFERENCE_KEY)
}
