<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, LogOut, Search, Settings, ShieldCheck, SquarePen, UserRound, X } from '@lucide/vue'
import { addHistory, clearHistory, getHistory, removeHistory } from '@/utils/searchHistory'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/Avatar.vue'
import SiteLogo from '@/components/SiteLogo.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const toast = useToastStore()

const keyword = ref(typeof route.params.keyword === 'string' ? route.params.keyword : '')
const navItems = [
  { label: '首页', to: '/' },
  { label: '话题', to: '/topics' },
  { label: '用户', to: '/users' },
  { label: '聊天室', to: '/chat' },
  { label: '关于', to: '/about' },
]

const active = (to) => (to === '/' ? route.path === '/' : route.path.startsWith(to))

const history = ref(getHistory())
const historyOpen = ref(false)
let blurTimer = null

function openHistory() {
  clearTimeout(blurTimer)
  history.value = getHistory()
  historyOpen.value = true
}
function closeHistoryLater() {
  blurTimer = setTimeout(() => (historyOpen.value = false), 150)
}
function goSearch(q) {
  const value = String(q || '').trim()
  if (!value) return
  keyword.value = value
  history.value = addHistory(value)
  historyOpen.value = false
  router.push(`/search/${encodeURIComponent(value)}`)
}
function doSearch() {
  goSearch(keyword.value)
}
function dropHistory(item) {
  history.value = removeHistory(item)
}
function wipeHistory() {
  history.value = clearHistory()
}
watch(() => route.fullPath, () => {
  historyOpen.value = false
  if (typeof route.params.keyword === 'string') keyword.value = route.params.keyword
})

async function logout() {
  await session.logout()
  toast.success('已退出登录')
  router.push('/')
}

const badge = computed(() => (session.notificationCount > 99 ? '99+' : session.notificationCount))
</script>

<template>
  <header class="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur">
    <div class="page-container navbar min-h-16 gap-2 px-0">
      <div class="navbar-start gap-2">
        <RouterLink to="/" class="flex items-center gap-2 text-lg font-bold text-indigo-600">
          <SiteLogo :size="34" rounded="0.55rem" />
          <span class="hidden sm:inline">不挂高数</span>
        </RouterLink>
        <nav class="ml-4 hidden items-center gap-1 lg:flex">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="btn btn-ghost btn-sm font-normal"
            :class="active(item.to) ? 'text-indigo-600 bg-indigo-50' : 'text-slate-600'"
          >
            {{ item.label }}
          </RouterLink>
        </nav>
      </div>

      <div class="navbar-center relative">
        <form class="join hidden w-full max-w-xs md:flex" @submit.prevent="doSearch">
          <input
            v-model="keyword"
            class="input input-bordered input-sm join-item w-full focus:border-indigo-400 focus:outline-none"
            placeholder="搜索问题、用户…"
            aria-label="站内搜索"
            autocomplete="off"
            @focus="openHistory"
            @blur="closeHistoryLater"
          />
          <button class="btn btn-sm btn-primary join-item" type="submit"><Search class="size-4" />搜索</button>
        </form>

        <div
          v-if="historyOpen && history.length"
          class="theme-menu absolute left-0 top-full z-50 mt-1 hidden w-[19rem] rounded-box border p-3 shadow-xl md:block"
          @mousedown.prevent
        >
          <div class="mb-2 flex items-center justify-between">
            <span class="text-xs font-medium text-slate-500">搜索历史</span>
            <button class="btn btn-ghost btn-xs" type="button" @click="wipeHistory">清空</button>
          </div>
          <div class="flex flex-wrap gap-1.5">
            <button
              v-for="item in history"
              :key="item"
              class="group flex items-center gap-1 rounded-full border border-slate-200 px-2.5 py-1 text-xs text-slate-600 hover:border-indigo-300 hover:text-indigo-600"
              type="button"
              @click="goSearch(item)"
            >
              <span class="max-w-[9rem] truncate">{{ item }}</span>
              <span class="text-slate-400 hover:text-rose-500" @click.stop="dropHistory(item)"><X class="size-3" /></span>
            </button>
          </div>
        </div>
      </div>

      <div class="navbar-end gap-1">
        <ThemeToggle />

        <RouterLink v-if="session.loggedIn" to="/messages" class="btn btn-ghost btn-circle btn-sm relative" title="消息通知">
          <Bell class="size-5" />
          <span v-if="session.notificationCount > 0" class="badge badge-error badge-xs absolute -top-0.5 -right-0.5 border-0 text-white">
            {{ badge }}
          </span>
        </RouterLink>

        <RouterLink v-if="session.loggedIn" to="/publish" class="btn btn-primary btn-sm hidden sm:inline-flex">
          <SquarePen class="size-4" />
          发起讨论
        </RouterLink>

        <div v-if="session.loggedIn" class="dropdown dropdown-end">
          <button tabindex="0" class="btn btn-ghost btn-circle avatar btn-sm" aria-label="用户菜单">
            <Avatar :src="session.user?.headUrl" :name="session.user?.userName" :size="32" />
          </button>
          <ul tabindex="0" class="menu dropdown-content menu-sm z-50 mt-2 w-52 rounded-box border border-slate-200 bg-white p-2 shadow-lg">
            <li class="menu-title px-2 py-1 truncate">
              {{ session.user?.userName }}
              <span v-if="session.power === 2" class="badge badge-warning badge-xs ml-1">VIP</span>
            </li>
            <li><RouterLink :to="`/user/${session.user?.userId}`"><UserRound class="size-4" />我的主页</RouterLink></li>
            <li><RouterLink to="/messages"><Bell class="size-4" />消息通知</RouterLink></li>
            <li><RouterLink to="/settings"><Settings class="size-4" />个人设置</RouterLink></li>
            <li v-if="session.isAdmin"><RouterLink to="/admin/dashboard"><ShieldCheck class="size-4" />后台管理</RouterLink></li>
            <li><button type="button" @click="logout"><LogOut class="size-4" />退出登录</button></li>
          </ul>
        </div>

        <template v-else>
          <RouterLink to="/sign-in" class="btn btn-ghost btn-sm">登录</RouterLink>
          <RouterLink to="/sign-up" class="btn btn-primary btn-sm">注册</RouterLink>
        </template>
      </div>
    </div>

    <!-- 小屏导航 -->
    <nav class="page-container flex gap-1 overflow-x-auto pb-2 lg:hidden">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="btn btn-ghost btn-xs shrink-0"
        :class="active(item.to) ? 'text-indigo-600 bg-indigo-50' : ''"
      >
        {{ item.label }}
      </RouterLink>
    </nav>
  </header>
</template>
