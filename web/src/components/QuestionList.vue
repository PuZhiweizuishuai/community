<script setup>
import { Eye, MessageCircle, ThumbsUp } from '@lucide/vue'
import Avatar from '@/components/Avatar.vue'
import { splitTags, shortNumber } from '@/utils/format'

/** 后端某些列表可能没带作者，缺数据时退化为纯文本，避免出现 /user/undefined */
const authorPath = (item) => (item.user?.userId ? `/user/${item.user.userId}` : null)

defineProps({
  items: { type: Array, default: () => [] },
  showStatus: { type: Boolean, default: false },
  emptyText: { type: String, default: '这里还什么问题都没有，去发起第一个讨论吧～' },
})
</script>

<template>
  <div>
    <div v-if="!items.length" class="surface px-6 py-14 text-center text-sm text-slate-400">
      {{ emptyText }}
    </div>
    <article
      v-for="item in items"
      :key="item.questionId"
      class="surface mb-3 p-4 transition hover:border-indigo-200 hover:shadow-sm"
    >
      <div class="flex items-start gap-3">
        <RouterLink v-if="authorPath(item)" :to="authorPath(item)" class="shrink-0">
          <Avatar :src="item.user?.headUrl" :name="item.user?.userName" :size="38" />
        </RouterLink>
        <Avatar v-else :src="item.user?.headUrl" :name="item.user?.userName" :size="38" />
        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-2">
            <span v-if="item.classification" class="badge badge-sm badge-outline border-indigo-200 text-indigo-600">
              {{ item.classification }}
            </span>
            <RouterLink
              :to="`/question/${item.questionId}`"
              class="line-clamp-1 flex-1 font-medium text-slate-800 hover:text-indigo-600"
            >
              {{ item.title }}
            </RouterLink>
            <span v-if="showStatus" class="badge badge-sm" :class="item.status === 1 ? 'badge-success' : 'badge-error'">
              {{ item.status === 1 ? '正常' : '已删除' }}
            </span>
          </div>

          <div class="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-400">
            <RouterLink v-if="authorPath(item)" :to="authorPath(item)" class="hover:text-indigo-600">
              {{ item.user?.userName || '匿名用户' }}
            </RouterLink>
            <span v-else>{{ item.user?.userName || '匿名用户' }}</span>
            <span>{{ item.alterTime || item.createTime || '刚刚' }}</span>
            <span class="flex items-center gap-1"><ThumbsUp class="size-3.5" /> {{ shortNumber(item.likeCount) }}</span>
            <span class="flex items-center gap-1"><MessageCircle class="size-3.5" /> {{ shortNumber(item.commentCount) }}</span>
            <span class="flex items-center gap-1"><Eye class="size-3.5" /> {{ shortNumber(item.viewCount) }}</span>
          </div>

          <div v-if="splitTags(item.tag).length" class="mt-2 flex flex-wrap gap-1.5">
            <RouterLink
              v-for="tag in splitTags(item.tag)"
              :key="tag"
              :to="{ name: 'home', query: { tag } }"
              class="badge badge-sm bg-slate-100 text-slate-500 hover:bg-indigo-50 hover:text-indigo-600"
            >
              # {{ tag }}
            </RouterLink>
          </div>
        </div>
      </div>
    </article>
  </div>
</template>
