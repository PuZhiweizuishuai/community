<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ChevronLeft, ChevronRight, Copy, File, FileText, Film, Music, Search, Trash2 } from '@lucide/vue'
import { adminApi, assetUrl } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
const confirm = useConfirmStore()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = 20
const storages = ref([])

const filters = reactive({ keyword: '', fileType: '', storageId: '', status: '' })

const typeOptions = [
  { value: '', label: '全部类型' },
  { value: 'image', label: '图片' },
  { value: 'video', label: '视频' },
  { value: 'audio', label: '音频' },
  { value: 'pdf', label: 'PDF' },
  { value: 'other', label: '其他' },
]
const typeMeta = {
  image: { label: '图片', badge: 'badge-success', icon: FileText },
  video: { label: '视频', badge: 'badge-info', icon: Film },
  audio: { label: '音频', badge: 'badge-secondary', icon: Music },
  pdf: { label: 'PDF', badge: 'badge-warning', icon: FileText },
  other: { label: '其他', badge: 'badge-ghost', icon: File },
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.fileType) params.fileType = filters.fileType
    if (filters.storageId !== '') params.storageId = filters.storageId
    if (filters.status !== '') params.status = filters.status
    const data = await adminApi.files(params)
    records.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function search() {
  page.value = 1
  await load()
}

function changePage(next) {
  if (next < 1 || next > totalPages.value || next === page.value) return
  page.value = next
  load()
}

onMounted(async () => {
  storages.value = await adminApi.storages()
  await load()
})

function formatSize(bytes) {
  if (bytes == null) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`
}

function formatTime(millis) {
  if (!millis) return '-'
  return new Date(millis).toLocaleString('zh-CN', { hour12: false })
}

async function copyUrl(record) {
  try {
    await navigator.clipboard.writeText(assetUrl(record.url))
    toast.success('链接已复制')
  } catch {
    toast.warning('复制失败，请手动复制')
  }
}

async function remove(record) {
  if (
    !(await confirm.ask({
      title: '删除文件',
      message: `确定删除文件「${record.originalName || record.fileKey}」吗？物理文件将被移除，引用该文件的页面将无法显示。`,
      confirmText: '删除',
      danger: true,
    }))
  )
    return
  await adminApi.deleteFile(record.id)
  toast.success('文件已删除')
  await load()
}
</script>

<template>
  <div class="surface p-5">
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <div class="mr-auto">
        <h2 class="section-title">文件管理（{{ total }}）</h2>
        <p class="mt-1 text-xs ui-text-muted">系统保存的全部文件；删除会同时移除物理文件并保留审计记录</p>
      </div>
    </div>

    <form class="mb-4 flex flex-wrap items-center gap-2" @submit.prevent="search">
      <input v-model="filters.keyword" class="input input-bordered input-sm w-48" placeholder="搜索原文件名 / 存储键" />
      <select v-model="filters.fileType" class="select select-bordered select-sm">
        <option v-for="option in typeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
      </select>
      <select v-model="filters.storageId" class="select select-bordered select-sm">
        <option value="">全部存储</option>
        <option v-for="storage in storages" :key="storage.id" :value="storage.id">{{ storage.name }}（ID {{ storage.id }}）</option>
      </select>
      <select v-model="filters.status" class="select select-bordered select-sm">
        <option value="">全部状态</option>
        <option value="0">正常</option>
        <option value="1">已删除</option>
      </select>
      <button class="btn btn-primary btn-sm" type="submit"><Search class="size-4" />查询</button>
    </form>

    <div class="overflow-x-auto">
      <table class="table table-sm">
        <thead>
          <tr>
            <th>预览</th><th>原文件名</th><th>类型</th><th>大小</th><th>存储</th>
            <th>上传者</th><th>上传时间</th><th>状态</th><th>URL</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in records" :key="record.id" :class="{ 'opacity-50': record.status === 1 }">
            <td>
              <a v-if="record.fileType === 'image' && record.status === 0" :href="assetUrl(record.url)" target="_blank" rel="noopener">
                <img :src="assetUrl(record.url)" :alt="record.originalName" class="size-10 rounded object-cover" loading="lazy" />
              </a>
              <span v-else class="flex size-10 items-center justify-center rounded bg-base-200 ui-text-muted">
                <component :is="(typeMeta[record.fileType] || typeMeta.other).icon" class="size-5" />
              </span>
            </td>
            <td class="max-w-48">
              <span class="block truncate font-medium" :title="record.originalName">{{ record.originalName || '-' }}</span>
              <span class="block truncate text-xs ui-text-muted" :title="record.fileKey">{{ record.fileKey }}</span>
            </td>
            <td>
              <span class="badge badge-sm" :class="(typeMeta[record.fileType] || typeMeta.other).badge">
                {{ (typeMeta[record.fileType] || typeMeta.other).label }}
              </span>
            </td>
            <td class="whitespace-nowrap text-xs">{{ formatSize(record.fileSize) }}</td>
            <td class="whitespace-nowrap text-xs">{{ record.storageName }}（{{ record.storageId }}）</td>
            <td class="text-xs">{{ record.uploaderName || '-' }}</td>
            <td class="whitespace-nowrap text-xs">{{ formatTime(record.createTime) }}</td>
            <td>
              <span class="badge badge-sm" :class="record.status === 0 ? 'badge-success' : 'badge-error'">
                {{ record.status === 0 ? '正常' : '已删除' }}
              </span>
            </td>
            <td class="max-w-40">
              <span class="block truncate text-xs ui-text-muted" :title="record.url">{{ record.url }}</span>
            </td>
            <td>
              <div class="flex items-center gap-1">
                <button class="btn btn-ghost btn-xs" type="button" title="复制链接" @click="copyUrl(record)">
                  <Copy class="size-3.5" />
                </button>
                <button
                  v-if="record.status === 0"
                  class="btn btn-ghost btn-xs text-rose-500"
                  type="button"
                  title="删除文件"
                  @click="remove(record)"
                >
                  <Trash2 class="size-3.5" />
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && !records.length">
            <td colspan="10" class="py-8 text-center ui-text-muted">暂无文件</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-4 flex items-center justify-end gap-2 text-sm">
      <span class="ui-text-muted">第 {{ page }} / {{ totalPages }} 页</span>
      <button class="btn btn-ghost btn-xs" type="button" :disabled="page <= 1" @click="changePage(page - 1)">
        <ChevronLeft class="size-4" />上一页
      </button>
      <button class="btn btn-ghost btn-xs" type="button" :disabled="page >= totalPages" @click="changePage(page + 1)">
        下一页<ChevronRight class="size-4" />
      </button>
    </div>
  </div>
</template>