<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Eye, EyeOff, X } from '@lucide/vue'
import { authApi } from '@/api'
import { useToastStore } from '@/stores/toast'
import Captcha from '@/components/Captcha.vue'
import AuthShell from '@/components/AuthShell.vue'
import { checkPassword, PASSWORD_HINT } from '@/utils/validate'

const router = useRouter()
const toast = useToastStore()
const captchaRef = ref(null)
const submitting = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)
const emailTip = ref('')
const userIdTip = ref('')

// 邮件服务启用时才需要邮箱验证码；注册开关由后台系统配置控制
const mailEnabled = ref(false)
const registerEnabled = ref(true)
const mailCode = ref('')
const sendingCode = ref(false)
const codeCooldown = ref(0)

onMounted(async () => {
  try {
    const features = await authApi.features()
    mailEnabled.value = !!features?.mailEnabled
    registerEnabled.value = features?.registerEnabled !== false
  } catch {
    mailEnabled.value = false
  }
})

async function sendMailCode() {
  if (!emailValid.value) return toast.warning('请先填写有效的邮箱地址')
  if (!form.validateCode.trim()) return toast.warning('请先输入图形验证码')
  sendingCode.value = true
  try {
    await authApi.sendRegisterCode({ email: form.email.trim(), captcha: form.validateCode.trim() })
    toast.success('邮箱验证码已发送，10 分钟内有效')
    codeCooldown.value = 60
    const timer = setInterval(() => {
      codeCooldown.value -= 1
      if (codeCooldown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (error) {
    toast.error(error?.message || '发送失败，请稍后重试')
  } finally {
    sendingCode.value = false
    form.validateCode = ''
    captchaRef.value?.refresh?.()
  }
}

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

async function checkEmail() {
  emailTip.value = ''
  if (!form.email || !emailValid.value) return
  const result = await authApi.checkEmail(form.email.trim())
  emailTip.value = result?.available === false ? result.message : ''
}
async function checkUserId() {
  userIdTip.value = ''
  if (!form.userId.trim()) return
  const result = await authApi.checkUserId(form.userId.trim())
  userIdTip.value = result?.available === false ? result.message : ''
}

async function register() {
  errorMessage.value = ''
  if (!registerEnabled.value) return (errorMessage.value = '站点已关闭新用户注册')
  if (!userIdValid.value) return (errorMessage.value = '账号 ID 需为 3~20 位字母、数字、下划线或连字符')
  if (!form.userName.trim()) return (errorMessage.value = '请填写昵称')
  if (!emailValid.value) return (errorMessage.value = '请输入有效的邮箱地址')
  if (checkPassword(form.password)) return (errorMessage.value = checkPassword(form.password))
  if (form.password !== form.password2) return (errorMessage.value = '两次输入的密码不一致')
  if (!form.validateCode.trim()) return (errorMessage.value = '请输入验证码')
  if (mailEnabled.value && !mailCode.value.trim()) return (errorMessage.value = '请输入邮箱验证码')

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
      mailCode: mailCode.value.trim(),
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
  <AuthShell title="创建账号" subtitle="加入社区，与大家一起交流技术">
    <div v-if="errorMessage" class="alert alert-error mb-4 py-2 text-sm">
      <span>{{ errorMessage }}</span>
      <button class="btn btn-ghost btn-xs ml-auto" type="button" @click="errorMessage = ''"><X class="size-3.5" /></button>
    </div>

    <div v-if="!registerEnabled" class="alert alert-warning mb-4 py-3 text-sm">
      <span>站点已关闭新用户注册，如有疑问请联系管理员。已有账号可直接登录。</span>
    </div>

    <form v-if="registerEnabled" class="space-y-4" novalidate @submit.prevent="register">
      <div class="grid gap-3 sm:grid-cols-2">
        <label class="form-control">
          <span class="label-text mb-1 text-sm font-medium ui-text-strong">账号 ID</span>
          <input v-model="form.userId" class="input input-bordered w-full" placeholder="用于个人主页地址" autocomplete="username" @blur="checkUserId" />
          <span v-if="userIdTip" class="label-text-alt mt-1 text-rose-500">{{ userIdTip }}</span>
        </label>
        <label class="form-control">
          <span class="label-text mb-1 text-sm font-medium ui-text-strong">昵称</span>
          <input v-model="form.userName" maxlength="20" class="input input-bordered w-full" placeholder="展示给其他用户" />
        </label>
      </div>

      <label class="form-control">
        <span class="label-text mb-1 text-sm font-medium ui-text-strong">邮箱</span>
        <input v-model="form.email" type="email" class="input input-bordered w-full" placeholder="you@example.com" autocomplete="email" @blur="checkEmail" />
        <span v-if="emailTip" class="label-text-alt mt-1 text-rose-500">{{ emailTip }}</span>
      </label>

      <div class="grid gap-3 sm:grid-cols-2">
        <label class="form-control">
          <span class="label-text mb-1 text-sm font-medium ui-text-strong">密码</span>
          <div class="relative">
            <input v-model="form.password" :type="showPassword ? 'text' : 'password'" class="input input-bordered w-full pr-10" :placeholder="PASSWORD_HINT" autocomplete="new-password" />
            <button class="btn btn-ghost btn-xs absolute right-1 top-1/2 -translate-y-1/2" type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
              <component :is="showPassword ? EyeOff : Eye" class="size-4" />
            </button>
          </div>
        </label>
        <label class="form-control">
          <span class="label-text mb-1 text-sm font-medium ui-text-strong">确认密码</span>
          <input v-model="form.password2" :type="showPassword ? 'text' : 'password'" class="input input-bordered w-full" placeholder="再输入一次" autocomplete="new-password" />
        </label>
      </div>

      <div>
        <span class="label-text mb-1 block text-sm font-medium ui-text-strong">验证码</span>
        <Captcha ref="captchaRef" v-model="form.validateCode" placeholder="输入图中字符" />
      </div>

      <div v-if="mailEnabled">
        <span class="label-text mb-1 block text-sm font-medium ui-text-strong">邮箱验证码</span>
        <div class="flex gap-2">
          <input v-model="mailCode" class="input input-bordered min-w-0 flex-1" placeholder="邮件中的 6 位数字" maxlength="6" />
          <button class="btn btn-outline shrink-0" type="button" :disabled="sendingCode || codeCooldown > 0" @click="sendMailCode">
            {{ codeCooldown > 0 ? `${codeCooldown}s 后重发` : sendingCode ? '发送中…' : '发送验证码' }}
          </button>
        </div>
      </div>

      <details class="rounded-lg border ui-border">
        <summary class="cursor-pointer select-none px-3 py-2.5 text-sm ui-text-base">补充资料（选填）</summary>
        <div class="grid gap-3 border-t ui-border p-3 sm:grid-cols-2">
          <label class="form-control">
            <span class="label-text mb-1 text-xs ui-text-base">生日</span>
            <input v-model="form.birthday" type="date" class="input input-bordered input-sm" />
          </label>
          <label class="form-control">
            <span class="label-text mb-1 text-xs ui-text-base">性别</span>
            <select v-model="form.sex" class="select select-bordered select-sm"><option>保密</option><option>男</option><option>女</option></select>
          </label>
          <label class="form-control">
            <span class="label-text mb-1 text-xs ui-text-base">学校</span>
            <input v-model="form.school" class="input input-bordered input-sm" />
          </label>
          <label class="form-control">
            <span class="label-text mb-1 text-xs ui-text-base">专业</span>
            <input v-model="form.major" class="input input-bordered input-sm" />
          </label>
        </div>
      </details>

      <button class="btn btn-primary w-full" :disabled="submitting" type="submit">
        {{ submitting ? '注册中…' : '创建账号' }}
      </button>
    </form>

    <p class="mt-5 text-center text-sm ui-text-base">
      已有账号？
      <RouterLink to="/sign-in" class="font-medium ui-text-brand hover:underline">去登录</RouterLink>
    </p>
  </AuthShell>
</template>
