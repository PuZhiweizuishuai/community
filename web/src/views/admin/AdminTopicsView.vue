<script setup>
import { computed, onMounted, ref } from 'vue'
import { ChevronDown, ChevronRight, Plus, Power, RefreshCw, Search, ShieldCheck, SquarePen, Tags, Trash2, Users, X } from '@lucide/vue'
import { adminApi, settingApi, assetUrl } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useMetaStore } from '@/stores/meta'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/Avatar.vue'

const meta = useMetaStore()
const session = useSessionStore()
const toast = useToastStore()
const confirm = useConfirmStore()

const topics = ref([])
const loading = ref(false)
const type = ref('')
const reviewFilter = ref('')
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
  visibility: 0,
})
const form = ref(emptyForm())

// ---------------- 吧务团队 ----------------
const moderatorTarget = ref(null)
const moderators = ref([])
const moderatorLoading = ref(false)
const assignForm = ref({ userCode: '', role: 'moderator' })

async function openModerators(item) {
  moderatorTarget.value = item
  assignForm.value = { userCode: '', role: 'moderator' }
  await loadModerators()
}

async function loadModerators() {
  if (!moderatorTarget.value) return
  moderatorLoading.value = true
  try {
    moderators.value = (await adminApi.topicModerators(moderatorTarget.value.id)) || []
  } finally {
    moderatorLoading.value = false
  }
}

async function assignModerator() {
  if (!assignForm.value.userCode.trim()) return toast.warning('请填写用户登录账号')
  await adminApi.assignTopicModerator(moderatorTarget.value.id, {
    userCode: assignForm.value.userCode.trim(),
    role: assignForm.value.role,
  })
  toast.success('吧务已任命')
  assignForm.value.userCode = ''
  await loadModerators()
}

async function removeModerator(item) {
  if (!(await confirm.ask({ title: '卸任吧务', message: `确定卸任「${item.userName || item.userCode}」？`, confirmText: '卸任', danger: true }))) return
  await adminApi.removeTopicModerator(moderatorTarget.value.id, item.userId)
  toast.success('已卸任')
  await loadModerators()
}

const typeLabel = (value) => meta.topicTypes.find((t) => String(t.value) === String(value))?.label || `分类 ${value}`
const reviewLabel = (status) => (Number(status) === 0 ? '待审核' : Number(status) === 2 ? '未通过' : '已通过')
const reviewClass = (status) =>
  Number(status) === 0 ? 'badge-warning' : Number(status) === 2 ? 'badge-error' : 'badge-success'
