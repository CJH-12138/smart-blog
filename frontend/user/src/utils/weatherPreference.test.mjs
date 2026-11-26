import test from 'node:test'
import assert from 'node:assert/strict'

import {
  clearWeatherPreference,
  loadWeatherPreference,
  normalizeWeatherPreference,
  saveManualWeatherPreference
} from './weatherPreference.js'

function createStorage() {
  const values = new Map()
  return {
    getItem(key) {
      return values.has(key) ? values.get(key) : null
    },
    setItem(key, value) {
      values.set(key, String(value))
    },
    removeItem(key) {
      values.delete(key)
    }
  }
}

test('normalizeWeatherPreference falls back to auto mode for invalid values', () => {
  assert.deepEqual(normalizeWeatherPreference(null), { mode: 'auto' })
  assert.deepEqual(normalizeWeatherPreference({ mode: 'manual', city: '   ' }), { mode: 'auto' })
  assert.deepEqual(normalizeWeatherPreference({ mode: 'weird', city: '郑州' }), { mode: 'auto' })
})

test('saveManualWeatherPreference persists manual city and loadWeatherPreference reads it back', () => {
  const storage = createStorage()

  saveManualWeatherPreference(storage, ' 郑州市 ')

  assert.deepEqual(loadWeatherPreference(storage), { mode: 'manual', city: '郑州市' })
})

test('clearWeatherPreference removes persisted manual city and resets to auto', () => {
  const storage = createStorage()

  saveManualWeatherPreference(storage, '郑州')
  clearWeatherPreference(storage)

  assert.deepEqual(loadWeatherPreference(storage), { mode: 'auto' })
})
