<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowUpRight,
  CheckCircle2,
  Eye,
  Flag,
  Inbox,
  RefreshCw,
  Search,
  ShieldAlert,
  Trash2,
  X,
  XCircle,
} from '@lucide/vue'
import { adminApi } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/Avatar.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()
const confirm = useConfirmStore()

const status = computed(() => String(route.query.status ?? '0'))
const type = computed(() => String(route.query.type || 'all'))
const page = computed(() => route.query.page || '1')
const keywordQuery = computed(() => String(route.query.keyword || ''))
const keyword = ref(keywordQuery.value)

const data = ref(null)
const loading = ref(false)
const detail = ref(null)
const detailLoading = ref(false)
const handling = ref(false)
const handleForm = reactive({ accepted: true, note: '', deleteContent: true })

const statusTabs = [
  { value: '0', label: '待处理' },
  { value: '1', label: '已受理' },
  { value: '2', label: '已驳回' },
  { value: '-1', label: '全部' },
]
const typeTabs = [
  { value: 'all', label: '全部对象' },
  { value: 'question', label: '问题' },
  { value: 'comment', label: '评论' },
]

function statusMeta(value) {
  if (Number(value) === 0) return { label: '待处理', cls: 'badge-warning' }
  if (Number(value) === 2) return { label: '已驳回', cls: 'badge-error' }
  return { label: '已受理', cls: 'badge-success' }
}

function targetStatusMeta(value) {
  return Number(value) === 1
    ? { label: '正常', cls: 'badge-success' }
    : { label: '已删除', cls: 'badge-error' }
}

function reviewMeta(value) {
  if (Number(value) === 0) return { label: '待审核', cls: 'badge-warning' }
  if (Number(value) === 2) return { label: '未通过', cls: 'badge-error' }
  return { label: '已通过', cls: 'badge-success' }
}

async function load() {
  loading.value = true
  try {
    data.value = await adminApi.reports({
      status: status.value,
      type: type.value,
      keyword: keywordQuery.value,
      page: page.value,
      size: 20,
    })
  } finally {
    loading.value = false
  }
}
watch([status, type, page, keywordQuery], load, { immediate: true })
watch(keywordQuery, (value) => {
  keyword.value = value
})

function updateQuery(patch) {
  const next = { ...route.query, page: 1, ...patch }
  if (!next.keyword) delete next.keyword
  // 默认状态是「待处理」，选择「全部」时必须保留 status=-1，不能删掉查询参数
  if (next.type === 'all') delete next.type
  router.push({ name: 'admin-reports', query: next })
}

function submitSearch() {
  updateQuery({ keyword: keyword.value.trim() || '' })
}

async function openDetail(item) {
  detail.value = { report: item, question: null, commentDetail: null }
  detailLoading.value = true
  handleForm.accepted = true
  handleForm.note = ''
  handleForm.deleteContent = true
  try {
    detail.value = await adminApi.reportDetail(item.id)
  } finally {
    detailLoading.value = false
  }
}

async function submitHandle() {
  const report = detail.value?.report
  if (!report) return
  const note = handleForm.note.trim()
  if (!handleForm.accepted && !note) {
    toast.warning('请填写驳回说明')
    return
  }
  if (handleForm.accepted && handleForm.deleteContent) {
    const ok = await confirm.ask({
      title: '受理并删除内容',
      message: '受理后将同时删除被举报内容，删除操作会同步通知内容作者，确认继续？',
      confirmText: '受理并删除',
      danger: true,
    })
    if (!ok) return
  }
  handling.value = true
  try {
    await adminApi.handleReport(report.id, {
      accepted: handleForm.accepted,
      note: note || null,
      deleteContent: handleForm.accepted && handleForm.deleteContent,
    })
    toast.success(handleForm.accepted ? '举报已受理' : '举报已驳回')
    detail.value = null
    await load()
  } finally {
    handling.value = false
  }
}
</script>

