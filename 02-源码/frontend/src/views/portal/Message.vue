<template>
  <div>
    <div class="page-head">
      <h2>站内消息</h2>
      <span class="sub">未读 {{ unread }} 条 · 发货、退款、审核结果都会在这里通知</span>
      <span class="spacer"></span>
      <el-button size="small" :disabled="!unread" @click="readAll">全部标记已读</el-button>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <div class="filters">
      <el-radio-group v-model="type" size="small" @change="search">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="订单">订单</el-radio-button>
        <el-radio-button label="退换">退换</el-radio-button>
        <el-radio-button label="系统">系统</el-radio-button>
      </el-radio-group>
      <span class="spacer"></span>
      <span class="muted">点击消息即视为已读</span>
    </div>

    <div v-loading="loading" class="list">
      <div v-for="m in rows" :key="m.id" class="card msg" :class="{ unread: !m.isRead }" @click="read(m)">
        <div class="m-head">
          <span class="st" :class="typeCls(m.msgType)">{{ m.msgType }}</span>
          <b>{{ m.title }}</b>
          <span class="spacer"></span>
          <span class="m-time">{{ fmtTime(m.createTime) }}</span>
        </div>
        <p class="m-txt">{{ m.content }}</p>
        <div class="m-foot">
          <span v-if="!m.isRead" class="dot">未读</span>
          <el-button v-if="m.linkUrl" link type="primary" size="small" @click.stop="go(m)">查看详情</el-button>
        </div>
      </div>
      <p v-if="!rows.length && !loading" class="empty">暂无消息</p>
    </div>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     v-model:current-page="q.page" :page-size="q.size" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime } from '@/utils/format'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const unread = ref(0)
const loading = ref(false)
const type = ref('')
const q = reactive({ page: 1, size: 10 })

const typeCls = (t) => ({ 订单: 'st-blue', 退换: 'st-amber', 系统: 'st-gray' }[t] || 'st-gray')

async function load() {
  loading.value = true
  try {
    const r = await api.messagePage({ page: q.page, size: q.size, type: type.value || undefined })
    rows.value = r.rows || []
    total.value = r.total || 0
    unread.value = Number(await api.unread()) || 0
    store.unread = unread.value
  } finally {
    loading.value = false
  }
}
function search() {
  q.page = 1
  load()
}
async function read(m) {
  if (m.isRead) return
  await api.readMsg(m.id)
  m.isRead = 1
  unread.value = Math.max(0, unread.value - 1)
  store.unread = unread.value
}
async function readAll() {
  await api.readAll()
  ElMessage.success('已全部标记为已读')
  load()
}
function go(m) {
  const path = m.linkUrl || ''
  if (path.startsWith('/my/orders')) router.push('/portal/orders')
  else if (path.startsWith('/portal')) router.push(path)
  else router.push('/portal')
}

onMounted(load)
</script>

<style scoped>
.list { display: flex; flex-direction: column; gap: 9px; }
.msg { cursor: pointer; }
.msg.unread { border-left: 3px solid var(--primary); background: #F9FBFE; }
.m-head { display: flex; align-items: center; gap: 9px; font-size: 13px; }
.m-head .spacer { flex: 1; }
.m-time { color: var(--text-3); font-size: 11px; }
.m-txt { color: var(--text-2); font-size: 12.5px; line-height: 1.85; margin: 7px 0 0; }
.m-foot { display: flex; align-items: center; gap: 10px; margin-top: 6px; }
.dot { color: var(--danger); font-size: 11px; font-weight: 600; }
.muted { color: var(--text-3); font-size: 11.5px; }
.empty { text-align: center; color: var(--text-3); font-size: 12.5px; padding: 46px 0; }
</style>
