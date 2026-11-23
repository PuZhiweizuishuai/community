<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CheckCircle2,
  Eye,
  Inbox,
  MessageCircle,
  MessageCircleQuestion,
  RefreshCw,
  Search,
  ShieldCheck,
  Tags,
  TriangleAlert,
  X,
  XCircle,
} from '@lucide/vue'
import { adminApi, assetUrl } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useToastStore } from '@/stores/toast'
import { useMetaStore } from '@/stores/meta'
import MarkdownView from '@/components/MarkdownView.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()
const confirm = useConfirmStore()
const meta = useMetaStore()

const type = computed(() => route.query.type || 'question')
const status = computed(() => String(route.query.status ?? '0'))
const page = computed(() => route.query.page || '1')
const keyword = ref(String(route.query.keyword || ''))

const data = ref(null)
const counts = ref({ question: 0, comment: 0, topic: 0, total: 0 })
const loading = ref(false)
const busyId = ref(null)

const detail = ref(null)
const detailLoading = ref(false)

const rejectTarget = ref(null)
const rejecting = ref(false)
const rejectForm = reactive({ reason: '', suggestion: '' })

const typeTabs = [
  { value: 'question', label: '问题', icon: MessageCircleQuestion },
  { value: 'comment', label: '评论', icon: MessageCircle },
  { value: 'topic', label: '话题', icon: Tags },
]
const statusTabs = [
  { value: '0', label: '待审核' },
  { value: '2', label: '未通过' },
  { value: '1', label: '已通过' },
  { value: '-1', label: '全部' },
]

function statusMeta(value) {
  if (Number(value) === 0) return { label: '待审核', cls: 'badge-warning' }
  if (Number(value) === 2) return { label: '未通过', cls: 'badge-error' }
  return { label: '已通过', cls: 'badge-success' }
}

const typeLabel = (value) => ({ question: '问题', comment: '评论', topic: '话题' })[value] || '内容'
const visibilityLabel = (value) =>
  Number(value) === 2 ? '私有 · 仅邀请' : Number(value) === 1 ? '私有 · 可申请' : '公开'
const topicTypeLabel = (value) =>
  meta.topicTypes.find((item) => Number(item.value) === Number(value))?.label || '未分类'
const searchPlaceholder = computed(() => {
  if (type.value === 'question') return '搜索问题标题'
  if (type.value === 'comment') return '搜索评论内容'
  return '搜索话题名称 / 简介'
})
const isTopicReject = computed(() => rejectTarget.value?.type === 'topic')

async function load() {
  loading.value = true
  try {
    const [list, summary] = await Promise.all([
      adminApi.reviews({ type: type.value, status: status.value, keyword: keyword.value.trim(), page: page.value, size: 20 }),
      adminApi.reviewCount(),
    ])
    data.value = list
    counts.value = { question: 0, comment: 0, topic: 0, total: 0, ...(summary || {}) }
  } finally {
    loading.value = false
  }
}
watch([type, status, page], load, { immediate: true })
watch(
  () => route.query.keyword,
  (value) => {
    keyword.value = String(value || '')
    load()
  },
)

function updateQuery(patch) {
  const next = { ...route.query, page: 1, ...patch }
  if (!next.keyword) delete next.keyword
  router.push({ name: 'admin-reviews', query: next })
}

function submitSearch() {
  updateQuery({ keyword: keyword.value.trim() || '' })
}

async function openDetail(item) {
  detailLoading.value = true
  detail.value = { item, contents: [] }
  try {
    detail.value = await adminApi.reviewDetail(item.type, item.id)
  } finally {
    detailLoading.value = false
  }
}

async function approve(item) {
  const name = typeLabel(item.type)
  const extra = item.type === 'topic' ? '，创建者将自动成为吧主' : ''
  if (!(await confirm.ask({ title: '通过审核', message: `确认通过该${name}？通过后将立即公开展示${extra}。`, confirmText: '通过' }))) return
  busyId.value = `${item.type}-${item.id}`
  try {
    await adminApi.approveReview(item.type, item.id)
    toast.success('审核已通过')
    detail.value = null
    await load()
  } finally {
    busyId.value = null
  }
}

