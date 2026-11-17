<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  FolderOpen,
  HardDrive,
  Home,
  LayoutDashboard,
  ListTree,
  LogOut,
  Megaphone,
  Menu,
  MessageCircleQuestion,
  Search,
  Settings,
  Tags,
  Users,
  X,
} from '@lucide/vue'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/Avatar.vue'
import SiteLogo from '@/components/SiteLogo.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const toast = useToastStore()
const search = ref('')
const menuOpen = ref(false)

const menus = [
  { label: '数据概览', to: '/admin/dashboard', icon: LayoutDashboard },
  { label: '问题管理', to: '/admin/questions', icon: MessageCircleQuestion },
  { label: '用户管理', to: '/admin/users', icon: Users },
  { label: '站内搜索', to: '/admin/search', icon: Search },
  { label: '控制面板', to: '/admin/controller', icon: Settings },
  { label: '存储配置', to: '/admin/storage', icon: HardDrive },
  { label: '文件管理', to: '/admin/files', icon: FolderOpen },
  { label: '话题管理', to: '/admin/topics', icon: Tags },
  { label: '分类管理', to: '/admin/classifications', icon: ListTree },
  { label: '广告与新闻', to: '/admin/advertisements', icon: Megaphone },
]

watch(() => route.fullPath, () => (menuOpen.value = false))

function doSearch() {
  router.push({ name: 'admin-search', query: search.value ? { search: search.value } : {} })
}

async function logout() {
  await session.adminLogout()
  toast.success('已退出后台')
  router.push('/admin/login')
}
</script>

<template>
  <div class="flex min-h-screen ui-bg-page">
    <!-- 侧栏：桌面常驻，移动端变为抽屉，同一份 DOM 保证两套布局完全一致 -->
    <div v-if="menuOpen" class="fixed inset-0 z-50 bg-slate-900/50 md:hidden" @click="menuOpen = false" />
    <aside class="admin-sidebar" :class="{ 'is-open': menuOpen }">
      <div class="flex h-14 shrink-0 items-center gap-2 border-b border-white/10 px-4 text-sm font-bold text-white sm:h-16 sm:px-5 sm:text-base">
        <RouterLink to="/" class="flex items-center gap-2">
          <SiteLogo :size="32" rounded="0.7rem" />
          社区管理后台
        </RouterLink>
        <button class="btn btn-ghost btn-circle btn-sm ml-auto text-white md:hidden" type="button" aria-label="关闭菜单" @click="menuOpen = false">
          <X class="size-4" />
        </button>
      </div>
      <nav class="flex-1 space-y-1 overflow-y-auto p-3">
        <RouterLink
          v-for="item in menus"
          :key="item.to"
          :to="item.to"
          class="admin-nav-link flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm"
          :class="{ 'is-active': route.path === item.to }"
          @click="menuOpen = false"
        >
          <component :is="item.icon" class="size-4" />
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="border-t border-white/10 p-4 text-xs text-slate-400">
        <RouterLink to="/" class="flex items-center gap-1.5 hover:text-white"><ArrowLeft class="size-3.5" />返回前台</RouterLink>
      </div>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="ui-header sticky top-0 z-30 flex h-14 items-center justify-between gap-2 border-b px-3 sm:h-16 sm:px-4 lg:px-6">
        <div class="flex min-w-0 items-center gap-2">
          <button class="btn btn-ghost btn-circle btn-sm md:hidden" type="button" aria-label="打开菜单" @click="menuOpen = true">
            <Menu class="size-5" />
          </button>
          <h1 class="truncate text-sm font-semibold ui-text-strong sm:text-base">{{ route.meta.title || '管理后台' }}</h1>
        </div>
        <div class="flex items-center gap-1 sm:gap-2">
          <form class="join hidden md:flex" @submit.prevent="doSearch">
            <input v-model="search" class="input input-bordered input-sm join-item w-44 lg:w-56" placeholder="搜索问题 / 用户" />
            <button class="btn btn-sm btn-primary join-item" type="submit"><Search class="size-4" />搜索</button>
          </form>
          <RouterLink to="/admin/search" class="btn btn-ghost btn-circle btn-sm md:hidden" aria-label="站内搜索"><Search class="size-5" /></RouterLink>
          <ThemeToggle />

          <div class="dropdown dropdown-end">
            <button tabindex="0" class="btn btn-ghost btn-sm gap-2 px-1.5 sm:px-3">
              <Avatar :src="session.admin?.headUrl" :name="session.admin?.userName" :size="26" />
              <span class="hidden sm:inline">{{ session.admin?.userName }}</span>
            </button>
            <ul tabindex="0" class="menu dropdown-content menu-sm z-50 mt-2 w-40 rounded-box border ui-border p-2">
              <li><RouterLink to="/"><Home class="size-4" />返回前台</RouterLink></li>
              <li><button type="button" @click="logout"><LogOut class="size-4" />退出后台</button></li>
            </ul>
          </div>
        </div>
      </header>

      <main class="min-w-0 flex-1 p-3 sm:p-4 lg:p-6">
        <slot />
      </main>

      <footer class="border-t ui-border ui-bg px-4 py-3 text-center text-xs ui-text-muted">
        不挂高数 · 社区管理后台
      </footer>
    </div>
  </div>
</template>
