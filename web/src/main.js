import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import './assets/main.css'
import { ApiError, setErrorHandler, setUnauthorizedHandler } from '@/api/request'
import { useToastStore } from '@/stores/toast'
import { useSessionStore } from '@/stores/session'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

const toast = useToastStore(pinia)

// 异步 watcher（页面拉数据）里抛出的错误统一兜底，避免控制台出现未处理拒绝的警告
app.config.errorHandler = (error, instance, info) => {
  if (error instanceof ApiError) return // 统一 fetch 组件已经弹过提示
  console.error('[vue]', info, error)
  toast.error(`页面出现异常：${error?.message || '未知错误'}`)
}

// 路由懒加载失败、导航守卫抛错时给出可见反馈，而不是白屏
router.onError((error, to) => {
  console.error('[router] 页面加载失败', to?.fullPath, error)
  toast.error(`页面加载失败：${error?.message || '未知错误'}`)
})

// 统一 fetch 组件的两类全局回调
setErrorHandler((error) => toast.error(error.message))
setUnauthorizedHandler(() => {
  const session = useSessionStore(pinia)
  const path = window.location.pathname
  session.refresh(true)
  if (path.startsWith('/admin')) {
    router.push({ name: 'admin-login', query: { redirect: path } })
  } else if (path !== '/sign-in' && path !== '/sign-up') {
    router.push({ name: 'sign-in', query: { redirect: path + window.location.search } })
  }
})

app.mount('#app')
