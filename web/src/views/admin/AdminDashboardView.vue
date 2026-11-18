<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  ArrowUpRight,
  ChevronRight,
  Database,
  Flag,
  FolderOpen,
  Gauge,
  ListTree,
  Mail,
  Megaphone,
  MessageCircle,
  MessageCircleQuestion,
  RefreshCw,
  ShieldCheck,
  Tags,
  UserPlus,
  UserX,
  Users,
  Wifi,
} from '@lucide/vue'
import { adminApi } from '@/api'
import { useSessionStore } from '@/stores/session'
import EChart from '@/components/EChart.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const data = ref(null)
const loading = ref(false)
const updatedAt = ref('')
const page = computed(() => route.query.page || '1')

async function load() {
  loading.value = true
  try {
    data.value = await adminApi.dashboard({ page: page.value, size: 20 })
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } finally {
    loading.value = false
  }
}
watch(page, load, { immediate: true })

const rows = computed(() => data.value?.pagination?.data || [])
const latest = computed(() => rows.value[0] || null)
const overview = computed(() => data.value?.overview || {})
const pending = computed(
  () => overview.value.pendingReview || { question: 0, comment: 0, total: 0 },
)
const system = computed(() => overview.value.system || {})
const reports = computed(
  () => overview.value.report || { pending: 0, accepted: 0, rejected: 0, total: 0 },
)
const canReview = computed(() => session.can('admin:review'))
const canManageReports = computed(() => session.can('admin:report'))
const latestTime = computed(() => latest.value?.time?.slice(5, 16) || '—')

function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '—'
  return Number(value).toLocaleString('zh-CN')
}

function formatBytes(bytes) {
  const value = Number(bytes)
  if (!Number.isFinite(value) || value <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1)
  const size = value / 1024 ** index
  return `${size >= 100 || index === 0 ? size.toFixed(0) : size.toFixed(1)} ${units[index]}`
}

function deltaHint(value) {
  const count = Number(value || 0)
  return count > 0 ? `较上次采样 +${formatNumber(count)}` : '较上次采样无新增'
}

const cards = computed(() => {
  const total = data.value || {}
  const ov = overview.value
  const items = [
    {
      label: '注册用户',
      value: total.userNumber,
      hint: latest.value ? deltaHint(latest.value.userAddCount) : '社区累计注册账号',
      icon: Users,
      grad: 'from-indigo-500 to-blue-500 shadow-indigo-500/25',
      valueClass: 'ui-text-brand',
      to: '/admin/users',
      permission: 'admin:user',
    },
    {
      label: '问题总数',
      value: total.questionNumber,
      hint: latest.value ? deltaHint(latest.value.questionAddCount) : '社区累计问题数量',
      icon: MessageCircleQuestion,
      grad: 'from-sky-500 to-cyan-500 shadow-sky-500/25',
      valueClass: 'text-sky-600',
      to: '/admin/questions',
      permission: 'admin:question',
    },
    {
      label: '评论总数',
      value: ov.commentNumber,
      hint: '未删除的评论内容',
      icon: MessageCircle,
      grad: 'from-violet-500 to-purple-500 shadow-violet-500/25',
      valueClass: 'text-violet-600',
      to: '/admin/comments',
      permission: 'admin:comment',
    },
    {
      label: '当前在线',
      value: total.onlineUser,
      hint: '实时在线会话数',
      icon: Wifi,
      grad: 'from-emerald-500 to-teal-500 shadow-emerald-500/25',
      valueClass: 'text-emerald-600',
      to: '',
      permission: '',
    },
    {
      label: '待审核内容',
      value: pending.value.total,
      hint: `待审问题 ${formatNumber(pending.value.question)} · 待审评论 ${formatNumber(pending.value.comment)}`,
      icon: ShieldCheck,
      grad: 'from-amber-500 to-orange-500 shadow-amber-500/25',
      valueClass: Number(pending.value.total) > 0 ? 'text-amber-600' : 'ui-text-strong',
      to: '/admin/reviews',
      permission: 'admin:review',
      badge: Number(pending.value.total) > 0 ? '需处理' : '',
    },
    {
      label: '封禁用户',
      value: ov.bannedUserNumber,
      hint: '当前生效中的封禁账号',
      icon: UserX,
      grad: 'from-rose-500 to-red-500 shadow-rose-500/25',
      valueClass: Number(ov.bannedUserNumber) > 0 ? 'text-rose-600' : 'ui-text-strong',
      to: '/admin/users',
      permission: 'admin:user',
    },
    {
      label: '文件数量',
      value: ov.fileNumber,
      hint: `存储占用 ${formatBytes(ov.storageSize)}`,
      icon: FolderOpen,
      grad: 'from-cyan-500 to-sky-500 shadow-cyan-500/25',
      valueClass: 'text-cyan-600',
      to: '/admin/files',
      permission: 'admin:file',
    },
    {
      label: '投放中广告',
      value: ov.activeAdvertisementNumber,
      hint: '仍在有效期内的投放',
      icon: Megaphone,
      grad: 'from-fuchsia-500 to-pink-500 shadow-fuchsia-500/25',
      valueClass: 'text-fuchsia-600',
      to: '/admin/advertisements',
      permission: 'admin:advertisement',
    },
  ]
  return items.map((item) => ({ ...item, link: Boolean(item.to) && session.can(item.permission) }))
})

