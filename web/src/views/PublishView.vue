<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Check, ChevronDown, ChevronUp, Eye, FileText, Lock, MessageCircle, Minus, Plus, Send, ShieldCheck, Trash2, TriangleAlert, X } from '@lucide/vue'
import { questionApi } from '@/api'
import { useSessionStore } from '@/stores/session'
import { useMetaStore } from '@/stores/meta'
import { useToastStore } from '@/stores/toast'
import VditorEditor from '@/components/VditorEditor.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import Captcha from '@/components/Captcha.vue'
import AdCarousel from '@/components/AdCarousel.vue'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const meta = useMetaStore()
const toast = useToastStore()
meta.loadTagCategories()
meta.loadAds('publish')

const SEGMENT_TYPES = [
  { value: 0, label: '普通文本', hint: '所有人可直接阅读', icon: FileText },
  { value: 1, label: '密码可见', hint: '读者输入阅读密码后可见', icon: Lock },
  { value: 2, label: '回复可见', hint: '读者回复该问题后可见', icon: MessageCircle },
]
let segmentSeq = 0
const newSegment = () => ({ key: ++segmentSeq, type: 0, content: '', password: '' })

const editId = computed(() => route.params.id || null)
const title = ref('')
const classification = ref('')
const segments = ref([newSegment()])
const tags = ref([])
const tagInput = ref('')
const captcha = ref('')
const createTime = ref(0)
const questionId = ref(-1)
const submitting = ref(false)
const captchaRef = ref(null)
const showTagPanel = ref(false)
const previewOpen = ref(false)

const editorRefs = {}
function setEditorRef(key, el) {
  if (el) editorRefs[key] = el
  else delete editorRefs[key]
}
function contentOf(segment) {
  return editorRefs[segment.key]?.getValue?.() ?? segment.content ?? ''
}
function segmentTypeLabel(type) {
  return SEGMENT_TYPES.find((t) => t.value === type)?.label || '普通文本'
}

const editClasses = computed(() => meta.classifications.filter((c) => c.value !== 0))
const ads = computed(() => meta.adsFor('publish'))

const checks = computed(() => {
  const list = [
    { label: '标题（≤ 50 字）', ok: title.value.trim().length > 0 && title.value.length <= 50 },
    { label: '问题分类', ok: !!classification.value },
    { label: '正文内容', ok: segments.value.every((s) => (s.content || '').trim().length > 0) },
  ]
  if (segments.value.some((s) => s.type === 1)) {
    list.push({ label: '阅读密码（密码可见段落）', ok: segments.value.filter((s) => s.type === 1).every((s) => (s.password || '').trim().length > 0) })
  }
  list.push(
    { label: '标签（1 ~ 6 个）', ok: tags.value.length > 0 },
    { label: '验证码', ok: captcha.value.trim().length > 0 },
  )
  return list
})
const readyCount = computed(() => checks.value.filter((c) => c.ok).length)

async function loadEdit() {
  if (!editId.value) {
    title.value = ''
    classification.value = ''
    segments.value = [newSegment()]
    tags.value = []
    questionId.value = -1
    createTime.value = 0
    return
  }
  const data = await questionApi.edit(editId.value)
  const question = data.question
  title.value = question.title || ''
  classification.value = question.classification || ''
  tags.value = String(question.tag || '').split(/[,，]/).filter(Boolean)
  createTime.value = question.createTime || 0
  questionId.value = question.questionId
  const list = (data.contents || []).map((c) => ({ key: ++segmentSeq, type: c.type, content: c.content || '', password: c.password || '' }))
  segments.value = list.length ? list : [newSegment()]
  await nextTick()
  segments.value.forEach((s) => editorRefs[s.key]?.setValue?.(s.content))
}
watch(editId, loadEdit, { immediate: true })

function addTag(tag) {
  const clean = String(tag).trim()
  if (!clean || tags.value.includes(clean)) return
  if (tags.value.length >= 6) return toast.warning('最多只能添加 6 个标签')
  tags.value.push(clean)
  tagInput.value = ''
}
function removeTag(tag) {
  tags.value = tags.value.filter((t) => t !== tag)
}

function addSegment() {
  if (segments.value.length >= 3) return toast.warning('最多只能添加 3 个内容段落')
  segments.value.push(newSegment())
}
function removeSegment(index) {
  if (segments.value.length <= 1) return toast.warning('至少保留一个内容段落')
  segments.value.splice(index, 1)
}

