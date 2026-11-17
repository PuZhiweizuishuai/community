<script setup>
import { Eye, Inbox, MessageCircle, ThumbsUp } from '@lucide/vue'
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
    <div v-if="!items.length" class="surface empty-state">
      <Inbox class="size-9" :stroke-width="1.5" />
      <span>{{ emptyText }}</span>
    </div>
    <article
      v-for="item in items"
      :key="item.questionId"
      class="surface mb-3 p-4 hover:border-indigo-200 sm:p-5"
    >
      <div class="flex items-start gap-3">
        <RouterLink v-if="authorPath(item)" :to="authorPath(item)" class="shrink-0">
          <Avatar :src="item.user?.headUrl" :name="item.user?.userName" :size="38" />
        </RouterLink>
        <Avatar v-else :src="item.user?.headUrl" :name="item.user?.userName" :size="38" />
        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-2">
            <span v-if="item.classification" class="badge badge-sm badge-primary badge-outline">
              {{ item.classification }}
            </span>
            <RouterLink
              :to="`/question/${item.questionId}`"
              class="line-clamp-2 flex-1 text-[15px] font-semibold ui-text-strong hover:text-indigo-600 sm:line-clamp-1"
            >
              {{ item.title }}
            </RouterLink>
            <span v-if="showStatus" class="badge badge-sm" :class="item.status === 1 ? 'badge-success' : 'badge-error'">
              {{ item.status === 1 ? '正常' : '已删除' }}
            </span>
          </div>

          <div class="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs ui-text-muted">
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
              class="chip"
            >
              # {{ tag }}
            </RouterLink>
          </div>
        </div>
      </div>
    </article>
  </div>
</template>
