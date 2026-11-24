<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { EDITOR_UPLOAD_URL } from '@/api'
import { useThemeStore } from '@/stores/theme'
import { vditorPreviewTheme, vditorThemeOf } from '@/utils/vditorTheme'
import {
  bindVditorCopyDelegate,
  hardenVditorInlineHandlers,
  observeVditorInlineHandlers,
} from '@/utils/vditorCsp'

const theme = useThemeStore()

const props = defineProps({
  modelValue: { type: String, default: '' },
  height: { type: Number, default: 420 },
  /** 移动端（<640px）高度；0 表示沿用 height */
  mobileHeight: { type: Number, default: 0 },
  /**
   * 移动端工具栏项。移动端每个按钮会被 Vditor 放大到约 49px 宽，
   * 完整工具栏会折成三四行、把编辑区挤没，所以窄屏场景传入精简子集；
   * 留空表示移动端仍用完整工具栏。
   */
  mobileToolbar: { type: Array, default: () => [] },
  placeholder: { type: String, default: '写点什么吧…支持 Markdown' },
  mode: { type: String, default: 'ir' },
  upload: { type: Boolean, default: true },
})
const emit = defineEmits(['update:modelValue', 'ready'])

const holderId = `vditor-${Math.random().toString(36).slice(2)}`
const holder = ref(null)
let editor = null
let editorReady = false
// 严格 CSP 下 Vditor 的内联事件处理器会被拦截，这里用监听 + 委托代替
let stopObserving = () => {}
let unbindCopy = () => {}
// Vditor 构造是异步的：初始化完成前收到的值先暂存，after 回调里统一写入
let pendingValue = null
let internalUpdate = false

/** 仅在 Vditor 初始化完成后调用，避免访问尚未创建的内部实例 */
function applyValue(value) {
  if (!editor) return
  const next = value ?? ''
  if (editor.getValue?.() !== next) editor.setValue?.(next)
}

// 移动端屏幕窄，Vditor 在窄屏会把每个工具栏按钮放大到约 49px，
// 完整工具栏会折成三四行、把编辑区挤成两三行可见高度。
// Vditor 的工具栏换行在初始化时计算，也没有运行时替换工具栏的 API，
// 所以由调用方通过 mobile-toolbar 传一套精简子集，这里只在初始化时选一次。
const MOBILE_QUERY = '(max-width: 639px)'
const isMobileLayout =
  typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia(MOBILE_QUERY).matches
    : false

/** 桌面端完整工具栏 */
const FULL_TOOLBAR = [
  'emoji',
  'headings',
  'bold',
  'italic',
  'strike',
  '|',
  'list',
  'ordered-list',
  'check',
  'outdent',
  'indent',
  '|',
  'quote',
  'line',
  'code',
  'inline-code',
  'insert-before',
  'insert-after',
  '|',
  'upload',
  'link',
  'table',
  '|',
  'undo',
  'redo',
  '|',
  'edit-mode',
  'fullscreen',
  'outline',
  'preview',
]

function toolbarOf() {
  const list = isMobileLayout && props.mobileToolbar.length ? props.mobileToolbar : FULL_TOOLBAR
  return list.filter((item) => item !== 'upload' || props.upload)
}

function heightOf() {
  if (isMobileLayout && props.mobileHeight > 0) return props.mobileHeight
  return props.height
}

function options() {
  const t = vditorThemeOf(theme.isDark)
  return {
    height: heightOf(),
    mode: props.mode,
    value: pendingValue ?? props.modelValue ?? '',
    placeholder: props.placeholder,
    theme: t.editor,
    icon: 'material',
    cache: { enable: false },
    counter: { enable: true, max: 100000 },
    preview: {
      delay: 300,
      markdown: { toc: true },
      // 关键：代码高亮与内容主题跟随全站主题，否则夜间模式下代码块仍是白底
      ...vditorPreviewTheme(theme.isDark),
    },
    toolbar: toolbarOf(),
    upload: props.upload
      ? {
          url: EDITOR_UPLOAD_URL,
          fieldName: 'file[]',
          max: 9 * 1024 * 1024,
          accept: 'image/*,.pdf,.zip,.mp4,.mp3',
          multiple: true,
          withCredentials: true,
          format: (files, responseText) => responseText,
        }
      : undefined,
    input: (value) => {
      internalUpdate = true
      emit('update:modelValue', value)
      internalUpdate = false
    },
    after: () => {
      editorReady = true
      // 初始化期间通过 setValue / v-model 传入的内容，此时才有内部实例可写入
      if (pendingValue !== null) {
        applyValue(pendingValue)
        pendingValue = null
      }
      hardenVditorInlineHandlers(holder.value)
      emit('ready', editor)
    },
  }
}

onMounted(() => {
  // Vditor 构造函数内部会自行初始化，不要再手动调用 init()
  editor = new Vditor(holder.value, options())
  // 预览面板会反复重绘，持续剥掉内联事件处理器并用事件委托接管复制按钮
  stopObserving = observeVditorInlineHandlers(holder.value)
  unbindCopy = bindVditorCopyDelegate(holder.value)
})

onBeforeUnmount(() => {
  stopObserving()
  unbindCopy()
  try {
    editor?.destroy?.()
  } catch {
    /* 编辑器可能尚未初始化完成 */
  }
  editor = null
  editorReady = false
  pendingValue = null
})

watch(
  () => theme.isDark,
  (dark) => {
    if (!editorReady || !editor) return
    const t = vditorThemeOf(dark)
    // setTheme(theme, contentTheme, codeTheme)：一次切换编辑器外观、内容主题与代码高亮
    editor.setTheme?.(t.editor, t.content, t.code)
  },
)

watch(
  () => props.modelValue,
  (value) => {
    if (internalUpdate) return
    if (!editorReady || !editor) {
      // 初始化完成前只记录待写入值，由 after 回调统一应用
      pendingValue = value ?? ''
      return
    }
    applyValue(value)
  },
)

defineExpose({
  getValue: () => (editorReady && editor ? editor.getValue?.() ?? '' : pendingValue ?? props.modelValue ?? ''),
  setValue: (value) => {
    const next = value ?? ''
    if (!editorReady || !editor) {
      pendingValue = next
      return
    }
    applyValue(next)
  },
  focus: () => {
    if (editorReady) editor?.focus?.()
  },
  clear: () => {
    pendingValue = ''
    if (editorReady && editor) editor.setValue?.('')
    emit('update:modelValue', '')
  },
})
</script>

<template>
  <div :id="holderId" ref="holder" class="vditor-holder w-full" />
</template>
