<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Cloud, HardDrive, Pencil, Plus, PlugZap, Trash2, X } from '@lucide/vue'
import { adminApi } from '@/api'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
const storages = ref([])
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const editing = ref(false)

const emptyForm = () => ({
  id: null,
  name: '',
  endpoint: '',
  region: 'us-east-1',
  accessKey: '',
  secretKey: '',
  bucket: '',
  pathStyleAccess: true,
  urlExpireSeconds: 86400,
  enabled: true,
})
const form = reactive(emptyForm())

async function load() {
  loading.value = true
  try {
    storages.value = await adminApi.storages()
  } finally {
    loading.value = false
  }
}
onMounted(load)

const local = () => storages.value.find((s) => s.id === 0)
const objectStorages = () => storages.value.filter((s) => s.id !== 0)

function payloadOf(storage, enabled) {
  return {
    id: storage.id,
    name: storage.name,
    endpoint: storage.endpoint,
    region: storage.region,
    accessKey: storage.accessKey,
    secretKey: '',
    bucket: storage.bucket,
    pathStyleAccess: storage.pathStyleAccess,
    urlExpireSeconds: storage.urlExpireSeconds,
    enabled,
  }
}

async function toggle(storage) {
  await adminApi.saveStorage(payloadOf(storage, !storage.enabled))
  toast.success(storage.enabled ? '已停用' : '已启用')
  await load()
}

function openCreate() {
  Object.assign(form, emptyForm())
  editing.value = true
}

function openEdit(storage) {
  Object.assign(form, {
    id: storage.id,
    name: storage.name,
    endpoint: storage.endpoint || '',
    region: storage.region || 'us-east-1',
    accessKey: storage.accessKey || '',
    secretKey: '',
    bucket: storage.bucket || '',
    pathStyleAccess: storage.pathStyleAccess !== false,
    urlExpireSeconds: storage.urlExpireSeconds || 86400,
    enabled: storage.enabled !== false,
  })
  editing.value = true
}

