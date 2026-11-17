<script setup>
import { ArrowUpRight } from '@lucide/vue'
const year = new Date().getFullYear()
</script>

<template>
  <footer class="site-footer mt-4 mb-16 border-t ui-border ui-bg lg:mt-8 lg:mb-0">
    <div class="page-container flex flex-col items-center justify-between gap-2 pt-5 text-center text-xs ui-text-muted sm:flex-row sm:text-left sm:text-sm">
      <span>Copyright © 2019 - {{ year }} 不挂高数社区 · 保留所有权利</span>
      <div class="flex items-center gap-4">
        <RouterLink to="/" class="link-muted">首页</RouterLink>
        <RouterLink to="/about" class="link-muted">关于我们</RouterLink>
        <a href="https://github.com/PuZhiweizuishuai/community" target="_blank" rel="noopener" class="link-muted inline-flex items-center gap-0.5">GitHub<ArrowUpRight class="size-3" /></a>
      </div>
    </div>
  </footer>
</template>
