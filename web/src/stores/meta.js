import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { metaApi } from '@/api'

const EMPTY = {
  hots: [],
  hotQuestions: [],
  hotUsers: [],
  tagCategories: [],
  classifications: [],
  sorts: [],
  topicTypes: [],
  advertisements: {},
  onlineCount: 0,
}

/**
 * 全站公共数据按用途分片懒加载：
 * load()            基础字典（分类/排序/话题分类/在线人数），启动时调用
 * loadHot()         热门标签 / 热门问题（侧边栏、登录页统计）
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
    loadHot,
    loadHotUsers,
    loadTagCategories,
    loadAds,
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