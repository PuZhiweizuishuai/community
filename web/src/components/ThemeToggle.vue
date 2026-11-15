<script setup>
import { computed } from 'vue'
import { Check, Monitor, Moon, Sun } from '@lucide/vue'
import { useThemeStore } from '@/stores/theme'

const theme = useThemeStore()
const options = [
  { value: 'system', label: '跟随系统', icon: Monitor },
  { value: 'light', label: '白天模式', icon: Sun },
  { value: 'dark', label: '夜间模式', icon: Moon },
]
const currentIcon = computed(() => (theme.isDark ? Moon : Sun))
</script>

<template>
  <div class="dropdown dropdown-end">
    <button tabindex="0" class="btn btn-ghost btn-circle btn-sm" :title="`主题：${theme.mode === 'system' ? '跟随系统' : theme.resolved === 'dark' ? '夜间' : '白天'}`" aria-label="切换主题">
      <component :is="currentIcon" class="size-5" />
    </button>
    <ul tabindex="0" class="menu dropdown-content menu-sm theme-menu z-50 mt-2 w-40 rounded-box border p-2 shadow-lg">
      <li class="menu-title px-2 py-1 text-xs">外观设置</li>
      <li v-for="item in options" :key="item.value">
        <button
          type="button"
          :class="theme.mode === item.value ? 'active font-medium text-indigo-500' : ''"
          @click="theme.setMode(item.value)"
        >
          <component :is="item.icon" class="size-4" />
          {{ item.label }}
          <Check v-if="theme.mode === item.value" class="ml-auto size-3.5" />
        </button>
      </li>
    </ul>
  </div>
</template>
