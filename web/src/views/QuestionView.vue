<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  ChevronDown,
  ChevronUp,
  Eye,
  Flag,
  Frown,
  Lock,
  MessageCircle,
  Reply,
  ShieldCheck,
  SquarePen,
  Star,
  ThumbsUp,
  Trash2,
  TriangleAlert,
  X,
} from '@lucide/vue'
import { questionApi, commentApi, interactionApi, reportApi } from '@/api'
import { useSessionStore } from '@/stores/session'
import { useConfirmStore } from '@/stores/confirm'
import { useToastStore } from '@/stores/toast'
import { useMetaStore } from '@/stores/meta'
import { splitTags, shortNumber, normalizeCommentMarkdown, timeAgo } from '@/utils/format'
import Avatar from '@/components/Avatar.vue'
import CommentComposer from '@/components/CommentComposer.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import MarkdownToc from '@/components/MarkdownToc.vue'
import VditorEditor from '@/components/VditorEditor.vue'
import Captcha from '@/components/Captcha.vue'
import Pagination from '@/components/Pagination.vue'
import AdCarousel from '@/components/AdCarousel.vue'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const toast = useToastStore()
const confirm = useConfirmStore()
const meta = useMetaStore()

const question = ref(null)
const contents = ref([])
const bodyEl = ref(null)
const unlockPasswords = ref({})
const unlocking = ref({})
const commentPage = ref(null)
const relevant = ref([])
const loading = ref(false)
const commentsLoading = ref(false)
const liked = ref(false)
const followed = ref(false)
const canEdit = ref(false)
const loginRequired = ref(false)
const commentText = ref('')
const captcha = ref('')
const captchaRef = ref(null)
const commentEditor = ref(null)
const submitting = ref(false)
const expanded = ref({})
// 当前正在回复的评论：parentCommentId 为根评论，targetCommentId 为被回复评论
const replyComposer = ref(null)
const twoLevel = ref({})
const twoLevelPages = ref({})
const twoLevelLoading = ref({})
const TWO_LEVEL_PAGE_SIZE = 10
const tocRef = ref(null)

/** 正文各段落渲染完成后，基于整个正文容器生成右侧目录 */
function onBodyRendered() {
  nextTick(() => {
    if (bodyEl.value) tocRef.value?.render(bodyEl.value)
  })
}

const id = computed(() => route.params.id)
const page = computed(() => route.query.page || '1')
const sort = computed(() => route.query.sort || '0')
const comments = computed(() => commentPage.value?.data || [])
// 审核状态：0 待审核，1 已通过，2 未通过；存量数据默认 1
const reviewStatus = computed(() => Number(question.value?.reviewStatus ?? 1))
const reviewPending = computed(() => reviewStatus.value === 0)
const reviewRejected = computed(() => reviewStatus.value === 2)
const ads = computed(() => meta.adsFor('question'))
// 详情接口已按审核状态装配标签（作者本人可见自己的待审核标签）
const questionTags = computed(() => {
  const list = question.value?.tags
  if (Array.isArray(list) && list.length) return list
  return splitTags(question.value?.tag).map((name) => ({ name, status: 1 }))
})
const canComment = computed(() => session.canWrite('content:comment:create'))
const canReport = computed(() => session.canWrite('content:report:create'))
const reportTarget = ref(null)
const reportSubmitting = ref(false)
const reportForm = reactive({ reason: 'spam', description: '' })
const reportReasons = [
  { value: 'spam', label: '垃圾广告' },
  { value: 'abuse', label: '辱骂攻击' },
  { value: 'porn', label: '色情低俗' },
  { value: 'illegal', label: '违法违规' },
  { value: 'other', label: '其他' },
]
meta.loadAds('question')

const sortTabs = [
  { value: '0', label: '最早回复' },
  { value: '1', label: '最新回复' },
  { value: '2', label: '最多点赞' },
  { value: '3', label: '最多讨论' },
]

async function loadQuestion() {
  loading.value = true
  try {
    const data = await questionApi.detail(id.value)
    question.value = data.question
    contents.value = data.contents || []
    loginRequired.value = !!data.loginRequired
    liked.value = !!data.isQuestionClickLike
    followed.value = !!data.isFollowQuestion
    canEdit.value = !!data.canEdit
  } finally {
    loading.value = false
  }
}

function goSignIn() {
  router.push({ name: 'sign-in', query: { redirect: route.fullPath } })
}

