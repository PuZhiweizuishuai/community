import { version } from 'vditor/package.json'

/**
 * Vditor 主题配置的唯一来源。
 *
 * Vditor 的代码高亮主题与内容主题是运行时从 CDN 动态注入 <link> 的，
 * 仅传 preview.mode='dark' 并不会切换它，默认仍是 github（亮色）。
 * 这里显式给出白天/夜间两套 code theme + content theme，供编辑器与预览共用。
 */
export const VDITOR_CDN = `https://unpkg.com/vditor@${version}`
export const VDITOR_CONTENT_THEME_PATH = `${VDITOR_CDN}/dist/css/content-theme`

export const VDITOR_THEMES = {
  light: { editor: 'classic', content: 'light', code: 'github', previewMode: 'light' },
  dark: { editor: 'dark', content: 'dark', code: 'github-dark', previewMode: 'dark' },
}

export function vditorThemeOf(isDark) {
  return isDark ? VDITOR_THEMES.dark : VDITOR_THEMES.light
}

/** 传给 Vditor.preview / IOptions.preview 的主题相关配置 */
export function vditorPreviewTheme(isDark) {
  const t = vditorThemeOf(isDark)
  return {
    cdn: VDITOR_CDN,
    hljs: {
      enable: true,
      lineNumber: false,
      defaultLang: '',
      style: t.code,
    },
    theme: {
      current: t.content,
      path: VDITOR_CONTENT_THEME_PATH,
    },
  }
}
