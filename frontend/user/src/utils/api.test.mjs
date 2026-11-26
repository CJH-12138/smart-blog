import test from 'node:test'
import assert from 'node:assert/strict'

import { buildApiUrl, normalizeBaseApi } from './api.js'

test('normalizeBaseApi falls back to /api when env value is missing', () => {
  assert.equal(normalizeBaseApi(undefined), '/api')
  assert.equal(normalizeBaseApi(''), '/api')
})

test('normalizeBaseApi trims trailing slashes for relative and absolute values', () => {
  assert.equal(normalizeBaseApi('/api/'), '/api')
  assert.equal(normalizeBaseApi('http://localhost:8080/api/'), 'http://localhost:8080/api')
})

test('buildApiUrl appends the path without duplicate slashes', () => {
  assert.equal(buildApiUrl(undefined, '/user/avatar'), '/api/user/avatar')
  assert.equal(buildApiUrl('/api/', '/user/avatar'), '/api/user/avatar')
  assert.equal(buildApiUrl('http://localhost:8080/api/', '/user/avatar'), 'http://localhost:8080/api/user/avatar')
})
