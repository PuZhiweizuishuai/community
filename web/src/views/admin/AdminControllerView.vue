<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import {
  Activity,
  Clock,
  Database,
  Download,
  Eye,
  FileText,
  Gauge,
  House,
  Inbox,
  Layers,
  ListTree,
  MessageCircleQuestion,
  Pin,
  RefreshCw,
  ScrollText,
  Settings,
  Tags,
  Trash2,
  TriangleAlert,
  Users,
  X,
  Zap,
} from '@lucide/vue'
import { adminApi } from '@/api'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const tab = ref('cache')
const topIds = ref(['', '', ''])
const logs = ref([])
const cache = ref(null)
const loading = ref(false)
const busyRefresh = ref(null)
const busyPrefix = ref(null)
const clearing = ref(false)
const savingTop = ref(false)
const deleting = ref(false)
const deleteTarget = ref(null)
const clearConfirm = ref(false)
const password = ref('')
const autoRefresh = ref(false)
let autoTimer = null

const tabs = [
  { key: 'cache', label: '缓存与统计', icon: Database },
  { key: 'top', label: '首页置顶', icon: Pin },
  { key: 'logs', label: '系统日志', icon: ScrollText },
]

const refreshActions = [
  { name: 'User', label: '活跃用户', desc: '重算活跃用户榜单', icon: Users, grad: 'from-indigo-500 to-blue-500' },
  { name: 'Tag', label: '热门话题', desc: '重算热门话题榜单', icon: Tags, grad: 'from-violet-500 to-purple-500' },
  { name: 'Question', label: '热门问题', desc: '重算热门问题榜单', icon: MessageCircleQuestion, grad: 'from-cyan-500 to-teal-500' },
  { name: 'TagClass', label: '话题分类', desc: '同步前台分类缓存', icon: ListTree, grad: 'from-amber-500 to-orange-500' },
]

const prefixMeta = [
  { prefix: 'meta:', label: '元数据', desc: '热门标签 / 问题 / 在线人数', icon: Layers, grad: 'from-indigo-500 to-violet-500', badge: 'badge-primary' },
  { prefix: 'home:', label: '首页列表', desc: '首页问题分页缓存', icon: House, grad: 'from-cyan-500 to-sky-500', badge: 'badge-info' },
  { prefix: 'topics:', label: '话题', desc: '话题列表与详情', icon: Tags, grad: 'from-violet-500 to-fuchsia-500', badge: 'badge-secondary' },
  { prefix: 'classification:', label: '动态分类', desc: '问题动态分类', icon: ListTree, grad: 'from-amber-500 to-orange-500', badge: 'badge-warning' },
]

const hitRateNum = computed(() => Number.parseFloat(cache.value?.hitRate) || 0)
const filledTop = computed(() => topIds.value.map((v) => String(v).trim()).filter((v) => v !== ''))
const entries = computed(() => cache.value?.entries || [])

function prefixOf(key) {
  return prefixMeta.find((p) => key.startsWith(p.prefix)) || null
}
function prefixCount(prefix) {
  return entries.value.filter((e) => e.key.startsWith(prefix)).length
}

async function loadLogs() {
  const result = await adminApi.controller()
  logs.value = result.logs || []
}
async function loadCache() {
  cache.value = await adminApi.cacheStats()
}
async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadLogs(), loadCache()])
  } finally {
    loading.value = false
  }
}

async function refresh(name) {
  const label = { User: '活跃用户', Tag: '热门话题', Question: '热门问题', TagClass: '话题分类' }[name] || name
  busyRefresh.value = name
  try {
    if (name === 'TagClass') await adminApi.refreshTagClass()
    else await adminApi.refreshCache(name)
    toast.success(`${label}已更新`)
    await loadCache()
  } finally {
    busyRefresh.value = null
  }
}

async function invalidatePrefix(prefix) {
  busyPrefix.value = prefix
  try {
    await adminApi.invalidateCache(prefix)
    toast.success(`已失效「${prefix}」前缀缓存`)
    await loadCache()
  } finally {
    busyPrefix.value = null
  }
}

async function clearCache() {
  clearing.value = true
  try {
    await adminApi.refreshCacheAll()
    toast.success('全站缓存已清空')
    clearConfirm.value = false
    await loadCache()
  } finally {
    clearing.value = false
  }
}

