<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RotateCcw, X, ZoomIn } from '@lucide/vue'

/**
 * 轻量图片裁剪组件（不引第三方依赖）。
 * 上传前在画布里拖动 / 缩放图片，按指定比例裁出区域并输出 JPEG dataURL。
 */
const props = defineProps({
  src: { type: String, required: true },
  aspect: { type: Number, default: 1 },
  outputWidth: { type: Number, default: 300 },
  outputHeight: { type: Number, default: 300 },
  title: { type: String, default: '裁剪图片' },
  mime: { type: String, default: 'image/jpeg' },
})
const emit = defineEmits(['confirm', 'cancel'])

const canvas = ref(null)
const zoom = ref(1)
const ready = ref(false)

let image = null
let W = 0
let H = 0
let cropW = 0
let cropH = 0
let cropLeft = 0
let cropTop = 0
let baseScale = 1
let offsetX = 0
let offsetY = 0
let dragging = false
let startX = 0
let startY = 0
let observer = null

function clamp(value, min, max) {
  if (min > max) return (min + max) / 2
  return Math.min(max, Math.max(min, value))
}

function geometry() {
  const scale = baseScale * zoom.value
  const drawW = image.width * scale
  const drawH = image.height * scale
  const maxOffsetX = cropLeft - W / 2 + drawW / 2
  const minOffsetX = cropLeft + cropW - W / 2 - drawW / 2
  const maxOffsetY = cropTop - H / 2 + drawH / 2
  const minOffsetY = cropTop + cropH - H / 2 - drawH / 2
  offsetX = clamp(offsetX, minOffsetX, maxOffsetX)
  offsetY = clamp(offsetY, minOffsetY, maxOffsetY)
  return { scale, drawW, drawH }
}

function draw() {
  const el = canvas.value
  if (!el || !image || !ready.value) return
  const ctx = el.getContext('2d')
  const { drawW, drawH } = geometry()
  const left = W / 2 + offsetX - drawW / 2
  const top = H / 2 + offsetY - drawH / 2

  ctx.clearRect(0, 0, W, H)
  ctx.fillStyle = '#0f172a'
  ctx.fillRect(0, 0, W, H)
  ctx.drawImage(image, left, top, drawW, drawH)

  // 裁切框外遮罩
  ctx.fillStyle = 'rgba(15, 23, 42, 0.62)'
  ctx.fillRect(0, 0, W, cropTop)
  ctx.fillRect(0, cropTop, cropLeft, cropH)
  ctx.fillRect(cropLeft + cropW, cropTop, W - cropLeft - cropW, cropH)
  ctx.fillRect(0, cropTop + cropH, W, H - cropTop - cropH)

  // 裁切框 + 三分线
  ctx.strokeStyle = '#ffffff'
  ctx.lineWidth = 2
  ctx.strokeRect(cropLeft, cropTop, cropW, cropH)
  ctx.strokeStyle = 'rgba(255,255,255,0.32)'
  ctx.lineWidth = 1
  for (let i = 1; i < 3; i++) {
    ctx.beginPath()
    ctx.moveTo(cropLeft + (cropW / 3) * i, cropTop)
    ctx.lineTo(cropLeft + (cropW / 3) * i, cropTop + cropH)
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(cropLeft, cropTop + (cropH / 3) * i)
    ctx.lineTo(cropLeft + cropW, cropTop + (cropH / 3) * i)
    ctx.stroke()
  }
  return { left, top, scale: baseScale * zoom.value }
}

function resize() {
  const el = canvas.value
  if (!el) return
  W = el.clientWidth || 480
  H = el.clientHeight || 320
  el.width = W
  el.height = H
  const limitW = W * 0.82
  const limitH = H * 0.82
  if (limitW / props.aspect <= limitH) {
    cropW = limitW
    cropH = cropW / props.aspect
  } else {
    cropH = limitH
    cropW = cropH * props.aspect
  }
  cropLeft = (W - cropW) / 2
  cropTop = (H - cropH) / 2
  if (image) {
    baseScale = Math.max(cropW / image.width, cropH / image.height)
    draw()
  }
}

