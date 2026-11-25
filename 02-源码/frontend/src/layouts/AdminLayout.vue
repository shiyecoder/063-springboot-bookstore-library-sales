<template>
  <div class="shell">
    <aside class="sidebar" :style="{ background: sideBg }">
      <div class="brand">
        <span class="dot">书</span>
        <span>书店运营中台</span>
      </div>
      <nav class="nav">
        <template v-for="m in menus" :key="m.id">
          <!-- 一级无子菜单：直接渲染叶子 -->
          <div v-if="!visibleChildren(m).length" class="nav-item"
               :class="{ active: isActive(m.path) }" @click="go(m.path)">
            <Icon :name="m.icon || 'file'" :size="15" />
            <span>{{ m.name }}</span>
          </div>
          <!-- 一级有子菜单：分组可折叠，默认展开当前组 -->
          <div v-else>
            <div class="nav-item" @click="toggle(m.id)">
              <Icon :name="m.icon || 'board'" :size="15" />
              <span>{{ m.name }}</span>
              <span class="caret">{{ open[m.id] ? '▾' : '▸' }}</span>
            </div>
            <div v-show="open[m.id]" class="nav-sub">
              <div v-for="c in visibleChildren(m)" :key="c.id" class="nav-item"
                   :class="{ active: isActive(c.path) }" @click="go(c.path)">
                <span>{{ c.name }}</span>
              </div>
            </div>
          </div>
        </template>
      </nav>
    </aside>

    <div class="main">
      <header class="topbar">
        <span class="crumb">
          <template v-if="group">{{ group }} <span style="opacity:.5">/</span> </template>
          <b>{{ route.meta.title || '工作台' }}</b>
        </span>
        <span class="spacer"></span>
        <span class="role-chip" :style="{ color: sideBg, borderColor: sideBg + '55' }">{{ roleName }}</span>
        <div class="icon-btn" title="站内消息" @click="router.push('/message')">
          <Icon name="bell" />
          <i v-if="store.unread" class="badge-dot">{{ store.unread > 99 ? '99+' : store.unread }}</i>
        </div>
        <el-dropdown trigger="click" @command="onCommand">
          <div class="userbox">
            <img class="avatar" :src="avatar" alt="头像" />
            <div>
              <div class="uname">{{ store.user.realName || store.user.username }}</div>
              <div class="urole">{{ store.user.orgName || roleName }}</div>
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="portal">前台书店</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <div class="content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import { api } from '@/api'
import { store, clearLogin } from '@/store/user'

const route = useRoute()
const router = useRouter()
const open = reactive({})

// 侧栏底色按角色区分（读者不进入本布局）
const SIDE = {
  ADMIN: '#22314A',
  OPS: '#1F3A56',
  BUYER: '#23453C',
  KEEPER: '#3A3550',
  CLERK: '#4A3327'
}
const sideBg = computed(() => SIDE[store.user.roleCode] || '#2C456D')
const roleName = computed(() => store.user.roleName || '')
const avatar = computed(() => store.user.avatar || '/uploads/seed/avatar/01.jpg')

const menus = computed(() => store.menus || [])

/** 带 :id 的详情节不进侧栏，避免出现无法点击的死链 */
function visibleChildren(m) {
  return (m.children || []).filter((c) => c.path && !c.path.includes(':'))
}

function isActive(p) {
  if (!p) return false
  if (p === '/book') return route.path === '/book' || /^\/book\/\d+\/edit$/.test(route.path)
  return route.path === p
}

function go(p) {
  if (p && route.path !== p) router.push(p)
}

function toggle(id) {
  open[id] = !open[id]
}

/** 面包屑一级标题：找到当前路由所属分组 */
const group = computed(() => {
  for (const m of menus.value) {
    for (const c of m.children || []) {
      if (c.path === route.path) return m.name
    }
  }
  return ''
})

function onCommand(c) {
  if (c === 'profile') router.push('/profile')
  else if (c === 'portal') window.open('#/portal', '_blank')
  else if (c === 'logout') {
    api.logout().catch(() => {})
    clearLogin()
    router.push('/login')
  }
}

onMounted(async () => {
  // 登录时缓存的菜单可能因管理员改过权限而过期，进入后台时静默刷新一次
  try {
    const ms = await api.menus()
    if (Array.isArray(ms) && ms.length) {
      store.menus = ms
      localStorage.setItem('menus', JSON.stringify(ms))
    }
  } catch (e) {
    /* 刷新失败沿用缓存，不打断使用 */
  }
  // 默认展开包含当前路由的分组
  for (const m of store.menus || []) {
    if ((m.children || []).some((c) => c.path === route.path)) open[m.id] = true
  }
  try {
    const u = await api.unread()
    store.unread = Number(u) || 0
  } catch (e) {
    store.unread = 0
  }
})
</script>

<style scoped>
.role-chip {
  font-size: 11.5px;
  border: 1px solid;
  border-radius: 10px;
  padding: 1px 8px;
  line-height: 17px;
}
</style>
