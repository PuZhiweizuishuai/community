<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Ban, ChevronDown, Crown, History, KeyRound, LockOpen, ShieldCheck, UserRound, X } from '@lucide/vue'
import { adminApi } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/Avatar.vue'
import Pagination from '@/components/Pagination.vue'
import { formatDateTime, parseUserAgent } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()
const confirm = useConfirmStore()
const session = useSessionStore()
const page = computed(() => route.query.page || '1')
const data = ref(null)
const canAssignRoles = computed(() => session.can('admin:user:role'))
const canBan = computed(() => session.can('admin:user:ban'))

async function load() {
  data.value = await adminApi.users({ page: page.value, size: 20 })
}
watch(page, load, { immediate: true })

async function setPower(user, power) {
  await adminApi.updateUserPower(user.id, power)
  toast.success('用户等级已更新')
  await load()
}

// ---------------- 角色分配 ----------------
const roleTarget = ref(null)
const roleOptions = ref([])
const roleSelection = ref(new Set())
const roleLoading = ref(false)
const roleSaving = ref(false)

async function openRoles(user) {
  roleTarget.value = user
  roleLoading.value = true
  try {
    // 每次打开都拉取最新角色，避免刚新增的角色不在选项里
    roleOptions.value = (await adminApi.roleOptions()) || []
    const result = await adminApi.userRoles(user.id)
    roleSelection.value = new Set(result?.roleIds || [])
  } finally {
    roleLoading.value = false
  }
}

function toggleRole(roleId) {
  const next = new Set(roleSelection.value)
  if (next.has(roleId)) next.delete(roleId)
  else next.add(roleId)
  roleSelection.value = next
}

async function saveRoles() {
  if (!roleTarget.value || roleSaving.value) return
  roleSaving.value = true
  try {
    await adminApi.saveUserRoles(roleTarget.value.id, [...roleSelection.value])
    toast.success('角色已更新')
    roleTarget.value = null
    await load()
    // 被修改的可能就是当前登录账号，刷新后菜单与路由权限立即同步
    await session.refresh(true)
  } finally {
    roleSaving.value = false
  }
}

// ---------------- 封禁 / 解封 ----------------
const banTarget = ref(null)
const banForm = ref({ type: 1, days: 7, reason: '' })
const banSaving = ref(false)
const banDayPresets = [1, 3, 7, 30]

function isBannedUser(user) {
  if (user.banType === 2) return true
  return user.banType === 1 && Number(user.banEndTime || 0) > Date.now()
}

function isSuperAdmin(user) {
  return (user.roleCodes || []).includes('super_admin')
}

/** 不能封禁自己，也不能封禁超级管理员 */
function canBanUser(user) {
  return canBan.value && user.id !== session.user?.id && !isSuperAdmin(user)
}

function banEndText(user) {
  if (user.banType === 2) return '永久'
  if (!user.banEndTime) return ''
  return new Date(Number(user.banEndTime)).toLocaleString('zh-CN', { hour12: false })
}

const banPreviewEnd = computed(() => {
  if (banForm.value.type === 2) return '永久封禁'
  const days = Number(banForm.value.days) || 0
  if (days < 1) return '请设置封禁天数'
  return new Date(Date.now() + days * 24 * 3600 * 1000).toLocaleString('zh-CN', { hour12: false })
})

function openBan(user) {
  banTarget.value = user
  banForm.value = { type: 1, days: 7, reason: '' }
}

async function saveBan() {
  if (!banTarget.value || banSaving.value) return
  if (!banForm.value.reason.trim()) return toast.warning('请填写封禁原因')
  if (banForm.value.type === 1 && (!banForm.value.days || Number(banForm.value.days) < 1)) {
    return toast.warning('请设置临时封禁天数')
  }
  banSaving.value = true
  try {
    await adminApi.banUser(banTarget.value.id, {
      type: banForm.value.type,
      days: banForm.value.type === 1 ? Number(banForm.value.days) : null,
      reason: banForm.value.reason.trim(),
    })
    toast.success(banForm.value.type === 2 ? '已永久封禁该用户' : '已临时封禁该用户')
    banTarget.value = null
    await load()
  } finally {
    banSaving.value = false
  }
}

