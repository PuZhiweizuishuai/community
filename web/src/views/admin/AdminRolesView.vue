<script setup>
import { computed, onMounted, ref } from 'vue'
import { CheckCheck, KeyRound, Plus, ShieldCheck, SquarePen, Trash2, Users, X } from '@lucide/vue'
import { adminApi } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
const confirm = useConfirmStore()
const session = useSessionStore()

const roles = ref([])
const groups = ref([])
const permissions = ref([])
const selectedRoleId = ref(0)
const checkedCodes = ref(new Set())
const loading = ref(false)
const loadingPermissions = ref(false)
const savingPerms = ref(false)
const editing = ref(null)
const saving = ref(false)
const deleteError = ref('')

const selectedRole = computed(() => roles.value.find((role) => role.id === selectedRoleId.value) || null)
const isBuiltIn = computed(() => !!selectedRole.value?.builtIn)
const isSuperAdminRole = computed(() => selectedRole.value?.code === 'super_admin')
// 内置默认角色不可删除，但权限可调整；只有超级管理员权限锁定
const canEditPermissions = computed(() => !!selectedRole.value && !isSuperAdminRole.value)

const emptyRole = () => ({
  id: 0,
  code: '',
  name: '',
  description: '',
  status: 1,
  sortOrder: 0,
})

async function load() {
  loading.value = true
  try {
    const data = await adminApi.roles()
    roles.value = data || []
    if (!roles.value.some((role) => role.id === selectedRoleId.value)) {
      selectedRoleId.value = roles.value[0]?.id || 0
    }
    await loadRolePermissions()
  } finally {
    loading.value = false
  }
}

async function loadPermissions() {
  const data = await adminApi.permissions()
  permissions.value = data?.list || []
  groups.value = data?.groups || []
}

async function loadRolePermissions() {
  if (!selectedRoleId.value) {
    checkedCodes.value = new Set()
    return
  }
  loadingPermissions.value = true
  try {
    const data = await adminApi.rolePermissionIds(selectedRoleId.value)
    const ids = new Set(data?.permissionIds || [])
    checkedCodes.value = new Set(permissions.value.filter((item) => ids.has(item.id)).map((item) => item.code))
  } finally {
    loadingPermissions.value = false
  }
}

async function selectRole(role) {
  if (selectedRoleId.value === role.id) return
  selectedRoleId.value = role.id
  await loadRolePermissions()
}

function has(code) {
  return checkedCodes.value.has(code)
}

function setCodes(next) {
  checkedCodes.value = new Set(next)
}

function togglePermission(permission) {
  if (!canEditPermissions.value) return
  const next = new Set(checkedCodes.value)
  if (next.has(permission.code)) {
    if (permission.code === 'admin:access') {
      // 取消后台访问：移除所有后台权限，保留社区权限（content:*/file:*）
      ;[...next].forEach((code) => {
        if (code.startsWith('admin:')) next.delete(code)
      })
    } else {
      next.delete(permission.code)
    }
  } else {
    next.add(permission.code)
    // 勾选任意后台权限时自动补上后台访问；社区权限不需要后台身份
    if (permission.code.startsWith('admin:')) next.add('admin:access')
  }
  setCodes(next)
}

function groupChecked(group) {
  return group.items.every((item) => checkedCodes.value.has(item.code))
}

function toggleGroup(group) {
  if (!canEditPermissions.value) return
  const next = new Set(checkedCodes.value)
  if (groupChecked(group)) {
    group.items.forEach((item) => next.delete(item.code))
    // 取消后台访问时同步清理所有后台权限，社区权限不受影响
    if (group.items.some((item) => item.code === 'admin:access')) {
      ;[...next].forEach((code) => {
        if (code.startsWith('admin:')) next.delete(code)
      })
    }
  } else {
    group.items.forEach((item) => next.add(item.code))
    // 本组包含后台权限时自动补上后台访问
    if (group.items.some((item) => item.code.startsWith('admin:'))) next.add('admin:access')
  }
  setCodes(next)
}

async function savePermissions() {
  if (!selectedRole.value || !canEditPermissions.value) return
  savingPerms.value = true
  try {
    const permissionIds = permissions.value
      .filter((item) => checkedCodes.value.has(item.code))
      .map((item) => item.id)
    await adminApi.saveRolePermissions(selectedRole.value.id, permissionIds)
    toast.success('角色权限已保存')
    await load()
    // 当前登录账号可能就在这个角色里，刷新后菜单立即跟随权限变化
    await session.refresh(true)
  } finally {
    savingPerms.value = false
  }
}

function openCreate() {
  deleteError.value = ''
  editing.value = emptyRole()
}

function openEdit(role) {
  deleteError.value = ''
  editing.value = { ...role }
}

