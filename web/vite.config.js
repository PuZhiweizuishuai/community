import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'

const BACKEND = process.env.VITE_BACKEND_URL || 'http://localhost:8080'

// 开发环境把 API / 静态资源 / WebSocket 全部代理到 Spring Boot，
// 保持同源，SESSION cookie 才能随请求自动携带。
export default defineConfig({
  plugins: [vue(), vueJsx(), vueDevTools(), tailwindcss()],
  // sockjs-client 是 webpack 时代的 CommonJS 包，内部直接引用了 webpack 注入的
  // `global`。Vite 不会自动注入该变量，这里把它映射到浏览器原生的 globalThis，
  // 否则进入聊天室会抛 "ReferenceError: global is not defined"。
  define: {
    global: 'globalThis',
  },
  optimizeDeps: {
    include: ['sockjs-client', '@stomp/stompjs'],
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: BACKEND, changeOrigin: false },
      '/file': { target: BACKEND, changeOrigin: false },
      '/image': { target: BACKEND, changeOrigin: false },
      '/captche': { target: BACKEND, changeOrigin: false },
      '/adUrl': { target: BACKEND, changeOrigin: false },
      '/ws': { target: BACKEND, changeOrigin: false, ws: true },
    },
  },
})
