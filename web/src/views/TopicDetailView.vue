<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Pin, Star, Tags, Users } from '@lucide/vue'
import { assetUrl, discoverApi, interactionApi, moderationApi } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useMetaStore } from '@/stores/meta'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/Avatar.vue'
import QuestionList from '@/components/QuestionList.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()
const meta = useMetaStore()
const session = useSessionStore()
const toast = useToastStore()
const confirm = useConfirmStore()

const id = computed(() => route.params.id)
const page = computed(() => route.query.page || '1')
const topic = ref(null)
const moderators = ref([])
const isModerator = ref(false)
const followed = ref(false)
const pagination = ref(null)
const loading = ref(false)
const tagEditor = ref(null)
const tagInput = ref('')

async function loadTopic() {
  const data = await discoverApi.topic(id.value)
  topic.value = data?.tagClass || null
  moderators.value = data?.moderators || []
  isModerator.value = !!data?.isModerator
  followed.value = !!data?.followTag
}

async function loadQuestions() {
  loading.value = true
  try {
    pagination.value = await discoverApi.topicQuestions(id.value, { page: page.value, size: 15 })
  } finally {
    loading.value = false
  }
}

watch(
  id,
  () => {
    loadTopic()
    loadQuestions()
  },
  { immediate: true },
)
watch(page, loadQuestions)

function goPage(next) {
  if (Number(next) === Number(page.value)) return
  const query = { ...route.query }
  if (Number(next) === 1) delete query.page
  else query.page = next
  router.push({ name: 'topic-detail', params: { id: id.value }, query })
}

async function toggleFollow() {
  if (!session.loggedIn) {
    toast.warning('请先登录后再关注话题')
    router.push({ name: 'sign-in', query: { redirect: route.fullPath } })
    return
  }
  const result = await interactionApi.followTopic({ topicId: Number(id.value) })
  followed.value = !!result?.following
  toast.success(followed.value ? '关注成功' : '已取消关注')
  await loadTopic()
}

async function reload() {
  await loadTopic()
  await loadQuestions()
}

async function onModerate({ action, item }) {
  if (action === 'top') {
    await moderationApi.setTop(id.value, item.questionId, !item.isTop)
    toast.success(item.isTop ? '已取消置顶' : '已置顶')
  } else if (action === 'essence') {
    await moderationApi.setEssence(id.value, item.questionId, !item.isEssence)
    toast.success(item.isEssence ? '已取消加精' : '已加精')
  } else if (action === 'delete') {
    if (!(await confirm.ask({ title: '删除帖子', message: `确定删除「${item.title}」？`, confirmText: '删除', danger: true }))) return
    await moderationApi.remove(id.value, item.questionId)
    toast.success('帖子已删除')
  } else if (action === 'tags') {
    tagEditor.value = { questionId: item.questionId, title: item.title }
    tagInput.value = (item.tags || []).map((t) => t.name).join(', ')
    return
  }
  await reload()
}

async function saveTags() {
  if (!tagEditor.value) return
  const names = tagInput.value.split(/[,，]/).map((t) => t.trim()).filter(Boolean).slice(0, 5)
  await moderationApi.setTags(id.value, tagEditor.value.questionId, names)
  toast.success('标签已更新')
  tagEditor.value = null
  await reload()
}
</script>

