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
let headings = []

function render(source) {
  if (!source || !el.value) return
  // 正文可能由多个段落容器组成，需要递归收集全部标题；
  // Vditor.outlineRender 只识别直接子级标题，因此先放入临时容器再生成目录
  headings = Array.from(source.querySelectorAll('h1, h2, h3, h4, h5, h6')).filter((node) => node.id)
  el.value.innerHTML = ''
  if (headings.length) {
    const headingContainer = document.createElement('div')
    headings.forEach((heading) => headingContainer.appendChild(heading.cloneNode(true)))
    try {
      Vditor.outlineRender(headingContainer, el.value)
    } catch (error) {
      console.warn('[toc] 目录生成失败', error)
    }
  }
  // outlineRender 会加上 .vditor-outline（默认 display:none / width:250px），这里适配侧栏
  el.value.style.display = hasContent() ? 'block' : 'none'
  el.value.style.width = '100%'
  el.value.style.borderRight = 'none'
  el.value.style.background = 'transparent'
  hasToc.value = hasContent()
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
    <h3 class="mb-2 flex items-center gap-2 section-title">
      <span class="h-3.5 w-1 rounded-full bg-indigo-500" />
      目录
    </h3>
    <div ref="el" class="markdown-toc max-h-80 overflow-y-auto text-sm" />
  </section>
</template>
