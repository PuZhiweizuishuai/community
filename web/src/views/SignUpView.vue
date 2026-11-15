<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Eye, EyeOff, X } from '@lucide/vue'
import { authApi } from '@/api'
import { useToastStore } from '@/stores/toast'
import Captcha from '@/components/Captcha.vue'
import AuthShell from '@/components/AuthShell.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const router = useRouter()
const toast = useToastStore()
const captchaRef = ref(null)
const submitting = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)
const emailTip = ref('')
const userIdTip = ref('')

const form = reactive({
  userId: '',
  userName: '',
  email: '',
  password: '',
  password2: '',
  sex: '保密',
  birthday: '',
  school: '',
  major: '',
  validateCode: '',
})

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()))
const userIdValid = computed(() => /^[A-Za-z0-9_-]{3,20}$/.test(form.userId.trim()))
const passwordScore = computed(() => {
  const value = form.password
  let score = 0
  if (value.length >= 6) score++
  if (value.length >= 10) score++
  if (/[A-Z]/.test(value) && /[a-z]/.test(value)) score++
  if (/\d/.test(value)) score++
  if (/[^A-Za-z0-9]/.test(value)) score++
  return Math.min(4, Math.max(0, score - 1))
})
const strengthLabel = computed(() => ['弱', '一般', '良好', '很强'][passwordScore.value] || '弱')
const strengthColor = computed(() => ['bg-rose-400', 'bg-amber-400', 'bg-sky-400', 'bg-emerald-500'][passwordScore.value])

async function checkEmail() {
  emailTip.value = ''
  if (!form.email || !emailValid.value) return
  const result = await authApi.checkEmail(form.email.trim())
  emailTip.value = result?.success === false ? result.msg : ''
}
async function checkUserId() {
  userIdTip.value = ''
  if (!form.userId.trim()) return
  const result = await authApi.checkUserId(form.userId.trim())
  userIdTip.value = result?.success === false ? result.msg : ''
}

