<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Heart, History, Image, KeyRound, Lock, Pencil, Star, Tags, UserRoundPen, Users } from '@lucide/vue'
import { authApi, settingApi, assetUrl } from '@/api'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/Avatar.vue'
import Captcha from '@/components/Captcha.vue'
import ImageCropper from '@/components/ImageCropper.vue'
import Pagination from '@/components/Pagination.vue'
import { checkPassword, PASSWORD_HINT } from '@/utils/validate'
import { formatDateTime, parseUserAgent } from '@/utils/format'
import { dataUrlToBlob, readAsDataUrl } from '@/utils/image'

const session = useSessionStore()
const toast = useToastStore()
const router = useRouter()

const tab = ref('profile')
const user = ref({ ...session.user })

// ---------------- 隐私设置 ----------------
const privacy = reactive({
  showFollowQuestions: true,
  showFollowTopics: true,
  showFans: true,
  showFollowingUsers: true,
})
const privacySaving = ref(false)

const privacyOptions = [
  { key: 'showFollowQuestions', label: '关注的问题', desc: '允许其他用户查看你关注的问题列表', icon: Star },
  { key: 'showFollowTopics', label: '关注的话题', desc: '允许其他用户查看你关注的话题列表', icon: Tags },
  { key: 'showFans', label: '粉丝', desc: '允许其他用户查看你的粉丝列表', icon: Heart },
  { key: 'showFollowingUsers', label: '关注的人', desc: '允许其他用户查看你关注的人列表', icon: Users },
]

function syncPrivacy(value) {
  privacy.showFollowQuestions = Number(value?.showFollowQuestions ?? 1) === 1
  privacy.showFollowTopics = Number(value?.showFollowTopics ?? 1) === 1
  privacy.showFans = Number(value?.showFans ?? 1) === 1
  privacy.showFollowingUsers = Number(value?.showFollowingUsers ?? 1) === 1
}
syncPrivacy(session.user)

watch(
  () => session.user,
  (value) => {
    user.value = { ...value }
    syncPrivacy(value)
  },
  { deep: true },
)

async function savePrivacy() {
  privacySaving.value = true
  try {
    await settingApi.updatePrivacy({ ...privacy })
    await session.refresh(true)
    toast.success('隐私设置已更新')
  } catch {
    // 保存失败时恢复为服务端当前状态
    syncPrivacy(session.user)
  } finally {
    privacySaving.value = false
  }
}

const saving = ref(false)
const pwd = ref({ oldPassword: '', newPassword: '', confirm: '', captcha: '' })
const captchaRef = ref(null)
const avatarInput = ref(null)
const coverInput = ref(null)
const crop = ref(null)
const uploading = ref(false)

const tabs = [
  { value: 'profile', label: '基本资料', icon: UserRoundPen, hint: '昵称、学校与自我介绍' },
  { value: 'security', label: '安全设置', icon: KeyRound, hint: '修改登录密码' },
  { value: 'privacy', label: '隐私设置', icon: Lock, hint: '控制主页公开范围' },
  { value: 'login-history', label: '登录历史', icon: History, hint: '自查最近登录记录' },
  { value: 'media', label: '头像与封面', icon: Image, hint: '上传前可裁剪' },
]

// ---------------- 登录历史 ----------------
const historyData = ref(null)
const historyLoading = ref(false)
const historyPage = ref(1)

async function loadHistory() {
  historyLoading.value = true
  try {
    historyData.value = await authApi.loginHistory({ page: historyPage.value, size: 10 })
  } finally {
    historyLoading.value = false
  }
}

function changeHistoryPage(page) {
  historyPage.value = Number(page) || 1
  loadHistory()
}

watch(tab, (value) => {
  if (value === 'login-history') loadHistory()
})

const coverUrl = computed(() => assetUrl(user.value?.userTopPhotoUrl))

