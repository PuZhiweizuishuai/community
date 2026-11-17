<script setup>
import { computed, onMounted, ref } from 'vue'
import { Plus, RefreshCw, SquarePen, Trash2, TriangleAlert, X } from '@lucide/vue'
import { adminApi } from '@/api'
import { useMetaStore } from '@/stores/meta'
import { useToastStore } from '@/stores/toast'

const meta = useMetaStore()
const toast = useToastStore()

const kind = ref('question')
const list = ref([])
const loading = ref(false)
const editing = ref(null)
const saving = ref(false)
const deleteError = ref('')

const kinds = [
  { value: 'question', label: '问题分类', hint: '首页筛选与发帖时选择，例如 提问 / 讨论 / 分享' },
  { value: 'topic', label: '话题所属分类', hint: '话题的分组，例如 开发语言 / 平台框架' },
]
const current = computed(() => kinds.find((k) => k.value === kind.value))
const filtered = computed(() => list.value.filter((item) => item.kind === kind.value))

const emptyForm = () => ({
  id: 0,
  kind: kind.value,
  code: null,
  name: '',
  sortOrder: 0,
  status: 1,
})

async function load() {
  loading.value = true
  try {
    const result = await adminApi.classifications()
    list.value = result.list || []
  } finally {
    loading.value = false
  }
}
onMounted(load)

function switchKind(value) {
  kind.value = value
  deleteError.value = ''
}

function openCreate() {
  editing.value = emptyForm()
  deleteError.value = ''
}
function openEdit(item) {
  editing.value = { ...item }
  deleteError.value = ''
}

async function save() {
  if (!editing.value.name?.trim()) return toast.warning('请填写分类名称')
  saving.value = true
  try {
    await adminApi.saveClassification({ ...editing.value, kind: kind.value })
    toast.success(editing.value.id > 0 ? '分类已更新' : '分类已创建')
    editing.value = null
    await sync()
  } finally {
    saving.value = false
  }
}

async function remove(item) {
  deleteError.value = ''
  if (!window.confirm(`确定删除「${item.name}」分类？`)) return
  try {
    await adminApi.deleteClassification(item.id)
    toast.success('分类已删除')
    await sync()
  } catch (error) {
    // 后端会说明被多少问题/话题占用，这里在页面内保留完整提示
    deleteError.value = error?.message || '删除失败'
  }
}

async function sync() {
  await adminApi.refreshClassifications()
  await meta.load(true)
  await load()
}
</script>

<template>
  <div class="space-y-4">
    <div class="surface flex flex-wrap items-center gap-2 p-4">
      <div class="mr-auto">
        <h2 class="section-title">分类管理</h2>
        <p class="mt-1 text-xs ui-text-muted">问题分类与话题所属分类均可动态增删；删除前会校验是否仍有关联问题</p>
      </div>
      <button class="btn btn-ghost btn-sm" type="button" @click="sync"><RefreshCw class="size-4" />同步前台缓存</button>
      <button class="btn btn-primary btn-sm" type="button" @click="openCreate"><Plus class="size-4" />新增分类</button>
    </div>

    <div class="surface p-4">
      <div class="tabs tabs-bordered mb-4">
        <button
          v-for="item in kinds"
          :key="item.value"
          class="tab"
          :class="kind === item.value ? 'tab-active font-medium ui-text-brand' : ''"
          type="button"
          @click="switchKind(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
      <p class="mb-4 text-xs ui-text-muted">{{ current.hint }}</p>

      <div v-if="deleteError" class="alert alert-warning mb-4 py-2 text-sm">
        <TriangleAlert class="size-4 shrink-0" />
        <span>{{ deleteError }}</span>
        <button class="btn btn-ghost btn-xs ml-auto" type="button" @click="deleteError = ''">关闭</button>
      </div>

      <div v-if="loading && !list.length" class="empty-state">加载中…</div>
      <div v-else class="overflow-x-auto">
        <table class="table table-sm">
          <thead>
            <tr><th>编号</th><th>名称</th><th>排序</th><th>状态</th><th>关联问题</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in filtered" :key="item.id">
              <td class="font-mono text-xs">{{ item.code }}</td>
              <td class="font-medium ui-text-strong">{{ item.name }}</td>
              <td class="text-xs">{{ item.sortOrder }}</td>
              <td><span class="badge badge-sm" :class="item.status === 1 ? 'badge-success' : 'badge-ghost'">{{ item.status === 1 ? '启用' : '停用' }}</span></td>
              <td>
                <span class="badge badge-sm" :class="item.usageCount > 0 ? 'badge-outline badge-warning' : 'badge-ghost'">
                  {{ item.usageCount }} 条
                </span>
              </td>
              <td class="space-x-1">
                <button class="btn btn-ghost btn-xs" type="button" @click="openEdit(item)"><SquarePen class="size-3.5" />编辑</button>
                <button class="btn btn-ghost btn-xs text-rose-500" type="button" @click="remove(item)"><Trash2 class="size-3.5" />删除</button>
              </td>
            </tr>
            <tr v-if="!filtered.length"><td colspan="6" class="text-center ui-text-muted">暂无分类，点击右上角新增</td></tr>
          </tbody>
        </table>
      </div>

      <div class="mt-4 rounded-lg ui-bg-soft p-3 text-xs leading-relaxed ui-text-base">
        <b class="ui-text-strong">删除规则：</b>
        若分类下还有问题（问题分类按名称匹配，话题分类按该分类下话题的标签匹配），服务端会拒绝删除并提示数量；
        请先把这些问题改成其他分类或删除。话题分类下即使没有问题、但仍有话题时也需要先迁移话题，避免出现无归属分类。
      </div>
    </div>

    <dialog v-if="editing" class="modal modal-open">
      <div class="modal-box max-w-md">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-base font-semibold">{{ editing.id > 0 ? '编辑' : '新增' }}{{ current.label }}</h3>
          <button class="btn btn-ghost btn-circle btn-xs" type="button" aria-label="关闭" @click="editing = null"><X class="size-4" /></button>
        </div>

        <div class="space-y-3">
          <label class="form-control">
            <span class="label-text mb-1 font-medium">分类名称 *</span>
            <input v-model="editing.name" maxlength="50" class="input input-bordered input-sm" placeholder="例如 提问 / 开发语言" />
          </label>
          <div class="grid gap-3 sm:grid-cols-2">
            <label class="form-control">
              <span class="label-text mb-1 font-medium">编号</span>
              <input v-model.number="editing.code" type="number" min="1" class="input input-bordered input-sm" placeholder="留空自动分配" />
            </label>
            <label class="form-control">
              <span class="label-text mb-1 font-medium">排序值</span>
              <input v-model.number="editing.sortOrder" type="number" class="input input-bordered input-sm" />
            </label>
          </div>
          <label class="form-control">
            <span class="label-text mb-1 font-medium">状态</span>
            <select v-model.number="editing.status" class="select select-bordered select-sm">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </label>
          <p class="rounded-lg bg-amber-50 p-2 text-xs leading-relaxed text-amber-800">
            重命名问题分类时，历史帖子的分类会自动同步更新；修改话题分类编号时会同步更新该分类下所有话题的归属。
          </p>
        </div>

        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="editing = null">取消</button>
          <button class="btn btn-primary btn-sm" :disabled="saving" type="button" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </dialog>
  </div>
</template>