const pendingTiles = computed(() => [
  {
    key: 'question',
    label: '待审核问题',
    value: pending.value.question,
    icon: MessageCircleQuestion,
    grad: 'from-amber-500 to-orange-500',
    valueClass: Number(pending.value.question) > 0 ? 'text-amber-600' : 'ui-text-strong',
    to: '/admin/reviews?type=question&status=0',
  },
  {
    key: 'comment',
    label: '待审核评论',
    value: pending.value.comment,
    icon: MessageCircle,
    grad: 'from-sky-500 to-cyan-500',
    valueClass: Number(pending.value.comment) > 0 ? 'text-amber-600' : 'ui-text-strong',
    to: '/admin/reviews?type=comment&status=0',
  },
  {
    key: 'total',
    label: '待处理合计',
    value: pending.value.total,
    icon: ShieldCheck,
    grad: 'from-indigo-500 to-violet-500',
    valueClass: 'ui-text-brand',
    to: '/admin/reviews',
  },
])

const systemRows = computed(() => [
  {
    label: '用户注册',
    icon: UserPlus,
    enabled: Boolean(system.value.registerEnabled),
    on: '开放',
    off: '关闭',
  },
  {
    label: '聊天室',
    icon: MessageCircle,
    enabled: Boolean(system.value.chatEnabled),
    on: '开放',
    off: '关闭',
  },
  {
    label: '内容审核',
    icon: ShieldCheck,
    enabled: Boolean(system.value.reviewEnabled),
    on: '已开启',
    off: '已关闭',
  },
  {
    label: '邮件服务',
    icon: Mail,
    enabled: Boolean(system.value.mailEnabled),
    on: '已启用',
    off: '未启用',
  },
])

const reportTiles = computed(() => [
  {
    key: 'pending',
    label: '待处理举报',
    value: reports.value.pending,
    valueClass: Number(reports.value.pending) > 0 ? 'text-amber-600' : 'ui-text-strong',
    to: '/admin/reports?status=0',
  },
  {
    key: 'accepted',
    label: '已受理',
    value: reports.value.accepted,
    valueClass: 'text-emerald-600',
    to: '/admin/reports?status=1',
  },
  {
    key: 'rejected',
    label: '已驳回',
    value: reports.value.rejected,
    valueClass: 'ui-text-strong',
    to: '/admin/reports?status=2',
  },
])

