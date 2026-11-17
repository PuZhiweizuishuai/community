<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Heart, MessageSquareText, SquarePen, Star, Tags, Trophy, UserMinus, UserPlus, Users } from '@lucide/vue'
import { discoverApi, interactionApi, assetUrl } from '@/api'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/Avatar.vue'
import QuestionList from '@/components/QuestionList.vue'
import Pagination from '@/components/Pagination.vue'
import AdCarousel from '@/components/AdCarousel.vue'
import { useMetaStore } from '@/stores/meta'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const toast = useToastStore()
const meta = useMetaStore()

const userId = computed(() => route.params.userId)
const tab = computed(() => route.query.tab || 'posts')
const page = computed(() => route.query.page || '1')
const data = ref(null)
const followed = ref(false)
const showDetail = ref(false)

const tabs = [
  { value: 'posts', label: 'TA 的问题', icon: MessageSquareText },
  { value: 'questions', label: '关注的问题', icon: Star },
  { value: 'topic', label: '关注的话题', icon: Tags },
  { value: 'user', label: '关注的人', icon: Users },
  { value: 'fans', label: '粉丝', icon: Heart },
]

async function load() {
  data.value = await discoverApi.userHome(userId.value, { page: page.value, size: 15, tab: tab.value })
  followed.value = !!data.value.isFollowUser
}
watch([userId, tab, page], load, { immediate: true })

function go(patch) {
  router.push({ name: 'user', params: { userId: userId.value }, query: { ...route.query, ...patch } })
}

async function toggleFollow() {
  if (!session.loggedIn) {
    toast.warning('请先登录')
    return router.push({ name: 'sign-in', query: { redirect: route.fullPath } })
  }
  const result = await interactionApi.followUser({ followUserId: data.value.user.id })
  followed.value = result.following
  toast.success(followed.value ? '关注成功' : '已取消关注')
}

const items = computed(() => data.value?.pagination?.data || [])
const user = computed(() => data.value?.user || null)
const ads = computed(() => meta.adsFor('userHome'))
meta.loadAds('userHome')
</script>

