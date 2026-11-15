<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronDown, Crown, RotateCcw, Search, ShieldCheck, Trash2, UserRound } from '@lucide/vue'
import { adminApi, questionApi } from '@/api'
import { useToastStore } from '@/stores/toast'
import { statusLabel, powerLabel } from '@/utils/format'
import Avatar from '@/components/Avatar.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()
const keyword = computed(() => route.query.search || '')
const type = computed(() => route.query.type || 'question')
const page = computed(() => route.query.page || '1')
const input = ref(keyword.value)
const data = ref(null)

watch(keyword, (v) => (input.value = v))

async function load() {
  data.value = await adminApi.search({ search: keyword.value, type: type.value, page: page.value, size: 20 })
}
watch([keyword, type, page], load, { immediate: true })

function go(patch) {
  router.push({ name: 'admin-search', query: { ...route.query, ...patch } })
}

async function remove(id) {
  await questionApi.remove(id)
  toast.success('已删除')
  await load()
}
async function recover(id) {
  await questionApi.recover(id)
  toast.success('已恢复')
  await load()
}
async function setPower(user, power) {
  await adminApi.updateUserPower(user.id, power)
  toast.success('权限已更新')
  await load()
}
</script>

<template>
  <div class="surface p-5">
    <form class="join mb-4 w-full max-w-lg" @submit.prevent="go({ search: input, page: 1 })">
      <input v-model="input" class="input input-bordered input-sm join-item flex-1" placeholder="搜索问题或用户" />
      <button class="btn btn-sm btn-primary join-item" type="submit"><Search class="size-4" />搜索</button>
    </form>

    <div class="tabs tabs-bordered mb-4">
      <button class="tab" :class="type === 'question' ? 'tab-active text-indigo-600' : ''" type="button" @click="go({ type: 'question', page: 1 })">问题</button>
      <button class="tab" :class="type === 'user' ? 'tab-active text-indigo-600' : ''" type="button" @click="go({ type: 'user', page: 1 })">用户</button>
    </div>

    <div v-if="!keyword" class="py-12 text-center text-sm text-slate-400">输入关键词开始搜索</div>

    <div v-else-if="type === 'question'" class="overflow-x-auto">
      <table class="table table-sm">
        <thead><tr><th>ID</th><th>标题</th><th>发起人</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in data?.questions?.data || []" :key="item.questionId">
            <td class="text-xs">{{ item.questionId }}</td>
            <td><RouterLink :to="`/question/${item.questionId}`" class="hover:text-indigo-600">{{ item.title }}</RouterLink></td>
            <td class="text-xs">{{ item.user?.userName }}</td>
            <td><span class="badge badge-sm" :class="item.status === 1 ? 'badge-success' : 'badge-error'">{{ statusLabel(item.status) }}</span></td>
            <td>
              <button v-if="item.status === 1" class="btn btn-ghost btn-xs text-rose-500" type="button" @click="remove(item.questionId)"><Trash2 class="size-3.5" />删除</button>
              <button v-else class="btn btn-ghost btn-xs text-emerald-600" type="button" @click="recover(item.questionId)"><RotateCcw class="size-3.5" />恢复</button>
            </td>
          </tr>
          <tr v-if="!data?.questions?.data?.length"><td colspan="5" class="text-center text-slate-400">没有匹配的问题</td></tr>
        </tbody>
      </table>
      <Pagination :pagination="data?.questions" @change="go({ page: $event })" />
    </div>

    <div v-else class="overflow-x-auto">
      <table class="table table-sm">
        <thead><tr><th>用户</th><th>账号</th><th>邮箱</th><th>权限</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="user in data?.users?.data || []" :key="user.id">
            <td><RouterLink :to="`/user/${user.userId}`" class="flex items-center gap-2"><Avatar :src="user.headUrl" :name="user.userName" :size="26" />{{ user.userName }}</RouterLink></td>
            <td>{{ user.userId }}</td>
            <td class="text-xs">{{ user.email }}</td>
            <td><span class="badge badge-sm badge-ghost">{{ powerLabel(user.power) }}</span></td>
            <td>
              <div class="dropdown">
                <button tabindex="0" class="btn btn-ghost btn-xs">权限 <ChevronDown class="size-3" /></button>
                <ul tabindex="0" class="menu dropdown-content menu-xs z-30 w-32 rounded-box border border-slate-200 bg-white p-2 shadow-lg">
                  <li><button type="button" @click="setPower(user, 0)"><ShieldCheck class="size-3.5" />管理员</button></li>
                  <li><button type="button" @click="setPower(user, 2)"><Crown class="size-3.5" />VIP</button></li>
                  <li><button type="button" @click="setPower(user, 1)"><UserRound class="size-3.5" />普通</button></li>
                </ul>
              </div>
            </td>
          </tr>
          <tr v-if="!data?.users?.data?.length"><td colspan="5" class="text-center text-slate-400">没有匹配的用户</td></tr>
        </tbody>
      </table>
      <Pagination :pagination="data?.users" @change="go({ page: $event })" />
    </div>
  </div>
</template>
