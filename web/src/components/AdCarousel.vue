<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, Megaphone } from '@lucide/vue'
import { assetUrl, metaApi } from '@/api'

const props = defineProps({
  items: { type: Array, default: () => [] },
  title: { type: String, default: '推广' },
  autoplay: { type: Boolean, default: true },
})

const list = computed(() => props.items.filter(Boolean))
const index = ref(0)
let timer = null

watch(
  list,
  (value) => {
    if (index.value >= value.length) index.value = 0
  },
  { deep: true },
)

function start() {
  if (!props.autoplay || timer) return
  timer = setInterval(() => {
    if (list.value.length > 1) index.value = (index.value + 1) % list.value.length
  }, 5000)
}
function stop() {
  if (timer) clearInterval(timer)
  timer = null
}
watch(() => props.autoplay, (value) => (value ? start() : stop()), { immediate: true })
onBeforeUnmount(stop)

const current = computed(() => list.value[index.value] || null)

async function openAd(ad) {
  let target = ad?.url || '/'
  try {
    const result = await metaApi.adClick(ad.url, ad.id)
    if (result?.url) target = result.url
  } catch {
    /* 统计失败不影响跳转 */
  }
  if (target.startsWith('http')) window.open(target, '_blank', 'noopener')
  else window.location.href = target
}
</script>

<template>
  <section v-if="list.length" class="surface overflow-hidden">
    <div class="flex items-center justify-between border-b border-slate-100 px-4 py-2.5">
      <h3 class="flex items-center gap-1.5 text-sm font-semibold text-slate-700"><Megaphone class="size-4 text-indigo-500" />{{ title }}</h3>
      <span class="text-xs text-slate-400">{{ index + 1 }} / {{ list.length }}</span>
    </div>
    <div class="relative" @mouseenter="stop" @mouseleave="start">
      <button
        v-if="current"
        type="button"
        class="group block w-full text-left"
        @click="openAd(current)"
      >
        <img :src="assetUrl(current.image)" :alt="current.title" class="h-36 w-full object-cover" />
        <span class="line-clamp-1 block px-4 py-2 text-sm text-slate-700 group-hover:text-indigo-600">
          {{ current.title }}
        </span>
      </button>
      <div v-if="list.length > 1" class="absolute inset-x-0 bottom-9 flex items-center justify-between px-2">
        <button class="btn btn-circle btn-xs bg-white/80" type="button" aria-label="上一条" @click.stop="index = (index - 1 + list.length) % list.length"><ChevronLeft class="size-4" /></button>
        <button class="btn btn-circle btn-xs bg-white/80" type="button" aria-label="下一条" @click.stop="index = (index + 1) % list.length"><ChevronRight class="size-4" /></button>
      </div>
    </div>
  </section>
  <section v-else class="surface px-4 py-6 text-center text-sm text-slate-400">
    <p class="font-medium text-slate-500">{{ title }}</p>
    <p class="mt-1">广告位招租 · 虚位以待</p>
  </section>
</template>
