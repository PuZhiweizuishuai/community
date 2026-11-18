import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api'

/**
 * 登录态：SPA 启动时通过 /api/auth/session 拉取一次，之后由登录/退出动作更新。
 *
 * 管理员身份不再依赖独立的 admin 会话，而是由后端 RBAC 返回的权限编码决定：
 * session.permissions 同时驱动路由守卫、后台菜单与操作按钮。
 */
export const useSessionStore = defineStore('session', () => {
  const user = ref(null)
  const admin = ref(null)
  const permissions = ref([])
  const banned = ref(false)
  const ban = ref(null)
  const notificationCount = ref(0)
  const loaded = ref(false)

  const loggedIn = computed(() => !!user.value)
  const isAdmin = computed(() => permissions.value.includes('admin:access') || !!admin.value)
  const power = computed(() => user.value?.power ?? 1)
  const banMessage = computed(() => ban.value?.banMessage || '')

  /** 判断是否拥有某个权限点；不传编码视为无需权限 */
  function can(code) {
    if (!code) return true
    return permissions.value.includes(code)
  }

  /** 是否可以进行社区写操作：已登录、未被封禁且拥有对应权限 */
  function canWrite(code) {
    return loggedIn.value && !banned.value && can(code)
  }

  let pending = null

  async function refresh(force = false) {
    if (!force && pending) return pending
    pending = authApi
      .session()
      .then((data) => {
        user.value = data?.user ?? null
        admin.value = data?.admin ?? null
        permissions.value = Array.isArray(data?.permissions) ? data.permissions : []
        banned.value = !!data?.banned
        ban.value = data?.ban ?? null
        notificationCount.value = data?.notificationCount ?? 0
        loaded.value = true
        return data
      })
      .catch(() => {
        user.value = null
        admin.value = null
        permissions.value = []
        banned.value = false
        ban.value = null
        notificationCount.value = 0
        loaded.value = true
        return null
      })
      .finally(() => {
        pending = null
      })
    return pending
  }

  function applyLogin(data) {
    user.value = data?.user ?? null
    loaded.value = true
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      reset()
    }
  }

  async function adminLogout() {
    try {
      await authApi.adminLogout()
    } finally {
      // 后台与前台共用登录态，退出后台即完整退出
      reset()
    }
  }

  function reset() {
    user.value = null
    admin.value = null
    permissions.value = []
    banned.value = false
    ban.value = null
    notificationCount.value = 0
  }

  return {
    user,
    admin,
    permissions,
    banned,
    ban,
    notificationCount,
    loaded,
    loggedIn,
    isAdmin,
    power,
    banMessage,
    can,
    canWrite,
    refresh,
    applyLogin,
    logout,
    adminLogout,
  }
})
