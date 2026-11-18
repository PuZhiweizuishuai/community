import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import { useMetaStore } from '@/stores/meta'

const routes = [
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '首页' } },
  {
    path: '/question/:id',
    name: 'question',
    component: () => import('@/views/QuestionView.vue'),
    meta: { title: '问题详情' },
  },
  {
    path: '/publish',
    name: 'publish',
    component: () => import('@/views/PublishView.vue'),
    meta: { title: '发起讨论', requiresAuth: true, permission: 'content:question:publish' },
  },
  {
    path: '/publish/:id',
    name: 'publish-edit',
    component: () => import('@/views/PublishView.vue'),
    meta: { title: '编辑讨论', requiresAuth: true, permission: 'content:question:publish' },
  },
  { path: '/sign-in', name: 'sign-in', component: () => import('@/views/SignInView.vue'), meta: { title: '登录', bare: true } },
  { path: '/sign-up', name: 'sign-up', component: () => import('@/views/SignUpView.vue'), meta: { title: '注册', bare: true } },
  { path: '/reset-password', name: 'reset-password', component: () => import('@/views/ResetPasswordView.vue'), meta: { title: '重置密码', bare: true } },
  {
    path: '/search/:keyword?',
    name: 'search',
    component: () => import('@/views/SearchView.vue'),
    meta: { title: '搜索' },
  },
  { path: '/topics', name: 'topics', component: () => import('@/views/TopicsView.vue'), meta: { title: '话题' } },
  { path: '/users', name: 'user-list', component: () => import('@/views/UserListView.vue'), meta: { title: '用户' } },
  {
    path: '/user/:userId',
    name: 'user',
    component: () => import('@/views/UserView.vue'),
    meta: { title: '个人主页' },
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('@/views/MessagesView.vue'),
    meta: { title: '消息通知', requiresAuth: true },
  },
  {
    path: '/my/reviews',
    name: 'my-reviews',
    component: () => import('@/views/MyReviewsView.vue'),
    meta: { title: '我的审核', requiresAuth: true },
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { title: '个人设置', requiresAuth: true },
  },
  { path: '/about', name: 'about', component: () => import('@/views/AboutView.vue'), meta: { title: '关于' } },
  { path: '/chat', name: 'chat-rules', component: () => import('@/views/ChatRulesView.vue'), meta: { title: '聊天室' } },
  {
    path: '/chat/room',
    name: 'chat-room',
    component: () => import('@/views/ChatRoomView.vue'),
    meta: { title: '聊天室', bare: true, requiresAuth: true },
  },
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('@/views/admin/AdminLoginView.vue'),
    meta: { title: '后台登录', bare: true, adminArea: true },
  },
  { path: '/admin', redirect: '/admin/dashboard', meta: { adminArea: true } },
  {
    path: '/admin/dashboard',
    name: 'admin-dashboard',
    component: () => import('@/views/admin/AdminDashboardView.vue'),
    meta: { title: '数据概览', adminArea: true, requiresAdmin: true, permission: 'admin:access' },
  },
  {
    path: '/admin/users',
    name: 'admin-users',
    component: () => import('@/views/admin/AdminUsersView.vue'),
    meta: { title: '用户管理', adminArea: true, requiresAdmin: true, permission: 'admin:user' },
  },
  {
    path: '/admin/questions',
    name: 'admin-questions',
    component: () => import('@/views/admin/AdminQuestionsView.vue'),
    meta: { title: '问题管理', adminArea: true, requiresAdmin: true, permission: 'admin:question' },
  },
  {
    path: '/admin/reviews',
    name: 'admin-reviews',
    component: () => import('@/views/admin/AdminReviewsView.vue'),
    meta: { title: '内容审核', adminArea: true, requiresAdmin: true, permission: 'admin:review' },
  },
  {
    path: '/admin/search',
    name: 'admin-search',
    component: () => import('@/views/admin/AdminSearchView.vue'),
    meta: { title: '站内搜索', adminArea: true, requiresAdmin: true, permission: 'admin:search' },
  },
  {
    path: '/admin/controller',
    name: 'admin-controller',
    component: () => import('@/views/admin/AdminControllerView.vue'),
    meta: { title: '控制面板', adminArea: true, requiresAdmin: true, permission: 'admin:system' },
  },
  {
    path: '/admin/roles',
    name: 'admin-roles',
    component: () => import('@/views/admin/AdminRolesView.vue'),
    meta: { title: '角色权限', adminArea: true, requiresAdmin: true, permission: 'admin:role' },
  },
  {
    path: '/admin/storage',
    name: 'admin-storage',
    component: () => import('@/views/admin/AdminStorageView.vue'),
    meta: { title: '存储配置', adminArea: true, requiresAdmin: true, permission: 'admin:storage' },
  },
  {
    path: '/admin/files',
    name: 'admin-files',
    component: () => import('@/views/admin/AdminFilesView.vue'),
    meta: { title: '文件管理', adminArea: true, requiresAdmin: true, permission: 'admin:file' },
  },
  {
    path: '/admin/topics',
    name: 'admin-topics',
    component: () => import('@/views/admin/AdminTopicsView.vue'),
    meta: { title: '话题管理', adminArea: true, requiresAdmin: true, permission: 'admin:topic' },
  },
  {
    path: '/admin/classifications',
    name: 'admin-classifications',
    component: () => import('@/views/admin/AdminClassificationsView.vue'),
    meta: { title: '分类管理', adminArea: true, requiresAdmin: true, permission: 'admin:classification' },
  },
  {
    path: '/admin/advertisements',
    name: 'admin-ads',
    component: () => import('@/views/admin/AdminAdvertisementsView.vue'),
    meta: { title: '广告与新闻', adminArea: true, requiresAdmin: true, permission: 'admin:advertisement' },
  },
  {
    path: '/admin/advertisements/new',
    name: 'admin-ad-new',
    component: () => import('@/views/admin/AdminAdvertisementEditView.vue'),
    meta: { title: '新增广告', adminArea: true, requiresAdmin: true, permission: 'admin:advertisement' },
  },
  {
    path: '/admin/advertisements/:id',
    name: 'admin-ad-edit',
    component: () => import('@/views/admin/AdminAdvertisementEditView.vue'),
    meta: { title: '编辑广告', adminArea: true, requiresAdmin: true, permission: 'admin:advertisement' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面走丢了' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, saved) {
    if (saved) return saved
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const session = useSessionStore()
  if (!session.loaded) await session.refresh()
  const meta = useMetaStore()
  const chatRoute = to.name === 'chat-rules' || to.name === 'chat-room'
  if (chatRoute) {
    // 进入聊天室前强制刷新开关，避免管理员关闭后旧页面仍可进入
    await meta.load(true)
    if (!meta.chatEnabled) return { name: 'home' }
  } else if (!meta.loaded) {
    await meta.load()
  }
  if (to.meta.requiresAuth && !session.loggedIn) {
    return { name: 'sign-in', query: { redirect: to.fullPath } }
  }
  // 被封禁账号只能浏览公开页面，后台与需权限的页面统一回首页
  if (session.banned && (to.meta.requiresAdmin || to.meta.permission)) {
    return { name: 'home' }
  }
  if (to.meta.requiresAdmin && !session.isAdmin) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }
  // 细粒度权限：后台无权限停留在后台首页，普通用户无权限回首页
  if (to.meta.permission && !session.can(to.meta.permission)) {
    if (to.meta.adminArea) {
      return session.isAdmin
        ? { name: 'admin-dashboard' }
        : { name: 'admin-login', query: { redirect: to.fullPath } }
    }
    return { name: 'home' }
  }
  if (to.name === 'sign-in' && session.loggedIn) return { name: 'home' }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 不挂高数` : '不挂高数 · 社区'
})

export default router