async function saveProfile() {
  saving.value = true
  try {
    await settingApi.updateData({
      userName: user.value.userName,
      sex: user.value.sex,
      school: user.value.school,
      major: user.value.major,
      simpleSelfIntroduction: user.value.simpleSelfIntroduction,
      selfIntroduction: user.value.selfIntroduction,
      like: user.value.likes,
    })
    await session.refresh(true)
    toast.success('资料已更新')
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  const pwdError = checkPassword(pwd.value.newPassword)
  if (pwdError) return toast.warning(pwdError)
  if (pwd.value.newPassword !== pwd.value.confirm) return toast.warning('两次输入的新密码不一致')
  saving.value = true
  try {
    await settingApi.updatePassword({
      oldPassword: pwd.value.oldPassword,
      newPassword: pwd.value.newPassword,
      CAPTCHA: pwd.value.captcha,
    })
    toast.success('密码修改成功，请重新登录')
    pwd.value = { oldPassword: '', newPassword: '', confirm: '', captcha: '' }
    // 后端已吊销全部会话与记住我 token，刷新登录态并跳转登录页
    await session.refresh(true)
    router.push('/sign-in')
  } catch {
    pwd.value.captcha = ''
    captchaRef.value?.refresh?.()
  } finally {
    saving.value = false
  }
}

async function pickFile(event, kind) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) return toast.warning('请选择图片文件')
  const src = await readAsDataUrl(file)
  crop.value =
    kind === 'avatar'
      ? { src, kind, aspect: 1, outputWidth: 240, outputHeight: 240, title: '裁剪新头像（1:1）' }
      : { src, kind, aspect: 3, outputWidth: 1200, outputHeight: 400, title: '裁剪主页封面（3:1）' }
}

