import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { metaApi } from '@/api'

const EMPTY = {
  hots: [],
  hotTopics: [],
  hotQuestions: [],
  hotUsers: [],
  tagCategories: [],
  classifications: [],
  sorts: [],
  topicTypes: [],
  advertisements: {},
  onlineCount: 0,
  chatEnabled: true,
  reviewEnabled: false,
}

/**
 * 全站公共数据按用途分片懒加载：
 * load()            基础字典（分类/排序/话题分类/在线人数），启动时调用
 * loadHot()         热门话题 / 热门标签 / 热门问题（侧边栏、登录页统计）
 * loadHotUsers()    热门用户（仅用户列表页）
 * loadTagCategories() 标签分类（仅发布页）
 * loadAds(position) 按广告位拉取广告
 * 每个分片只请求一次，多个组件共享并自动去重并发请求。
 */
export const useMetaStore = defineStore('meta', () => {
  const data = ref({ ...EMPTY })
  const loaded = ref(false)
  const done = new Set()
  const pending = new Map()

  function section(key, request, merge, force = false) {
    if (done.has(key) && !force) return Promise.resolve(data.value)
    if (pending.has(key)) return pending.get(key)
    const task = request()
      .then((result) => {
        merge(result)
        done.add(key)
        return data.value
      })
      .catch(() => data.value)
      .finally(() => {
        pending.delete(key)
      })
    pending.set(key, task)
    return task
  }

  function load(force = false) {
    return section('base', () => metaApi.get(), (result) => {
      data.value = { ...data.value, ...result }
      loaded.value = true
    }, force)
  }

  function loadHot() {
    return section('hot', () => metaApi.hot(), (result) => {
      data.value = {
        ...data.value,
        hots: result?.hots || [],
        hotTopics: result?.hotTopics || [],
        hotQuestions: result?.hotQuestions || [],
      }
    })
  }

  function loadHotUsers() {
    return section('hotUsers', () => metaApi.hotUsers(), (result) => {
      data.value = { ...data.value, hotUsers: result || [] }
    })
  }

  function loadTagCategories() {
    return section('tagCategories', () => metaApi.tagCategories(), (result) => {
      data.value = { ...data.value, tagCategories: result || [] }
    })
  }

  function loadAds(position) {
    return section(`ads:${position}`, () => metaApi.ads(position), (result) => {
      data.value = {
        ...data.value,
        advertisements: { ...data.value.advertisements, [position]: result || [] },
      }
    })
  }

  const ads = computed(() => data.value.advertisements || {})
  const hots = computed(() => data.value.hots || [])
  const hotTopics = computed(() => data.value.hotTopics || [])
  const hotQuestions = computed(() => data.value.hotQuestions || [])
  const hotUsers = computed(() => data.value.hotUsers || [])
  const classifications = computed(() => data.value.classifications || [])
  const sorts = computed(() => data.value.sorts || [])
  const topicTypes = computed(() => data.value.topicTypes || [])
  const tagCategories = computed(() => data.value.tagCategories || [])
  // 聊天室开关：管理员关闭后隐藏入口并拦截路由；默认开启
  const chatEnabled = computed(() => data.value.chatEnabled !== false)
  // 内容审核开关：开启后发布页/评论框提示「提交后需审核」
  const reviewEnabled = computed(() => data.value.reviewEnabled === true)

  /** 后台保存配置后同步本地状态，头部入口立即更新 */
  function setChatEnabled(enabled) {
    data.value = { ...data.value, chatEnabled: !!enabled }
  }

  function adsFor(position) {
    const list = ads.value[position]
    return Array.isArray(list) ? list : Object.values(list || {})
  }

  /** 问题类型 code → 名称；后台/审核等页面统一用它展示，避免直接显示数字 */
  function classificationLabel(code) {
    if (code === null || code === undefined || code === '') return ''
    const item = classifications.value.find((c) => Number(c.value) === Number(code))
    return item?.label || String(code)
  }

  return {
    data,
    loaded,
    load,
    loadHot,
    loadHotUsers,
    loadTagCategories,
    loadAds,
    ads,
    adsFor,
    hots,
    hotTopics,
    hotQuestions,
    hotUsers,
    classifications,
    sorts,
    topicTypes,
    tagCategories,
    classificationLabel,
    chatEnabled,
    setChatEnabled,
    reviewEnabled,
  }
})