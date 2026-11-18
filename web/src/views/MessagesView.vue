<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BellRing, CheckCheck, Compass, Inbox, MessageCircle, ThumbsUp } from '@lucide/vue'
import { messageApi } from '@/api'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import { useMetaStore } from '@/stores/meta'
import Pagination from '@/components/Pagination.vue'
import AdCarousel from '@/components/AdCarousel.vue'
import Avatar from '@/components/Avatar.vue'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const toast = useToastStore()
const meta = useMetaStore()

const type = computed(() => route.query.type || '0')
const page = computed(() => route.query.page || '1')
const data = ref(null)

// 内容审核相关系统消息：点击后进入「我的审核」修改/查看
const REVIEW_TYPES = [10, 11, 12, 13, 14, 15]
const REVIEW_COMMENT_TYPES = [11, 13, 15]

function isReviewMessage(value) {
  return REVIEW_TYPES.includes(Number(value))
}

function reviewQuery(value) {
  return { type: REVIEW_COMMENT_TYPES.includes(Number(value)) ? 'comment' : 'question' }
}

function itemLink(item) {
  return isReviewMessage(item.type)
    ? { name: 'my-reviews', query: reviewQuery(item.type) }
    : `/question/${item.outerid}`
}

const tabs = [
  { value: '0', label: '回复', key: 'commentCount', icon: MessageCircle },
  { value: '3', label: '点赞', key: 'likeCount', icon: ThumbsUp },
  { value: '5', label: '系统', key: 'systemCount', icon: BellRing },
]

async function load() {
  data.value = await messageApi.list({ page: page.value, size: 10, type: type.value })
  session.notificationCount = data.value.totalCount || 0
}
watch([type, page], load, { immediate: true })

function go(patch) {
  router.push({ name: 'messages', query: { ...route.query, ...patch } })
}

async function openMessage(item) {
  try {
    const result = await messageApi.read(item.id)
    if (isReviewMessage(result?.type)) {
      router.push({ name: 'my-reviews', query: reviewQuery(result.type) })
      return
    }
    if (result?.outerId) router.push(`/question/${result.outerId}`)
  } catch {
    toast.warning('这条消息已不可读')
  }
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
          <h1 class="text-lg font-semibold ui-text-strong">消息通知</h1>
          <button class="btn btn-ghost btn-sm" type="button" @click="readAll"><CheckCheck class="size-4" />一键已读</button>
        </div>
        <div class="tabs tabs-bordered mb-4">
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
            <span v-if="data?.[tab.key]" class="badge badge-error badge-xs ml-1 text-white">{{ data[tab.key] }}</span>
          </button>
        </div>

        <div v-if="!data?.notifications?.data?.length" class="empty-state">
          <Inbox class="size-10 ui-text-muted" :stroke-width="1.5" />
          <p class="mt-2">这里还没有消息～</p>
        </div>
        <ul v-else class="divide-y divide-slate-100">
          <li v-for="item in data.notifications.data" :key="item.id">
            <button class="flex w-full items-start gap-3 py-3 text-left hover:bg-slate-50" type="button" @click="openMessage(item)">
              <Avatar :src="null" :name="item.notifierName || '系'" :size="34" />
              <div class="min-w-0 flex-1">
                <p class="text-sm ui-text-strong">
                  <b>{{ item.notifierName || '系统' }}</b>
                  <span class="ui-text-base"> {{ item.typeName }}</span>
                  <RouterLink v-if="item.outerTitle" :to="itemLink(item)" class="ui-text-brand">「{{ item.outerTitle }}」</RouterLink>
                </p>
                <p v-if="item.commentContent" class="line-clamp-2 mt-1 text-xs ui-text-base">{{ item.commentContent }}</p>
                <p class="mt-1 text-xs ui-text-muted">{{ item.createTime }}</p>
              </div>
              <span v-if="item.status === 0" class="badge badge-primary badge-xs">未读</span>
            </button>
          </li>
        </ul>

        <Pagination :pagination="data?.notifications" @change="go({ page: $event })" />
      </div>

      <aside class="side-panel">
        <section class="surface p-4 text-sm">
          <h3 class="mb-2 flex items-center gap-1.5 font-semibold ui-text-strong"><Compass class="size-4 ui-text-brand" />快捷导航</h3>
          <div class="space-y-1 text-xs">
            <RouterLink to="/settings" class="link-muted block">个人设置</RouterLink>
            <RouterLink v-if="session.user" :to="`/user/${session.user.userId}`" class="link-muted block">我的主页</RouterLink>
            <RouterLink v-if="session.canWrite('content:question:publish')" to="/publish" class="link-muted block">发起讨论</RouterLink>
          </div>
        </section>
        <AdCarousel :items="ads" title="社区推广" />
      </aside>
    </div>
  </div>
</template>
