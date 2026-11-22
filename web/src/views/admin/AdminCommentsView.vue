<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowUpRight,
  Eye,
  Inbox,
  MessageCircle,
  RefreshCw,
  RotateCcw,
  Search,
  Trash2,
  X,
} from '@lucide/vue'
import { adminApi } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useMetaStore } from '@/stores/meta'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/Avatar.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()
const meta = useMetaStore()
const toast = useToastStore()
const confirm = useConfirmStore()

const status = computed(() => String(route.query.status ?? '-1'))
const page = computed(() => route.query.page || '1')
const keywordQuery = computed(() => String(route.query.keyword || ''))
const keyword = ref(keywordQuery.value)

const data = ref(null)
const loading = ref(false)
const busyId = ref(null)
const detail = ref(null)
const detailLoading = ref(false)

const statusTabs = [
  { value: '-1', label: '全部评论' },
  { value: '1', label: '正常' },
  { value: '0', label: '已删除' },
]

function statusMeta(value) {
  return Number(value) === 1
    ? { label: '正常', cls: 'badge-success' }
    : { label: '已删除', cls: 'badge-error' }
}

function reviewMeta(value) {
  if (Number(value) === 0) return { label: '待审核', cls: 'badge-warning' }
  if (Number(value) === 2) return { label: '未通过', cls: 'badge-error' }
  return { label: '已通过', cls: 'badge-success' }
}

function typeLabel(item) {
  return Number(item?.type) === 1 ? '评论问题' : '回复评论'
}

async function load() {
  loading.value = true
  try {
    data.value = await adminApi.comments({
      status: status.value,
      keyword: keywordQuery.value,
      page: page.value,
      size: 20,
    })
  } finally {
    loading.value = false
  }
}
watch([status, page, keywordQuery], load, { immediate: true })
watch(keywordQuery, (value) => {
  keyword.value = value
})

function updateQuery(patch) {
  const next = { ...route.query, page: 1, ...patch }
  if (!next.keyword) delete next.keyword
  if (next.status === '-1') delete next.status
  router.push({ name: 'admin-comments', query: next })
}

function submitSearch() {
  updateQuery({ keyword: keyword.value.trim() || '' })
}

async function openDetail(item) {
  detail.value = { comment: item, question: null, parentComment: null, rootComment: null }
  detailLoading.value = true
  try {
    detail.value = await adminApi.commentDetail(item.commentId)
  } finally {
    detailLoading.value = false
  }
}

async function remove(item) {
  const message =
    Number(item.type) === 1
      ? '删除后该一级评论及其回复线程将不再公开展示，可在「已删除」中恢复。'
      : '删除后该回复将不再公开展示，可在「已删除」中恢复。'
  if (!(await confirm.ask({ title: '删除评论', message, confirmText: '删除', danger: true })))
    return
  busyId.value = item.commentId
  try {
    await adminApi.deleteComment(item.commentId)
    toast.success('评论已删除')
    detail.value = null
    await load()
  } finally {
    busyId.value = null
  }
}

async function recover(item) {
  if (
    !(await confirm.ask({
      title: '恢复评论',
      message: '确认恢复该评论？恢复后是否公开展示仍取决于审核状态。',
      confirmText: '恢复',
    }))
  )
    return
  busyId.value = item.commentId
  try {
    await adminApi.recoverComment(item.commentId)
    toast.success('评论已恢复')
    detail.value = null
    await load()
  } finally {
    busyId.value = null
  }
}
</script>