<template>
  <div class="space-y-5">
    <div class="surface relative overflow-hidden p-5 sm:p-6">
      <div
        class="pointer-events-none absolute -right-20 -top-24 size-56 rounded-full bg-gradient-to-br from-rose-500/15 to-orange-500/10 blur-2xl"
      />
      <div class="relative flex flex-wrap items-start justify-between gap-4">
        <div class="flex items-start gap-3">
          <span
            class="grid size-11 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-rose-500 to-orange-500 text-white shadow-lg shadow-rose-500/25"
          >
            <Flag class="size-5" />
          </span>
          <div>
            <h1 class="text-lg font-bold ui-text-strong">举报管理</h1>
            <p class="mt-0.5 text-sm ui-text-base">
              受理用户对问题与评论的举报，可填写处理说明并在受理时同步删除违规内容
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
        <div class="flex flex-wrap gap-1">
          <button
            v-for="tab in typeTabs"
            :key="tab.value"
            class="btn btn-xs"
            :class="type === tab.value ? 'btn-primary' : 'btn-ghost'"
            type="button"
            @click="updateQuery({ type: tab.value })"
          >
            {{ tab.label }}
          </button>
        </div>
        <span class="text-xs ui-text-muted">共 {{ data?.allCount ?? 0 }} 条举报</span>
        <form class="join ml-auto" @submit.prevent="submitSearch">
          <input
            v-model="keyword"
            class="input input-bordered input-sm join-item w-44 sm:w-60"
            placeholder="搜索举报说明或内容"
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
              <th>举报对象</th>
              <th class="w-52">举报理由</th>
              <th class="w-32">举报人</th>
              <th class="w-24">状态</th>
              <th class="w-40">举报时间</th>
              <th class="w-44">处理信息</th>
              <th class="w-32 text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in data?.data || []" :key="item.id" class="hover:bg-slate-50">
              <td class="text-xs ui-text-muted">{{ item.id }}</td>
              <td>
                <div class="flex flex-wrap items-center gap-1.5">
                  <span class="badge badge-outline badge-xs">{{ item.targetTypeName }}</span>
                  <span class="badge badge-xs" :class="targetStatusMeta(item.targetStatus).cls">
                    {{ targetStatusMeta(item.targetStatus).label }}
                  </span>
                  <RouterLink
                    :to="`/question/${item.questionId}`"
                    class="text-xs ui-text-brand hover:underline"
                  >
                    问题 #{{ item.questionId }}
                  </RouterLink>
                </div>
                <p class="mt-1 line-clamp-1 text-xs ui-text-muted">{{ item.questionTitle }}</p>
                <p class="mt-1 line-clamp-2 max-w-md text-sm font-medium ui-text-strong">
                  {{ item.targetSnapshot || '（无内容快照）' }}
                </p>
              </td>
              <td>
                <span class="badge badge-sm badge-ghost">{{ item.reasonName }}</span>
                <p v-if="item.description" class="mt-1 line-clamp-2 text-xs ui-text-muted">
                  {{ item.description }}
                </p>
              </td>
              <td>
                <RouterLink
                  v-if="item.reporterUserId"
                  :to="`/user/${item.reporterUserId}`"
                  class="line-clamp-1 text-xs hover:text-indigo-600"
                >
                  {{ item.reporterName }}
                </RouterLink>
                <span v-else class="text-xs ui-text-muted">{{ item.reporterName }}</span>
                <p class="mt-1 text-xs ui-text-muted">作者：{{ item.targetAuthorName }}</p>
              </td>
              <td>
                <span class="badge badge-sm" :class="statusMeta(item.status).cls">
                  {{ statusMeta(item.status).label }}
                </span>
              </td>
              <td class="whitespace-nowrap text-xs ui-text-base">{{ item.createTime }}</td>
              <td class="text-xs">
                <template v-if="Number(item.status) !== 0">
                  <p class="ui-text-base">{{ item.handlerName || '管理员' }}</p>
                  <p class="mt-0.5 ui-text-muted">{{ item.handleTime }}</p>
                  <p v-if="Number(item.contentRemoved) === 1" class="mt-0.5 text-rose-500">
                    已删除被举报内容
                  </p>
                </template>
                <span v-else class="ui-text-muted">—</span>
              </td>
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
                    v-if="Number(item.status) === 0"
                    class="btn btn-ghost btn-xs text-rose-500"
                    type="button"
                    @click="openDetail(item)"
                  >
                    <ShieldAlert class="size-3.5" />处理
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && !(data?.data || []).length">
              <td colspan="8">
                <div class="flex flex-col items-center py-12 text-center text-sm ui-text-muted">
                  <Inbox class="size-9 ui-text-muted" :stroke-width="1.5" />
                  <p class="mt-2">
                    {{ status === '0' ? '当前没有待处理的举报' : '没有符合条件的举报记录' }}
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
              class="grid size-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-rose-500 to-orange-500 text-white"
            >
              <Flag class="size-5" />
            </span>
            <div>
              <h3 class="text-base font-semibold ui-text-strong">
                举报详情 #{{ detail.report?.id }}
              </h3>
              <div class="mt-1 flex flex-wrap items-center gap-1.5">
                <span class="badge badge-sm" :class="statusMeta(detail.report?.status).cls">
                  {{ statusMeta(detail.report?.status).label }}
                </span>
                <span class="badge badge-outline badge-sm">{{
                  detail.report?.targetTypeName
                }}</span>
                <span class="badge badge-ghost badge-sm">{{ detail.report?.reasonName }}</span>
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
          <div class="grid gap-3 sm:grid-cols-2">
            <div class="rounded-xl ui-bg-soft p-3">
              <p class="text-xs ui-text-muted">举报人</p>
              <p class="mt-1 text-sm font-medium ui-text-strong">
                {{ detail.report?.reporterName || '未知用户' }}
                <span
                  v-if="detail.report?.reporterUserId"
                  class="text-xs font-normal ui-text-muted"
                >
                  @{{ detail.report.reporterUserId }}
                </span>
              </p>
            </div>
            <div class="rounded-xl ui-bg-soft p-3">
              <p class="text-xs ui-text-muted">举报时间</p>
              <p class="mt-1 text-sm font-medium ui-text-strong">{{ detail.report?.createTime }}</p>
            </div>
            <div class="rounded-xl ui-bg-soft p-3">
              <p class="text-xs ui-text-muted">被举报内容作者</p>
              <p class="mt-1 text-sm font-medium ui-text-strong">
                {{ detail.report?.targetAuthorName || '未知用户' }}
              </p>
            </div>
            <div class="rounded-xl ui-bg-soft p-3">
              <p class="text-xs ui-text-muted">内容当前状态</p>
              <p class="mt-1 flex flex-wrap gap-1.5">
                <span
                  class="badge badge-sm"
                  :class="targetStatusMeta(detail.report?.targetStatus).cls"
                >
                  {{ targetStatusMeta(detail.report?.targetStatus).label }}
                </span>
                <span
                  class="badge badge-sm"
                  :class="reviewMeta(detail.report?.targetReviewStatus).cls"
                >
                  {{ reviewMeta(detail.report?.targetReviewStatus).label }}
                </span>
              </p>
            </div>
          </div>

          <div v-if="detail.report?.description" class="rounded-xl border ui-border p-4">
            <p class="text-xs ui-text-muted">举报补充说明</p>
            <p class="mt-2 whitespace-pre-wrap break-words text-sm ui-text-base">
              {{ detail.report.description }}
            </p>
          </div>

          <div class="rounded-xl border ui-border p-4">
            <p class="mb-2 text-xs ui-text-muted">举报时内容快照</p>
            <p class="whitespace-pre-wrap break-words text-sm ui-text-base">
              {{ detail.report?.targetSnapshot || '（无内容快照）' }}
            </p>
          </div>

          <div class="rounded-xl border ui-border p-4">
            <p class="mb-2 text-xs ui-text-muted">所属问题</p>
            <template v-if="detail.question">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="text-sm font-semibold ui-text-strong">{{ detail.question.title }}</p>
                  <p class="mt-1 text-xs ui-text-muted">
                    {{ detail.question.authorName || '未知用户' }}
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
                <span class="badge badge-sm" :class="targetStatusMeta(detail.question.status).cls">
                  {{ targetStatusMeta(detail.question.status).label }}
                </span>
                <span class="badge badge-sm" :class="reviewMeta(detail.question.reviewStatus).cls">
                  {{ reviewMeta(detail.question.reviewStatus).label }}
                </span>
              </div>
            </template>
            <p v-else class="text-sm ui-text-muted">问题不存在或已被彻底删除</p>
          </div>

          <div v-if="detail.commentDetail" class="space-y-3">
            <div class="rounded-xl border ui-border p-4">
              <div class="mb-2 flex flex-wrap items-center justify-between gap-2">
                <p class="text-xs ui-text-muted">
                  被举报评论 #{{ detail.commentDetail.comment?.commentId }}
                </p>
                <div class="flex gap-1.5">
                  <span
                    class="badge badge-xs"
                    :class="targetStatusMeta(detail.commentDetail.comment?.status).cls"
                  >
                    {{ targetStatusMeta(detail.commentDetail.comment?.status).label }}
                  </span>
                  <span
                    class="badge badge-xs"
                    :class="reviewMeta(detail.commentDetail.comment?.reviewStatus).cls"
                  >
                    {{ reviewMeta(detail.commentDetail.comment?.reviewStatus).label }}
                  </span>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <Avatar
                  :src="detail.commentDetail.comment?.authorHeadUrl"
                  :name="detail.commentDetail.comment?.authorName"
                  :size="28"
                />
                <p class="text-xs ui-text-muted">
                  {{ detail.commentDetail.comment?.authorName || '未知用户' }} ·
                  {{ detail.commentDetail.comment?.createTime }}
                </p>
              </div>
              <div class="mt-2">
                <MarkdownView :content="detail.commentDetail.comment?.content || ''" />
              </div>
            </div>

            <div v-if="detail.commentDetail.parentComment" class="rounded-xl border ui-border p-4">
              <p class="text-xs ui-text-muted">
                被回复的评论 #{{ detail.commentDetail.parentComment.commentId }} ·
                {{ detail.commentDetail.parentComment.authorName || '未知用户' }}
              </p>
              <p class="mt-2 whitespace-pre-wrap break-words text-sm ui-text-base">
                {{ detail.commentDetail.parentComment.content || '（无内容）' }}
              </p>
            </div>
          </div>

          <div v-if="Number(detail.report?.status) !== 0" class="rounded-xl border ui-border p-4">
            <p class="text-sm font-semibold ui-text-strong">处理结果</p>
            <div class="mt-2 flex flex-wrap items-center gap-2 text-xs ui-text-muted">
              <span class="badge badge-sm" :class="statusMeta(detail.report.status).cls">
                {{ statusMeta(detail.report.status).label }}
              </span>
              <span>{{ detail.report.handlerName || '管理员' }}</span>
              <span>· {{ detail.report.handleTime }}</span>
              <span v-if="Number(detail.report.contentRemoved) === 1" class="text-rose-500">
                · 已同步删除被举报内容
              </span>
            </div>
            <p
              v-if="detail.report.handleNote"
              class="mt-2 whitespace-pre-wrap text-sm ui-text-base"
            >
              {{ detail.report.handleNote }}
            </p>
          </div>

          <div v-if="Number(detail.report?.status) === 0" class="rounded-xl border ui-border p-4">
            <p class="text-sm font-semibold ui-text-strong">处理举报</p>
            <div class="mt-3 flex flex-wrap gap-2">
              <button
                class="btn btn-sm"
                :class="handleForm.accepted ? 'btn-primary' : 'btn-ghost'"
                type="button"
                @click="handleForm.accepted = true"
              >
                <CheckCircle2 class="size-4" />受理举报
              </button>
              <button
                class="btn btn-sm"
                :class="!handleForm.accepted ? 'btn-error' : 'btn-ghost'"
                type="button"
                @click="handleForm.accepted = false"
              >
                <XCircle class="size-4" />驳回举报
              </button>
            </div>
            <label class="mt-3 block">
              <span class="mb-1 block text-xs font-medium ui-text-base">
                {{ handleForm.accepted ? '处理说明（选填）' : '驳回说明（必填）' }}
              </span>
              <textarea
                v-model="handleForm.note"
                rows="3"
                maxlength="500"
                class="textarea textarea-bordered w-full text-sm focus:outline-none"
                :placeholder="
                  handleForm.accepted
                    ? '例如：内容确实违规，已同步删除并通知作者'
                    : '例如：经核查内容未违反社区规范，举报不成立'
                "
              />
              <span class="mt-1 block text-right text-xs ui-text-muted">
                {{ handleForm.note.length }} / 500
              </span>
            </label>
            <label
              v-if="handleForm.accepted"
              class="mt-1 flex cursor-pointer select-none items-center gap-2 text-sm ui-text-base"
            >
              <input
                v-model="handleForm.deleteContent"
                type="checkbox"
                class="checkbox checkbox-sm"
              />
              同时删除被举报内容
            </label>
            <div class="mt-4 flex justify-end">
              <button
                class="btn btn-primary btn-sm"
                type="button"
                :disabled="handling"
                @click="submitHandle"
              >
                <Trash2 v-if="handleForm.accepted && handleForm.deleteContent" class="size-4" />
                <CheckCircle2 v-else class="size-4" />
                {{ handling ? '提交中…' : '提交处理' }}
              </button>
            </div>
          </div>
        </div>

        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="detail = null">关闭</button>
        </div>
      </div>
    </dialog>
  </div>
</template>