async function submit() {
  if (!title.value.trim()) return toast.warning('请填写标题')
  if (title.value.length > 50) return toast.warning('标题不能超过 50 个字')
  if (!classification.value) return toast.warning('请选择问题分类')
  const contents = []
  for (let i = 0; i < segments.value.length; i++) {
    const segment = segments.value[i]
    const content = contentOf(segment)
    if (!content.trim()) return toast.warning(`第 ${i + 1} 段正文不能为空`)
    if (segment.type === 1) {
      const password = segment.password.trim()
      if (!password) return toast.warning(`请为第 ${i + 1} 段设置阅读密码`)
      if (password.length > 64) return toast.warning(`第 ${i + 1} 段阅读密码不能超过 64 个字符`)
      contents.push({ type: segment.type, content, password })
    } else {
      contents.push({ type: segment.type, content })
    }
  }
  if (!tags.value.length) return toast.warning('请至少添加一个标签')
  if (!captcha.value.trim()) return toast.warning('请输入验证码')

  submitting.value = true
  try {
    const result = await questionApi.save({
      title: title.value.trim(),
      classification: classification.value,
      contents,
      tag: tags.value.join(','),
      questionId: questionId.value,
      createTime: createTime.value,
      captcha: captcha.value,
    })
    // 审核开启时后端返回 reviewStatus=0，提示用户内容已进入审核区
    if (Number(result?.reviewStatus) === 0) {
      toast.success(editId.value ? '修改已提交审核，通过后将重新公开展示' : '发布成功，已提交审核')
    } else {
      toast.success(editId.value ? '修改成功' : '发布成功')
    }
    router.push(`/question/${result.questionId || questionId.value}`)
  } catch {
    captcha.value = ''
    captchaRef.value?.refresh?.()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <!-- 页面标题栏 -->
    <div class="mb-5 flex flex-wrap items-end justify-between gap-3">
      <div>
        <div class="flex items-center gap-1.5 text-xs ui-text-muted">
          <RouterLink to="/" class="hover:text-indigo-600">首页</RouterLink>
          <span>/</span>
          <span>{{ editId ? '编辑讨论' : '发起讨论' }}</span>
        </div>
        <h1 class="mt-1 text-2xl font-bold ui-text-strong">{{ editId ? '编辑你的讨论' : '分享你的问题与想法' }}</h1>
        <p class="mt-1 text-sm ui-text-base">清晰的标题、完整的背景与合适的标签，能让你更快得到高质量的回复。</p>
      </div>
      <button class="btn btn-ghost btn-sm" type="button" @click="previewOpen = true"><Eye class="size-4" />预览正文</button>
    </div>

    <div v-if="!session.can('file:upload')" class="alert alert-warning mb-4 py-2 text-sm">
      <TriangleAlert class="size-4 shrink-0" />
      <span>当前账号没有文件上传权限，正文中的图片与附件上传会失败；纯文本发布不受影响。</span>
    </div>

    <div class="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_320px]">
      <!-- 主编辑区 -->
      <div class="min-w-0 space-y-4">
        <section class="surface p-5">
          <label class="block">
            <span class="mb-2 flex items-baseline justify-between">
              <span class="text-xs font-medium ui-text-base">标题 <span class="text-rose-500">*</span></span>
              <span class="text-xs" :class="title.length > 45 ? 'text-rose-500' : 'ui-text-muted'">{{ title.length }} / 50</span>
            </span>
            <input
              v-model="title"
              maxlength="50"
              class="w-full border-0 border-b ui-border bg-transparent pb-3 text-xl font-semibold ui-text-strong placeholder:font-normal placeholder:text-slate-300 focus:border-indigo-400 focus:outline-none"
              placeholder="用一句话概括你的问题"
            />
          </label>

          <div class="mt-5">
            <span class="mb-2 block text-xs font-medium ui-text-base">问题分类 <span class="text-rose-500">*</span></span>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="item in editClasses"
                :key="item.value"
                type="button"
                class="rounded-full border px-3.5 py-1.5 text-sm transition"
                :class="classification === item.label
                  ? 'border-indigo-500 ui-bg-brand font-medium ui-text-brand'
                  : 'ui-border ui-text-base hover:border-indigo-300 hover:text-indigo-600'"
                @click="classification = item.label"
              >
                {{ item.label }}
              </button>
            </div>
          </div>
        </section>

        <section class="surface overflow-hidden">
          <div class="flex items-center justify-between border-b ui-border px-5 py-3">
            <div>
              <h2 class="section-title">正文 <span class="text-rose-500">*</span></h2>
              <p class="mt-0.5 text-xs ui-text-muted">支持 Markdown、代码块、公式、流程图与图片拖拽上传</p>
            </div>
            <span class="hidden text-xs ui-text-muted sm:block">{{ segments.length }} / 3 个段落</span>
          </div>
          <div class="space-y-3 p-2 sm:p-3">
            <div v-for="(segment, index) in segments" :key="segment.key" class="rounded-xl border ui-border">
              <div class="flex flex-wrap items-center gap-2 border-b ui-border px-3 py-2">
                <span class="text-xs font-medium ui-text-base">段落 {{ index + 1 }}</span>
                <div class="flex flex-wrap gap-1">
                  <button
                    v-for="t in SEGMENT_TYPES"
                    :key="t.value"
                    type="button"
                    class="rounded-full border px-2.5 py-1 text-xs transition"
                    :class="segment.type === t.value
                      ? 'border-indigo-500 ui-bg-brand font-medium ui-text-brand'
                      : 'ui-border ui-text-base hover:border-indigo-300 hover:text-indigo-600'"
                    :title="t.hint"
                    @click="segment.type = t.value"
                  >
                    <component :is="t.icon" class="mr-1 inline size-3" />{{ t.label }}
                  </button>
                </div>
                <button v-if="segments.length > 1" class="btn btn-ghost btn-xs ml-auto text-rose-500" type="button" @click="removeSegment(index)">
                  <Trash2 class="size-3.5" />删除
                </button>
              </div>
              <div v-if="segment.type === 1" class="flex flex-wrap items-center gap-2 border-b ui-border px-3 py-2">
                <Lock class="size-4 shrink-0 ui-text-muted" />
                <input
                  v-model="segment.password"
                  type="text"
                  maxlength="64"
                  class="input input-bordered input-sm w-full max-w-sm focus:outline-none"
                  placeholder="设置阅读密码，读者输入相同密码即可阅读本段"
                />
              </div>
              <div v-else-if="segment.type === 2" class="border-b ui-border px-3 py-2 text-xs ui-text-muted">
                读者回复该问题后才能查看本段内容，你作为作者始终可见
              </div>
              <div class="p-1">
                <VditorEditor :ref="(el) => setEditorRef(segment.key, el)" v-model="segment.content" :height="360" />
              </div>
            </div>
            <button class="btn btn-outline btn-sm w-full" type="button" :disabled="segments.length >= 3" @click="addSegment">
              <Plus class="size-4" />添加段落（最多 3 个）
            </button>
          </div>
        </section>
      </div>

      <!-- 右侧发布设置 -->
      <aside class="space-y-4 xl:sticky xl:top-20 xl:self-start">
        <section class="surface p-4">
          <h2 class="mb-1 section-title">发布设置</h2>
          <p class="mb-4 text-xs ui-text-muted">完成 {{ readyCount }} / {{ checks.length }} 项即可发布</p>

          <div class="mb-4 space-y-1.5">
            <div v-for="check in checks" :key="check.label" class="flex items-center gap-2 text-xs">
              <span
                class="flex h-4 w-4 items-center justify-center rounded-full"
                :class="check.ok ? 'bg-emerald-100 text-emerald-600' : 'ui-bg-soft ui-text-muted'"
              >
                <component :is="check.ok ? Check : Minus" class="size-3" />
              </span>
              <span :class="check.ok ? 'ui-text-base' : 'ui-text-strong'">{{ check.label }}</span>
            </div>
          </div>

          <div class="border-t ui-border pt-4">
            <div class="mb-2 flex items-center justify-between">
              <span class="text-xs font-medium ui-text-base">标签 <span class="text-rose-500">*</span></span>
              <span class="text-xs ui-text-muted">{{ tags.length }} / 6</span>
            </div>

            <div v-if="tags.length" class="mb-2 flex flex-wrap gap-1.5">
              <span v-for="tag in tags" :key="tag" class="badge badge-primary badge-sm gap-1">
                {{ tag }}
                <button type="button" class="inline-flex" aria-label="移除标签" @click="removeTag(tag)"><X class="size-3" /></button>
              </span>
            </div>

            <div class="join w-full">
              <input
                v-model="tagInput"
                class="input input-bordered input-sm join-item min-w-0 flex-1 focus:outline-none"
                placeholder="输入标签后回车"
                @keydown.enter.prevent="addTag(tagInput)"
              />
              <button class="btn btn-sm join-item" type="button" @click="addTag(tagInput)"><Plus class="size-4" />添加</button>
            </div>

            <button
              class="mt-2 flex w-full items-center justify-between rounded-lg border ui-border px-3 py-2 text-xs ui-text-base hover:border-indigo-300 hover:text-indigo-600"
              type="button"
              @click="showTagPanel = !showTagPanel"
            >
              从常用标签中选择
              <component :is="showTagPanel ? ChevronUp : ChevronDown" class="size-4" />
            </button>

            <div v-if="showTagPanel" class="mt-2 max-h-56 space-y-2 overflow-y-auto rounded-lg ui-bg-soft p-3">
              <div v-for="group in meta.tagCategories" :key="group.categoryName">
                <p class="mb-1 text-xs font-medium ui-text-base">{{ group.categoryName }}</p>
                <div class="flex flex-wrap gap-1">
                  <button
                    v-for="tag in group.tags"
                    :key="tag"
                    class="badge badge-xs cursor-pointer ui-border ui-bg ui-text-base hover:border-indigo-300 hover:text-indigo-600"
                    type="button"
                    @click="addTag(tag)"
                  >
                    {{ tag }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="mt-4 border-t ui-border pt-4">
            <span class="mb-2 block text-xs font-medium ui-text-base">验证码 <span class="text-rose-500">*</span></span>
            <Captcha ref="captchaRef" v-model="captcha" />
          </div>

          <p v-if="meta.reviewEnabled" class="mt-3 flex items-start gap-1.5 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs leading-relaxed text-amber-700">
            <ShieldCheck class="mt-0.5 size-3.5 shrink-0" />
            当前社区已开启内容审核，{{ editId ? '保存修改' : '发布' }}后需管理员审核通过才公开展示，审核结果会通过站内消息通知你。
          </p>

          <button
            class="btn btn-primary mt-5 w-full"
            :disabled="submitting || readyCount < checks.length"
            type="button"
            @click="submit"
          >
            <Send class="size-4" />
            {{ submitting ? '提交中…' : editId
              ? (meta.reviewEnabled ? '保存并提交审核' : '保存修改')
              : (meta.reviewEnabled ? '发布并提交审核' : '发布讨论') }}
          </button>
          <p class="mt-2 text-center text-xs ui-text-muted">发布表示你已阅读并同意社区规范</p>
        </section>

        <section class="surface p-4">
          <h2 class="mb-3 flex items-center gap-2 section-title">
            <span class="h-3.5 w-1 rounded-full bg-amber-400" />
            写出好问题的 4 个建议
          </h2>
          <ol class="space-y-2 text-xs leading-relaxed ui-text-base">
            <li><b class="ui-text-strong">1. 标题具体</b> —— 避免「求助」「急」这类空泛描述。</li>
            <li><b class="ui-text-strong">2. 背景完整</b> —— 说明环境、版本与期望结果。</li>
            <li><b class="ui-text-strong">3. 附上尝试</b> —— 写清已试过的方法与报错信息。</li>
            <li><b class="ui-text-strong">4. 保持友善</b> —— 尊重每一位愿意花时间回答的人。</li>
          </ol>
        </section>

        <AdCarousel :items="ads" title="社区推广" />
      </aside>
    </div>

    <!-- 正文预览 -->
    <dialog v-if="previewOpen" class="modal modal-open">
      <div class="modal-box max-w-3xl">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-base font-semibold">正文预览</h3>
          <button class="btn btn-ghost btn-circle btn-sm" type="button" aria-label="关闭" @click="previewOpen = false"><X class="size-4" /></button>
        </div>
        <div class="mb-4 border-b ui-border pb-3">
          <h2 class="text-xl font-bold ui-text-strong">{{ title || '（未填写标题）' }}</h2>
          <p class="mt-1 text-xs ui-text-muted">{{ classification || '未选择分类' }} · {{ tags.join(' / ') || '暂无标签' }}</p>
        </div>
        <div class="max-h-[55vh] overflow-y-auto">
          <div v-for="(segment, index) in segments" :key="segment.key" class="mb-5">
            <p class="mb-1 text-xs font-medium ui-text-muted">段落 {{ index + 1 }} · {{ segmentTypeLabel(segment.type) }}</p>
            <MarkdownView :content="contentOf(segment)" />
          </div>
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="previewOpen = false">关闭</button>
        </div>
      </div>
    </dialog>
  </div>
</template>
