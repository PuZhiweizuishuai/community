<script setup>
import { ArrowUpRight } from '@lucide/vue'
const year = new Date().getFullYear()
</script>

<template>
  <footer class="mt-8 border-t border-slate-200 bg-white">
    <div class="page-container flex flex-col items-center justify-between gap-2 py-5 text-sm text-slate-500 sm:flex-row">
      <span>Copyright © 2019 - {{ year }} 不挂高数社区 · 保留所有权利</span>
      <div class="flex items-center gap-4">
        <RouterLink to="/" class="link-muted">首页</RouterLink>
        <RouterLink to="/about" class="link-muted">关于我们</RouterLink>
        <a href="https://github.com/PuZhiweizuishuai/community" target="_blank" rel="noopener" class="link-muted inline-flex items-center gap-0.5">GitHub<ArrowUpRight class="size-3" /></a>
      </div>
    </div>
  </footer>
</template>
