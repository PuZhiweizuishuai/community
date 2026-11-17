import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useConfirmStore = defineStore('confirm', () => {
  const request = ref(null)

  function ask(options = {}) {
    request.value?.resolve(false)
    return new Promise((resolve) => {
      request.value = {
        title: '确认操作',
        message: '',
        confirmText: '确定',
        cancelText: '取消',
        danger: false,
        ...options,
        resolve,
      }
    })
  }

  function settle(result) {
    const current = request.value
    if (!current) return
    request.value = null
    current.resolve(result)
  }

  return { request, ask, settle }
})
