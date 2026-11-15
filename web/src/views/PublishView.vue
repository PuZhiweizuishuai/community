<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Check, ChevronDown, ChevronUp, Eye, Minus, Plus, Send, X } from '@lucide/vue'
import { questionApi } from '@/api'
import { useMetaStore } from '@/stores/meta'
import { useToastStore } from '@/stores/toast'
import VditorEditor from '@/components/VditorEditor.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import Captcha from '@/components/Captcha.vue'
import AdCarousel from '@/components/AdCarousel.vue'

const route = useRoute()
const router = useRouter()
const meta = useMetaStore()
const toast = useToastStore()

const editId = computed(() => route.params.id || null)
const title = ref('')
const classification = ref('')
const description = ref('')
const tags = ref([])
const tagInput = ref('')
const captcha = ref('')
const createTime = ref(0)
const questionId = ref(-1)
const submitting = ref(false)
const captchaRef = ref(null)
const editor = ref(null)
const showTagPanel = ref(false)
const previewOpen = ref(false)

const editClasses = computed(() => meta.classifications.filter((c) => c.value !== 0))
const ads = computed(() => meta.adsFor('publish'))

const checks = computed(() => [
  { label: '标题（≤ 50 字）', ok: title.value.trim().length > 0 && title.value.length <= 50 },
  { label: '问题分类', ok: !!classification.value },
  { label: '正文内容', ok: (description.value || '').trim().length > 0 },
  { label: '标签（1 ~ 6 个）', ok: tags.value.length > 0 },
  { label: '验证码', ok: captcha.value.trim().length > 0 },
])
const readyCount = computed(() => checks.value.filter((c) => c.ok).length)

