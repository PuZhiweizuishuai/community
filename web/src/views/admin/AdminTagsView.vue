<script setup>
import { computed, onMounted, ref } from 'vue'
import { Check, RefreshCw, Search, SquarePen, Tags, Trash2, X, XCircle } from '@lucide/vue'
import { adminApi } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useToastStore } from '@/stores/toast'
import { formatDateTime } from '@/utils/format'
import Pagination from '@/components/Pagination.vue'

const toast = useToastStore()
const confirm = useConfirmStore()

const status = ref(0)
const keyword = ref('')
const page = ref(1)
const loading = ref(false)
const pagination = ref(null)

const rejectTarget = ref(null)
const rejectReason = ref('')
const renameTarget = ref(null)
const renameName = ref('')
const saving = ref(false)

const tabs = [
  { value: 0, label: '待审核' },
  { value: 1, label: '已通过' },
  { value: 2, label: '未通过' },
  { value: -1, label: '全部' },
]

const rows = computed(() => pagination.value?.data || [])

async function load() {
  loading.value = true
  try {
    pagination.value = await adminApi.tags({
      status: status.value < 0 ? undefined : status.value,
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      size: 20,
    })
  } finally {
    loading.value = false
  }
}
onMounted(load)

function switchStatus(value) {
  status.value = value
  page.value = 1
  load()
}

function goPage(next) {
  page.value = next
  load()
}

async function approve(item) {
  await adminApi.approveTag(item.id)
  toast.success(`标签「${item.name}」已通过审核`)
  await load()
}

function openReject(item) {
  rejectTarget.value = item
  rejectReason.value = ''
}
async function submitReject() {
  if (!rejectReason.value.trim()) return toast.warning('请填写不通过原因')
  saving.value = true
  try {
    await adminApi.rejectTag(rejectTarget.value.id, rejectReason.value.trim())
    toast.success('已拒绝该标签')
    rejectTarget.value = null
    await load()
  } finally {
    saving.value = false
  }
}

