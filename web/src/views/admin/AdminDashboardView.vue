<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessageCircleQuestion, Users, Wifi } from '@lucide/vue'
import { adminApi } from '@/api'
import EChart from '@/components/EChart.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()
const data = ref(null)
const page = computed(() => route.query.page || '1')

async function load() {
  data.value = await adminApi.dashboard({ page: page.value, size: 20 })
}
watch(page, load, { immediate: true })

const rows = computed(() => data.value?.pagination?.data || [])
const option = computed(() => {
  const list = [...rows.value].reverse()
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['用户总数', '问题总数', '新增用户', '新增问题'], bottom: 0, textStyle: { color: '#64748b' } },
    grid: { left: 50, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: list.map((r) => r.time), axisLabel: { color: '#94a3b8', fontSize: 10 } },
    yAxis: { type: 'value', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: '#eef2f7' } } },
    series: [
      { name: '用户总数', type: 'line', smooth: true, data: list.map((r) => r.userCount), itemStyle: { color: '#4f46e5' } },
      { name: '问题总数', type: 'line', smooth: true, data: list.map((r) => r.questionCount), itemStyle: { color: '#0ea5e9' } },
      { name: '新增用户', type: 'bar', data: list.map((r) => r.userAddCount), itemStyle: { color: '#f59e0b' } },
      { name: '新增问题', type: 'bar', data: list.map((r) => r.questionAddCount), itemStyle: { color: '#10b981' } },
    ],
  }
})
</script>

<template>
  <div class="space-y-5">
    <div class="grid gap-4 sm:grid-cols-3">
      <div class="surface p-5"><p class="flex items-center gap-1.5 text-xs text-slate-400"><Users class="size-4 text-indigo-500" />注册用户</p><p class="mt-1 text-2xl font-bold text-indigo-600">{{ data?.userNumber ?? '-' }}</p></div>
      <div class="surface p-5"><p class="flex items-center gap-1.5 text-xs text-slate-400"><MessageCircleQuestion class="size-4 text-sky-500" />问题总数</p><p class="mt-1 text-2xl font-bold text-sky-600">{{ data?.questionNumber ?? '-' }}</p></div>
      <div class="surface p-5"><p class="flex items-center gap-1.5 text-xs text-slate-400"><Wifi class="size-4 text-emerald-500" />当前在线</p><p class="mt-1 text-2xl font-bold text-emerald-600">{{ data?.onlineUser ?? '-' }}</p></div>
    </div>

    <div class="surface p-5">
      <h2 class="mb-3 text-sm font-semibold text-slate-700">数据变化趋势</h2>
      <EChart v-if="rows.length" :option="option" :height="300" />
      <p v-else class="py-16 text-center text-sm text-slate-400">暂无统计数据</p>
    </div>

    <div class="surface overflow-x-auto p-5">
      <h2 class="mb-3 text-sm font-semibold text-slate-700">近期数据</h2>
      <table class="table table-sm">
        <thead><tr><th>时间</th><th>用户总数</th><th>问题总数</th><th>新增用户</th><th>新增问题</th></tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="row.time"><td>{{ row.time }}</td><td>{{ row.userCount }}</td><td>{{ row.questionCount }}</td><td>{{ row.userAddCount }}</td><td>{{ row.questionAddCount }}</td></tr>
          <tr v-if="!rows.length"><td colspan="5" class="text-center text-slate-400">暂无数据</td></tr>
        </tbody>
      </table>
      <Pagination :pagination="data?.pagination" @change="router.push({ name: 'admin-dashboard', query: { page: $event } })" />
    </div>
  </div>
</template>