async function saveRole() {
  if (!editing.value?.name?.trim()) return toast.warning('请填写角色名称')
  if (!editing.value.id && !editing.value.code?.trim()) return toast.warning('请填写角色编码')
  saving.value = true
  try {
    const saved = await adminApi.saveRole({
      id: editing.value.id || null,
      code: editing.value.code,
      name: editing.value.name,
      description: editing.value.description,
      status: editing.value.status,
      sortOrder: editing.value.sortOrder,
    })
    toast.success(editing.value.id ? '角色已更新' : '角色已创建')
    editing.value = null
    await load()
    if (saved?.id) {
      selectedRoleId.value = saved.id
      await loadRolePermissions()
    }
  } finally {
    saving.value = false
  }
}

async function removeRole(role) {
  deleteError.value = ''
  if (role.builtIn) return
  const ok = await confirm.ask({
    title: '删除角色',
    message: `确定删除角色「${role.name}」？该角色下 ${role.userCount} 名用户会立即失去对应权限。`,
    confirmText: '删除',
    danger: true,
  })
  if (!ok) return
  try {
    await adminApi.deleteRole(role.id)
    toast.success('角色已删除')
    selectedRoleId.value = 0
    await load()
    await session.refresh(true)
  } catch (error) {
    deleteError.value = error?.message || '删除失败'
  }
}

onMounted(async () => {
  await loadPermissions()
  await load()
})
</script>

