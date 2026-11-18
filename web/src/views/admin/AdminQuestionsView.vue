<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { RotateCcw, Trash2 } from '@lucide/vue'
import { adminApi, questionApi } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useMetaStore } from '@/stores/meta'
import { useToastStore } from '@/stores/toast'
import { statusLabel } from '@/utils/format'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()
const confirm = useConfirmStore()
const meta = useMetaStore()
const page = computed(() => route.query.page || '1')
const data = ref(null)

async function load() {
  data.value = await adminApi.questions({ page: page.value, size: 20 })
}
watch(page, load, { immediate: true })

async function remove(id) {
  if (!(await confirm.ask({ title: '删除问题', message: '确定删除该问题？', confirmText: '删除', danger: true }))) return
  await questionApi.remove(id)
  toast.success('已删除')
  await load()
}
async function recover(id) {
  await questionApi.recover(id)
  toast.success('已恢复')
  await load()
}
</script>

<template>
  <div class="surface overflow-x-auto p-5">
    <h2 class="mb-4 section-title">所有问题（{{ data?.allCount ?? 0 }}）</h2>
    <table class="table table-sm">
      <thead><tr><th>ID</th><th>标题</th><th>分类</th><th>发起人</th><th>发布时间</th><th>状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="item in data?.data || []" :key="item.questionId">
          <td class="text-xs">{{ item.questionId }}</td>
          <td><RouterLink :to="`/question/${item.questionId}`" class="line-clamp-1 max-w-xs hover:text-indigo-600">{{ item.title }}</RouterLink></td>
          <td><span class="badge badge-sm badge-outline">{{ meta.classificationLabel(item.classification) }}</span></td>
          <td><RouterLink :to="`/user/${item.user?.userId}`" class="text-xs hover:text-indigo-600">{{ item.user?.userName }}</RouterLink></td>
          <td class="text-xs">{{ item.createTime }}</td>
          <td><span class="badge badge-sm" :class="item.status === 1 ? 'badge-success' : 'badge-error'">{{ statusLabel(item.status) }}</span></td>
          <td>
            <button v-if="item.status === 1" class="btn btn-ghost btn-xs text-rose-500" type="button" @click="remove(item.questionId)"><Trash2 class="size-3.5" />删除</button>
            <button v-else class="btn btn-ghost btn-xs text-emerald-600" type="button" @click="recover(item.questionId)"><RotateCcw class="size-3.5" />恢复</button>
          </td>
        </tr>
        <tr v-if="!data?.data?.length"><td colspan="7" class="text-center ui-text-muted">暂无问题</td></tr>
      </tbody>
    </table>
    <Pagination :pagination="data" @change="router.push({ name: 'admin-questions', query: { page: $event } })" />
  </div>
</template>
