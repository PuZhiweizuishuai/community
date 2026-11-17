<script setup>
/**
 * 站点 Logo：由 static/favicon.png 等比压缩为 160×160 透明 PNG（约 26KB），
 * 统一替换原来的「不」文字占位符。
 */
defineProps({
  size: { type: Number, default: 32 },
  rounded: { type: String, default: '0.6rem' },
})
const src = `${import.meta.env.BASE_URL}logo.png`
</script>

<template>
  <img
    :src="src"
    alt="不挂高数社区 Logo"
    class="shrink-0 ui-bg object-contain"
    :style="{ width: size + 'px', height: size + 'px', borderRadius: rounded }"
  />
</template>
