/**
 * 图片相关的浏览器端小工具，供裁剪组件 / 上传流程共用。
 */

/** 读取本地文件为 dataURL（裁剪组件的预览源） */
export function readAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

/** dataURL 转 Blob，便于塞进 FormData 走统一上传接口 */
export function dataUrlToBlob(dataUrl) {
  const [meta, base64] = dataUrl.split(',')
  const mime = /:(.*?);/.exec(meta)?.[1] || 'image/jpeg'
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return new Blob([bytes], { type: mime })
}

/** 选择图片前的统一校验：类型必须是图片，体积不超过 maxMb */
export function validateImageFile(file, maxMb = 10) {
  if (!file) return '请选择图片文件'
  if (!file.type.startsWith('image/')) return '请选择图片文件（JPG / PNG / WebP）'
  if (file.size > maxMb * 1024 * 1024) return `图片不能超过 ${maxMb}MB`
  return ''
}

/**
 * 确认浏览器能真正解码这张图（HEIC 等格式会失败）。
 * 不能解码时裁剪组件会一直停在加载态，所以选图阶段就拦掉。
 */
export function ensureDecodable(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('decode failed'))
    img.src = src
  })
}
