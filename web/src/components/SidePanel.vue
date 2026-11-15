<script setup>
import { computed } from 'vue'
import { Flame, Star, Tags } from '@lucide/vue'
import { useMetaStore } from '@/stores/meta'
import { assetUrl } from '@/api'
import AdCarousel from '@/components/AdCarousel.vue'

const props = defineProps({
  position: { type: String, default: 'home' },
  tagClass: { type: Object, default: null },
  followed: { type: Boolean, default: false },
  showNews: { type: Boolean, default: true },
  showHotTags: { type: Boolean, default: true },
  showHotQuestions: { type: Boolean, default: true },
})
defineEmits(['follow-topic'])

const meta = useMetaStore()
const ads = computed(() => meta.adsFor(props.position))
const news = computed(() => meta.adsFor('news'))
</script>

<template>
  <aside class="side-panel">
    <section v-if="tagClass" class="surface p-4">
      <div class="flex items-start gap-3">
        <img :src="assetUrl(tagClass.image)" :alt="tagClass.title" class="h-12 w-12 rounded-lg object-cover" />
        <div class="min-w-0">
          <h3 class="font-semibold text-slate-800">{{ tagClass.title }}</h3>
          <p class="line-clamp-2 mt-1 text-xs text-slate-500">{{ tagClass.simpleDesc || '暂无话题简介' }}</p>
        </div>
      </div>
      <div class="mt-3 flex items-center justify-between text-xs text-slate-500">
        <span>{{ tagClass.followCount }} 人关注</span>
        <span>{{ tagClass.talkCount }} 条讨论</span>
      </div>
      <button
        class="btn btn-sm mt-3 w-full"
        :class="followed ? 'btn-outline' : 'btn-primary'"
        type="button"
        @click="$emit('follow-topic', tagClass)"
      >
        <Star class="size-4" />
        {{ followed ? '取消关注' : '关注话题' }}
      </button>
    </section>

    <AdCarousel :items="ads" title="社区推广" />

    <section v-if="showHotTags && meta.hots.length" class="surface p-4">
      <h3 class="mb-3 flex items-center gap-1.5 text-sm font-semibold text-slate-700"><Tags class="size-4 text-indigo-500" />热门标签</h3>
      <div class="flex flex-wrap gap-2">
        <RouterLink
          v-for="tag in meta.hots.slice(0, 20)"
          :key="tag"
          :to="{ name: 'home', query: { tag, page: 1 } }"
          class="badge badge-lg border-slate-200 bg-slate-50 text-slate-600 hover:border-indigo-300 hover:bg-indigo-50 hover:text-indigo-600"
        >
          {{ tag }}
        </RouterLink>
      </div>
    </section>

    <section v-if="showHotQuestions && meta.hotQuestions.length" class="surface p-4">
      <h3 class="mb-3 flex items-center gap-1.5 text-sm font-semibold text-slate-700"><Flame class="size-4 text-orange-500" />热门问题</h3>
      <ol class="space-y-2.5">
        <li v-for="(item, i) in meta.hotQuestions.slice(0, 8)" :key="item.questionId" class="flex items-start gap-2 text-sm">
          <span
            class="flex h-5 w-5 shrink-0 items-center justify-center rounded text-xs font-bold"
            :class="i < 3 ? 'bg-rose-500 text-white' : 'bg-slate-100 text-slate-500'"
          >
            {{ i + 1 }}
          </span>
          <RouterLink :to="`/question/${item.questionId}`" class="line-clamp-2 link-muted">
            {{ item.title }}
          </RouterLink>
        </li>
      </ol>
    </section>

    <AdCarousel v-if="showNews" :items="news" title="New! 社区新闻" />
  </aside>
</template>
