<script setup>
import { TriangleAlert } from '@lucide/vue'
import { useConfirmStore } from '@/stores/confirm'

const confirm = useConfirmStore()
</script>

<template>
  <dialog v-if="confirm.request" class="modal modal-open">
    <div class="modal-box max-w-sm">
      <div class="mb-2 flex items-center gap-2">
        <TriangleAlert v-if="confirm.request.danger" class="size-5 shrink-0 text-rose-500" />
        <h3 class="text-base font-semibold">{{ confirm.request.title }}</h3>
      </div>
      <p class="whitespace-pre-line text-sm leading-relaxed">{{ confirm.request.message }}</p>
      <div class="modal-action">
        <button class="btn btn-ghost btn-sm" type="button" @click="confirm.settle(false)">{{ confirm.request.cancelText }}</button>
        <button
          class="btn btn-sm"
          :class="confirm.request.danger ? 'btn-error' : 'btn-primary'"
          type="button"
          @click="confirm.settle(true)"
        >{{ confirm.request.confirmText }}</button>
      </div>
    </div>
  </dialog>
</template>
