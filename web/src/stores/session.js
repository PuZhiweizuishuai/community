import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api'

/**
 * 登录态：SPA 启动时通过 /api/auth/session 拉取一次，之后由登录/退出动作更新。
 */
export const useSessionStore = defineStore('session', () => {
  const user = ref(null)
  const admin = ref(null)
  const notificationCount = ref(0)
  const loaded = ref(false)

  const loggedIn = computed(() => !!user.value)
  const isAdmin = computed(() => !!admin.value)
  const power = computed(() => user.value?.power ?? 1)

  let pending = null

  async function refresh(force = false) {
    if (!force && pending) return pending
    pending = authApi
      .session()
      .then((data) => {
        user.value = data?.user ?? null
        admin.value = data?.admin ?? null
        notificationCount.value = data?.notificationCount ?? 0
        loaded.value = true
        return data
      })
      .catch(() => {
        user.value = null
        admin.value = null
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
      user.value = null
      admin.value = null
      notificationCount.value = 0
    }
  }

  async function adminLogout() {
    try {
      await authApi.adminLogout()
    } finally {
      admin.value = null
    }
  }

  return {
    user,
    admin,
    notificationCount,
    loaded,
    loggedIn,
    isAdmin,
    power,
    refresh,
    applyLogin,
    logout,
    adminLogout,
  }
})
