<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Eye, EyeOff, X } from '@lucide/vue'
import { authApi } from '@/api'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import Captcha from '@/components/Captcha.vue'
import AuthShell from '@/components/AuthShell.vue'

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
const captchaRef = ref(null)

// 找回密码弹窗
const mailEnabled = ref(false)
const forgotOpen = ref(false)
const forgotEmail = ref('')
const forgotCaptcha = ref('')
const forgotSubmitting = ref(false)
const forgotCaptchaRef = ref(null)

onMounted(async () => {
  try {
    const features = await authApi.features()
    mailEnabled.value = !!features?.mailEnabled
  } catch {
    mailEnabled.value = false
  }
})

function openForgot() {
  if (!mailEnabled.value) {
    toast.info('邮件找回未启用，请联系管理员重置密码')
    return
  }
  forgotEmail.value = email.value.trim()
  forgotCaptcha.value = ''
  forgotOpen.value = true
}

function closeForgot() {
  forgotOpen.value = false
  // 弹窗与登录共用会话验证码，关闭后刷新登录验证码
  captcha.value = ''
  captchaRef.value?.refresh?.()
}

async function submitForgot() {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(forgotEmail.value.trim())) {
    return toast.warning('请输入有效的邮箱地址')
  }
  if (!forgotCaptcha.value.trim()) return toast.warning('请输入验证码')
  forgotSubmitting.value = true
  try {
    await authApi.forgotPassword({ email: forgotEmail.value.trim(), captcha: forgotCaptcha.value.trim() })
    toast.success('重置链接已发送到邮箱，30 分钟内有效')
    closeForgot()
  } catch (error) {
    toast.error(error?.message || '发送失败，请稍后重试')
    forgotCaptcha.value = ''
    forgotCaptchaRef.value?.refresh?.()
  } finally {
    forgotSubmitting.value = false
  }
}

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim()))
const passwordValid = computed(() => password.value.length >= 6)

async function login() {
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
  <AuthShell title="登录" subtitle="欢迎回来，继续参与社区讨论">
    <div v-if="errorMessage" class="alert alert-error mb-4 py-2 text-sm">
      <span>{{ errorMessage }}</span>
      <button class="btn btn-ghost btn-xs ml-auto" type="button" @click="errorMessage = ''"><X class="size-3.5" /></button>
    </div>

    <form class="space-y-4" novalidate @submit.prevent="login">
      <label class="form-control">
        <span class="label-text mb-1 text-sm font-medium ui-text-strong">邮箱</span>
        <input
          v-model="email"
          type="email"
          class="input input-bordered w-full focus:border-indigo-400 focus:outline-none"
          placeholder="you@example.com"
          autocomplete="email"
        />
      </label>

      <label class="form-control">
        <span class="label-text mb-1 text-sm font-medium ui-text-strong">密码</span>
        <div class="relative">
          <input
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            class="input input-bordered w-full pr-10 focus:border-indigo-400 focus:outline-none"
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
        <span class="label-text mb-1 block text-sm font-medium ui-text-strong">验证码</span>
        <Captcha ref="captchaRef" v-model="captcha" placeholder="输入图中字符" />
      </div>

      <div class="flex items-center justify-between text-sm">
        <label class="flex cursor-pointer items-center gap-2 ui-text-base">
          <input v-model="remember" type="checkbox" class="checkbox checkbox-sm checkbox-primary" />
          记住我
        </label>
        <button class="text-xs ui-text-muted hover:text-indigo-600" type="button" @click="openForgot">忘记密码？</button>
      </div>

      <button class="btn btn-primary w-full" :disabled="submitting" type="submit">
        {{ submitting ? '登录中…' : '登录' }}
      </button>
    </form>

    <p class="mt-5 text-center text-sm ui-text-base">
      还没有账号？
      <RouterLink to="/sign-up" class="font-medium ui-text-brand hover:underline">立即注册</RouterLink>
    </p>

    <!-- 找回密码弹窗 -->
    <div v-if="forgotOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" @click.self="closeForgot">
      <div class="surface w-full max-w-sm space-y-4 p-6">
        <div class="flex items-center justify-between">
          <h2 class="text-base font-semibold ui-text-strong">找回密码</h2>
          <button class="btn btn-ghost btn-xs" type="button" aria-label="关闭" @click="closeForgot"><X class="size-4" /></button>
        </div>
        <p class="text-xs ui-text-muted">输入注册邮箱，我们会发送一封含重置链接的邮件（30 分钟内有效）。</p>
        <label class="form-control">
          <span class="label-text mb-1 text-sm font-medium ui-text-strong">邮箱</span>
          <input v-model="forgotEmail" type="email" class="input input-bordered w-full" placeholder="you@example.com" />
        </label>
        <div>
          <span class="label-text mb-1 block text-sm font-medium ui-text-strong">验证码</span>
          <Captcha ref="forgotCaptchaRef" v-model="forgotCaptcha" placeholder="输入图中字符" />
        </div>
        <button class="btn btn-primary w-full" type="button" :disabled="forgotSubmitting" @click="submitForgot">
          {{ forgotSubmitting ? '发送中…' : '发送重置链接' }}
        </button>
      </div>
    </div>
  </AuthShell>
</template>
