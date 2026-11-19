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
  features: () => api.get('/auth/features'),
  forgotPassword: (data) => api.post('/auth/forgot-password', data),
  resetPassword: (data) => api.post('/auth/reset-password', data),
  sendRegisterCode: (data) => api.post('/auth/send-register-code', data),
  checkEmail: (email) => api.get('/checkEmail', { email }, { silent: true }),
  checkUserId: (userId) => api.get('/checkUserId', { userId }, { silent: true }),
  adminSession: () => api.get('/admin/session'),
  adminLogin: (data) => api.form('/admin/login', data),
  adminLogout: () => api.form('/admin/logout', {}),
}

export const metaApi = {
  get: () => api.get('/meta'),
  hot: () => api.get('/meta/hot'),
  hotUsers: () => api.get('/meta/hot-users'),
  topicOptions: (params) => api.get('/meta/topic-options', params),
  tagCategories: () => api.get('/meta/tag-categories'),
  ads: (position) => api.get('/meta/ads', { position }),
  adClick: (url, id) => api.get('/meta/ad', { url, id }),
}

export const homeApi = {
  list: (params) => api.get('/home', params),
}

export const questionApi = {
  detail: (id) => api.get(`/questions/${encodeURIComponent(id)}`),
  comments: (id, params) => api.get(`/questions/${encodeURIComponent(id)}/comments`, params),
  relevant: (id) => api.get(`/questions/${encodeURIComponent(id)}/relevant`),
  edit: (id) => api.get(`/questions/${encodeURIComponent(id)}/edit`),
  save: (data) => api.post('/questions', data),
  unlockContent: (id, data) => api.post(`/questions/${encodeURIComponent(id)}/content/unlock`, data),
  remove: (questionId) => api.form('/admin/delete/question', { questionId }),
  recover: (questionId) => api.form('/admin/recovery/question', { questionId }),
}

export const commentApi = {
  create: (data) => api.post('/comment', data),
  twoLevel: (commentId, params) => api.get(`/twoLevelComment/${commentId}`, params),
}

export const interactionApi = {
  like: (data) => api.post('/like', data),
  followTopic: (data) => api.post('/follow/topic', data),
  followQuestion: (data) => api.post('/follow/question', data),
  followUser: (data) => api.post('/follow/user', data),
}

export const discoverApi = {
  topics: (type) => api.get('/topics', { type }),
  topic: (id) => api.get(`/topics/${encodeURIComponent(id)}`),
  topicQuestions: (id, params) => api.get(`/topics/${encodeURIComponent(id)}/questions`, params),
  search: (params) => api.get('/search', params),
  userList: (params) => api.get('/users', params),
  userHome: (userId, params) => api.get(`/users/${encodeURIComponent(userId)}`, params),
}

// 话题吧务操作：置顶、加精、删帖、改标签
export const moderationApi = {
  remove: (topicId, questionId) => api.post(`/topics/${topicId}/moderation/delete`, { questionId }),
  recover: (topicId, questionId) => api.post(`/topics/${topicId}/moderation/recover`, { questionId }),
  setTop: (topicId, questionId, enabled) => api.post(`/topics/${topicId}/moderation/top`, { questionId, enabled }),
  setEssence: (topicId, questionId, enabled) =>
    api.post(`/topics/${topicId}/moderation/essence`, { questionId, enabled }),
  setTags: (topicId, questionId, tagNames) =>
    api.post(`/topics/${topicId}/moderation/tags`, { questionId, tagNames }),
}

export const messageApi = {
  list: (params) => api.get('/messages', params),
  read: (id) => api.post(`/messages/${id}/read`, {}),
  readAll: () => api.post('/messages/read-all', {}),
}

// 用户侧内容审核：查看自己的待审/被驳回内容，修改评论后重新提交
export const reviewApi = {
  mine: (params) => api.get('/reviews/mine', params),
  resubmitComment: (commentId, content) =>
    api.post(`/reviews/comments/${commentId}/resubmit`, { content }),
}

// 用户举报问题 / 评论
export const reportApi = {
  create: (data) => api.post('/reports', data),
}

export const settingApi = {
  updateData: (data) => api.form('/update/data', data),
  updatePassword: (data) => api.form('/update/password', data),
  updateHeadUrl: (imgBase) => api.form('/update/HeadUrl', { imgBase }),
  // 统一上传入口 /api/file/upload，兼容 editormd-image-file 与 file[] 字段
  uploadImage: (formData) => api.upload('/file/upload', formData),
  uploadFiles: (formData) => api.upload('/file/upload', formData),
}