function openReject(item) {
  rejectTarget.value = item
  rejectForm.reason = ''
  rejectForm.suggestion = ''
  detail.value = null
}

async function confirmReject() {
  if (!rejectTarget.value) return
  const reason = rejectForm.reason.trim()
  if (!reason) return toast.warning('请填写审核不通过原因')
  rejecting.value = true
  try {
    await adminApi.rejectReview({
      type: rejectTarget.value.type,
      id: rejectTarget.value.id,
      reason,
      suggestion: isTopicReject.value ? '' : rejectForm.suggestion.trim(),
    })
    toast.success('已驳回并通知作者')
    rejectTarget.value = null
    detail.value = null
    await load()
  } finally {
    rejecting.value = false
  }
}
</script>

<template>
  <div class="space-y-5">
    <!-- 页头 -->
    <div class="surface relative overflow-hidden p-5 sm:p-6">
      <div class="pointer-events-none absolute -right-20 -top-24 size-56 rounded-full bg-gradient-to-br from-rose-500/15 to-orange-500/10 blur-2xl" />
      <div class="relative flex flex-wrap items-start justify-between gap-4">
        <div class="flex items-start gap-3">
          <span class="grid size-11 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-rose-500 to-orange-500 text-white shadow-lg shadow-rose-500/25">
            <ShieldCheck class="size-5" />
          </span>
          <div>
            <h1 class="text-lg font-bold ui-text-strong">内容审核</h1>
            <p class="mt-0.5 text-sm ui-text-base">统一审核用户发布的问题、评论与新建话题，通过后才对其他用户可见；驳回时请填写原因</p>
          </div>
        </div>
        <button class="btn btn-ghost btn-sm" type="button" :disabled="loading" @click="load">
          <RefreshCw class="size-4" :class="loading ? 'animate-spin' : ''" />刷新
        </button>
      </div>
    </div>

    <!-- 待审概览 -->
    <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      <div class="surface flex items-center justify-between gap-3 p-4">
        <div>
          <p class="text-xs font-medium ui-text-muted">待审核问题</p>
          <p class="mt-1 text-2xl font-bold" :class="counts.question ? 'text-amber-600' : 'ui-text-strong'">{{ counts.question }}</p>
        </div>
        <span class="grid size-11 place-items-center rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white"><MessageCircleQuestion class="size-5" /></span>
      </div>
      <div class="surface flex items-center justify-between gap-3 p-4">
        <div>
          <p class="text-xs font-medium ui-text-muted">待审核评论</p>
          <p class="mt-1 text-2xl font-bold" :class="counts.comment ? 'text-amber-600' : 'ui-text-strong'">{{ counts.comment }}</p>
        </div>
        <span class="grid size-11 place-items-center rounded-xl bg-gradient-to-br from-sky-500 to-cyan-500 text-white"><MessageCircle class="size-5" /></span>
      </div>
      <div class="surface flex items-center justify-between gap-3 p-4">
        <div>
          <p class="text-xs font-medium ui-text-muted">待审核话题</p>
          <p class="mt-1 text-2xl font-bold" :class="counts.topic ? 'text-amber-600' : 'ui-text-strong'">{{ counts.topic }}</p>
        </div>
        <span class="grid size-11 place-items-center rounded-xl bg-gradient-to-br from-violet-500 to-fuchsia-500 text-white"><Tags class="size-5" /></span>
      </div>
      <div class="surface flex items-center justify-between gap-3 p-4">
        <div>
          <p class="text-xs font-medium ui-text-muted">待审核合计</p>
          <p class="mt-1 text-2xl font-bold ui-text-brand">{{ counts.total }}</p>
        </div>
        <span class="grid size-11 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-500 text-white"><ShieldCheck class="size-5" /></span>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="surface p-4">
      <div class="flex flex-wrap items-center gap-3">
        <div class="flex gap-1 rounded-lg ui-bg-soft p-1">
          <button
            v-for="tab in typeTabs"
            :key="tab.value"
            class="flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition"
            :class="type === tab.value ? 'ui-bg shadow-sm ui-text-brand' : 'ui-text-base hover:ui-text-strong'"
            type="button"
            @click="updateQuery({ type: tab.value, keyword: '' })"
          >
            <component :is="tab.icon" class="size-4" />{{ tab.label }}
            <span v-if="counts[tab.value]" class="badge badge-warning badge-xs">{{ counts[tab.value] }}</span>
          </button>
        </div>
        <div class="flex flex-wrap gap-1">
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
        <form class="join ml-auto" @submit.prevent="submitSearch">
          <input v-model="keyword" class="input input-bordered input-sm join-item w-44 sm:w-56" :placeholder="searchPlaceholder" />
          <button class="btn btn-sm btn-primary join-item" type="submit"><Search class="size-4" />搜索</button>
        </form>
      </div>
    </div>

    <!-- 列表 -->
    <section class="surface overflow-hidden">
      <div class="overflow-x-auto">
        <table class="table table-sm">
          <thead>
            <tr>
              <th class="w-16">ID</th>
              <th>{{ type === 'question' ? '问题标题' : type === 'comment' ? '评论内容' : '话题名称' }}</th>
              <th class="w-36">作者</th>
              <th class="w-44">{{ type === 'question' ? '分类 / 标签' : type === 'comment' ? '所属问题' : '分类 / 可见性' }}</th>
              <th class="w-40">提交时间</th>
              <th class="w-24">状态</th>
              <th class="w-52 text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in data?.data || []" :key="`${item.type}-${item.id}`" class="hover:bg-slate-50">
              <td class="text-xs ui-text-muted">{{ item.id }}</td>
              <td>
                <p class="line-clamp-2 max-w-md text-sm font-medium ui-text-strong">{{ item.title || '（无内容）' }}</p>
                <p v-if="item.type === 'topic'" class="mt-0.5 line-clamp-1 text-xs ui-text-muted">{{ item.description || '暂无话题简介' }}</p>
                <p v-if="item.reviewReason" class="mt-0.5 line-clamp-1 text-xs text-rose-500">驳回原因：{{ item.reviewReason }}</p>
              </td>
              <td>
                <RouterLink
                  v-if="item.authorUserId"
                  :to="`/user/${item.authorUserId}`"
                  class="line-clamp-1 text-xs hover:text-indigo-600"
                >
                  {{ item.authorName || '未知用户' }}
                </RouterLink>
                <span v-else class="text-xs ui-text-muted">系统</span>
              </td>
              <td class="text-xs ui-text-base">
                <template v-if="item.type === 'question'">
                  <span class="badge badge-outline badge-sm">{{ meta.classificationLabel(item.classification) }}</span>
                  <span class="mt-1 block truncate ui-text-muted">{{ item.tag }}</span>
                </template>
                <template v-else-if="item.type === 'topic'">
                  <span class="badge badge-outline badge-sm">{{ topicTypeLabel(item.topicType) }}</span>
                  <span class="mt-1 block text-xs ui-text-muted">{{ visibilityLabel(item.visibility) }}</span>
                </template>
                <RouterLink v-else :to="`/question/${item.questionId}`" class="line-clamp-2 hover:text-indigo-600">{{ item.questionTitle || '问题已删除' }}</RouterLink>
              </td>
              <td class="whitespace-nowrap text-xs ui-text-base">{{ item.createTime }}</td>
              <td>
                <span class="badge badge-sm" :class="statusMeta(item.reviewStatus).cls">{{ statusMeta(item.reviewStatus).label }}</span>
              </td>
              <td>
                <div class="flex items-center justify-end gap-1">
                  <button class="btn btn-ghost btn-xs ui-text-brand" type="button" @click="openDetail(item)">
                    <Eye class="size-3.5" />查看
                  </button>
                  <template v-if="Number(item.reviewStatus) === 0">
                    <button class="btn btn-ghost btn-xs text-emerald-600" type="button" :disabled="busyId === `${item.type}-${item.id}`" @click="approve(item)">
                      <CheckCircle2 class="size-3.5" />通过
                    </button>
                    <button class="btn btn-ghost btn-xs text-rose-500" type="button" @click="openReject(item)">
                      <XCircle class="size-3.5" />驳回
                    </button>
                  </template>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && !(data?.data || []).length">
              <td colspan="7">
                <div class="flex flex-col items-center py-12 text-center text-sm ui-text-muted">
                  <Inbox class="size-9 ui-text-muted" :stroke-width="1.5" />
                  <p class="mt-2">{{ status === '0' ? '太棒了，当前没有待审核内容' : '没有符合条件的内容' }}</p>
                </div>
              </td>
            </tr>
            <tr v-if="loading">
              <td colspan="7" class="py-10 text-center text-sm ui-text-muted">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="px-4 pb-2">
        <Pagination :pagination="data" @change="updateQuery({ page: $event })" />
      </div>
    </section>

    <!-- 详情 -->
    <dialog v-if="detail" class="modal modal-open">
      <div class="modal-box max-w-3xl">
        <div class="flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span class="grid size-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-rose-500 to-orange-500 text-white">
              <ShieldCheck class="size-5" />
            </span>
            <div>
              <h3 class="text-base font-semibold ui-text-strong">审核{{ typeLabel(detail.item.type) }} #{{ detail.item.id }}</h3>
              <p class="mt-0.5 text-xs ui-text-muted">
                {{ detail.item.authorName || '未知用户' }} · {{ detail.item.createTime }}
                <span class="badge badge-sm ml-1" :class="statusMeta(detail.item.reviewStatus).cls">{{ statusMeta(detail.item.reviewStatus).label }}</span>
              </p>
            </div>
          </div>
          <button class="btn btn-ghost btn-circle btn-sm" type="button" aria-label="关闭" @click="detail = null"><X class="size-4" /></button>
        </div>

        <div v-if="detailLoading" class="py-10 text-center text-sm ui-text-muted">加载中…</div>
        <div v-else class="mt-4 max-h-[55vh] space-y-4 overflow-y-auto pr-1">
          <template v-if="detail.item.type === 'topic'">
            <div class="rounded-xl border ui-border p-4">
              <div class="flex items-start gap-3">
                <img
                  :src="assetUrl(detail.item.image)"
                  alt="话题图标"
                  class="h-14 w-14 shrink-0 rounded-xl object-cover ui-bg-soft"
                />
                <div class="min-w-0">
                  <h4 class="text-lg font-bold ui-text-strong">{{ detail.item.title }}</h4>
                  <div class="mt-2 flex flex-wrap items-center gap-2 text-xs">
                    <span class="badge badge-outline badge-sm">{{ topicTypeLabel(detail.item.topicType) }}</span>
                    <span
                      class="badge badge-sm"
                      :class="Number(detail.item.visibility) === 0 ? 'badge-ghost' : 'badge-warning'"
                    >
                      {{ visibilityLabel(detail.item.visibility) }}
                    </span>
                  </div>
                </div>
              </div>
              <p class="mt-3 whitespace-pre-line text-sm leading-relaxed ui-text-base">
                {{ detail.item.description || '暂无话题简介' }}
              </p>
              <RouterLink :to="`/topics/${detail.item.id}`" class="btn btn-ghost btn-xs mt-3">
                <Eye class="size-3.5" />查看话题主页
              </RouterLink>
            </div>
          </template>
          <template v-else-if="detail.item.type === 'question'">
            <div class="rounded-xl border ui-border p-4">
              <h4 class="text-lg font-bold ui-text-strong">{{ detail.item.title }}</h4>
              <div class="mt-2 flex flex-wrap items-center gap-2 text-xs ui-text-muted">
                <span class="badge badge-outline badge-sm">{{ meta.classificationLabel(detail.item.classification) }}</span>
                <span>{{ detail.item.tag }}</span>
              </div>
            </div>
            <div v-for="(segment, index) in detail.contents || []" :key="segment.contentId || index" class="rounded-xl border ui-border p-4">
              <p class="mb-2 text-xs ui-text-muted">段落 {{ index + 1 }} · {{ ['普通文本', '密码可见', '回复可见'][segment.type] || '内容' }}</p>
              <MarkdownView :content="segment.content || ''" />
            </div>
            <p v-if="!(detail.contents || []).length" class="rounded-xl border border-dashed ui-border p-6 text-center text-sm ui-text-muted">没有正文内容</p>
          </template>
          <template v-else>
            <RouterLink :to="`/question/${detail.item.questionId}`" class="block rounded-xl border ui-border p-3 text-sm ui-text-brand hover:bg-slate-50">
              所属问题：{{ detail.item.questionTitle || '问题已删除' }}
            </RouterLink>
            <div class="rounded-xl border ui-border p-4">
              <p class="mb-2 text-xs ui-text-muted">评论内容</p>
              <MarkdownView :content="detail.item.content || ''" />
            </div>
          </template>

          <div v-if="detail.item.reviewReason || detail.item.reviewSuggestion" class="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm">
            <p class="flex items-center gap-1.5 font-semibold text-rose-600"><TriangleAlert class="size-4" />审核记录</p>
            <p v-if="detail.item.reviewReason" class="mt-2 text-rose-700">原因：{{ detail.item.reviewReason }}</p>
            <p v-if="detail.item.reviewSuggestion" class="mt-1 text-rose-700">修改意见：{{ detail.item.reviewSuggestion }}</p>
            <p class="mt-1 text-xs text-rose-500">审核人：{{ detail.item.reviewerName || '管理员' }} · {{ detail.item.reviewTime }}</p>
          </div>
        </div>

        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="detail = null">关闭</button>
          <template v-if="Number(detail.item.reviewStatus) === 0">
            <button class="btn btn-error btn-sm" type="button" @click="openReject(detail.item)">
              <XCircle class="size-4" />驳回
            </button>
            <button class="btn btn-success btn-sm text-white" type="button" :disabled="busyId === `${detail.item.type}-${detail.item.id}`" @click="approve(detail.item)">
              <CheckCircle2 class="size-4" />通过
            </button>
          </template>
        </div>
      </div>
    </dialog>

    <!-- 驳回 -->
    <dialog v-if="rejectTarget" class="modal modal-open">
      <div class="modal-box max-w-lg">
        <div class="flex items-start gap-3">
          <span class="grid size-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-rose-500 to-red-500 text-white"><XCircle class="size-5" /></span>
          <div class="min-w-0">
            <h3 class="text-base font-semibold ui-text-strong">驳回{{ typeLabel(rejectTarget.type) }}</h3>
            <p class="mt-1 line-clamp-2 text-sm ui-text-base">「{{ rejectTarget.title }}」</p>
          </div>
        </div>
        <label class="form-control mt-4">
          <span class="label-text mb-1 text-xs font-medium ui-text-base">审核不通过原因 <span class="text-rose-500">*</span></span>
          <textarea
            v-model="rejectForm.reason"
            rows="3"
            :maxlength="isTopicReject ? 200 : 500"
            class="textarea textarea-bordered w-full text-sm focus:outline-none"
            :placeholder="isTopicReject ? '例如：话题名称或简介不符合社区规范' : '例如：内容包含广告推广信息，不符合社区规范'"
          />
          <span class="mt-1 text-right text-xs ui-text-muted">{{ rejectForm.reason.length }} / {{ isTopicReject ? 200 : 500 }}</span>
        </label>
        <label v-if="!isTopicReject" class="form-control mt-2">
          <span class="label-text mb-1 text-xs font-medium ui-text-base">修改意见（选填）</span>
          <textarea v-model="rejectForm.suggestion" rows="3" maxlength="500" class="textarea textarea-bordered w-full text-sm focus:outline-none" placeholder="告诉作者如何修改才能通过审核，例如：请删除推广链接并补充问题背景" />
          <span class="mt-1 text-right text-xs ui-text-muted">{{ rejectForm.suggestion.length }} / 500</span>
        </label>
        <p class="mt-2 text-xs ui-text-muted">
          {{ isTopicReject
            ? '驳回后话题创建者会在消息中心收到原因，可调整后重新发起话题申请。'
            : '驳回后作者会在消息中心收到原因与修改意见，可修改后重新提交审核。' }}
        </p>
        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="rejectTarget = null">取消</button>
          <button class="btn btn-error btn-sm" type="button" :disabled="rejecting" @click="confirmReject">
            <RefreshCw v-if="rejecting" class="size-4 animate-spin" />
            <XCircle v-else class="size-4" />{{ rejecting ? '提交中…' : '确认驳回' }}
          </button>
        </div>
      </div>
    </dialog>
  </div>
</template>
