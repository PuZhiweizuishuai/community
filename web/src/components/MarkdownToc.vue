<script setup>
import { onBeforeUnmount, ref } from 'vue'
import Vditor from 'vditor'

/**
 * 正文目录：对齐原 Thymeleaf 页面的 #catalogView。
 * 由 MarkdownView 在渲染完成后把内容元素传进来，用 Vditor.outlineRender 生成目录，
 * 再跟随页面滚动高亮当前章节。
 */
const el = ref(null)
const hasToc = ref(false)
let sourceEl = null
let headings = []

function render(source) {
  if (!source || !el.value) return
  sourceEl = source
  el.value.innerHTML = ''
  try {
    Vditor.outlineRender(sourceEl, el.value)
  } catch (error) {
    console.warn('[toc] 目录生成失败', error)
  }
  // outlineRender 会加上 .vditor-outline（默认 display:none / width:250px），这里适配侧栏
  el.value.style.display = hasContent() ? 'block' : 'none'
  el.value.style.width = '100%'
  el.value.style.borderRight = 'none'
  el.value.style.background = 'transparent'
  hasToc.value = hasContent()
  headings = Array.from(sourceEl.children).filter((node) => /^H[1-6]$/.test(node.tagName))
  highlight()
}

function hasContent() {
  return !!el.value && el.value.innerText.trim() !== ''
}

function highlight() {
  if (!el.value || !headings.length) return
  const line = window.scrollY + 140
  let active = headings[0]
  for (const heading of headings) {
    if (heading.getBoundingClientRect().top + window.scrollY <= line) active = heading
    else break
  }
  el.value
    .querySelectorAll('.vditor-outline__item--current')
    .forEach((node) => node.classList.remove('vditor-outline__item--current'))
  if (active?.id) {
    const item = el.value.querySelector(`[data-target-id="${CSS.escape(active.id)}"]`)
    item?.classList.add('vditor-outline__item--current')
  }
}

function onScroll() {
  highlight()
}

window.addEventListener('scroll', onScroll, { passive: true })
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))

defineExpose({ render })
</script>

<template>
  <section v-show="hasToc" class="surface p-4">
    <h3 class="mb-2 flex items-center gap-2 text-sm font-semibold text-slate-700">
      <span class="h-3.5 w-1 rounded-full bg-indigo-500" />
      目录
    </h3>
    <div ref="el" class="markdown-toc max-h-80 overflow-y-auto text-sm" />
  </section>
</template>