async function loadEdit() {
  if (!editId.value) {
    title.value = ''
    classification.value = ''
    description.value = ''
    tags.value = []
    questionId.value = -1
    createTime.value = 0
    editor.value?.setValue?.('')
    return
  }
  const question = await questionApi.edit(editId.value)
  title.value = question.title || ''
  classification.value = question.classification || ''
  description.value = question.description || ''
  tags.value = String(question.tag || '').split(/[,，]/).filter(Boolean)
  createTime.value = question.createTime || 0
  questionId.value = question.questionId
  editor.value?.setValue?.(description.value)
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

function currentContent() {
  return editor.value?.getValue?.() ?? description.value ?? ''
}

async function submit() {
  const content = currentContent()
  if (!title.value.trim()) return toast.warning('请填写标题')
  if (title.value.length > 50) return toast.warning('标题不能超过 50 个字')
  if (!classification.value) return toast.warning('请选择问题分类')
  if (!content.trim()) return toast.warning('正文不能为空')
  if (!tags.value.length) return toast.warning('请至少添加一个标签')
  if (!captcha.value.trim()) return toast.warning('请输入验证码')

  submitting.value = true
  try {
    const result = await questionApi.save({
      title: title.value.trim(),
      classification: classification.value,
      description: content,
      tag: tags.value.join(','),
      questionId: questionId.value,
      createTime: createTime.value,
      captcha: captcha.value,
    })
    toast.success(editId.value ? '修改成功' : '发布成功')
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
        <div class="flex items-center gap-1.5 text-xs text-slate-400">
          <RouterLink to="/" class="hover:text-indigo-600">首页</RouterLink>
          <span>/</span>
          <span>{{ editId ? '编辑讨论' : '发起讨论' }}</span>
        </div>
        <h1 class="mt-1 text-2xl font-bold text-slate-800">{{ editId ? '编辑你的讨论' : '分享你的问题与想法' }}</h1>
        <p class="mt-1 text-sm text-slate-500">清晰的标题、完整的背景与合适的标签，能让你更快得到高质量的回复。</p>
      </div>
      <button class="btn btn-ghost btn-sm" type="button" @click="previewOpen = true"><Eye class="size-4" />预览正文</button>
    </div>

    <div class="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_320px]">
      <!-- 主编辑区 -->
      <div class="min-w-0 space-y-4">
        <section class="surface p-5">
          <label class="block">
            <span class="mb-2 flex items-baseline justify-between">
              <span class="text-xs font-medium text-slate-500">标题 <span class="text-rose-500">*</span></span>
              <span class="text-xs" :class="title.length > 45 ? 'text-rose-500' : 'text-slate-400'">{{ title.length }} / 50</span>
            </span>
            <input
              v-model="title"
              maxlength="50"
              class="w-full border-0 border-b border-slate-200 bg-transparent pb-3 text-xl font-semibold text-slate-800 placeholder:font-normal placeholder:text-slate-300 focus:border-indigo-400 focus:outline-none"
              placeholder="用一句话概括你的问题"
            />
          </label>

          <div class="mt-5">
            <span class="mb-2 block text-xs font-medium text-slate-500">问题分类 <span class="text-rose-500">*</span></span>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="item in editClasses"
                :key="item.value"
                type="button"
                class="rounded-full border px-3.5 py-1.5 text-sm transition"
                :class="classification === item.label
                  ? 'border-indigo-500 bg-indigo-50 font-medium text-indigo-600'
                  : 'border-slate-200 text-slate-600 hover:border-indigo-300 hover:text-indigo-600'"
                @click="classification = item.label"
              >
                {{ item.label }}
              </button>
            </div>
          </div>
        </section>

        <section class="surface overflow-hidden">
          <div class="flex items-center justify-between border-b border-slate-100 px-5 py-3">
            <div>
              <h2 class="text-sm font-semibold text-slate-700">正文 <span class="text-rose-500">*</span></h2>
              <p class="mt-0.5 text-xs text-slate-400">支持 Markdown、代码块、公式、流程图与图片拖拽上传</p>
            </div>
            <span class="hidden text-xs text-slate-400 sm:block">自动保存已关闭 · 记得及时发布</span>
          </div>
          <div class="p-2 sm:p-3">
            <VditorEditor ref="editor" v-model="description" :height="520" />
          </div>
        </section>
      </div>

      <!-- 右侧发布设置 -->
      <aside class="space-y-4 xl:sticky xl:top-20 xl:self-start">
        <section class="surface p-4">
          <h2 class="mb-1 text-sm font-semibold text-slate-700">发布设置</h2>
          <p class="mb-4 text-xs text-slate-400">完成 {{ readyCount }} / {{ checks.length }} 项即可发布</p>

          <div class="mb-4 space-y-1.5">
            <div v-for="check in checks" :key="check.label" class="flex items-center gap-2 text-xs">
              <span
                class="flex h-4 w-4 items-center justify-center rounded-full"
                :class="check.ok ? 'bg-emerald-100 text-emerald-600' : 'bg-slate-100 text-slate-400'"
              >
                <component :is="check.ok ? Check : Minus" class="size-3" />
              </span>
              <span :class="check.ok ? 'text-slate-500' : 'text-slate-700'">{{ check.label }}</span>
            </div>
          </div>

          <div class="border-t border-slate-100 pt-4">
            <div class="mb-2 flex items-center justify-between">
              <span class="text-xs font-medium text-slate-600">标签 <span class="text-rose-500">*</span></span>
              <span class="text-xs text-slate-400">{{ tags.length }} / 6</span>
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
              class="mt-2 flex w-full items-center justify-between rounded-lg border border-slate-200 px-3 py-2 text-xs text-slate-500 hover:border-indigo-300 hover:text-indigo-600"
              type="button"
              @click="showTagPanel = !showTagPanel"
            >
              从常用标签中选择
              <component :is="showTagPanel ? ChevronUp : ChevronDown" class="size-4" />
            </button>

            <div v-if="showTagPanel" class="mt-2 max-h-56 space-y-2 overflow-y-auto rounded-lg bg-slate-50 p-3">
              <div v-for="group in meta.tagCategories" :key="group.categoryName">
                <p class="mb-1 text-xs font-medium text-slate-600">{{ group.categoryName }}</p>
                <div class="flex flex-wrap gap-1">
                  <button
                    v-for="tag in group.tags"
                    :key="tag"
                    class="badge badge-xs cursor-pointer border-slate-200 bg-white text-slate-500 hover:border-indigo-300 hover:text-indigo-600"
                    type="button"
                    @click="addTag(tag)"
                  >
                    {{ tag }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="mt-4 border-t border-slate-100 pt-4">
            <span class="mb-2 block text-xs font-medium text-slate-600">验证码 <span class="text-rose-500">*</span></span>
            <Captcha ref="captchaRef" v-model="captcha" />
          </div>

          <button
            class="btn btn-primary mt-5 w-full"
            :disabled="submitting || readyCount < checks.length"
            type="button"
            @click="submit"
          >
            <Send class="size-4" />
            {{ submitting ? '发布中…' : editId ? '保存修改' : '发布讨论' }}
          </button>
          <p class="mt-2 text-center text-xs text-slate-400">发布表示你已阅读并同意社区规范</p>
        </section>

        <section class="surface p-4">
          <h2 class="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-700">
            <span class="h-3.5 w-1 rounded-full bg-amber-400" />
            写出好问题的 4 个建议
          </h2>
          <ol class="space-y-2 text-xs leading-relaxed text-slate-500">
            <li><b class="text-slate-700">1. 标题具体</b> —— 避免「求助」「急」这类空泛描述。</li>
            <li><b class="text-slate-700">2. 背景完整</b> —— 说明环境、版本与期望结果。</li>
            <li><b class="text-slate-700">3. 附上尝试</b> —— 写清已试过的方法与报错信息。</li>
            <li><b class="text-slate-700">4. 保持友善</b> —— 尊重每一位愿意花时间回答的人。</li>
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
        <div class="mb-4 border-b border-slate-100 pb-3">
          <h2 class="text-xl font-bold text-slate-800">{{ title || '（未填写标题）' }}</h2>
          <p class="mt-1 text-xs text-slate-400">{{ classification || '未选择分类' }} · {{ tags.join(' / ') || '暂无标签' }}</p>
        </div>
        <div class="max-h-[55vh] overflow-y-auto">
          <MarkdownView :content="currentContent()" />
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="previewOpen = false">关闭</button>
        </div>
      </div>
    </dialog>
  </div>
</template>