async function unlockSegment(segment) {
  const password = (unlockPasswords.value[segment.contentId] || '').trim()
  if (!password) return toast.warning('请输入阅读密码')
  unlocking.value[segment.contentId] = true
  try {
    const result = await questionApi.unlockContent(id.value, {
      contentId: segment.contentId,
      password,
    })
    segment.content = result.content
    segment.locked = false
  } catch {
    // 密码错误等提示由全局错误处理弹出
  } finally {
    unlocking.value[segment.contentId] = false
  }
}

function goReply() {
  if (!requireLogin()) return
  document.getElementById('reply-box')?.scrollIntoView({ behavior: 'smooth' })
  nextTick(() => commentEditor.value?.focus?.())
}

async function loadComments() {
  if (loginRequired.value || !question.value) return
  commentsLoading.value = true
  try {
    commentPage.value = await questionApi.comments(id.value, {
      page: page.value,
      size: 15,
      sort: sort.value,
    })
    expanded.value = {}
    twoLevel.value = {}
    twoLevelPages.value = {}
    twoLevelLoading.value = {}
    replyComposer.value = null
  } finally {
    commentsLoading.value = false
  }
}

async function loadRelevant() {
  relevant.value = (await questionApi.relevant(id.value)) || []
}

// 问题正文与相关问题只在切换问题时加载，评论排序/翻页不重复查问题；
// 登录可见的帖子匿名不请求正文、评论与相关问题
watch(
  id,
  async () => {
    await loadQuestion()
    if (loginRequired.value) {
      relevant.value = []
      commentPage.value = null
      return
    }
    loadRelevant()
    loadComments()
  },
  { immediate: true },
)
watch([page, sort], loadComments)

function updateQuery(patch) {
  router.push({
    name: 'question',
    params: { id: id.value },
    query: { ...route.query, ...patch },
    hash: '#comments',
  })
}

function requireLogin() {
  if (session.loggedIn) return true
  toast.warning('请先登录后继续操作')
  router.push({ name: 'sign-in', query: { redirect: route.fullPath } })
  return false
}

async function likeQuestion() {
  if (!requireLogin()) return
  const result = await interactionApi.like({ questionId: Number(id.value), commentId: -1 })
  liked.value = result.liked
  toast.success(result.message)
}

async function likeComment(comment) {
  if (!requireLogin()) return
  const result = await interactionApi.like({
    questionId: Number(id.value),
    commentId: comment.commentId,
  })
  comment.likeCount = Math.max(0, Number(comment.likeCount || 0) + (result.liked ? 1 : -1))
  toast.success(result.message)
}

async function followQuestion() {
  if (!requireLogin()) return
  const result = await interactionApi.followQuestion({ followQuestionId: Number(id.value) })
  followed.value = result.following
  toast.success(followed.value ? '关注成功' : '已取消关注')
}

async function loadTwoLevel(comment, page = 1) {
  const rootId = comment.commentId
  if (twoLevelLoading.value[rootId]) return
  twoLevelLoading.value[rootId] = true
  try {
    const result = await commentApi.twoLevel(rootId, {
      page,
      size: TWO_LEVEL_PAGE_SIZE,
    })
    twoLevel.value[rootId] = result?.data || []
    twoLevelPages.value[rootId] = result || null
  } finally {
    twoLevelLoading.value[rootId] = false
  }
}

async function toggleTwoLevel(comment) {
  const rootId = comment.commentId
  if (expanded.value[rootId]) {
    expanded.value[rootId] = false
    return
  }
  if (!twoLevelPages.value[rootId]) {
    await loadTwoLevel(comment, 1)
  }
  expanded.value[rootId] = true
}

/** 只有评论作者本人可以删除自己的评论 */
function canDeleteComment(comment) {
  return Number(comment?.user?.id) === Number(session.user?.id)
}

async function removeComment(comment, parentComment) {
  if (
    !(await confirm.ask({
      title: '删除回复',
      message: '确定删除这条回复吗？删除后无法恢复。',
      confirmText: '删除',
      danger: true,
    }))
  )
    return
  await commentApi.remove(comment.commentId)
  toast.success('回复已删除')
  if (question.value) {
    question.value.commentCount = Math.max(0, Number(question.value.commentCount || 0) - 1)
  }
  // 二级回复删除后刷新当前页；一级评论删除后重新拉取评论列表
  if (parentComment) {
    await loadTwoLevel(parentComment, Number(twoLevelPages.value[parentComment.commentId]?.nowPage || 1))
  } else {
    await loadComments()
  }
}