function openRename(item) {
  renameTarget.value = item
  renameName.value = item.name
}
async function submitRename() {
  if (!renameName.value.trim()) return toast.warning('标签名称不能为空')
  saving.value = true
  try {
    await adminApi.renameTag(renameTarget.value.id, renameName.value.trim())
    toast.success('标签已重命名')
    renameTarget.value = null
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(item) {
  if (!(await confirm.ask({
    title: '删除标签',
    message: `确定删除标签「${item.name}」？所有帖子的该标签都会被移除。`,
    confirmText: '删除',
    danger: true,
  }))) return
  await adminApi.deleteTag(item.id)
  toast.success('标签已删除')
  await load()
}

const statusBadge = (value) => {
  if (value === 0) return { label: '待审核', cls: 'badge-warning' }
  if (value === 1) return { label: '已通过', cls: 'badge-success' }
  return { label: '未通过', cls: 'badge-error' }
}
</script>

<template>
  <div class="space-y-4">
    <div class="surface flex flex-wrap items-center gap-2 p-4">
      <div class="mr-auto">
        <h2 class="section-title flex items-center gap-1.5"><Tags class="size-4 ui-text-brand" />标签审核</h2>
        <p class="mt-1 text-xs ui-text-muted">用户发帖时创建的新标签需审核通过后才会在公开页面显示</p>
      </div>
      <button class="btn btn-ghost btn-sm" type="button" @click="load"><RefreshCw class="size-4" />刷新</button>
    </div>

    <div class="surface p-4">
      <div class="flex flex-wrap items-center gap-2">
        <div class="tabs tabs-boxed tabs-sm">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            class="tab"
            :class="status === tab.value ? 'tab-active font-medium' : ''"
            type="button"
            @click="switchStatus(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>
        <form class="join ml-auto" @submit.prevent="page = 1; load()">
          <input v-model="keyword" class="input input-bordered input-sm join-item w-52" placeholder="搜索标签名称" />
          <button class="btn btn-primary btn-sm join-item" type="submit"><Search class="size-4" />搜索</button>
        </form>
      </div>

      <div v-if="loading && !rows.length" class="empty-state">加载中…</div>
      <div v-else-if="!rows.length" class="empty-state">没有匹配的标签</div>
      <div v-else class="mt-4 overflow-x-auto">
        <table class="table table-sm">
          <thead>
            <tr>
              <th>标签</th>
              <th>状态</th>
              <th>使用数</th>
              <th>创建人</th>
              <th>创建时间</th>
              <th>审核信息</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in rows" :key="item.id">
              <td class="font-medium ui-text-strong">{{ item.name }}</td>
              <td>
                <span class="badge badge-sm" :class="statusBadge(item.status).cls">{{ statusBadge(item.status).label }}</span>
              </td>
              <td>{{ item.usageCount }}</td>
              <td class="text-xs">{{ item.creatorName || '存量数据' }}</td>
              <td class="text-xs">{{ formatDateTime(item.createTime) }}</td>
              <td class="max-w-[220px] text-xs ui-text-muted">
                <span v-if="item.rejectReason" class="line-clamp-2 text-rose-500">{{ item.rejectReason }}</span>
                <span v-else-if="item.reviewerName">{{ item.reviewerName }} · {{ formatDateTime(item.reviewTime) }}</span>
                <span v-else>—</span>
              </td>
              <td class="space-x-1 whitespace-nowrap">
                <button v-if="item.status !== 1" class="btn btn-ghost btn-xs text-emerald-600" type="button" @click="approve(item)">
                  <Check class="size-3.5" />通过
                </button>
                <button v-if="item.status !== 2" class="btn btn-ghost btn-xs text-amber-600" type="button" @click="openReject(item)">
                  <XCircle class="size-3.5" />拒绝
                </button>
                <button class="btn btn-ghost btn-xs" type="button" @click="openRename(item)"><SquarePen class="size-3.5" />重命名</button>
                <button class="btn btn-ghost btn-xs text-rose-500" type="button" @click="remove(item)"><Trash2 class="size-3.5" />删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <Pagination :pagination="pagination" class="mt-3" @change="goPage" />
    </div>

    <!-- 拒绝标签 -->
    <dialog v-if="rejectTarget" class="modal modal-open">
      <div class="modal-box max-w-md">
        <div class="mb-3 flex items-center justify-between">
          <h3 class="text-base font-semibold">拒绝标签「{{ rejectTarget.name }}」</h3>
          <button class="btn btn-ghost btn-circle btn-xs" type="button" aria-label="关闭" @click="rejectTarget = null"><X class="size-4" /></button>
        </div>
        <textarea v-model="rejectReason" rows="3" maxlength="200" class="textarea textarea-bordered w-full" placeholder="请说明不通过原因，会通过站内消息通知创建人" />
        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="rejectTarget = null">取消</button>
          <button class="btn btn-error btn-sm text-white" :disabled="saving" type="button" @click="submitReject">{{ saving ? '提交中…' : '确认拒绝' }}</button>
        </div>
      </div>
    </dialog>

    <!-- 重命名标签 -->
    <dialog v-if="renameTarget" class="modal modal-open">
      <div class="modal-box max-w-md">
        <div class="mb-3 flex items-center justify-between">
          <h3 class="text-base font-semibold">重命名标签</h3>
          <button class="btn btn-ghost btn-circle btn-xs" type="button" aria-label="关闭" @click="renameTarget = null"><X class="size-4" /></button>
        </div>
        <input v-model="renameName" maxlength="50" class="input input-bordered input-sm w-full" />
        <p class="mt-2 text-xs ui-text-muted">重命名会同步刷新所有关联帖子的标签展示与搜索索引。</p>
        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="renameTarget = null">取消</button>
          <button class="btn btn-primary btn-sm" :disabled="saving" type="button" @click="submitRename">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </dialog>
  </div>
</template>
