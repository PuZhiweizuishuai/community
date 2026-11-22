/**
 * 前端表单校验规则（与后端 StringUtil.PASSWORD_REGEX / PASSWORD_MESSAGE 保持一致）。
 *
 * 密码：8~30 位，必须同时包含字母与数字，允许常用符号。
 * 不要只校验长度——后端会按同一规则拒绝纯数字/纯字母口令。
 */

export const PASSWORD_HINT = '密码需为 8~30 位，且同时包含字母和数字'

const PASSWORD_RE =
  /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d`~!@#$%^&*()+=|{}':;,\[\]<>/?~！@#￥%……&*（）——+|{}【】‘；：”“'。，、？]{8,30}$/

/** 返回空串表示通过，否则返回提示语 */
export function checkPassword(password) {
  if (!password) return '密码不能为空'
  if (!PASSWORD_RE.test(password)) return PASSWORD_HINT
  return ''
}
