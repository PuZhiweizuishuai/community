<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import Vditor from 'vditor'
import { useThemeStore } from '@/stores/theme'
import { VDITOR_CDN, vditorPreviewTheme, vditorThemeOf } from '@/utils/vditorTheme'

const props = defineProps({
  content: { type: String, default: '' },
})

const theme = useThemeStore()

/**
 * 聊天室消息的 Markdown 渲染。
 *
 * 这里只借用 Vditor 的「Markdown → HTML」能力（`Vditor.md2html`），不创建编辑器实例、
 * 不加载上传与工具栏，单条消息只做一次转换：
 *  - 纯文本消息完全不触发解析（聊天室里绝大多数消息都是纯文本）；
 *  - 解析结果出来之前先按纯文本显示，避免气泡在 Lute 脚本加载期间闪成空白；
 *  - 转换结果按内容缓存，重复内容（表情、常用短语）不再解析；
 *  - 不启用代码高亮，避免为短消息额外拉取 highlight.js，配色统一由主题变量控制。
 */
const html = ref('')
const emit = defineEmits(['rendered'])
let seq = 0

/** 聊天室最多保留 200 条消息，缓存上限略大于它即可 */
const cache = new Map()
const CACHE_LIMIT = 400

/** 只有出现 Markdown 语义时才走解析，避免无谓开销 */
const MARKDOWN_HINT =
  /(^|\n)\s*(#{1,6}\s|[-*+]\s|\d+[.)]\s|>|\||```)|`[^`\n]+`|\*\*[^*\n]+\*\*|~~[^~\n]+~~|!?\[[^\]\n]*\]\([^)\n]*\)/

/**
 * 兜底清洗：Lute 默认已开启 XSS 过滤，这里再剥掉脚本类标签与内联事件，
 * 并给外链补上 target/rel（严格 CSP 下页内跳转外部站点体验不好）。
 */
function harden(raw) {
  const tpl = document.createElement('template')
  tpl.innerHTML = raw
  tpl.content.querySelectorAll('script,style,iframe,object,embed,link,meta,form').forEach((n) => n.remove())
  tpl.content.querySelectorAll('*').forEach((node) => {
    Array.from(node.attributes).forEach((attr) => {
      const name = attr.name.toLowerCase()
      if (name.startsWith('on')) {
        node.removeAttribute(attr.name)
      } else if ((name === 'href' || name === 'src') && /^\s*(javascript|data:text\/html)/i.test(attr.value)) {
        node.removeAttribute(attr.name)
      }
    })
    if (node.tagName === 'A') {
      node.setAttribute('target', '_blank')
      node.setAttribute('rel', 'noopener noreferrer nofollow')
    }
    if (node.tagName === 'IMG') node.setAttribute('loading', 'lazy')
  })
  return tpl.innerHTML
}

async function render() {
  const content = props.content || ''
  if (!MARKDOWN_HINT.test(content)) {
    html.value = ''
    return
  }

  const cached = cache.get(content)
  if (cached !== undefined) {
    html.value = cached
    emit('rendered')
    return
  }

  const current = ++seq
  try {
    const raw = await Vditor.md2html(content, {
      ...vditorPreviewTheme(theme.isDark),
      mode: vditorThemeOf(theme.isDark).previewMode,
      emojiPath: `${VDITOR_CDN}/dist/images/emoji`,
      hljs: { enable: false },
      anchor: 0,
      markdown: { sanitize: true, toc: false, codeBlockPreview: false, mathBlockPreview: false },
    })
    // 内容在解析期间被替换时丢弃这次结果
    if (current !== seq) return
    const safe = harden(raw)
    cache.set(content, safe)
    if (cache.size > CACHE_LIMIT) cache.delete(cache.keys().next().value)
    html.value = safe
    emit('rendered')
  } catch {
    // 解析失败（例如 CDN 不可达）时保持纯文本显示，消息不能因此消失
    if (current !== seq) return
    html.value = ''
  }
}

watch(() => props.content, render, { immediate: true })

onBeforeUnmount(() => {
  seq += 1
})
</script>

<template>
  <div v-if="!html" class="chat-md-text">{{ content }}</div>
  <!-- eslint-disable-next-line vue/no-v-html -->
  <div v-else class="chat-md markdown-body" v-html="html" />
</template>
