<script setup>
import { Frown, Home } from '@lucide/vue'
</script>

<template>
  <div class="page-container">
    <div class="surface mx-auto max-w-xl p-12 text-center">
      <Frown class="mx-auto size-16 text-slate-300" :stroke-width="1.5" />
      <h1 class="mt-4 text-2xl font-bold text-slate-800">页面走丢了</h1>
      <p class="mt-2 text-sm text-slate-500">你要找的内容不存在，或者已经被移走了。</p>
      <RouterLink to="/" class="btn btn-primary mt-6"><Home class="size-4" />回到首页</RouterLink>
    </div>
  </div>
</template>
