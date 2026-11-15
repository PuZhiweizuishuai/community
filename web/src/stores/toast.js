import { ref } from 'vue'
import { defineStore } from 'pinia'

let seed = 0

export const useToastStore = defineStore('toast', () => {
  const items = ref([])

  function push(message, type = 'info', timeout = 3200) {
    const id = ++seed
    items.value.push({ id, message, type })
    if (timeout > 0) {
      setTimeout(() => remove(id), timeout)
    }
    return id
  }

  function remove(id) {
    items.value = items.value.filter((item) => item.id !== id)
  }

  const success = (message) => push(message, 'success')
  const error = (message) => push(message, 'error', 4200)
  const info = (message) => push(message, 'info')
  const warning = (message) => push(message, 'warning')

  return { items, push, remove, success, error, info, warning }
})
