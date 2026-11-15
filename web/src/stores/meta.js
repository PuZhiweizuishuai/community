import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { metaApi } from '@/api'

const EMPTY = {
  hots: [],
  hotQuestions: [],
  hotUsers: [],
  tagCategories: [],
  tagClasses: {},
  classifications: [],
  sorts: [],
  topicTypes: [],
  advertisements: {},
  onlineCount: 0,
}

/**
 * 全站公共数据：侧边栏（热门标签 / 热门问题 / 广告轮播 / 新闻）、下拉选项。
 * 只拉取一次，多个页面共享。
 */
export const useMetaStore = defineStore('meta', () => {
  const data = ref({ ...EMPTY })
  const loaded = ref(false)
  let pending = null

  async function load(force = false) {
    if (loaded.value && !force) return data.value
    if (pending) return pending
    pending = metaApi
      .get()
      .then((result) => {
        data.value = { ...EMPTY, ...result }
        loaded.value = true
        return data.value
      })
      .catch(() => data.value)
      .finally(() => {
        pending = null
      })
    return pending
  }

  const ads = computed(() => data.value.advertisements || {})
  const hots = computed(() => data.value.hots || [])
  const hotQuestions = computed(() => data.value.hotQuestions || [])
  const hotUsers = computed(() => data.value.hotUsers || [])
  const classifications = computed(() => data.value.classifications || [])
  const sorts = computed(() => data.value.sorts || [])
  const topicTypes = computed(() => data.value.topicTypes || [])
  const tagCategories = computed(() => data.value.tagCategories || [])

  function adsFor(position) {
    const list = ads.value[position]
    return Array.isArray(list) ? list : Object.values(list || {})
  }

  return {
    data,
    loaded,
    load,
    ads,
    adsFor,
    hots,
    hotQuestions,
    hotUsers,
    classifications,
    sorts,
    topicTypes,
    tagCategories,
  }
})
