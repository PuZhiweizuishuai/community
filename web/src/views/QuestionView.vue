<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, ChevronDown, ChevronUp, Eye, Frown, MessageCircle, Reply, SquarePen, Star, ThumbsUp, Trash2 } from '@lucide/vue'
import { questionApi, commentApi, interactionApi } from '@/api'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import { useMetaStore } from '@/stores/meta'
import { splitTags, shortNumber } from '@/utils/format'
import Avatar from '@/components/Avatar.vue'
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
const meta = useMetaStore()

const data = ref(null)
const loading = ref(false)
const liked = ref(false)
const followed = ref(false)
const commentText = ref('')
const captcha = ref('')
const captchaRef = ref(null)
const commentEditor = ref(null)
const submitting = ref(false)
const expanded = ref({})
const replyTarget = ref(null)
const twoLevel = ref({})
const tocRef = ref(null)

/** 正文渲染完成后生成右侧目录 */
function onBodyRendered(element) {
  if (tocRef.value) tocRef.value.render(element)
  else nextTick(() => tocRef.value?.render(element))
}

const id = computed(() => route.params.id)
const page = computed(() => route.query.page || '1')
const sort = computed(() => route.query.sort || '0')
const comments = computed(() => data.value?.comments?.data || [])
const question = computed(() => data.value?.question || null)
const ads = computed(() => meta.adsFor('question'))
meta.loadAds('question')

const sortTabs = [
  { value: '0', label: '最早回复' },
  { value: '1', label: '最新回复' },
  { value: '2', label: '最多点赞' },
  { value: '3', label: '最多讨论' },
]

async function load() {
  loading.value = true
  try {
    data.value = await questionApi.detail(id.value, { page: page.value, size: 15, sort: sort.value })
    liked.value = !!data.value.isQuestionClickLike
    followed.value = !!data.value.isFollowQuestion
    expanded.value = {}
    twoLevel.value = {}
  } finally {
    loading.value = false
  }
}
watch([id, page, sort], load, { immediate: true })

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

function replyTo(comment, parent) {
  if (!requireLogin()) return
  replyTarget.value = {
    parentCommentId: parent?.commentId ?? comment.commentId,
    parentId: parent?.commentId ?? comment.commentId,
    name: parent?.user?.userName || comment.user?.userName,
  }
  nextTick(() => commentEditor.value?.focus?.())
}

async function submitComment() {
  if (!requireLogin()) return
  const content = commentEditor.value?.getValue?.() ?? commentText.value
  if (!content.trim()) {
    toast.warning('评论内容不能为空')
    return
  }
  submitting.value = true
  try {
    await commentApi.create({
      questionId: Number(id.value),
      parentCommentId: replyTarget.value?.parentCommentId ?? -1,
      parentId: replyTarget.value?.parentId ?? -1,
      content,
      type: replyTarget.value ? 2 : 1,
      captcha: captcha.value,
    })
    toast.success('评论发布成功')
    commentEditor.value?.clear?.()
    commentText.value = ''
    captcha.value = ''
    captchaRef.value?.refresh?.()
    replyTarget.value = null
    await load()
  } catch (error) {
    captchaRef.value?.refresh?.()
    if (error?.code === 2003) requireLogin()
  } finally {
    submitting.value = false
  }
}

async function removeQuestion() {
  if (!window.confirm('确定要删除这个问题吗？')) return
  await questionApi.remove(id.value)
  toast.success('问题已删除')
  router.push('/')
}
</script>

<template>
  <div class="page-container">
    <div v-if="loading && !data" class="surface p-8 text-center ui-text-muted">正在加载问题…</div>

    <div v-else-if="question" class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(260px,1fr)]">
      <div class="min-w-0">
        <article class="surface p-5">
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

          <div class="my-5 border-t ui-border pt-4">
            <MarkdownView :content="question.description" @rendered="onBodyRendered" />
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
            <RouterLink v-if="data.canEdit" :to="`/publish/${question.questionId}`" class="btn btn-ghost btn-sm"><SquarePen class="size-4" />编辑</RouterLink>
            <button v-if="data.canEdit || session.isAdmin" class="btn btn-ghost btn-sm text-rose-500" type="button" @click="removeQuestion"><Trash2 class="size-4" />删除</button>
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

          <div v-if="!comments.length" class="empty-state">还没有人回复，来抢占沙发吧～</div>

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
                  <MarkdownView :content="comment.content" :anchor="0" />
                </div>
                <div class="mt-2 flex flex-wrap items-center gap-3 text-xs ui-text-muted">
                  <button type="button" class="flex items-center gap-1 hover:text-indigo-600" @click="likeComment(comment)"><ThumbsUp class="size-3.5" />{{ comment.likeCount }}</button>
                  <button v-if="Number(comment.commentCount) > 0" type="button" class="flex items-center gap-1 hover:text-indigo-600" @click="toggleTwoLevel(comment)">
                    <component :is="expanded[comment.commentId] ? ChevronUp : ChevronDown" class="size-3.5" />
                    {{ expanded[comment.commentId] ? '收起' : `展开 ${comment.commentCount} 条回复` }}
                  </button>
                  <button type="button" class="flex items-center gap-1 hover:text-indigo-600" @click="replyTo(comment)"><Reply class="size-3.5" />回复</button>
                </div>

                <div v-if="expanded[comment.commentId]" class="mt-3 space-y-3 rounded-lg ui-bg-soft p-3">
                  <div v-for="child in twoLevel[comment.commentId] || []" :key="child.commentId" class="flex items-start gap-2">
                    <Avatar :src="child.user?.headUrl" :name="child.user?.userName" :size="26" />
                    <div class="min-w-0 flex-1">
                      <div class="text-xs ui-text-base">
                        <b class="ui-text-strong">{{ child.user?.userName }}</b>
                        <span v-if="child.targetName"> 回复 @{{ child.targetName }}</span>
                        · {{ child.createTime }}
                      </div>
                      <div class="mt-1"><MarkdownView :content="child.content" :anchor="0" /></div>
                    </div>
                    <button type="button" class="btn btn-ghost btn-xs" @click="replyTo(comment, child)">回复</button>
                  </div>
                </div>
              </div>
            </div>
          </article>

          <Pagination :pagination="data.comments" @change="updateQuery({ page: $event })" />
        </section>

        <!-- 发表回复 -->
        <section class="surface mt-5 p-5">
          <h2 class="mb-3 text-base font-semibold ui-text-strong">发表回复</h2>
          <div v-if="replyTarget" class="alert alert-info mb-3 py-2 text-sm">
            正在回复 @{{ replyTarget.name }}
            <button class="btn btn-ghost btn-xs ml-auto" type="button" @click="replyTarget = null">取消</button>
          </div>
          <template v-if="session.loggedIn">
            <VditorEditor ref="commentEditor" v-model="commentText" :height="220" placeholder="友善地表达你的观点…" />
            <div class="mt-3 flex flex-wrap items-center gap-3">
              <div class="w-56"><Captcha ref="captchaRef" v-model="captcha" /></div>
              <button class="btn btn-primary btn-sm" :disabled="submitting" type="button" @click="submitComment">
                {{ submitting ? '发布中…' : '发布回复' }}
              </button>
            </div>
          </template>
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

        <section v-if="data.relevantQuestions?.length" class="surface p-4">
          <h3 class="mb-3 section-title">相关问题</h3>
          <ul class="space-y-2">
            <li v-for="item in data.relevantQuestions.slice(0, 8)" :key="item.questionId">
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
