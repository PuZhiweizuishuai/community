<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  Hash,
  House,
  LogOut,
  MessageCircle,
  MessageCircleMore,
  Search,
  Settings,
  ShieldCheck,
  SquarePen,
  UserRound,
  X,
} from '@lucide/vue'
import { addHistory, clearHistory, getHistory, removeHistory } from '@/utils/searchHistory'
import { useSessionStore } from '@/stores/session'
import { useMetaStore } from '@/stores/meta'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/Avatar.vue'
import SiteLogo from '@/components/SiteLogo.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const meta = useMetaStore()
const toast = useToastStore()

const keyword = ref(typeof route.params.keyword === 'string' ? route.params.keyword : '')
const navItems = computed(() => [
  { label: '首页', to: '/' },
  { label: '话题', to: '/topics' },
  { label: '用户', to: '/users' },
  ...(meta.chatEnabled ? [{ label: '聊天室', to: '/chat' }] : []),
  { label: '关于', to: '/about' },
])

const active = (to) => (to === '/' ? route.path === '/' : route.path.startsWith(to))

const history = ref(getHistory())
const historyOpen = ref(false)
const mobileSearchOpen = ref(false)
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
  mobileSearchOpen.value = false
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
watch(
  () => route.fullPath,
  () => {
    historyOpen.value = false
    mobileSearchOpen.value = false
    if (typeof route.params.keyword === 'string') keyword.value = route.params.keyword
  },
)

async function logout() {
  await session.logout()
  toast.success('已退出登录')
  router.push('/')
}

const badge = computed(() => (session.notificationCount > 99 ? '99+' : session.notificationCount))
// 未登录时保留「发起」入口（点击后去登录）；已登录但无权限或被封禁时隐藏
const canPublish = computed(
  () => !session.loggedIn || (session.can('content:question:publish') && !session.banned),
)

/** 移动端底部导航：主操作「发起」居中凸起，桌面端改用顶部横向导航 */
const bottomTabs = computed(() => [
  { label: '首页', to: '/', icon: House, active: active('/') },
  { label: '话题', to: '/topics', icon: Hash, active: active('/topics') },
  ...(meta.chatEnabled ? [{ label: '聊天室', to: '/chat', icon: MessageCircle, active: active('/chat') }] : []),
  ...(canPublish.value ? [{ label: '发起', to: '/publish', icon: SquarePen, primary: true, active: active('/publish') }] : []),
  {
    label: session.loggedIn ? '我的' : '登录',
    to: session.loggedIn ? `/user/${session.user?.userId}` : '/sign-in',
    icon: UserRound,
    active:
      route.path.startsWith('/user/') ||
      route.path.startsWith('/settings') ||
      route.path.startsWith('/messages'),
  },
])
</script>