async function saveTop() {
  const ids = filledTop.value.map(Number)
  if (!ids.length) return toast.warning('请至少填写一个问题 ID')
  if (ids.some((n) => !Number.isFinite(n) || n < 0)) return toast.warning('问题 ID 需为非负整数')
  savingTop.value = true
  try {
    await adminApi.settingTop(ids)
    toast.success('置顶设置成功')
  } finally {
    savingTop.value = false
  }
}

function openDelete(log) {
  deleteTarget.value = log
  password.value = ''
}
async function confirmDelete() {
  if (!password.value) return toast.warning('请输入管理员密码')
  deleting.value = true
  try {
    await adminApi.deleteLog({ title: deleteTarget.value.title, password: password.value })
    toast.success('日志已删除')
    deleteTarget.value = null
    password.value = ''
    await loadLogs()
  } finally {
    deleting.value = false
  }
}

watch(autoRefresh, (on) => {
  clearInterval(autoTimer)
  autoTimer = null
  if (on) autoTimer = setInterval(() => loadCache().catch(() => {}), 5000)
})
onBeforeUnmount(() => clearInterval(autoTimer))

loadAll()
</script>

<template>
  <div class="space-y-5">
    <!-- 页头 -->
    <div class="surface relative overflow-hidden p-5 sm:p-6">
      <div class="pointer-events-none absolute -right-20 -top-24 size-56 rounded-full bg-gradient-to-br from-indigo-500/15 to-violet-500/10 blur-2xl" />
      <div class="pointer-events-none absolute -bottom-24 left-1/3 size-48 rounded-full bg-gradient-to-tr from-cyan-500/10 to-transparent blur-2xl" />
      <div class="relative flex flex-wrap items-start justify-between gap-4">
        <div class="flex items-start gap-3">
          <span class="grid size-11 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-500 text-white shadow-lg shadow-indigo-500/25">
            <Settings class="size-5" />
          </span>
          <div>
            <h1 class="text-lg font-bold text-slate-800">控制面板</h1>
            <p class="mt-0.5 text-sm text-slate-500">首页置顶、缓存观测与系统日志的统一管理入口</p>
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <label class="flex cursor-pointer select-none items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium text-slate-600">
            <span class="relative flex size-2">
              <span v-if="autoRefresh" class="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
              <span class="relative inline-flex size-2 rounded-full" :class="autoRefresh ? 'bg-emerald-500' : 'bg-slate-300'" />
            </span>
            自动刷新
            <input v-model="autoRefresh" type="checkbox" class="toggle toggle-xs toggle-success" />
          </label>
          <button class="btn btn-primary btn-sm" type="button" :disabled="loading" @click="loadAll">
            <RefreshCw class="size-4" :class="loading ? 'animate-spin' : ''" />刷新全部
          </button>
        </div>
      </div>
    </div>

    <!-- 概览指标 -->
    <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      <div class="surface ctrl-rise group relative overflow-hidden p-4" style="animation-delay: 0ms">
        <div class="flex items-center justify-between gap-3">
          <div class="min-w-0">
            <p class="text-xs font-medium text-slate-400">缓存条目</p>
            <p class="mt-1.5 text-2xl font-bold text-slate-800">{{ cache?.size ?? '—' }}</p>
            <p class="mt-1 truncate text-xs text-slate-400">本地 TTL 内存缓存</p>
          </div>
          <span class="grid size-12 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-500 text-white shadow-lg shadow-indigo-500/20 transition group-hover:scale-105">
            <Database class="size-6" />
          </span>
        </div>
      </div>
      <div class="surface ctrl-rise group relative overflow-hidden p-4" style="animation-delay: 60ms">
        <div class="flex items-center justify-between gap-3">
          <div class="min-w-0">
            <p class="text-xs font-medium text-slate-400">缓存命中率</p>
            <p class="mt-1.5 text-2xl font-bold text-emerald-600">{{ cache?.hitRate ?? '—' }}</p>
            <p class="mt-1 truncate text-xs text-slate-400">命中 {{ cache?.hits ?? 0 }} / 未命中 {{ cache?.misses ?? 0 }}</p>
          </div>
          <span class="grid size-12 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-emerald-500 to-teal-500 text-white shadow-lg shadow-emerald-500/20 transition group-hover:scale-105">
            <Gauge class="size-6" />
          </span>
        </div>
      </div>
      <div class="surface ctrl-rise group relative overflow-hidden p-4" style="animation-delay: 120ms">
        <div class="flex items-center justify-between gap-3">
          <div class="min-w-0">
            <p class="text-xs font-medium text-slate-400">日志文件</p>
            <p class="mt-1.5 text-2xl font-bold text-slate-800">{{ logs.length }}</p>
            <p class="mt-1 truncate text-xs text-slate-400">可查看 / 下载 / 清理</p>
          </div>
          <span class="grid size-12 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-cyan-500 to-sky-500 text-white shadow-lg shadow-cyan-500/20 transition group-hover:scale-105">
            <ScrollText class="size-6" />
          </span>
        </div>
      </div>
      <div class="surface ctrl-rise group relative overflow-hidden p-4" style="animation-delay: 180ms">
        <div class="flex items-center justify-between gap-3">
          <div class="min-w-0">
            <p class="text-xs font-medium text-slate-400">首页置顶</p>
            <p class="mt-1.5 text-2xl font-bold text-slate-800">{{ filledTop.length }}<span class="text-base font-medium text-slate-400"> / 3</span></p>
            <p class="mt-1 truncate text-xs text-slate-400">按填写顺序展示</p>
          </div>
          <span class="grid size-12 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-violet-500 to-fuchsia-500 text-white shadow-lg shadow-violet-500/20 transition group-hover:scale-105">
            <Pin class="size-6" />
          </span>
        </div>
      </div>
    </div>

    <!-- 分段导航 -->
    <div class="surface p-1.5">
      <div class="flex gap-1 overflow-x-auto">
        <button
          v-for="t in tabs"
          :key="t.key"
          class="flex shrink-0 items-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition"
          :class="tab === t.key ? 'bg-gradient-to-br from-indigo-500 to-violet-500 text-white shadow-md shadow-indigo-500/25' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'"
          type="button"
          @click="tab = t.key"
        >
          <component :is="t.icon" class="size-4" />
          {{ t.label }}
          <span v-if="t.key === 'logs' && logs.length" class="rounded-full px-1.5 text-xs" :class="tab === t.key ? 'bg-white/25' : 'bg-slate-200 text-slate-500'">{{ logs.length }}</span>
          <span v-if="t.key === 'cache' && cache" class="rounded-full px-1.5 text-xs" :class="tab === t.key ? 'bg-white/25' : 'bg-slate-200 text-slate-500'">{{ cache.size }}</span>
        </button>
      </div>
    </div>

    <!-- 面板 -->
    <Transition name="fade" mode="out-in">
      <div :key="tab">
        <!-- 缓存与统计 -->
        <div v-if="tab === 'cache'" class="space-y-5">
          <section class="surface p-5">
            <div class="mb-4 flex items-center gap-2.5">
              <span class="grid size-8 place-items-center rounded-lg bg-gradient-to-br from-indigo-500 to-blue-500 text-white"><Zap class="size-4" /></span>
              <div>
                <h2 class="text-sm font-semibold text-slate-700">榜单重算</h2>
                <p class="text-xs text-slate-400">手动重算后会自动失效对应的页面缓存</p>
              </div>
            </div>
            <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <button
                v-for="action in refreshActions"
                :key="action.name"
                class="group flex items-center gap-3 rounded-xl border border-slate-200 p-3 text-left transition hover:border-indigo-300 hover:bg-slate-50 disabled:opacity-60"
                type="button"
                :disabled="busyRefresh !== null"
                @click="refresh(action.name)"
              >
                <span class="grid size-10 shrink-0 place-items-center rounded-lg bg-gradient-to-br text-white transition group-hover:scale-105" :class="action.grad">
                  <RefreshCw v-if="busyRefresh === action.name" class="size-5 animate-spin" />
                  <component :is="action.icon" v-else class="size-5" />
                </span>
                <span class="min-w-0">
                  <span class="block text-sm font-semibold text-slate-700">{{ action.label }}</span>
                  <span class="block truncate text-xs text-slate-400">{{ busyRefresh === action.name ? '重算中…' : action.desc }}</span>
                </span>
              </button>
            </div>
          </section>

          <section class="surface p-5">
            <div class="mb-4 flex flex-wrap items-center gap-2.5">
              <span class="grid size-8 place-items-center rounded-lg bg-gradient-to-br from-emerald-500 to-teal-500 text-white"><Activity class="size-4" /></span>
              <div class="mr-auto">
                <h2 class="text-sm font-semibold text-slate-700">本地 TTL 缓存</h2>
                <p class="text-xs text-slate-400">读多写少的数据统一走内存缓存，写操作主动失效</p>
              </div>
              <button class="btn btn-ghost btn-xs" type="button" @click="loadCache"><RefreshCw class="size-3.5" />刷新状态</button>
              <button class="btn btn-outline btn-xs text-rose-500" type="button" @click="clearConfirm = true"><Trash2 class="size-3.5" />清空全部</button>
            </div>

            <div class="grid gap-3 sm:grid-cols-4">
              <div class="rounded-xl bg-slate-50 p-3">
                <p class="flex items-center gap-1.5 text-xs text-slate-400"><Database class="size-3.5" />缓存条目</p>
                <p class="mt-1 text-xl font-bold text-slate-800">{{ cache?.size ?? 0 }}</p>
              </div>
              <div class="rounded-xl bg-slate-50 p-3">
                <p class="flex items-center gap-1.5 text-xs text-slate-400"><Gauge class="size-3.5" />命中</p>
                <p class="mt-1 text-xl font-bold text-emerald-600">{{ cache?.hits ?? 0 }}</p>
              </div>
              <div class="rounded-xl bg-slate-50 p-3">
                <p class="flex items-center gap-1.5 text-xs text-slate-400"><TriangleAlert class="size-3.5" />未命中</p>
                <p class="mt-1 text-xl font-bold text-amber-600">{{ cache?.misses ?? 0 }}</p>
              </div>
              <div class="rounded-xl bg-slate-50 p-3">
                <p class="flex items-center gap-1.5 text-xs text-slate-400"><Activity class="size-3.5" />命中率</p>
                <p class="mt-1 text-xl font-bold text-indigo-600">{{ cache?.hitRate ?? '0%' }}</p>
              </div>
            </div>
            <div class="mt-3 flex items-center gap-3">
              <progress class="progress progress-success h-2 flex-1" :value="hitRateNum" max="100" />
              <span class="text-xs font-medium text-slate-500">{{ hitRateNum.toFixed(1) }}%</span>
            </div>

            <!-- 按前缀失效 -->
            <div class="mt-5">
              <h3 class="mb-2.5 text-xs font-semibold uppercase tracking-wide text-slate-400">按业务前缀失效</h3>
              <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <div v-for="p in prefixMeta" :key="p.prefix" class="rounded-xl border border-slate-200 p-3">
                  <div class="flex items-center gap-2.5">
                    <span class="grid size-9 shrink-0 place-items-center rounded-lg bg-gradient-to-br text-white" :class="p.grad"><component :is="p.icon" class="size-4" /></span>
                    <div class="min-w-0 flex-1">
                      <p class="truncate text-sm font-semibold text-slate-700">{{ p.label }}</p>
                      <p class="truncate text-xs text-slate-400">{{ prefixCount(p.prefix) }} 项 · {{ p.desc }}</p>
                    </div>
                  </div>
                  <button
                    class="btn btn-outline btn-xs mt-2.5 w-full"
                    type="button"
                    :disabled="busyPrefix !== null"
                    @click="invalidatePrefix(p.prefix)"
                  >
                    <RefreshCw class="size-3.5" :class="busyPrefix === p.prefix ? 'animate-spin' : ''" />
                    {{ busyPrefix === p.prefix ? '失效中…' : '失效该前缀' }}
                  </button>
                </div>
              </div>
            </div>

            <!-- 缓存条目明细 -->
            <div class="mt-5">
              <div class="mb-2.5 flex items-center justify-between">
                <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-400">缓存条目明细</h3>
                <span class="text-xs text-slate-400">{{ entries.length }} 项</span>
              </div>
              <div v-if="entries.length" class="controller-scroll max-h-72 overflow-y-auto rounded-xl border border-slate-200">
                <table class="table table-sm">
                  <thead class="sticky top-0 z-10 bg-slate-50">
                    <tr><th>键名</th><th>前缀</th><th>剩余 TTL</th><th>状态</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="entry in entries" :key="entry.key">
                      <td class="max-w-[260px]"><span class="block truncate font-mono text-xs text-slate-600">{{ entry.key }}</span></td>
                      <td>
                        <span class="badge badge-sm" :class="prefixOf(entry.key)?.badge || 'badge-ghost'">{{ prefixOf(entry.key)?.label || '其它' }}</span>
                      </td>
                      <td class="whitespace-nowrap text-xs text-slate-500"><Clock class="mr-1 inline size-3.5" />{{ entry.ttlSeconds }}s</td>
                      <td>
                        <span class="badge badge-sm" :class="entry.expired ? 'badge-error' : 'badge-success'">{{ entry.expired ? '已过期' : '生效中' }}</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-else class="flex flex-col items-center rounded-xl border border-dashed border-slate-200 py-8 text-center text-sm text-slate-400">
                <Inbox class="size-8 text-slate-300" :stroke-width="1.5" />
                <p class="mt-2">暂无缓存条目</p>
              </div>
            </div>
          </section>
        </div>

        <!-- 首页置顶 -->
        <div v-else-if="tab === 'top'" class="grid gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
          <section class="surface p-5">
            <div class="mb-4 flex items-center gap-2.5">
              <span class="grid size-8 place-items-center rounded-lg bg-gradient-to-br from-violet-500 to-fuchsia-500 text-white"><Pin class="size-4" /></span>
              <div>
                <h2 class="text-sm font-semibold text-slate-700">首页置顶问题</h2>
                <p class="text-xs text-slate-400">填写问题 ID，按顺序展示在首页顶部，留空则忽略</p>
              </div>
            </div>
            <div class="space-y-3">
              <label v-for="(value, index) in topIds" :key="index" class="flex items-center gap-3">
                <span class="grid size-9 shrink-0 place-items-center rounded-lg bg-gradient-to-br from-indigo-500 to-violet-500 text-sm font-bold text-white">{{ index + 1 }}</span>
                <span class="min-w-0 flex-1">
                  <span class="label-text mb-1 block text-xs font-medium text-slate-600">置顶问题 {{ index + 1 }}</span>
                  <input v-model="topIds[index]" type="number" min="0" class="input input-bordered input-sm w-full" :placeholder="`输入问题 ID ${index + 1}`" />
                </span>
              </label>
            </div>
            <div class="mt-5 flex flex-wrap items-center gap-2">
              <button class="btn btn-primary btn-sm" type="button" :disabled="savingTop" @click="saveTop">
                <RefreshCw v-if="savingTop" class="size-4 animate-spin" />
                <Pin v-else class="size-4" />{{ savingTop ? '保存中…' : '保存置顶' }}
              </button>
              <button class="btn btn-ghost btn-sm" type="button" @click="topIds = ['', '', '']"><X class="size-4" />清空</button>
            </div>
          </section>

          <aside class="space-y-4">
            <section class="surface p-5">
              <h3 class="mb-3 text-sm font-semibold text-slate-700">展示顺序预览</h3>
              <ol class="space-y-2">
                <li v-for="(value, index) in topIds" :key="index" class="flex items-center gap-2.5 rounded-lg border p-2.5" :class="String(value).trim() ? 'border-indigo-200 bg-slate-50' : 'border-dashed border-slate-200'">
                  <span class="grid size-6 shrink-0 place-items-center rounded-md text-xs font-bold" :class="String(value).trim() ? 'bg-indigo-500 text-white' : 'bg-slate-200 text-slate-400'">{{ index + 1 }}</span>
                  <span v-if="String(value).trim()" class="truncate font-mono text-sm text-slate-700">问题 #{{ value }}</span>
                  <span v-else class="text-xs text-slate-400">未设置</span>
                </li>
              </ol>
            </section>
            <section class="surface p-5">
              <h3 class="mb-2 text-sm font-semibold text-slate-700">小贴士</h3>
              <ul class="list-inside list-disc space-y-1.5 text-xs leading-relaxed text-slate-500">
                <li>问题 ID 可在问题详情页地址栏或后台问题管理中查看。</li>
                <li>置顶按填写顺序展示，留空的位置会被自动忽略。</li>
                <li>保存后立即生效，首页顶部即可看到置顶内容。</li>
              </ul>
            </section>
          </aside>
        </div>

        <!-- 系统日志 -->
        <section v-else class="surface overflow-hidden">
          <div class="flex flex-wrap items-center gap-2.5 border-b border-slate-100 p-5">
            <span class="grid size-8 place-items-center rounded-lg bg-gradient-to-br from-cyan-500 to-sky-500 text-white"><ScrollText class="size-4" /></span>
            <div class="mr-auto">
              <h2 class="text-sm font-semibold text-slate-700">系统日志</h2>
              <p class="text-xs text-slate-400">共 {{ logs.length }} 个日志文件，当前日志不可删除</p>
            </div>
            <button class="btn btn-ghost btn-xs" type="button" @click="loadLogs"><RefreshCw class="size-3.5" />刷新</button>
          </div>
          <div class="controller-scroll overflow-x-auto">
            <table class="table table-sm">
              <thead>
                <tr><th>日志文件</th><th>时间</th><th>状态</th><th class="text-right">操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="log in logs" :key="log.title" class="hover:bg-slate-50">
                  <td>
                    <span class="flex items-center gap-2">
                      <FileText class="size-4 shrink-0 text-slate-400" />
                      <span class="font-mono text-xs text-slate-700">{{ log.title }}</span>
                    </span>
                  </td>
                  <td class="whitespace-nowrap text-xs text-slate-500">{{ log.time }}</td>
                  <td>
                    <span class="badge badge-sm" :class="log.show ? 'badge-success' : 'badge-ghost'">{{ log.show ? '当前日志' : '历史日志' }}</span>
                  </td>
                  <td>
                    <div class="flex items-center justify-end gap-1">
                      <a :href="adminApi.logUrl(log.title, 'show')" target="_blank" rel="noopener" class="btn btn-ghost btn-xs text-indigo-600"><Eye class="size-3.5" />查看</a>
                      <a :href="adminApi.logUrl(log.title, 'download')" class="btn btn-ghost btn-xs text-indigo-600"><Download class="size-3.5" />下载</a>
                      <button v-if="!log.show" class="btn btn-ghost btn-xs text-rose-500" type="button" @click="openDelete(log)"><Trash2 class="size-3.5" />删除</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="!logs.length">
                  <td colspan="4">
                    <div class="flex flex-col items-center py-10 text-center text-sm text-slate-400">
                      <Inbox class="size-9 text-slate-300" :stroke-width="1.5" />
                      <p class="mt-2">暂无日志文件</p>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </Transition>

    <!-- 删除日志确认 -->
    <dialog v-if="deleteTarget" class="modal modal-open">
      <div class="modal-box max-w-md">
        <div class="flex items-start gap-3">
          <span class="grid size-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-rose-500 to-red-500 text-white"><Trash2 class="size-5" /></span>
          <div class="min-w-0">
            <h3 class="text-base font-semibold text-slate-800">删除日志</h3>
            <p class="mt-1 text-sm text-slate-500">确定删除 <span class="font-mono text-xs text-slate-700">{{ deleteTarget.title }}</span>？此操作不可恢复，需输入管理员密码确认。</p>
          </div>
        </div>
        <input v-model="password" type="password" class="input input-bordered mt-4 w-full" placeholder="管理员密码" @keyup.enter="confirmDelete" />
        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="deleteTarget = null">取消</button>
          <button class="btn btn-error btn-sm" type="button" :disabled="deleting" @click="confirmDelete">
            <RefreshCw v-if="deleting" class="size-4 animate-spin" />
            <Trash2 v-else class="size-4" />{{ deleting ? '删除中…' : '确认删除' }}
          </button>
        </div>
      </div>
    </dialog>

    <!-- 清空缓存确认 -->
    <dialog v-if="clearConfirm" class="modal modal-open">
      <div class="modal-box max-w-md">
        <div class="flex items-start gap-3">
          <span class="grid size-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white"><TriangleAlert class="size-5" /></span>
          <div>
            <h3 class="text-base font-semibold text-slate-800">清空全站缓存</h3>
            <p class="mt-1 text-sm text-slate-500">将清空全部 {{ cache?.size ?? 0 }} 项本地缓存，下次访问会重新查库回填，短时间内数据库压力可能上升。确定继续？</p>
          </div>
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="clearConfirm = false">取消</button>
          <button class="btn btn-warning btn-sm" type="button" :disabled="clearing" @click="clearCache">
            <RefreshCw v-if="clearing" class="size-4 animate-spin" />
            <Trash2 v-else class="size-4" />{{ clearing ? '清空中…' : '确认清空' }}
          </button>
        </div>
      </div>
    </dialog>
  </div>
</template>

<style scoped>
.ctrl-rise {
  animation: ctrl-rise 0.4s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes ctrl-rise {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* 滚动条跟随主题配色 */
.controller-scroll {
  scrollbar-width: thin;
  scrollbar-color: var(--soft-border) transparent;
}
.controller-scroll::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
.controller-scroll::-webkit-scrollbar-track {
  background: transparent;
}
.controller-scroll::-webkit-scrollbar-thumb {
  background: var(--soft-border);
  border-radius: 9999px;
}
.controller-scroll::-webkit-scrollbar-thumb:hover {
  background: var(--brand);
}

@media (prefers-reduced-motion: reduce) {
  .ctrl-rise {
    animation: none;
  }
}
</style>
