<script setup>
import { computed } from 'vue'
import { BookOpen, MessagesSquare, PencilLine } from '@lucide/vue'
import { useMetaStore } from '@/stores/meta'
import SiteLogo from '@/components/SiteLogo.vue'

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  eyebrow: { type: String, default: '' },
})

const meta = useMetaStore()
const year = new Date().getFullYear()
const stats = computed(() => [
  { label: '热门标签', value: meta.hots.length || '—' },
  { label: '热门讨论', value: meta.hotQuestions.length || '—' },
  { label: '在线成员', value: meta.data.onlineCount ?? '—' },
])
const features = [
  { icon: PencilLine, title: '提出好问题', desc: '把背景、尝试和期望写清楚，更容易得到高质量回答' },
  { icon: BookOpen, title: '分享与沉淀', desc: '支持 Markdown、代码块与流程图，让知识可被检索' },
  { icon: MessagesSquare, title: '和同好交流', desc: '关注话题与用户，消息通知不错过每一条有价值的回复' },
]
</script>

<template>
  <div class="auth-bg min-h-screen lg:grid lg:grid-cols-[minmax(0,1.05fr)_minmax(0,1fr)]">
    <!-- 品牌展示区（大屏） -->
    <aside class="relative hidden overflow-hidden bg-gradient-to-br from-indigo-600 via-violet-600 to-indigo-800 px-12 py-10 text-white lg:flex lg:flex-col">
      <div class="pointer-events-none absolute -right-24 -top-24 h-80 w-80 rounded-full bg-white/10 blur-2xl" />
      <div class="pointer-events-none absolute -bottom-32 -left-16 h-96 w-96 rounded-full bg-sky-300/10 blur-3xl" />

      <RouterLink to="/" class="relative flex items-center gap-3">
        <SiteLogo :size="42" rounded="0.8rem" />
        <div>
          <p class="text-lg font-bold leading-tight">不挂高数</p>
          <p class="text-xs text-white/70">开发者问答社区</p>
        </div>
      </RouterLink>

      <div class="relative my-auto max-w-md py-10">
        <span class="inline-flex items-center gap-2 rounded-full border border-white/25 bg-white/10 px-3 py-1 text-xs">
          <span class="h-1.5 w-1.5 rounded-full bg-emerald-300" />
          和开发者一起把问题聊明白
        </span>
        <h2 class="mt-6 text-4xl font-bold leading-tight tracking-tight">
          每一个好问题，<br />都值得一个好答案。
        </h2>
        <p class="mt-4 text-sm leading-relaxed text-white/75">
          从一次提问开始，记录思路、分享经验、结识同好。社区里已有大量技术讨论与资料沉淀，等你来补充下一条。
        </p>

        <ul class="mt-9 space-y-4">
          <li v-for="item in features" :key="item.title" class="flex items-start gap-3">
            <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-white/15"><component :is="item.icon" class="size-5" /></span>
            <div>
              <p class="text-sm font-medium">{{ item.title }}</p>
              <p class="mt-0.5 text-xs leading-relaxed text-white/65">{{ item.desc }}</p>
            </div>
          </li>
        </ul>
      </div>

      <div class="relative flex items-center gap-8 border-t border-white/15 pt-6">
        <div v-for="item in stats" :key="item.label">
          <p class="text-xl font-bold">{{ item.value }}</p>
          <p class="text-xs text-white/60">{{ item.label }}</p>
        </div>
        <p class="ml-auto text-xs text-white/50">© {{ year }} 不挂高数</p>
      </div>
    </aside>

    <!-- 表单区 -->
    <main class="flex min-h-screen items-center justify-center px-5 py-10">
      <div class="w-full max-w-[440px]">
        <RouterLink to="/" class="mb-6 flex items-center justify-center gap-2 lg:hidden">
          <SiteLogo :size="38" rounded="0.75rem" />
          <span class="text-lg font-bold text-indigo-600">不挂高数</span>
        </RouterLink>

        <div class="surface p-7 sm:p-9">
          <p v-if="eyebrow" class="mb-1 text-xs font-medium uppercase tracking-widest text-indigo-500">{{ eyebrow }}</p>
          <h1 class="text-2xl font-bold text-slate-800">{{ title }}</h1>
          <p v-if="subtitle" class="mt-1.5 text-sm text-slate-500">{{ subtitle }}</p>
          <div class="mt-6">
            <slot />
          </div>
        </div>

        <p class="mt-6 text-center text-xs text-slate-400">
          登录即表示你同意社区规范 ·
          <RouterLink to="/about" class="link-muted">关于我们</RouterLink>
          ·
          <RouterLink to="/" class="link-muted">返回首页</RouterLink>
        </p>
      </div>
    </main>
  </div>
</template>
