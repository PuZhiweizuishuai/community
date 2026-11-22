/**
 * Vditor 与严格 CSP 的兼容层。
 *
 * Vditor 给代码块注入的「复制」按钮是内联写法：
 * `<span onmouseover="..." onclick="select();execCommand('copy')">`。
 * 站点 CSP 为 `script-src 'self'`（不含 unsafe-inline），浏览器会拦截这类内联事件处理器，
 * 表现为控制台报错、复制按钮失效。
 *
 * 这里剥掉内联属性，改用 `data-copy-code` + 事件委托复刻同样的行为，
 * 既不放宽 CSP，也不牺牲「复制代码」体验。
 */

function stripInlineHandlers(root) {
  root.querySelectorAll('span[onclick], span[onmouseover]').forEach((span) => {
    span.removeAttribute('onclick')
    span.removeAttribute('onmouseover')
    span.setAttribute('aria-label', '复制')
    span.dataset.copyCode = ''
  })
}

/** 处理一次已渲染的内容（MarkdownView 的 after 回调里调用即可） */
export function hardenVditorInlineHandlers(root) {
  if (root) stripInlineHandlers(root)
}

/**
 * 持续处理动态渲染的内容（编辑器预览面板会反复重绘）。
 * 用 rAF 节流，避免每次 DOM 变更都全量查询。
 * @returns {() => void} 取消监听的函数
 */
export function observeVditorInlineHandlers(root) {
  if (!root) return () => {}
  let scheduled = false
  const observer = new MutationObserver(() => {
    if (scheduled) return
    scheduled = true
    requestAnimationFrame(() => {
      scheduled = false
      stripInlineHandlers(root)
    })
  })
  observer.observe(root, { childList: true, subtree: true })
  return () => observer.disconnect()
}

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    // http 等非安全上下文没有 clipboard API，退回选区复制
    const area = document.createElement('textarea')
    area.value = text
    area.setAttribute('readonly', '')
    area.style.position = 'fixed'
    area.style.opacity = '0'
    document.body.appendChild(area)
    area.select()
    document.execCommand('copy')
    document.body.removeChild(area)
  }
}

/**
 * 复制按钮的事件委托：点击 `[data-copy-code]` 时复制其前一个兄弟节点（code 元素）的文本。
 * @returns {() => void} 取消监听的函数
 */
export function bindVditorCopyDelegate(root) {
  if (!root) return () => {}
  const onClick = async (event) => {
    const btn = event.target?.closest?.('[data-copy-code]')
    if (!btn) return
    const source = btn.previousElementSibling
    if (!source) return
    // Vditor 的代码块结构是 <pre><textarea>代码</textarea><span>复制</span></pre>：
    // textarea 的内容在 value 里，textContent 是空的
    const text = typeof source.value === 'string' ? source.value : source.textContent || ''
    await copyText(text)
    btn.setAttribute('aria-label', '已复制')
    setTimeout(() => btn.setAttribute('aria-label', '复制'), 1500)
  }
  root.addEventListener('click', onClick)
  return () => root.removeEventListener('click', onClick)
}
