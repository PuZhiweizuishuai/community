/** 把 "a,b，c" 形式的标签串切成数组 */
export function splitTags(tag) {
  if (!tag) return []
  return String(tag)
    .split(/[,，]/)
    .map((t) => t.trim())
    .filter(Boolean)
}

/** 数字缩写：1200 -> 1.2k */
export function shortNumber(value) {
  const n = Number(value || 0)
  if (n < 1000) return String(n)
  if (n < 10000) return `${(n / 1000).toFixed(1)}k`
  return `${(n / 10000).toFixed(1)}w`
}

/** 后台状态码文案 */
export function statusLabel(status) {
  return Number(status) === 1 ? '正常' : '已删除'
}

export function powerLabel(power) {
  if (power === 0) return '管理员'
  if (power === 2) return 'VIP'
  return '普通用户'
}

export function notificationIcon(type) {
  if (type >= 6) return 'system'
  if (type === 3) return 'like'
  return 'comment'
}

/** 拼接带查询参数的路由，过滤空值 */
export function queryOf(params) {
  return Object.fromEntries(
    Object.entries(params || {}).filter(([, v]) => v !== undefined && v !== null && v !== ''),
  )
}
