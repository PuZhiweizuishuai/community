<script setup>
import { computed, watch } from 'vue'
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

watch(
  () => [props.position, props.showNews, props.showHotTags, props.showHotQuestions],
  () => {
    if (props.showHotTags || props.showHotQuestions) meta.loadHot()
    meta.loadAds(props.position)
    if (props.showNews) meta.loadAds('news')
  },
  { immediate: true },
)
</script>

<template>
  <aside class="side-panel">
    <section v-if="tagClass" class="surface p-4">
      <div class="flex items-start gap-3">
        <img :src="assetUrl(tagClass.image)" :alt="tagClass.title" class="h-12 w-12 rounded-lg object-cover" />
        <div class="min-w-0">
          <RouterLink :to="`/topics/${tagClass.id}`" class="font-semibold ui-text-strong hover:text-indigo-600">{{ tagClass.title }}</RouterLink>
          <p class="line-clamp-2 mt-1 text-xs ui-text-base">{{ tagClass.simpleDesc || '暂无话题简介' }}</p>
        </div>
      </div>
      <div class="mt-3 flex items-center justify-between text-xs ui-text-base">
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
      <h3 class="section-title mb-3"><Tags class="size-4 ui-text-brand" />热门标签</h3>
      <div class="flex flex-wrap gap-2">
        <RouterLink
          v-for="tag in meta.hots.slice(0, 20)"
          :key="tag"
          :to="{ name: 'home', query: { tag, page: 1 } }"
          class="chip"
        >
          {{ tag }}
        </RouterLink>
      </div>
    </section>

    <section v-if="showHotQuestions && meta.hotQuestions.length" class="surface p-4">
      <h3 class="section-title mb-3"><Flame class="size-4 text-orange-500" />热门问题</h3>
      <ol class="space-y-2.5">
        <li v-for="(item, i) in meta.hotQuestions.slice(0, 8)" :key="item.questionId" class="flex items-start gap-2 text-sm">
          <span
            class="flex h-5 w-5 shrink-0 items-center justify-center rounded text-xs font-bold"
            :class="i < 3 ? 'rank-top text-white' : 'ui-bg-soft ui-text-base'"
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
