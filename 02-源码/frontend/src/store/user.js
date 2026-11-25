import { reactive, computed } from 'vue'

/**
 * 登录态：后端 /auth/login 返回 { token, user, menus }
 * user 字段：id/username/realName/avatar/roleCode/roleName/orgName/dataScope
 * 六个角色：READER 读者 / CLERK 店员 / KEEPER 库管员 / BUYER 采购员 / OPS 运营管理员 / ADMIN 系统管理员
 */
function readUser() {
  try {
    const raw = localStorage.getItem('user')
    if (!raw || raw === 'undefined' || raw === 'null') return {}
    return JSON.parse(raw) || {}
  } catch (e) {
    return {}
  }
}

export const store = reactive({
  user: readUser(),
  menus: (() => {
    try {
      return JSON.parse(localStorage.getItem('menus') || '[]') || []
    } catch (e) {
      return []
    }
  })(),
  unread: 0
})

export function setLogin(data) {
  localStorage.setItem('token', data.token)
  localStorage.setItem('user', JSON.stringify(data.user || {}))
  localStorage.setItem('menus', JSON.stringify(data.menus || []))
  store.user = data.user || {}
  store.menus = data.menus || []
}

export function clearLogin() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('menus')
  store.user = {}
  store.menus = []
}

/** 角色判断：页面用它决定按钮显隐（后端仍会二次校验） */
export function hasRole(...roles) {
  return roles.includes(store.user.roleCode)
}

export const isReader = computed(() => store.user.roleCode === 'READER')
export const roleCode = computed(() => store.user.roleCode || '')

/** 角色简称，顶栏与徽标展示用 */
export const roleShort = computed(() => {
  const map = {
    READER: '读者',
    CLERK: '书店店员',
    KEEPER: '库管员',
    BUYER: '采购员',
    OPS: '运营管理员',
    ADMIN: '系统管理员'
  }
  return map[store.user.roleCode] || store.user.roleName || ''
})

/** 侧栏底色按角色区分，让演示时一眼看出当前身份 */
export const sideColor = computed(() => {
  const map = { ADMIN: '#22314A', OPS: '#1F3A56', BUYER: '#23453C', KEEPER: '#3A3550', CLERK: '#4A3327' }
  return map[store.user.roleCode] || '#2C456D'
})
