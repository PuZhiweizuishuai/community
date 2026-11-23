/**
 * 首屏主题引导：在 Vue 挂载前确定 data-theme，避免白天/夜间模式切换闪屏。
 * 独立成文件而不是内联 <script>，这样 CSP 可以保持 script-src 'self'（不含 unsafe-inline）。
 */
;(function () {
  try {
    var mode = localStorage.getItem('community-theme-mode') || 'system'
    var dark =
      mode === 'dark' ||
      (mode === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
    document.documentElement.style.colorScheme = dark ? 'dark' : 'light'
    var themeColor = document.querySelector('meta[name="theme-color"]')
    if (themeColor) themeColor.setAttribute('content', dark ? '#0f172a' : '#f6f7fb')
  } catch (e) {
    document.documentElement.setAttribute('data-theme', 'light')
  }
})()
