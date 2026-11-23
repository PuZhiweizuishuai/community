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

/**
 * 旧版编辑器保存的回复整段带 4 个以上空格缩进，Markdown 会按缩进代码块渲染成 <pre><code>。
 * 这里去掉公共缩进，让历史评论按普通段落展示；正常内容原样返回。
 */
export function normalizeCommentMarkdown(content) {
  const text = String(content ?? '').replace(/\r\n?/g, '\n')
  if (!text.trim()) return ''
  if (!/^[ \t]{4,}\S/m.test(text)) return text.trim()

  const lines = text.split('\n')
  const indentOf = (line) => line.match(/^[ \t]*/)[0].length
  const minIndent = Math.min(...lines.filter((line) => line.trim()).map(indentOf))
  if (minIndent >= 4) {
    return lines
      .map((line) => (line.trim() ? line.slice(Math.min(indentOf(line), minIndent)) : ''))
      .join('\n')
      .trim()
  }
  // 首行带缩进、后续行顶格（如旧版嵌入的 iframe），只去掉首行缩进
  return [lines[0].trimStart(), ...lines.slice(1)].join('\n').trim()
}

/** 后台状态码文案 */
export function statusLabel(status) {
  return Number(status) === 1 ? '正常' : '已删除'
}

/** 毫秒时间戳 → yyyy-MM-dd HH:mm；空值返回占位符 */
export function formatDateTime(value) {
  if (value === null || value === undefined || value === '') return '—'
  const date = new Date(Number(value))
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/**
 * 毫秒时间戳 → 相对时间：1 分钟内显示秒、1 小时内显示分钟、24 小时内显示小时、
 * 4 天内显示天，更早显示完整日期。由前端计算，后端只返回时间戳。
 */
export function timeAgo(value) {
  const time = Number(value)
  if (!time || !Number.isFinite(time)) return ''
  const diff = Date.now() - time
  if (diff < 0) return formatDateTime(time)
  if (diff < 60_000) return `${Math.max(1, Math.floor(diff / 1000))} 秒前`
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  if (diff < 345_600_000) return `${Math.floor(diff / 86_400_000)} 天前`
  return formatDateTime(time)
}

function pad2(n) {
  return String(n).padStart(2, '0')
}

/** 聊天消息的日期分组键：本地时区的 yyyy-MM-dd */
export function chatDayKey(value) {
  const date = new Date(Number(value) || Date.now())
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

/** 聊天消息的日期分隔文案：今天 / 昨天 / yyyy-MM-dd */
export function chatDayLabel(value) {
  const key = chatDayKey(value)
  if (key === chatDayKey(Date.now())) return '今天'
  if (key === chatDayKey(Date.now() - 86_400_000)) return '昨天'
  return key
}

/** 聊天消息时间：当天只显示 HH:mm，跨天补上 MM-dd */
export function chatTimeLabel(value) {
  const date = new Date(Number(value) || Date.now())
  const hm = `${pad2(date.getHours())}:${pad2(date.getMinutes())}`
  return chatDayKey(value) === chatDayKey(Date.now()) ? hm : `${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${hm}`
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

/** 简化 User-Agent 展示：浏览器 · 操作系统 */
export function parseUserAgent(value) {
  const ua = String(value || '').trim()
  if (!ua) return '未知设备'
  let browser = ''
  if (/MicroMessenger/i.test(ua)) browser = '微信'
  else if (/Edg\//i.test(ua)) browser = 'Edge'
  else if (/Firefox\//i.test(ua)) browser = 'Firefox'
  else if (/Chrome\//i.test(ua)) browser = 'Chrome'
  else if (/Safari\//i.test(ua)) browser = 'Safari'
  else if (/curl\//i.test(ua)) browser = 'curl'
  else if (/PostmanRuntime/i.test(ua)) browser = 'Postman'

  let os = ''
  if (/Windows NT/i.test(ua)) os = 'Windows'
  else if (/Android/i.test(ua)) os = 'Android'
  else if (/iPhone|iPad|iPod/i.test(ua)) os = 'iOS'
  else if (/Mac OS X/i.test(ua)) os = 'macOS'
  else if (/Linux/i.test(ua)) os = 'Linux'

  const label = [browser, os].filter(Boolean).join(' · ')
  return label || ua.slice(0, 60)
}
