<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, Tags } from '@lucide/vue'
import { discoverApi, assetUrl } from '@/api'
import { useMetaStore } from '@/stores/meta'
import { useSessionStore } from '@/stores/session'
import SidePanel from '@/components/SidePanel.vue'

const route = useRoute()
const router = useRouter()
const meta = useMetaStore()
const session = useSessionStore()
const type = computed(() => route.query.type || '1')
const data = ref(null)
const loading = ref(false)
const myOpen = ref(false)
const myData = ref(null)
const myLoading = ref(false)

async function load() {
  loading.value = true
  try {
    data.value = await discoverApi.topics(type.value)
  } finally {
    loading.value = false
  }
}
watch(type, load, { immediate: true })

async function openMine() {
  myOpen.value = true
  myLoading.value = true
  try {
    myData.value = await discoverApi.myTopics()
  } finally {
    myLoading.value = false
  }
}

function reviewLabel(status) {
  if (Number(status) === 0) return '待审核'
  if (Number(status) === 2) return '未通过'
  return '已通过'
}

function visibilityLabel(visibility) {
  if (Number(visibility) === 2) return '私有 · 仅邀请'
  if (Number(visibility) === 1) return '私有 · 可申请'
  return '公开'
}

function memberStatusLabel(item) {
  const status = Number(item.status)
  if (status === 1) return '已加入'
  if (status === 2) return '未通过'
  return item.joinType === 'invite' ? '待接受邀请' : '申请审核中'
}
</script>

<template>
  <div class="page-container">
    <div class="surface mb-4 flex flex-wrap items-center gap-2 p-4">
      <h1 class="mr-2 flex items-center gap-1.5 text-base font-semibold ui-text-strong"><Tags class="size-4 ui-text-brand" />话题分类</h1>
      <button
        v-for="item in meta.topicTypes"
        :key="item.value"
        class="btn btn-xs"
        :class="String(type) === String(item.value) ? 'btn-primary' : 'btn-ghost'"
        type="button"
        @click="router.push({ name: 'topics', query: { type: item.value } })"
      >
        {{ item.label }}
      </button>
      <div class="ml-auto flex items-center gap-2">
        <button v-if="session.loggedIn" class="btn btn-ghost btn-sm" type="button" @click="openMine">我的话题</button>
        <RouterLink v-if="session.canWrite('content:topic:create')" to="/topics/create" class="btn btn-primary btn-sm">
          <Plus class="size-4" />发起话题
        </RouterLink>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(260px,1fr)]">
      <div class="min-w-0">
        <div v-if="loading && !data" class="surface empty-state">加载中…</div>
        <div v-else-if="!data?.tags?.length" class="surface empty-state">该分类下暂时没有话题</div>
        <div v-else class="grid gap-3 sm:grid-cols-2">
          <RouterLink
            v-for="tag in data.tags"
            :key="tag.id || tag.title"
            :to="`/topics/${tag.id}`"
            class="surface flex items-start gap-3 p-4 hover:border-indigo-200"
          >
            <img :src="assetUrl(tag.image)" :alt="tag.title" class="h-14 w-14 shrink-0 rounded-lg object-cover" />
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-1.5">
                <h2 class="font-medium ui-text-strong">{{ tag.title }}</h2>
                <span v-if="Number(tag.visibility) === 1" class="badge badge-xs badge-warning">私有 · 可申请</span>
              </div>
              <p class="line-clamp-2 mt-1 text-xs ui-text-base">{{ tag.simpleDesc || '暂无简介' }}</p>
              <p class="mt-2 text-xs ui-text-muted">{{ tag.followCount }} 人关注 · {{ tag.talkCount }} 条讨论</p>
            </div>
          </RouterLink>
        </div>
      </div>

      <SidePanel position="class" />
    </div>

    <dialog v-if="myOpen" class="modal modal-open">
      <div class="modal-box max-w-2xl">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-base font-semibold">我的话题</h3>
          <button class="btn btn-ghost btn-sm" type="button" @click="myOpen = false">关闭</button>
        </div>
        <div v-if="myLoading" class="empty-state">加载中…</div>
        <template v-else>
          <h4 class="mb-2 text-sm font-medium ui-text-strong">我创建的话题</h4>
          <div v-if="!myData?.created?.length" class="empty-state py-4 text-xs">还没有创建过话题</div>
          <ul v-else class="mb-5 space-y-2">
            <li v-for="item in myData.created" :key="item.id">
              <RouterLink :to="`/topics/${item.id}`" class="flex items-center gap-2 rounded-lg border ui-border p-2.5 hover:border-indigo-300">
                <img :src="assetUrl(item.image)" :alt="item.title" class="h-9 w-9 rounded-lg object-cover" />
                <span class="min-w-0 flex-1 truncate text-sm ui-text-strong">{{ item.title }}</span>
                <span class="badge badge-xs" :class="Number(item.reviewStatus) === 1 ? 'badge-success' : Number(item.reviewStatus) === 2 ? 'badge-error' : 'badge-warning'">
                  {{ reviewLabel(item.reviewStatus) }}
                </span>
                <span class="badge badge-xs badge-ghost">{{ visibilityLabel(item.visibility) }}</span>
              </RouterLink>
            </li>
          </ul>

          <h4 class="mb-2 text-sm font-medium ui-text-strong">我参与的话题</h4>
          <div v-if="!myData?.joined?.length" class="empty-state py-4 text-xs">还没有加入或申请中的话题</div>
          <ul v-else class="space-y-2">
            <li v-for="item in myData.joined" :key="item.memberId">
              <RouterLink :to="`/topics/${item.topic.id}`" class="flex items-center gap-2 rounded-lg border ui-border p-2.5 hover:border-indigo-300">
                <img :src="assetUrl(item.topic.image)" :alt="item.topic.title" class="h-9 w-9 rounded-lg object-cover" />
                <span class="min-w-0 flex-1 truncate text-sm ui-text-strong">{{ item.topic.title }}</span>
                <span class="badge badge-xs badge-ghost">{{ memberStatusLabel(item) }}</span>
              </RouterLink>
            </li>
          </ul>
        </template>
      </div>
    </dialog>
  </div>
</template>
