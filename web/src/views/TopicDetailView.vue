<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Clock, Lock, Pin, Settings, Star, Tags, UserPlus, Users } from '@lucide/vue'
import { assetUrl, discoverApi, interactionApi, moderationApi } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useMetaStore } from '@/stores/meta'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/Avatar.vue'
import QuestionList from '@/components/QuestionList.vue'
import Pagination from '@/components/Pagination.vue'
import TopicManageDialog from '@/components/TopicManageDialog.vue'

const route = useRoute()
const router = useRouter()
const meta = useMetaStore()
const session = useSessionStore()
const toast = useToastStore()
const confirm = useConfirmStore()

const id = computed(() => route.params.id)
const page = computed(() => route.query.page || '1')
const classification = computed(() => route.query.class || '0')
const topic = ref(null)
const moderators = ref([])
const access = ref(null)
const pendingCount = ref(0)
const followed = ref(false)
const pagination = ref(null)
const loading = ref(false)
const tagEditor = ref(null)
const tagInput = ref('')
const manageOpen = ref(false)
const joinMessage = ref('')
const joining = ref(false)

const canViewContent = computed(() => !!access.value?.canViewContent)
const canManage = computed(
  () => !!access.value?.isOwner || !!access.value?.isModerator || !!access.value?.isAdmin,
)
const joinStatus = computed(() => access.value?.joinStatus || 'none')
const reviewStatus = computed(() => Number(topic.value?.reviewStatus ?? 1))
const visibilityLabel = computed(() => {
  const visibility = Number(topic.value?.visibility || 0)
  if (visibility === 2) return '私有 · 仅邀请'
  if (visibility === 1) return '私有 · 可申请'
  return '公开'
})

async function loadTopic() {
  const data = await discoverApi.topic(id.value)
  topic.value = data?.tagClass || null
  moderators.value = data?.moderators || []
  access.value = data?.access || null
  pendingCount.value = Number(data?.pendingApplicationCount || 0)
  followed.value = !!data?.followTag
}

async function loadQuestions() {
  if (!access.value?.canViewContent) {
    pagination.value = null
    return
  }
  loading.value = true
  try {
    pagination.value = await discoverApi.topicQuestions(id.value, {
      page: page.value,
      size: 15,
      class: classification.value,
    })
  } finally {
    loading.value = false
  }
}

async function reload() {
  await loadTopic()
  await loadQuestions()
}

watch(id, reload, { immediate: true })
watch([page, classification], loadQuestions)

function goPage(next) {
  if (Number(next) === Number(page.value)) return
  const query = { ...route.query }
  if (Number(next) === 1) delete query.page
  else query.page = next
  router.push({ name: 'topic-detail', params: { id: id.value }, query })
}

function requireLogin() {
  if (session.loggedIn) return true
  toast.warning('请先登录后继续操作')
  router.push({ name: 'sign-in', query: { redirect: route.fullPath } })
  return false
}

async function toggleFollow() {
  if (!requireLogin()) return
  const result = await interactionApi.followTopic({ topicId: Number(id.value) })
  followed.value = !!result?.following
  toast.success(followed.value ? '关注成功' : '已取消关注')
  await loadTopic()
}

async function applyJoin() {
  if (!requireLogin()) return
  joining.value = true
  try {
    await discoverApi.joinTopic(id.value, { message: joinMessage.value.trim() })
    joinMessage.value = ''
    toast.success('加入申请已提交，等待吧主团队审核')
    await reload()
  } finally {
    joining.value = false
  }
}

