<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { CornerDownLeft, SendHorizontal, Smile, SquarePen } from '@lucide/vue'
import { useThemeStore } from '@/stores/theme'
import { useToastStore } from '@/stores/toast'
import { vditorPreviewTheme, vditorThemeOf } from '@/utils/vditorTheme'
import { hardenVditorInlineHandlers, observeVditorInlineHandlers } from '@/utils/vditorCsp'

const props = defineProps({
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '输入消息，支持 Markdown…' },
  maxLength: { type: Number, default: 4000 },
  /** 桌面端编辑器高度（px）；工具栏约 42px，剩余给输入区，移动端另算 */
  height: { type: Number, default: 108 },
})
const emit = defineEmits(['send'])

const theme = useThemeStore()
const toast = useToastStore()

const holder = ref(null)
const value = ref('')
const ready = ref(false)
/** 移动端默认收起工具栏，给消息区让出高度 */
const toolbarOpen = ref(false)
let editor = null
let stopObserving = () => {}
/** 连点发送时的防抖时间戳 */
let lastSent = 0
/**
 * 中文输入法下「回车确认候选词」的 keydown 带 isComposing=true。
 * 如果只在 keydown 发送，这一下会被直接丢掉，用户必须再敲一次回车才能发消息。
 * 现在改成：keydown 只负责拦住 Vditor 的默认回车，keyup / compositionend 才触发发送。
 */
let pendingEnter = false
let composingEnter = false

const length = computed(() => value.value.trim().length)
const overLimit = computed(() => length.value > props.maxLength)
// 不依赖 ready：Vditor 的 after 要等 CDN 上的 Lute 加载完，
// 期间编辑区其实已经能打字，若把发送在这里挡掉，第一次回车同样会「没反应」
const canSend = computed(() => !props.disabled && length.value > 0 && !overLimit.value)

function options() {
  const t = vditorThemeOf(theme.isDark)
  return {
    // 移动端工具栏收起，不需要再为它预留高度
    height: window.innerWidth < 640 ? 84 : props.height,
    mode: 'ir',
    value: '',
    placeholder: props.placeholder,
    theme: t.editor,
    icon: 'material',
    cache: { enable: false },
    // 计数器交给外层的页脚，避免与工具栏抢空间
    counter: { enable: false },
    // 只保留 Markdown 编辑相关按钮：聊天室里不做上传、不做全屏
    toolbar: [
      'emoji',
      'headings',
      'bold',
      'italic',
      'strike',
      '|',
      'inline-code',
      'code',
      'quote',
      'link',
      '|',
      'list',
      'ordered-list',
      'check',
      '|',
      'undo',
      'redo',
    ],
    preview: {
      delay: 160,
      ...vditorPreviewTheme(theme.isDark),
    },
    // 聊天室不提供文件上传：直接拦下，不请求后端，也不占用服务器存储
    upload: {
      handler: () => '聊天室暂不支持上传文件，请使用图片或附件链接',
    },
    input: (markdown) => (value.value = markdown),
    after: () => {
      ready.value = true
      hardenVditorInlineHandlers(holder.value)
      // 桌面端进入房间即可直接打字；移动端不抢焦点，避免键盘立刻弹出
      if (window.matchMedia('(min-width: 640px) and (pointer: fine)').matches) editor?.focus?.()
    },
  }
}

function clear() {
  value.value = ''
  editor?.setValue?.('')
}

function submit() {
  if (!canSend.value) {
    if (overLimit.value) toast.warning(`单条消息最多 ${props.maxLength} 个字符`)
    // 连接未建立时给出明确反馈，避免第一次回车像「被吞掉」一样毫无提示
    else if (props.disabled) toast.info('聊天室连接中，请稍候再发送')
    return
  }
  const now = Date.now()
  if (now - lastSent < 400) return
  lastSent = now
  emit('send', value.value.trim())
  clear()
}

