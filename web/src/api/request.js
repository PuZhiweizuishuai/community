/**
 * 全站统一 fetch 请求组件
 * ---------------------------------------------------------------------------
 * 所有与后端的交互都从这里发出，集中处理：
 *   - baseURL（开发走 Vite 代理 /api，生产可配 VITE_API_BASE 指向后端域名）
 *   - credentials: 'include'（Spring Session 的 SESSION cookie 必须随请求携带）
 *   - JSON / 表单 / multipart 三种请求体
 *   - 统一响应体 { status, message, timestamp, data } 归一化：成功只返回 data
 *   - HTTP 状态码统一抛 ApiError（status 与响应体 status 一致）
 *   - 全局 loading 计数、全局错误提示、401 登录态失效回调
 *
 * 用法：
 *   import { api, ApiError } from '@/api/request'
 *   const data = await api.get('/home', { page: 1 })
 *   await api.post('/auth/login', { email, password })
 *   await api.form('/admin/login', { email, password, validateCode })
 *   const raw = await api.upload('/file/upload', formData)
 */

export const BASE_URL = import.meta.env.VITE_API_BASE || '/api'
export const ASSET_BASE = import.meta.env.VITE_ASSET_BASE || ''

/** 业务/网络错误：status 与后端响应体 status、HTTP 状态码保持一致 */
export class ApiError extends Error {
  constructor(message, { status = 0, payload = null, cause } = {}) {
    super(message || '请求失败，请稍后重试')
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
    this.cause = cause
  }
}

let loadingCount = 0
const loadingListeners = new Set()
let errorHandler = null
let unauthorizedHandler = null

/** 订阅全局 loading 状态（返回取消订阅函数） */
export function onLoadingChange(listener) {
  loadingListeners.add(listener)
  listener(loadingCount > 0)
  return () => loadingListeners.delete(listener)
}

/** 注册全局错误提示，例如 toast */
export function setErrorHandler(handler) {
  errorHandler = handler
}

/** 注册登录态失效回调，例如跳转登录页 */
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

function changeLoading(delta) {
  const before = loadingCount > 0
  loadingCount = Math.max(0, loadingCount + delta)
  const after = loadingCount > 0
  if (before !== after) loadingListeners.forEach((fn) => fn(after))
}

function joinUrl(path) {
  if (/^https?:\/\//i.test(path)) return path
  if (path.startsWith(BASE_URL + '/') || path === BASE_URL) return path
  return BASE_URL + (path.startsWith('/') ? path : `/${path}`)
}

function appendParams(url, params) {
  if (!params) return url
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    if (Array.isArray(value)) value.forEach((v) => search.append(key, v))
    else search.append(key, value)
  })
  const query = search.toString()
  if (!query) return url
  return url + (url.includes('?') ? '&' : '?') + query
}

function buildBody(data, bodyType) {
  if (data === undefined || data === null) return undefined
  if (bodyType === 'raw') return data
  if (bodyType === 'multipart' || data instanceof FormData) return data
  if (bodyType === 'form') {
    const params = new URLSearchParams()
    Object.entries(data).forEach(([key, value]) => {
      if (value === undefined || value === null) return
      params.append(key, typeof value === 'boolean' ? String(value) : value)
    })
    return params
  }
  return JSON.stringify(data)
}

function defaultHeaders(bodyType) {
  if (bodyType === 'json') return { Accept: 'application/json', 'Content-Type': 'application/json' }
  if (bodyType === 'form')
    return { Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' }
  return { Accept: 'application/json' }
}

/** 统一响应体 { status, message, timestamp, data }：成功取 data，失败抛 ApiError */
function normalize(payload) {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) return payload
  if ('status' in payload && 'message' in payload) {
    const ok = Number(payload.status) >= 200 && Number(payload.status) < 300
    if (ok) return payload.data !== undefined ? payload.data : payload
    throw new ApiError(payload.message, { status: payload.status, payload })
  }
  // 非统一响应体（如 Vditor 上传协议）按原样返回，由调用方自行解析
  return payload
}

/** 登录接口自身的 401 表示凭证错误，不触发「登录态失效」的全局跳转 */
function isAuthEndpoint(url) {
  return /\/auth\/login$|\/admin\/login$/.test(url)
}

