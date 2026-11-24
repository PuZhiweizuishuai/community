<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { Check, Clock, ImagePlus, Loader, Settings, Trash2, UserPlus, Users, X } from '@lucide/vue'
import { assetUrl, discoverApi, settingApi } from '@/api'
import { useConfirmStore } from '@/stores/confirm'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/Avatar.vue'
import ImageCropper from '@/components/ImageCropper.vue'
import Pagination from '@/components/Pagination.vue'
import { timeAgo } from '@/utils/format'
import { dataUrlToBlob, ensureDecodable, readAsDataUrl, validateImageFile } from '@/utils/image'

const props = defineProps({
  topic: { type: Object, required: true },
  access: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['close', 'updated'])

const toast = useToastStore()
const confirm = useConfirmStore()

const tab = ref('applications')
const page = ref(1)
const list = ref(null)
const loading = ref(false)
const inviteCode = ref('')
const inviting = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const reviewing = ref(false)

const canManageSettings = computed(() => !!props.access?.isOwner || !!props.access?.isAdmin)
const canRemove = computed(() => canManageSettings.value)

const settings = ref({
  simpleDesc: props.topic.simpleDesc || '',
  image: props.topic.image || '/image/topicicon/default.svg',
  visibility: Number(props.topic.visibility || 0),
})
const savingSettings = ref(false)
// 图标上传：选图 → 1:1 裁剪 → 上传
const iconInput = ref(null)
const iconCrop = ref(null)
const iconUploading = ref(false)

const status = computed(() => (tab.value === 'applications' ? 0 : 1))

async function load() {
  loading.value = true
  try {
    list.value = await discoverApi.topicMembers(props.topic.id, {
      status: status.value,
      joinType: tab.value === 'applications' ? 'apply' : undefined,
      page: page.value,
      size: 10,
    })
  } finally {
    loading.value = false
  }
}

function switchTab(next) {
  if (tab.value === next) return
  tab.value = next
  page.value = 1
}

function changePage(next) {
  page.value = Number(next) || 1
  load()
}

async function invite() {
  const userCode = inviteCode.value.trim()
  if (!userCode) return toast.warning('请填写用户登录账号')
  inviting.value = true
  try {
    await discoverApi.inviteTopicMember(props.topic.id, { userCode })
    toast.success('邀请已发送，等待对方接受')
    inviteCode.value = ''
    await load()
    emit('updated')
  } finally {
    inviting.value = false
  }
}

async function approve(member) {
  await discoverApi.reviewTopicApplication(props.topic.id, member.id, { approve: true })
  toast.success('已通过加入申请')
  await load()
  emit('updated')
}

function openReject(member) {
  rejectTarget.value = member
  rejectReason.value = ''
}

async function confirmReject() {
  if (!rejectTarget.value || reviewing.value) return
  reviewing.value = true
  try {
    await discoverApi.reviewTopicApplication(props.topic.id, rejectTarget.value.id, {
      approve: false,
      reason: rejectReason.value.trim(),
    })
    toast.success('已拒绝加入申请')
    rejectTarget.value = null
    await load()
    emit('updated')
  } finally {
    reviewing.value = false
  }
}

async function remove(member) {
  const ok = await confirm.ask({
    title: '移除成员',
    message: `确定将「${member.userName || member.userCode}」移出话题？`,
    confirmText: '移除',
    danger: true,
  })
  if (!ok) return
  await discoverApi.removeTopicMember(props.topic.id, member.userId)
  toast.success('已移除成员')
  await load()
  emit('updated')
}

async function pickIcon(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  const invalid = validateImageFile(file)
  if (invalid) return toast.warning(invalid)
  const src = await readAsDataUrl(file)
  try {
    await ensureDecodable(src)
  } catch {
    return toast.warning('这张图片无法解析，请换一张 JPG / PNG 图片')
  }
  iconCrop.value = { src, title: '裁剪话题图标（1:1）' }
}

async function confirmIcon(dataUrl) {
  iconCrop.value = null
  iconUploading.value = true
  try {
    const formData = new FormData()
    formData.append('editormd-image-file', dataUrlToBlob(dataUrl), 'topic-icon.jpeg')
    formData.append('type', 'topic')
    const result = await settingApi.uploadImage(formData)
    if (!result?.url) return toast.error('图标上传失败，请重试')
    settings.value.image = result.url
    toast.success('图标已上传，记得保存设置')
  } catch {
    toast.error('图标上传失败，请重试')
  } finally {
    iconUploading.value = false
  }
}

async function saveSettings() {
  savingSettings.value = true
  try {
    await discoverApi.updateTopicSettings(props.topic.id, {
      simpleDesc: settings.value.simpleDesc.trim(),
      image: settings.value.image.trim(),
      visibility: Number(settings.value.visibility),
    })
    toast.success('话题设置已更新')
    emit('updated')
  } finally {
    savingSettings.value = false
  }
}

watch(status, () => {
  page.value = 1
  load()
})
onMounted(load)
</script>

<template>
  <dialog class="modal modal-open">
    <div class="modal-box max-w-3xl">
      <div class="mb-4 flex items-start justify-between gap-3">
        <div>
          <h3 class="text-base font-semibold">话题管理 · {{ topic.title }}</h3>
          <p class="mt-0.5 text-xs ui-text-muted">审核加入申请、邀请成员、管理话题可见性</p>
        </div>
        <button class="btn btn-ghost btn-circle btn-xs" type="button" aria-label="关闭" @click="emit('close')">
          <X class="size-4" />
        </button>
      </div>

      <div class="tabs tabs-bordered mb-4">
        <button
          class="tab"
          :class="tab === 'applications' ? 'tab-active font-medium ui-text-brand' : ''"
          type="button"
          @click="switchTab('applications')"
        >
          <Clock class="size-4" />加入申请
        </button>
        <button
          class="tab"
          :class="tab === 'members' ? 'tab-active font-medium ui-text-brand' : ''"
          type="button"
          @click="switchTab('members')"
        >
          <Users class="size-4" />成员
        </button>
        <button
          v-if="canManageSettings"
          class="tab"
          :class="tab === 'settings' ? 'tab-active font-medium ui-text-brand' : ''"
          type="button"
          @click="switchTab('settings')"
        >
          <Settings class="size-4" />设置
        </button>
      </div>

      <template v-if="tab !== 'settings'">
        <div class="mb-3 flex flex-wrap items-center gap-2">
          <input
            v-model="inviteCode"
            class="input input-bordered input-sm min-w-0 flex-1"
            placeholder="输入用户登录账号，邀请加入话题"
            @keydown.enter.prevent="invite"
          />
          <button class="btn btn-primary btn-sm" type="button" :disabled="inviting" @click="invite">
            <UserPlus class="size-4" />{{ inviting ? '发送中…' : '邀请' }}
          </button>
        </div>

        <div v-if="loading && !list" class="empty-state">加载中…</div>
        <div v-else-if="!list?.data?.length" class="empty-state">
          {{ tab === 'applications' ? '暂无待审核申请' : '暂无成员' }}
        </div>
        <ul v-else class="space-y-2">
          <li
            v-for="member in list.data"
            :key="member.id"
            class="flex flex-wrap items-center gap-2 rounded-lg border ui-border p-2.5"
          >
            <Avatar :src="member.headUrl" :name="member.userName" :size="30" />
            <div class="min-w-0 flex-1">
              <RouterLink
                v-if="member.userCode"
                :to="`/user/${member.userCode}`"
                class="text-sm font-medium ui-text-strong hover:text-indigo-600"
              >
                {{ member.userName || member.userCode }}
              </RouterLink>
              <p v-else class="text-sm ui-text-strong">已注销用户</p>
              <p class="text-xs ui-text-muted">
                {{ member.joinType === 'invite' ? '邀请' : '申请' }} · {{ timeAgo(member.createTime) || '刚刚' }}
                <span v-if="member.message"> · {{ member.message }}</span>
              </p>
            </div>
            <template v-if="tab === 'applications'">
              <button class="btn btn-primary btn-xs" type="button" @click="approve(member)">
                <Check class="size-3.5" />通过
              </button>
              <button class="btn btn-ghost btn-xs text-rose-500" type="button" @click="openReject(member)">拒绝</button>
            </template>
            <button
              v-else-if="canRemove"
              class="btn btn-ghost btn-xs text-rose-500"
              type="button"
              @click="remove(member)"
            >
              <Trash2 class="size-3.5" />移除
            </button>
          </li>
        </ul>
        <Pagination :pagination="list" @change="changePage" />
      </template>

      <template v-else>
        <div class="space-y-4">
          <label class="form-control">
            <span class="label-text mb-1 font-medium">话题可见性</span>
            <select v-model.number="settings.visibility" class="select select-bordered select-sm">
              <option :value="0">公开</option>
              <option :value="1">私有 · 可申请（内容仅成员可见）</option>
              <option :value="2">私有 · 仅邀请（话题与内容均不可见）</option>
            </select>
            <span class="label-text-alt mt-1 ui-text-muted">改为私有后，非成员将立即无法查看话题内容。</span>
          </label>

          <div class="form-control">
            <span class="label-text mb-1 font-medium">话题图标</span>
            <div class="flex items-center gap-3">
              <div class="relative shrink-0">
                <img :src="assetUrl(settings.image)" alt="话题图标" class="h-14 w-14 rounded-lg border ui-border object-cover" />
                <span
                  v-if="iconUploading"
                  class="absolute inset-0 flex items-center justify-center rounded-lg bg-slate-900/55"
                >
                  <Loader class="size-4 animate-spin text-white" />
                </span>
              </div>
              <div class="min-w-0 flex-1">
                <button
                  class="btn btn-outline btn-xs"
                  type="button"
                  :disabled="iconUploading"
                  @click="iconInput?.click()"
                >
                  <ImagePlus class="size-3.5" />
                  {{ iconUploading ? '上传中…' : '选择图片并裁剪' }}
                </button>
                <input ref="iconInput" type="file" accept="image/*" class="hidden" @change="pickIcon" />
                <input v-model="settings.image" class="input input-bordered input-xs mt-2 w-full" placeholder="图标地址" />
              </div>
            </div>
            <span class="label-text-alt mt-1 ui-text-muted">
              建议 1:1 正方形，选择图片后可拖动、缩放裁剪，确认后才会上传；保存设置后生效。
            </span>
          </div>

          <label class="form-control">
            <span class="label-text mb-1 font-medium">话题简介</span>
            <textarea v-model="settings.simpleDesc" maxlength="500" rows="4" class="textarea textarea-bordered" />
          </label>

          <div class="flex justify-end">
            <button class="btn btn-primary btn-sm" type="button" :disabled="savingSettings" @click="saveSettings">
              {{ savingSettings ? '保存中…' : '保存设置' }}
            </button>
          </div>
        </div>
      </template>

      <div v-if="rejectTarget" class="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-3">
        <p class="text-sm font-medium text-rose-700">
          拒绝「{{ rejectTarget.userName || rejectTarget.userCode }}」的加入申请
        </p>
        <textarea
          v-model="rejectReason"
          rows="2"
          maxlength="200"
          class="textarea textarea-bordered textarea-sm mt-2 w-full"
          placeholder="填写拒绝原因（可选），将通过站内消息通知对方"
        />
        <div class="mt-2 flex justify-end gap-2">
          <button class="btn btn-ghost btn-xs" type="button" @click="rejectTarget = null">取消</button>
          <button class="btn btn-error btn-xs" type="button" :disabled="reviewing" @click="confirmReject">
            {{ reviewing ? '处理中…' : '确认拒绝' }}
          </button>
        </div>
      </div>
    </div>
  </dialog>

  <ImageCropper
    v-if="iconCrop"
    :src="iconCrop.src"
    :aspect="1"
    :output-width="400"
    :output-height="400"
    :title="iconCrop.title"
    @confirm="confirmIcon"
    @cancel="iconCrop = null"
  />
</template>
