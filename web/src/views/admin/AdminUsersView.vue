<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronDown, Crown, KeyRound, ShieldCheck, UserRound, X } from '@lucide/vue'
import { adminApi } from '@/api'
import { useToastStore } from '@/stores/toast'
import { powerLabel } from '@/utils/format'
import Avatar from '@/components/Avatar.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()
const page = computed(() => route.query.page || '1')
const data = ref(null)

async function load() {
  data.value = await adminApi.users({ page: page.value, size: 20 })
}
watch(page, load, { immediate: true })

async function setPower(user, power) {
  await adminApi.updateUserPower(user.id, power)
  toast.success('权限已更新')
  await load()
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
      <thead><tr><th>用户</th><th>账号</th><th>邮箱</th><th>注册时间</th><th>权限</th><th>操作</th></tr></thead>
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
          <td class="text-xs">{{ user.creationTime }}</td>
          <td><span class="badge badge-sm" :class="user.power === 0 ? 'badge-error' : user.power === 2 ? 'badge-warning' : 'badge-ghost'">{{ powerLabel(user.power) }}</span></td>
          <td>
            <div class="dropdown">
              <button tabindex="0" class="btn btn-ghost btn-xs">操作 <ChevronDown class="size-3" /></button>
              <ul tabindex="0" class="menu dropdown-content menu-xs z-30 w-36 rounded-box border ui-border ui-bg p-2 shadow-lg">
                <li><button type="button" @click="setPower(user, 0)"><ShieldCheck class="size-3.5" />设为管理员</button></li>
                <li><button type="button" @click="setPower(user, 2)"><Crown class="size-3.5" />设为 VIP</button></li>
                <li><button type="button" @click="setPower(user, 1)"><UserRound class="size-3.5" />设为普通用户</button></li>
                <li><button type="button" class="text-rose-500" @click="resetTarget = user"><KeyRound class="size-3.5" />重置密码</button></li>
              </ul>
            </div>
          </td>
        </tr>
        <tr v-if="!data?.data?.length"><td colspan="6" class="text-center ui-text-muted">暂无用户</td></tr>
      </tbody>
    </table>
    <Pagination :pagination="data" @change="router.push({ name: 'admin-users', query: { page: $event } })" />
  </div>

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