function replyTo(comment, target) {
  if (!requireLogin()) return
  if (!canComment.value) {
    toast.warning('当前账号没有发表评论权限')
    return
  }
  const targetCommentId = target?.commentId ?? comment.commentId
  // 再次点击同一目标的回复按钮时收起输入框
  if (replyComposer.value?.targetCommentId === targetCommentId) {
    replyComposer.value = null
    return
  }
  replyComposer.value = {
    parentCommentId: comment.commentId,
    targetCommentId,
    targetName: target?.user?.userName || comment.user?.userName,
  }
}

function closeReplyComposer() {
  replyComposer.value = null
}

/** 回复成功后就地插入列表，避免整页刷新丢失当前的展开/输入状态 */
async function onReplyCreated(created) {
  const rootId = replyComposer.value?.parentCommentId
  replyComposer.value = null
  // 开启审核时回复先进入审核区，不在评论区展示
  if (Number(created?.reviewStatus) === 0) {
    toast.success('回复已提交审核，通过后将公开展示')
    return
  }
  if (!created || !rootId) return
  const root = comments.value.find((comment) => comment.commentId === rootId)
  if (root) root.commentCount = Number(root.commentCount || 0) + 1
  if (question.value) question.value.commentCount = Number(question.value.commentCount || 0) + 1
  // 新回复排序在末尾：直接跳到最后一页，保证新回复可见
  const total = Number(root?.commentCount || 1)
  const lastPage = Math.max(1, Math.ceil(total / TWO_LEVEL_PAGE_SIZE))
  await loadTwoLevel({ commentId: rootId }, lastPage)
  expanded.value[rootId] = true
  toast.success('回复发布成功')
}

async function submitComment() {
  if (!requireLogin()) return
  if (!canComment.value) {
    toast.warning('当前账号没有发表评论权限')
    return
  }
  const content = commentEditor.value?.getValue?.() ?? commentText.value
  if (!content.trim()) {
    toast.warning('评论内容不能为空')
    return
  }
  submitting.value = true
  try {
    const created = await commentApi.create({
      questionId: Number(id.value),
      // 一级评论：parentId 指向问题本身
      parentId: Number(id.value),
      content,
      type: 1,
      captcha: captcha.value,
    })
    commentEditor.value?.clear?.()
    commentText.value = ''
    captcha.value = ''
    captchaRef.value?.refresh?.()
    if (Number(created?.reviewStatus) === 0) {
      toast.success('评论已提交审核，通过后将公开展示')
      return
    }
    toast.success('评论发布成功')
    if (question.value) question.value.commentCount = Number(question.value.commentCount || 0) + 1
    await loadComments()
    // 回复后解锁「回复可见」段落：重新拉取详情让服务端下发内容
    if (contents.value.some((s) => s.locked && s.type === 2)) await loadQuestion()
  } catch {
    captchaRef.value?.refresh?.()
  } finally {
    submitting.value = false
  }
}

async function removeQuestion() {
  if (
    !(await confirm.ask({
      title: '删除问题',
      message: '确定要删除这个问题吗？',
      confirmText: '删除',
      danger: true,
    }))
  )
    return
  await questionApi.remove(id.value)
  toast.success('问题已删除')
  router.push('/')
}

function canReportItem(ownerId) {
  return canReport.value && Number(ownerId) !== Number(session.user?.id)
}

function openReport(targetType, targetId, snapshot, ownerId) {
  if (!requireLogin()) return
  if (!canReport.value) {
    toast.warning('当前账号没有举报权限')
    return
  }
  if (Number(ownerId) === Number(session.user?.id)) {
    toast.warning('不能举报自己发布的内容')
    return
  }
  reportForm.reason = 'spam'
  reportForm.description = ''
  reportTarget.value = { targetType, targetId, snapshot }
}

