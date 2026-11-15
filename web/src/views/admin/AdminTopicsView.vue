<script setup>
import { computed, onMounted, ref } from 'vue'
import { ChevronDown, ChevronRight, Plus, Power, RefreshCw, Search, SquarePen, Tags, Trash2, X } from '@lucide/vue'
import { adminApi, settingApi, assetUrl } from '@/api'
import { useMetaStore } from '@/stores/meta'
import { useToastStore } from '@/stores/toast'

const meta = useMetaStore()
const toast = useToastStore()

const topics = ref([])
const loading = ref(false)
const type = ref('')
const keyword = ref('')
const editing = ref(null)
const saving = ref(false)
const collapsed = ref({})
const viewMode = ref('group')

const emptyForm = () => ({
  id: 0,
  title: '',
  simpleDesc: '',
  image: '/image/topicicon/default.svg',
  type: 1,
  status: 1,
})
const form = ref(emptyForm())

const typeLabel = (value) => meta.topicTypes.find((t) => String(t.value) === String(value))?.label || `分类 ${value}`

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return topics.value.filter((t) => {
    if (type.value && String(t.type) !== String(type.value)) return false
    if (kw && !(t.title || '').toLowerCase().includes(kw) && !(t.simpleDesc || '').toLowerCase().includes(kw)) return false
    return true
  })
})

/** 按所属分类分组展示；没有匹配到动态分类的归到「未分类」 */
const groups = computed(() => {
  const result = meta.topicTypes.map((item) => ({
    code: item.value,
    label: item.label,
    topics: [],
  }))
  const unknown = { code: '__unknown__', label: '未分类 / 已停用分类', topics: [] }
  const index = new Map(result.map((g) => [String(g.code), g]))
  for (const topic of filtered.value) {
    const group = index.get(String(topic.type)) || unknown
    group.topics.push(topic)
  }
  if (unknown.topics.length) result.push(unknown)
  return result
})

async function load(force = false) {
  loading.value = true
  try {
    const result = await adminApi.topics(force ? {} : { type: type.value || undefined, keyword: keyword.value || undefined })
    topics.value = result.topics || []
  } finally {
    loading.value = false
  }
}
onMounted(() => load(true))

function openCreate(groupCode) {
  form.value = emptyForm()
  if (typeof groupCode === 'number') form.value.type = groupCode
  editing.value = form.value
}
function openEdit(item) {
  form.value = { ...item }
  editing.value = form.value
}

async function save() {
  if (!form.value.title.trim()) return toast.warning('请填写话题名称')
  saving.value = true
  try {
    await adminApi.saveTopic(form.value)
    toast.success(form.value.id > 0 ? '话题已更新' : '话题已创建')
    editing.value = null
    await load(true)
  } finally {
    saving.value = false
  }
}

async function remove(item) {
  if (!window.confirm(`确定删除话题「${item.title}」？该操作不可恢复。`)) return
  await adminApi.deleteTopic(item.id)
  toast.success('话题已删除')
  await load(true)
}

async function toggleStatus(item) {
  await adminApi.saveTopic({ ...item, status: item.status === 1 ? 0 : 1 })
  toast.success(item.status === 1 ? '已停用' : '已启用')
  await load(true)
}

/** 把话题移动到另一个分类（含后端缓存同步） */
async function moveTopic(item, event) {
  const next = Number(event.target.value)
  if (!next || next === item.type) return
  await adminApi.saveTopic({ ...item, type: next })
  toast.success('话题所属分类已更新')
  await load(true)
}

async function refreshCache() {
  await adminApi.refreshTopics()
  toast.success('话题缓存已同步')
  await load(true)
}

async function uploadImage(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const formData = new FormData()
  formData.append('editormd-image-file', file)
  formData.append('type', 'topic')
  const result = await settingApi.uploadImage(formData)
  if (result?.url) {
    form.value.image = result.url
    toast.success('图标已上传')
  }
  event.target.value = ''
}
</script>

