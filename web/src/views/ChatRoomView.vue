<script setup>
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import {
  ArrowDown,
  ArrowLeft,
  BookOpen,
  Loader,
  MessagesSquare,
  TriangleAlert,
  Users,
  Wifi,
} from '@lucide/vue'
import { useSessionStore } from '@/stores/session'
import { useMetaStore } from '@/stores/meta'
import Avatar from '@/components/Avatar.vue'
import ChatComposer from '@/components/ChatComposer.vue'
import ChatMarkdown from '@/components/ChatMarkdown.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { chatDayKey, chatDayLabel, chatTimeLabel } from '@/utils/format'

const session = useSessionStore()
const meta = useMetaStore()

const messages = ref([])
const members = ref([])
const online = ref(0)
const connected = ref(false)
const connecting = ref(true)
const rejected = ref('')
const disabled = ref('')
const area = ref(null)
const composer = ref(null)
const atBottom = ref(true)
const unread = ref(0)
/** 同一发送者在这个时间窗内的连续消息会合并显示（只保留一条头像与昵称） */
const GROUP_WINDOW = 5 * 60 * 1000
let seed = 0
let client = null

// 每个窗口一个令牌：服务端把「重复登录」提示原样带回，只有被拒绝的窗口会处理
const clientToken = `${Date.now()}-${Math.random().toString(36).slice(2)}`

const selfId = computed(() => String(session.user?.userId ?? ''))
const selfName = computed(() => session.user?.userName ?? '')

const statusText = computed(() => {
  if (disabled.value) return '已关闭'
  if (rejected.value) return '已被拒绝'
  if (connected.value) return `在线 ${online.value} 人`
  return connecting.value ? '连接中…' : '已断开'
})
const statusTone = computed(() => {
  if (disabled.value || rejected.value) return 'warn'
  if (connected.value) return 'online'
  return connecting.value ? 'pending' : 'offline'
})
const canChat = computed(() => connected.value && !rejected.value && !disabled.value)

/** 给消息补上「日期分隔 / 是否本人 / 是否合并」等展示信息 */
const rows = computed(() => {
  const list = []
  let lastDay = ''
  let prev = null
  messages.value.forEach((message) => {
    const day = chatDayKey(message.time)
    const showDay = day !== lastDay
    lastDay = day
    const mine = message.type === 'CHAT' && String(message.id) === selfId.value
    const grouped =
      message.type === 'CHAT' &&
      !showDay &&
      prev?.type === 'CHAT' &&
      String(prev.id) === String(message.id) &&
      message.time - prev.time < GROUP_WINDOW
    list.push({
      ...message,
      showDay,
      dayLabel: showDay ? chatDayLabel(message.time) : '',
      timeLabel: chatTimeLabel(message.time),
      mine,
      grouped,
    })
    prev = message
  })
  return list
})

function scrollBottom(smooth = false) {
  nextTick(() => {
    const el = area.value
    if (!el) return
    el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
  })
}

function onScroll() {
  const el = area.value
  if (!el) return
  atBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 80
  if (atBottom.value) unread.value = 0
}

/** Markdown 是异步渲染的：转换完成会改变气泡高度，此时需要重新贴底 */
function onRendered() {
  if (atBottom.value) scrollBottom()
}

function push(payload) {
  messages.value.push({
    key: `m${++seed}`,
    type: payload.type || 'CHAT',
    content: payload.content || '',
    sender: payload.sender || '',
    image: payload.image || '',
    id: payload.id ?? '',
    time: Number(payload.time) || Date.now(),
  })
  if (messages.value.length > 200) messages.value.shift()
  if (atBottom.value) {
    scrollBottom()
    // Markdown / 图片渲染后高度会变化，补一次滚动
    setTimeout(() => atBottom.value && scrollBottom(), 180)
  } else {
    unread.value += 1
  }
}

function connect() {
  // 管理员关闭聊天室后不再建立连接
  if (!meta.chatEnabled) {
    disabled.value = '聊天室已被管理员关闭'
    connecting.value = false
    return
  }
  client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      connected.value = true
      connecting.value = false
      client.subscribe('/topic/public', (frame) => {
        let payload
        try {
          payload = JSON.parse(frame.body || '{}')
        } catch {
          return
        }
        if (typeof payload.count === 'number') online.value = payload.count
        if (Array.isArray(payload.members)) members.value = payload.members

        if (payload.type === 'DISABLED') {
          disabled.value = payload.content || '聊天室已被管理员关闭'
          connected.value = false
          client?.deactivate?.()
          return
        }

        if (payload.type === 'DUPLICATE') {
          // 广播消息，只有令牌匹配的窗口（即被拒绝的新窗口）需要响应
          if (payload.clientToken !== clientToken) return
          rejected.value = payload.content || '该账号已在其他窗口加入聊天室'
          connected.value = false
          client?.deactivate?.()
          return
        }
        push(payload)
      })
      // 身份由服务端从握手会话中取得，前端只上报令牌
      client.publish({
        destination: '/app/chat.addUser',
        body: JSON.stringify({ type: 'JOIN', clientToken }),
      })
    },
    onDisconnect: () => {
      connected.value = false
      connecting.value = false
    },
    onStompError: () => {
      connected.value = false
      connecting.value = false
    },
  })
  client.activate()
}

