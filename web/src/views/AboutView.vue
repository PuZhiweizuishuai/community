<script setup>
import { ArrowUpRight, Code, Mail, UserRound } from '@lucide/vue'
import { assetUrl } from '@/api'
</script>

<template>
  <div class="page-container">
    <div class="surface mx-auto max-w-3xl p-8 text-center">
      <h1 class="text-2xl font-bold text-slate-800">关于不挂高数</h1>
      <p class="mt-3 text-sm leading-relaxed text-slate-600">
        一个面向开发者的问答社区。在这里可以提问、分享、讨论技术，也可以记录自己的学习过程。
        本项目最初基于 Spring Boot + Thymeleaf 构建，现已重构为 Spring Boot REST API + Vue 3 前后端分离架构。
      </p>

      <div class="mt-8 grid gap-4 text-left sm:grid-cols-3">
        <div class="rounded-lg border border-slate-100 p-4">
          <h2 class="flex items-center gap-1.5 font-semibold text-slate-700"><UserRound class="size-4 text-indigo-500" />关于我</h2>
          <p class="mt-1 text-xs text-slate-500">热爱折腾的开发者，喜欢把想法变成能跑起来的东西。</p>
        </div>
        <div class="rounded-lg border border-slate-100 p-4">
          <h2 class="flex items-center gap-1.5 font-semibold text-slate-700"><Mail class="size-4 text-indigo-500" />联系我</h2>
          <p class="mt-1 text-xs text-slate-500">puzhiweipuzhiwei@foxmail.com</p>
          <a href="https://github.com/PuZhiweizuishuai/community" target="_blank" rel="noopener" class="mt-1 inline-flex items-center gap-0.5 text-xs text-indigo-600">GitHub 仓库 <ArrowUpRight class="size-3" /></a>
        </div>
        <div class="rounded-lg border border-slate-100 p-4">
          <h2 class="flex items-center gap-1.5 font-semibold text-slate-700"><Code class="size-4 text-indigo-500" />技术栈</h2>
          <p class="mt-1 text-xs text-slate-500">Spring Boot 4 · Java 25 · MyBatis · Redis · Vue 3 · Vite · daisyUI · Vditor · ECharts</p>
        </div>
      </div>

      <div class="mt-8">
        <img :src="assetUrl('/image/101-desktop-wallpaper.png')" alt="社区" class="mx-auto h-40 rounded-lg object-cover" />
        <p class="mt-3 text-xs text-slate-400">用提问连接每一个愿意分享的人</p>
      </div>
    </div>
  </div>
</template>
