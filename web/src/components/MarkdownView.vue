<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { useThemeStore } from '@/stores/theme'
import { vditorPreviewTheme, vditorThemeOf } from '@/utils/vditorTheme'

const theme = useThemeStore()

const props = defineProps({
  content: { type: String, default: '' },
  anchor: { type: Number, default: 1 },
})
const emit = defineEmits(['rendered'])

const el = ref(null)
let disposed = false

async function render() {
  if (!el.value || disposed) return
  await nextTick()
  el.value.innerHTML = ''
  Vditor.preview(el.value, props.content || '', {
    // 代码高亮主题 / 内容主题必须显式指定，默认永远是亮色 github
    ...vditorPreviewTheme(theme.isDark),
    mode: vditorThemeOf(theme.isDark).previewMode,
    anchor: props.anchor,
    markdown: { toc: true },
    after: () => emit('rendered', el.value),
  })
}

onMounted(render)
watch(() => props.content, render)
watch(() => theme.isDark, render)
onBeforeUnmount(() => (disposed = true))
</script>

<template>
  <div ref="el" class="markdown-body vditor-reset min-h-[2rem]" />
</template>
