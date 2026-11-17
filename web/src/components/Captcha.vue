<script setup>
import { ref } from 'vue'
import { captchaUrl } from '@/api'

defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '请输入验证码' },
})
const emit = defineEmits(['update:modelValue'])

const url = ref(captchaUrl())
function refresh() {
  url.value = captchaUrl()
  emit('update:modelValue', '')
}
defineExpose({ refresh })
</script>

<template>
  <div class="flex items-center gap-2">
    <input
      :value="modelValue"
      type="text"
      class="input input-bordered input-sm flex-1 focus:border-indigo-400 focus:outline-none"
      :placeholder="placeholder"
      maxlength="8"
      autocomplete="off"
      @input="emit('update:modelValue', $event.target.value)"
    />
    <button type="button" class="h-10 w-[130px] shrink-0 overflow-hidden rounded border ui-border" title="点击刷新验证码" @click="refresh">
      <img :src="url" alt="验证码" class="h-full w-full object-cover" />
    </button>
  </div>
</template>
