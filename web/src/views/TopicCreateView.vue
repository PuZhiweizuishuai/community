<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Check,
  ChevronLeft,
  Crown,
  FileText,
  Globe2,
  ImagePlus,
  Loader,
  Lock,
  MessageSquare,
  Minus,
  Send,
  Settings2,
  ShieldCheck,
  Sparkles,
  UserPlus,
  Users,
} from '@lucide/vue'
import { assetUrl, discoverApi, settingApi } from '@/api'
import { useMetaStore } from '@/stores/meta'
import { useToastStore } from '@/stores/toast'
import Captcha from '@/components/Captcha.vue'
import ImageCropper from '@/components/ImageCropper.vue'
import { dataUrlToBlob, ensureDecodable, readAsDataUrl, validateImageFile } from '@/utils/image'

const router = useRouter()
const meta = useMetaStore()
const toast = useToastStore()

const DEFAULT_ICON = '/image/topicicon/default.svg'

const form = ref({
  title: '',
  type: meta.topicTypes[0]?.value || 1,
  simpleDesc: '',
  image: DEFAULT_ICON,
  visibility: 0,
  captcha: '',
})
const saving = ref(false)
const captchaRef = ref(null)

// 图标上传：先选本地图片 → 裁剪 1:1 → 才真正上传
const iconInput = ref(null)
const iconCrop = ref(null)
const iconUploading = ref(false)
const hasCustomIcon = computed(() => !!form.value.image && form.value.image !== DEFAULT_ICON)

const visibilityOptions = [
  { value: 0, label: '公开', desc: '所有用户都可以浏览话题内容并发帖', icon: Globe2 },
  { value: 1, label: '私有 · 可申请', desc: '话题信息可见，内容仅成员可见，用户可申请加入', icon: Users },
  { value: 2, label: '私有 · 仅邀请', desc: '话题与内容均不可见，只能由吧主邀请加入', icon: Lock },
]

const requiredChecks = computed(() => [
  { label: '话题名称', ok: form.value.title.trim().length > 0 },
  { label: '所属分类', ok: !!form.value.type },
  { label: '验证码', ok: form.value.captcha.trim().length > 0 },
])
const readyCount = computed(() => requiredChecks.value.filter((item) => item.ok).length)
const progress = computed(() => Math.round((readyCount.value / requiredChecks.value.length) * 100))
const canSubmit = computed(() => readyCount.value === requiredChecks.value.length)
const categoryLabel = computed(
  () => meta.topicTypes.find((item) => Number(item.value) === Number(form.value.type))?.label || '未选择分类',
)
const currentVisibility = computed(
  () => visibilityOptions.find((item) => item.value === form.value.visibility) || visibilityOptions[0],
)
const previewTitle = computed(() => form.value.title.trim() || '你的话题名称')
const previewDesc = computed(
  () => form.value.simpleDesc.trim() || '用一句话介绍这个话题的讨论范围，让同好一眼了解这里聊什么。',
)

async function pickIcon(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  const invalid = validateImageFile(file)
  if (invalid) return toast.warning(invalid)
  const src = await readAsDataUrl(file)
  try {
    await ensureDecodable(src)
  } catch {
    return toast.warning('这张图片无法解析，请换一张 JPG / PNG 图片')
  }
  iconCrop.value = { src, title: '裁剪话题图标（1:1）' }
}

async function confirmIcon(dataUrl) {
  iconCrop.value = null
  iconUploading.value = true
  try {
    const formData = new FormData()
    formData.append('editormd-image-file', dataUrlToBlob(dataUrl), 'topic-icon.jpeg')
    formData.append('type', 'topic')
    const result = await settingApi.uploadImage(formData)
    if (!result?.url) return toast.error('图标上传失败，请重试')
    form.value.image = result.url
    toast.success('图标已上传')
  } catch {
    toast.error('图标上传失败，请重试')
  } finally {
    iconUploading.value = false
  }
}

function resetIcon() {
  form.value.image = DEFAULT_ICON
}

