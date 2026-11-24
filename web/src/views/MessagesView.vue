<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BellRing,
  CheckCheck,
  Compass,
  Flag,
  Inbox,
  MessageCircle,
  RotateCcw,
  ShieldAlert,
  ShieldCheck,
  Tag,
  ThumbsUp,
  Trash2,
  UsersRound,
} from '@lucide/vue'
import { messageApi } from '@/api'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import { useMetaStore } from '@/stores/meta'
import Pagination from '@/components/Pagination.vue'
import AdCarousel from '@/components/AdCarousel.vue'
import Avatar from '@/components/Avatar.vue'
import CommentComposer from '@/components/CommentComposer.vue'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const toast = useToastStore()
const meta = useMetaStore()

const type = computed(() => route.query.type || '0')
const page = computed(() => route.query.page || '1')
const data = ref(null)
const loading = ref(false)
/** 当前展开回复框的消息 id */
const replyingId = ref(null)

// 内容审核相关系统消息：点击后进入「我的审核」修改/查看
const REVIEW_TYPES = [10, 11, 12, 13, 14, 15]
const REVIEW_COMMENT_TYPES = [11, 13, 15]
// 话题通知：outerId 指向话题 ID
const TOPIC_TYPES = [22, 23, 24, 25, 26, 27]
// 标签通知没有可跳转的详情页（outerId 是占位值）
const TAG_TYPES = [20, 21]
// 内容删除 / 恢复
const DELETE_TYPES = [6, 7, 16]
const RESTORE_TYPES = [8, 9, 17]
// 举报处理结果
const REPORT_TYPES = [18, 19]
// 审核通过 / 未通过，用于给图标配色
const PASS_TYPES = [12, 13, 20, 22, 25]
const REJECT_TYPES = [14, 15, 21, 23, 26]
// 可以直接回复的消息：别人回复了我的问题 / 我的评论
const REPLY_TYPES = [1, 2]

const includes = (list, value) => list.includes(Number(value))

const isReviewMessage = (value) => includes(REVIEW_TYPES, value)
const isTopicMessage = (value) => includes(TOPIC_TYPES, value)

function reviewQuery(value) {
  return { type: includes(REVIEW_COMMENT_TYPES, value) ? 'comment' : 'question' }
}

function itemLink(item) {
  if (isReviewMessage(item.type)) return { name: 'my-reviews', query: reviewQuery(item.type) }
  if (isTopicMessage(item.type)) return `/topics/${item.outerid}`
  if (includes(TAG_TYPES, item.type)) return null
  return `/question/${item.outerid}`
}

/** 「内容已删除」「话题已删除」是占位文案，不做成链接避免点进空页面 */
function isPlaceholder(title) {
  return !title || title === '内容已删除' || title === '话题已删除'
}

const ICON_TONES = {
  brand: 'bg-[var(--brand-soft)] text-[var(--brand)]',
  success: 'bg-success/10 text-success',
  danger: 'bg-error/10 text-error',
  warning: 'bg-warning/10 text-warning',
}

/** 系统消息按业务场景换图标与色调，扫一眼就能区分审核、删除、举报、话题 */
function systemIcon(item) {
  const t = Number(item.type)
  if (isReviewMessage(t)) {
    if (includes(REJECT_TYPES, t)) return { comp: ShieldAlert, tone: ICON_TONES.danger }
    if (includes(PASS_TYPES, t)) return { comp: ShieldCheck, tone: ICON_TONES.success }
    return { comp: ShieldCheck, tone: ICON_TONES.brand }
  }
  if (includes(DELETE_TYPES, t)) return { comp: Trash2, tone: ICON_TONES.danger }
  if (includes(RESTORE_TYPES, t)) return { comp: RotateCcw, tone: ICON_TONES.success }
  if (includes(REPORT_TYPES, t)) return { comp: Flag, tone: ICON_TONES.warning }
  if (includes(TAG_TYPES, t)) return { comp: Tag, tone: ICON_TONES.brand }
  if (includes(TOPIC_TYPES, t)) return { comp: UsersRound, tone: ICON_TONES.brand }
  return { comp: BellRing, tone: ICON_TONES.brand }
}

