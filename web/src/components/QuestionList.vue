<script setup>
import { Eye, Inbox, Lock, MessageCircle, Pin, Sparkles, ThumbsUp } from '@lucide/vue'
import Avatar from '@/components/Avatar.vue'
import { splitTags, shortNumber, timeAgo } from '@/utils/format'
import { useMetaStore } from '@/stores/meta'

/** 后端某些列表可能没带作者，缺数据时退化为纯文本，避免出现 /user/undefined */
const authorPath = (item) => (item.user?.userId ? `/user/${item.user.userId}` : null)

const props = defineProps({
  items: { type: Array, default: () => [] },
  showStatus: { type: Boolean, default: false },
  moderator: { type: Boolean, default: false },
  emptyText: { type: String, default: '这里还什么问题都没有，去发起第一个讨论吧～' },
})
const emit = defineEmits(['moderate'])

const meta = useMetaStore()

/** 优先使用后端按审核状态装配的标签，旧数据回退到冗余 tag 串 */
const tagsOf = (item) => {
  if (Array.isArray(item.tags) && item.tags.length) return item.tags.map((t) => t.name)
  return splitTags(item.tag)
}
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
          <!-- 标题独占一行，避免话题/类型等标签挤压标题 -->
          <div class="flex items-start gap-2">
            <RouterLink
              :to="`/question/${item.questionId}`"
              class="line-clamp-2 min-w-0 flex-1 text-[15px] font-semibold leading-snug ui-text-strong hover:text-indigo-600"
            >
              {{ item.title }}
            </RouterLink>
            <span v-if="showStatus" class="badge badge-sm shrink-0" :class="item.status === 1 ? 'badge-success' : 'badge-error'">
              {{ item.status === 1 ? '正常' : '已删除' }}
            </span>
          </div>

          <!-- 话题、类型、状态与互动数据统一放在标题下方，标题保持干净 -->
          <div class="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1.5 text-xs ui-text-muted">
            <RouterLink
              v-if="item.topicId"
              :to="`/topics/${item.topicId}`"
              class="inline-flex items-center rounded-full ui-bg-brand px-2.5 py-0.5 text-xs font-medium ui-text-brand hover:opacity-80"
            >
              {{ item.topicTitle || '话题' }}
            </RouterLink>
            <span v-if="item.classification" class="ui-text-base">
              {{ meta.classificationLabel(item.classification) }}
            </span>
            <span v-if="item.isTop" class="inline-flex items-center gap-0.5 font-medium text-amber-600">
              <Pin class="size-3" />置顶
            </span>
            <span v-if="item.isEssence" class="inline-flex items-center gap-0.5 font-medium text-emerald-600">
              <Sparkles class="size-3" />精华
            </span>
            <span v-if="item.visibility === 1" class="inline-flex items-center gap-0.5">
              <Lock class="size-3" />登录可见
            </span>
            <RouterLink v-if="authorPath(item)" :to="authorPath(item)" class="hover:text-indigo-600">
              {{ item.user?.userName || '匿名用户' }}
            </RouterLink>
            <span v-else>{{ item.user?.userName || '匿名用户' }}</span>
            <span>{{ timeAgo(item.alterTime || item.createTime) || '刚刚' }}</span>
            <span class="flex items-center gap-1"><ThumbsUp class="size-3.5" /> {{ shortNumber(item.likeCount) }}</span>
            <span class="flex items-center gap-1"><MessageCircle class="size-3.5" /> {{ shortNumber(item.commentCount) }}</span>
            <span class="flex items-center gap-1"><Eye class="size-3.5" /> {{ shortNumber(item.viewCount) }}</span>
          </div>

          <div v-if="tagsOf(item).length" class="mt-2 flex flex-wrap gap-1">
            <RouterLink
              v-for="tag in tagsOf(item)"
              :key="tag"
              :to="{ name: 'home', query: { tag } }"
              class="chip chip-xs"
            >
              # {{ tag }}
            </RouterLink>
          </div>

          <div v-if="moderator" class="mt-2 flex flex-wrap gap-1 border-t ui-border pt-2">
            <button class="btn btn-ghost btn-xs" type="button" @click="emit('moderate', { action: 'top', item })">
              {{ item.isTop ? '取消置顶' : '置顶' }}
            </button>
            <button class="btn btn-ghost btn-xs" type="button" @click="emit('moderate', { action: 'essence', item })">
              {{ item.isEssence ? '取消加精' : '加精' }}
            </button>
            <button class="btn btn-ghost btn-xs" type="button" @click="emit('moderate', { action: 'tags', item })">改标签</button>
            <button class="btn btn-ghost btn-xs text-rose-500" type="button" @click="emit('moderate', { action: 'delete', item })">删除</button>
          </div>
        </div>
      </div>
    </article>
  </div>
</template>
