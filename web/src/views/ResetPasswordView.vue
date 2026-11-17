<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock } from '@lucide/vue'
import { authApi } from '@/api'
import { useToastStore } from '@/stores/toast'
import AuthShell from '@/components/AuthShell.vue'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const token = computed(() => route.query.token || '')
const newPassword = ref('')
const confirm = ref('')
const submitting = ref(false)

async function submit() {
  if (!token.value) return toast.error('重置链接无效，请重新申请')
  if (newPassword.value.length < 6) return toast.warning('新密码至少 6 位')
  if (newPassword.value !== confirm.value) return toast.warning('两次输入的密码不一致')
  submitting.value = true
  try {
    await authApi.resetPassword({ token: token.value, newPassword: newPassword.value })
    toast.success('密码已重置，请使用新密码登录')
    router.push('/sign-in')
  } catch (error) {
    toast.error(error?.message || '重置失败，链接可能已过期')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthShell eyebrow="Reset password" title="重置密码" subtitle="设置一个新密码，重置后所有设备需重新登录">
    <div v-if="!token" class="alert alert-error py-2 text-sm">
      <span>重置链接无效，请回到登录页重新申请。</span>
    </div>
    <form v-else class="space-y-4" novalidate @submit.prevent="submit">
      <label class="form-control">
        <span class="label-text mb-1.5 font-medium ui-text-strong">新密码</span>
        <div class="relative">
          <span class="pointer-events-none absolute left-3 top-1/2 z-10 -translate-y-1/2 ui-text-muted"><Lock class="size-4" /></span>
          <input v-model="newPassword" type="password" class="input input-bordered w-full pl-9" placeholder="至少 6 位" autocomplete="new-password" />
        </div>
      </label>
      <label class="form-control">
        <span class="label-text mb-1.5 font-medium ui-text-strong">确认新密码</span>
        <div class="relative">
          <span class="pointer-events-none absolute left-3 top-1/2 z-10 -translate-y-1/2 ui-text-muted"><Lock class="size-4" /></span>
          <input v-model="confirm" type="password" class="input input-bordered w-full pl-9" placeholder="再输入一次" autocomplete="new-password" />
        </div>
      </label>
      <button class="btn btn-primary w-full" :disabled="submitting" type="submit">
        {{ submitting ? '重置中…' : '重置密码' }}
      </button>
    </form>
    <RouterLink to="/sign-in" class="btn btn-outline mt-4 w-full">返回登录</RouterLink>
  </AuthShell>
</template>