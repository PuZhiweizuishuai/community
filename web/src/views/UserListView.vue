<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Flame, UserRoundPlus } from '@lucide/vue'
import { discoverApi } from '@/api'
import { useMetaStore } from '@/stores/meta'
import Avatar from '@/components/Avatar.vue'
import Pagination from '@/components/Pagination.vue'
import SidePanel from '@/components/SidePanel.vue'

const route = useRoute()
const router = useRouter()
const meta = useMetaStore()
const page = computed(() => route.query.page || '1')
const data = ref(null)

async function load() {
  data.value = await discoverApi.userList({ page: page.value, size: 10, class: route.query.class || '0' })
}
watch([page, () => route.query.class], load, { immediate: true })
</script>

<template>
  <div class="page-container">
    <div class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(260px,1fr)]">
      <div class="min-w-0">
        <section v-if="page === '1' && meta.hotUsers.length" class="surface mb-5 p-4">
          <h2 class="mb-3 flex items-center gap-1.5 text-base font-semibold text-slate-800"><Flame class="size-4 text-orange-500" />活跃用户</h2>
          <div class="grid gap-3 sm:grid-cols-2">
            <RouterLink v-for="user in meta.hotUsers.slice(0, 6)" :key="user.userId" :to="`/user/${user.userId}`" class="flex items-center gap-3 rounded-lg border border-slate-100 p-3 hover:border-indigo-200">
              <Avatar :src="user.headUrl" :name="user.userName" :size="40" />
              <div class="min-w-0">
                <p class="font-medium text-slate-800">{{ user.userName }}</p>
                <p class="line-clamp-1 text-xs text-slate-400">{{ user.simpleSelfIntroduction || '暂无签名' }}</p>
              </div>
            </RouterLink>
          </div>
        </section>

        <h2 class="mb-3 flex items-center gap-1.5 text-base font-semibold text-slate-800"><UserRoundPlus class="size-4 text-indigo-500" />最新加入</h2>
        <div class="grid gap-3 sm:grid-cols-2">
          <RouterLink v-for="user in data?.users?.data || []" :key="user.userId" :to="`/user/${user.userId}`" class="surface flex items-center gap-3 p-4 hover:border-indigo-200">
            <Avatar :src="user.headUrl" :name="user.userName" :size="44" />
            <div class="min-w-0">
              <p class="font-medium text-slate-800">{{ user.userName }}</p>
              <p class="line-clamp-1 text-xs text-slate-400">{{ user.simpleSelfIntroduction || '这个人很懒，什么都没写' }}</p>
            </div>
          </RouterLink>
        </div>
        <Pagination :pagination="data?.users" @change="router.push({ name: 'user-list', query: { page: $event } })" />
      </div>

      <SidePanel position="user" />
    </div>
  </div>
</template>