const isUnread = (item) => item.status === 0

/** 图标按 id 预先算好，模板里直接取，避免每条消息重复判断 */
const iconMap = computed(() => {
  const map = {}
  for (const item of data.value?.notifications?.data || []) {
    map[item.id] = systemIcon(item)
  }
  return map
})

const tabs = computed(() => [
  { value: '0', label: '回复', key: 'commentCount', icon: MessageCircle },
  { value: '3', label: '点赞', key: 'likeCount', icon: ThumbsUp },
  { value: '5', label: '系统', key: 'systemCount', icon: BellRing },
])

async function load() {
  loading.value = true
  try {
    data.value = await messageApi.list({ page: page.value, size: 10, type: type.value })
    session.notificationCount = data.value.totalCount || 0
  } finally {
    loading.value = false
  }
}
watch([type, page], load, { immediate: true })

function go(patch) {
  router.push({ name: 'messages', query: { ...route.query, ...patch } })
}

async function openMessage(item) {
  let result
  try {
    result = await messageApi.read(item.id)
  } catch {
    toast.warning('这条消息已不可读')
    return
  }
  const itemType = Number(result?.type)
  applyRead(item)
  if (isReviewMessage(itemType)) {
    router.push({ name: 'my-reviews', query: reviewQuery(itemType) })
    return
  }
  if (isTopicMessage(itemType) && result?.outerId) {
    router.push(`/topics/${result.outerId}`)
    return
  }
  // 内容已被删除：跳过去只会看到错误页，这里直接提示
  if (includes(DELETE_TYPES, itemType)) {
    toast.info('相关内容已被删除，无法查看')
    return
  }
  // 标签通知、没有可跳转对象的消息：只标记已读
  if (includes(TAG_TYPES, itemType) || !result?.outerId) {
    toast.info('已标记为已读')
    return
  }
  router.push(`/question/${result.outerId}`)
}

/** 只有「别人回复了我」的消息可以就地回复；点赞、系统消息都不行 */
function canReply(item) {
  return (
    item.actorType === 'user' &&
    includes(REPLY_TYPES, item.type) &&
    Number(item.outerid) > 0 &&
    !isPlaceholder(item.outerTitle)
  )
}

function toggleReply(item) {
  replyingId.value = replyingId.value === item.id ? null : item.id
}

/** 本地把消息置为已读：同步右上角未读角标和对应标签页的小红点 */
function applyRead(item) {
  if (!isUnread(item)) return
  item.status = 1
  session.notificationCount = Math.max(0, (session.notificationCount || 0) - 1)
  const code = Number(item.type)
  const key = code <= 2 ? 'commentCount' : code <= 4 ? 'likeCount' : 'systemCount'
  if (data.value && typeof data.value[key] === 'number') {
    data.value[key] = Math.max(0, data.value[key] - 1)
  }
}

/** 标记单条已读（服务端 + 本地），失败时不影响当前操作 */
async function markRead(item) {
  if (!isUnread(item)) return
  applyRead(item)
  try {
    await messageApi.read(item.id)
  } catch {
    // 已读失败不打断用户，下次刷新列表会以服务端状态为准
  }
}

async function onReplyCreated(item, created) {
  replyingId.value = null
  // 开启审核时回复先进入待审，对方要等审核通过才会收到通知
  const pending = Number(created?.reviewStatus) === 0
  toast.success(pending ? '回复已提交审核，通过后对方会收到通知' : '回复已发布，对方会收到通知')
  await markRead(item)
}

async function readAll() {
  await messageApi.readAll()
  toast.success('已全部标记为已读')
  await load()
}

