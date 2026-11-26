import test from 'node:test'
import assert from 'node:assert/strict'

import { sortVersionHistory } from './systemVersion.js'

test('sortVersionHistory orders newer releases before older ones', () => {
  const versions = [
    { id: 1, sortOrder: 1, releaseDate: '2026-04-20 10:00:00' },
    { id: 2, sortOrder: 1, releaseDate: '2026-04-25 10:00:00' },
    { id: 3, sortOrder: 1, releaseDate: '2026-04-22 10:00:00' }
  ]

  assert.deepEqual(
    sortVersionHistory(versions).map((item) => item.id),
    [2, 3, 1]
  )
})

test('sortVersionHistory prefers higher sortOrder before releaseDate', () => {
  const versions = [
    { id: 1, sortOrder: 1, releaseDate: '2026-04-25 10:00:00' },
    { id: 2, sortOrder: 10, releaseDate: '2026-04-20 10:00:00' },
    { id: 3, sortOrder: 5, releaseDate: '2026-04-22 10:00:00' }
  ]

  assert.deepEqual(
    sortVersionHistory(versions).map((item) => item.id),
    [2, 3, 1]
  )
})
