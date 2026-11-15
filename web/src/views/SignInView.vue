<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Eye, EyeOff, Lock, Mail, X } from '@lucide/vue'
import { authApi } from '@/api'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import Captcha from '@/components/Captcha.vue'
import AuthShell from '@/components/AuthShell.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const toast = useToastStore()

const email = ref('')
const password = ref('')
const captcha = ref('')
const remember = ref(false)
const showPassword = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const touched = ref(false)
const captchaRef = ref(null)

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim()))
const passwordValid = computed(() => password.value.length >= 6)

async function login() {
  touched.value = true
  errorMessage.value = ''
  if (!emailValid.value) return (errorMessage.value = '请输入有效的邮箱地址')
  if (!passwordValid.value) return (errorMessage.value = '密码至少 6 位')
  if (!captcha.value.trim()) return (errorMessage.value = '请输入验证码')

  submitting.value = true
  try {
    const result = await authApi.login({
      email: email.value.trim(),
      password: password.value,
      captcha: captcha.value.trim(),
      remember: remember.value,
    })
    session.applyLogin(result)
    await session.refresh(true)
    toast.success('欢迎回来！')
    router.push(route.query.redirect || '/')
  } catch (error) {
    errorMessage.value = error?.message || '登录失败，请稍后重试'
    captcha.value = ''
    captchaRef.value?.refresh?.()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthShell eyebrow="Welcome back" title="登录账号" subtitle="欢迎回来，继续参与社区讨论">
    <div class="mb-5 flex justify-end lg:hidden">
      <ThemeToggle />
    </div>

    <div v-if="errorMessage" class="alert alert-error mb-4 py-2 text-sm">
      <span>{{ errorMessage }}</span>
      <button class="btn btn-ghost btn-xs ml-auto" type="button" @click="errorMessage = ''"><X class="size-3.5" /></button>
    </div>

    <form class="space-y-4" novalidate @submit.prevent="login">
      <label class="form-control">
        <span class="label-text mb-1.5 font-medium text-slate-700">邮箱</span>
        <div class="relative">
          <span class="pointer-events-none absolute left-3 top-1/2 z-10 -translate-y-1/2 text-slate-400"><Mail class="size-4" /></span>
          <input
            v-model="email"
            type="email"
            class="input input-bordered w-full pl-9 focus:border-indigo-400 focus:outline-none"
            :class="touched && !emailValid ? 'input-error' : ''"
            placeholder="you@example.com"
            autocomplete="email"
          />
        </div>
        <span v-if="touched && !emailValid" class="label-text-alt mt-1 text-rose-500">请输入有效的邮箱地址</span>
      </label>

      <label class="form-control">
        <span class="label-text mb-1.5 flex justify-between font-medium text-slate-700">
          密码
          <span class="text-xs font-normal text-slate-400">至少 6 位</span>
        </span>
        <div class="relative">
          <span class="pointer-events-none absolute left-3 top-1/2 z-10 -translate-y-1/2 text-slate-400"><Lock class="size-4" /></span>
          <input
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            class="input input-bordered w-full pl-9 pr-10 focus:border-indigo-400 focus:outline-none"
            :class="touched && !passwordValid ? 'input-error' : ''"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
          <button
            class="btn btn-ghost btn-xs absolute right-1 top-1/2 -translate-y-1/2"
            type="button"
            :aria-label="showPassword ? '隐藏密码' : '显示密码'"
            @click="showPassword = !showPassword"
          >
            <component :is="showPassword ? EyeOff : Eye" class="size-4" />
          </button>
        </div>
      </label>

      <div>
        <span class="label-text mb-1.5 block font-medium text-slate-700">验证码</span>
        <Captcha ref="captchaRef" v-model="captcha" placeholder="输入图中字符" />
      </div>

      <div class="flex items-center justify-between text-sm">
        <label class="flex cursor-pointer items-center gap-2 text-slate-600">
          <input v-model="remember" type="checkbox" class="checkbox checkbox-sm checkbox-primary" />
          记住我（7 天）
        </label>
        <button class="link text-xs text-slate-400 hover:text-indigo-600" type="button" @click="toast.info('找回密码功能即将上线')">忘记密码？</button>
      </div>

      <button class="btn btn-primary w-full" :disabled="submitting" type="submit">
        {{ submitting ? '登录中…' : '登录' }}
      </button>
    </form>

    <div class="my-5 flex items-center gap-3 text-xs text-slate-400">
      <span class="h-px flex-1 bg-slate-200" />
      还没有账号？
      <span class="h-px flex-1 bg-slate-200" />
    </div>
    <RouterLink to="/sign-up" class="btn btn-outline w-full">创建账号</RouterLink>

    <div class="mt-5 hidden justify-center lg:flex">
      <ThemeToggle />
    </div>
  </AuthShell>
</template>