async function save() {
  saving.value = true
  try {
    await adminApi.saveStorage({ ...form })
    toast.success('存储配置已保存')
    editing.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function test() {
  testing.value = true
  try {
    await adminApi.testStorage({ ...form })
    toast.success('连接成功')
  } finally {
    testing.value = false
  }
}

async function remove(storage) {
  if (!window.confirm(`确定删除对象存储「${storage.name}」吗？`)) return
  await adminApi.deleteStorage(storage.id)
  toast.success('已删除')
  await load()
}
</script>

<template>
  <div class="space-y-4">
    <div class="surface p-5">
      <div class="mb-4 flex flex-wrap items-center gap-2">
        <div class="mr-auto">
          <h2 class="section-title">存储配置</h2>
          <p class="mt-1 text-xs ui-text-muted">
            上传默认使用本地存储；启用多个存储时，每次上传随机选择一个。文件访问地址按存储 ID 区分：/api/file/{存储ID}/…
          </p>
        </div>
        <button class="btn btn-primary btn-sm" type="button" @click="openCreate">
          <Plus class="size-4" />新增对象存储
        </button>
      </div>

      <div v-if="loading" class="py-10 text-center ui-text-muted">加载中…</div>
      <template v-else>
        <!-- 本地存储 -->
        <div v-if="local()" class="mb-4 flex flex-wrap items-center gap-3 rounded-xl border ui-border p-4">
          <span class="flex size-10 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-500">
            <HardDrive class="size-5" />
          </span>
          <div class="mr-auto">
            <div class="flex items-center gap-2 font-medium">
              {{ local().name }}
              <span class="badge badge-sm badge-ghost">ID 0</span>
              <span class="badge badge-sm" :class="local().enabled ? 'badge-success' : 'badge-ghost'">
                {{ local().enabled ? '已启用' : '已停用' }}
              </span>
            </div>
            <p class="mt-0.5 text-xs ui-text-muted">服务器本地磁盘 · 已存 {{ local().fileCount }} 个文件</p>
          </div>
          <label class="flex cursor-pointer items-center gap-2 text-sm">
            <input type="checkbox" class="toggle toggle-sm toggle-primary" :checked="local().enabled" @change="toggle(local())" />
            启用上传
          </label>
        </div>

        <!-- 对象存储列表 -->
        <div class="space-y-3">
          <div
            v-for="storage in objectStorages()"
            :key="storage.id"
            class="flex flex-wrap items-center gap-3 rounded-xl border ui-border p-4"
          >
            <span class="flex size-10 items-center justify-center rounded-lg bg-sky-500/10 text-sky-500">
              <Cloud class="size-5" />
            </span>
            <div class="mr-auto min-w-0">
              <div class="flex flex-wrap items-center gap-2 font-medium">
                {{ storage.name }}
                <span class="badge badge-sm badge-ghost">ID {{ storage.id }}</span>
                <span class="badge badge-sm" :class="storage.enabled ? 'badge-success' : 'badge-ghost'">
                  {{ storage.enabled ? '已启用' : '已停用' }}
                </span>
              </div>
              <p class="mt-0.5 truncate text-xs ui-text-muted">
                {{ storage.endpoint }} · {{ storage.bucket }} · 已存 {{ storage.fileCount }} 个文件
              </p>
            </div>
            <label class="flex cursor-pointer items-center gap-2 text-sm">
              <input type="checkbox" class="toggle toggle-sm toggle-primary" :checked="storage.enabled" @change="toggle(storage)" />
              启用上传
            </label>
            <div class="flex items-center gap-1">
              <button class="btn btn-ghost btn-xs" type="button" @click="openEdit(storage)">
                <Pencil class="size-3.5" />编辑
              </button>
              <button class="btn btn-ghost btn-xs text-rose-500" type="button" @click="remove(storage)">
                <Trash2 class="size-3.5" />删除
              </button>
            </div>
          </div>
          <div v-if="!objectStorages().length" class="rounded-xl border border-dashed ui-border p-6 text-center text-sm ui-text-muted">
            还没有对象存储，点击右上角「新增对象存储」添加 S3 / MinIO 等 S3 兼容存储
          </div>
        </div>
      </template>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div v-if="editing" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4" @click.self="editing = false">
      <div class="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl ui-bg p-5 shadow-xl">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="section-title">{{ form.id == null ? '新增对象存储' : '编辑对象存储' }}</h3>
          <button class="btn btn-ghost btn-circle btn-sm" type="button" aria-label="关闭" @click="editing = false">
            <X class="size-4" />
          </button>
        </div>
        <form class="space-y-3" @submit.prevent="save">
          <label class="form-control w-full">
            <span class="mb-1 text-xs font-medium ui-text-muted">存储名称</span>
            <input v-model="form.name" class="input input-bordered input-sm w-full" placeholder="如：MinIO 主存储" required />
          </label>
          <label class="form-control w-full">
            <span class="mb-1 text-xs font-medium ui-text-muted">服务地址 Endpoint</span>
            <input v-model="form.endpoint" class="input input-bordered input-sm w-full" placeholder="http://127.0.0.1:9000" required />
          </label>
          <div class="grid grid-cols-2 gap-3">
            <label class="form-control w-full">
              <span class="mb-1 text-xs font-medium ui-text-muted">Region</span>
              <input v-model="form.region" class="input input-bordered input-sm w-full" placeholder="us-east-1" />
            </label>
            <label class="form-control w-full">
              <span class="mb-1 text-xs font-medium ui-text-muted">Bucket</span>
              <input v-model="form.bucket" class="input input-bordered input-sm w-full" placeholder="community" required />
            </label>
          </div>
          <label class="form-control w-full">
            <span class="mb-1 text-xs font-medium ui-text-muted">AccessKey</span>
            <input v-model="form.accessKey" class="input input-bordered input-sm w-full" autocomplete="off" required />
          </label>
          <label class="form-control w-full">
            <span class="mb-1 text-xs font-medium ui-text-muted">SecretKey</span>
            <input
              v-model="form.secretKey"
              type="password"
              class="input input-bordered input-sm w-full"
              autocomplete="new-password"
              :placeholder="form.id != null ? '留空则沿用已保存的密钥' : ''"
              :required="form.id == null"
            />
          </label>
          <div class="grid grid-cols-2 gap-3">
            <label class="form-control w-full">
              <span class="mb-1 text-xs font-medium ui-text-muted">预签名有效期（秒）</span>
              <input v-model.number="form.urlExpireSeconds" type="number" min="1" max="604800" class="input input-bordered input-sm w-full" />
            </label>
            <div class="flex flex-col justify-end gap-2 pb-1">
              <label class="flex cursor-pointer items-center gap-2 text-sm">
                <input v-model="form.pathStyleAccess" type="checkbox" class="checkbox checkbox-sm checkbox-primary" />
                Path-Style 访问
              </label>
              <label class="flex cursor-pointer items-center gap-2 text-sm">
                <input v-model="form.enabled" type="checkbox" class="checkbox checkbox-sm checkbox-primary" />
                启用上传
              </label>
            </div>
          </div>
          <p class="text-xs ui-text-muted">MinIO 等自建服务需勾选 Path-Style；AWS S3 可取消。预签名有效期最长 7 天（604800 秒）。</p>
          <div class="flex justify-end gap-2 pt-2">
            <button class="btn btn-ghost btn-sm" type="button" :disabled="testing" @click="test">
              <PlugZap class="size-4" />{{ testing ? '测试中…' : '测试连接' }}
            </button>
            <button class="btn btn-primary btn-sm" type="submit" :disabled="saving">
              {{ saving ? '保存中…' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>