<template>
  <div class="space-y-5">
    <div class="surface relative overflow-hidden p-5 sm:p-6">
      <div
        class="pointer-events-none absolute -right-20 -top-24 size-56 rounded-full bg-gradient-to-br from-violet-500/15 to-fuchsia-500/10 blur-2xl"
      />
      <div class="relative flex flex-wrap items-start justify-between gap-4">
        <div class="flex items-start gap-3">
          <span
            class="grid size-11 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-violet-500 to-fuchsia-500 text-white shadow-lg shadow-violet-500/25"
          >
            <MessageCircle class="size-5" />
          </span>
          <div>
            <h1 class="text-lg font-bold ui-text-strong">评论管理</h1>
            <p class="mt-0.5 text-sm ui-text-base">
              查看社区全部评论（含已删除），可删除或恢复评论，并查看所属问题与回复上下文
            </p>
          </div>
        </div>
        <button class="btn btn-ghost btn-sm" type="button" :disabled="loading" @click="load">
          <RefreshCw class="size-4" :class="loading ? 'animate-spin' : ''" />刷新
        </button>
      </div>
    </div>

    <div class="surface p-4">
      <div class="flex flex-wrap items-center gap-3">
        <div class="flex gap-1 rounded-lg ui-bg-soft p-1">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            class="rounded-md px-3 py-1.5 text-sm font-medium transition"
            :class="
              status === tab.value
                ? 'ui-bg shadow-sm ui-text-brand'
                : 'ui-text-base hover:ui-text-strong'
            "
            type="button"
            @click="updateQuery({ status: tab.value })"
          >
            {{ tab.label }}
          </button>
        </div>
        <span class="text-xs ui-text-muted">共 {{ data?.allCount ?? 0 }} 条评论</span>
        <form class="join ml-auto" @submit.prevent="submitSearch">
          <input
            v-model="keyword"
            class="input input-bordered input-sm join-item w-44 sm:w-60"
            placeholder="搜索评论内容"
          />
          <button class="btn btn-sm btn-primary join-item" type="submit">
            <Search class="size-4" />搜索
          </button>
        </form>
      </div>
    </div>

    <section class="surface overflow-hidden">
      <div class="overflow-x-auto">
        <table class="table table-sm">
          <thead>
            <tr>
              <th class="w-16">ID</th>
              <th>评论内容</th>
              <th class="w-32">作者</th>
              <th class="w-56">所属问题</th>
              <th class="w-20">审核</th>
              <th class="w-20">状态</th>
              <th class="w-40">评论时间</th>
              <th class="w-44 text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in data?.data || []"
              :key="item.commentId"
              :class="Number(item.status) === 0 ? 'opacity-70' : 'hover:bg-slate-50'"
            >
              <td class="text-xs ui-text-muted">{{ item.commentId }}</td>
              <td>
                <p class="line-clamp-2 max-w-md text-sm font-medium ui-text-strong">
                  {{ item.content || '（无内容）' }}
                </p>
                <div class="mt-1 flex flex-wrap items-center gap-1.5 text-xs ui-text-muted">
                  <span class="badge badge-outline badge-xs">{{ typeLabel(item) }}</span>
                  <span v-if="Number(item.type) === 2">
                    回复 @{{ item.parentAuthorName || '未知' }}
                  </span>
                  <span v-if="Number(item.commentCount) > 0">· {{ item.commentCount }} 条回复</span>
                </div>
              </td>
              <td>
                <RouterLink
                  v-if="item.authorUserId"
                  :to="`/user/${item.authorUserId}`"
                  class="line-clamp-1 text-xs hover:text-indigo-600"
                >
                  {{ item.authorName || '未知用户' }}
                </RouterLink>
                <span v-else class="text-xs ui-text-muted">{{
                  item.authorName || '未知用户'
                }}</span>
              </td>
              <td>
                <RouterLink
                  :to="`/question/${item.questionId}`"
                  class="line-clamp-2 text-xs hover:text-indigo-600"
                >
                  {{ item.questionTitle || '问题不存在' }}
                </RouterLink>
                <span
                  v-if="Number(item.questionStatus) === 0"
                  class="badge badge-ghost badge-xs mt-1"
                >
                  问题已删除
                </span>
              </td>
              <td>
                <span class="badge badge-sm" :class="reviewMeta(item.reviewStatus).cls">
                  {{ reviewMeta(item.reviewStatus).label }}
                </span>
              </td>
              <td>
                <span class="badge badge-sm" :class="statusMeta(item.status).cls">
                  {{ statusMeta(item.status).label }}
                </span>
              </td>
              <td class="whitespace-nowrap text-xs ui-text-base">{{ item.createTime }}</td>
              <td>
                <div class="flex items-center justify-end gap-1">
                  <button
                    class="btn btn-ghost btn-xs ui-text-brand"
                    type="button"
                    @click="openDetail(item)"
                  >
                    <Eye class="size-3.5" />查看
                  </button>
                  <button
                    v-if="Number(item.status) === 1"
                    class="btn btn-ghost btn-xs text-rose-500"
                    type="button"
                    :disabled="busyId === item.commentId"
                    @click="remove(item)"
                  >
                    <Trash2 class="size-3.5" />删除
                  </button>
                  <button
                    v-else
                    class="btn btn-ghost btn-xs text-emerald-600"
                    type="button"
                    :disabled="busyId === item.commentId"
                    @click="recover(item)"
                  >
                    <RotateCcw class="size-3.5" />恢复
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && !(data?.data || []).length">
              <td colspan="8">
                <div class="flex flex-col items-center py-12 text-center text-sm ui-text-muted">
                  <Inbox class="size-9 ui-text-muted" :stroke-width="1.5" />
                  <p class="mt-2">
                    {{ status === '0' ? '当前没有已删除的评论' : '没有符合条件的评论' }}
                  </p>
                </div>
              </td>
            </tr>
            <tr v-if="loading">
              <td colspan="8" class="py-10 text-center text-sm ui-text-muted">加载中…</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="px-4 pb-2">
        <Pagination :pagination="data" @change="updateQuery({ page: $event })" />
      </div>
    </section>

    <dialog v-if="detail" class="modal modal-open">
      <div class="modal-box max-w-3xl">
        <div class="flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span
              class="grid size-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-violet-500 to-fuchsia-500 text-white"
            >
              <MessageCircle class="size-5" />
            </span>
            <div>
              <h3 class="text-base font-semibold ui-text-strong">
                评论详情 #{{ detail.comment?.commentId }}
              </h3>
              <div class="mt-1 flex flex-wrap items-center gap-1.5">
                <span class="badge badge-sm" :class="statusMeta(detail.comment?.status).cls">
                  {{ statusMeta(detail.comment?.status).label }}
                </span>
                <span class="badge badge-sm" :class="reviewMeta(detail.comment?.reviewStatus).cls">
                  {{ reviewMeta(detail.comment?.reviewStatus).label }}
                </span>
                <span class="badge badge-outline badge-sm">{{ typeLabel(detail.comment) }}</span>
              </div>
            </div>
          </div>
          <button
            class="btn btn-ghost btn-circle btn-sm"
            type="button"
            aria-label="关闭"
            @click="detail = null"
          >
            <X class="size-4" />
          </button>
        </div>

        <div v-if="detailLoading" class="py-10 text-center text-sm ui-text-muted">加载中…</div>
        <div v-else class="mt-4 max-h-[58vh] space-y-4 overflow-y-auto pr-1">
          <div class="rounded-xl border ui-border p-4">
            <div class="flex items-center gap-2">
              <Avatar
                :src="detail.comment?.authorHeadUrl"
                :name="detail.comment?.authorName"
                :size="30"
              />
              <div class="min-w-0">
                <p class="text-sm font-semibold ui-text-strong">
                  {{ detail.comment?.authorName || '未知用户' }}
                </p>
                <p class="text-xs ui-text-muted">
                  {{ detail.comment?.createTime }}
                  <span
                    v-if="
                      detail.comment?.modifiedTime &&
                      detail.comment.modifiedTime !== detail.comment.createTime
                    "
                  >
                    · 编辑于 {{ detail.comment.modifiedTime }}
                  </span>
                </p>
              </div>
            </div>
            <div class="mt-3">
              <MarkdownView :content="detail.comment?.content || ''" />
            </div>
            <div class="mt-3 flex flex-wrap gap-3 text-xs ui-text-muted">
              <span>点赞 {{ detail.comment?.likeCount ?? 0 }}</span>
              <span>回复 {{ detail.comment?.commentCount ?? 0 }}</span>
              <span>评论 ID #{{ detail.comment?.commentId }}</span>
            </div>
          </div>

          <div class="rounded-xl border ui-border p-4">
            <p class="mb-2 text-xs ui-text-muted">所属问题</p>
            <template v-if="detail.question">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="text-sm font-semibold ui-text-strong">{{ detail.question.title }}</p>
                  <p class="mt-1 text-xs ui-text-muted">
                    {{ meta.classificationLabel(detail.question.classification) || '未分类' }}
                    <span v-if="detail.question.tag"> · {{ detail.question.tag }}</span>
                    · {{ detail.question.authorName || '未知用户' }}
                    <span v-if="detail.question.createTime">
                      · {{ detail.question.createTime }}</span
                    >
                  </p>
                </div>
                <RouterLink
                  :to="`/question/${detail.question.questionId}`"
                  class="btn btn-ghost btn-xs ui-text-brand shrink-0"
                >
                  查看问题<ArrowUpRight class="size-3.5" />
                </RouterLink>
              </div>
              <div class="mt-2 flex flex-wrap gap-1.5">
                <span class="badge badge-sm" :class="statusMeta(detail.question.status).cls">
                  {{ statusMeta(detail.question.status).label }}
                </span>
                <span class="badge badge-sm" :class="reviewMeta(detail.question.reviewStatus).cls">
                  {{ reviewMeta(detail.question.reviewStatus).label }}
                </span>
              </div>
            </template>
            <p v-else class="text-sm ui-text-muted">问题不存在或已被彻底删除</p>
          </div>

          <div v-if="detail.parentComment" class="rounded-xl border ui-border p-4">
            <div class="mb-2 flex flex-wrap items-center justify-between gap-2">
              <p class="text-xs ui-text-muted">
                被回复的评论 #{{ detail.parentComment.commentId }}
              </p>
              <div class="flex gap-1.5">
                <span class="badge badge-xs" :class="statusMeta(detail.parentComment.status).cls">
                  {{ statusMeta(detail.parentComment.status).label }}
                </span>
                <span
                  class="badge badge-xs"
                  :class="reviewMeta(detail.parentComment.reviewStatus).cls"
                >
                  {{ reviewMeta(detail.parentComment.reviewStatus).label }}
                </span>
              </div>
            </div>
            <p class="text-xs ui-text-muted">
              {{ detail.parentComment.authorName || '未知用户' }} ·
              {{ detail.parentComment.createTime }}
            </p>
            <p class="mt-2 whitespace-pre-wrap break-words text-sm ui-text-base">
              {{ detail.parentComment.content || '（无内容）' }}
            </p>
          </div>

          <div
            v-else-if="Number(detail.comment?.type) === 2"
            class="rounded-xl border border-dashed ui-border p-4"
          >
            <p class="text-xs ui-text-muted">被回复的评论已不存在</p>
          </div>

          <div v-if="detail.rootComment" class="rounded-xl border ui-border p-4">
            <div class="mb-2 flex flex-wrap items-center justify-between gap-2">
              <p class="text-xs ui-text-muted">所属根评论 #{{ detail.rootComment.commentId }}</p>
              <div class="flex gap-1.5">
                <span class="badge badge-xs" :class="statusMeta(detail.rootComment.status).cls">
                  {{ statusMeta(detail.rootComment.status).label }}
                </span>
                <span
                  class="badge badge-xs"
                  :class="reviewMeta(detail.rootComment.reviewStatus).cls"
                >
                  {{ reviewMeta(detail.rootComment.reviewStatus).label }}
                </span>
              </div>
            </div>
            <p class="text-xs ui-text-muted">
              {{ detail.rootComment.authorName || '未知用户' }} ·
              {{ detail.rootComment.createTime }}
            </p>
            <p class="mt-2 whitespace-pre-wrap break-words text-sm ui-text-base">
              {{ detail.rootComment.content || '（无内容）' }}
            </p>
          </div>

          <div
            v-if="detail.comment?.reviewReason || detail.comment?.reviewSuggestion"
            class="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm"
          >
            <p class="font-semibold text-amber-700">审核记录</p>
            <p v-if="detail.comment.reviewReason" class="mt-2 text-amber-800">
              原因：{{ detail.comment.reviewReason }}
            </p>
            <p v-if="detail.comment.reviewSuggestion" class="mt-1 text-amber-800">
              修改意见：{{ detail.comment.reviewSuggestion }}
            </p>
          </div>
        </div>

        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="detail = null">关闭</button>
          <button
            v-if="Number(detail.comment?.status) === 1"
            class="btn btn-error btn-sm"
            type="button"
            :disabled="detailLoading || busyId === detail.comment?.commentId"
            @click="remove(detail.comment)"
          >
            <Trash2 class="size-4" />删除评论
          </button>
          <button
            v-else
            class="btn btn-success btn-sm text-white"
            type="button"
            :disabled="detailLoading || busyId === detail.comment?.commentId"
            @click="recover(detail.comment)"
          >
            <RotateCcw class="size-4" />恢复评论
          </button>
        </div>
      </div>
    </dialog>
  </div>
</template>