<template>
  <div class="space-y-4">
    <div class="surface flex flex-wrap items-center gap-2 p-4">
      <div class="mr-auto">
        <h2 class="section-title">角色权限</h2>
        <p class="mt-1 text-xs ui-text-muted">
          按角色动态配置后台与社区权限；默认角色不可删除但可调整权限，超级管理员权限固定
        </p>
      </div>
      <button class="btn btn-primary btn-sm" type="button" @click="openCreate">
        <Plus class="size-4" />新增角色
      </button>
    </div>

    <div v-if="deleteError" class="alert alert-warning py-2 text-sm">
      <span>{{ deleteError }}</span>
      <button class="btn btn-ghost btn-xs ml-auto" type="button" @click="deleteError = ''">关闭</button>
    </div>

    <div class="grid gap-4 lg:grid-cols-[18rem_minmax(0,1fr)]">
      <!-- 角色列表 -->
      <div class="surface p-3">
        <div class="mb-2 flex items-center justify-between px-1">
          <span class="text-xs font-medium ui-text-muted">全部角色（{{ roles.length }}）</span>
          <span v-if="loading" class="text-xs ui-text-muted">加载中…</span>
        </div>
        <div class="space-y-2">
          <button
            v-for="role in roles"
            :key="role.id"
            type="button"
            class="w-full rounded-lg border p-3 text-left transition"
            :class="selectedRoleId === role.id ? 'border-indigo-400 ui-bg-brand' : 'ui-border hover:bg-base-200/60'"
            @click="selectRole(role)"
          >
            <div class="flex items-center gap-2">
              <KeyRound class="size-4 shrink-0 ui-text-brand" />
              <span class="truncate text-sm font-medium ui-text-strong">{{ role.name }}</span>
              <span v-if="role.builtIn" class="badge badge-xs badge-warning">内置</span>
              <span v-if="role.status !== 1" class="badge badge-xs badge-ghost">停用</span>
            </div>
            <div class="mt-1 truncate font-mono text-[11px] ui-text-muted">{{ role.code }}</div>
            <div class="mt-2 flex items-center gap-3 text-xs ui-text-muted">
              <span class="inline-flex items-center gap-1"><ShieldCheck class="size-3" />{{ role.permissionCount }} 项权限</span>
              <span class="inline-flex items-center gap-1"><Users class="size-3" />{{ role.userCount }} 人</span>
            </div>
          </button>
          <div v-if="!roles.length && !loading" class="empty-state">暂无角色</div>
        </div>
      </div>

      <!-- 权限矩阵 -->
      <div class="surface p-4">
        <div v-if="!selectedRole" class="empty-state">请选择一个角色配置权限</div>
        <template v-else>
          <div class="mb-4 flex flex-wrap items-start justify-between gap-2">
            <div>
              <h3 class="flex items-center gap-2 text-base font-semibold ui-text-strong">
                {{ selectedRole.name }}
                <span v-if="selectedRole.builtIn" class="badge badge-sm badge-warning">内置角色</span>
              </h3>
              <p class="mt-1 text-xs ui-text-muted">
                {{ selectedRole.description || '暂无角色说明' }}
              </p>
            </div>
            <div class="flex items-center gap-1">
              <button class="btn btn-ghost btn-sm" type="button" @click="openEdit(selectedRole)">
                <SquarePen class="size-4" />编辑角色
              </button>
              <button
                class="btn btn-ghost btn-sm text-rose-500"
                type="button"
                :disabled="selectedRole.builtIn"
                :title="selectedRole.builtIn ? '内置角色不可删除' : '删除角色'"
                @click="removeRole(selectedRole)"
              >
                <Trash2 class="size-4" />删除
              </button>
            </div>
          </div>

          <div v-if="isSuperAdminRole" class="alert mb-4 py-2 text-sm">
            <ShieldCheck class="size-4 shrink-0" />
            <span>超级管理员拥有全部权限且不可修改，保证系统始终有人可以管理后台。</span>
          </div>
          <div v-else-if="isBuiltIn" class="alert mb-4 py-2 text-sm">
            <ShieldCheck class="size-4 shrink-0" />
            <span>这是内置默认角色：不可删除，但可以调整其权限；调整会影响所有拥有该角色的用户。</span>
          </div>

          <p class="mb-3 text-xs ui-text-muted">
            后台权限（admin:*）会自动包含「后台访问」；社区权限（发布、评论、上传）不要求后台身份。
          </p>

          <div v-if="loadingPermissions" class="empty-state">正在加载权限…</div>
          <div v-else class="space-y-3">
            <div v-for="group in groups" :key="group.name" class="rounded-lg border ui-border p-3">
              <div class="flex items-center justify-between">
                <span class="text-sm font-semibold ui-text-strong">{{ group.name }}</span>
                <button
                  class="btn btn-ghost btn-xs"
                  type="button"
                  :disabled="!canEditPermissions"
                  @click="toggleGroup(group)"
                >
                  <CheckCheck class="size-3.5" />{{ groupChecked(group) ? '取消本组' : '全选本组' }}
                </button>
              </div>
              <div class="mt-2 grid gap-2 sm:grid-cols-2">
                <label
                  v-for="permission in group.items"
                  :key="permission.id"
                  class="flex cursor-pointer items-start gap-2 rounded-md p-1.5 hover:bg-base-200/60"
                  :class="permission.code === 'admin:access' ? 'opacity-100' : ''"
                >
                  <input
                    type="checkbox"
                    class="checkbox checkbox-sm mt-0.5"
                    :checked="has(permission.code)"
                    :disabled="!canEditPermissions"
                    @change="togglePermission(permission)"
                  />
                  <span class="min-w-0">
                    <span class="block text-sm font-medium ui-text-strong">
                      {{ permission.name }}
                      <span v-if="permission.code === 'admin:access'" class="badge badge-xs badge-info ml-1">基础</span>
                    </span>
                    <span class="block text-xs leading-relaxed ui-text-muted">{{ permission.description }}</span>
                    <span class="block font-mono text-[10px] ui-text-muted">{{ permission.code }}</span>
                  </span>
                </label>
              </div>
            </div>
          </div>

          <div class="mt-4 flex justify-end">
            <button
              class="btn btn-primary btn-sm"
              type="button"
              :disabled="!canEditPermissions || savingPerms || loadingPermissions"
              @click="savePermissions"
            >
              {{ savingPerms ? '保存中…' : '保存权限' }}
            </button>
          </div>
        </template>
      </div>
    </div>

    <!-- 新增 / 编辑角色 -->
    <dialog v-if="editing" class="modal modal-open">
      <div class="modal-box max-w-md">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-base font-semibold">{{ editing.id ? '编辑角色' : '新增角色' }}</h3>
          <button class="btn btn-ghost btn-circle btn-xs" type="button" aria-label="关闭" @click="editing = null">
            <X class="size-4" />
          </button>
        </div>
        <div class="space-y-3">
          <label class="form-control">
            <span class="label-text mb-1 font-medium">角色名称 *</span>
            <input v-model="editing.name" maxlength="64" class="input input-bordered input-sm" placeholder="例如 内容审核员" />
          </label>
          <label class="form-control">
            <span class="label-text mb-1 font-medium">角色编码 *</span>
            <input
              v-model="editing.code"
              class="input input-bordered input-sm font-mono"
              :readonly="!!editing.id"
              maxlength="32"
              placeholder="例如 content_admin"
            />
            <span class="mt-1 text-xs ui-text-muted">小写字母开头，仅含小写字母、数字和下划线，创建后不可修改</span>
          </label>
          <label class="form-control">
            <span class="label-text mb-1 font-medium">角色说明</span>
            <textarea v-model="editing.description" maxlength="255" class="textarea textarea-bordered textarea-sm" rows="2" />
          </label>
          <div class="grid gap-3 sm:grid-cols-2">
            <label class="form-control">
              <span class="label-text mb-1 font-medium">排序值</span>
              <input v-model.number="editing.sortOrder" type="number" class="input input-bordered input-sm" />
            </label>
            <label class="form-control">
              <span class="label-text mb-1 font-medium">状态</span>
              <select v-model.number="editing.status" class="select select-bordered select-sm" :disabled="!!editing.builtIn">
                <option :value="1">启用</option>
                <option :value="0">停用</option>
              </select>
            </label>
          </div>
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="editing = null">取消</button>
          <button class="btn btn-primary btn-sm" :disabled="saving" type="button" @click="saveRole">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </dialog>
  </div>
</template>