const resourceRows = computed(() => {
  const ov = overview.value
  const items = [
    {
      label: '文件存储',
      hint: `占用 ${formatBytes(ov.storageSize)}`,
      value: `${formatNumber(ov.fileNumber)} 个`,
      icon: FolderOpen,
      iconClass: 'text-cyan-600',
      to: '/admin/files',
      permission: 'admin:file',
    },
    {
      label: '话题',
      hint: '有效话题数量',
      value: formatNumber(ov.topicNumber),
      icon: Tags,
      iconClass: 'text-violet-600',
      to: '/admin/topics',
      permission: 'admin:topic',
    },
    {
      label: '分类配置',
      hint: '问题分类 / 话题分类',
      value: `${formatNumber(ov.questionClassificationNumber)} / ${formatNumber(ov.topicClassificationNumber)}`,
      icon: ListTree,
      iconClass: 'text-amber-600',
      to: '/admin/classifications',
      permission: 'admin:classification',
    },
    {
      label: '封禁用户',
      hint: '当前生效中的封禁',
      value: formatNumber(ov.bannedUserNumber),
      icon: UserX,
      iconClass: 'text-rose-600',
      to: '/admin/users',
      permission: 'admin:user',
    },
    {
      label: '投放中广告',
      hint: '未结束的投放任务',
      value: formatNumber(ov.activeAdvertisementNumber),
      icon: Megaphone,
      iconClass: 'text-fuchsia-600',
      to: '/admin/advertisements',
      permission: 'admin:advertisement',
    },
  ]
  return items.map((item) => ({ ...item, link: session.can(item.permission) }))
})

