<script setup>
import { ArrowRight, MessageCircle, TriangleAlert } from '@lucide/vue'
import { useSessionStore } from '@/stores/session'
const session = useSessionStore()
</script>

<template>
  <div class="page-container">
    <div class="surface mx-auto max-w-3xl p-8">
      <h1 class="flex items-center gap-2 text-2xl font-bold ui-text-strong"><MessageCircle class="size-6 ui-text-brand" />社区聊天室</h1>
      <p class="mt-2 text-sm ui-text-base">和其他开发者实时交流，聊技术、聊生活，也欢迎来摸鱼。</p>

      <RouterLink v-if="session.loggedIn" to="/chat/room" class="btn btn-primary mt-5">开始聊天<ArrowRight class="size-4" /></RouterLink>
      <RouterLink v-else :to="{ name: 'sign-in', query: { redirect: '/chat/room' } }" class="btn btn-primary mt-5">登录后开始聊天<ArrowRight class="size-4" /></RouterLink>

      <div class="alert alert-warning mt-8 text-sm">
        <TriangleAlert class="size-5 shrink-0" />
        <span>进房前请先读一读下面的约定</span>
      </div>
      <ol class="mt-4 list-inside list-decimal space-y-2 text-sm ui-text-base">
        <li>友善交流，禁止人身攻击、辱骂与歧视性言论。</li>
        <li>不要刷屏、发布广告或与社区无关的内容。</li>
        <li>禁止发布违法违规、涉密或侵犯他人隐私的信息。</li>
        <li>聊天内容仅用于交流，请勿直接粘贴大段敏感代码或密钥。</li>
        <li>管理员有权对违规行为进行提醒、禁言或移除。</li>
      </ol>
    </div>
  </div>
</template>