async function submitReport() {
  if (!reportTarget.value) return
  const description = reportForm.description.trim()
  if (reportForm.reason === 'other' && !description) {
    toast.warning('请补充举报说明')
    return
  }
  reportSubmitting.value = true
  try {
    await reportApi.create({
      targetType: reportTarget.value.targetType,
      targetId: reportTarget.value.targetId,
      reason: reportForm.reason,
      description,
    })
    toast.success('举报已提交，我们会尽快处理')
    reportTarget.value = null
  } finally {
    reportSubmitting.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div v-if="loading && !question" class="surface p-8 text-center ui-text-muted">
      正在加载问题…
    </div>

    <div
      v-else-if="question"
      class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(260px,1fr)]"
    >
      <div class="min-w-0">
        <article class="surface p-5">
          <!-- 审核状态横幅：仅作者与管理员能看到未公开的内容 -->
          <div
            v-if="reviewStatus !== 1"
            class="mb-4 rounded-xl border p-4"
            :class="reviewPending ? 'border-amber-200 bg-amber-50' : 'border-rose-200 bg-rose-50'"
          >
            <div class="flex items-start gap-2.5">
              <component
                :is="reviewPending ? ShieldCheck : TriangleAlert"
                class="mt-0.5 size-5 shrink-0"
                :class="reviewPending ? 'text-amber-500' : 'text-rose-500'"
              />
              <div class="min-w-0 flex-1">
                <p
                  class="text-sm font-semibold"
                  :class="reviewPending ? 'text-amber-700' : 'text-rose-700'"
                >
                  {{ reviewPending ? '该内容正在审核中' : '该内容未通过审核' }}
                </p>
                <p
                  class="mt-1 text-xs leading-relaxed"
                  :class="reviewPending ? 'text-amber-600' : 'text-rose-600'"
                >
                  {{
                    reviewPending
                      ? '当前仅你和管理员可见，审核通过后将自动公开展示。'
                      : '其他用户暂时无法查看，请按以下审核意见修改后重新提交。'
                  }}
                </p>
                <div
                  v-if="reviewRejected && (question.reviewReason || question.reviewSuggestion)"
                  class="mt-2 space-y-1 text-xs leading-relaxed text-rose-600"
                >
                  <p v-if="question.reviewReason"><b>原因：</b>{{ question.reviewReason }}</p>
                  <p v-if="question.reviewSuggestion">
                    <b>修改建议：</b>{{ question.reviewSuggestion }}
                  </p>
                </div>
                <RouterLink
                  v-if="canEdit"
                  :to="`/publish/${question.questionId}`"
                  class="btn btn-xs mt-3"
                  :class="reviewPending ? 'btn-warning' : 'btn-error text-white'"
                >
                  <SquarePen class="size-3.5" />{{ reviewPending ? '继续修改' : '修改并重新提交' }}
                </RouterLink>
              </div>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <RouterLink
              v-if="question.topicId"
              :to="`/topics/${question.topicId}`"
              class="badge badge-sm badge-primary hover:opacity-80"
            >
              {{ question.topicTitle || '话题' }}
            </RouterLink>
            <span class="badge badge-outline ui-border ui-text-brand">{{
              meta.classificationLabel(question.classification)
            }}</span>
            <h1 class="w-full text-xl font-bold leading-snug ui-text-strong sm:w-auto sm:flex-1">
              {{ question.title }}
            </h1>
          </div>

          <div class="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs ui-text-muted">
            <RouterLink
              :to="`/user/${question.user?.userId}`"
              class="flex items-center gap-1.5 hover:text-indigo-600"
            >
              <Avatar :src="question.user?.headUrl" :name="question.user?.userName" :size="22" />
              {{ question.user?.userName }}
            </RouterLink>
            <span>发布于 {{ timeAgo(question.createTime) || '未知' }}</span>
            <span class="flex items-center gap-1"
              ><Eye class="size-3.5" />{{ shortNumber(question.viewCount) }} 次浏览</span
            >
            <span class="flex items-center gap-1"
              ><MessageCircle class="size-3.5" />{{
                shortNumber(question.commentCount)
              }}
              条回复</span
            >
            <span class="flex items-center gap-1"
              ><ThumbsUp class="size-3.5" />{{ shortNumber(question.likeCount) }} 个赞</span
            >
          </div>

          <div v-if="loginRequired" class="my-5 rounded-xl border border-dashed ui-border ui-bg-soft p-8 text-center">
            <Lock class="mx-auto size-7 ui-text-muted" />
            <p class="mt-3 text-base font-semibold ui-text-strong">该帖子设置为登录后可见</p>
            <p class="mt-1 text-xs ui-text-muted">登录后即可查看完整正文、评论与相关讨论</p>
            <button class="btn btn-primary btn-sm mt-4" type="button" @click="goSignIn">登录后查看</button>
          </div>

          <div v-else ref="bodyEl" class="my-5 space-y-4 border-t ui-border pt-4">
            <div v-for="segment in contents" :key="segment.contentId">
              <MarkdownView
                v-if="!segment.locked"
                :content="segment.content"
                @rendered="onBodyRendered"
              />
              <div
                v-else-if="segment.type === 1"
                class="rounded-xl border border-dashed ui-border ui-bg-soft p-5 text-center"
              >
                <Lock class="mx-auto size-6 ui-text-muted" />
                <p class="mt-2 text-sm font-medium ui-text-strong">本段内容需输入阅读密码后查看</p>
                <div class="mx-auto mt-3 flex max-w-sm items-center gap-2">
                  <input
                    v-model="unlockPasswords[segment.contentId]"
                    type="password"
                    class="input input-bordered input-sm min-w-0 flex-1 focus:outline-none"
                    placeholder="请输入阅读密码"
                    @keydown.enter.prevent="unlockSegment(segment)"
                  />
                  <button
                    class="btn btn-primary btn-sm"
                    type="button"
                    :disabled="unlocking[segment.contentId]"
                    @click="unlockSegment(segment)"
                  >
                    {{ unlocking[segment.contentId] ? '解锁中…' : '解锁' }}
                  </button>
                </div>
              </div>
              <div
                v-else
                class="rounded-xl border border-dashed ui-border ui-bg-soft p-5 text-center"
              >
                <MessageCircle class="mx-auto size-6 ui-text-muted" />
                <p class="mt-2 text-sm font-medium ui-text-strong">本段内容需回复后查看</p>
                <p class="mt-1 text-xs ui-text-muted">发表一条回复，即可解锁本段内容</p>
                <button class="btn btn-primary btn-sm mt-3" type="button" @click="goReply">
                  去回复
                </button>
              </div>
            </div>
          </div>

          <div v-if="questionTags.length" class="mb-4 flex flex-wrap gap-1.5">
            <RouterLink
              v-for="tag in questionTags"
              :key="tag.id || tag.name"
              :to="{ name: 'home', query: { tag: tag.name } }"
              class="badge badge-sm ui-bg-soft ui-text-base hover:bg-indigo-50 hover:text-indigo-600"
            >
              # {{ tag.name }}
              <span v-if="tag.status === 0" class="ml-1 badge badge-xs badge-warning">待审核</span>
            </RouterLink>
          </div>

          <div v-if="!loginRequired" class="flex flex-wrap items-center gap-2 border-t ui-border pt-4">
            <button
              class="btn btn-sm"
              :class="liked ? 'btn-primary' : 'btn-outline'"
              type="button"
              @click="likeQuestion"
            >
              <ThumbsUp class="size-4" />{{ liked ? '已点赞' : '点赞' }}
            </button>
            <button
              class="btn btn-sm"
              :class="followed ? 'btn-primary' : 'btn-outline'"
              type="button"
              @click="followQuestion"
            >
              <Star class="size-4" />{{ followed ? '已关注' : '关注问题' }}
            </button>
            <RouterLink
              v-if="canEdit && session.canWrite('content:question:publish')"
              :to="`/publish/${question.questionId}`"
              class="btn btn-ghost btn-sm"
              ><SquarePen class="size-4" />编辑</RouterLink
            >
            <button
              v-if="canEdit || session.isAdmin"
              class="btn btn-ghost btn-sm text-rose-500"
              type="button"
              @click="removeQuestion"
            >
              <Trash2 class="size-4" />删除
            </button>
            <button
              v-if="reviewStatus === 1 && canReportItem(question.user?.id) && !canEdit"
              class="btn btn-ghost btn-sm ui-text-muted"
              type="button"
              @click="
                openReport('question', question.questionId, question.title, question.user?.id)
              "
            >
              <Flag class="size-4" />举报
            </button>
          </div>
        </article>

        <!-- 回复列表：登录可见的帖子匿名不展示 -->
        <section v-if="!loginRequired" id="comments" class="surface mt-5 p-5">
          <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
            <h2 class="text-base font-semibold ui-text-strong">
              全部回复（{{ question.commentCount }}）
            </h2>
            <div class="tabs tabs-xs max-w-full overflow-x-auto">
              <button
                v-for="tab in sortTabs"
                :key="tab.value"
                class="tab"
                :class="sort === tab.value ? 'tab-active ui-text-brand' : ''"
                type="button"
                @click="updateQuery({ sort: tab.value, page: 1 })"
              >
                {{ tab.label }}
              </button>
            </div>
          </div>

          <div v-if="commentsLoading && !commentPage" class="empty-state">正在加载回复…</div>
          <div v-else-if="!comments.length" class="empty-state">还没有人回复，来抢占沙发吧～</div>

          <article
            v-for="comment in comments"
            :key="comment.commentId"
            class="border-t ui-border py-4 first:border-t-0"
          >
            <div class="flex items-start gap-3">
              <RouterLink :to="`/user/${comment.user?.userId}`">
                <Avatar :src="comment.user?.headUrl" :name="comment.user?.userName" :size="34" />
              </RouterLink>
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2 text-xs">
                  <RouterLink
                    :to="`/user/${comment.user?.userId}`"
                    class="font-medium ui-text-strong hover:text-indigo-600"
                  >
                    {{ comment.user?.userName }}
                  </RouterLink>
                  <span v-if="comment.targetName" class="ui-text-muted"
                    >回复 @{{ comment.targetName }}</span
                  >
                  <span class="ui-text-muted">{{
                    timeAgo(comment.modifiedTime || comment.createTime) || '刚刚'
                  }}</span>
                </div>
                <div class="mt-1.5">
                  <MarkdownView :content="normalizeCommentMarkdown(comment.content)" :anchor="0" />
                </div>
                <div class="mt-2 flex flex-wrap items-center gap-3 text-xs ui-text-muted">
                  <button
                    type="button"
                    class="flex items-center gap-1 hover:text-indigo-600"
                    @click="likeComment(comment)"
                  >
                    <ThumbsUp class="size-3.5" />{{ comment.likeCount }}
                  </button>
                  <button
                    v-if="Number(comment.commentCount) > 0"
                    type="button"
                    class="flex items-center gap-1 hover:text-indigo-600"
                    @click="toggleTwoLevel(comment)"
                  >
                    <component
                      :is="expanded[comment.commentId] ? ChevronUp : ChevronDown"
                      class="size-3.5"
                    />
                    {{
                      expanded[comment.commentId] ? '收起' : `展开 ${comment.commentCount} 条回复`
                    }}
                  </button>
                  <button
                    v-if="canComment"
                    type="button"
                    class="flex items-center gap-1 transition-colors hover:text-indigo-600"
                    @click="replyTo(comment)"
                  >
                    <Reply class="size-3.5" />回复
                  </button>
                  <button
                    v-if="canReportItem(comment.user?.id)"
                    type="button"
                    class="flex items-center gap-1 transition-colors hover:text-rose-500"
                    @click="
                      openReport('comment', comment.commentId, comment.content, comment.user?.id)
                    "
                  >
                    <Flag class="size-3.5" />举报
                  </button>
                  <button
                    v-if="canDeleteComment(comment)"
                    type="button"
                    class="flex items-center gap-1 transition-colors hover:text-rose-500"
                    @click="removeComment(comment)"
                  >
                    <Trash2 class="size-3.5" />删除
                  </button>
                </div>

                <!-- 回复一级评论：输入框就地展开，不再跳转到底部 -->
                <CommentComposer
                  v-if="
                    replyComposer?.parentCommentId === comment.commentId &&
                    replyComposer?.targetCommentId === comment.commentId
                  "
                  :question-id="Number(id)"
                  :target-comment-id="comment.commentId"
                  :target-name="replyComposer.targetName"
                  @created="onReplyCreated"
                  @cancel="closeReplyComposer"
                />

                <div
                  v-if="expanded[comment.commentId]"
                  class="mt-3 space-y-3 rounded-lg ui-bg-soft p-3"
                >
                  <div v-for="child in twoLevel[comment.commentId] || []" :key="child.commentId">
                    <div class="flex items-start gap-2">
                      <Avatar :src="child.user?.headUrl" :name="child.user?.userName" :size="26" />
                      <div class="min-w-0 flex-1">
                        <div class="text-xs ui-text-base">
                          <b class="ui-text-strong">{{ child.user?.userName }}</b>
                          <span v-if="child.targetName"> 回复 @{{ child.targetName }}</span>
                          · {{ timeAgo(child.createTime) || '刚刚' }}
                        </div>
                        <div class="mt-1">
                          <MarkdownView
                            :content="normalizeCommentMarkdown(child.content)"
                            :anchor="0"
                          />
                        </div>
                      </div>
                      <div class="flex items-center gap-1">
                        <button
                          v-if="canComment"
                          type="button"
                          class="btn btn-ghost btn-xs"
                          @click="replyTo(comment, child)"
                        >
                          回复
                        </button>
                        <button
                          v-if="canReportItem(child.user?.id)"
                          type="button"
                          class="btn btn-ghost btn-xs text-rose-500"
                          @click="
                            openReport('comment', child.commentId, child.content, child.user?.id)
                          "
                        >
                          举报
                        </button>
                        <button
                          v-if="canDeleteComment(child)"
                          type="button"
                          class="btn btn-ghost btn-xs text-rose-500"
                          @click="removeComment(child, comment)"
                        >
                          删除
                        </button>
                      </div>
                    </div>
                    <!-- 回复二级评论：在对应回复下方就地展开 -->
                    <CommentComposer
                      v-if="
                        replyComposer?.parentCommentId === comment.commentId &&
                        replyComposer?.targetCommentId === child.commentId
                      "
                      :question-id="Number(id)"
                      :target-comment-id="child.commentId"
                      :target-name="replyComposer.targetName"
                      class="ml-8"
                      @created="onReplyCreated"
                      @cancel="closeReplyComposer"
                    />
                  </div>

                  <div v-if="twoLevelLoading[comment.commentId]" class="text-center text-xs ui-text-muted">
                    正在加载回复…
                  </div>
                  <div
                    v-else-if="Number(twoLevelPages[comment.commentId]?.totalPage || 1) > 1"
                    class="flex flex-wrap items-center justify-between gap-2 border-t ui-border pt-2 text-xs ui-text-muted"
                  >
                    <button
                      class="btn btn-ghost btn-xs"
                      type="button"
                      :disabled="Number(twoLevelPages[comment.commentId]?.nowPage || 1) <= 1"
                      @click="
                        loadTwoLevel(comment, Number(twoLevelPages[comment.commentId]?.nowPage || 1) - 1)
                      "
                    >
                      上一页
                    </button>
                    <span>
                      第 {{ twoLevelPages[comment.commentId]?.nowPage || 1 }} /
                      {{ twoLevelPages[comment.commentId]?.totalPage || 1 }} 页 · 共
                      {{ twoLevelPages[comment.commentId]?.allCount || 0 }} 条回复
                    </span>
                    <button
                      class="btn btn-ghost btn-xs"
                      type="button"
                      :disabled="
                        Number(twoLevelPages[comment.commentId]?.nowPage || 1) >=
                        Number(twoLevelPages[comment.commentId]?.totalPage || 1)
                      "
                      @click="
                        loadTwoLevel(comment, Number(twoLevelPages[comment.commentId]?.nowPage || 1) + 1)
                      "
                    >
                      下一页
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </article>

          <Pagination :pagination="commentPage" @change="updateQuery({ page: $event })" />
        </section>

        <!-- 发表评论 -->
        <section id="reply-box" class="surface mt-5 p-5">
          <h2 class="mb-3 text-base font-semibold ui-text-strong">发表评论</h2>
          <div
            v-if="reviewStatus !== 1"
            class="rounded-xl border border-dashed ui-border ui-bg-soft p-5 text-center text-sm ui-text-base"
          >
            内容审核通过后才能发表评论
          </div>
          <template v-else-if="canComment">
            <VditorEditor
              ref="commentEditor"
              v-model="commentText"
              :height="220"
              placeholder="友善地表达你的观点…"
            />
            <div class="mt-3 flex flex-wrap items-center gap-3">
              <div class="w-56"><Captcha ref="captchaRef" v-model="captcha" /></div>
              <button
                class="btn btn-primary btn-sm"
                :disabled="submitting"
                type="button"
                @click="submitComment"
              >
                {{ submitting ? '提交中…' : meta.reviewEnabled ? '提交评论审核' : '发布评论' }}
              </button>
            </div>
            <p v-if="meta.reviewEnabled" class="mt-2 text-xs ui-text-muted">
              当前社区已开启内容审核，评论需管理员审核通过后才公开展示。
            </p>
          </template>
          <div
            v-else-if="session.loggedIn"
            class="rounded-xl border border-dashed ui-border ui-bg-soft p-5 text-center text-sm ui-text-base"
          >
            当前账号没有发表评论权限，请联系管理员。
          </div>
          <div v-else class="py-6 text-center text-sm ui-text-base">
            <RouterLink
              :to="{ name: 'sign-in', query: { redirect: route.fullPath } }"
              class="btn btn-primary btn-sm"
              >登录后参与讨论</RouterLink
            >
          </div>
        </section>
      </div>

      <aside class="side-panel">
        <section v-if="question.user" class="surface p-4">
          <h3 class="mb-3 section-title">发起人</h3>
          <RouterLink :to="`/user/${question.user.userId}`" class="flex items-center gap-3">
            <Avatar :src="question.user.headUrl" :name="question.user.userName" :size="44" />
            <div>
              <p class="font-medium ui-text-strong">{{ question.user.userName }}</p>
              <p class="flex items-center gap-1 text-xs ui-text-muted">
                查看 TA 的主页 <ArrowRight class="size-3" />
              </p>
            </div>
          </RouterLink>
        </section>

        <MarkdownToc ref="tocRef" />

        <section v-if="relevant.length" class="surface p-4">
          <h3 class="mb-3 section-title">相关问题</h3>
          <ul class="space-y-2">
            <li v-for="item in relevant.slice(0, 8)" :key="item.questionId">
              <RouterLink
                :to="`/question/${item.questionId}`"
                class="line-clamp-2 text-sm link-muted"
                >{{ item.title }}</RouterLink
              >
            </li>
          </ul>
        </section>

        <AdCarousel :items="ads" title="社区推广" />
      </aside>
    </div>

    <div v-else class="surface flex flex-col items-center p-10 text-center ui-text-muted">
      <Frown class="size-12 ui-text-muted" :stroke-width="1.5" />
      <p class="mt-3 text-lg">这个问题走丢了</p>
      <RouterLink to="/" class="btn btn-primary btn-sm mt-4">回到首页</RouterLink>
    </div>

    <dialog v-if="reportTarget" class="modal modal-open">
      <div class="modal-box max-w-lg">
        <div class="flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span
              class="grid size-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-rose-500 to-orange-500 text-white"
            >
              <Flag class="size-5" />
            </span>
            <div>
              <h3 class="text-base font-semibold ui-text-strong">
                举报{{ reportTarget.targetType === 'question' ? '问题' : '评论' }}
              </h3>
              <p class="mt-0.5 text-xs ui-text-muted">
                请选择举报理由并补充必要说明，管理员会尽快处理
              </p>
            </div>
          </div>
          <button
            class="btn btn-ghost btn-circle btn-sm"
            type="button"
            aria-label="关闭"
            @click="reportTarget = null"
          >
            <X class="size-4" />
          </button>
        </div>

        <div class="mt-4 rounded-xl ui-bg-soft p-3 text-sm ui-text-base">
          <p class="line-clamp-3">{{ reportTarget.snapshot || '（无内容）' }}</p>
        </div>

        <label class="form-control mt-4">
          <span class="label-text mb-1 text-xs font-medium ui-text-base"
            >举报理由 <span class="text-rose-500">*</span></span
          >
          <select
            v-model="reportForm.reason"
            class="select select-bordered select-sm w-full focus:outline-none"
          >
            <option v-for="item in reportReasons" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </label>

        <label class="form-control mt-3">
          <span class="label-text mb-1 text-xs font-medium ui-text-base">
            补充说明{{ reportForm.reason === 'other' ? '（必填）' : '（选填）' }}
          </span>
          <textarea
            v-model="reportForm.description"
            rows="3"
            maxlength="500"
            class="textarea textarea-bordered w-full text-sm focus:outline-none"
            placeholder="请描述违规情况，便于管理员核实处理"
          />
          <span class="mt-1 text-right text-xs ui-text-muted"
            >{{ reportForm.description.length }} / 500</span
          >
        </label>

        <p class="mt-2 text-xs ui-text-muted">请如实举报，恶意举报可能会影响账号信用。</p>

        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="reportTarget = null">
            取消
          </button>
          <button
            class="btn btn-primary btn-sm"
            type="button"
            :disabled="reportSubmitting"
            @click="submitReport"
          >
            {{ reportSubmitting ? '提交中…' : '提交举报' }}
          </button>
        </div>
      </div>
    </dialog>
  </div>
</template>
