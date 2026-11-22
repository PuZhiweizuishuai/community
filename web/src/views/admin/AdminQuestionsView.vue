<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowUpRight,
  Eye,
  Inbox,
  MessageCircleQuestion,
  RefreshCw,
  RotateCcw,
  Search,
  Trash2,
  X,
} from '@lucide/vue'
import { adminApi, questionApi } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useMetaStore } from '@/stores/meta'
import { useToastStore } from '@/stores/toast'
import { formatDateTime, splitTags } from '@/utils/format'
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
  { value: '-1', label: '全部问题' },
  { value: '1', label: '正常' },
  { value: '0', label: '已删除' },
]

const contentTypes = ['普通文本', '密码可见', '回复可见']

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

function tagsOf(item) {
  return splitTags(item?.tag)
}

/** 列表行内最多展示 3 个标签，其余以 +N 折叠，避免行高被标签撑开 */
function visibleTags(item) {
  return tagsOf(item).slice(0, 3)
}

function hiddenTagCount(item) {
  return Math.max(0, tagsOf(item).length - 3)
}

async function load() {
  loading.value = true
  try {
    data.value = await adminApi.questions({
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
  router.push({ name: 'admin-questions', query: next })
}

function submitSearch() {
  updateQuery({ keyword: keyword.value.trim() || '' })
}

async function openDetail(item) {
  detail.value = { question: item, contents: [], contentsLoaded: false }
  detailLoading.value = true
  try {
    detail.value = await adminApi.questionDetail(item.questionId)
    detail.value.contentsLoaded = true
  } catch {
    // 详情已被彻底删除等异常：关闭弹窗，错误提示由请求层统一弹出
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

async function remove(item) {
  const message =
    '删除后该问题将不再公开展示，其下评论也不再可见，可在「已删除」中恢复；作者与评论者会收到通知。'
  if (!(await confirm.ask({ title: '删除问题', message, confirmText: '删除', danger: true })))
    return
  busyId.value = item.questionId
  try {
    await questionApi.adminRemove(item.questionId)
    toast.success('问题已删除')
    detail.value = null
    await load()
  } finally {
    busyId.value = null
  }
}

async function recover(item) {
  if (
    !(await confirm.ask({
      title: '恢复问题',
      message: '确认恢复该问题？恢复后是否公开展示仍取决于审核状态。',
      confirmText: '恢复',
    }))
  )
    return
  busyId.value = item.questionId
  try {
    await questionApi.recover(item.questionId)
    toast.success('问题已恢复')
    detail.value = null
    await load()
  } finally {
    busyId.value = null
  }
}
</script>

<template>
  <div class="space-y-5">
    <!-- 页头 -->
    <div class="surface relative overflow-hidden p-5 sm:p-6">
      <div
        class="pointer-events-none absolute -right-20 -top-24 size-56 rounded-full bg-gradient-to-br from-indigo-500/15 to-sky-500/10 blur-2xl"
      />
      <div class="relative flex flex-wrap items-start justify-between gap-4">
        <div class="flex items-start gap-3">
          <span
            class="grid size-11 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 to-sky-500 text-white shadow-lg shadow-indigo-500/25"
          >
            <MessageCircleQuestion class="size-5" />
          </span>
          <div>
            <h1 class="text-lg font-bold ui-text-strong">问题管理</h1>
            <p class="mt-0.5 text-sm ui-text-base">
              查看社区全部问题（含已删除），可按状态与关键字筛选，删除或恢复后同步通知作者与评论者
            </p>
          </div>
        </div>
        <button class="btn btn-ghost btn-sm" type="button" :disabled="loading" @click="load">
          <RefreshCw class="size-4" :class="loading ? 'animate-spin' : ''" />刷新
        </button>
      </div>
    </div>

    <!-- 筛选 -->
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
        <span class="text-xs ui-text-muted">共 {{ data?.allCount ?? 0 }} 条问题</span>
        <form class="join ml-auto" @submit.prevent="submitSearch">
          <input
            v-model="keyword"
            class="input input-bordered input-sm join-item w-44 sm:w-60"
            placeholder="搜索问题标题或标签"
          />
          <button class="btn btn-sm btn-primary join-item" type="submit">
            <Search class="size-4" />搜索
          </button>
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
              <th>问题标题</th>
              <th class="w-24">分类</th>
              <th class="w-28">发起人</th>
              <th class="w-28">浏览 / 回复</th>
              <th class="w-20">审核</th>
              <th class="w-20">状态</th>
              <th class="whitespace-nowrap">发布时间</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in data?.data || []"
              :key="item.questionId"
              :class="Number(item.status) === 0 ? 'opacity-70' : ''"
            >
              <td class="text-xs ui-text-muted">{{ item.questionId }}</td>
              <td>
                <div class="min-w-[260px] max-w-[440px]">
                  <p class="line-clamp-2 text-sm font-medium ui-text-strong">
                    {{ item.title || '（无标题）' }}
                  </p>
                  <div class="mt-1 flex flex-wrap items-center gap-1.5 text-xs ui-text-muted">
                    <span v-if="item.topicTitle">#{{ item.topicTitle }}</span>
                    <span
                      v-for="tag in visibleTags(item)"
                      :key="tag"
                      class="badge badge-outline badge-xs"
                    >
                      {{ tag }}
                    </span>
                    <span v-if="hiddenTagCount(item) > 0">+{{ hiddenTagCount(item) }}</span>
                    <span v-if="!item.topicTitle && !tagsOf(item).length">—</span>
                  </div>
                </div>
              </td>
              <td>
                <span class="badge badge-sm badge-outline whitespace-nowrap">{{
                  meta.classificationLabel(item.classification) || '未分类'
                }}</span>
              </td>
              <td>
                <RouterLink
                  v-if="item.user?.userId"
                  :to="`/user/${item.user.userId}`"
                  class="line-clamp-1 text-xs hover:text-indigo-600"
                >
                  {{ item.user.userName || '未知用户' }}
                </RouterLink>
                <span v-else class="text-xs ui-text-muted">{{
                  item.user?.userName || '未知用户'
                }}</span>
              </td>
              <td class="whitespace-nowrap text-xs ui-text-base">
                {{ item.viewCount ?? 0 }} / {{ item.commentCount ?? 0 }}
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
              <td class="whitespace-nowrap text-xs ui-text-base">
                {{ formatDateTime(item.createTime) }}
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
                    v-if="Number(item.status) === 1"
                    class="btn btn-ghost btn-xs text-rose-500"
                    type="button"
                    :disabled="busyId === item.questionId"
                    @click="remove(item)"
                  >
                    <Trash2 class="size-3.5" />删除
                  </button>
                  <button
                    v-else
                    class="btn btn-ghost btn-xs text-emerald-600"
                    type="button"
                    :disabled="busyId === item.questionId"
                    @click="recover(item)"
                  >
                    <RotateCcw class="size-3.5" />恢复
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && !(data?.data || []).length">
              <td colspan="9">
                <div class="flex flex-col items-center py-12 text-center text-sm ui-text-muted">
                  <Inbox class="size-9 ui-text-muted" :stroke-width="1.5" />
                  <p class="mt-2">
                    {{
                      status === '0'
                        ? '当前没有已删除的问题'
                        : keywordQuery
                          ? '没有符合条件的问题'
                          : '暂无问题'
                    }}
                  </p>
                </div>
              </td>
            </tr>
            <tr v-if="loading">
              <td colspan="9" class="py-10 text-center text-sm ui-text-muted">加载中…</td>
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
            <span
              class="grid size-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 to-sky-500 text-white"
            >
              <MessageCircleQuestion class="size-5" />
            </span>
            <div>
              <h3 class="text-base font-semibold ui-text-strong">
                问题详情 #{{ detail.question?.questionId }}
              </h3>
              <div class="mt-1 flex flex-wrap items-center gap-1.5">
                <span class="badge badge-sm" :class="statusMeta(detail.question?.status).cls">
                  {{ statusMeta(detail.question?.status).label }}
                </span>
                <span class="badge badge-sm" :class="reviewMeta(detail.question?.reviewStatus).cls">
                  {{ reviewMeta(detail.question?.reviewStatus).label }}
                </span>
                <span class="badge badge-outline badge-sm">
                  {{ meta.classificationLabel(detail.question?.classification) || '未分类' }}
                </span>
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

        <div
          v-if="detailLoading && !detail.contentsLoaded"
          class="py-10 text-center text-sm ui-text-muted"
        >
          加载中…
        </div>
        <div v-else class="mt-4 max-h-[58vh] space-y-4 overflow-y-auto pr-1">
          <div class="rounded-xl border ui-border p-4">
            <h4 class="text-lg font-bold ui-text-strong">
              {{ detail.question?.title || '（无标题）' }}
            </h4>
            <div class="mt-3 flex items-center gap-2">
              <Avatar
                :src="detail.question?.user?.headUrl"
                :name="detail.question?.user?.userName"
                :size="30"
              />
              <div class="min-w-0">
                <p class="text-sm font-semibold ui-text-strong">
                  {{ detail.question?.user?.userName || '未知用户' }}
                </p>
                <p class="text-xs ui-text-muted">
                  {{ formatDateTime(detail.question?.createTime) }}
                  <span v-if="detail.question?.topicTitle">
                    · #{{ detail.question.topicTitle }}</span
                  >
                </p>
              </div>
            </div>
            <div class="mt-3 flex flex-wrap items-center gap-1.5">
              <span
                v-for="tag in tagsOf(detail.question)"
                :key="tag"
                class="badge badge-outline badge-sm"
              >
                {{ tag }}
              </span>
              <span v-if="!tagsOf(detail.question).length" class="text-xs ui-text-muted">
                未设置标签
              </span>
            </div>
            <div class="mt-3 flex flex-wrap gap-3 text-xs ui-text-muted">
              <span>浏览 {{ detail.question?.viewCount ?? 0 }}</span>
              <span>回复 {{ detail.question?.commentCount ?? 0 }}</span>
              <span>点赞 {{ detail.question?.likeCount ?? 0 }}</span>
              <span>关注 {{ detail.question?.followCount ?? 0 }}</span>
            </div>
          </div>

          <div
            v-for="(segment, index) in detail.contents || []"
            :key="segment.contentId || index"
            class="rounded-xl border ui-border p-4"
          >
            <p class="mb-2 text-xs ui-text-muted">
              段落 {{ index + 1 }} · {{ contentTypes[segment.type] || '内容' }}
            </p>
            <MarkdownView :content="segment.content || ''" />
          </div>
          <p
            v-if="detail.contentsLoaded && !(detail.contents || []).length"
            class="rounded-xl border border-dashed ui-border p-6 text-center text-sm ui-text-muted"
          >
            该问题没有正文内容
          </p>

          <div
            v-if="detail.question?.reviewReason || detail.question?.reviewSuggestion"
            class="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm"
          >
            <p class="font-semibold text-amber-700">审核记录</p>
            <p v-if="detail.question.reviewReason" class="mt-2 text-amber-800">
              原因：{{ detail.question.reviewReason }}
            </p>
            <p v-if="detail.question.reviewSuggestion" class="mt-1 text-amber-800">
              修改意见：{{ detail.question.reviewSuggestion }}
            </p>
          </div>
        </div>

        <div class="modal-action">
          <RouterLink
            v-if="Number(detail.question?.status) === 1"
            :to="`/question/${detail.question?.questionId}`"
            class="btn btn-ghost btn-sm ui-text-brand"
          >
            查看前台页面<ArrowUpRight class="size-3.5" />
          </RouterLink>
          <button class="btn btn-ghost btn-sm" type="button" @click="detail = null">关闭</button>
          <button
            v-if="Number(detail.question?.status) === 1"
            class="btn btn-error btn-sm"
            type="button"
            :disabled="detailLoading || busyId === detail.question?.questionId"
            @click="remove(detail.question)"
          >
            <Trash2 class="size-4" />删除问题
          </button>
          <button
            v-else
            class="btn btn-success btn-sm text-white"
            type="button"
            :disabled="detailLoading || busyId === detail.question?.questionId"
            @click="recover(detail.question)"
          >
            <RotateCcw class="size-4" />恢复问题
          </button>
        </div>
      </div>
    </dialog>
  </div>
</template>
