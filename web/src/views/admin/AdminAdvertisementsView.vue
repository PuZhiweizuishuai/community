<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronLeft, ChevronRight, CircleCheck, CirclePlay, CircleX, Filter, ListChecks, Megaphone, Plus, SquarePen } from '@lucide/vue'
import { adminApi, assetUrl } from '@/api'
import { useToastStore } from '@/stores/toast'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const page = computed(() => route.query.page || '1')
const position = ref(route.query.position || '')
const status = ref(route.query.status || '')
const data = ref(null)
const positions = ref([])
const selected = ref([])
const setting = ref(null)
const days = ref(7)
const targetPosition = ref('home page')
const busy = ref(false)
const batchResult = ref(null)

const rows = computed(() => {
  const list = data.value?.data || []
  return list.filter((item) => {
    if (position.value && item.position !== position.value) return false
    if (status.value !== '' && String(item.status) !== String(status.value)) return false
    return true
  })
})
const activeCount = computed(() => rows.value.filter((r) => r.status === 1).length)
const allOnPageSelected = computed(() => rows.value.length > 0 && rows.value.every((r) => selected.value.includes(r.id)))
const selectableIds = computed(() => rows.value.map((r) => r.id))
const positionOptions = computed(() => positions.value.map((p) => p.position))

async function load() {
  const [list, stats] = await Promise.all([
    adminApi.advertisements({ page: page.value, size: 20 }),
    adminApi.adPositions().catch(() => ({ positions: [] })),
  ])
  data.value = list
  positions.value = stats.positions || []
  selected.value = selected.value.filter((id) => selectableIds.value.includes(id))
}
onMounted(load)
watch(page, () => load())

function go(patch) {
  router.push({ name: 'admin-ads', query: { ...route.query, ...patch } })
}

function toggleAll() {
  selected.value = allOnPageSelected.value ? [] : [...selectableIds.value]
}
function toggleOne(id) {
  selected.value = selected.value.includes(id)
    ? selected.value.filter((x) => x !== id)
    : [...selected.value, id]
}

function openBatchSetting() {
  if (!selected.value.length) return toast.warning('请先勾选要投放的广告')
  targetPosition.value = position.value || 'home page'
  days.value = 7
  setting.value = { mode: 'batch' }
}
function openSingleSetting(item) {
  selected.value = [item.id]
  targetPosition.value = item.position || 'home page'
  days.value = 7
  setting.value = { mode: 'batch' }
}

async function submitBatchSetting() {
  busy.value = true
  try {
    const result = await adminApi.batchSettingAd({
      ids: [...selected.value],
      position: targetPosition.value,
      time: Number(days.value),
    })
    batchResult.value = result
    toast.success(`${result.successCount} 条投放成功${result.failedCount ? `，${result.failedCount} 条失败` : ''}`)
    setting.value = null
    selected.value = []
    await load()
  } finally {
    busy.value = false
  }
}

async function batchClose() {
  if (!selected.value.length) return toast.warning('请先勾选要结束的广告')
  if (!window.confirm(`确定结束选中的 ${selected.value.length} 条广告投放？`)) return
  busy.value = true
  try {
    const result = await adminApi.batchCloseAd([...selected.value])
    batchResult.value = result
    toast.success(`${result.successCount} 条已结束`)
    selected.value = []
    await load()
  } finally {
    busy.value = false
  }
}

async function closeOne(item) {
  if (!window.confirm(`确定提前结束「${item.title}」的投放？`)) return
  const result = await adminApi.batchCloseAd([item.id])
  toast.success(result.results?.[0]?.message || '已结束')
  await load()
}

function fmt(time) {
  return time ? new Date(Number(time)).toLocaleString('zh-CN') : '—'
}
function capacityOf(pos) {
  return positions.value.find((p) => p.position === pos)
}
</script>