async function unban(user) {
  const ok = await confirm.ask({
    title: '解除封禁',
    message: `确定解除「${user.userName}」的封禁？解封后该用户可立即恢复写操作。`,
    confirmText: '解封',
  })
  if (!ok) return
  await adminApi.unbanUser(user.id)
  toast.success('已解除封禁')
  await load()
}
// ---------------- 登录历史 ----------------
const historyTarget = ref(null)
const historyData = ref(null)
const historyLoading = ref(false)
const historyPage = ref(1)

async function openHistory(user) {
  historyTarget.value = user
  historyData.value = null
  historyPage.value = 1
  await loadHistory()
}

async function loadHistory() {
  if (!historyTarget.value) return
  historyLoading.value = true
  try {
    historyData.value = await adminApi.userLoginHistory(historyTarget.value.id, {
      page: historyPage.value,
      size: 10,
    })
  } finally {
    historyLoading.value = false
  }
}

function changeHistoryPage(page) {
  historyPage.value = Number(page) || 1
  loadHistory()
}

const resetTarget = ref(null)
const resetting = ref(false)
const tempPassword = ref('')

async function confirmReset() {
  if (!resetTarget.value || resetting.value) return
  resetting.value = true
  try {
    const data = await adminApi.resetPassword(resetTarget.value.id)
    tempPassword.value = data?.tempPassword || ''
    resetTarget.value = null
    toast.success('密码已重置')
  } finally {
    resetting.value = false
  }
}
</script>