/** 光标位于列表 / 引用 / 代码块内，或者正在输入块级语法时，回车交给 Vditor 续写 */
function caretInStructure() {
  const node = window.getSelection()?.anchorNode
  const el = node?.nodeType === 1 ? node : node?.parentElement
  if (!el) return false
  const structure = el.closest('li, blockquote, pre, table, .vditor-wysiwyg__block')
  // 即时渲染模式的编辑区本身就是一个 <pre class="vditor-reset">，要把它排除在外
  if (structure && structure !== holder.value && !structure.classList.contains('vditor-reset')) return true
  // 只写了一半的块级语法（``` 、- 、1. 、> 、# ）直接发出去没有意义，先让编辑器接管
  const block = el.closest('p[data-block]')
  return /^\s*(```|~~~|[-*+]\s|\d+[.)]\s|>|#{1,6}\s)/.test(block?.textContent ?? '')
}

function onKeydown(event) {
  if (event.key !== 'Enter' && event.keyCode !== 13) return
  // 只在编辑区内生效：工具栏按钮（如表情面板）里的回车属于按钮自身的行为
  if (!event.target?.closest?.('.vditor-ir, .vditor-wysiwyg, .vditor-sv')) return
  if (event.ctrlKey || event.metaKey) {
    event.preventDefault()
    event.stopPropagation()
    submit()
    return
  }
  // 输入法组合态回车：候选词还没提交，先拦住 Vditor，等 compositionend / keyup 再发
  if (event.isComposing || event.keyCode === 229) {
    composingEnter = true
    event.preventDefault()
    event.stopPropagation()
    return
  }
  // Vditor 的即时渲染模式没有实现 Shift + Enter 软换行，这里补一个「新起一段」
  if (event.shiftKey) {
    event.preventDefault()
    event.stopPropagation()
    document.execCommand('insertParagraph')
    return
  }
  if (caretInStructure()) return
  // 捕获阶段拦下回车：不让 Vditor 再插入一个空段落，发送留给 keyup
  event.preventDefault()
  event.stopPropagation()
  pendingEnter = true
}

/** 等 Vditor 的 input 回调与输入法提交都同步完，再读取内容发送 */
function sendOnNextFrame() {
  requestAnimationFrame(() => submit())
}

function onKeyup(event) {
  if (event.key !== 'Enter' && event.keyCode !== 13) return
  if (composingEnter) {
    composingEnter = false
    if (!event.isComposing) sendOnNextFrame()
    return
  }
  if (!pendingEnter || event.isComposing) return
  pendingEnter = false
  sendOnNextFrame()
}

/** 部分输入法只在 compositionend 收尾、不再补 keyup，这里兜底 */
function onCompositionEnd() {
  if (!composingEnter) return
  composingEnter = false
  sendOnNextFrame()
}

onMounted(() => {
  editor = new Vditor(holder.value, options())
  stopObserving = observeVditorInlineHandlers(holder.value)
})

onBeforeUnmount(() => {
  stopObserving()
  try {
    editor?.destroy?.()
  } catch {
    /* 编辑器可能尚未初始化完成 */
  }
  editor = null
  ready.value = false
})

watch(
  () => props.disabled,
  (disabled) => {
    if (!ready.value || !editor) return
    if (disabled) editor.disabled?.()
    else editor.enable?.()
  },
)

watch(
  () => theme.isDark,
  (dark) => {
    if (!ready.value || !editor) return
    const t = vditorThemeOf(dark)
    editor.setTheme?.(t.editor, t.content, t.code)
  },
)

defineExpose({
  focus: () => editor?.focus?.(),
  clear,
})
</script>

<template>
  <div class="chat-composer" :class="{ 'is-toolbar-open': toolbarOpen }">
    <div class="chat-composer-card">
      <div
        ref="holder"
        class="chat-composer-editor"
        @keydown.capture="onKeydown"
        @keyup.capture="onKeyup"
        @compositionend.capture="onCompositionEnd"
      />

      <div class="chat-composer-bar">
        <div class="flex min-w-0 items-center gap-2 text-xs ui-text-muted">
          <button
            type="button"
            class="btn btn-ghost btn-xs gap-1 px-1.5 sm:hidden"
            :aria-pressed="toolbarOpen"
            :title="toolbarOpen ? '收起格式工具栏' : '展开格式工具栏'"
            @click="toolbarOpen = !toolbarOpen"
          >
            <SquarePen class="size-3.5" />
            格式
          </button>
          <span class="hidden items-center gap-1 sm:inline-flex">
            <Smile class="size-3.5" />
            Markdown 可用
          </span>
          <span class="hidden items-center gap-1 lg:inline-flex">
            <CornerDownLeft class="size-3.5" />
            Enter 发送 · Shift + Enter 换行
          </span>
          <span v-if="overLimit" class="text-rose-500">已超出 {{ length - maxLength }} 字</span>
        </div>

        <div class="flex shrink-0 items-center gap-2">
          <span class="hidden text-xs tabular-nums ui-text-muted sm:inline">{{ length }}/{{ maxLength }}</span>
          <button
            type="button"
            class="btn btn-primary btn-sm gap-1.5"
            :disabled="disabled || !canSend"
            @click="submit"
          >
            <SendHorizontal class="size-4" />
            <span class="hidden sm:inline">发送</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