/**
 * 底层请求。
 * @param {string} path 相对 /api 的路径，例如 '/home'
 * @param {object} options
 */
export async function request(path, options = {}) {
  const {
    method = 'GET',
    params,
    data,
    bodyType = 'json',
    headers: extraHeaders = {},
    signal,
    timeout = 20000,
    loading = true,
    silent = false,
    raw = false,
    responseType = 'json',
  } = options

  const url = appendParams(joinUrl(path), params)
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeout)
  if (signal) signal.addEventListener('abort', () => controller.abort(), { once: true })

  const headers = { ...defaultHeaders(bodyType), ...extraHeaders }
  const init = {
    method,
    headers,
    credentials: 'include',
    signal: controller.signal,
  }
  const body = buildBody(data, bodyType)
  if (body !== undefined && method !== 'GET' && method !== 'HEAD') init.body = body

  if (loading) changeLoading(1)
  try {
    const response = await fetch(url, init)
    let payload
    if (responseType === 'text') {
      payload = await response.text()
    } else if (responseType === 'blob') {
      payload = await response.blob()
    } else {
      const text = await response.text()
      if (!text) payload = null
      else {
        try {
          payload = JSON.parse(text)
        } catch {
          payload = text
        }
      }
    }

    if (!response.ok) {
      if (response.status === 401 && unauthorizedHandler && !isAuthEndpoint(url)) {
        unauthorizedHandler(payload, url)
      }
      const message = payload?.message || `请求失败（HTTP ${response.status}）`
      throw new ApiError(message, { status: payload?.status ?? response.status, payload })
    }

    if (raw || responseType !== 'json') return payload
    return normalize(payload)
  } catch (error) {
    const apiError =
      error instanceof ApiError
        ? error
        : new ApiError(
            error?.name === 'AbortError' ? '请求超时，请稍后重试' : '网络连接异常，请检查后端服务是否已启动',
            { cause: error },
          )
    if (!silent && errorHandler) errorHandler(apiError)
    throw apiError
  } finally {
    clearTimeout(timer)
    if (loading) changeLoading(-1)
  }
}

/** 高阶封装：常用动词 */
export const api = {
  get: (path, params, options = {}) => request(path, { ...options, method: 'GET', params }),
  delete: (path, params, options = {}) => request(path, { ...options, method: 'DELETE', params }),
  post: (path, data, options = {}) => request(path, { ...options, method: 'POST', data, bodyType: 'json' }),
  put: (path, data, options = {}) => request(path, { ...options, method: 'PUT', data, bodyType: 'json' }),
  patch: (path, data, options = {}) => request(path, { ...options, method: 'PATCH', data, bodyType: 'json' }),
  /** 表单编码提交，兼容后端大量以 @RequestParam 接收的老接口 */
  form: (path, data, options = {}) => request(path, { ...options, method: 'POST', data, bodyType: 'form' }),
  /** multipart 上传，返回原始响应，便于 Vditor / 头像上传按自身协议解析 */
  upload: (path, formData, options = {}) =>
    request(joinUrl(path), {
      ...options,
      method: 'POST',
      data: formData,
      bodyType: 'multipart',
      raw: true,
    }),
  /** 需要读取完整响应体（如第三方上传协议）时使用 */
  raw: (path, options = {}) => request(path, { ...options, raw: true }),
}

/** 后端静态资源（头像、上传文件）地址；开发由 Vite 代理到 Spring Boot */
export function assetUrl(path) {
  if (!path) return `${ASSET_BASE}/image/head/nohead.png`
  if (/^https?:\/\//i.test(path) || path.startsWith('data:')) return path
  let normalized = path.startsWith('/') ? path : `/${path}`
  // 历史本地文件链接 /file/... 统一映射到新接口 /api/file/0/...
  if (normalized.startsWith('/file/')) normalized = `/api/file/0${normalized}`
  return `${ASSET_BASE}${normalized}`
}

/** 验证码图片地址，带随机参数防止缓存；会话 cookie 由浏览器自动携带 */
export function captchaUrl() {
  return `${BASE_URL}/captcha?t=${Date.now()}-${Math.random().toString(36).slice(2)}`
}

/** 编辑器/Vditor 上传地址（表单字段名 file[]） */
export const EDITOR_UPLOAD_URL = `${BASE_URL}/file/upload`

export default api
