/**
 * 全局格式化工具：全系统统一调用，禁止各页面自行拼时间字符串
 */

/** 统一时间解析：兼容 2026-11-01T22:16:15（接口默认）、2026-11-01 22:16:15 与 Date */
function toDate(v) {
  if (v instanceof Date) return v
  const s = String(v).trim()
  let d = new Date(s)
  if (isNaN(d.getTime())) d = new Date(s.replace(/-/g, '/'))
  return d
}

/** 时间：yyyy-MM-dd HH:mm，空值统一显示占位符 */
export function fmtTime(v, withSecond = false) {
  if (!v && v !== 0) return '—'
  const d = toDate(v)
  if (isNaN(d.getTime())) return String(v)
  const p = (n) => String(n).padStart(2, '0')
  const base = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
  return withSecond ? `${base}:${p(d.getSeconds())}` : base
}

/** 日期：yyyy-MM-dd */
export function fmtDate(v) {
  if (!v) return '—'
  return fmtTime(v).slice(0, 10)
}

/** 相对时长：把两个时间差描述成「3小时12分」，用于时效展示 */
export function fmtGap(from, to) {
  if (!from || !to) return '—'
  const a = toDate(from).getTime()
  const b = toDate(to).getTime()
  if (isNaN(a) || isNaN(b)) return '—'
  let min = Math.round((b - a) / 60000)
  const neg = min < 0
  min = Math.abs(min)
  const day = Math.floor(min / 1440)
  const hour = Math.floor((min % 1440) / 60)
  const m = min % 60
  let s = day > 0 ? `${day}天${hour}小时` : hour > 0 ? `${hour}小时${m}分` : `${m}分`
  return (neg ? '剩余 ' : '已过 ') + s
}

/** 金额 / 数字 */
export function fmtNum(v, digits = 0) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  if (isNaN(n)) return String(v)
  return n.toFixed(digits).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/** 状态 → 标签色（五档，见 design-system 第 6 节） */
const STATUS_TAG = {
  待受理: 't-gray',
  已分派: 't-blue',
  处理中: 't-blue',
  待审核: 't-amber',
  待客户确认: 't-amber',
  待回访: 't-amber',
  已归档: 't-green',
  已退回: 't-red',
  已取消: 't-red',
  待下发: 't-gray',
  整改中: 't-blue',
  待验收: 't-amber',
  已验收: 't-green',
  已关闭: 't-gray',
  未处理: 't-red',
  已处理: 't-green',
  待审批: 't-amber',
  已通过: 't-green',
  已驳回: 't-red',
  已受理: 't-blue',
  通过: 't-green',
  驳回: 't-red',
  接受: 't-green',
  超期未确认: 't-red',
  未读: 't-amber',
  已读: 't-gray'
}
export function statusTag(s) {
  return STATUS_TAG[s] || 't-gray'
}

/** 严重级别 P1~P4 → 标签色 */
const LEVEL_TAG = { P1: 't-red', P2: 't-amber', P3: 't-blue', P4: 't-gray' }
export function levelTag(code) {
  return LEVEL_TAG[code] || 't-gray'
}

/** 共性聚类档位 HIGH/MID/LOW → 中文 + 标签色 */
export function clusterLabel(level) {
  const map = { HIGH: ['高相似', 't-red'], MID: ['中相似', 't-amber'], LOW: ['低相似', 't-gray'] }
  return map[level] || ['—', 't-gray']
}

/** 满意度星级 → 文本 */
export function satisfactionText(v) {
  if (!v && v !== 0) return '—'
  return `${v} 分`
}

/** 是否超期（deadline 早于当前且未完成） */
export function isOverdue(deadline, finish) {
  if (!deadline || finish) return false
  return new Date(String(deadline).replace(/-/g, '/')).getTime() < Date.now()
}