async function answerInvite(accept) {
  if (!requireLogin()) return
  if (accept) {
    await discoverApi.acceptTopicInvite(id.value)
    toast.success('已加入话题')
  } else {
    await discoverApi.declineTopicInvite(id.value)
    toast.success('已拒绝邀请')
  }
  await reload()
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
            <span class="badge badge-sm" :class="topic.visibility === 0 ? 'badge-ghost' : 'badge-warning'">
              {{ visibilityLabel }}
            </span>
            <span v-if="reviewStatus === 0" class="badge badge-sm badge-warning">待审核</span>
            <span v-else-if="reviewStatus === 2" class="badge badge-sm badge-error">审核未通过</span>
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
        <div class="flex shrink-0 flex-wrap items-center gap-2">
          <button
            v-if="canViewContent && reviewStatus === 1"
            class="btn btn-sm"
            :class="followed ? 'btn-outline' : 'btn-primary'"
            type="button"
            @click="toggleFollow"
          >
            <Star class="size-4" />{{ followed ? '取消关注' : '关注话题' }}
          </button>
          <RouterLink
            v-if="canViewContent && reviewStatus === 1 && session.canWrite('content:question:publish')"
            :to="{ name: 'publish', query: { topic: topic.id } }"
            class="btn btn-sm btn-outline"
          >
            发帖
          </RouterLink>
          <button
            v-if="canManage"
            class="btn btn-sm btn-outline"
            type="button"
            @click="manageOpen = true"
          >
            <Settings class="size-4" />话题管理
            <span v-if="pendingCount" class="badge badge-error badge-xs ml-1 text-white">{{ pendingCount }}</span>
          </button>
        </div>
      </div>

      <div
        v-if="reviewStatus === 0"
        class="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-700"
      >
        <Clock class="mr-1 inline size-3.5" />话题正在等待管理员审核，通过后将公开展示，并自动任命你为吧主。
      </div>
      <div
        v-else-if="reviewStatus === 2"
        class="mt-4 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-600"
      >
        话题未通过审核<span v-if="topic.reviewReason">：{{ topic.reviewReason }}</span>
      </div>

      <p v-if="canManage" class="mt-3 flex items-center gap-1 text-xs ui-text-brand">
        <Tags class="size-3.5" />你当前是该话题吧务，可在帖子下方进行置顶、加精、改标签与删除操作。
      </p>
    </div>

    <!-- 无内容权限：私有话题锁定面板 -->
    <section
      v-if="topic && access && !canViewContent"
      class="surface mb-4 p-8 text-center"
    >
      <Lock class="mx-auto size-8 ui-text-muted" />
      <h2 class="mt-3 text-base font-semibold ui-text-strong">
        {{ joinStatus === 'invited' ? '你收到了加入邀请' : '该话题为私有话题' }}
      </h2>

      <template v-if="!session.loggedIn">
        <p class="mt-1 text-sm ui-text-muted">登录后可查看话题详情并申请加入</p>
        <RouterLink :to="{ name: 'sign-in', query: { redirect: route.fullPath } }" class="btn btn-primary btn-sm mt-4">
          登录后查看
        </RouterLink>
      </template>

      <template v-else-if="joinStatus === 'invited'">
        <p class="mt-1 text-sm ui-text-muted">接受邀请后即可查看话题内容</p>
        <div class="mt-4 flex justify-center gap-2">
          <button class="btn btn-primary btn-sm" type="button" @click="answerInvite(true)">接受邀请</button>
          <button class="btn btn-ghost btn-sm" type="button" @click="answerInvite(false)">拒绝</button>
        </div>
      </template>

      <template v-else-if="joinStatus === 'pending'">
        <p class="mt-1 text-sm ui-text-muted">加入申请正在等待吧主团队审核</p>
      </template>

      <template v-else-if="joinStatus === 'rejected'">
        <p class="mt-1 text-sm ui-text-muted">上次申请未通过，你可以重新提交申请</p>
        <div class="mx-auto mt-4 flex max-w-md items-center gap-2">
          <input v-model="joinMessage" maxlength="200" class="input input-bordered input-sm min-w-0 flex-1" placeholder="补充申请说明（可选）" />
          <button class="btn btn-primary btn-sm" type="button" :disabled="joining" @click="applyJoin">重新申请</button>
        </div>
      </template>

      <template v-else-if="Number(topic.visibility) === 1">
        <p class="mt-1 text-sm ui-text-muted">本话题仅成员可查看内容，你可以申请加入，由吧主团队审核</p>
        <div class="mx-auto mt-4 flex max-w-md items-center gap-2">
          <input v-model="joinMessage" maxlength="200" class="input input-bordered input-sm min-w-0 flex-1" placeholder="补充申请说明（可选）" />
          <button class="btn btn-primary btn-sm" type="button" :disabled="joining" @click="applyJoin">
            <UserPlus class="size-4" />{{ joining ? '提交中…' : '申请加入' }}
          </button>
        </div>
      </template>

      <template v-else>
        <p class="mt-1 text-sm ui-text-muted">该话题仅限邀请加入，请联系吧主团队</p>
      </template>
    </section>

    <template v-if="canViewContent">
      <!-- 问题类型筛选：与首页一致 -->
      <div class="surface mb-4 flex flex-wrap items-center gap-2 px-3 py-2.5 sm:px-4 sm:py-3">
        <span class="mr-1 text-xs ui-text-muted">类型</span>
        <RouterLink
          v-for="item in meta.classifications"
          :key="item.value"
          class="btn btn-xs ui-text-base"
          :class="String(classification) === String(item.value) ? 'btn-primary' : 'btn-ghost'"
          :to="{ name: 'topic-detail', params: { id }, query: { class: item.value } }"
        >
          {{ item.label }}
        </RouterLink>
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
            :moderator="canManage"
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
              <div class="flex justify-between"><dt>可见性</dt><dd>{{ visibilityLabel }}</dd></div>
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
    </template>

    <TopicManageDialog
      v-if="manageOpen && topic && canManage"
      :topic="topic"
      :access="access"
      @close="manageOpen = false"
      @updated="reload"
    />

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