async function register() {
  errorMessage.value = ''
  if (!userIdValid.value) return (errorMessage.value = '账号 ID 需为 3~20 位字母、数字、下划线或连字符')
  if (!form.userName.trim()) return (errorMessage.value = '请填写昵称')
  if (!emailValid.value) return (errorMessage.value = '请输入有效的邮箱地址')
  if (form.password.length < 6) return (errorMessage.value = '密码至少 6 位')
  if (form.password !== form.password2) return (errorMessage.value = '两次输入的密码不一致')
  if (!form.validateCode.trim()) return (errorMessage.value = '请输入验证码')

  submitting.value = true
  try {
    await authApi.register({
      userId: form.userId.trim(),
      userName: form.userName.trim(),
      email: form.email.trim(),
      password: form.password,
      sex: form.sex,
      birthday: form.birthday,
      school: form.school,
      major: form.major,
      validateCode: form.validateCode.trim(),
    })
    toast.success('注册成功，请登录')
    router.push('/sign-in')
  } catch (error) {
    errorMessage.value = error?.message || '注册失败，请稍后重试'
    form.validateCode = ''
    captchaRef.value?.refresh?.()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthShell eyebrow="Join the community" title="创建账号" subtitle="加入社区，与大家一起交流技术">
    <div class="mb-3 flex justify-end lg:hidden"><ThemeToggle /></div>

    <div v-if="errorMessage" class="alert alert-error mb-4 py-2 text-sm">
      <span>{{ errorMessage }}</span>
      <button class="btn btn-ghost btn-xs ml-auto" type="button" @click="errorMessage = ''"><X class="size-3.5" /></button>
    </div>

    <form class="space-y-5" novalidate @submit.prevent="register">
      <!-- 账号信息 -->
      <section>
        <h2 class="mb-2.5 flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <span class="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-50 text-[10px] text-indigo-600">1</span>
          账号信息
        </h2>
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="form-control">
            <span class="label-text mb-1 font-medium text-slate-700">账号 ID *</span>
            <input v-model="form.userId" class="input input-bordered input-sm" placeholder="用于个人主页地址" autocomplete="username" @blur="checkUserId" />
            <span v-if="userIdTip" class="label-text-alt mt-1 text-rose-500">{{ userIdTip }}</span>
            <span v-else class="label-text-alt mt-1 text-slate-400">3~20 位字母 / 数字 / _ -</span>
          </label>
          <label class="form-control">
            <span class="label-text mb-1 font-medium text-slate-700">昵称 *</span>
            <input v-model="form.userName" maxlength="20" class="input input-bordered input-sm" placeholder="展示给其他用户" />
          </label>
        </div>
        <label class="form-control mt-3">
          <span class="label-text mb-1 font-medium text-slate-700">邮箱 *</span>
          <input v-model="form.email" type="email" class="input input-bordered input-sm" placeholder="you@example.com" @blur="checkEmail" />
          <span v-if="emailTip" class="label-text-alt mt-1 text-rose-500">{{ emailTip }}</span>
        </label>
      </section>

      <!-- 安全设置 -->
      <section class="border-t border-slate-100 pt-4">
        <h2 class="mb-2.5 flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <span class="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-50 text-[10px] text-indigo-600">2</span>
          安全设置
        </h2>
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="form-control">
            <span class="label-text mb-1 font-medium text-slate-700">密码 *</span>
            <div class="relative">
              <input v-model="form.password" :type="showPassword ? 'text' : 'password'" class="input input-bordered input-sm w-full pr-12" placeholder="至少 6 位" autocomplete="new-password" />
              <button class="btn btn-ghost btn-xs absolute right-1 top-1/2 -translate-y-1/2" type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword"><component :is="showPassword ? EyeOff : Eye" class="size-4" /></button>
            </div>
          </label>
          <label class="form-control">
            <span class="label-text mb-1 font-medium text-slate-700">确认密码 *</span>
            <input v-model="form.password2" :type="showPassword ? 'text' : 'password'" class="input input-bordered input-sm" placeholder="再输入一次" autocomplete="new-password" />
          </label>
        </div>
        <div v-if="form.password" class="mt-2">
          <div class="flex gap-1">
            <span v-for="i in 4" :key="i" class="h-1 flex-1 rounded-full" :class="i <= passwordScore + 1 ? strengthColor : 'bg-slate-200'" />
          </div>
          <p class="mt-1 text-xs text-slate-400">密码强度：{{ strengthLabel }}</p>
        </div>
      </section>

      <!-- 补充资料 -->
      <section class="border-t border-slate-100 pt-4">
        <h2 class="mb-2.5 flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <span class="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-50 text-[10px] text-indigo-600">3</span>
          补充资料（选填）
        </h2>
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="form-control">
            <span class="label-text mb-1 text-slate-600">生日</span>
            <input v-model="form.birthday" type="date" class="input input-bordered input-sm" />
          </label>
          <label class="form-control">
            <span class="label-text mb-1 text-slate-600">性别</span>
            <select v-model="form.sex" class="select select-bordered select-sm"><option>保密</option><option>男</option><option>女</option></select>
          </label>
          <label class="form-control">
            <span class="label-text mb-1 text-slate-600">学校</span>
            <input v-model="form.school" class="input input-bordered input-sm" />
          </label>
          <label class="form-control">
            <span class="label-text mb-1 text-slate-600">专业</span>
            <input v-model="form.major" class="input input-bordered input-sm" />
          </label>
        </div>
      </section>

      <div class="border-t border-slate-100 pt-4">
        <span class="label-text mb-1.5 block font-medium text-slate-700">验证码 *</span>
        <Captcha ref="captchaRef" v-model="form.validateCode" placeholder="输入图中字符" />
      </div>

      <button class="btn btn-primary w-full" :disabled="submitting" type="submit">
        {{ submitting ? '注册中…' : '创建账号' }}
      </button>
    </form>

    <div class="my-5 flex items-center gap-3 text-xs text-slate-400">
      <span class="h-px flex-1 bg-slate-200" />
      已有账号？
      <span class="h-px flex-1 bg-slate-200" />
    </div>
    <RouterLink to="/sign-in" class="btn btn-outline w-full">去登录</RouterLink>

    <div class="mt-5 hidden justify-center lg:flex"><ThemeToggle /></div>
  </AuthShell>
</template>