const visibilityLabel = (visibility) =>
  Number(visibility) === 2 ? '私有 · 仅邀请' : Number(visibility) === 1 ? '私有 · 可申请' : '公开'

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
    const result = await adminApi.topics(
      force
        ? {}
        : {
            type: type.value || undefined,
            reviewStatus: reviewFilter.value === '' ? undefined : reviewFilter.value,
            keyword: keyword.value || undefined,
          },
    )
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
  if (!(await confirm.ask({ title: '删除话题', message: `确定删除话题「${item.title}」？该操作不可恢复。`, confirmText: '删除', danger: true }))) return
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
        <h2 class="section-title">话题管理</h2>
        <p class="mt-1 text-xs ui-text-muted">按所属分类分组展示，可直接维护话题信息；话题审核已统一移到「内容审核」</p>
      </div>
      <RouterLink v-if="session.can('admin:review')" to="/admin/reviews?type=topic&status=0" class="btn btn-ghost btn-sm">
        <ShieldCheck class="size-4" />话题审核
      </RouterLink>
      <RouterLink to="/admin/classifications" class="btn btn-ghost btn-sm"><Tags class="size-4" />管理分类</RouterLink>
      <button class="btn btn-ghost btn-sm" type="button" @click="refreshCache"><RefreshCw class="size-4" />同步缓存</button>
      <button class="btn btn-primary btn-sm" type="button" @click="openCreate()"><Plus class="size-4" />新增话题</button>
    </div>

    <div class="surface flex flex-wrap items-center gap-2 p-4">
      <select v-model="type" class="select select-bordered select-sm w-40" @change="load()">
        <option value="">全部分类</option>
        <option v-for="item in meta.topicTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <select v-model="reviewFilter" class="select select-bordered select-sm w-36" @change="load()">
        <option value="">全部审核状态</option>
        <option value="0">待审核</option>
        <option value="1">已通过</option>
        <option value="2">未通过</option>
      </select>
      <form class="join" @submit.prevent="load()">
        <input v-model="keyword" class="input input-bordered input-sm join-item w-56" placeholder="搜索话题名称 / 简介" />
        <button class="btn btn-sm btn-primary join-item" type="submit"><Search class="size-4" />搜索</button>
      </form>
      <div class="ml-auto flex items-center gap-2">
        <span class="text-xs ui-text-muted">共 {{ filtered.length }} 个话题</span>
        <div class="btn-group btn-group-sm">
          <button class="btn btn-xs" :class="viewMode === 'group' ? 'btn-active btn-primary' : 'btn-ghost'" type="button" @click="viewMode = 'group'">分组</button>
          <button class="btn btn-xs" :class="viewMode === 'flat' ? 'btn-active btn-primary' : 'btn-ghost'" type="button" @click="viewMode = 'flat'">平铺</button>
        </div>
      </div>
    </div>

    <div v-if="loading && !topics.length" class="surface empty-state">加载中…</div>
    <div v-else-if="!filtered.length" class="surface empty-state">没有匹配的话题</div>

    <!-- 分组视图 -->
    <div v-else-if="viewMode === 'group'" class="space-y-4">
      <section v-for="group in groups" :key="group.code" class="surface overflow-hidden">
        <div class="flex flex-wrap items-center gap-2 border-b ui-border px-4 py-3">
          <button class="btn btn-ghost btn-xs" type="button" :aria-label="collapsed[group.code] ? '展开' : '收起'" @click="collapsed[group.code] = !collapsed[group.code]">
            <component :is="collapsed[group.code] ? ChevronRight : ChevronDown" class="size-4" />
          </button>
          <h3 class="section-title">{{ group.label }}</h3>
          <span class="badge badge-sm badge-ghost">{{ group.topics.length }} 个话题</span>
          <span class="text-xs ui-text-muted">共 {{ group.topics.reduce((sum, t) => sum + (t.talkCount || 0), 0) }} 条讨论</span>
          <button v-if="typeof group.code === 'number'" class="btn btn-ghost btn-xs ml-auto" type="button" @click="openCreate(group.code)"><Plus class="size-3.5" />在此分类新增</button>
        </div>

        <div v-if="!collapsed[group.code]" class="grid gap-3 p-4 md:grid-cols-2 xl:grid-cols-3">
          <article v-for="item in group.topics" :key="item.id" class="rounded-xl border ui-border p-3 transition hover:border-indigo-300 hover:shadow-sm">
            <div class="flex items-start gap-3">
              <img :src="assetUrl(item.image)" :alt="item.title" class="h-12 w-12 shrink-0 rounded-lg ui-bg-soft object-cover" />
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-1.5">
                  <h4 class="truncate font-medium ui-text-strong">{{ item.title }}</h4>
                  <span class="badge badge-xs" :class="item.status === 1 ? 'badge-success' : 'badge-ghost'">{{ item.status === 1 ? '启用' : '停用' }}</span>
                  <span class="badge badge-xs" :class="reviewClass(item.reviewStatus)">{{ reviewLabel(item.reviewStatus) }}</span>
                  <span class="badge badge-xs badge-outline">{{ visibilityLabel(item.visibility) }}</span>
                </div>
                <p class="line-clamp-2 mt-1 text-xs ui-text-base">{{ item.simpleDesc || '暂无简介' }}</p>
                <p class="mt-1.5 text-xs ui-text-muted">
                  {{ item.creatorName ? `创建人 ${item.creatorName}` : '系统创建' }} · {{ item.followCount }} 人关注 · {{ item.talkCount }} 条讨论
                </p>
              </div>
            </div>
            <div class="mt-3 flex flex-wrap gap-1 border-t ui-border pt-2">
              <select class="select select-ghost select-xs max-w-[130px]" :value="item.type" @change="moveTopic(item, $event)">
                <option v-for="option in meta.topicTypes" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
              <button class="btn btn-ghost btn-xs" type="button" @click="openModerators(item)"><Users class="size-3.5" />吧务团队</button>
              <button class="btn btn-ghost btn-xs" type="button" @click="openEdit(item)"><SquarePen class="size-3.5" />编辑</button>
              <button class="btn btn-ghost btn-xs" type="button" @click="toggleStatus(item)"><Power class="size-3.5" />{{ item.status === 1 ? '停用' : '启用' }}</button>
              <button class="btn btn-ghost btn-xs text-rose-500" type="button" @click="remove(item)"><Trash2 class="size-3.5" />删除</button>
            </div>
          </article>
          <p v-if="!group.topics.length" class="col-span-full py-6 text-center text-xs ui-text-muted">该分类下暂无话题</p>
        </div>
      </section>
    </div>

    <!-- 平铺视图 -->
    <div v-else class="surface grid gap-3 p-4 md:grid-cols-2 xl:grid-cols-3">
      <article v-for="item in filtered" :key="item.id" class="rounded-xl border ui-border p-3">
        <div class="flex items-start gap-3">
          <img :src="assetUrl(item.image)" :alt="item.title" class="h-12 w-12 shrink-0 rounded-lg ui-bg-soft object-cover" />
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-1.5">
              <h4 class="truncate font-medium ui-text-strong">{{ item.title }}</h4>
              <span class="badge badge-xs" :class="reviewClass(item.reviewStatus)">{{ reviewLabel(item.reviewStatus) }}</span>
              <span class="badge badge-xs badge-outline">{{ visibilityLabel(item.visibility) }}</span>
            </div>
            <p class="mt-0.5 text-xs ui-text-brand">{{ typeLabel(item.type) }}</p>
            <p class="line-clamp-2 mt-1 text-xs ui-text-base">{{ item.simpleDesc || '暂无简介' }}</p>
          </div>
        </div>
        <div class="mt-3 flex flex-wrap gap-1 border-t ui-border pt-2">
          <button class="btn btn-ghost btn-xs" type="button" @click="openModerators(item)"><Users class="size-3.5" />吧务</button>
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
            <span class="label-text-alt mt-1 ui-text-muted">名称同时作为前台标签，修改后旧标签的帖子不会自动迁移</span>
          </label>

          <div class="grid gap-3 sm:grid-cols-3">
            <label class="form-control">
              <span class="label-text mb-1 font-medium">所属分类 *</span>
              <select v-model.number="form.type" class="select select-bordered select-sm">
                <option v-for="item in meta.topicTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label class="form-control">
              <span class="label-text mb-1 font-medium">可见性</span>
              <select v-model.number="form.visibility" class="select select-bordered select-sm">
                <option :value="0">公开</option>
                <option :value="1">私有 · 可申请</option>
                <option :value="2">私有 · 仅邀请</option>
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
              <img :src="assetUrl(form.image)" alt="图标预览" class="h-14 w-14 rounded-lg border ui-border ui-bg-soft object-cover" />
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

    <!-- 吧务团队 -->
    <dialog v-if="moderatorTarget" class="modal modal-open">
      <div class="modal-box max-w-lg">
        <div class="mb-4 flex items-center justify-between">
          <div>
            <h3 class="text-base font-semibold">吧务团队 · {{ moderatorTarget.title }}</h3>
            <p class="mt-0.5 text-xs ui-text-muted">吧务可置顶、加精、删除本话题帖子并修改标签</p>
          </div>
          <button class="btn btn-ghost btn-circle btn-xs" type="button" aria-label="关闭" @click="moderatorTarget = null"><X class="size-4" /></button>
        </div>

        <div v-if="moderatorLoading" class="empty-state py-6">加载中…</div>
        <div v-else-if="!moderators.length" class="empty-state py-6">该话题还没有吧务</div>
        <ul v-else class="space-y-2">
          <li v-for="item in moderators" :key="item.id" class="flex items-center gap-2 rounded-lg border ui-border p-2">
            <Avatar :src="item.headUrl" :name="item.userName" :size="28" />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm ui-text-strong">{{ item.userName || item.userCode }}</p>
              <p class="text-xs ui-text-muted">@{{ item.userCode }}</p>
            </div>
            <span class="badge badge-sm" :class="item.role === 'owner' ? 'badge-primary' : 'badge-ghost'">
              {{ item.role === 'owner' ? '吧主' : '小吧' }}
            </span>
            <button class="btn btn-ghost btn-xs text-rose-500" type="button" @click="removeModerator(item)">卸任</button>
          </li>
        </ul>

        <div class="mt-4 border-t ui-border pt-4">
          <span class="mb-2 block text-xs font-medium ui-text-base">任命新吧务</span>
          <div class="flex flex-wrap items-center gap-2">
            <input v-model="assignForm.userCode" class="input input-bordered input-sm min-w-0 flex-1" placeholder="用户登录账号，如 zhangsan" />
            <select v-model="assignForm.role" class="select select-bordered select-sm w-28">
              <option value="moderator">小吧</option>
              <option value="owner">吧主</option>
            </select>
            <button class="btn btn-primary btn-sm" type="button" @click="assignModerator">任命</button>
          </div>
        </div>
      </div>
    </dialog>
  </div>
</template>