<template>
  <div class="space-y-4">
    <div class="surface flex flex-wrap items-center gap-2 p-4">
      <div class="mr-auto">
        <h2 class="text-sm font-semibold text-slate-700">话题管理</h2>
        <p class="mt-1 text-xs text-slate-400">按所属分类分组展示，可直接修改话题的分类、名称、图标与简介</p>
      </div>
      <RouterLink to="/admin/classifications" class="btn btn-ghost btn-sm"><Tags class="size-4" />管理分类</RouterLink>
      <button class="btn btn-ghost btn-sm" type="button" @click="refreshCache"><RefreshCw class="size-4" />同步缓存</button>
      <button class="btn btn-primary btn-sm" type="button" @click="openCreate()"><Plus class="size-4" />新增话题</button>
    </div>

    <div class="surface flex flex-wrap items-center gap-2 p-4">
      <select v-model="type" class="select select-bordered select-sm w-40" @change="load()">
        <option value="">全部分类</option>
        <option v-for="item in meta.topicTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <form class="join" @submit.prevent="load()">
        <input v-model="keyword" class="input input-bordered input-sm join-item w-56" placeholder="搜索话题名称 / 简介" />
        <button class="btn btn-sm btn-primary join-item" type="submit"><Search class="size-4" />搜索</button>
      </form>
      <div class="ml-auto flex items-center gap-2">
        <span class="text-xs text-slate-400">共 {{ filtered.length }} 个话题</span>
        <div class="btn-group btn-group-sm">
          <button class="btn btn-xs" :class="viewMode === 'group' ? 'btn-active btn-primary' : 'btn-ghost'" type="button" @click="viewMode = 'group'">分组</button>
          <button class="btn btn-xs" :class="viewMode === 'flat' ? 'btn-active btn-primary' : 'btn-ghost'" type="button" @click="viewMode = 'flat'">平铺</button>
        </div>
      </div>
    </div>

    <div v-if="loading && !topics.length" class="surface py-12 text-center text-sm text-slate-400">加载中…</div>
    <div v-else-if="!filtered.length" class="surface py-12 text-center text-sm text-slate-400">没有匹配的话题</div>

    <!-- 分组视图 -->
    <div v-else-if="viewMode === 'group'" class="space-y-4">
      <section v-for="group in groups" :key="group.code" class="surface overflow-hidden">
        <div class="flex flex-wrap items-center gap-2 border-b border-slate-100 px-4 py-3">
          <button class="btn btn-ghost btn-xs" type="button" :aria-label="collapsed[group.code] ? '展开' : '收起'" @click="collapsed[group.code] = !collapsed[group.code]">
            <component :is="collapsed[group.code] ? ChevronRight : ChevronDown" class="size-4" />
          </button>
          <h3 class="text-sm font-semibold text-slate-700">{{ group.label }}</h3>
          <span class="badge badge-sm badge-ghost">{{ group.topics.length }} 个话题</span>
          <span class="text-xs text-slate-400">共 {{ group.topics.reduce((sum, t) => sum + (t.talkCount || 0), 0) }} 条讨论</span>
          <button v-if="typeof group.code === 'number'" class="btn btn-ghost btn-xs ml-auto" type="button" @click="openCreate(group.code)"><Plus class="size-3.5" />在此分类新增</button>
        </div>

        <div v-if="!collapsed[group.code]" class="grid gap-3 p-4 md:grid-cols-2 xl:grid-cols-3">
          <article v-for="item in group.topics" :key="item.id" class="rounded-xl border border-slate-200 p-3 transition hover:border-indigo-300 hover:shadow-sm">
            <div class="flex items-start gap-3">
              <img :src="assetUrl(item.image)" :alt="item.title" class="h-12 w-12 shrink-0 rounded-lg bg-slate-50 object-cover" />
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2">
                  <h4 class="truncate font-medium text-slate-800">{{ item.title }}</h4>
                  <span class="badge badge-xs" :class="item.status === 1 ? 'badge-success' : 'badge-ghost'">{{ item.status === 1 ? '启用' : '停用' }}</span>
                </div>
                <p class="line-clamp-2 mt-1 text-xs text-slate-500">{{ item.simpleDesc || '暂无简介' }}</p>
                <p class="mt-1.5 text-xs text-slate-400">{{ item.followCount }} 人关注 · {{ item.talkCount }} 条讨论</p>
              </div>
            </div>
            <div class="mt-3 flex flex-wrap gap-1 border-t border-slate-100 pt-2">
              <select class="select select-ghost select-xs max-w-[130px]" :value="item.type" @change="moveTopic(item, $event)">
                <option v-for="option in meta.topicTypes" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
              <button class="btn btn-ghost btn-xs" type="button" @click="openEdit(item)"><SquarePen class="size-3.5" />编辑</button>
              <button class="btn btn-ghost btn-xs" type="button" @click="toggleStatus(item)"><Power class="size-3.5" />{{ item.status === 1 ? '停用' : '启用' }}</button>
              <button class="btn btn-ghost btn-xs text-rose-500" type="button" @click="remove(item)"><Trash2 class="size-3.5" />删除</button>
            </div>
          </article>
          <p v-if="!group.topics.length" class="col-span-full py-6 text-center text-xs text-slate-400">该分类下暂无话题</p>
        </div>
      </section>
    </div>

    <!-- 平铺视图 -->
    <div v-else class="surface grid gap-3 p-4 md:grid-cols-2 xl:grid-cols-3">
      <article v-for="item in filtered" :key="item.id" class="rounded-xl border border-slate-200 p-3">
        <div class="flex items-start gap-3">
          <img :src="assetUrl(item.image)" :alt="item.title" class="h-12 w-12 shrink-0 rounded-lg bg-slate-50 object-cover" />
          <div class="min-w-0">
            <h4 class="truncate font-medium text-slate-800">{{ item.title }}</h4>
            <p class="mt-0.5 text-xs text-indigo-500">{{ typeLabel(item.type) }}</p>
            <p class="line-clamp-2 mt-1 text-xs text-slate-500">{{ item.simpleDesc || '暂无简介' }}</p>
          </div>
        </div>
        <div class="mt-3 flex gap-1 border-t border-slate-100 pt-2">
          <button class="btn btn-ghost btn-xs" type="button" @click="openEdit(item)"><SquarePen class="size-3.5" />编辑</button>
          <button class="btn btn-ghost btn-xs text-rose-500" type="button" @click="remove(item)"><Trash2 class="size-3.5" />删除</button>
        </div>
      </article>
    </div>

    <!-- 编辑弹窗 -->
    <dialog v-if="editing" class="modal modal-open">
      <div class="modal-box max-w-xl">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-base font-semibold">{{ form.id > 0 ? '编辑话题' : '新增话题' }}</h3>
          <button class="btn btn-ghost btn-circle btn-xs" type="button" aria-label="关闭" @click="editing = null"><X class="size-4" /></button>
        </div>

        <div class="space-y-3">
          <label class="form-control">
            <span class="label-text mb-1 font-medium">话题名称 *</span>
            <input v-model="form.title" maxlength="50" class="input input-bordered input-sm" placeholder="例如 JavaScript" />
            <span class="label-text-alt mt-1 text-slate-400">名称同时作为前台标签，修改后旧标签的帖子不会自动迁移</span>
          </label>

          <div class="grid gap-3 sm:grid-cols-2">
            <label class="form-control">
              <span class="label-text mb-1 font-medium">所属分类 *</span>
              <select v-model.number="form.type" class="select select-bordered select-sm">
                <option v-for="item in meta.topicTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label class="form-control">
              <span class="label-text mb-1 font-medium">状态</span>
              <select v-model.number="form.status" class="select select-bordered select-sm">
                <option :value="1">启用</option>
                <option :value="0">停用</option>
              </select>
            </label>
          </div>

          <div>
            <span class="label-text mb-1 block font-medium">话题图标</span>
            <div class="flex items-center gap-3">
              <img :src="assetUrl(form.image)" alt="图标预览" class="h-14 w-14 rounded-lg border border-slate-200 bg-slate-50 object-cover" />
              <div class="min-w-0 flex-1">
                <input type="file" accept="image/*" class="file-input file-input-bordered file-input-xs w-full" @change="uploadImage" />
                <input v-model="form.image" class="input input-bordered input-xs mt-2 w-full" placeholder="也可直接填写图标地址" />
              </div>
            </div>
          </div>

          <label class="form-control">
            <span class="label-text mb-1 font-medium">话题简介</span>
            <textarea v-model="form.simpleDesc" maxlength="500" rows="4" class="textarea textarea-bordered" placeholder="一句话介绍这个话题…" />
          </label>
        </div>

        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="editing = null">取消</button>
          <button class="btn btn-primary btn-sm" :disabled="saving" type="button" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </dialog>
  </div>
</template>
