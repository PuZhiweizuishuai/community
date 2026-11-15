<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Home,
  LayoutDashboard,
  ListTree,
  LogOut,
  Megaphone,
  MessageCircleQuestion,
  Search,
  Settings,
  Tags,
  Users,
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

const menus = [
  { label: '数据概览', to: '/admin/dashboard', icon: LayoutDashboard },
  { label: '问题管理', to: '/admin/questions', icon: MessageCircleQuestion },
  { label: '用户管理', to: '/admin/users', icon: Users },
  { label: '站内搜索', to: '/admin/search', icon: Search },
  { label: '控制面板', to: '/admin/controller', icon: Settings },
  { label: '话题管理', to: '/admin/topics', icon: Tags },
  { label: '分类管理', to: '/admin/classifications', icon: ListTree },
  { label: '广告与新闻', to: '/admin/advertisements', icon: Megaphone },
]

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
  <div class="flex min-h-screen bg-slate-50">
    <aside class="hidden w-60 shrink-0 flex-col bg-slate-900 text-slate-200 md:flex">
      <RouterLink to="/" class="flex h-16 items-center gap-2 border-b border-white/10 px-5 text-base font-bold text-white">
        <SiteLogo :size="32" rounded="0.55rem" />
        社区管理后台
      </RouterLink>
      <nav class="flex-1 space-y-1 p-3">
        <RouterLink
          v-for="item in menus"
          :key="item.to"
          :to="item.to"
          class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition"
          :class="route.path === item.to ? 'bg-indigo-600 text-white' : 'hover:bg-white/10'"
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
      <header class="flex h-16 items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 lg:px-6">
        <h1 class="truncate text-base font-semibold text-slate-700">{{ route.meta.title || '管理后台' }}</h1>
        <div class="flex items-center gap-2">
          <form class="join hidden sm:flex" @submit.prevent="doSearch">
            <input v-model="search" class="input input-bordered input-sm join-item" placeholder="搜索问题 / 用户" />
            <button class="btn btn-sm btn-primary join-item" type="submit"><Search class="size-4" />搜索</button>
          </form>
          <ThemeToggle />

          <div class="dropdown dropdown-end">
            <button tabindex="0" class="btn btn-ghost btn-sm gap-2">
              <Avatar :src="session.admin?.headUrl" :name="session.admin?.userName" :size="26" />
              <span class="hidden sm:inline">{{ session.admin?.userName }}</span>
            </button>
            <ul tabindex="0" class="menu dropdown-content menu-sm z-50 mt-2 w-40 rounded-box border border-slate-200 bg-white p-2 shadow-lg">
              <li><RouterLink to="/"><Home class="size-4" />返回前台</RouterLink></li>
              <li><button type="button" @click="logout"><LogOut class="size-4" />退出后台</button></li>
            </ul>
          </div>
        </div>
      </header>

      <div class="border-b border-slate-200 bg-white px-4 py-2 md:hidden">
        <div class="flex gap-1 overflow-x-auto">
          <RouterLink v-for="item in menus" :key="item.to" :to="item.to" class="btn btn-xs shrink-0" :class="route.path === item.to ? 'btn-primary' : 'btn-ghost'">
            <component :is="item.icon" class="size-3.5" />
            {{ item.label }}
          </RouterLink>
        </div>
      </div>

      <main class="flex-1 p-4 lg:p-6">
        <slot />
      </main>

      <footer class="border-t border-slate-200 bg-white px-6 py-3 text-center text-xs text-slate-400">
        不挂高数 · 社区管理后台
      </footer>
    </div>
  </div>
</template>