const option = computed(() => {
  const list = [...rows.value].reverse()
  return {
    tooltip: { trigger: 'axis', confine: true },
    legend: {
      data: ['用户总数', '问题总数', '新增用户', '新增问题'],
      bottom: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#64748b', fontSize: 12 },
    },
    grid: { left: 8, right: 8, top: 34, bottom: 40, containLabel: true },
    xAxis: {
      type: 'category',
      data: list.map((row) => (row.time || '').slice(5, 16)),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 11, hideOverlap: true },
    },
    yAxis: [
      {
        type: 'value',
        name: '累计',
        nameTextStyle: { color: '#94a3b8', fontSize: 11 },
        axisLabel: { color: '#94a3b8', fontSize: 11 },
        splitLine: { lineStyle: { color: '#eef2f7' } },
      },
      {
        type: 'value',
        name: '新增',
        nameTextStyle: { color: '#94a3b8', fontSize: 11 },
        axisLabel: { color: '#94a3b8', fontSize: 11 },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '用户总数',
        type: 'line',
        smooth: true,
        symbol: 'none',
        yAxisIndex: 0,
        data: list.map((row) => row.userCount),
        lineStyle: { width: 2.5, color: '#4f46e5' },
        itemStyle: { color: '#4f46e5' },
      },
      {
        name: '问题总数',
        type: 'line',
        smooth: true,
        symbol: 'none',
        yAxisIndex: 0,
        data: list.map((row) => row.questionCount),
        lineStyle: { width: 2.5, color: '#0ea5e9' },
        itemStyle: { color: '#0ea5e9' },
      },
      {
        name: '新增用户',
        type: 'bar',
        yAxisIndex: 1,
        barMaxWidth: 14,
        data: list.map((row) => row.userAddCount),
        itemStyle: { color: '#f59e0b', borderRadius: [4, 4, 0, 0] },
      },
      {
        name: '新增问题',
        type: 'bar',
        yAxisIndex: 1,
        barMaxWidth: 14,
        data: list.map((row) => row.questionAddCount),
        itemStyle: { color: '#10b981', borderRadius: [4, 4, 0, 0] },
      },
    ],
  }
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-lg font-bold ui-text-strong sm:text-xl">数据概览</h1>
        <p class="mt-1 text-xs ui-text-muted">
          社区内容、审核待办与系统运行状态总览<span v-if="updatedAt">
            · 更新于 {{ updatedAt }}</span
          >
        </p>
      </div>
      <button class="btn btn-ghost btn-sm" type="button" :disabled="loading" @click="load">
        <RefreshCw class="size-4" :class="loading && 'animate-spin'" />刷新
      </button>
    </div>

    <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      <component
        :is="card.link ? RouterLink : 'div'"
        v-for="card in cards"
        :key="card.label"
        :to="card.link ? card.to : undefined"
        class="surface group relative overflow-hidden p-4"
        :class="card.link ? 'transition hover:-translate-y-0.5 hover:shadow-md' : ''"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="flex items-center gap-1.5 text-xs font-medium ui-text-muted">
              {{ card.label }}
              <span v-if="card.badge" class="badge badge-warning badge-xs">{{ card.badge }}</span>
            </p>
            <p class="mt-1.5 text-2xl font-bold tabular-nums" :class="card.valueClass">
              {{ formatNumber(card.value) }}
            </p>
            <p class="mt-1 truncate text-xs ui-text-muted" :title="card.hint">{{ card.hint }}</p>
          </div>
          <span
            class="grid size-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br text-white shadow-lg transition group-hover:scale-105"
            :class="card.grad"
          >
            <component :is="card.icon" class="size-5" />
          </span>
        </div>
      </component>
    </div>

    <div class="grid gap-4 xl:grid-cols-3">
      <section class="surface relative overflow-hidden p-5 xl:col-span-2">
        <div
          class="pointer-events-none absolute -right-16 -top-20 size-48 rounded-full bg-gradient-to-br from-amber-400/10 to-orange-500/5 blur-2xl"
        />
        <div class="relative flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 class="section-title">审核与举报</h2>
            <p class="mt-1 text-xs ui-text-muted">审核待办与用户举报的处理状态</p>
          </div>
          <div class="flex items-center gap-2">
            <span v-if="!system.reviewEnabled" class="badge badge-ghost badge-sm">审核已关闭</span>
            <RouterLink v-if="canReview" to="/admin/reviews" class="btn btn-primary btn-sm">
              <ShieldCheck class="size-4" />前往审核
            </RouterLink>
          </div>
        </div>

        <div class="relative mt-4 grid gap-3 sm:grid-cols-3">
          <component
            :is="canReview ? RouterLink : 'div'"
            v-for="tile in pendingTiles"
            :key="tile.key"
            :to="canReview ? tile.to : undefined"
            class="flex items-center justify-between gap-3 rounded-xl border ui-border p-3"
            :class="canReview ? 'transition hover:border-indigo-300 hover:bg-slate-50' : ''"
          >
            <div class="min-w-0">
              <p class="text-xs font-medium ui-text-muted">{{ tile.label }}</p>
              <p class="mt-1 text-2xl font-bold tabular-nums" :class="tile.valueClass">
                {{ formatNumber(tile.value) }}
              </p>
            </div>
            <span
              class="grid size-10 shrink-0 place-items-center rounded-lg bg-gradient-to-br text-white"
              :class="tile.grad"
            >
              <component :is="tile.icon" class="size-5" />
            </span>
          </component>
        </div>

        <div
          class="relative mt-3 flex flex-wrap items-center justify-between gap-2 text-xs ui-text-muted"
        >
          <p class="flex items-center gap-1.5">
            <span
              class="inline-flex size-1.5 rounded-full"
              :class="
                !data
                  ? 'bg-slate-300'
                  : Number(pending.total) > 0
                    ? 'bg-amber-500'
                    : 'bg-emerald-500'
              "
            />
            {{
              !data
                ? '正在加载审核状态…'
                : Number(pending.total) > 0
                  ? '存在待处理内容，建议尽快完成审核'
                  : '当前没有待审核内容，状态良好'
            }}
          </p>
          <RouterLink
            v-if="canReview && Number(pending.total) > 0"
            to="/admin/reviews"
            class="inline-flex items-center gap-0.5 ui-text-brand"
          >
            立即处理<ArrowUpRight class="size-3" />
          </RouterLink>
        </div>

        <div class="relative mt-4 border-t ui-border pt-4">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h3 class="flex items-center gap-1.5 text-sm font-semibold ui-text-strong">
                <Flag class="size-4 text-rose-500" />举报处理
              </h3>
              <p class="mt-0.5 text-xs ui-text-muted">
                共 {{ formatNumber(reports.total) }} 条举报，用户可在问题与评论区举报违规内容
              </p>
            </div>
            <RouterLink
              v-if="canManageReports"
              to="/admin/reports"
              class="btn btn-ghost btn-xs ui-text-brand"
            >
              前往举报管理<ArrowUpRight class="size-3.5" />
            </RouterLink>
          </div>
          <div class="mt-3 grid gap-2 sm:grid-cols-3">
            <component
              :is="canManageReports ? RouterLink : 'div'"
              v-for="tile in reportTiles"
              :key="tile.key"
              :to="canManageReports ? tile.to : undefined"
              class="rounded-lg ui-bg-soft p-3"
              :class="canManageReports ? 'transition hover:bg-slate-50' : ''"
            >
              <p class="text-xs ui-text-muted">{{ tile.label }}</p>
              <p class="mt-1 text-xl font-bold tabular-nums" :class="tile.valueClass">
                {{ formatNumber(tile.value) }}
              </p>
            </component>
          </div>
        </div>
      </section>

      <section class="surface p-5">
        <div>
          <h2 class="section-title">系统状态</h2>
          <p class="mt-1 text-xs ui-text-muted">功能开关与缓存运行指标</p>
        </div>

        <ul class="mt-3 space-y-1.5">
          <li
            v-for="item in systemRows"
            :key="item.label"
            class="flex items-center justify-between gap-2 rounded-lg ui-bg-soft px-3 py-2"
          >
            <span class="flex items-center gap-2 text-xs font-medium ui-text-base">
              <component :is="item.icon" class="size-3.5 ui-text-muted" />{{ item.label }}
            </span>
            <span
              class="badge badge-sm"
              :class="data && item.enabled ? 'badge-success' : 'badge-ghost'"
            >
              {{ data ? (item.enabled ? item.on : item.off) : '—' }}
            </span>
          </li>
        </ul>

        <div class="mt-3 grid grid-cols-2 gap-2">
          <div class="rounded-lg ui-bg-soft p-3">
            <p class="flex items-center gap-1 text-xs ui-text-muted">
              <Gauge class="size-3.5" />缓存命中率
            </p>
            <p class="mt-1 text-lg font-bold text-emerald-600">{{ system.cacheHitRate || '0%' }}</p>
          </div>
          <div class="rounded-lg ui-bg-soft p-3">
            <p class="flex items-center gap-1 text-xs ui-text-muted">
              <Database class="size-3.5" />缓存条目
            </p>
            <p class="mt-1 text-lg font-bold tabular-nums ui-text-strong">
              {{ formatNumber(system.cacheSize || 0) }}
            </p>
          </div>
        </div>

        <p v-if="!system.reviewEnabled" class="mt-3 text-xs ui-text-muted">
          内容审核关闭时，新发布的问题与评论将直接展示。
        </p>
      </section>
    </div>

    <div class="grid gap-4 xl:grid-cols-3">
      <section class="surface p-5 xl:col-span-2">
        <div class="mb-3 flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 class="section-title">数据变化趋势</h2>
            <p class="mt-1 text-xs ui-text-muted">
              定时采样（约 6 小时一次），柱状为区间新增，折线为累计总量
            </p>
          </div>
          <div class="flex flex-wrap items-center gap-1.5 text-xs">
            <span class="rounded-md ui-bg-soft px-2 py-1 ui-text-base"
              >最近采样 {{ latestTime }}</span
            >
            <span class="rounded-md ui-bg-soft px-2 py-1 ui-text-base">
              新增用户 <b class="text-amber-600">+{{ formatNumber(latest?.userAddCount ?? 0) }}</b>
            </span>
            <span class="rounded-md ui-bg-soft px-2 py-1 ui-text-base">
              新增问题
              <b class="text-emerald-600">+{{ formatNumber(latest?.questionAddCount ?? 0) }}</b>
            </span>
          </div>
        </div>
        <EChart v-if="rows.length" :option="option" :height="320" />
        <div v-else class="empty-state">暂无统计数据</div>
      </section>

      <section class="surface p-5">
        <div>
          <h2 class="section-title">资源与运营</h2>
          <p class="mt-1 text-xs ui-text-muted">文件、话题与运营资源概览</p>
        </div>

        <div class="mt-2 space-y-0.5">
          <component
            :is="row.link ? RouterLink : 'div'"
            v-for="row in resourceRows"
            :key="row.label"
            :to="row.link ? row.to : undefined"
            class="flex items-center gap-3 rounded-lg px-2 py-2.5"
            :class="row.link ? 'transition hover:bg-slate-50' : ''"
          >
            <span
              class="grid size-8 shrink-0 place-items-center rounded-lg ui-bg-soft"
              :class="row.iconClass"
            >
              <component :is="row.icon" class="size-4" />
            </span>
            <span class="min-w-0 flex-1">
              <span class="block text-xs ui-text-muted">{{ row.label }}</span>
              <span class="mt-0.5 block truncate text-xs ui-text-muted">{{ row.hint }}</span>
            </span>
            <span class="shrink-0 text-sm font-bold tabular-nums ui-text-strong">{{
              row.value
            }}</span>
            <ChevronRight v-if="row.link" class="size-3.5 shrink-0 ui-text-muted" />
          </component>
        </div>
      </section>
    </div>

    <section class="surface overflow-hidden">
      <div class="px-5 pt-5">
        <h2 class="section-title">近期采样数据</h2>
        <p class="mt-1 text-xs ui-text-muted">
          共 {{ formatNumber(data?.pagination?.allCount ?? 0) }} 条记录，按时间倒序
        </p>
      </div>
      <div class="mt-3 overflow-x-auto">
        <table class="table table-sm">
          <thead>
            <tr>
              <th>采样时间</th>
              <th class="text-right">用户总数</th>
              <th class="text-right">问题总数</th>
              <th class="text-right">新增用户</th>
              <th class="text-right">新增问题</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.time" class="hover:bg-slate-50">
              <td class="whitespace-nowrap text-xs ui-text-base">{{ row.time }}</td>
              <td class="text-right font-medium tabular-nums ui-text-strong">
                {{ formatNumber(row.userCount) }}
              </td>
              <td class="text-right font-medium tabular-nums ui-text-strong">
                {{ formatNumber(row.questionCount) }}
              </td>
              <td class="text-right font-medium tabular-nums text-amber-600">
                +{{ formatNumber(row.userAddCount) }}
              </td>
              <td class="text-right font-medium tabular-nums text-emerald-600">
                +{{ formatNumber(row.questionAddCount) }}
              </td>
            </tr>
            <tr v-if="!rows.length && !loading">
              <td colspan="5" class="py-12 text-center text-sm ui-text-muted">暂无数据</td>
            </tr>
            <tr v-if="loading && !rows.length">
              <td colspan="5" class="py-12 text-center text-sm ui-text-muted">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="px-4 pb-2">
        <Pagination
          :pagination="data?.pagination"
          @change="router.push({ name: 'admin-dashboard', query: { page: $event } })"
        />
      </div>
    </section>
  </div>
</template>