<template>
  <div class="page-container">
    <div v-if="topic" class="surface mb-4 p-5">
      <div class="flex flex-wrap items-start gap-4">
        <img :src="assetUrl(topic.image)" :alt="topic.title" class="h-20 w-20 shrink-0 rounded-xl ui-bg-soft object-cover" />
        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-2">
            <h1 class="text-xl font-bold ui-text-strong">{{ topic.title }}</h1>
            <span class="badge badge-sm badge-ghost">{{ topic.followCount }} 人关注</span>
            <span class="badge badge-sm badge-ghost">{{ topic.talkCount }} 条讨论</span>
          </div>
          <p class="mt-1 text-sm ui-text-base">{{ topic.simpleDesc || '暂无话题简介' }}</p>
          <div v-if="moderators.length" class="mt-3 flex flex-wrap items-center gap-2 text-xs ui-text-muted">
            <Users class="size-3.5" />
            <span>吧务：</span>
            <RouterLink
              v-for="moderator in moderators"
              :key="moderator.id"
              :to="`/user/${moderator.userCode}`"
              class="flex items-center gap-1 hover:text-indigo-600"
            >
              <Avatar :src="moderator.headUrl" :name="moderator.userName" :size="20" />
              {{ moderator.userName }}
              <span v-if="moderator.role === 'owner'" class="badge badge-xs badge-primary">吧主</span>
            </RouterLink>
          </div>
        </div>
        <div class="flex shrink-0 items-center gap-2">
          <button class="btn btn-sm" :class="followed ? 'btn-outline' : 'btn-primary'" type="button" @click="toggleFollow">
            <Star class="size-4" />{{ followed ? '取消关注' : '关注话题' }}
          </button>
          <RouterLink
            v-if="session.canWrite('content:question:publish')"
            :to="{ name: 'publish', query: { topic: topic.id } }"
            class="btn btn-sm btn-outline"
          >
            发帖
          </RouterLink>
        </div>
      </div>
      <p v-if="isModerator" class="mt-3 flex items-center gap-1 text-xs ui-text-brand">
        <Tags class="size-3.5" />你当前是该话题吧务，可在帖子下方进行置顶、加精、改标签与删除操作。
      </p>
    </div>

    <div class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(260px,1fr)]">
      <div class="min-w-0">
        <div class="mb-3 flex items-center gap-2">
          <h2 class="section-title"><Pin class="size-4 ui-text-brand" />话题帖子</h2>
          <span class="text-xs ui-text-muted">置顶帖优先展示</span>
        </div>
        <div v-if="loading && !pagination" class="surface space-y-3 p-4">
          <div v-for="i in 5" :key="i" class="skeleton h-16 w-full rounded-xl" />
        </div>
        <QuestionList
          v-else
          :items="pagination?.data || []"
          :moderator="isModerator"
          empty-text="这个吧还没有帖子，来发第一帖吧～"
          @moderate="onModerate"
        />
        <Pagination :pagination="pagination" @change="goPage" />
      </div>

      <aside class="side-panel">
        <section class="surface p-4">
          <h3 class="section-title mb-3">关于本吧</h3>
          <p class="text-xs leading-relaxed ui-text-base">{{ topic?.simpleDesc || '暂无简介' }}</p>
          <dl class="mt-3 space-y-1.5 text-xs ui-text-base">
            <div class="flex justify-between"><dt>所属分类</dt><dd>{{ meta.topicTypes.find((t) => Number(t.value) === Number(topic?.type))?.label || '未分类' }}</dd></div>
            <div class="flex justify-between"><dt>关注人数</dt><dd>{{ topic?.followCount || 0 }}</dd></div>
            <div class="flex justify-between"><dt>讨论总数</dt><dd>{{ topic?.talkCount || 0 }}</dd></div>
          </dl>
        </section>
        <section v-if="moderators.length" class="surface p-4">
          <h3 class="section-title mb-3">吧务团队</h3>
          <ul class="space-y-2">
            <li v-for="moderator in moderators" :key="moderator.id">
              <RouterLink :to="`/user/${moderator.userCode}`" class="flex items-center gap-2 text-sm hover:text-indigo-600">
                <Avatar :src="moderator.headUrl" :name="moderator.userName" :size="28" />
                <span class="ui-text-strong">{{ moderator.userName }}</span>
                <span class="badge badge-xs" :class="moderator.role === 'owner' ? 'badge-primary' : 'badge-ghost'">
                  {{ moderator.role === 'owner' ? '吧主' : '小吧' }}
                </span>
              </RouterLink>
            </li>
          </ul>
        </section>
      </aside>
    </div>

    <dialog v-if="tagEditor" class="modal modal-open">
      <div class="modal-box max-w-md">
        <h3 class="text-base font-semibold">修改标签</h3>
        <p class="mt-1 text-xs ui-text-muted line-clamp-2">{{ tagEditor.title }}</p>
        <input v-model="tagInput" class="input input-bordered input-sm mt-4 w-full" placeholder="用逗号分隔，最多 5 个" />
        <p class="mt-2 text-xs ui-text-muted">新输入的标签会进入待审核状态，通过后才公开显示。</p>
        <div class="modal-action">
          <button class="btn btn-ghost btn-sm" type="button" @click="tagEditor = null">取消</button>
          <button class="btn btn-primary btn-sm" type="button" @click="saveTags">保存</button>
        </div>
      </div>
    </dialog>
  </div>
</template>
