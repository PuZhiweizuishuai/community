<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Check, ImagePlus, Minus, Upload } from '@lucide/vue'
import { adminApi, settingApi, assetUrl } from '@/api'
import { useToastStore } from '@/stores/toast'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const isEdit = computed(() => !!route.params.id && route.params.id !== 'new')
const form = ref({ id: null, title: '', url: '', image: '' })
const preview = ref('')
const saving = ref(false)
const uploading = ref(false)
const dragging = ref(false)
const fileInput = ref(null)
const loading = ref(false)

const imageUrl = computed(() => preview.value || (form.value.image ? assetUrl(form.value.image) : ''))
const urlValid = computed(() => {
  const value = form.value.url?.trim()
  return !value || /^https?:\/\//i.test(value) || value.startsWith('/')
})
const checks = computed(() => [
  { label: '广告标题', ok: form.value.title.trim().length > 0 },
  { label: '展示图片', ok: !!form.value.image },
  { label: '跳转链接格式正确', ok: urlValid.value },
])
const ready = computed(() => checks.value.every((c) => c.ok))

onMounted(async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const ad = await adminApi.advertisement(route.params.id)
    form.value = { id: ad?.id, title: ad?.title || '', url: ad?.url || '', image: ad?.image || '' }
  } finally {
    loading.value = false
  }
})

async function uploadFile(file) {
  if (!file) return
  if (!file.type.startsWith('image/')) return toast.warning('请选择图片文件')
  uploading.value = true
  try {
    preview.value = URL.createObjectURL(file)
    const formData = new FormData()
    formData.append('editormd-image-file', file)
    formData.append('type', 'ad')
    const result = await settingApi.uploadImage(formData)
    if (result?.url) {
      form.value.image = result.url
      toast.success('图片已上传')
    }
  } finally {
    uploading.value = false
  }
}
function onPick(event) {
  uploadFile(event.target.files?.[0])
  event.target.value = ''
}
function onDrop(event) {
  dragging.value = false
  uploadFile(event.dataTransfer?.files?.[0])
}