<template>
  <div class="page-container">
    <div v-if="user" class="surface mb-5 overflow-hidden">
      <div class="h-32 bg-gradient-to-r from-indigo-500 to-violet-400 sm:h-40" :style="user.userTopPhotoUrl ? { backgroundImage: `url(${assetUrl(user.userTopPhotoUrl)})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}"></div>
      <div class="flex flex-wrap items-end gap-4 px-5 pb-5 -mt-9">
        <Avatar :src="user.headUrl" :name="user.userName" :size="76" />
        <div class="min-w-0 flex-1 pt-9">
          <h1 class="text-xl font-bold ui-text-strong">{{ user.userName }}</h1>
          <p class="line-clamp-1 text-sm ui-text-base">{{ user.simpleSelfIntroduction || '这个人很懒，什么都没写～' }}</p>
          <p class="mt-1 text-xs ui-text-muted">
            {{ user.school || '未填写学校' }} · {{ user.major || '未填写专业' }} · {{ user.sex || '保密' }}
          </p>
        </div>
        <div class="flex gap-2">
          <button v-if="showDetail" class="btn btn-ghost btn-sm" type="button" @click="showDetail = !showDetail">收起资料</button>
          <button v-else class="btn btn-ghost btn-sm" type="button" @click="showDetail = !showDetail">详细资料</button>
          <RouterLink v-if="data.isSelf" to="/settings" class="btn btn-outline btn-sm"><SquarePen class="size-4" />编辑资料</RouterLink>
          <button v-else class="btn btn-sm" :class="followed ? 'btn-outline' : 'btn-primary'" type="button" @click="toggleFollow">
            <component :is="followed ? UserMinus : UserPlus" class="size-4" />
            {{ followed ? '取消关注' : '关注 TA' }}
          </button>
        </div>
      </div>
      <div v-if="showDetail" class="border-t ui-border px-5 py-4 text-sm ui-text-base">
        <p><b>自我介绍：</b>{{ user.selfIntroduction || '暂无' }}</p>
        <p class="mt-1"><b>爱好：</b>{{ user.likes || '暂无' }}</p>
        <p class="mt-1"><b>注册时间：</b>{{ user.creationTime || '未知' }}</p>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(260px,1fr)]">
      <div class="min-w-0">
        <div class="surface mb-4 flex flex-wrap gap-1 overflow-x-auto p-2">
          <button
            v-for="item in tabs"
            :key="item.value"
            class="btn btn-sm shrink-0"
            :class="tab === item.value ? 'btn-primary' : 'btn-ghost'"
            type="button"
            @click="go({ tab: item.value, page: 1 })"
          >
            <component :is="item.icon" class="size-4" />
            {{ item.label }}
          </button>
        </div>

        <QuestionList v-if="tab === 'posts'" :items="items" empty-text="TA 还没有发布过问题" />

        <template v-else-if="tab === 'questions'">
          <div v-if="!items.length" class="surface empty-state">还没有关注任何问题</div>
          <RouterLink
            v-for="item in items"
            :key="item.id || item.followQuestionId"
            :to="`/question/${item.followQuestionId}`"
            class="surface mb-3 flex items-center justify-between p-4 hover:border-indigo-200"
          >
            <span class="line-clamp-1 text-sm ui-text-strong">{{ item.followQuestionTitle }}</span>
            <span class="text-xs ui-text-muted">{{ item.time }}</span>
          </RouterLink>
        </template>

        <template v-else-if="tab === 'topic'">
          <div v-if="!(data?.followTopics?.length)" class="surface empty-state">还没有关注任何话题</div>
          <div v-else class="grid gap-3 sm:grid-cols-2">
            <RouterLink v-for="topic in data.followTopics" :key="topic.topicId || topic.topicTitle" :to="{ name: 'home', query: { tag: topic.topicTitle } }" class="surface flex items-center gap-3 p-3">
              <img :src="assetUrl(topic.topicImage)" alt="" class="h-10 w-10 rounded object-cover" />
              <span class="text-sm ui-text-strong">{{ topic.topicTitle }}</span>
            </RouterLink>
          </div>
        </template>

        <template v-else>
          <div v-if="!items.length" class="surface empty-state">这里还空空如也</div>
          <div v-else class="grid gap-3 sm:grid-cols-2">
            <RouterLink v-for="person in items" :key="person.userId" :to="`/user/${person.userId}`" class="surface flex items-center gap-3 p-4 hover:border-indigo-200">
              <Avatar :src="person.headUrl" :name="person.userName" :size="40" />
              <div class="min-w-0">
                <p class="font-medium ui-text-strong">{{ person.userName }}</p>
                <p class="line-clamp-1 text-xs ui-text-muted">{{ person.simpleSelfIntroduction || '暂无签名' }}</p>
              </div>
            </RouterLink>
          </div>
        </template>

        <Pagination :pagination="data?.pagination" @change="go({ page: $event })" />
      </div>

      <aside class="side-panel">
        <section v-if="user" class="surface p-4">
          <h3 class="mb-3 flex items-center gap-1.5 section-title"><Trophy class="size-4 text-amber-500" />个人成就</h3>
          <div class="grid grid-cols-3 gap-2 text-center">
            <div><p class="text-lg font-bold ui-text-brand">{{ user.likeCount }}</p><p class="text-xs ui-text-muted">获赞</p></div>
            <div><p class="text-lg font-bold ui-text-brand">{{ user.followCount }}</p><p class="text-xs ui-text-muted">关注</p></div>
            <div><p class="text-lg font-bold ui-text-brand">{{ user.fansCount }}</p><p class="text-xs ui-text-muted">粉丝</p></div>
          </div>
          <div class="mt-3 space-y-1 border-t ui-border pt-3 text-xs">
            <RouterLink :to="{ name: 'user', params: { userId: user.userId }, query: { tab: 'questions' } }" class="link-muted flex items-center gap-1.5"><Star class="size-3.5" />关注的问题</RouterLink>
            <RouterLink :to="{ name: 'user', params: { userId: user.userId }, query: { tab: 'topic' } }" class="link-muted flex items-center gap-1.5"><Tags class="size-3.5" />关注的话题</RouterLink>
            <RouterLink :to="{ name: 'user', params: { userId: user.userId }, query: { tab: 'fans' } }" class="link-muted flex items-center gap-1.5"><Users class="size-3.5" />我的粉丝</RouterLink>
          </div>
        </section>
        <AdCarousel :items="ads" title="社区推广" />
      </aside>
    </div>
  </div>
</template>
