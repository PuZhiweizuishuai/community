<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { EDITOR_UPLOAD_URL } from '@/api'
import { useThemeStore } from '@/stores/theme'
import { vditorPreviewTheme, vditorThemeOf } from '@/utils/vditorTheme'

const theme = useThemeStore()

const props = defineProps({
  modelValue: { type: String, default: '' },
  height: { type: Number, default: 420 },
  placeholder: { type: String, default: '写点什么吧…支持 Markdown' },
  mode: { type: String, default: 'ir' },
  upload: { type: Boolean, default: true },
})
const emit = defineEmits(['update:modelValue', 'ready'])

const holderId = `vditor-${Math.random().toString(36).slice(2)}`
const holder = ref(null)
let editor = null
let internalUpdate = false

function options() {
  const t = vditorThemeOf(theme.isDark)
  return {
    height: props.height,
    mode: props.mode,
    value: props.modelValue || '',
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
    toolbar: [
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
      props.upload ? 'upload' : '',
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
    ].filter(Boolean),
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
    after: () => emit('ready', editor),
  }
}

onMounted(() => {
  // Vditor 构造函数内部会自行初始化，不要再手动调用 init()
  editor = new Vditor(holder.value, options())
})

onBeforeUnmount(() => {
  try {
    editor?.destroy?.()
  } catch {
    /* 编辑器可能尚未初始化完成 */
  }
  editor = null
})

watch(
  () => theme.isDark,
  (dark) => {
    if (!editor) return
    const t = vditorThemeOf(dark)
    // setTheme(theme, contentTheme, codeTheme)：一次切换编辑器外观、内容主题与代码高亮
    editor.setTheme?.(t.editor, t.content, t.code)
  },
)

watch(
  () => props.modelValue,
  (value) => {
    if (internalUpdate || !editor) return
    const current = editor.getValue?.()
    if (current !== value) editor.setValue?.(value ?? '')
  },
)

defineExpose({
  getValue: () => editor?.getValue?.() ?? '',
  setValue: (value) => editor?.setValue?.(value ?? ''),
  focus: () => editor?.focus?.(),
  clear: () => {
    editor?.setValue?.('')
    emit('update:modelValue', '')
  },
})
</script>

<template>
  <div :id="holderId" ref="holder" class="vditor-holder w-full" />
</template>
