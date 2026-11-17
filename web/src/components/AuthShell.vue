<script setup>
import SiteLogo from '@/components/SiteLogo.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
})
</script>

<template>
  <div class="auth-bg relative flex min-h-[100dvh] flex-col items-center justify-center px-4 py-10">
    <div class="absolute right-4 top-4">
      <ThemeToggle />
    </div>

    <div class="w-full max-w-[420px]">
      <RouterLink to="/" class="mb-6 flex items-center justify-center gap-2">
        <SiteLogo :size="40" rounded="0.75rem" />
        <span class="text-lg font-bold ui-text-brand">不挂高数</span>
      </RouterLink>

      <div class="surface p-6 sm:p-8">
        <h1 class="text-2xl font-bold ui-text-strong">{{ title }}</h1>
        <p v-if="subtitle" class="mt-1.5 text-sm ui-text-base">{{ subtitle }}</p>
        <div class="mt-6">
          <slot />
        </div>
      </div>

      <p class="mt-5 text-center text-xs ui-text-muted">
        登录即表示你同意社区规范 ·
        <RouterLink to="/about" class="link-muted">关于我们</RouterLink>
        ·
        <RouterLink to="/" class="link-muted">返回首页</RouterLink>
      </p>
    </div>
  </div>
</template>
