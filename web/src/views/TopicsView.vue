<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tags } from '@lucide/vue'
import { discoverApi, assetUrl } from '@/api'
import { useMetaStore } from '@/stores/meta'
import SidePanel from '@/components/SidePanel.vue'

const route = useRoute()
const router = useRouter()
const meta = useMetaStore()
const type = computed(() => route.query.type || '1')
const data = ref(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    data.value = await discoverApi.topics(type.value)
  } finally {
    loading.value = false
  }
}
watch(type, load, { immediate: true })
</script>

<template>
  <div class="page-container">
    <div class="surface mb-4 flex flex-wrap items-center gap-2 p-4">
      <h1 class="mr-2 flex items-center gap-1.5 text-base font-semibold text-slate-800"><Tags class="size-4 text-indigo-500" />话题分类</h1>
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
    </div>

    <div class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(260px,1fr)]">
      <div class="min-w-0">
        <div v-if="loading && !data" class="surface p-10 text-center text-sm text-slate-400">加载中…</div>
        <div v-else-if="!data?.tags?.length" class="surface p-10 text-center text-sm text-slate-400">该分类下暂时没有话题</div>
        <div v-else class="grid gap-3 sm:grid-cols-2">
          <RouterLink
            v-for="tag in data.tags"
            :key="tag.id || tag.title"
            :to="{ name: 'home', query: { tag: tag.title } }"
            class="surface flex items-start gap-3 p-4 hover:border-indigo-200"
          >
            <img :src="assetUrl(tag.image)" :alt="tag.title" class="h-14 w-14 shrink-0 rounded-lg object-cover" />
            <div class="min-w-0">
              <h2 class="font-medium text-slate-800">{{ tag.title }}</h2>
              <p class="line-clamp-2 mt-1 text-xs text-slate-500">{{ tag.simpleDesc || '暂无简介' }}</p>
              <p class="mt-2 text-xs text-slate-400">{{ tag.followCount }} 人关注 · {{ tag.talkCount }} 条讨论</p>
            </div>
          </RouterLink>
        </div>
      </div>

      <SidePanel position="class" />
    </div>
  </div>
</template>
