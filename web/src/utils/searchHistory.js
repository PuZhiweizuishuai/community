/**
 * 站内搜索历史：保存在 localStorage，最多 10 条，去重且最近使用的排在最前。
 */
export const SEARCH_HISTORY_KEY = 'community-search-history'
const MAX = 10

function read() {
  try {
    const value = JSON.parse(localStorage.getItem(SEARCH_HISTORY_KEY) || '[]')
    return Array.isArray(value) ? value.filter((item) => typeof item === 'string' && item.trim()) : []
  } catch {
    return []
  }
}

function write(list) {
  try {
    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(list))
  } catch {
    /* 隐私模式 / 存储配额不足时静默降级 */
  }
}

export function getHistory() {
  return read()
}

export function addHistory(keyword) {
  const value = String(keyword || '').trim()
  if (!value) return read()
  const list = read().filter((item) => item.toLowerCase() !== value.toLowerCase())
  list.unshift(value)
  const next = list.slice(0, MAX)
  write(next)
  return next
}

export function removeHistory(keyword) {
  const next = read().filter((item) => item !== keyword)
  write(next)
  return next
}

export function clearHistory() {
  write([])
  return []
}
