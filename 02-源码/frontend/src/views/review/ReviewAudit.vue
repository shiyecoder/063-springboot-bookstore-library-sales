<template>
  <div>
    <div class="page-head">
      <h2>书评审核</h2>
      <span class="sub">待审核 {{ pending }} 条 · 内容少于 15 字或含不文明用语会被判为不合格</span>
      <span class="spacer"></span>
      <el-radio-group v-model="q.status" size="small" @change="search">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="待审核">待审核</el-radio-button>
        <el-radio-button label="已展示">已展示</el-radio-button>
        <el-radio-button label="已隐藏">已隐藏</el-radio-button>
        <el-radio-button label="已屏蔽">已屏蔽</el-radio-button>
      </el-radio-group>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="书名 / 书评标题 / 读者" clearable style="width: 240px" @keyup.enter="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="q.bookId" placeholder="全部图书" clearable filterable style="width: 210px" @change="search">
        <el-option v-for="b in opts.books" :key="b.id" :label="b.bookName" :value="b.id" />
      </el-select>
      <span class="spacer"></span>
      <el-button type="primary" size="small" @click="search">查询</el-button>
    </div>

    <div class="rv-list" v-loading="loading">
      <div v-for="r in rows" :key="r.id" class="card rv-card">
        <div class="rv-left">
          <img class="cover" :src="r.coverImage" alt="" />
          <div class="rv-book">
            <div class="rv-bookname">{{ r.bookName }}</div>
            <div class="rv-sub">{{ r.bookNo }}</div>
          </div>
        </div>
        <div class="rv-main">
          <div class="rv-head">
            <img class="avatar" :src="r.avatar" alt="" />
            <b>{{ r.customerName }}</b>
            <el-rate :model-value="r.score" disabled size="small" />
            <span class="st" :class="stCls(r.status)">{{ r.status }}</span>
            <span class="rv-time">{{ fmtTime(r.createTime) }}</span>
          </div>
          <div class="rv-title">{{ r.title }}</div>
          <p class="rv-txt">{{ r.content }}</p>
          <div v-if="r.imageUrls" class="rv-imgs">
            <el-image v-for="(u, i) in r.imageUrls.split(',')" :key="i" :src="u" fit="cover"
                      :preview-src-list="r.imageUrls.split(',')" :initial-index="i" preview-teleported class="rv-img" />
          </div>
          <div class="rv-meta">
            <span>有用 {{ r.usefulCount }}</span>
            <span v-if="r.replyContent">已回复</span>
            <span class="len" :class="{ bad: (r.content || '').length < 15 }">
              正文 {{ (r.content || '').length }} 字
            </span>
          </div>
        </div>
        <div class="rv-ops" v-if="canAudit">
          <template v-if="r.status === '待审核'">
            <el-button type="success" size="small" plain @click="act(r, '已展示')">通过展示</el-button>
            <el-button type="warning" size="small" plain @click="act(r, '已隐藏')">隐藏</el-button>
            <el-button type="danger" size="small" plain @click="act(r, '已屏蔽')">屏蔽</el-button>
          </template>
          <template v-else>
            <el-button v-if="r.status !== '已展示'" size="small" plain type="success" @click="act(r, '已展示')">
              恢复展示
            </el-button>
            <el-button v-if="r.status === '已展示'" size="small" plain @click="act(r, '已隐藏')">下架</el-button>
          </template>
        </div>
      </div>
      <p v-if="!rows.length && !loading" class="empty">没有符合条件的书评</p>
    </div>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     v-model:current-page="q.page" :page-size="q.size" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const opts = ref({})
const loading = ref(false)
const q = reactive({ page: 1, size: 8, keyword: '', status: '待审核', bookId: null })

const canAudit = computed(() => ['ADMIN', 'OPS', 'CLERK'].includes(store.user.roleCode))
const pending = computed(() => rows.value.filter((r) => r.status === '待审核').length)
const stCls = (s) => ({ 待审核: 'st-amber', 已展示: 'st-green', 已隐藏: 'st-gray', 已屏蔽: 'st-red' }[s] || 'st-gray')

async function load() {
  loading.value = true
  try {
    const r = await api.reviewPage({
      ...q,
      keyword: q.keyword || undefined,
      status: q.status || undefined,
      bookId: q.bookId || undefined
    })
    rows.value = r.rows || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}
function search() {
  q.page = 1
  load()
}
async function act(r, target) {
  let reason = ''
  if (target !== '已展示') {
    const res = await ElMessageBox.prompt(`将书评置为「${target}」，请填写处理依据`, '审核意见', {
      inputPlaceholder: '如：含广告链接 / 与图书内容无关',
      inputValidator: (v) => (v && v.trim().length >= 3 ? true : '请至少填写 3 个字')
    }).catch(() => null)
    if (!res) return
    reason = res.value
  }
  await api.reviewAudit(r.id, { targetStatus: target, reason })
  ElMessage.success(`已${target === '已展示' ? '通过并展示' : target}`)
  load()
}

onMounted(async () => {
  opts.value = await api.options()
  load()
})
</script>

<style scoped>
.rv-list { display: flex; flex-direction: column; gap: 10px; }
.rv-card { display: flex; gap: 16px; align-items: flex-start; }
.rv-left { width: 132px; flex: 0 0 132px; display: flex; gap: 9px; }
.rv-bookname { font-size: 12.5px; font-weight: 600; line-height: 1.5; }
.rv-sub { color: var(--text-3); font-size: 11px; }
.rv-main { flex: 1; min-width: 0; }
.rv-head { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.rv-time { margin-left: auto; color: var(--text-3); font-size: 11px; }
.rv-title { font-weight: 600; font-size: 13px; margin: 7px 0 3px; }
.rv-txt { color: var(--text-2); font-size: 12.5px; line-height: 1.85; margin: 0; }
.rv-imgs { display: flex; gap: 6px; margin-top: 8px; }
.rv-img { width: 66px; height: 50px; border-radius: 3px; border: 1px solid var(--line); }
.rv-meta { display: flex; gap: 14px; color: var(--text-3); font-size: 11px; margin-top: 8px; }
.rv-meta .bad { color: var(--danger); font-weight: 600; }
.rv-ops { display: flex; flex-direction: column; gap: 6px; width: 96px; flex: 0 0 96px; }
.empty { color: var(--text-3); font-size: 12px; text-align: center; padding: 40px 0; }
</style>