function send(content) {
  if (!canChat.value || !content) return
  client?.publish?.({
    destination: '/app/chat.sendMessage',
    body: JSON.stringify({ type: 'CHAT', content, clientToken }),
  })
  // 自己发出的消息尽快回到列表底部，便于确认已发送
  atBottom.value = true
  unread.value = 0
  scrollBottom(true)
  composer.value?.focus?.()
}

onBeforeUnmount(() => client?.deactivate?.())
connect()
</script>

<template>
  <div class="chat-shell flex flex-col ui-bg-page">
    <!-- 顶部栏：返回、房间状态、成员与主题 -->
    <header class="chat-topbar shrink-0">
      <div class="mx-auto flex h-14 w-full max-w-[1180px] items-center gap-2 px-3 sm:px-5">
        <RouterLink to="/chat" class="btn btn-ghost btn-square btn-sm" aria-label="返回聊天室介绍">
          <ArrowLeft class="size-4" />
        </RouterLink>

        <div class="min-w-0">
          <h1 class="flex items-center gap-1.5 truncate text-sm font-semibold ui-text-strong sm:text-base">
            <MessagesSquare class="size-4 shrink-0 ui-text-brand" />
            社区聊天室
          </h1>
          <p class="hidden truncate text-xs ui-text-muted sm:block">支持 Markdown，请遵守社区守则</p>
        </div>

        <span class="chat-status shrink-0" :class="`chat-status-${statusTone}`">
          <span class="chat-status-dot" />
          {{ statusText }}
        </span>

        <div class="ml-auto flex items-center gap-0.5">
          <RouterLink to="/chat" class="btn btn-ghost btn-sm gap-1.5 hidden md:inline-flex">
            <BookOpen class="size-4" />
            守则
          </RouterLink>
          <ThemeToggle />
          <RouterLink
            :to="`/user/${session.user?.userId}`"
            class="ml-1 flex items-center gap-2 rounded-full p-0.5 transition hover:opacity-80"
            :title="selfName"
          >
            <Avatar :src="session.user?.headUrl" :name="selfName" :size="30" />
          </RouterLink>
        </div>
      </div>
    </header>

    <div class="mx-auto flex min-h-0 w-full max-w-[1180px] flex-1">
      <!-- 消息区 + 输入区 -->
      <section class="flex min-w-0 flex-1 flex-col">
        <div v-if="disabled" class="alert alert-warning mx-3 mt-3 items-start gap-2 rounded-xl p-4 text-sm sm:mx-5">
          <TriangleAlert class="size-4 shrink-0" />
          <div>
            <p class="font-semibold">聊天室已关闭</p>
            <p class="mt-1">{{ disabled }}</p>
            <RouterLink to="/" class="btn btn-outline btn-sm mt-3">返回首页</RouterLink>
          </div>
        </div>

        <div v-if="rejected" class="alert alert-warning mx-3 mt-3 items-start gap-2 rounded-xl p-4 text-sm sm:mx-5">
          <TriangleAlert class="size-4 shrink-0" />
          <div>
            <p class="font-semibold">无法加入聊天室</p>
            <p class="mt-1">{{ rejected }}</p>
            <p class="mt-1 text-xs opacity-75">同一账号同一时间只能在一个窗口中聊天，请关闭其他窗口后刷新本页。</p>
            <button class="btn btn-outline btn-sm mt-3" type="button" @click="$router.go(0)">刷新重试</button>
          </div>
        </div>

        <div ref="area" class="chat-stream min-h-0 flex-1 overflow-y-auto px-3 py-4 sm:px-5" @scroll.passive="onScroll">
          <div class="chat-stream-inner">
            <div v-if="!messages.length && canChat" class="empty-state">
              <MessagesSquare class="size-10" />
              <p class="font-medium ui-text-base">还没有人说话</p>
              <p class="text-xs">来打个招呼吧，支持 **加粗**、`代码`、> 引用 等 Markdown 写法</p>
            </div>

            <div v-for="row in rows" :key="row.key">
              <div v-if="row.showDay" class="chat-day">
                <span>{{ row.dayLabel }}</span>
              </div>

              <p v-if="row.type !== 'CHAT'" class="chat-system">
                {{ row.sender }} {{ row.type === 'JOIN' ? '加入了聊天室' : '离开了聊天室' }}
              </p>

              <div v-else class="chat-row" :class="{ 'chat-row-mine': row.mine, 'chat-row-grouped': row.grouped }">
                <div class="chat-avatar">
                  <RouterLink v-if="!row.grouped" :to="`/user/${row.id}`" :title="row.sender">
                    <Avatar :src="row.image" :name="row.sender" :size="34" />
                  </RouterLink>
                </div>

                <div class="chat-main">
                  <div v-if="!row.grouped" class="chat-meta">
                    <span class="chat-name">{{ row.sender }}</span>
                    <span v-if="row.mine" class="chat-self">我</span>
                    <time class="chat-time">{{ row.timeLabel }}</time>
                  </div>
                  <div class="chat-bubble" :class="row.mine ? 'chat-bubble-mine' : 'chat-bubble-other'">
                    <ChatMarkdown :content="row.content" @rendered="onRendered" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-dock shrink-0">
          <button v-if="!atBottom" type="button" class="chat-jump" @click="scrollBottom(true)">
            <ArrowDown class="size-3.5" />
            {{ unread ? `${unread} 条新消息` : '回到底部' }}
          </button>

          <!-- 连接中 / 断开时在输入区上方给一句明确提示 -->
          <p v-if="!canChat && !disabled && !rejected" class="chat-hint">
            <Loader class="size-3.5 animate-spin" />
            {{ connecting ? '正在连接聊天室…' : '连接已断开，正在重试…' }}
          </p>

          <ChatComposer
            ref="composer"
            :disabled="!canChat"
            :max-length="4000"
            @send="send"
          />
        </div>
      </section>

      <!-- 宽屏右侧栏：在线成员 + 守则速览 -->
      <aside class="chat-aside hidden w-[268px] shrink-0 flex-col gap-3 overflow-y-auto p-4 lg:flex">
        <div class="surface p-3">
          <div class="mb-2 flex items-center justify-between">
            <span class="section-title"><Users class="size-4" />在线成员</span>
            <span class="text-xs tabular-nums ui-text-muted">{{ online }} 人</span>
          </div>
          <ul v-if="members.length" class="space-y-1">
            <li v-for="member in members" :key="member.id">
              <RouterLink
                :to="`/user/${member.id}`"
                class="flex items-center gap-2 rounded-lg px-1.5 py-1 text-sm transition hover:ui-bg-soft"
              >
                <Avatar :src="member.image" :name="member.name" :size="26" />
                <span class="min-w-0 flex-1 truncate ui-text-base">{{ member.name }}</span>
                <span v-if="String(member.id) === selfId" class="chip chip-xs">我</span>
              </RouterLink>
            </li>
          </ul>
          <p v-else class="px-1.5 py-2 text-xs ui-text-muted">暂无其他成员</p>
        </div>

        <div class="surface p-3">
          <span class="section-title mb-2"><BookOpen class="size-4" />聊天室守则</span>
          <ul class="mt-2 space-y-1.5 text-xs leading-relaxed ui-text-base">
            <li>友善交流，禁止攻击、辱骂与歧视。</li>
            <li>不要刷屏、发广告，或发布违规内容。</li>
            <li>请勿粘贴密钥、身份证号等敏感信息。</li>
          </ul>
          <RouterLink to="/chat" class="btn btn-ghost btn-xs mt-2 w-full">查看完整守则</RouterLink>
        </div>

        <div class="surface p-3">
          <span class="section-title mb-2"><Wifi class="size-4" />Markdown 速记</span>
          <ul class="mt-2 space-y-1.5 text-xs ui-text-base">
            <li class="flex items-center justify-between gap-2"><code class="chat-kbd">**粗体**</code><span class="ui-text-muted">加粗</span></li>
            <li class="flex items-center justify-between gap-2"><code class="chat-kbd">`代码`</code><span class="ui-text-muted">行内代码</span></li>
            <li class="flex items-center justify-between gap-2"><code class="chat-kbd">```js</code><span class="ui-text-muted">代码块</span></li>
            <li class="flex items-center justify-between gap-2"><code class="chat-kbd">&gt; 引用</code><span class="ui-text-muted">引用</span></li>
            <li class="flex items-center justify-between gap-2"><code class="chat-kbd">[标题](链接)</code><span class="ui-text-muted">链接</span></li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>