async function save() {
  if (!form.value.title.trim()) return toast.warning('请填写广告标题')
  if (!urlValid.value) return toast.warning('跳转链接需以 http(s):// 或 / 开头')
  saving.value = true
  try {
    if (isEdit.value) await adminApi.alterAd(form.value)
    else await adminApi.addAd(form.value)
    toast.success(isEdit.value ? '广告已更新' : '广告已创建')
    router.push('/admin/advertisements')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-5xl">
    <!-- 标题栏 -->
    <div class="mb-5 flex flex-wrap items-end justify-between gap-3">
      <div>
        <div class="flex items-center gap-1.5 text-xs text-slate-400">
          <RouterLink to="/admin/advertisements" class="hover:text-indigo-600">广告与新闻</RouterLink>
          <span>/</span>
          <span>{{ isEdit ? '编辑' : '新增' }}</span>
        </div>
        <h1 class="mt-1 text-xl font-bold text-slate-800">{{ isEdit ? '编辑广告' : '新增广告' }}</h1>
        <p class="mt-1 text-sm text-slate-500">配置展示素材与跳转地址，保存后在「广告列表」里设置投放位置与时间。</p>
      </div>
      <RouterLink to="/admin/advertisements" class="btn btn-ghost btn-sm"><ArrowLeft class="size-4" />返回列表</RouterLink>
    </div>

    <div v-if="loading" class="surface p-10 text-center text-sm text-slate-400">正在载入广告信息…</div>

    <div v-else class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,1fr)_300px]">
      <!-- 主表单 -->
      <div class="min-w-0 space-y-5">
        <section class="surface p-5">
          <div class="mb-4 flex items-center gap-2 border-b border-slate-100 pb-3">
            <span class="flex h-6 w-6 items-center justify-center rounded-md bg-indigo-50 text-xs font-bold text-indigo-600">1</span>
            <h2 class="text-sm font-semibold text-slate-700">基础信息</h2>
          </div>

          <label class="form-control">
            <span class="label-text mb-1.5 flex justify-between font-medium text-slate-700">
              广告标题 <span class="text-xs font-normal text-slate-400">{{ form.title.length }} / 60</span>
            </span>
            <input v-model="form.title" maxlength="60" class="input input-bordered w-full focus:border-indigo-400 focus:outline-none" placeholder="例如：社区年度技术沙龙" />
            <span class="label-text-alt mt-1 text-slate-400">会显示在图片下方，也会用于后台检索</span>
          </label>

          <label class="form-control mt-4">
            <span class="label-text mb-1.5 font-medium text-slate-700">跳转链接</span>
            <input v-model="form.url" class="input input-bordered w-full focus:border-indigo-400 focus:outline-none" placeholder="https://example.com 或 /about" />
            <span class="label-text-alt mt-1" :class="urlValid ? 'text-slate-400' : 'text-rose-500'">
              {{ urlValid ? '用户点击广告图时打开，统计浏览量后由服务端跳转' : '请填写以 http(s):// 或 / 开头的合法地址' }}
            </span>
          </label>
        </section>

        <section class="surface p-5">
          <div class="mb-4 flex items-center gap-2 border-b border-slate-100 pb-3">
            <span class="flex h-6 w-6 items-center justify-center rounded-md bg-indigo-50 text-xs font-bold text-indigo-600">2</span>
            <h2 class="text-sm font-semibold text-slate-700">展示素材</h2>
            <span class="ml-auto text-xs text-slate-400">建议 16:6 宽幅图</span>
          </div>

          <div
            class="relative flex h-44 items-center justify-center overflow-hidden rounded-xl border-2 border-dashed transition"
            :class="dragging ? 'border-indigo-400 bg-indigo-50' : 'border-slate-200 bg-slate-50'"
            @dragover.prevent="dragging = true"
            @dragleave.prevent="dragging = false"
            @drop.prevent="onDrop"
          >
            <img v-if="imageUrl" :src="imageUrl" alt="广告图片预览" class="h-full w-full object-cover" />
            <div v-else class="text-center text-xs text-slate-400">
              <ImagePlus class="mx-auto size-8 text-slate-300" :stroke-width="1.5" />
              <p class="mt-1">拖拽图片到这里，或点击下方按钮上传</p>
            </div>
            <span v-if="uploading" class="absolute inset-0 flex items-center justify-center bg-slate-900/40 text-sm text-white">上传中…</span>
          </div>

          <div class="mt-3 flex flex-wrap items-center gap-2">
            <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="onPick" />
            <button class="btn btn-outline btn-sm" type="button" :disabled="uploading" @click="fileInput?.click()"><Upload class="size-4" />选择图片</button>
            <span v-if="form.image" class="truncate text-xs text-slate-400">{{ form.image }}</span>
          </div>

          <label class="form-control mt-3">
            <span class="label-text mb-1.5 text-slate-600">或直接填写图片地址</span>
            <input v-model="form.image" class="input input-bordered input-sm w-full focus:border-indigo-400 focus:outline-none" placeholder="/image/… 或 https://…" @input="preview = ''" />
          </label>
        </section>
      </div>

      <!-- 右侧：预览 / 检查 / 操作 -->
      <aside class="space-y-4 lg:sticky lg:top-20 lg:self-start">
        <section class="surface p-4">
          <h2 class="mb-3 text-sm font-semibold text-slate-700">前台效果预览</h2>
          <div class="overflow-hidden rounded-lg border border-slate-200">
            <div class="h-24 bg-slate-100">
              <img v-if="imageUrl" :src="imageUrl" alt="" class="h-full w-full object-cover" />
              <div v-else class="flex h-full items-center justify-center text-xs text-slate-400">暂无图片</div>
            </div>
            <p class="truncate bg-white px-3 py-2 text-xs text-slate-700">{{ form.title || '（未填写标题）' }}</p>
          </div>
          <p class="mt-2 text-xs text-slate-400">这是侧边栏推广位的展示效果，实际投放位置在列表中设置。</p>
        </section>

        <section class="surface p-4">
          <h2 class="mb-1 text-sm font-semibold text-slate-700">保存前检查</h2>
          <p class="mb-3 text-xs text-slate-400">{{ checks.filter((c) => c.ok).length }} / {{ checks.length }} 项已完成</p>
          <div class="space-y-1.5">
            <div v-for="check in checks" :key="check.label" class="flex items-center gap-2 text-xs">
              <span class="flex h-4 w-4 items-center justify-center rounded-full" :class="check.ok ? 'bg-emerald-100 text-emerald-600' : 'bg-slate-100 text-slate-400'">
                <component :is="check.ok ? Check : Minus" class="size-3" />
              </span>
              <span :class="check.ok ? 'text-slate-500' : 'text-slate-700'">{{ check.label }}</span>
            </div>
          </div>
          <div class="mt-4 space-y-2 border-t border-slate-100 pt-3">
            <button class="btn btn-primary btn-sm w-full" :disabled="saving || !ready" type="button" @click="save">
              {{ saving ? '保存中…' : isEdit ? '保存修改' : '创建广告' }}
            </button>
            <RouterLink to="/admin/advertisements" class="btn btn-ghost btn-sm w-full">取消</RouterLink>
          </div>
        </section>

        <section class="surface p-4">
          <h2 class="mb-2 text-sm font-semibold text-slate-700">小贴士</h2>
          <ul class="list-inside list-disc space-y-1.5 text-xs leading-relaxed text-slate-500">
            <li>图片会自动裁剪填充，重要内容请放在中间。</li>
            <li>创建后默认为「未投放」，需要在列表中点击「投放」。</li>
            <li>每个位置可投放的广告数由后台配置限制。</li>
          </ul>
        </section>
      </aside>
    </div>
  </div>
</template>
