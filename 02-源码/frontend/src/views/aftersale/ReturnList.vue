<template>
  <div>
    <div class="page-head">
      <h2>退换申请</h2>
      <span class="sub">签收后 7 天内可申请，超过 15 天不再受理；同订单同图书仅允许 1 个进行中退换单</span>
      <span class="spacer"></span>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <div class="kpis">
      <div class="kpi"><div class="label">全部退换单</div><div class="value">{{ total }}</div></div>
      <div class="kpi"><div class="label">待审核</div><div class="value" style="color:#B4761E">{{ cnt('待审核') }}</div></div>
      <div class="kpi"><div class="label">待寄回 / 已寄回</div><div class="value">{{ cnt('待寄回') + cnt('已寄回') }}</div></div>
      <div class="kpi"><div class="label">已退款</div><div class="value" style="color:#2B7955">{{ cnt('已退款') }}</div></div>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="退换单号 / 订单号 / 图书" clearable style="width: 240px" @keyup.enter="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="q.status" placeholder="全部状态" clearable @change="search">
        <el-option label="待审核" value="待审核" />
        <el-option label="待寄回" value="待寄回" />
        <el-option label="已寄回" value="已寄回" />
        <el-option label="已退款" value="已退款" />
        <el-option label="已驳回" value="已驳回" />
        <el-option label="已关闭" value="已关闭" />
      </el-select>
      <span class="spacer"></span>
      <el-button size="small" @click="reset">重置</el-button>
      <el-button type="primary" size="small" @click="search">查询</el-button>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column prop="returnNo" label="退换单号" width="140" />
        <el-table-column label="图书" min-width="190">
          <template #default="{ row }">
            <div class="bk">
              <img class="cover" :src="row.coverImage" alt="" />
              <div>
                <div class="bk-name">{{ row.bookName }}</div>
                <div class="bk-sub">{{ row.orderNo }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="读者" width="86" />
        <el-table-column prop="returnType" label="类型" width="94" />
        <el-table-column prop="reasonType" label="原因" width="94" />
        <el-table-column label="申请件数" width="86" align="right" prop="applyQty" />
        <el-table-column label="退款金额" width="94" align="right">
          <template #default="{ row }"><b style="color:#B02A37">¥{{ fmt(row.refundAmount) }}</b></template>
        </el-table-column>
        <el-table-column label="状态" width="88">
          <template #default="{ row }">
            <span class="st" :class="stCls(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="140">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="94" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '待审核'" link type="primary" size="small"
                       @click="$router.push('/return/desk')">去处理</el-button>
            <el-button v-else-if="['待寄回', '已寄回'].includes(row.status)" link type="primary" size="small"
                       @click="$router.push('/return/receive')">去验收</el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @current-change="load" @size-change="search" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { api } from '@/api'
import { fmtTime, fmtNum } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ page: 1, size: 10, keyword: '', status: '' })
const dist = computed(() => {
  const m = {}
  rows.value.forEach((r) => (m[r.status] = (m[r.status] || 0) + 1))
  return m
})
const cnt = (s) => dist.value[s] || 0
const fmt = (v) => fmtNum(v, 2)
const stCls = (s) => ({
  待审核: 'st-amber', 待寄回: 'st-blue', 已寄回: 'st-blue',
  已退款: 'st-green', 已驳回: 'st-red', 已关闭: 'st-gray'
}[s] || 'st-gray')

async function load() {
  loading.value = true
  try {
    const r = await api.returnPage({ ...q, keyword: q.keyword || undefined, status: q.status || undefined })
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
function reset() {
  Object.assign(q, { page: 1, keyword: '', status: '' })
  load()
}

onMounted(load)
</script>

<style scoped>
.bk { display: flex; gap: 8px; align-items: center; }
.bk-name { font-size: 12.5px; font-weight: 500; }
.bk-sub { color: var(--text-3); font-size: 11px; }
.muted { color: var(--text-3); }
</style>
