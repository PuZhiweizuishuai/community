<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Lock, Mail } from '@lucide/vue'
import { authApi } from '@/api'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import Captcha from '@/components/Captcha.vue'
import SiteLogo from '@/components/SiteLogo.vue'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const toast = useToastStore()
const email = ref('')
const password = ref('')
const validateCode = ref('')
const submitting = ref(false)
const captchaRef = ref(null)

async function login() {
  if (!email.value || !password.value || !validateCode.value) return toast.warning('请填写完整登录信息')
  submitting.value = true
  try {
    await authApi.adminLogin({ email: email.value, password: password.value, validateCode: validateCode.value })
    const data = await session.refresh(true)
    if (!data?.isAdmin) {
      toast.error('当前账号没有后台访问权限，请联系超级管理员分配角色')
      return
    }
    toast.success('登录成功')
    router.push(route.query.redirect || '/admin/dashboard')
  } catch {
    validateCode.value = ''
    captchaRef.value?.refresh?.()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="flex min-h-[100dvh] items-center justify-center bg-slate-900 px-4">
    <div class="w-full max-w-sm rounded-xl ui-bg p-7 shadow-2xl">
      <div class="mb-6 text-center">
        <SiteLogo :size="44" rounded="0.75rem" class="mx-auto mb-3" />
        <h1 class="text-lg font-semibold ui-text-strong">社区管理后台</h1>
        <p class="mt-1 text-xs ui-text-muted">仅限管理员登录</p>
      </div>
      <form class="space-y-3" @submit.prevent="login">
        <label class="form-control">
          <span class="label-text mb-1 font-medium">管理员邮箱</span>
          <div class="relative">
            <span class="pointer-events-none absolute left-3 top-1/2 z-10 -translate-y-1/2 ui-text-muted"><Mail class="size-4" /></span>
            <input v-model="email" type="email" class="input input-bordered input-sm w-full pl-9" />
          </div>
        </label>
        <label class="form-control">
          <span class="label-text mb-1 font-medium">密码</span>
          <div class="relative">
            <span class="pointer-events-none absolute left-3 top-1/2 z-10 -translate-y-1/2 ui-text-muted"><Lock class="size-4" /></span>
            <input v-model="password" type="password" class="input input-bordered input-sm w-full pl-9" />
          </div>
        </label>
        <div><span class="label-text mb-1 block font-medium">验证码</span><Captcha ref="captchaRef" v-model="validateCode" /></div>
        <button class="btn btn-primary w-full" :disabled="submitting" type="submit">{{ submitting ? '登录中…' : '登录后台' }}</button>
        <RouterLink to="/" class="flex items-center justify-center gap-1 text-center text-xs ui-text-muted hover:text-indigo-600"><ArrowLeft class="size-3" />返回前台</RouterLink>
      </form>
    </div>
  </div>
</template>