<template>
  <header class="ui-header sticky top-0 z-40 border-b">
    <div class="page-container navbar min-h-16 gap-2 px-0">
      <div class="navbar-start gap-2">
        <RouterLink to="/" class="flex items-center gap-2 text-lg font-bold ui-text-brand">
          <SiteLogo :size="34" rounded="0.7rem" />
          <span class="hidden sm:inline">不挂高数</span>
        </RouterLink>
        <nav class="ml-4 hidden items-center gap-1 lg:flex">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="btn btn-ghost btn-sm font-normal"
            :class="active(item.to) ? 'ui-text-brand ui-bg-brand' : 'ui-text-base'"
          >
            {{ item.label }}
          </RouterLink>
        </nav>
      </div>

      <div class="navbar-center relative">
        <form class="join hidden w-full max-w-xs md:flex" @submit.prevent="doSearch">
          <input
            v-model="keyword"
            class="input input-bordered input-sm join-item w-full focus:outline-none"
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
          class="theme-menu absolute left-0 top-full z-50 mt-1 hidden w-[19rem] rounded-box border p-3 md:block"
          @mousedown.prevent
        >
          <div class="mb-2 flex items-center justify-between">
            <span class="text-xs font-medium ui-text-base">搜索历史</span>
            <button class="btn btn-ghost btn-xs" type="button" @click="wipeHistory">清空</button>
          </div>
          <div class="flex flex-wrap gap-1.5">
            <button
              v-for="item in history"
              :key="item"
              class="chip group max-w-full"
              type="button"
              @click="goSearch(item)"
            >
              <span class="max-w-[9rem] truncate">{{ item }}</span>
              <span class="ui-text-muted hover:text-rose-500" @click.stop="dropHistory(item)"><X class="size-3" /></span>
            </button>
          </div>
        </div>
      </div>

      <div class="navbar-end gap-0.5 sm:gap-1">
        <button
          class="btn btn-ghost btn-circle btn-sm md:hidden"
          type="button"
          :aria-label="mobileSearchOpen ? '关闭搜索' : '打开搜索'"
          @click="mobileSearchOpen = !mobileSearchOpen"
        >
          <X v-if="mobileSearchOpen" class="size-5" />
          <Search v-else class="size-5" />
        </button>
        <ThemeToggle />

        <RouterLink v-if="session.loggedIn" to="/messages" class="btn btn-ghost btn-circle btn-sm relative" title="消息通知">
          <Bell class="size-5" />
          <span v-if="session.notificationCount > 0" class="badge badge-error badge-xs absolute -top-0.5 -right-0.5 border-0 text-white">
            {{ badge }}
          </span>
        </RouterLink>

        <RouterLink v-if="session.loggedIn && canPublish" to="/publish" class="btn btn-primary btn-sm hidden lg:inline-flex">
          <SquarePen class="size-4" />
          发起讨论
        </RouterLink>

        <div v-if="session.loggedIn" class="dropdown dropdown-end">
          <button tabindex="0" class="btn btn-ghost btn-circle avatar btn-sm" aria-label="用户菜单">
            <Avatar :src="session.user?.headUrl" :name="session.user?.userName" :size="32" />
          </button>
          <ul tabindex="0" class="menu dropdown-content menu-sm z-50 mt-2 w-52 rounded-box border ui-border p-2">
            <li class="menu-title px-2 py-1 truncate">
              {{ session.user?.userName }}
              <span v-if="session.power === 2" class="badge badge-warning badge-xs ml-1">VIP</span>
              <span v-if="session.banned" class="badge badge-error badge-xs ml-1">已封禁</span>
            </li>
            <li><RouterLink :to="`/user/${session.user?.userId}`"><UserRound class="size-4" />我的主页</RouterLink></li>
            <li><RouterLink to="/messages"><Bell class="size-4" />消息通知</RouterLink></li>
            <li v-if="meta.chatEnabled"><RouterLink to="/chat/room"><MessageCircleMore class="size-4" />聊天室</RouterLink></li>
            <li><RouterLink to="/settings"><Settings class="size-4" />个人设置</RouterLink></li>
            <li v-if="session.isAdmin"><RouterLink to="/admin/dashboard"><ShieldCheck class="size-4" />后台管理</RouterLink></li>
            <li><button type="button" @click="logout"><LogOut class="size-4" />退出登录</button></li>
          </ul>
        </div>

        <template v-else>
          <RouterLink to="/sign-in" class="btn btn-ghost btn-sm px-2 sm:px-3">登录</RouterLink>
          <RouterLink to="/sign-up" class="btn btn-primary btn-sm hidden sm:inline-flex">注册</RouterLink>
        </template>
      </div>
    </div>

    <!-- 移动端搜索面板：小屏也能搜索并复用历史记录 -->
    <Transition name="fade">
      <div v-if="mobileSearchOpen" class="page-container pb-3 md:hidden">
        <form class="join w-full" @submit.prevent="doSearch">
          <input
            v-model="keyword"
            class="input input-bordered join-item w-full min-w-0 focus:outline-none"
            placeholder="搜索问题、用户…"
            aria-label="站内搜索"
            autocomplete="off"
          />
          <button class="btn btn-primary join-item px-3" type="submit"><Search class="size-4" /><span class="hidden min-[380px]:inline">搜索</span></button>
        </form>
        <div v-if="history.length" class="mt-2 flex flex-wrap items-center gap-1.5">
          <span class="text-xs ui-text-muted">搜索历史</span>
          <button v-for="item in history" :key="item" class="chip max-w-full" type="button" @click="goSearch(item)">
            <span class="max-w-[8rem] truncate">{{ item }}</span>
            <span class="ui-text-muted" @click.stop="dropHistory(item)"><X class="size-3" /></span>
          </button>
          <button class="btn btn-ghost btn-xs" type="button" @click="wipeHistory">清空</button>
        </div>
      </div>
    </Transition>
  </header>

  <!-- 移动端底部导航：固定且带安全区，桌面端隐藏 -->
  <nav class="mobile-tabbar fixed inset-x-0 bottom-0 z-40 border-t lg:hidden">
    <div class="mx-auto flex max-w-lg items-stretch justify-around px-1 pt-1.5 pb-1">
      <RouterLink
        v-for="tab in bottomTabs"
        :key="tab.label"
        :to="tab.to"
        class="flex min-h-[3.25rem] min-w-0 flex-1 flex-col items-center justify-center gap-0.5 rounded-xl px-1 text-[11px] font-medium transition"
        :class="tab.primary ? 'ui-text-brand' : tab.active ? 'ui-text-brand' : 'ui-text-muted'"
      >
        <span v-if="tab.primary" class="tab-fab -mt-6 grid size-12 shrink-0 place-items-center rounded-full">
          <component :is="tab.icon" class="size-5" />
        </span>
        <component v-else :is="tab.icon" class="size-5" :stroke-width="tab.active ? 2.3 : 1.8" />
        <span>{{ tab.label }}</span>
      </RouterLink>
    </div>
  </nav>
</template>
