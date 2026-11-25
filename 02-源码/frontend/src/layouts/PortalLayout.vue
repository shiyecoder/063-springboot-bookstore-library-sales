<template>
  <div class="portal">
    <header class="pbar">
      <div class="pbar-inner">
        <div class="plogo" @click="router.push('/portal')">
          <span class="pdot">书</span>
          <b>拾页书店</b>
          <em>网上书店与图书销售系统</em>
        </div>
        <nav class="pnav">
          <span v-for="n in navs" :key="n.path" class="pnav-item" :class="{ on: active(n.path) }"
                @click="router.push(n.path)">{{ n.label }}</span>
        </nav>
        <div class="psearch">
          <el-input v-model="kw" placeholder="搜书名 / 作者 / ISBN" clearable @keyup.enter="doSearch">
            <template #prefix><Icon name="search" :size="14" /></template>
          </el-input>
        </div>
        <div class="pactions">
          <div class="icon-btn" title="购物车" @click="router.push('/portal/cart')">
            <Icon name="archive" />
            <i v-if="cart.count" class="badge-dot">{{ cart.count }}</i>
          </div>
          <div class="icon-btn" title="站内消息" @click="router.push('/portal/message')">
            <Icon name="bell" />
          </div>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="userbox">
              <img class="avatar" :src="store.user.avatar || '/uploads/seed/avatar/02.jpg'" alt="头像" />
              <span class="uname">{{ store.user.realName || store.user.username || '访客' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                <el-dropdown-item command="reviews">我的书评</el-dropdown-item>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="admin" v-if="isStaff" divided>进入运营后台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="pmain">
      <router-view v-slot="{ Component }">
        <component :is="Component" />
      </router-view>
    </main>

    <footer class="pfoot">
      <span>拾页书店 · 网上书店与图书销售系统</span>
      <span>客服 400-800-1963 · 每日 9:00–21:00</span>
      <span>本系统为毕业设计演示数据，不产生真实交易</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import { store, clearLogin } from '@/store/user'
import { cart } from '@/store/cart'

const route = useRoute()
const router = useRouter()
const kw = ref('')

const navs = [
  { path: '/portal', label: '首页' },
  { path: '/portal/books', label: '全部图书' },
  { path: '/portal/orders', label: '我的订单' },
  { path: '/portal/reviews', label: '我的书评' },
  { path: '/portal/mobile', label: '移动端' }
]

const isStaff = computed(() => store.user.roleCode && store.user.roleCode !== 'READER')

function active(p) {
  if (p === '/portal') return route.path === '/portal'
  return route.path.startsWith(p)
}

function doSearch() {
  router.push({ path: '/portal/books', query: { keyword: kw.value || undefined } })
}

function onCommand(c) {
  if (c === 'admin') router.push('/dashboard')
  else if (c === 'orders') router.push('/portal/orders')
  else if (c === 'reviews') router.push('/portal/reviews')
  else if (c === 'profile') router.push('/portal/profile')
  else if (c === 'logout') {
    clearLogin()
    cart.clear()
    router.push('/login')
  }
}
</script>

<style scoped>
.portal { min-height: 100vh; background: var(--bg); display: flex; flex-direction: column; }
.pbar { background: var(--primary-dark); color: #E8EDF4; position: sticky; top: 0; z-index: 20; }
.pbar-inner { max-width: 1240px; margin: 0 auto; height: 58px; display: flex; align-items: center; gap: 20px; padding: 0 16px; }
.plogo { display: flex; align-items: center; gap: 8px; cursor: pointer; white-space: nowrap; }
.plogo b { font-size: 15.5px; letter-spacing: .5px; }
.plogo em { font-style: normal; font-size: 11px; opacity: .55; }
.pdot { width: 24px; height: 24px; border-radius: 5px; background: var(--accent); color: #2C456D;
  display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px; }
.pnav { display: flex; gap: 2px; margin-left: 8px; }
.pnav-item { padding: 6px 11px; border-radius: 4px; font-size: 13px; cursor: pointer; opacity: .82; white-space: nowrap; }
.pnav-item:hover { background: rgba(255, 255, 255, .1); opacity: 1; }
.pnav-item.on { background: rgba(201, 162, 39, .22); opacity: 1; font-weight: 600; }
.psearch { flex: 1; max-width: 300px; margin-left: auto; }
.psearch :deep(.el-input__wrapper) { background: rgba(255, 255, 255, .12); box-shadow: none; border-radius: 16px; }
.psearch :deep(.el-input__inner) { color: #fff; }
.psearch :deep(.el-input__inner::placeholder) { color: rgba(255, 255, 255, .55); }
.psearch :deep(.el-input__prefix) { color: rgba(255, 255, 255, .7); }
.pactions { display: flex; align-items: center; gap: 6px; }
.pactions .icon-btn { color: rgba(255, 255, 255, .8); }
.pactions .icon-btn:hover { background: rgba(255, 255, 255, .12); color: #fff; }
.pactions .uname { color: #E8EDF4; }
.pmain { flex: 1; max-width: 1240px; width: 100%; margin: 0 auto; padding: 16px; }
.pfoot { border-top: 1px solid var(--line); background: var(--paper); color: var(--text-3);
  font-size: 11.5px; padding: 12px 16px; display: flex; gap: 22px; justify-content: center; flex-wrap: wrap; }
@media (max-width: 900px) {
  .pnav, .psearch, .plogo em { display: none; }
}
</style>
