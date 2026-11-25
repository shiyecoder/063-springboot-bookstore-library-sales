import { createRouter, createWebHashHistory } from 'vue-router'

/**
 * 路由表与「菜单表」保持一一对应：
 * - 路径、标题取自 sys_menu，侧栏由 /auth/menus 动态渲染，改菜单不必改前端
 * - meta.roles 只做体验层拦截（读者进后台直接跳门户），真正鉴权在后端角色白名单
 */
const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { title: '登录', open: true } },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'book', component: () => import('@/views/book/BookList.vue'), meta: { title: '图书列表' } },
      { path: 'book/new', component: () => import('@/views/book/BookEdit.vue'), meta: { title: '新增图书' } },
      { path: 'book/:id/edit', component: () => import('@/views/book/BookEdit.vue'), meta: { title: '编辑图书' } },
      { path: 'category', component: () => import('@/views/book/CategoryList.vue'), meta: { title: '分类维护' } },

      { path: 'stock-in', component: () => import('@/views/stock/StockInList.vue'), meta: { title: '入库登记' } },
      { path: 'stock-in/inspect', component: () => import('@/views/stock/StockInspect.vue'), meta: { title: '入库质检' } },
      { path: 'stock/check', component: () => import('@/views/stock/StockCheck.vue'), meta: { title: '库存盘点' } },
      { path: 'stock/flow', component: () => import('@/views/stock/StockFlow.vue'), meta: { title: '库存流水' } },

      { path: 'order', component: () => import('@/views/order/OrderList.vue'), meta: { title: '订单台账' } },
      { path: 'order/deliver', component: () => import('@/views/order/DeliverDesk.vue'), meta: { title: '发货工作台' } },
      { path: 'order/detail/:id', component: () => import('@/views/order/OrderDetail.vue'), meta: { title: '订单详情' } },

      { path: 'review', component: () => import('@/views/review/ReviewAudit.vue'), meta: { title: '书评审核' } },
      { path: 'review/reply', component: () => import('@/views/review/ReviewReply.vue'), meta: { title: '书评回复' } },

      { path: 'return', component: () => import('@/views/aftersale/ReturnList.vue'), meta: { title: '退换申请' } },
      { path: 'return/desk', component: () => import('@/views/aftersale/ReturnDesk.vue'), meta: { title: '售后处理台' } },
      { path: 'return/receive', component: () => import('@/views/aftersale/ReturnReceive.vue'), meta: { title: '退换验收' } },

      { path: 'replenish', component: () => import('@/views/replenish/Board.vue'), meta: { title: '补货看板' } },
      { path: 'replenish/detail/:id', component: () => import('@/views/replenish/Detail.vue'), meta: { title: '建议详情' } },
      { path: 'replenish/rule', component: () => import('@/views/replenish/Rule.vue'), meta: { title: '规则配置' } },
      { path: 'replenish/trace', component: () => import('@/views/replenish/Trace.vue'), meta: { title: '计算留痕' } },
      { path: 'report', component: () => import('@/views/report/Sales.vue'), meta: { title: '销售报表' } },

      { path: 'system/user', component: () => import('@/views/system/UserList.vue'), meta: { title: '用户管理' } },
      { path: 'system/role', component: () => import('@/views/system/RoleList.vue'), meta: { title: '角色与菜单' } },
      { path: 'system/menu', component: () => import('@/views/system/MenuDict.vue'), meta: { title: '菜单与字典' } },
      { path: 'system/log', component: () => import('@/views/system/OpLog.vue'), meta: { title: '操作日志' } },

      { path: 'profile', component: () => import('@/views/portal/Profile.vue'), meta: { title: '个人中心' } },
      { path: 'message', component: () => import('@/views/portal/Message.vue'), meta: { title: '站内消息' } },
      // 后台越权访问门户页时也走后台布局，避免出现没有侧栏的孤立页面
      { path: 'portal-home', component: () => import('@/views/portal/Home.vue'), meta: { title: '书店首页' } }
    ]
  },
  {
    path: '/portal',
    component: () => import('@/layouts/PortalLayout.vue'),
    children: [
      { path: '', component: () => import('@/views/portal/Home.vue'), meta: { title: '书店首页' } },
      { path: 'books', component: () => import('@/views/portal/BookList.vue'), meta: { title: '全部图书' } },
      { path: 'book/:id', component: () => import('@/views/portal/BookDetail.vue'), meta: { title: '图书详情' } },
      { path: 'cart', component: () => import('@/views/portal/Cart.vue'), meta: { title: '购物车' } },
      { path: 'orders', component: () => import('@/views/portal/MyOrders.vue'), meta: { title: '我的订单' } },
      { path: 'reviews', component: () => import('@/views/portal/MyReviews.vue'), meta: { title: '我的书评' } },
      { path: 'message', component: () => import('@/views/portal/Message.vue'), meta: { title: '站内消息' } },
      { path: 'profile', component: () => import('@/views/portal/Profile.vue'), meta: { title: '个人中心' } },
      { path: 'mobile', component: () => import('@/views/portal/Mobile.vue'), meta: { title: '移动端' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to, from, next) => {
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '网上书店与图书销售系统'
  const token = localStorage.getItem('token')
  if (to.meta.open) return next()
  if (!token) return next({ path: '/login', query: { redirect: to.fullPath } })

  let role = ''
  try {
    role = JSON.parse(localStorage.getItem('user') || '{}').roleCode || ''
  } catch (e) {
    role = ''
  }
  // 读者只有门户权限，误入后台路径统一回到首页
  if (role === 'READER' && !to.path.startsWith('/portal') && to.path !== '/login') {
    return next('/portal')
  }
  next()
})

export default router
