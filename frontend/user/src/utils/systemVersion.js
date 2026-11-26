function normalizeTimestamp(value) {
  if (!value) {
    return 0
  }

  const timestamp = new Date(value).getTime()
  return Number.isNaN(timestamp) ? 0 : timestamp
}

function normalizeSortOrder(value) {
  const sortOrder = Number(value)
  return Number.isNaN(sortOrder) ? 0 : sortOrder
}

export function sortVersionHistory(list) {
  if (!Array.isArray(list)) {
    return []
  }

  return [...list].sort((left, right) => {
    const sortOrderDiff = normalizeSortOrder(right.sortOrder) - normalizeSortOrder(left.sortOrder)
    if (sortOrderDiff !== 0) {
      return sortOrderDiff
    }

    return normalizeTimestamp(right.releaseDate) - normalizeTimestamp(left.releaseDate)
  })
}