async function loadImage() {
  ready.value = false
  if (!props.src) return
  const img = new Image()
  img.onload = () => {
    image = img
    zoom.value = 1
    offsetX = 0
    offsetY = 0
    ready.value = true
    resize()
  }
  img.onerror = () => {
    console.error('[cropper] 图片加载失败')
  }
  img.src = props.src
}

function onPointerDown(event) {
  if (!ready.value) return
  dragging = true
  startX = event.clientX - offsetX
  startY = event.clientY - offsetY
  canvas.value?.setPointerCapture?.(event.pointerId)
}
function onPointerMove(event) {
  if (!dragging) return
  offsetX = event.clientX - startX
  offsetY = event.clientY - startY
  draw()
}
function onPointerUp() {
  dragging = false
}
function onWheel(event) {
  if (!ready.value) return
  zoom.value = clamp(zoom.value + (event.deltaY > 0 ? -0.1 : 0.1), 1, 3)
  draw()
}
function reset() {
  zoom.value = 1
  offsetX = 0
  offsetY = 0
  draw()
}

function confirm() {
  if (!image) return
  const state = draw()
  if (!state) return
  const out = document.createElement('canvas')
  out.width = props.outputWidth
  out.height = props.outputHeight
  const ctx = out.getContext('2d')
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, out.width, out.height)
  const sx = (cropLeft - state.left) / state.scale
  const sy = (cropTop - state.top) / state.scale
  const sw = cropW / state.scale
  const sh = cropH / state.scale
  ctx.drawImage(image, sx, sy, sw, sh, 0, 0, out.width, out.height)
  emit('confirm', out.toDataURL(props.mime, 0.92))
}

onMounted(async () => {
  await nextTick()
  resize()
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(resize)
    observer.observe(canvas.value)
  }
  window.addEventListener('resize', resize)
  loadImage()
})
onBeforeUnmount(() => {
  observer?.disconnect()
  window.removeEventListener('resize', resize)
})
watch(() => props.src, loadImage)
watch(zoom, draw)
</script>

<template>
  <div class="fixed inset-0 z-[130] flex items-center justify-center bg-slate-900/60 p-4">
    <div class="w-full max-w-2xl rounded-2xl ui-bg p-5 shadow-2xl">
      <div class="mb-4 flex items-center justify-between">
        <div>
          <h3 class="text-base font-semibold ui-text-strong">{{ title }}</h3>
          <p class="mt-0.5 text-xs ui-text-muted">拖动图片调整位置，滚轮或滑块缩放，确认后才会上传</p>
        </div>
        <button class="btn btn-ghost btn-circle btn-sm" type="button" aria-label="关闭" @click="emit('cancel')"><X class="size-4" /></button>
      </div>

      <canvas
        ref="canvas"
        class="h-72 w-full touch-none rounded-xl bg-slate-900 sm:h-80"
        style="cursor: grab"
        @pointerdown="onPointerDown"
        @pointermove="onPointerMove"
        @pointerup="onPointerUp"
        @pointercancel="onPointerUp"
        @wheel.prevent="onWheel"
      />

      <div class="mt-4 flex items-center gap-3">
        <span class="flex shrink-0 items-center gap-1 text-xs ui-text-base"><ZoomIn class="size-3.5" />缩放</span>
        <input v-model.number="zoom" type="range" min="1" max="3" step="0.05" class="range range-primary range-xs flex-1" />
        <span class="w-10 text-right text-xs ui-text-base">{{ zoom.toFixed(1) }}×</span>
        <button class="btn btn-ghost btn-xs" type="button" @click="reset"><RotateCcw class="size-3.5" />重置</button>
      </div>

      <div class="mt-4 flex justify-end gap-2">
        <button class="btn btn-ghost btn-sm" type="button" @click="emit('cancel')">取消</button>
        <button class="btn btn-primary btn-sm" type="button" :disabled="!ready" @click="confirm">裁剪并上传</button>
      </div>
    </div>
  </div>
</template>