async function submit() {
  if (!form.value.title.trim()) return toast.warning('请填写话题名称')
  if (!form.value.type) return toast.warning('请选择话题分类')
  if (!form.value.captcha.trim()) return toast.warning('请输入验证码')
  saving.value = true
  try {
    const result = await discoverApi.createTopic({
      title: form.value.title.trim(),
      type: Number(form.value.type),
      simpleDesc: form.value.simpleDesc.trim(),
      image: form.value.image.trim(),
      visibility: Number(form.value.visibility),
      captcha: form.value.captcha,
    })
    toast.success('话题已提交审核，通过后你将自动成为吧主')
    router.push(`/topics/${result.topicId}`)
  } catch {
    form.value.captcha = ''
    captchaRef.value?.refresh?.()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page-container max-w-6xl">
    <!-- 页面头部 -->
    <section class="surface relative mb-5 overflow-hidden p-5 sm:p-6">
      <div class="pointer-events-none absolute -right-20 -top-28 size-64 rounded-full bg-gradient-to-br from-indigo-500/20 via-violet-500/10 to-sky-400/10 blur-3xl" />
      <div class="relative flex flex-wrap items-start gap-4">
        <div class="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 to-violet-600 text-white shadow-lg shadow-indigo-500/20">
          <Sparkles class="size-6" />
        </div>
        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-2">
            <h1 class="text-xl font-bold ui-text-strong sm:text-2xl">发起一个新话题</h1>
            <span class="badge badge-sm border-0 ui-bg-brand ui-text-brand">需管理员审核</span>
          </div>
          <p class="mt-1 max-w-2xl text-sm leading-relaxed ui-text-base">
            创建一个属于同好的讨论空间。审核通过后你将自动成为吧主，可邀请成员、审核加入申请并调整话题可见性。
          </p>
        </div>
        <RouterLink to="/topics" class="btn btn-ghost btn-sm shrink-0">
          <ChevronLeft class="size-4" />返回话题
        </RouterLink>
      </div>
    </section>

    <div class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
      <!-- 主表单 -->
      <div class="min-w-0 space-y-4">
        <section class="surface p-5 sm:p-6">
          <div class="mb-5 flex items-start gap-3">
            <span class="flex size-9 shrink-0 items-center justify-center rounded-xl ui-bg-brand ui-text-brand">
              <MessageSquare class="size-4" />
            </span>
            <div>
              <h2 class="section-title">基本信息</h2>
              <p class="mt-1 text-xs ui-text-muted">名称与图标会展示在话题列表和话题详情页</p>
            </div>
          </div>

          <label class="block">
            <span class="mb-2 flex items-baseline justify-between">
              <span class="text-xs font-medium ui-text-base">话题名称 <span class="text-rose-500">*</span></span>
              <span class="text-xs" :class="form.title.length > 40 ? 'text-rose-500' : 'ui-text-muted'">
                {{ form.title.length }} / 50
              </span>
            </span>
            <input
              v-model="form.title"
              maxlength="50"
              class="w-full border-0 border-b ui-border bg-transparent pb-3 text-xl font-semibold ui-text-strong placeholder:font-normal placeholder:text-slate-300 focus:border-indigo-400 focus:outline-none"
              placeholder="例如：Rust 学习交流"
            />
            <span class="mt-2 block text-xs ui-text-muted">名称全站唯一，创建后暂不支持修改。</span>
          </label>

          <div class="mt-6">
            <span class="mb-2 block text-xs font-medium ui-text-base">所属分类 <span class="text-rose-500">*</span></span>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="item in meta.topicTypes"
                :key="item.value"
                type="button"
                class="rounded-full border px-3.5 py-1.5 text-sm transition"
                :class="Number(form.type) === Number(item.value)
                  ? 'border-indigo-500 ui-bg-brand font-medium ui-text-brand'
                  : 'ui-border ui-text-base hover:border-indigo-300 hover:text-indigo-600'"
                @click="form.type = Number(item.value)"
              >
                {{ item.label }}
              </button>
              <span v-if="!meta.topicTypes.length" class="text-xs ui-text-muted">暂无可选分类，请联系管理员配置</span>
            </div>
          </div>

          <div class="mt-6">
            <span class="mb-2 block text-xs font-medium ui-text-base">话题图标</span>
            <div class="rounded-xl border border-dashed ui-border ui-bg-soft p-4">
              <div class="flex items-center gap-4">
                <div class="relative shrink-0">
                  <img
                    :src="assetUrl(form.image)"
                    alt="话题图标"
                    class="h-16 w-16 rounded-xl ui-bg object-cover shadow-sm"
                  />
                  <span
                    v-if="iconUploading"
                    class="absolute inset-0 flex items-center justify-center rounded-xl bg-slate-900/55"
                  >
                    <Loader class="size-5 animate-spin text-white" />
                  </span>
                </div>
                <div class="min-w-0 flex-1">
                  <p class="text-sm font-medium ui-text-strong">
                    {{ hasCustomIcon ? '已使用自定义图标' : '上传话题图标' }}
                  </p>
                  <p class="mt-0.5 text-xs leading-relaxed ui-text-muted">
                    选择图片后可拖动、缩放裁剪成 1:1 正方形，确认后才会开始上传
                  </p>
                </div>
              </div>
              <div class="mt-3 flex flex-wrap items-center gap-2">
                <button
                  class="btn btn-outline btn-xs"
                  type="button"
                  :disabled="iconUploading"
                  @click="iconInput?.click()"
                >
                  <Loader v-if="iconUploading" class="size-3.5 animate-spin" />
                  <ImagePlus v-else class="size-3.5" />
                  {{ iconUploading ? '上传中…' : hasCustomIcon ? '重新选择并裁剪' : '选择图片并裁剪' }}
                </button>
                <button
                  v-if="hasCustomIcon"
                  class="btn btn-ghost btn-xs font-normal ui-text-muted"
                  type="button"
                  :disabled="iconUploading"
                  @click="resetIcon"
                >
                  使用默认图标
                </button>
                <span class="text-[11px] leading-relaxed ui-text-muted">
                  JPG / PNG / WebP，≤ 10MB，输出 400 × 400
                </span>
              </div>
            </div>
            <input ref="iconInput" type="file" accept="image/*" class="hidden" @change="pickIcon" />
          </div>
        </section>

        <section class="surface p-5 sm:p-6">
          <div class="mb-4 flex items-start justify-between gap-3">
            <div class="flex items-start gap-3">
              <span class="flex size-9 shrink-0 items-center justify-center rounded-xl ui-bg-brand ui-text-brand">
                <FileText class="size-4" />
              </span>
              <div>
                <h2 class="section-title">话题简介</h2>
                <p class="mt-1 text-xs ui-text-muted">清晰的简介能帮助用户快速判断是否加入讨论</p>
              </div>
            </div>
            <span class="shrink-0 text-xs ui-text-muted">{{ form.simpleDesc.length }} / 500</span>
          </div>
          <textarea
            v-model="form.simpleDesc"
            maxlength="500"
            rows="4"
            class="textarea textarea-bordered min-h-28 w-full leading-relaxed focus:border-indigo-400 focus:outline-none"
            placeholder="例如：这里讨论 Rust 语言学习、项目实践与生态工具，欢迎分享你的踩坑经验。"
          />
        </section>

        <section class="surface p-5 sm:p-6">
          <div class="mb-4 flex items-start gap-3">
            <span class="flex size-9 shrink-0 items-center justify-center rounded-xl ui-bg-brand ui-text-brand">
              <ShieldCheck class="size-4" />
            </span>
            <div>
              <h2 class="section-title">可见性</h2>
              <p class="mt-1 text-xs ui-text-muted">决定谁可以浏览话题信息与话题内的帖子</p>
            </div>
          </div>

          <div class="grid gap-3 md:grid-cols-3">
            <button
              v-for="option in visibilityOptions"
              :key="option.value"
              type="button"
              class="relative rounded-2xl border p-4 text-left transition"
              :class="form.visibility === option.value
                ? 'border-indigo-500 ui-bg-brand shadow-sm'
                : 'ui-border hover:border-indigo-300 hover:shadow-sm'"
              @click="form.visibility = option.value"
            >
              <span
                class="absolute right-3 top-3 flex size-5 items-center justify-center rounded-full"
                :class="form.visibility === option.value
                  ? 'bg-indigo-600 text-white'
                  : 'ui-bg-soft ui-text-muted'"
              >
                <Check v-if="form.visibility === option.value" class="size-3.5" />
              </span>
              <span
                class="flex size-9 items-center justify-center rounded-xl"
                :class="form.visibility === option.value ? 'ui-bg ui-text-brand' : 'ui-bg-soft ui-text-muted'"
              >
                <component :is="option.icon" class="size-4" />
              </span>
              <span
                class="mt-3 block text-sm font-medium"
                :class="form.visibility === option.value ? 'ui-text-brand' : 'ui-text-strong'"
              >
                {{ option.label }}
              </span>
              <span class="mt-1 block text-xs leading-relaxed ui-text-muted">{{ option.desc }}</span>
            </button>
          </div>

          <p class="mt-4 flex items-start gap-1.5 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs leading-relaxed text-amber-700">
            <ShieldCheck class="mt-0.5 size-3.5 shrink-0" />
            所有新建话题都需要管理员审核。审核通过后公开展示；私有话题仅成员或被邀请用户可查看内容。
          </p>
        </section>
      </div>

      <!-- 侧栏：预览 + 权益 + 提交 -->
      <aside class="space-y-4">
        <section class="surface overflow-hidden">
          <div class="relative h-20 bg-gradient-to-r from-indigo-500 via-violet-500 to-sky-400">
            <div class="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(255,255,255,0.35),transparent_45%)]" />
            <span class="absolute right-3 top-3 rounded-full bg-black/15 px-2 py-0.5 text-[11px] font-medium text-white backdrop-blur">
              实时预览
            </span>
          </div>
          <div class="px-4 pb-4">
            <div class="-mt-8 flex items-end gap-3">
              <img
                :src="assetUrl(form.image)"
                alt="话题图标预览"
                class="h-16 w-16 rounded-2xl border-4 border-[var(--surface-bg)] object-cover shadow-md"
              />
              <span
                class="mb-1 badge badge-sm"
                :class="form.visibility === 0 ? 'badge-ghost' : 'badge-warning'"
              >
                {{ currentVisibility.label }}
              </span>
            </div>
            <h3 class="mt-3 truncate text-base font-semibold ui-text-strong">{{ previewTitle }}</h3>
            <p class="mt-1 line-clamp-2 text-xs leading-relaxed ui-text-muted">{{ previewDesc }}</p>
            <div class="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 border-t ui-border pt-3 text-xs ui-text-muted">
              <span>{{ categoryLabel }}</span>
              <span>0 人关注</span>
              <span>0 条讨论</span>
            </div>
          </div>
        </section>

        <section class="surface p-4">
          <h2 class="section-title mb-3"><Crown class="size-4 ui-text-brand" />审核通过后你将获得</h2>
          <ul class="space-y-3">
            <li class="flex gap-2.5">
              <span class="flex size-7 shrink-0 items-center justify-center rounded-lg ui-bg-soft ui-text-brand">
                <UserPlus class="size-3.5" />
              </span>
              <div>
                <p class="text-sm font-medium ui-text-strong">邀请成员</p>
                <p class="mt-0.5 text-xs leading-relaxed ui-text-muted">邀请同好加入私有话题，组成讨论圈。</p>
              </div>
            </li>
            <li class="flex gap-2.5">
              <span class="flex size-7 shrink-0 items-center justify-center rounded-lg ui-bg-soft ui-text-brand">
                <ShieldCheck class="size-3.5" />
              </span>
              <div>
                <p class="text-sm font-medium ui-text-strong">审核加入申请</p>
                <p class="mt-0.5 text-xs leading-relaxed ui-text-muted">与吧务团队一起决定谁可以加入话题。</p>
              </div>
            </li>
            <li class="flex gap-2.5">
              <span class="flex size-7 shrink-0 items-center justify-center rounded-lg ui-bg-soft ui-text-brand">
                <Settings2 class="size-3.5" />
              </span>
              <div>
                <p class="text-sm font-medium ui-text-strong">调整话题设置</p>
                <p class="mt-0.5 text-xs leading-relaxed ui-text-muted">随时修改简介、图标与话题可见性。</p>
              </div>
            </li>
          </ul>
        </section>

        <section class="surface p-4">
          <div class="mb-3 flex items-center justify-between gap-2">
            <h2 class="section-title">提交审核</h2>
            <span class="text-xs ui-text-muted">{{ readyCount }} / {{ requiredChecks.length }}</span>
          </div>
          <div class="h-1.5 overflow-hidden rounded-full ui-bg-soft">
            <div
              class="h-full rounded-full bg-gradient-to-r from-indigo-500 to-violet-500 transition-all duration-300"
              :style="{ width: progress + '%' }"
            />
          </div>
          <ul class="mt-3 space-y-1.5">
            <li v-for="item in requiredChecks" :key="item.label" class="flex items-center gap-2 text-xs">
              <span
                class="flex size-4 items-center justify-center rounded-full"
                :class="item.ok ? 'bg-emerald-100 text-emerald-600' : 'ui-bg-soft ui-text-muted'"
              >
                <component :is="item.ok ? Check : Minus" class="size-3" />
              </span>
              <span :class="item.ok ? 'ui-text-base' : 'ui-text-strong'">{{ item.label }}</span>
            </li>
          </ul>

          <div class="mt-4 border-t ui-border pt-4">
            <span class="mb-2 block text-xs font-medium ui-text-base">验证码 <span class="text-rose-500">*</span></span>
            <Captcha ref="captchaRef" v-model="form.captcha" />
          </div>

          <button
            class="btn btn-primary mt-4 w-full"
            :disabled="saving || !canSubmit"
            type="button"
            @click="submit"
          >
            <Send class="size-4" />{{ saving ? '提交中…' : '提交审核' }}
          </button>
          <p class="mt-2 text-center text-[11px] leading-relaxed ui-text-muted">
            提交即表示你已阅读并同意社区规范
          </p>
        </section>
      </aside>
    </div>

    <ImageCropper
      v-if="iconCrop"
      :src="iconCrop.src"
      :aspect="1"
      :output-width="400"
      :output-height="400"
      :title="iconCrop.title"
      @confirm="confirmIcon"
      @cancel="iconCrop = null"
    />
  </div>
</template>
