<script setup>
import { nextTick, onBeforeUnmount, ref } from 'vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { ArrowLeft, Send, TriangleAlert } from '@lucide/vue'
import { useSessionStore } from '@/stores/session'
import Avatar from '@/components/Avatar.vue'

const session = useSessionStore()
const messages = ref([])
const text = ref('')
const online = ref(0)
const connected = ref(false)
const rejected = ref('')
const area = ref(null)
let client = null

// 每个窗口一个令牌：服务端把「重复登录」提示原样带回，只有被拒绝的窗口会处理
const clientToken = `${Date.now()}-${Math.random().toString(36).slice(2)}`

function scrollBottom() {
  nextTick(() => {
    if (area.value) area.value.scrollTop = area.value.scrollHeight
  })
}

function push(message) {
  messages.value.push({ ...message, key: `${Date.now()}-${Math.random()}` })
  if (messages.value.length > 200) messages.value.shift()
  scrollBottom()
}

function identity() {
  return {
    sender: session.user.userName,
    image: session.user.headUrl,
    id: session.user.userId,
    clientToken,
  }
}

function connect() {
  client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      connected.value = true
      client.subscribe('/topic/public', (frame) => {
        let payload
        try {
          payload = JSON.parse(frame.body || '{}')
        } catch {
          return
        }
        if (typeof payload.count === 'number') online.value = payload.count

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
      client.publish({
        destination: '/app/chat.addUser',
        body: JSON.stringify({ ...identity(), type: 'JOIN' }),
      })
    },
    onDisconnect: () => (connected.value = false),
    onStompError: () => (connected.value = false),
  })
  client.activate()
}

function send() {
  const content = text.value.trim()
  if (!content || !connected.value || rejected.value) return
  client.publish({
    destination: '/app/chat.sendMessage',
    body: JSON.stringify({ ...identity(), content, type: 'CHAT' }),
  })
  text.value = ''
}

onBeforeUnmount(() => client?.deactivate?.())
connect()
</script>

<template>
  <div class="flex h-screen flex-col bg-slate-50">
    <header class="flex h-14 items-center justify-between border-b border-slate-200 bg-white px-4">
      <div class="flex items-center gap-2">
        <RouterLink to="/chat" class="btn btn-ghost btn-sm" aria-label="返回"><ArrowLeft class="size-4" /></RouterLink>
        <h1 class="font-semibold text-slate-800">社区聊天室</h1>
        <span class="badge badge-sm" :class="connected ? 'badge-success' : 'badge-ghost'">
          {{ connected ? `在线 ${online}` : rejected ? '已被拒绝' : '连接中…' }}
        </span>
      </div>
      <RouterLink :to="`/user/${session.user?.userId}`" class="flex items-center gap-2 text-sm text-slate-600">
        <Avatar :src="session.user?.headUrl" :name="session.user?.userName" :size="28" />
        {{ session.user?.userName }}
      </RouterLink>
    </header>

    <div v-if="rejected" class="mx-4 mt-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
      <p class="flex items-center gap-1.5 font-semibold"><TriangleAlert class="size-4" />无法加入聊天室</p>
      <p class="mt-1">{{ rejected }}</p>
      <p class="mt-1 text-xs opacity-75">同一账号同一时间只能在一个窗口中聊天，请关闭其他窗口后刷新本页。</p>
      <button class="btn btn-sm btn-outline mt-3" type="button" @click="$router.go(0)">刷新重试</button>
    </div>

    <div ref="area" class="flex-1 space-y-3 overflow-y-auto p-4">
      <p v-if="!messages.length && !rejected" class="py-10 text-center text-sm text-slate-400">还没有消息，说点什么吧～</p>
      <div v-for="message in messages" :key="message.key">
        <div v-if="message.type !== 'CHAT'" class="text-center text-xs text-slate-400">
          {{ message.sender }} {{ message.type === 'JOIN' ? '加入了聊天室' : '离开了聊天室' }}
        </div>
        <div v-else class="flex items-start gap-2" :class="message.sender === session.user?.userName ? 'flex-row-reverse' : ''">
          <RouterLink :to="`/user/${message.id}`">
            <Avatar :src="message.image" :name="message.sender" :size="32" />
          </RouterLink>
          <div class="max-w-[70%]">
            <p class="mb-0.5 text-xs text-slate-400" :class="message.sender === session.user?.userName ? 'text-right' : ''">{{ message.sender }}</p>
            <!-- 背景与文字必须用互斥的类名：同时写 bg-white 与 bg-indigo-600、
                 text-slate-700 与 text-white 会因工具类顺序导致白字白底而看不见 -->
            <div
              class="chat-bubble rounded-xl px-3 py-2 text-sm shadow-sm"
              :class="message.sender === session.user?.userName ? 'chat-bubble-mine' : 'chat-bubble-other'"
            >
              {{ message.content }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <form class="flex items-center gap-2 border-t border-slate-200 bg-white p-3" @submit.prevent="send">
      <input v-model="text" class="input input-bordered flex-1 focus:outline-none" placeholder="输入消息，回车发送…" :disabled="!connected || !!rejected" />
      <button class="btn btn-primary" type="submit" :disabled="!connected || !!rejected || !text.trim()"><Send class="size-4" />发送</button>
    </form>
  </div>
</template>