async function onCropConfirm(dataUrl) {
  const kind = crop.value?.kind
  crop.value = null
  uploading.value = true
  try {
    if (kind === 'avatar') {
      const url = await settingApi.updateHeadUrl(dataUrl)
      if (!url) return
      toast.success('头像已更新')
    } else {
      const formData = new FormData()
      formData.append('editormd-image-file', dataUrlToBlob(dataUrl), 'cover.jpeg')
      formData.append('type', 'top')
      const result = await settingApi.uploadImage(formData)
      if (result?.url || result?.success === 1) toast.success('封面已更新')
      else return
    }
    await session.refresh(true)
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <!-- 顶部资料卡 -->
    <div class="surface mb-5 overflow-hidden">
      <div
        class="relative h-32 bg-gradient-to-r from-indigo-500 via-violet-500 to-sky-400 sm:h-40"
        :style="user.userTopPhotoUrl ? { backgroundImage: `url(${coverUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}"
      >
        <div class="absolute inset-0 bg-gradient-to-t from-slate-900/25 to-transparent" />
      </div>
      <div class="flex flex-wrap items-end gap-4 px-4 pb-5 -mt-10 sm:px-6">
        <div class="relative">
          <div class="rounded-full border-4 border-white shadow-md">
            <Avatar :src="user.headUrl" :name="user.userName" :size="80" />
          </div>
          <button
            class="btn btn-circle btn-xs btn-primary absolute -right-1 -bottom-1"
            type="button"
            title="更换头像"
            @click="tab = 'media'"
          >
            <Pencil class="size-3.5" />
          </button>
        </div>
        <div class="min-w-0 flex-1 pt-10">
          <div class="flex flex-wrap items-center gap-2">
            <h1 class="text-xl font-bold ui-text-strong">{{ user.userName }}</h1>
            <span v-if="session.power === 2" class="badge badge-warning badge-sm">VIP</span>
            <span v-if="session.isAdmin" class="badge badge-error badge-sm">管理员</span>
          </div>
          <p class="mt-0.5 text-xs ui-text-muted">@{{ user.userId }} · {{ user.email }}</p>
          <p class="line-clamp-1 mt-1 text-sm ui-text-base">{{ user.simpleSelfIntroduction || '还没有写个性签名' }}</p>
        </div>
        <div class="flex gap-6 pt-10 text-center">
          <div><p class="text-lg font-bold ui-text-brand">{{ user.likeCount || 0 }}</p><p class="text-xs ui-text-muted">获赞</p></div>
          <div><p class="text-lg font-bold ui-text-brand">{{ user.followCount || 0 }}</p><p class="text-xs ui-text-muted">关注</p></div>
          <div><p class="text-lg font-bold ui-text-brand">{{ user.fansCount || 0 }}</p><p class="text-xs ui-text-muted">粉丝</p></div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-5 md:grid-cols-[220px_minmax(0,1fr)]">
      <!-- 左侧导航 -->
      <ul class="surface menu h-fit gap-1 p-3">
        <li v-for="item in tabs" :key="item.value">
          <button
            type="button"
            class="flex items-start gap-3 rounded-lg px-3 py-2.5 text-left"
            :class="tab === item.value ? 'ui-bg-brand ui-text-brand font-medium' : 'ui-text-base'"
            @click="tab = item.value"
          >
            <component :is="item.icon" class="mt-0.5 size-4" />
            <span>
              <span class="block text-sm">{{ item.label }}</span>
              <span class="block text-xs opacity-60">{{ item.hint }}</span>
            </span>
          </button>
        </li>
        <li class="my-2 border-t ui-border" />
        <li>
          <RouterLink :to="`/user/${user.userId}`" class="flex items-center gap-1.5 text-sm ui-text-base"><ArrowLeft class="size-3.5" />查看我的主页</RouterLink>
        </li>
      </ul>

      <!-- 右侧内容 -->
      <div class="surface min-w-0 p-5 sm:p-6">
        <div v-if="tab === 'profile'" class="space-y-5">
          <div>
            <h2 class="text-base font-semibold ui-text-strong">基本资料</h2>
            <p class="mt-1 text-xs ui-text-muted">这些信息会展示在你的个人主页上</p>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <label class="form-control">
              <span class="label-text mb-1.5 font-medium ui-text-strong">昵称</span>
              <input v-model="user.userName" class="input input-bordered w-full focus:border-indigo-400 focus:outline-none" maxlength="20" />
            </label>
            <label class="form-control">
              <span class="label-text mb-1.5 font-medium ui-text-strong">性别</span>
              <select v-model="user.sex" class="select select-bordered w-full focus:border-indigo-400 focus:outline-none">
                <option>保密</option><option>男</option><option>女</option>
              </select>
            </label>
            <label class="form-control">
              <span class="label-text mb-1.5 font-medium ui-text-strong">学校</span>
              <input v-model="user.school" class="input input-bordered w-full focus:border-indigo-400 focus:outline-none" placeholder="就读 / 毕业院校" />
            </label>
            <label class="form-control">
              <span class="label-text mb-1.5 font-medium ui-text-strong">专业</span>
              <input v-model="user.major" class="input input-bordered w-full focus:border-indigo-400 focus:outline-none" placeholder="所学专业" />
            </label>
          </div>

          <label class="form-control">
            <span class="label-text mb-1.5 flex justify-between font-medium ui-text-strong">
              个性签名 <span class="text-xs font-normal ui-text-muted">{{ (user.simpleSelfIntroduction || '').length }} / 50</span>
            </span>
            <input v-model="user.simpleSelfIntroduction" maxlength="50" class="input input-bordered w-full focus:border-indigo-400 focus:outline-none" placeholder="一句话介绍自己" />
          </label>

          <label class="form-control">
            <span class="label-text mb-1.5 flex justify-between font-medium ui-text-strong">
              自我介绍 <span class="text-xs font-normal ui-text-muted">{{ (user.selfIntroduction || '').length }} / 500</span>
            </span>
            <textarea v-model="user.selfIntroduction" maxlength="500" rows="4" class="textarea textarea-bordered w-full leading-relaxed focus:border-indigo-400 focus:outline-none" placeholder="多写一点，让大家更了解你…" />
          </label>

          <label class="form-control">
            <span class="label-text mb-1.5 font-medium ui-text-strong">爱好</span>
            <input v-model="user.likes" class="input input-bordered w-full focus:border-indigo-400 focus:outline-none" placeholder="用逗号分隔，例如：摄影, 跑步, 开源" />
          </label>

          <div class="flex items-center gap-3 border-t ui-border pt-4">
            <button class="btn btn-primary btn-sm px-6" :disabled="saving" type="button" @click="saveProfile">
              {{ saving ? '保存中…' : '保存修改' }}
            </button>
            <span class="text-xs ui-text-muted">修改后会立即同步到个人主页</span>
          </div>
        </div>

        <div v-else-if="tab === 'security'" class="max-w-md space-y-5">
          <div>
            <h2 class="text-base font-semibold ui-text-strong">修改密码</h2>
            <p class="mt-1 text-xs ui-text-muted">{{ PASSWORD_HINT }}</p>
          </div>
          <label class="form-control"><span class="label-text mb-1.5 font-medium ui-text-strong">原密码</span><input v-model="pwd.oldPassword" type="password" class="input input-bordered w-full" autocomplete="current-password" /></label>
          <label class="form-control"><span class="label-text mb-1.5 font-medium ui-text-strong">新密码</span><input v-model="pwd.newPassword" type="password" class="input input-bordered w-full" autocomplete="new-password" /></label>
          <label class="form-control"><span class="label-text mb-1.5 font-medium ui-text-strong">确认新密码</span><input v-model="pwd.confirm" type="password" class="input input-bordered w-full" autocomplete="new-password" /></label>
          <div>
            <span class="label-text mb-1.5 block font-medium ui-text-strong">验证码</span>
            <Captcha ref="captchaRef" v-model="pwd.captcha" />
          </div>
          <button class="btn btn-primary btn-sm px-6" :disabled="saving" type="button" @click="savePassword">
            {{ saving ? '提交中…' : '确认修改' }}
          </button>
        </div>

        <div v-else-if="tab === 'privacy'" class="space-y-5">
          <div>
            <h2 class="text-base font-semibold ui-text-strong">隐私设置</h2>
            <p class="mt-1 text-xs ui-text-muted">
              控制其他用户可以在你的个人主页查看哪些列表；你自己始终可以查看全部内容。
            </p>
          </div>

          <div class="space-y-3">
            <label
              v-for="option in privacyOptions"
              :key="option.key"
              class="flex cursor-pointer items-center gap-3 rounded-xl border ui-border p-4 transition hover:border-indigo-300"
            >
              <span class="flex size-9 shrink-0 items-center justify-center rounded-xl ui-bg-soft ui-text-brand">
                <component :is="option.icon" class="size-4" />
              </span>
              <span class="min-w-0 flex-1">
                <span class="block text-sm font-medium ui-text-strong">{{ option.label }}</span>
                <span class="mt-0.5 block text-xs leading-relaxed ui-text-muted">{{ option.desc }}</span>
              </span>
              <input v-model="privacy[option.key]" type="checkbox" class="toggle toggle-primary shrink-0" />
            </label>
          </div>

          <p class="flex items-start gap-1.5 rounded-lg ui-bg-soft px-3 py-2 text-xs leading-relaxed ui-text-muted">
            <Lock class="mt-0.5 size-3.5 shrink-0" />
            关闭后，其他用户访问对应页面时会看到「该用户未公开此内容」，不会影响你自己查看和操作。
          </p>

          <div class="border-t ui-border pt-4">
            <button class="btn btn-primary btn-sm px-6" :disabled="privacySaving" type="button" @click="savePrivacy">
              {{ privacySaving ? '保存中…' : '保存隐私设置' }}
            </button>
          </div>
        </div>

        <div v-else-if="tab === 'login-history'" class="space-y-5">
          <div>
            <h2 class="text-base font-semibold ui-text-strong">登录历史</h2>
            <p class="mt-1 text-xs ui-text-muted">
              仅记录成功登录，保留最近 180 天。如发现陌生 IP 或设备，请立即修改密码。
            </p>
          </div>

          <div v-if="historyLoading && !historyData" class="empty-state">正在加载登录历史…</div>
          <div v-else class="overflow-x-auto">
            <table class="table table-sm">
              <thead>
                <tr><th>登录时间</th><th>IP</th><th>入口</th><th>设备 / UA</th></tr>
              </thead>
              <tbody>
                <tr v-for="item in historyData?.data || []" :key="item.id">
                  <td class="whitespace-nowrap text-xs">{{ formatDateTime(item.loginTime) }}</td>
                  <td class="font-mono text-xs">{{ item.ip || '—' }}</td>
                  <td>
                    <span class="badge badge-sm" :class="item.loginType === 2 ? 'badge-warning' : 'badge-ghost'">
                      {{ item.loginType === 2 ? '后台' : '前台' }}
                    </span>
                  </td>
                  <td class="max-w-xs text-xs" :title="item.userAgent || ''">
                    <div>{{ parseUserAgent(item.userAgent) }}</div>
                    <div class="truncate text-[11px] ui-text-muted">{{ item.userAgent || '—' }}</div>
                  </td>
                </tr>
                <tr v-if="!historyData?.data?.length">
                  <td colspan="4" class="text-center ui-text-muted">暂无登录记录</td>
                </tr>
              </tbody>
            </table>
          </div>
          <Pagination :pagination="historyData" @change="changeHistoryPage" />
        </div>

        <div v-else class="space-y-5">
          <div>
            <h2 class="text-base font-semibold ui-text-strong">头像与主页封面</h2>
            <p class="mt-1 text-xs ui-text-muted">选择图片后先裁剪再上传，避免头像变形或封面被拉伸</p>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div class="rounded-xl border ui-border p-4">
              <div class="flex items-center gap-3">
                <Avatar :src="user.headUrl" :name="user.userName" :size="64" />
                <div>
                  <h3 class="section-title">头像</h3>
                  <p class="text-xs ui-text-muted">正方形 1:1，建议 240×240</p>
                </div>
              </div>
              <input ref="avatarInput" type="file" accept="image/*" class="hidden" @change="pickFile($event, 'avatar')" />
              <button class="btn btn-outline btn-sm mt-4 w-full" type="button" :disabled="uploading" @click="avatarInput?.click()">选择并裁剪头像</button>
            </div>

            <div class="rounded-xl border ui-border p-4">
              <img :src="coverUrl" alt="主页封面" class="h-16 w-full rounded-lg ui-bg-soft object-cover" />
              <div class="mt-3">
                <h3 class="section-title">主页封面</h3>
                <p class="text-xs ui-text-muted">宽幅 3:1，建议 1200×400</p>
              </div>
              <input ref="coverInput" type="file" accept="image/*" class="hidden" @change="pickFile($event, 'cover')" />
              <button class="btn btn-outline btn-sm mt-4 w-full" type="button" :disabled="uploading" @click="coverInput?.click()">选择并裁剪封面</button>
            </div>
          </div>

          <div v-if="uploading" class="alert alert-info py-2 text-sm">正在上传裁剪后的图片…</div>
        </div>
      </div>
    </div>

    <ImageCropper
      v-if="crop"
      :src="crop.src"
      :aspect="crop.aspect"
      :output-width="crop.outputWidth"
      :output-height="crop.outputHeight"
      :title="crop.title"
      @confirm="onCropConfirm"
      @cancel="crop = null"
    />
  </div>
</template>
