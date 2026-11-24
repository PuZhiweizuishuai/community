<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { commentApi } from '@/api'
import Captcha from '@/components/Captcha.vue'

const props = defineProps({
  questionId: { type: Number, required: true },
  /** 被回复的评论 id；topLevel 为 true 时忽略 */
  targetCommentId: { type: Number, default: 0 },
  targetName: { type: String, default: '' },
  /** 直接评论问题（一级评论），消息中心里对方的那条评论已不存在时使用 */
  topLevel: { type: Boolean, default: false },
})
const emit = defineEmits(['created', 'cancel', 'error'])

const content = ref('')
const captcha = ref('')
// 验证码失效后通过换 key 重新挂载，强制换一张新图
const captchaKey = ref(0)
const submitting = ref(false)
const errorText = ref('')
const textareaRef = ref(null)

const placeholder = computed(() => {
  if (props.topLevel) return '友善地表达你的观点…'
  return props.targetName ? `回复 @${props.targetName}…` : '友善地表达你的观点…'
})

onMounted(() => nextTick(() => textareaRef.value?.focus?.()))

async function submit() {
  const text = content.value.trim()
  if (!text || submitting.value) return
  errorText.value = ''
  submitting.value = true
  try {
    const created = await commentApi.create({
      questionId: props.questionId,
      parentId: props.topLevel ? props.questionId : props.targetCommentId,
      content: text,
      type: props.topLevel ? 1 : 2,
      captcha: captcha.value,
    })
    content.value = ''
    emit('created', created)
  } catch (err) {
    errorText.value = err?.message || '发布失败，请稍后重试'
    // 验证码一次性使用，失败后立即换一张，避免用户重试时一直报验证码错误
    captcha.value = ''
    captchaKey.value += 1
    emit('error', err)
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
      class="textarea textarea-bordered w-full min-h-[6.5rem] resize-y text-sm focus:outline-none sm:min-h-[4.5rem]"
      :placeholder="placeholder"
      @keydown.ctrl.enter.prevent="submit"
      @keydown.meta.enter.prevent="submit"
    />
    <p v-if="errorText" class="mt-1.5 text-xs text-error">{{ errorText }}</p>
    <div class="mt-2 flex flex-wrap items-center gap-2">
      <div class="w-full sm:w-56"><Captcha :key="captchaKey" v-model="captcha" /></div>
      <div class="ml-auto flex items-center gap-1.5">
        <button class="btn btn-ghost btn-xs sm:btn-sm" type="button" @click="emit('cancel')">
          取消
        </button>
        <button
          class="btn btn-primary btn-xs sm:btn-sm"
          type="button"
          :disabled="submitting || !content.trim()"
          @click="submit"
        >
          {{ submitting ? '发布中…' : '发布回复' }}
        </button>
      </div>
    </div>
  </div>
</template>
