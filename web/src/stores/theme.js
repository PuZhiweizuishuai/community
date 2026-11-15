import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const THEME_KEY = 'community-theme-mode'
const MODES = ['system', 'light', 'dark']

function readMode() {
  try {
    const saved = localStorage.getItem(THEME_KEY)
    return MODES.includes(saved) ? saved : 'system'
  } catch {
    return 'system'
  }
}

/**
 * 主题：默认跟随系统 prefers-color-scheme，用户可手动切换白天/夜间，
 * 选择写入 localStorage，下次打开自动恢复。
 */
export const useThemeStore = defineStore('theme', () => {
  const mode = ref(readMode())
  const systemDark = ref(
    typeof window !== 'undefined' && window.matchMedia
      ? window.matchMedia('(prefers-color-scheme: dark)').matches
      : false,
  )
  const resolved = computed(() =>
    mode.value === 'system' ? (systemDark.value ? 'dark' : 'light') : mode.value,
  )
  const isDark = computed(() => resolved.value === 'dark')

  let media = null
  const onSystemChange = (event) => (systemDark.value = event.matches)

  function apply() {
    if (typeof document === 'undefined') return
    document.documentElement.dataset.theme = resolved.value
    document.documentElement.style.colorScheme = resolved.value
  }

  function init() {
    apply()
    if (typeof window !== 'undefined' && window.matchMedia && !media) {
      media = window.matchMedia('(prefers-color-scheme: dark)')
      media.addEventListener('change', onSystemChange)
    }
  }

  function setMode(next) {
    if (!MODES.includes(next)) return
    mode.value = next
    try {
      localStorage.setItem(THEME_KEY, next)
    } catch {
      /* 隐私模式下忽略 */
    }
    apply()
  }

  /** 一键在白天/夜间之间切换（切换后不再跟随系统） */
  function toggle() {
    setMode(isDark.value ? 'light' : 'dark')
  }

  return { mode, systemDark, resolved, isDark, init, apply, setMode, toggle }
})
