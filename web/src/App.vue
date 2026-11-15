<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'
import { onLoadingChange } from '@/api/request'
import { useToastStore } from '@/stores/toast'
import { useMetaStore } from '@/stores/meta'
import { useThemeStore } from '@/stores/theme'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import ToastHost from '@/components/ToastHost.vue'
import AdminShell from '@/components/AdminShell.vue'

const route = useRoute()
const toast = useToastStore()
const meta = useMetaStore()
const theme = useThemeStore()

theme.init()

const loading = ref(false)
const stopLoading = onLoadingChange((value) => (loading.value = value))
onBeforeUnmount(() => stopLoading())

meta.load()

const bare = computed(() => !!route.meta.bare || route.name === 'admin-login')
const adminArea = computed(() => !!route.meta.adminArea && !bare.value)
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <div v-if="loading" class="fixed top-0 left-0 right-0 z-[100] h-0.5 bg-indigo-100">
      <div class="h-full w-1/3 animate-pulse bg-indigo-600" />
    </div>

    <template v-if="bare">
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in" appear>
          <component :is="Component" :key="route.path" />
        </Transition>
      </RouterView>
    </template>

    <template v-else-if="adminArea">
      <AdminShell>
        <RouterView v-slot="{ Component }">
          <Transition name="page" mode="out-in" appear>
            <component :is="Component" :key="route.path" />
          </Transition>
        </RouterView>
      </AdminShell>
    </template>

    <template v-else>
      <AppHeader />
      <main class="flex-1 py-6">
        <RouterView v-slot="{ Component }">
          <Transition name="page" mode="out-in" appear>
            <component :is="Component" :key="route.path" />
          </Transition>
        </RouterView>
      </main>
      <AppFooter />
    </template>

    <ToastHost :items="toast.items" @close="toast.remove" />
  </div>
</template>
