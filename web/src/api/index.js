/**
 * 后端接口清单：视图层只调用这里的方法，不直接拼接 URL。
 * 所有方法最终都走统一 fetch 组件（./request.js）。
 */
import { api, BASE_URL, EDITOR_UPLOAD_URL, assetUrl, captchaUrl } from './request'

export { assetUrl, captchaUrl, EDITOR_UPLOAD_URL, BASE_URL }

export const authApi = {
  session: () => api.get('/auth/session'),
  login: (data) => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout', {}),
  register: (data) => api.form('/register', data),
  checkEmail: (email) => api.raw('/checkEmail', { method: 'GET', params: { email }, silent: true }),
  checkUserId: (userId) => api.raw('/checkUserId', { method: 'GET', params: { userId }, silent: true }),
  adminSession: () => api.get('/admin/session'),
  adminLogin: (data) => api.form('/admin/login', data),
  adminLogout: () => api.form('/admin/logout', {}),
}

export const metaApi = {
  get: () => api.get('/meta'),
  adClick: (url, id) => api.get('/meta/ad', { url, id }),
}

export const homeApi = {
  list: (params) => api.get('/home', params),
}

export const questionApi = {
  detail: (id, params) => api.get(`/questions/${encodeURIComponent(id)}`, params),
  edit: (id) => api.get(`/questions/${encodeURIComponent(id)}/edit`),
  save: (data) => api.post('/questions', data),
  remove: (questionId) => api.form('/admin/delete/question', { questionId }),
  recover: (questionId) => api.form('/admin/recovery/question', { questionId }),
}

export const commentApi = {
  create: (data) => api.post('/comment', data),
  twoLevel: (commentId) => api.get(`/twoLevelComment/${commentId}`),
}

export const interactionApi = {
  like: (data) => api.post('/like', data),
  followTopic: (data) => api.post('/follow/topic', data),
  followQuestion: (data) => api.post('/follow/question', data),
  followUser: (data) => api.post('/follow/user', data),
}

export const discoverApi = {
  topics: (type) => api.get('/topics', { type }),
  topic: (title) => api.get(`/topics/${encodeURIComponent(title)}`),
  search: (params) => api.get('/search', params),
  userList: (params) => api.get('/users', params),
  userHome: (userId, params) => api.get(`/users/${encodeURIComponent(userId)}`, params),
}

export const messageApi = {
  list: (params) => api.get('/messages', params),
  read: (id) => api.post(`/messages/${id}/read`, {}),
  readAll: () => api.post('/messages/read-all', {}),
}

export const settingApi = {
  updateData: (data) => api.form('/update/data', data),
  updatePassword: (data) => api.form('/update/password', data),
  updateHeadUrl: (imgBase) => api.form('/update/HeadUrl', { imgBase }),
  uploadImage: (formData) => api.upload('/file/image/upload', formData),
  uploadFiles: (formData) => api.upload('/file/upload', formData),
}

export const adminApi = {
  dashboard: (params) => api.get('/admin/dashboard', params),
  users: (params) => api.get('/admin/users', params),
  questions: (params) => api.get('/admin/questions', params),
  search: (params) => api.get('/admin/search', params),
  controller: () => api.get('/admin/controller'),
  advertisements: (params) => api.get('/admin/advertisements', params),
  advertisement: (id) => api.get(`/admin/advertisements/${id}`),
  addAd: (data) => api.post('/admin/advertisement/add', data),
  alterAd: (data) => api.post('/admin/advertisement/alter', data),
  settingAd: (data) => api.post('/admin/advertisement/setting', data),
  closeAd: (data) => api.post('/admin/advertisement/close', data),
  adPositions: () => api.get('/admin/advertisement/positions'),
  batchSettingAd: (data) => api.post('/admin/advertisement/batch-setting', data),
  batchCloseAd: (ids) => api.post('/admin/advertisement/batch-close', { ids }),
  updateUserPower: (userId, power) => api.form('/admin/update/user', { userId, power }),
  resetPassword: (userId) => api.form('/admin/reset/password', { userId }),
  settingTop: (ids) =>
    api.form('/admin/settingTopQuestion', { id1: ids[0], id2: ids[1], id3: ids[2] }),
  refreshCache: (name) => api.get(`/admin/updateHot${name}`),
  refreshTagClass: () => api.get('/admin/updateTagClass'),
  cacheStats: () => api.get('/admin/cache'),
  refreshCacheAll: () => api.post('/admin/cache/refresh', {}),
  invalidateCache: (prefix) => api.form('/admin/cache/invalidate', { prefix }),
  // 动态分类维护
  classifications: (params) => api.get('/admin/classifications', params),
  saveClassification: (data) => api.post('/admin/classifications/save', data),
  deleteClassification: (id) => api.post('/admin/classifications/delete', { id }),
  refreshClassifications: () => api.post('/admin/classifications/refresh', {}),
  // 话题分类维护
  topics: (params) => api.get('/admin/topics', params),
  saveTopic: (data) => api.post('/admin/topics/save', data),
  deleteTopic: (id) => api.post('/admin/topics/delete', { id }),
  refreshTopics: () => api.post('/admin/topics/refresh', {}),
  deleteLog: (data) => api.post('/admin/log/delete', data),
  logUrl: (filename, type = 'download') =>
    `${BASE_URL}/admin/log/download/${encodeURIComponent(filename)}?type=${type}`,
}

export default api
