<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { commentApi } from '@/api'
import Captcha from '@/components/Captcha.vue'

const props = defineProps({
  questionId: { type: Number, required: true },
  targetCommentId: { type: Number, required: true },
  targetName: { type: String, default: '' },
})
const emit = defineEmits(['created', 'cancel'])

const content = ref('')
const captcha = ref('')
// 验证码失效后通过换 key 重新挂载，强制换一张新图
const captchaKey = ref(0)
const submitting = ref(false)
const textareaRef = ref(null)

onMounted(() => nextTick(() => textareaRef.value?.focus?.()))

async function submit() {
  const text = content.value.trim()
  if (!text) return
  submitting.value = true
  try {
    const created = await commentApi.create({
      questionId: props.questionId,
      parentId: props.targetCommentId,
      content: text,
      type: 2,
      captcha: captcha.value,
    })
    emit('created', created)
  } catch {
    // 验证码一次性使用，失败后立即换一张，避免用户重试时一直报验证码错误
    captcha.value = ''
    captchaKey.value += 1
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="mt-3 rounded-lg border ui-border ui-bg-soft p-3">
    <textarea
      ref="textareaRef"
      v-model="content"
      rows="3"
      maxlength="5000"
      class="textarea textarea-bordered w-full resize-y text-sm focus:outline-none"
      :placeholder="targetName ? `回复 @${targetName}…` : '友善地表达你的观点…'"
      @keydown.ctrl.enter.prevent="submit"
      @keydown.meta.enter.prevent="submit"
    />
    <div class="mt-2 flex flex-wrap items-center gap-2">
      <div class="w-56"><Captcha :key="captchaKey" v-model="captcha" /></div>
      <div class="ml-auto flex items-center gap-1.5">
        <button class="btn btn-ghost btn-xs" type="button" @click="emit('cancel')">取消</button>
        <button class="btn btn-primary btn-xs" type="button" :disabled="submitting || !content.trim()" @click="submit">
          {{ submitting ? '发布中…' : '发布回复' }}
        </button>
      </div>
    </div>
  </div>
</template>