<template>
  <div class="space-y-4">
    <!-- 概览 -->
    <div class="grid gap-3 sm:grid-cols-3">
      <div class="surface flex items-center justify-between p-4">
        <div><p class="text-xs ui-text-muted">广告总数</p><p class="mt-1 text-2xl font-bold ui-text-strong">{{ data?.allCount ?? 0 }}</p></div>
        <Megaphone class="size-7 ui-text-brand opacity-40" />
      </div>
      <div class="surface flex items-center justify-between p-4">
        <div><p class="text-xs ui-text-muted">本页投放中</p><p class="mt-1 text-2xl font-bold text-emerald-600">{{ activeCount }}</p></div>
        <CirclePlay class="size-7 text-emerald-500 opacity-40" />
      </div>
      <div class="surface flex items-center justify-between p-4">
        <div><p class="text-xs ui-text-muted">已选广告</p><p class="mt-1 text-2xl font-bold ui-text-brand">{{ selected.length }}</p></div>
        <ListChecks class="size-7 ui-text-brand opacity-40" />
      </div>
    </div>

    <div class="surface flex flex-wrap items-center gap-2 p-4">
      <div class="mr-auto">
        <h2 class="section-title">广告与新闻投放</h2>
        <p class="mt-1 text-xs ui-text-muted">支持勾选多条后统一设置投放位置与天数</p>
      </div>
      <RouterLink to="/admin/advertisements/new" class="btn btn-primary btn-sm"><Plus class="size-4" />新增广告</RouterLink>
    </div>

    <!-- 筛选 + 批量操作条 -->
    <div class="surface flex flex-wrap items-center gap-2 p-4">
      <select v-model="position" class="select select-bordered select-sm w-40">
        <option value="">全部位置</option>
        <option v-for="p in positionOptions" :key="p" :value="p">{{ p }}</option>
      </select>
      <select v-model="status" class="select select-bordered select-sm w-32">
        <option value="">全部状态</option>
        <option value="1">投放中</option>
        <option value="0">未投放</option>
      </select>
      <button class="btn btn-ghost btn-sm" type="button" @click="go({ page: 1, position: position || undefined, status: status || undefined })"><Filter class="size-4" />应用筛选</button>

      <div v-if="selected.length" class="ml-auto flex flex-wrap items-center gap-2 rounded-lg ui-bg-brand px-3 py-1.5">
        <span class="text-xs font-medium ui-text-brand">已选 {{ selected.length }} 条</span>
        <button class="btn btn-primary btn-xs" type="button" @click="openBatchSetting">批量投放</button>
        <button class="btn btn-outline btn-xs" type="button" @click="batchClose">批量结束</button>
        <button class="btn btn-ghost btn-xs" type="button" @click="selected = []">取消选择</button>
      </div>
    </div>

    <!-- 位置容量 -->
    <div class="surface p-4">
      <h3 class="mb-3 section-title">各投放位置容量</h3>
      <div class="grid gap-2 sm:grid-cols-3 lg:grid-cols-5">
        <button
          v-for="p in positions"
          :key="p.position"
          class="rounded-lg border p-2 text-left transition"
          :class="targetPosition === p.position ? 'border-indigo-400 ui-bg-brand' : 'ui-border hover:border-indigo-200'"
          type="button"
          @click="targetPosition = p.position"
        >
          <p class="truncate text-xs font-medium ui-text-strong">{{ p.position }}</p>
          <div class="mt-1 flex items-center gap-2">
            <progress class="progress progress-primary h-1 flex-1" :value="p.used" :max="p.max" />
            <span class="text-xs" :class="p.available > 0 ? 'ui-text-muted' : 'text-rose-500'">{{ p.used }}/{{ p.max }}</span>
          </div>
        </button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="surface overflow-x-auto p-4">
      <table class="table table-sm">
        <thead>
          <tr>
            <th><input type="checkbox" class="checkbox checkbox-xs checkbox-primary" :checked="allOnPageSelected" @change="toggleAll" /></th>
            <th>预览</th><th>标题</th><th>投放位置</th><th>浏览</th><th>开始 / 结束</th><th>状态</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in rows" :key="item.id" :class="selected.includes(item.id) ? 'bg-indigo-50/60' : ''">
            <td><input type="checkbox" class="checkbox checkbox-xs checkbox-primary" :checked="selected.includes(item.id)" @change="toggleOne(item.id)" /></td>
            <td><img :src="assetUrl(item.image)" alt="" class="h-10 w-16 rounded object-cover" /></td>
            <td class="max-w-[180px]"><a v-if="item.url" :href="item.url" target="_blank" rel="noopener" class="line-clamp-1 hover:text-indigo-600">{{ item.title }}</a><span v-else>{{ item.title }}</span></td>
            <td class="text-xs">{{ item.position || '未投放' }}</td>
            <td>{{ item.viewCount }}</td>
            <td class="text-xs">{{ fmt(item.startTime) }}<br />{{ fmt(item.endTime) }}</td>
            <td><span class="badge badge-sm" :class="item.status === 1 ? 'badge-success' : 'badge-ghost'">{{ item.status === 1 ? '投放中' : '未投放' }}</span></td>
            <td>
              <div class="flex gap-1">
                <RouterLink :to="`/admin/advertisements/${item.id}`" class="btn btn-ghost btn-xs"><SquarePen class="size-3.5" />编辑</RouterLink>
                <button class="btn btn-ghost btn-xs ui-text-brand" type="button" @click="openSingleSetting(item)">投放</button>
                <button v-if="item.status === 1" class="btn btn-ghost btn-xs text-rose-500" type="button" @click="closeOne(item)">结束</button>
              </div>
            </td>
          </tr>
          <tr v-if="!rows.length"><td colspan="8" class="text-center ui-text-muted">暂无广告</td></tr>
        </tbody>
      </table>
      <div class="mt-3 flex items-center gap-2">
        <button class="btn btn-ghost btn-xs" :disabled="Number(page) <= 1" type="button" @click="go({ page: Number(page) - 1 })"><ChevronLeft class="size-3.5" />上一页</button>
        <span class="text-xs ui-text-muted">第 {{ page }} / {{ data?.totalPage || 1 }} 页</span>
        <button class="btn btn-ghost btn-xs" :disabled="Number(page) >= Number(data?.totalPage || 1)" type="button" @click="go({ page: Number(page) + 1 })">下一页<ChevronRight class="size-3.5" /></button>
      </div>
    </div>

    <!-- 批量投放弹窗 -->
    <dialog v-if="setting" class="modal modal-open">
      <div class="modal-box max-w-md">
        <h3 class="text-base font-semibold">批量投放（{{ selected.length }} 条）</h3>
        <p class="mt-1 text-xs ui-text-muted">同一广告续期到相同位置不会重复占用名额</p>
        <label class="form-control mt-4">
          <span class="label-text mb-1 font-medium">投放位置</span>
          <select v-model="targetPosition" class="select select-bordered select-sm">
            <option v-for="p in positions" :key="p.position" :value="p.position">{{ p.position }}</option>
          </select>
        </label>
        <p v-if="capacityOf(targetPosition)" class="mt-1 text-xs" :class="capacityOf(targetPosition).available > 0 ? 'ui-text-muted' : 'text-rose-500'">
          该位置剩余 {{ capacityOf(targetPosition).available }} / {{ capacityOf(targetPosition).max }} 个名额
        </p>
        <label class="form-control mt-3">
          <span class="label-text mb-1 font-medium">投放天数（1 ~ 365）</span>
          <input v-model.number="days" type="number" min="1" max="365" class="input input-bordered input-sm" />
        </label>
        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="setting = null">取消</button>
          <button class="btn btn-primary btn-sm" :disabled="busy" type="button" @click="submitBatchSetting">{{ busy ? '处理中…' : '确认投放' }}</button>
        </div>
      </div>
    </dialog>

    <!-- 批量结果 -->
    <dialog v-if="batchResult" class="modal modal-open">
      <div class="modal-box max-w-lg">
        <h3 class="text-base font-semibold">{{ batchResult.action }}结果</h3>
        <p class="mt-1 text-sm ui-text-base">成功 {{ batchResult.successCount }} 条，失败 {{ batchResult.failedCount }} 条</p>
        <ul class="mt-3 max-h-64 space-y-1 overflow-y-auto">
          <li v-for="(r, i) in batchResult.results" :key="i" class="flex items-start gap-2 rounded-lg ui-bg-soft px-3 py-2 text-xs">
            <span class="shrink-0" :class="r.success ? 'text-emerald-600' : 'text-rose-500'"><component :is="r.success ? CircleCheck : CircleX" class="size-4" /></span>
            <span class="min-w-0 flex-1"><b>{{ r.title || `ID ${r.id}` }}</b> · {{ r.message }}</span>
          </li>
        </ul>
        <div class="modal-action"><button class="btn btn-primary btn-sm" type="button" @click="batchResult = null">知道了</button></div>
      </div>
    </dialog>
  </div>
</template>
