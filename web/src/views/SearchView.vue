<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { History, Search, X } from '@lucide/vue'
import { discoverApi } from '@/api'
import { addHistory, clearHistory, getHistory, removeHistory } from '@/utils/searchHistory'
import QuestionList from '@/components/QuestionList.vue'
import Pagination from '@/components/Pagination.vue'
import SidePanel from '@/components/SidePanel.vue'
import Avatar from '@/components/Avatar.vue'

const route = useRoute()
const router = useRouter()
const keyword = computed(() => route.params.keyword || '')
const type = computed(() => route.query.type || 'question')
const page = computed(() => route.query.page || '1')
const input = ref(keyword.value)
const data = ref(null)
const loading = ref(false)
const history = ref(getHistory())

watch(keyword, (value) => {
  input.value = value
  if (value) history.value = addHistory(value)
}, { immediate: true })

async function load() {
  data.value = null
  if (!keyword.value) return
  loading.value = true
  try {
    data.value = await discoverApi.search({
      search: keyword.value,
      page: page.value,
      size: 10,
      type: type.value,
    })
  } finally {
    loading.value = false
  }
}
watch([keyword, page, type], load, { immediate: true })

function go(patch) {
  router.push({
    name: 'search',
    params: { keyword: keyword.value },
    query: { ...route.query, ...patch },
  })
}
function doSearch() {
  const q = input.value.trim()
  if (!q) return
  history.value = addHistory(q)
  router.push(`/search/${encodeURIComponent(q)}`)
}
function goHistory(q) {
  input.value = q
  doSearch()
}
function dropHistory(item) {
  history.value = removeHistory(item)
}
function wipeHistory() {
  history.value = clearHistory()
}

const questions = computed(() => data.value?.questions?.data || [])
const users = computed(() => data.value?.users?.data || [])
const pagination = computed(() => (type.value === 'user' ? data.value?.users : data.value?.questions))
</script>

<template>
  <div class="page-container">
    <div class="surface mb-4 p-4">
      <form class="join w-full" @submit.prevent="doSearch">
        <input v-model="input" class="input input-bordered join-item w-full focus:outline-none" placeholder="搜索问题、用户…" autocomplete="off" />
        <button class="btn btn-primary join-item" type="submit"><Search class="size-4" />搜索</button>
      </form>

      <div v-if="history.length" class="mt-3 flex flex-wrap items-center gap-1.5 border-t border-slate-100 pt-3">
        <span class="mr-1 flex items-center gap-1 text-xs text-slate-400"><History class="size-3.5" />搜索历史</span>
        <button
          v-for="item in history"
          :key="item"
          class="group flex items-center gap-1 rounded-full border border-slate-200 px-2.5 py-1 text-xs text-slate-600 hover:border-indigo-300 hover:text-indigo-600"
          type="button"
          @click="goHistory(item)"
        >
          <span class="max-w-[10rem] truncate">{{ item }}</span>
          <span class="inline-flex text-slate-400 hover:text-rose-500" @click.stop="dropHistory(item)"><X class="size-3" /></span>
        </button>
        <button class="btn btn-ghost btn-xs ml-auto" type="button" @click="wipeHistory">清空</button>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(260px,1fr)]">
      <div class="min-w-0">
        <div class="surface mb-4 flex items-center gap-2 p-3">
          <h1 class="mr-auto text-base font-semibold text-slate-800">
            「{{ keyword || '全部' }}」的搜索结果
          </h1>
          <button class="btn btn-sm" :class="type === 'question' ? 'btn-primary' : 'btn-ghost'" type="button" @click="go({ type: 'question', page: 1 })">问题</button>
          <button class="btn btn-sm" :class="type === 'user' ? 'btn-primary' : 'btn-ghost'" type="button" @click="go({ type: 'user', page: 1 })">用户</button>
        </div>

        <p v-if="keyword" class="mb-3 text-xs text-slate-400">
          共找到 {{ pagination?.allCount ?? 0 }} 条{{ type === 'user' ? '用户' : '问题' }}结果
        </p>

        <div v-if="loading && !data" class="surface p-10 text-center text-sm text-slate-400">正在搜索…</div>

        <template v-else-if="type === 'question'">
          <QuestionList :items="questions" empty-text="没有找到相关问题，换个关键词试试～" />
        </template>

        <template v-else>
          <div v-if="!users.length" class="surface p-10 text-center text-sm text-slate-400">没有找到相关用户</div>
          <div v-else class="grid gap-3 sm:grid-cols-2">
            <RouterLink v-for="user in users" :key="user.userId" :to="`/user/${user.userId}`" class="surface flex items-center gap-3 p-4 hover:border-indigo-200">
              <Avatar :src="user.headUrl" :name="user.userName" :size="44" />
              <div class="min-w-0">
                <p class="font-medium text-slate-800">{{ user.userName }}</p>
                <p class="line-clamp-1 text-xs text-slate-400">{{ user.simpleSelfIntroduction || user.email || '这个人很懒，什么都没写' }}</p>
              </div>
            </RouterLink>
          </div>
        </template>

        <Pagination :pagination="pagination" @change="go({ page: $event })" />
      </div>

      <SidePanel position="search" />
    </div>
  </div>
</template>
