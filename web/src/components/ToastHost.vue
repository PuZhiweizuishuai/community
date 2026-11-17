<script setup>
import { CircleCheck, CircleX, Info, TriangleAlert, X } from '@lucide/vue'

defineProps({
  items: { type: Array, default: () => [] },
})
defineEmits(['close'])

const typeClass = {
  success: 'alert-success',
  error: 'alert-error',
  warning: 'alert-warning',
  info: 'alert-info',
}
const icon = { success: CircleCheck, error: CircleX, warning: TriangleAlert, info: Info }
</script>

<template>
  <div class="toast toast-top toast-end z-[120] gap-2">
    <div
      v-for="item in items"
      :key="item.id"
      class="alert flex max-w-full items-center gap-3 py-2.5 text-sm"
      :class="typeClass[item.type] || 'alert-info'"
    >
      <span class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-white/30">
        <component :is="icon[item.type] || Info" class="size-3.5" />
      </span>
      <span class="max-w-xs">{{ item.message }}</span>
      <button class="btn btn-ghost btn-xs" aria-label="关闭" @click="$emit('close', item.id)"><X class="size-3.5" /></button>
    </div>
  </div>
</template>
