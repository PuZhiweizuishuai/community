<script setup>
import { ref, watch } from 'vue'
import { assetUrl } from '@/api'

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '?' },
  size: { type: Number, default: 36 },
  rounded: { type: Boolean, default: true },
})

const fallback = ref(false)
watch(() => props.src, () => (fallback.value = false))

const url = () => (fallback.value ? '/image/head/nohead.png' : assetUrl(props.src))
</script>

<template>
  <span
    class="inline-flex shrink-0 items-center justify-center overflow-hidden bg-indigo-50 font-semibold text-indigo-600"
    :class="rounded ? 'rounded-full' : 'rounded-lg'"
    :style="{ width: size + 'px', height: size + 'px', fontSize: Math.max(11, size / 2.6) + 'px' }"
  >
    <img
      v-if="src || fallback"
      :src="url()"
      :alt="name"
      class="h-full w-full object-cover"
      @error="fallback = true"
    />
    <span v-else>{{ (name || '?').slice(0, 1).toUpperCase() }}</span>
  </span>
</template>
