<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
// 按需引入，避免把整个 echarts 打进后台首屏 chunk
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  TitleComponent,
  CanvasRenderer,
])

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 300 },
})

const el = ref(null)
let chart = null
let observer = null

function draw() {
  if (!el.value || !props.option) return
  if (!chart) chart = echarts.init(el.value)
  chart.setOption(props.option, true)
}

function resize() {
  chart?.resize()
}

onMounted(() => {
  draw()
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(resize)
    observer.observe(el.value)
  }
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})

watch(() => props.option, draw, { deep: true })
</script>

<template>
  <div ref="el" class="w-full" :style="{ height: height + 'px' }" />
</template>