export const adminApi = {
  dashboard: (params) => api.get('/admin/dashboard', params),
  users: (params) => api.get('/admin/users', params),
  questions: (params) => api.get('/admin/questions', params),
  comments: (params) => api.get('/admin/comments', params),
  commentDetail: (id) => api.get(`/admin/comments/${id}`),
  deleteComment: (id) => api.post(`/admin/comments/${id}/delete`, {}),
  recoverComment: (id) => api.post(`/admin/comments/${id}/recover`, {}),
  reports: (params) => api.get('/admin/reports', params),
  reportDetail: (id) => api.get(`/admin/reports/${id}`),
  handleReport: (id, data) => api.post(`/admin/reports/${id}/handle`, data),
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
  // RBAC 角色与权限
  roles: () => api.get('/admin/roles'),
  roleOptions: () => api.get('/admin/roles/options'),
  permissions: () => api.get('/admin/permissions'),
  rolePermissionIds: (id) => api.get(`/admin/roles/${id}/permissions`),
  saveRole: (data) => api.post('/admin/roles/save', data),
  deleteRole: (id) => api.post('/admin/roles/delete', { id }),
  saveRolePermissions: (roleId, permissionIds) =>
    api.post('/admin/roles/permissions', { roleId, permissionIds }),
  userRoles: (userId) => api.get(`/admin/users/${userId}/roles`),
  saveUserRoles: (userId, roleIds) => api.post('/admin/users/roles', { userId, roleIds }),
  // 用户封禁 / 解封
  userBan: (userId) => api.get(`/admin/users/${userId}/ban`),
  banUser: (userId, data) => api.post(`/admin/users/${userId}/ban`, data),
  unbanUser: (userId) => api.post(`/admin/users/${userId}/unban`, {}),
  settingTop: (ids) =>
    api.form('/admin/settingTopQuestion', { id1: ids[0], id2: ids[1], id3: ids[2] }),
  refreshCache: (name) => api.post(`/admin/updateHot${name}`, {}),
  refreshTagClass: () => api.post('/admin/updateTagClass', {}),
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
  topicModerators: (topicId) => api.get(`/admin/topics/${topicId}/moderators`),
  assignTopicModerator: (topicId, data) => api.post(`/admin/topics/${topicId}/moderators`, data),
  removeTopicModerator: (topicId, userId) =>
    api.post(`/admin/topics/${topicId}/moderators/remove`, { userId }),
  // 标签审核
  tags: (params) => api.get('/admin/tags', params),
  approveTag: (id) => api.post('/admin/tags/approve', { id }),
  rejectTag: (id, reason) => api.post('/admin/tags/reject', { id, reason }),
  renameTag: (id, name) => api.post('/admin/tags/rename', { id, name }),
  deleteTag: (id) => api.post('/admin/tags/delete', { id }),
  deleteLog: (data) => api.post('/admin/log/delete', data),
  // 系统配置（注册开关 / 聊天室 / 内容审核 / SMTP 邮件）
  systemConfig: () => api.get('/admin/config'),
  saveSystemConfig: (data) => api.post('/admin/config/save', data),
  testMail: (to) => api.post('/admin/config/test-mail', { to }),
  // 内容审核（问题 + 评论）
  reviews: (params) => api.get('/admin/reviews', params),
  reviewCount: () => api.get('/admin/reviews/count'),
  reviewDetail: (type, id) => api.get(`/admin/reviews/${type}/${id}`),
  approveReview: (type, id) => api.post('/admin/reviews/approve', { type, id }),
  rejectReview: (data) => api.post('/admin/reviews/reject', data),
  // 存储配置（本地 / 多对象存储）
  storages: () => api.get('/admin/storage'),
  saveStorage: (data) => api.post('/admin/storage/save', data),
  deleteStorage: (id) => api.post('/admin/storage/delete', { id }),
  testStorage: (data) => api.post('/admin/storage/test', data),
  // 文件管理
  files: (params) => api.get('/admin/file/list', params),
  deleteFile: (id) => api.post('/admin/file/delete', { id }),
  logUrl: (filename, type = 'download') =>
    `${BASE_URL}/admin/log/download/${encodeURIComponent(filename)}?type=${type}`,
}

export default api
