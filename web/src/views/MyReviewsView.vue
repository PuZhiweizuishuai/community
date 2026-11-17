<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  Inbox,
  MessageCircle,
  MessageCircleQuestion,
  RefreshCw,
  ShieldCheck,
  SquarePen,
  TriangleAlert,
  X,
} from '@lucide/vue'
import { reviewApi } from '@/api'
import { useToastStore } from '@/stores/toast'
import Pagination from '@/components/Pagination.vue'
import VditorEditor from '@/components/VditorEditor.vue'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const type = computed(() => route.query.type || 'question')
const status = computed(() => String(route.query.status ?? '0'))
const page = computed(() => route.query.page || '1')

const data = ref(null)
const loading = ref(false)

const editTarget = ref(null)
const editContent = ref('')
const saving = ref(false)

const typeTabs = [
  { value: 'question', label: '我的问题', icon: MessageCircleQuestion },
  { value: 'comment', label: '我的评论', icon: MessageCircle },
]
const statusTabs = [
  { value: '0', label: '待审核' },
  { value: '2', label: '未通过' },
  { value: '-1', label: '全部待处理' },
]

function statusMeta(value) {
  if (Number(value) === 0) return { label: '待审核', cls: 'badge-warning' }
  if (Number(value) === 2) return { label: '未通过', cls: 'badge-error' }
  return { label: '已通过', cls: 'badge-success' }
}

async function load() {
  loading.value = true
  try {
    data.value = await reviewApi.mine({ type: type.value, status: status.value, page: page.value, size: 10 })
  } finally {
    loading.value = false
  }
}
watch([type, status, page], load, { immediate: true })

function updateQuery(patch) {
  router.push({ name: 'my-reviews', query: { ...route.query, page: 1, ...patch } })
}

function openEdit(item) {
  editTarget.value = item
  editContent.value = item.content || ''
}

