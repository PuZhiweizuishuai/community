<script setup>
import { computed, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight } from '@lucide/vue'

const props = defineProps({
  pagination: { type: Object, default: null },
})
const emit = defineEmits(['change'])

const current = computed(() => Number(props.pagination?.nowPage || 1))
const total = computed(() => Math.max(1, Number(props.pagination?.totalPage || 1)))
const pages = computed(() => {
  const fromDto = props.pagination?.pages || []
  if (fromDto.length) return [...fromDto].sort((a, b) => Number(a) - Number(b))
  const from = Math.max(1, current.value - 2)
  const to = Math.min(total.value, from + 4)
  return Array.from({ length: Math.max(0, to - from + 1) }, (_, i) => from + i)
})
const canPrev = computed(() => current.value > 1)
const canNext = computed(() => current.value < total.value)
const jumpPage = ref('')

// 翻页后清空跳转输入，避免保留上一次的页码
watch(current, () => {
  jumpPage.value = ''
})

function go(page) {
  if (page < 1 || page > total.value || page === current.value) return
  emit('change', page)
}

function jump() {
  const raw = String(jumpPage.value ?? '').trim()
  if (!raw) return
  const page = Number(raw)
  if (!Number.isFinite(page)) return
  const target = Math.min(total.value, Math.max(1, Math.floor(page)))
  jumpPage.value = ''
  go(target)
}
</script>

<template>
  <div v-if="pagination" class="mt-5 flex flex-wrap items-center justify-center gap-1.5">
    <button class="btn btn-sm btn-ghost" :disabled="!canPrev" type="button" @click="go(current - 1)"><ChevronLeft class="size-3.5" />上一页</button>
    <button v-if="pagination.showFirstPage && !pages.includes(1)" class="btn btn-sm btn-ghost" type="button" @click="go(1)">1</button>
    <span v-if="pagination.showFirstPage && !pages.includes(1)" class="px-1 text-xs ui-text-muted">…</span>
    <button
      v-for="page in pages"
      :key="page"
      class="btn btn-sm"
      :class="Number(page) === current ? 'btn-primary' : 'btn-ghost'"
      type="button"
      @click="go(Number(page))"
    >
      {{ page }}
    </button>
    <span v-if="pagination.showEndPage && !pages.includes(total)" class="px-1 text-xs ui-text-muted">…</span>
    <button v-if="pagination.showEndPage && !pages.includes(total)" class="btn btn-sm btn-ghost" type="button" @click="go(total)">{{ total }}</button>
    <button class="btn btn-sm btn-ghost" :disabled="!canNext" type="button" @click="go(current + 1)">下一页<ChevronRight class="size-3.5" /></button>
    <span class="w-full text-center text-xs ui-text-muted sm:ml-2 sm:w-auto">共 {{ pagination.allCount ?? 0 }} 条 / {{ total }} 页</span>
    <form class="flex items-center gap-1.5 text-xs ui-text-muted" @submit.prevent="jump">
      <span>跳至</span>
      <input
        v-model="jumpPage"
        type="number"
        inputmode="numeric"
        min="1"
        :max="total"
        step="1"
        aria-label="跳转页码"
        class="input input-bordered input-xs w-14 text-center focus:outline-none"
      />
      <span>页</span>
      <button class="btn btn-xs btn-ghost" type="submit" :disabled="!String(jumpPage).trim()">跳转</button>
    </form>
  </div>
</template>