const ads = computed(() => meta.adsFor('message'))
meta.loadAds('message')
</script>

<template>
  <div class="page-container">
    <div class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(260px,1fr)]">
      <div class="surface min-w-0 p-5">
        <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
          <div>
            <h1 class="text-lg font-semibold ui-text-strong">消息通知</h1>
            <p class="mt-0.5 text-xs ui-text-muted">回复、点赞与系统通知统一在这里查看</p>
          </div>
          <button class="btn btn-ghost btn-sm" type="button" @click="readAll">
            <CheckCheck class="size-4" />一键已读
          </button>
        </div>

        <div class="tabs tabs-bordered mb-3">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            class="tab"
            :class="type === tab.value ? 'tab-active font-medium ui-text-brand' : ''"
            type="button"
            @click="go({ type: tab.value, page: 1 })"
          >
            <component :is="tab.icon" class="size-4" />
            {{ tab.label }}
            <span
              v-if="data?.[tab.key]"
              class="badge badge-error badge-xs ml-1 border-0 text-white"
              >{{ data[tab.key] }}</span
            >
          </button>
        </div>

        <div v-if="loading" class="space-y-3 py-2">
          <div v-for="i in 4" :key="i" class="flex items-start gap-3 py-2">
            <div class="size-9 shrink-0 animate-pulse rounded-full ui-bg-soft" />
            <div class="min-w-0 flex-1 space-y-2">
              <div class="h-3 w-2/3 animate-pulse rounded ui-bg-soft" />
              <div class="h-3 w-1/3 animate-pulse rounded ui-bg-soft" />
            </div>
          </div>
        </div>

        <div v-else-if="!data?.notifications?.data?.length" class="empty-state">
          <Inbox class="size-10 ui-text-muted" :stroke-width="1.5" />
          <p class="mt-2">这里还没有消息～</p>
          <p class="mt-1 text-xs">有人回复你、点赞你的内容，或者内容审核有结果时会出现在这里</p>
        </div>

        <ul v-else class="msg-list">
          <li
            v-for="item in data.notifications.data"
            :key="item.id"
            class="relative flex cursor-pointer items-start gap-3 px-2 py-3.5 transition-colors hover:bg-[var(--soft-bg)]"
            :class="isUnread(item) ? 'bg-[var(--soft-bg)]' : ''"
            role="button"
            tabindex="0"
            @click="openMessage(item)"
            @keydown.enter.prevent="openMessage(item)"
            @keydown.space.prevent="openMessage(item)"
          >
            <span
              v-if="isUnread(item)"
              class="absolute top-1/2 left-0 h-8 w-[3px] -translate-y-1/2 rounded-full"
              style="background-color: var(--brand)"
            />

            <!-- 系统消息不展示「某人」，改用场景图标；用户行为展示真实头像，可点进主页 -->
            <span
              v-if="item.actorType === 'system'"
              class="flex size-9 shrink-0 items-center justify-center rounded-full"
              :class="iconMap[item.id]?.tone"
            >
              <component :is="iconMap[item.id]?.comp" class="size-4" />
            </span>
            <RouterLink
              v-else-if="item.notifierId"
              :to="`/user/${item.notifierId}`"
              class="shrink-0 self-start rounded-full ring-offset-2 transition hover:ring-2 hover:ring-[var(--brand-ring)]"
              :title="`进入 ${item.notifierName} 的主页`"
              @click.stop
            >
              <Avatar :src="item.notifierAvatar" :name="item.notifierName || '?'" :size="36" />
            </RouterLink>
            <Avatar
              v-else
              class="self-start"
              :src="item.notifierAvatar"
              :name="item.notifierName || '?'"
              :size="36"
            />

            <div class="min-w-0 flex-1">
              <div class="flex items-start gap-2">
                <p class="min-w-0 flex-1 text-sm leading-6">
                  <template v-if="item.actorType === 'system'">
                    <span
                      class="mr-1.5 rounded px-1.5 py-0.5 text-[11px] font-medium"
                      :class="iconMap[item.id]?.tone"
                      >系统</span
                    >
                    <span :class="isUnread(item) ? 'font-medium ui-text-strong' : 'ui-text-base'">{{
                      item.typeName
                    }}</span>
                  </template>
                  <template v-else>
                    <b class="ui-text-strong">{{ item.notifierName }}</b>
                    <span :class="isUnread(item) ? 'ui-text-strong' : 'ui-text-base'">
                      {{ item.typeName }}</span
                    >
                  </template>
                </p>
                <span
                  class="hidden shrink-0 pt-1 text-xs whitespace-nowrap ui-text-muted sm:inline"
                  >{{ item.createTime }}</span
                >
              </div>

              <template v-if="isPlaceholder(item.outerTitle)">
                <p v-if="item.outerTitle" class="mt-0.5 truncate text-xs ui-text-muted">
                  {{ item.outerTitle }}
                </p>
              </template>
              <RouterLink
                v-else
                :to="itemLink(item) || '/'"
                class="mt-0.5 block truncate text-xs ui-text-brand hover:underline"
                @click.stop
              >
                「{{ item.outerTitle }}」
              </RouterLink>

              <p
                v-if="item.commentContent"
                class="line-clamp-2 mt-1 text-xs leading-5 ui-text-base"
              >
                {{ item.commentContent }}
              </p>

              <!-- 底部操作行：手机端放时间，能回复的消息再放「回复」入口 -->
              <div
                class="mt-1 flex items-center gap-3 text-xs ui-text-muted"
                :class="canReply(item) ? '' : 'sm:hidden'"
              >
                <span class="sm:hidden">{{ item.createTime }}</span>
                <button
                  v-if="canReply(item)"
                  type="button"
                  class="inline-flex items-center gap-1 ui-text-brand hover:underline"
                  @click.stop="toggleReply(item)"
                >
                  <MessageCircle class="size-3.5" />
                  {{ replyingId === item.id ? '收起' : '回复' }}
                </button>
              </div>

              <!-- 就地回复：普通文本框 + 图形验证码，提交走与问题页相同的评论接口 -->
              <div v-if="replyingId === item.id" @click.stop @keydown.stop>
                <CommentComposer
                  :question-id="Number(item.outerid)"
                  :target-comment-id="Number(item.commentId) > 0 ? Number(item.commentId) : 0"
                  :target-name="item.notifierName"
                  :top-level="!(Number(item.commentId) > 0)"
                  @created="onReplyCreated(item, $event)"
                  @cancel="replyingId = null"
                />
              </div>
            </div>
          </li>
        </ul>

        <Pagination :pagination="data?.notifications" @change="go({ page: $event })" />
      </div>

      <aside class="side-panel">
        <section class="surface p-4 text-sm">
          <h3 class="mb-2 flex items-center gap-1.5 font-semibold ui-text-strong">
            <Compass class="size-4 ui-text-brand" />快捷导航
          </h3>
          <div class="space-y-1 text-xs">
            <RouterLink to="/settings" class="link-muted block">个人设置</RouterLink>
            <RouterLink
              v-if="session.user"
              :to="`/user/${session.user.userId}`"
              class="link-muted block"
              >我的主页</RouterLink
            >
            <RouterLink
              v-if="session.canWrite('content:question:publish')"
              to="/publish"
              class="link-muted block"
              >发起讨论</RouterLink
            >
          </div>
        </section>
        <AdCarousel :items="ads" title="社区推广" />
      </aside>
    </div>
  </div>
</template>

<style scoped>
/* 用主题变量画分隔线，避免 divide-y 走 currentColor 在白底/暗底下都发灰 */
.msg-list > li + li {
  border-top: 1px solid var(--surface-border);
}
</style>
