import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一请求封装：
 * - 自动带 token；后端返回 code !== 200 时统一提示并抛错，页面不必各写一遍
 * - 401 清登录态回登录页；403 提示无权限
 * - 头像、封面等图片路径直接用后端返回的相对路径（由 vite 代理 /uploads）
 */
const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

http.interceptors.response.use(
  (res) => {
    const d = res.data
    if (d && typeof d.code !== 'undefined' && d.code !== 200) {
      if (d.code === 401) {
        localStorage.clear()
        if (!location.hash.includes('/login')) location.hash = '#/login'
      }
      ElMessage.error(d.message || '请求失败')
      return Promise.reject(new Error(d.message || '请求失败'))
    }
    // 后端分页统一返回 { total, page, size, records }，这里补一个 rows 别名，
    // 页面里只认 rows，避免每个列表页各写一套兼容逻辑
    const data = d && d.data
    if (data && !Array.isArray(data) && Array.isArray(data.records) && !data.rows) {
      data.rows = data.records
    }
    return data
  },
  (err) => {
    // 页面隐藏（切标签/跳转）时浏览器会中断请求，属正常，不弹窗
    if (document.visibilityState === 'hidden') return Promise.reject(err)
    if (err.response && err.response.status === 403) {
      ElMessage.error('当前角色无此操作权限')
    } else if (!err.message?.includes('Network Error')) {
      ElMessage.error(err.response?.data?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(err)
  }
)

export const api = {
  // 认证与个人
  login: (data) => http.post('/auth/login', data),
  register: (data) => http.post('/auth/register', data),
  logout: () => http.post('/auth/logout'),
  me: () => http.get('/auth/me'),
  menus: () => http.get('/auth/menus'),
  saveProfile: (data) => http.put('/auth/profile', data),
  changePassword: (data) => http.put('/auth/password', data),

  // 看板与选项
  dashboard: () => http.get('/dashboard'),
  options: () => http.get('/options'),

  // 图书
  bookPage: (p) => http.get('/book/page', { params: p }),
  bookDetail: (id) => http.get(`/book/${id}`),
  bookSave: (d) => (d.id ? http.put(`/book/${d.id}`, d) : http.post('/book', d)),
  bookDelete: (id) => http.delete(`/book/${id}`),
  onShelf: (id, d) => http.post(`/book/${id}/on-shelf`, d),

  // 分类
  categoryTree: () => http.get('/book-category/tree'),
  categoryPage: (p) => http.get('/book-category/page', { params: p }),
  categorySave: (d) => (d.id ? http.put(`/book-category/${d.id}`, d) : http.post('/book-category', d)),
  categoryDelete: (id) => http.delete(`/book-category/${id}`),

  // 入库与库存
  stockInPage: (p) => http.get('/stock-in/page', { params: p }),
  stockInSave: (d) => http.post('/stock-in', d),
  stockInInspect: (id, d) => http.post(`/stock-in/${id}/inspect`, d),
  flowPage: (p) => http.get('/stock/flow/page', { params: p }),
  stockCheck: (d) => http.post('/stock/check', d),

  // 订单
  orderPage: (p) => http.get('/order/page', { params: p }),
  orderDetail: (id) => http.get(`/order/${id}`),
  orderCreate: (d) => http.post('/order', d),
  orderPay: (id) => http.post(`/order/${id}/pay`),
  orderDeliver: (id, d) => http.post(`/order/${id}/deliver`, d),
  orderConfirm: (id) => http.post(`/order/${id}/confirm`),
  orderCancel: (id, d) => http.post(`/order/${id}/cancel`, d),

  // 书评
  reviewPage: (p) => http.get('/review/page', { params: p }),
  reviewCreate: (d) => http.post('/review', d),
  reviewAudit: (id, d) => http.post(`/review/${id}/audit`, d),
  reviewReply: (id, d) => http.post(`/review/${id}/reply`, d),
  reviewUseful: (id) => http.post(`/review/${id}/useful`),

  // 退换
  returnPage: (p) => http.get('/return/page', { params: p }),
  returnApply: (d) => http.post('/return', d),
  returnAudit: (id, d) => http.post(`/return/${id}/audit`, d),
  returnReceive: (id, d) => http.post(`/return/${id}/receive`, d),

  // 补货（创新点）
  replenishPage: (p) => http.get('/replenish/page', { params: p }),
  replenishDetail: (id) => http.get(`/replenish/${id}`),
  replenishStat: () => http.get('/replenish/stat'),
  replenishGenerate: () => http.post('/replenish/generate'),
  replenishAdopt: (id, d) => http.post(`/replenish/${id}/adopt`, d),
  ruleList: () => http.get('/replenish/rule'),
  ruleSave: (d) => http.post('/replenish/rule', d),

  // 报表
  report: (p) => http.get('/report/sales', { params: p }),

  // 门户与我的
  portalHome: () => http.get('/portal/home'),
  portalBooks: (p) => http.get('/portal/books', { params: p }),
  portalBook: (id) => http.get(`/portal/books/${id}`),
  myOrders: (p) => http.get('/my/orders', { params: p }),

  // 系统
  userPage: (p) => http.get('/system/user/page', { params: p }),
  userSave: (d) => (d.id ? http.put(`/system/user/${d.id}`, d) : http.post('/system/user', d)),
  resetPwd: (id) => http.post(`/system/user/${id}/reset`),
  rolePage: (p) => http.get('/system/role/page', { params: p }),
  roleList: () => http.get('/system/role/list'),
  roleSave: (d) => (d.id ? http.put(`/system/role/${d.id}`, d) : http.post('/system/role', d)),
  roleMenus: (id) => http.get(`/system/role/${id}/menus`),
  roleSaveMenus: (id, menuIds) => http.post(`/system/role/${id}/menus`, { menuIds }),
  menuTree: () => http.get('/system/menu/tree'),
  menuList: () => http.get('/system/menu/list'),
  dictAll: () => http.get('/system/dict/all'),
  dictPage: (p) => http.get('/system/dict/page', { params: p }),
  logPage: (p) => http.get('/system/log/page', { params: p }),
  messagePage: (p) => http.get('/system/message/page', { params: p }),
  unread: () => http.get('/system/message/unread'),
  readMsg: (id) => http.put(`/system/message/${id}/read`),
  readAll: () => http.put('/system/message/read-all')
}

export default http
