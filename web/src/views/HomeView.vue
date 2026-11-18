<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Pin, Plus, X } from '@lucide/vue'
import { homeApi, interactionApi } from '@/api'
import { useMetaStore } from '@/stores/meta'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import SidePanel from '@/components/SidePanel.vue'
import QuestionList from '@/components/QuestionList.vue'
import Pagination from '@/components/Pagination.vue'
import Avatar from '@/components/Avatar.vue'

const route = useRoute()
const router = useRouter()
const meta = useMetaStore()
const session = useSessionStore()
const toast = useToastStore()

const loading = ref(false)
const data = ref(null)
const followed = ref(false)

const query = computed(() => ({
  page: route.query.page || '1',
  size: route.query.size || '15',
  tag: route.query.tag || undefined,
  sort: route.query.sort || '3',
  class: route.query.class || '0',
}))

async function load() {
  loading.value = true
  try {
    data.value = await homeApi.list(query.value)
    followed.value = !!data.value?.followTag
  } finally {
    loading.value = false
  }
}

watch(query, load, { immediate: true, deep: true })

function updateQuery(patch) {
  const next = { ...route.query, ...patch }
  // 第一页不写入 URL，保持地址简洁
  if (String(next.page) === '1') delete next.page
  router.push({ name: 'home', query: next })
}

function goPage(page) {
  if (Number(page) === Number(query.value.page)) return
  updateQuery({ page })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function toggleFollow(tagClass) {
  if (!session.loggedIn) {
    toast.warning('请先登录后再关注话题')
    router.push({ name: 'sign-in', query: { redirect: route.fullPath } })
    return
  }
  const result = await interactionApi.followTopic({
    topicId: tagClass.id,
    topicTitle: tagClass.title,
  })
  followed.value = result?.following
  toast.success(followed.value ? '关注成功' : '已取消关注')
}

const tagClass = computed(() => data.value?.tagClass || null)
</script>

<template>
  <div class="page-container">
    <!-- 分类与排序，对应原首页顶部 pills -->
    <div class="surface mb-4 flex flex-wrap items-center gap-2 px-3 py-2.5 sm:px-4 sm:py-3">
      <RouterLink
        v-for="item in meta.classifications"
        :key="item.value"
        class="btn btn-xs ui-text-base"
        :class="String(query.class) === String(item.value) ? 'btn-primary' : 'btn-ghost'"
        :to="{ name: 'home', query: { tag: route.query.tag, sort: route.query.sort, class: item.value } }"
      >
        {{ item.label }}
      </RouterLink>
      <span class="mx-1 hidden h-4 w-px ui-bg-soft sm:block" />
      <button
        class="btn btn-xs ui-text-base"
        :class="query.sort === '3' ? 'btn-active btn-primary' : 'btn-ghost'"
        type="button"
        @click="updateQuery({ sort: '3', page: 1 })"
      >
        最新
      </button>
      <button
        class="btn btn-xs ui-text-base"
        :class="query.sort === '0' ? 'btn-active btn-primary' : 'btn-ghost'"
        type="button"
        @click="updateQuery({ sort: '0', page: 1 })"
      >
        消灭零回复
      </button>
      <span v-if="query.tag" class="badge badge-primary badge-sm ml-auto">
        标签：{{ query.tag }}
        <button type="button" class="ml-1 inline-flex" @click="updateQuery({ tag: undefined, page: 1 })"><X class="size-3" /></button>
      </span>
    </div>

    <div class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(260px,1fr)]">
      <div class="min-w-0">
        <div v-if="data?.topQuestions?.length" class="surface mb-4 p-4">
          <h2 class="section-title mb-3"><Pin class="size-4 ui-text-brand" />置顶推荐</h2>
          <div class="grid gap-2 sm:grid-cols-3">
            <RouterLink
              v-for="item in data.topQuestions"
              :key="item.questionId"
              :to="`/question/${item.questionId}`"
              class="flex items-center gap-2 rounded-lg border ui-border p-2 hover:border-indigo-200 hover:bg-indigo-50/40"
            >
              <Avatar :src="item.user?.headUrl" :name="item.user?.userName" :size="30" />
              <span class="line-clamp-2 text-xs ui-text-strong">{{ item.title }}</span>
            </RouterLink>
          </div>
        </div>

        <div class="mb-3 flex items-center justify-between gap-3">
          <div>
            <h1 class="text-xl font-bold ui-text-strong">发现</h1>
            <p class="mt-0.5 text-xs ui-text-muted">看看社区里正在讨论什么</p>
          </div>
          <RouterLink v-if="session.canWrite('content:question:publish')" to="/publish" class="btn btn-primary btn-sm hidden sm:inline-flex"><Plus class="size-4" />发起讨论</RouterLink>
        </div>

        <div v-if="loading && !data" class="surface space-y-3 p-4">
          <div v-for="i in 5" :key="i" class="skeleton h-16 w-full rounded-xl" />
        </div>
        <QuestionList v-else :items="data?.pagination?.data || []" />
        <Pagination :pagination="data?.pagination" @change="goPage" />
      </div>

      <SidePanel
        position="home"
        :tag-class="tagClass"
        :followed="followed"
        @follow-topic="toggleFollow"
      />
    </div>
  </div>
</template>
