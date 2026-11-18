<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, ChevronDown, ChevronUp, Eye, Frown, Lock, MessageCircle, Reply, ShieldCheck, SquarePen, Star, ThumbsUp, Trash2, TriangleAlert } from '@lucide/vue'
import { questionApi, commentApi, interactionApi } from '@/api'
import { useSessionStore } from '@/stores/session'
import { useConfirmStore } from '@/stores/confirm'
import { useToastStore } from '@/stores/toast'
import { useMetaStore } from '@/stores/meta'
import { splitTags, shortNumber, normalizeCommentMarkdown } from '@/utils/format'
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
const commentText = ref('')
const captcha = ref('')
const captchaRef = ref(null)
const commentEditor = ref(null)
const submitting = ref(false)
const expanded = ref({})
// 当前正在回复的评论：parentCommentId 为根评论，targetCommentId 为被回复评论
const replyComposer = ref(null)
const twoLevel = ref({})
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
const canComment = computed(() => session.canWrite('content:comment:create'))
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
    liked.value = !!data.isQuestionClickLike
    followed.value = !!data.isFollowQuestion
    canEdit.value = !!data.canEdit
  } finally {
    loading.value = false
  }
}

async function unlockSegment(segment) {
  const password = (unlockPasswords.value[segment.contentId] || '').trim()
  if (!password) return toast.warning('请输入阅读密码')
  unlocking.value[segment.contentId] = true
  try {
    const result = await questionApi.unlockContent(id.value, { contentId: segment.contentId, password })
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
  commentsLoading.value = true
  try {
    commentPage.value = await questionApi.comments(id.value, { page: page.value, size: 15, sort: sort.value })
    expanded.value = {}
    twoLevel.value = {}
    replyComposer.value = null
  } finally {
    commentsLoading.value = false
  }
}

async function loadRelevant() {
  relevant.value = (await questionApi.relevant(id.value)) || []
}

// 问题正文与相关问题只在切换问题时加载，评论排序/翻页不重复查问题
watch(id, () => {
  loadQuestion()
  loadRelevant()
}, { immediate: true })
watch([id, page, sort], loadComments, { immediate: true })

function updateQuery(patch) {
  router.push({ name: 'question', params: { id: id.value }, query: { ...route.query, ...patch }, hash: '#comments' })
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

async function toggleTwoLevel(comment) {
  if (expanded.value[comment.commentId]) {
    expanded.value[comment.commentId] = false
    return
  }
  if (!twoLevel.value[comment.commentId]) {
    twoLevel.value[comment.commentId] = await commentApi.twoLevel(comment.commentId)
  }
  expanded.value[comment.commentId] = true
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
  if (twoLevel.value[rootId]) {
    twoLevel.value[rootId].push(created)
  } else {
    // 未展开过该评论时拉取完整回复列表，保证已有回复和新回复一起展示
    twoLevel.value[rootId] = await commentApi.twoLevel(rootId)
  }
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
  if (!(await confirm.ask({ title: '删除问题', message: '确定要删除这个问题吗？', confirmText: '删除', danger: true }))) return
  await questionApi.remove(id.value)
  toast.success('问题已删除')
  router.push('/')
}
</script>

<template>
  <div class="page-container">
    <div v-if="loading && !question" class="surface p-8 text-center ui-text-muted">正在加载问题…</div>

    <div v-else-if="question" class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(260px,1fr)]">
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
                <p class="text-sm font-semibold" :class="reviewPending ? 'text-amber-700' : 'text-rose-700'">
                  {{ reviewPending ? '该内容正在审核中' : '该内容未通过审核' }}
                </p>
                <p class="mt-1 text-xs leading-relaxed" :class="reviewPending ? 'text-amber-600' : 'text-rose-600'">
                  {{ reviewPending
                    ? '当前仅你和管理员可见，审核通过后将自动公开展示。'
                    : '其他用户暂时无法查看，请按以下审核意见修改后重新提交。' }}
                </p>
                <div v-if="reviewRejected && (question.reviewReason || question.reviewSuggestion)" class="mt-2 space-y-1 text-xs leading-relaxed text-rose-600">
                  <p v-if="question.reviewReason"><b>原因：</b>{{ question.reviewReason }}</p>
                  <p v-if="question.reviewSuggestion"><b>修改建议：</b>{{ question.reviewSuggestion }}</p>
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
            <span class="badge badge-outline ui-border ui-text-brand">{{ question.classification }}</span>
            <h1 class="w-full text-xl font-bold leading-snug ui-text-strong sm:w-auto sm:flex-1">{{ question.title }}</h1>
          </div>

          <div class="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs ui-text-muted">
            <RouterLink :to="`/user/${question.user?.userId}`" class="flex items-center gap-1.5 hover:text-indigo-600">
              <Avatar :src="question.user?.headUrl" :name="question.user?.userName" :size="22" />
              {{ question.user?.userName }}
            </RouterLink>
            <span>发布于 {{ question.createTime }}</span>
            <span class="flex items-center gap-1"><Eye class="size-3.5" />{{ shortNumber(question.viewCount) }} 次浏览</span>
            <span class="flex items-center gap-1"><MessageCircle class="size-3.5" />{{ shortNumber(question.commentCount) }} 条回复</span>
            <span class="flex items-center gap-1"><ThumbsUp class="size-3.5" />{{ shortNumber(question.likeCount) }} 个赞</span>
          </div>

          <div ref="bodyEl" class="my-5 space-y-4 border-t ui-border pt-4">
            <div v-for="segment in contents" :key="segment.contentId">
              <MarkdownView v-if="!segment.locked" :content="segment.content" @rendered="onBodyRendered" />
              <div v-else-if="segment.type === 1" class="rounded-xl border border-dashed ui-border ui-bg-soft p-5 text-center">
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
                  <button class="btn btn-primary btn-sm" type="button" :disabled="unlocking[segment.contentId]" @click="unlockSegment(segment)">
                    {{ unlocking[segment.contentId] ? '解锁中…' : '解锁' }}
                  </button>
                </div>
              </div>
              <div v-else class="rounded-xl border border-dashed ui-border ui-bg-soft p-5 text-center">
                <MessageCircle class="mx-auto size-6 ui-text-muted" />
                <p class="mt-2 text-sm font-medium ui-text-strong">本段内容需回复后查看</p>
                <p class="mt-1 text-xs ui-text-muted">发表一条回复，即可解锁本段内容</p>
                <button class="btn btn-primary btn-sm mt-3" type="button" @click="goReply">去回复</button>
              </div>
            </div>
          </div>

          <div v-if="splitTags(question.tag).length" class="mb-4 flex flex-wrap gap-1.5">
            <RouterLink
              v-for="tag in splitTags(question.tag)"
              :key="tag"
              :to="{ name: 'home', query: { tag } }"
              class="badge badge-sm ui-bg-soft ui-text-base hover:bg-indigo-50 hover:text-indigo-600"
            >
              # {{ tag }}
            </RouterLink>
          </div>

          <div class="flex flex-wrap items-center gap-2 border-t ui-border pt-4">
            <button class="btn btn-sm" :class="liked ? 'btn-primary' : 'btn-outline'" type="button" @click="likeQuestion">
              <ThumbsUp class="size-4" />{{ liked ? '已点赞' : '点赞' }}
            </button>
            <button class="btn btn-sm" :class="followed ? 'btn-primary' : 'btn-outline'" type="button" @click="followQuestion">
              <Star class="size-4" />{{ followed ? '已关注' : '关注问题' }}
            </button>
            <RouterLink v-if="canEdit && session.canWrite('content:question:publish')" :to="`/publish/${question.questionId}`" class="btn btn-ghost btn-sm"><SquarePen class="size-4" />编辑</RouterLink>
            <button v-if="canEdit || session.isAdmin" class="btn btn-ghost btn-sm text-rose-500" type="button" @click="removeQuestion"><Trash2 class="size-4" />删除</button>
          </div>
        </article>

        <!-- 回复列表 -->
        <section id="comments" class="surface mt-5 p-5">
          <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
            <h2 class="text-base font-semibold ui-text-strong">全部回复（{{ question.commentCount }}）</h2>
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

          <article v-for="comment in comments" :key="comment.commentId" class="border-t ui-border py-4 first:border-t-0">
            <div class="flex items-start gap-3">
              <RouterLink :to="`/user/${comment.user?.userId}`">
                <Avatar :src="comment.user?.headUrl" :name="comment.user?.userName" :size="34" />
              </RouterLink>
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2 text-xs">
                  <RouterLink :to="`/user/${comment.user?.userId}`" class="font-medium ui-text-strong hover:text-indigo-600">
                    {{ comment.user?.userName }}
                  </RouterLink>
                  <span v-if="comment.targetName" class="ui-text-muted">回复 @{{ comment.targetName }}</span>
                  <span class="ui-text-muted">{{ comment.modifiedTime || comment.createTime }}</span>
                </div>
                <div class="mt-1.5">
                  <MarkdownView :content="normalizeCommentMarkdown(comment.content)" :anchor="0" />
                </div>
                <div class="mt-2 flex flex-wrap items-center gap-3 text-xs ui-text-muted">
                  <button type="button" class="flex items-center gap-1 hover:text-indigo-600" @click="likeComment(comment)"><ThumbsUp class="size-3.5" />{{ comment.likeCount }}</button>
                  <button v-if="Number(comment.commentCount) > 0" type="button" class="flex items-center gap-1 hover:text-indigo-600" @click="toggleTwoLevel(comment)">
                    <component :is="expanded[comment.commentId] ? ChevronUp : ChevronDown" class="size-3.5" />
                    {{ expanded[comment.commentId] ? '收起' : `展开 ${comment.commentCount} 条回复` }}
                  </button>
                  <button v-if="canComment" type="button" class="flex items-center gap-1 transition-colors hover:text-indigo-600" @click="replyTo(comment)"><Reply class="size-3.5" />回复</button>
                </div>

                <!-- 回复一级评论：输入框就地展开，不再跳转到底部 -->
                <CommentComposer
                  v-if="replyComposer?.parentCommentId === comment.commentId && replyComposer?.targetCommentId === comment.commentId"
                  :question-id="Number(id)"
                  :target-comment-id="comment.commentId"
                  :target-name="replyComposer.targetName"
                  @created="onReplyCreated"
                  @cancel="closeReplyComposer"
                />

                <div v-if="expanded[comment.commentId]" class="mt-3 space-y-3 rounded-lg ui-bg-soft p-3">
                  <div v-for="child in twoLevel[comment.commentId] || []" :key="child.commentId">
                    <div class="flex items-start gap-2">
                      <Avatar :src="child.user?.headUrl" :name="child.user?.userName" :size="26" />
                      <div class="min-w-0 flex-1">
                        <div class="text-xs ui-text-base">
                          <b class="ui-text-strong">{{ child.user?.userName }}</b>
                          <span v-if="child.targetName"> 回复 @{{ child.targetName }}</span>
                          · {{ child.createTime }}
                        </div>
                        <div class="mt-1"><MarkdownView :content="normalizeCommentMarkdown(child.content)" :anchor="0" /></div>
                      </div>
                      <button v-if="canComment" type="button" class="btn btn-ghost btn-xs" @click="replyTo(comment, child)">回复</button>
                    </div>
                    <!-- 回复二级评论：在对应回复下方就地展开 -->
                    <CommentComposer
                      v-if="replyComposer?.parentCommentId === comment.commentId && replyComposer?.targetCommentId === child.commentId"
                      :question-id="Number(id)"
                      :target-comment-id="child.commentId"
                      :target-name="replyComposer.targetName"
                      class="ml-8"
                      @created="onReplyCreated"
                      @cancel="closeReplyComposer"
                    />
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
          <div v-if="reviewStatus !== 1" class="rounded-xl border border-dashed ui-border ui-bg-soft p-5 text-center text-sm ui-text-base">
            内容审核通过后才能发表评论
          </div>
          <template v-else-if="canComment">
            <VditorEditor ref="commentEditor" v-model="commentText" :height="220" placeholder="友善地表达你的观点…" />
            <div class="mt-3 flex flex-wrap items-center gap-3">
              <div class="w-56"><Captcha ref="captchaRef" v-model="captcha" /></div>
              <button class="btn btn-primary btn-sm" :disabled="submitting" type="button" @click="submitComment">
                {{ submitting ? '提交中…' : meta.reviewEnabled ? '提交评论审核' : '发布评论' }}
              </button>
            </div>
            <p v-if="meta.reviewEnabled" class="mt-2 text-xs ui-text-muted">当前社区已开启内容审核，评论需管理员审核通过后才公开展示。</p>
          </template>
          <div v-else-if="session.loggedIn" class="rounded-xl border border-dashed ui-border ui-bg-soft p-5 text-center text-sm ui-text-base">
            当前账号没有发表评论权限，请联系管理员。
          </div>
          <div v-else class="py-6 text-center text-sm ui-text-base">
            <RouterLink :to="{ name: 'sign-in', query: { redirect: route.fullPath } }" class="btn btn-primary btn-sm">登录后参与讨论</RouterLink>
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
              <p class="flex items-center gap-1 text-xs ui-text-muted">查看 TA 的主页 <ArrowRight class="size-3" /></p>
            </div>
          </RouterLink>
        </section>

        <MarkdownToc ref="tocRef" />

        <section v-if="relevant.length" class="surface p-4">
          <h3 class="mb-3 section-title">相关问题</h3>
          <ul class="space-y-2">
            <li v-for="item in relevant.slice(0, 8)" :key="item.questionId">
              <RouterLink :to="`/question/${item.questionId}`" class="line-clamp-2 text-sm link-muted">{{ item.title }}</RouterLink>
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
  </div>
</template>