async function confirmEdit() {
  if (!editTarget.value) return
  if (!String(editContent.value || '').trim()) return toast.warning('评论内容不能为空')
  saving.value = true
  try {
    await reviewApi.resubmitComment(editTarget.value.id, editContent.value)
    toast.success('已重新提交审核，通过后将公开展示')
    editTarget.value = null
    await load()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(260px,1fr)]">
      <div class="min-w-0 space-y-5">
        <!-- 页头 -->
        <div class="surface relative overflow-hidden p-5">
          <div class="pointer-events-none absolute -right-16 -top-20 size-48 rounded-full bg-gradient-to-br from-rose-500/15 to-orange-500/10 blur-2xl" />
          <div class="relative flex items-start gap-3">
            <span class="grid size-11 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-rose-500 to-orange-500 text-white shadow-lg shadow-rose-500/20">
              <ShieldCheck class="size-5" />
            </span>
            <div class="min-w-0 flex-1">
              <h1 class="text-lg font-bold ui-text-strong">我的审核</h1>
              <p class="mt-0.5 text-sm ui-text-base">查看待审核与被驳回的内容；未通过的内容可按管理员建议修改后重新提交</p>
            </div>
            <button class="btn btn-ghost btn-sm" type="button" :disabled="loading" @click="load">
              <RefreshCw class="size-4" :class="loading ? 'animate-spin' : ''" />刷新
            </button>
          </div>
        </div>

        <!-- 筛选 -->
        <div class="surface flex flex-wrap items-center gap-3 p-3">
          <div class="flex gap-1 rounded-lg ui-bg-soft p-1">
            <button
              v-for="tab in typeTabs"
              :key="tab.value"
              class="flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition"
              :class="type === tab.value ? 'bg-white shadow-sm ui-text-brand' : 'ui-text-base hover:ui-text-strong'"
              type="button"
              @click="updateQuery({ type: tab.value })"
            >
              <component :is="tab.icon" class="size-4" />{{ tab.label }}
            </button>
          </div>
          <div class="flex gap-1">
            <button
              v-for="tab in statusTabs"
              :key="tab.value"
              class="btn btn-xs"
              :class="status === tab.value ? 'btn-primary' : 'btn-ghost'"
              type="button"
              @click="updateQuery({ status: tab.value })"
            >
              {{ tab.label }}
            </button>
          </div>
        </div>

        <!-- 列表 -->
        <div class="surface p-4 sm:p-5">
          <div v-if="loading && !data" class="empty-state">正在加载…</div>
          <div v-else-if="!(data?.data || []).length" class="empty-state">
            <Inbox class="size-10 ui-text-muted" :stroke-width="1.5" />
            <p class="mt-2">{{ status === '0' ? '没有待审核的内容～' : '这里还没有记录' }}</p>
          </div>
          <ul v-else class="space-y-3">
            <li v-for="item in data.data" :key="`${item.type}-${item.id}`" class="rounded-xl border ui-border p-4">
              <div class="flex flex-wrap items-center gap-2">
                <span class="badge badge-sm ui-bg-soft ui-text-base">{{ item.type === 'question' ? '问题' : '评论' }}</span>
                <span class="badge badge-sm" :class="statusMeta(item.reviewStatus).cls">{{ statusMeta(item.reviewStatus).label }}</span>
                <span class="text-xs ui-text-muted">{{ item.createTime }}</span>
                <span v-if="item.reviewTime" class="text-xs ui-text-muted">审核于 {{ item.reviewTime }}</span>
              </div>

              <template v-if="item.type === 'question'">
                <RouterLink :to="`/question/${item.id}`" class="mt-2 block text-base font-semibold ui-text-strong hover:text-indigo-600">
                  {{ item.title }}
                </RouterLink>
                <div class="mt-1 flex flex-wrap items-center gap-2 text-xs ui-text-muted">
                  <span class="badge badge-outline badge-sm">{{ item.classification }}</span>
                  <span>{{ item.tag }}</span>
                </div>
              </template>
              <template v-else>
                <p class="mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed ui-text-strong">{{ item.content }}</p>
                <RouterLink :to="`/question/${item.questionId}`" class="mt-1.5 inline-flex items-center gap-1 text-xs ui-text-brand">
                  所属问题：{{ item.questionTitle || '问题已删除' }}<ArrowRight class="size-3" />
                </RouterLink>
              </template>

              <div v-if="item.reviewReason || item.reviewSuggestion" class="mt-3 rounded-lg border border-rose-200 bg-rose-50 p-3 text-xs leading-relaxed text-rose-700">
                <p class="flex items-center gap-1.5 font-semibold"><TriangleAlert class="size-3.5" />审核未通过</p>
                <p v-if="item.reviewReason" class="mt-1.5"><b>原因：</b>{{ item.reviewReason }}</p>
                <p v-if="item.reviewSuggestion" class="mt-1"><b>修改建议：</b>{{ item.reviewSuggestion }}</p>
              </div>

              <div class="mt-3 flex flex-wrap items-center gap-2">
                <RouterLink v-if="item.type === 'question'" :to="`/publish/${item.id}`" class="btn btn-primary btn-xs">
                  <SquarePen class="size-3.5" />修改并重新提交
                </RouterLink>
                <button v-else class="btn btn-primary btn-xs" type="button" @click="openEdit(item)">
                  <SquarePen class="size-3.5" />修改并重新提交
                </button>
                <RouterLink :to="`/question/${item.questionId || item.id}`" class="btn btn-ghost btn-xs">查看原帖</RouterLink>
              </div>
            </li>
          </ul>
          <Pagination :pagination="data" @change="updateQuery({ page: $event })" />
        </div>
      </div>

      <aside class="space-y-4">
        <section class="surface p-4 text-sm">
          <h3 class="mb-2 flex items-center gap-1.5 font-semibold ui-text-strong"><ShieldCheck class="size-4 ui-text-brand" />审核说明</h3>
          <ul class="list-inside list-disc space-y-1.5 text-xs leading-relaxed ui-text-base">
            <li>开启内容审核后，新发布或修改的问题与评论需要管理员审核。</li>
            <li>审核通过后内容会自动公开展示，并发送消息通知。</li>
            <li>审核未通过时，请按「原因」与「修改建议」调整后重新提交。</li>
            <li>修改问题会重新进入审核，审核期间其他用户暂时无法查看。</li>
          </ul>
        </section>
        <section class="surface p-4 text-sm">
          <h3 class="mb-2 font-semibold ui-text-strong">快捷导航</h3>
          <div class="space-y-1 text-xs">
            <RouterLink to="/messages?type=5" class="link-muted block">消息通知</RouterLink>
            <RouterLink to="/publish" class="link-muted block">发起讨论</RouterLink>
          </div>
        </section>
      </aside>
    </div>

    <!-- 修改评论 -->
    <dialog v-if="editTarget" class="modal modal-open">
      <div class="modal-box max-w-2xl">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h3 class="text-base font-semibold ui-text-strong">修改评论</h3>
            <p class="mt-0.5 text-xs ui-text-muted">所属问题：{{ editTarget.questionTitle || '问题已删除' }}</p>
          </div>
          <button class="btn btn-ghost btn-circle btn-sm" type="button" aria-label="关闭" @click="editTarget = null"><X class="size-4" /></button>
        </div>
        <div v-if="editTarget.reviewReason || editTarget.reviewSuggestion" class="mt-3 rounded-lg border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
          <p v-if="editTarget.reviewReason"><b>原因：</b>{{ editTarget.reviewReason }}</p>
          <p v-if="editTarget.reviewSuggestion" class="mt-1"><b>修改建议：</b>{{ editTarget.reviewSuggestion }}</p>
        </div>
        <div class="mt-3">
          <VditorEditor v-model="editContent" :height="260" placeholder="修改你的评论…" />
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="editTarget = null">取消</button>
          <button class="btn btn-primary btn-sm" type="button" :disabled="saving" @click="confirmEdit">
            <RefreshCw v-if="saving" class="size-4 animate-spin" />
            <SquarePen v-else class="size-4" />{{ saving ? '提交中…' : '重新提交审核' }}
          </button>
        </div>
      </div>
    </dialog>
  </div>
</template>