<template>
  <div class="surface overflow-x-auto p-5">
    <h2 class="mb-4 section-title">所有用户（{{ data?.allCount ?? 0 }}）</h2>
    <table class="table table-sm">
      <thead><tr><th>用户</th><th>账号</th><th>邮箱</th><th>注册时间</th><th>角色</th><th>等级</th><th>状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="user in data?.data || []" :key="user.id">
          <td>
            <RouterLink :to="`/user/${user.userId}`" class="flex items-center gap-2">
              <Avatar :src="user.headUrl" :name="user.userName" :size="28" />
              <span class="font-medium">{{ user.userName }}</span>
            </RouterLink>
          </td>
          <td>{{ user.userId }}</td>
          <td class="text-xs">{{ user.email }}</td>
          <td class="text-xs">{{ formatDateTime(user.creationTime) }}</td>
          <td>
            <div v-if="user.roleNames?.length" class="flex flex-wrap gap-1">
              <span v-for="name in user.roleNames" :key="name" class="badge badge-sm badge-outline">{{ name }}</span>
            </div>
            <span v-else class="text-xs ui-text-muted">未分配</span>
          </td>
          <td>
            <span class="badge badge-sm" :class="user.power === 2 ? 'badge-warning' : 'badge-ghost'">
              {{ user.power === 2 ? 'VIP' : '普通用户' }}
            </span>
          </td>
          <td>
            <span
              class="badge badge-sm"
              :class="isBannedUser(user) ? (user.banType === 2 ? 'badge-error' : 'badge-warning') : 'badge-success'"
            >
              {{ isBannedUser(user) ? (user.banType === 2 ? '永久封禁' : '临时封禁') : '正常' }}
            </span>
            <div v-if="isBannedUser(user)" class="mt-1 max-w-40 text-[11px] leading-tight ui-text-muted">
              <div v-if="user.banReason" class="truncate" :title="user.banReason">{{ user.banReason }}</div>
              <div v-if="user.banType === 1">至 {{ banEndText(user) }}</div>
            </div>
          </td>
          <td>
            <div class="dropdown">
              <button tabindex="0" class="btn btn-ghost btn-xs">操作 <ChevronDown class="size-3" /></button>
              <ul tabindex="0" class="menu dropdown-content menu-xs z-30 w-36 rounded-box border ui-border ui-bg p-2 shadow-lg">
                <li v-if="canAssignRoles"><button type="button" @click="openRoles(user)"><ShieldCheck class="size-3.5" />分配角色</button></li>
                <li><button type="button" @click="setPower(user, 2)"><Crown class="size-3.5" />设为 VIP</button></li>
                <li><button type="button" @click="setPower(user, 1)"><UserRound class="size-3.5" />设为普通用户</button></li>
                <li><button type="button" @click="openHistory(user)"><History class="size-3.5" />登录历史</button></li>
                <li v-if="canBanUser(user)">
                  <button v-if="isBannedUser(user)" type="button" @click="unban(user)"><LockOpen class="size-3.5" />解除封禁</button>
                  <button v-else type="button" class="text-rose-500" @click="openBan(user)"><Ban class="size-3.5" />封禁用户</button>
                </li>
                <li><button type="button" class="text-rose-500" @click="resetTarget = user"><KeyRound class="size-3.5" />重置密码</button></li>
              </ul>
            </div>
          </td>
        </tr>
        <tr v-if="!data?.data?.length"><td colspan="8" class="text-center ui-text-muted">暂无用户</td></tr>
      </tbody>
    </table>
    <Pagination :pagination="data" @change="router.push({ name: 'admin-users', query: { page: $event } })" />
  </div>

  <!-- 封禁用户 -->
  <dialog v-if="banTarget" class="modal modal-open">
    <div class="modal-box max-w-md">
      <div class="mb-4 flex items-center justify-between">
        <h3 class="text-base font-semibold">封禁用户</h3>
        <button class="btn btn-ghost btn-circle btn-xs" type="button" aria-label="关闭" @click="banTarget = null"><X class="size-4" /></button>
      </div>
      <p class="mb-3 text-sm">
        确定封禁 <b class="ui-text-strong">{{ banTarget.userName }}</b>？封禁后该用户无法发布、评论、上传或进行其他写操作。
      </p>
      <div class="space-y-3">
        <div class="form-control">
          <span class="label-text mb-1 font-medium">封禁类型</span>
          <div class="flex flex-wrap gap-4">
            <label class="flex cursor-pointer items-center gap-2 text-sm">
              <input v-model.number="banForm.type" type="radio" class="radio radio-sm" :value="1" />临时封禁
            </label>
            <label class="flex cursor-pointer items-center gap-2 text-sm">
              <input v-model.number="banForm.type" type="radio" class="radio radio-sm" :value="2" />永久封禁
            </label>
          </div>
        </div>
        <div v-if="banForm.type === 1" class="form-control">
          <span class="label-text mb-1 font-medium">封禁天数</span>
          <div class="flex flex-wrap items-center gap-2">
            <button
              v-for="day in banDayPresets"
              :key="day"
              type="button"
              class="btn btn-xs"
              :class="banForm.days === day ? 'btn-primary' : 'btn-outline'"
              @click="banForm.days = day"
            >
              {{ day }} 天
            </button>
            <input v-model.number="banForm.days" type="number" min="1" max="3650" class="input input-bordered input-sm w-24" />
          </div>
          <span class="mt-1 text-xs ui-text-muted">预计解封：{{ banPreviewEnd }}</span>
        </div>
        <label class="form-control">
          <span class="label-text mb-1 font-medium">封禁原因 *</span>
          <textarea
            v-model="banForm.reason"
            maxlength="255"
            rows="3"
            class="textarea textarea-bordered textarea-sm"
            placeholder="请填写封禁原因，用户登录时会看到该提示"
          />
        </label>
        <p class="rounded-lg ui-bg-soft p-2 text-xs leading-relaxed ui-text-muted">
          临时封禁到期后自动失效；永久封禁需管理员手动解封。封禁不会删除用户的角色与历史内容。
        </p>
      </div>
      <div class="modal-action">
        <button class="btn btn-ghost btn-sm" type="button" @click="banTarget = null">取消</button>
        <button class="btn btn-error btn-sm" :disabled="banSaving" type="button" @click="saveBan">
          {{ banSaving ? '封禁中…' : '确认封禁' }}
        </button>
      </div>
    </div>
  </dialog>

  <!-- 角色分配 -->
  <dialog v-if="roleTarget" class="modal modal-open">
    <div class="modal-box max-w-md">
      <div class="mb-4 flex items-center justify-between">
        <h3 class="text-base font-semibold">分配角色</h3>
        <button class="btn btn-ghost btn-circle btn-xs" type="button" aria-label="关闭" @click="roleTarget = null"><X class="size-4" /></button>
      </div>
      <p class="mb-3 text-sm">
        为 <b class="ui-text-strong">{{ roleTarget.userName }}</b> 分配角色，角色权限立即生效。
      </p>
      <div v-if="roleLoading" class="empty-state">正在加载角色…</div>
      <div v-else class="max-h-72 space-y-2 overflow-y-auto">
        <label
          v-for="role in roleOptions"
          :key="role.id"
          class="flex cursor-pointer items-start gap-2 rounded-lg border ui-border p-2.5 hover:bg-base-200/60"
        >
          <input
            type="checkbox"
            class="checkbox checkbox-sm mt-0.5"
            :checked="roleSelection.has(role.id)"
            :disabled="role.status !== 1"
            @change="toggleRole(role.id)"
          />
          <span class="min-w-0">
            <span class="block text-sm font-medium ui-text-strong">
              {{ role.name }}
              <span v-if="role.builtIn" class="badge badge-xs badge-warning ml-1">内置</span>
              <span v-if="role.status !== 1" class="badge badge-xs badge-ghost ml-1">停用</span>
            </span>
            <span class="block font-mono text-[11px] ui-text-muted">{{ role.code }}</span>
          </span>
        </label>
        <div v-if="!roleOptions.length" class="empty-state">暂无可分配角色</div>
      </div>
      <div class="modal-action">
        <button class="btn btn-ghost btn-sm" type="button" @click="roleTarget = null">取消</button>
        <button class="btn btn-primary btn-sm" :disabled="roleSaving || roleLoading" type="button" @click="saveRoles">
          {{ roleSaving ? '保存中…' : '保存' }}
        </button>
      </div>
    </div>
  </dialog>

  <!-- 登录历史 -->
  <dialog v-if="historyTarget" class="modal modal-open">
    <div class="modal-box max-w-3xl">
      <div class="mb-4 flex items-center justify-between">
        <div>
          <h3 class="text-base font-semibold">登录历史</h3>
          <p class="mt-0.5 text-xs ui-text-muted">{{ historyTarget.userName }} · {{ historyTarget.userId }}</p>
        </div>
        <button class="btn btn-ghost btn-circle btn-xs" type="button" aria-label="关闭" @click="historyTarget = null"><X class="size-4" /></button>
      </div>
      <div v-if="historyLoading && !historyData" class="empty-state">正在加载登录历史…</div>
      <div v-else class="overflow-x-auto">
        <table class="table table-sm">
          <thead><tr><th>登录时间</th><th>IP</th><th>入口</th><th>设备 / UA</th></tr></thead>
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
      <p class="mt-2 text-xs ui-text-muted">仅记录成功登录，历史保留 180 天。</p>
      <div class="modal-action">
        <button class="btn btn-primary btn-sm" type="button" @click="historyTarget = null">关闭</button>
      </div>
    </div>
  </dialog>

  <!-- 重置密码确认 -->
  <dialog v-if="resetTarget" class="modal modal-open">
    <div class="modal-box max-w-md">
      <div class="mb-4 flex items-center justify-between">
        <h3 class="text-base font-semibold">重置密码</h3>
        <button class="btn btn-ghost btn-circle btn-xs" type="button" aria-label="关闭" @click="resetTarget = null"><X class="size-4" /></button>
      </div>
      <p class="text-sm leading-relaxed">
        确定重置 <b class="ui-text-strong">{{ resetTarget.userName }}</b> 的密码吗？将生成随机临时密码，原密码立即失效。
      </p>
      <div class="modal-action">
        <button class="btn btn-ghost btn-sm" type="button" @click="resetTarget = null">取消</button>
        <button class="btn btn-error btn-sm" :disabled="resetting" type="button" @click="confirmReset">{{ resetting ? '重置中…' : '确认重置' }}</button>
      </div>
    </div>
  </dialog>

  <!-- 临时密码展示 -->
  <dialog v-if="tempPassword" class="modal modal-open">
    <div class="modal-box max-w-md">
      <div class="mb-4 flex items-center justify-between">
        <h3 class="text-base font-semibold">临时密码</h3>
        <button class="btn btn-ghost btn-circle btn-xs" type="button" aria-label="关闭" @click="tempPassword = ''"><X class="size-4" /></button>
      </div>
      <input :value="tempPassword" readonly class="input input-bordered w-full font-mono" @focus="$event.target.select()" />
      <p class="mt-3 text-xs ui-text-muted">请通知该用户登录后立即修改密码，关闭弹窗后将无法再次查看。</p>
      <div class="modal-action">
        <button class="btn btn-primary btn-sm" type="button" @click="tempPassword = ''">我已记录</button>
      </div>
    </div>
  </dialog>
</template>
