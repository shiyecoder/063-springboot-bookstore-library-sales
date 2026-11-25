<template>
  <div>
    <div class="page-head">
      <h2>订单台账</h2>
      <span class="sub">共 {{ total }} 单 · 本页待发货 {{ waitDeliver }} 单、退换中 {{ returning }} 单</span>
      <span class="spacer"></span>
      <el-button size="small" :icon="Download" @click="exportCsv">导出当前页</el-button>
      <el-button type="primary" size="small" @click="$router.push('/order/deliver')">去发货</el-button>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="订单号 / 书名 / 收件人 / 快递单号" clearable style="width: 260px"
                @keyup.enter="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="q.status" placeholder="全部状态" clearable @change="search">
        <el-option v-for="s in opts.orderStatus" :key="s.name" :label="s.name" :value="s.name" />
      </el-select>
      <el-select v-model="q.bookId" placeholder="全部图书" clearable filterable @change="search" style="width: 210px">
        <el-option v-for="b in opts.books" :key="b.id" :label="b.bookName" :value="b.id" />
      </el-select>
      <span class="spacer"></span>
      <el-button size="small" @click="reset">重置</el-button>
      <el-button type="primary" size="small" @click="search">查询</el-button>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column prop="orderNo" label="订单号" width="150">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="open(row)">{{ row.orderNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="图书" min-width="180">
          <template #default="{ row }">
            <div class="bk">
              <img class="cover" :src="row.coverImage" alt="" />
              <div class="bk-txt">
                <div class="bk-name">{{ row.bookName }}</div>
                <div class="bk-sub">{{ row.author }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="receiverName" label="收件人" width="92" />
        <el-table-column label="件数" width="62" align="right" prop="itemCount" />
        <el-table-column label="应付" width="94" align="right">
          <template #default="{ row }"><b class="amt">¥{{ fmt(row.payAmount) }}</b></template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="st" :class="stCls(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="快递" width="150">
          <template #default="{ row }">
            <div v-if="row.expressNo">{{ row.expressCompany }}</div>
            <div v-if="row.expressNo" class="exp-no">{{ row.expressNo }}</div>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="140">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="112" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="open(row)">详情</el-button>
            <el-button v-if="row.status === '待发货' && canDeliver" link type="warning" size="small"
                       @click="$router.push('/order/deliver')">发货</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @current-change="load" @size-change="search" />
      </div>
    </div>

    <el-drawer v-model="drawer" :title="`订单 ${cur.orderNo || ''}`" size="560px">
      <template v-if="cur.id">
        <div class="od-head">
          <span class="st" :class="stCls(cur.status)">{{ cur.status }}</span>
          <span class="muted">下单 {{ fmtTime(cur.createTime) }}</span>
          <span class="spacer"></span>
          <b class="amt">¥{{ fmt(cur.payAmount) }}</b>
        </div>

        <el-timeline style="margin-top: 10px">
          <el-timeline-item v-for="(t, i) in cur.timeline || []" :key="i" :timestamp="fmtTime(t.time)"
                            :color="i === (cur.timeline || []).length - 1 ? '#3A5A8C' : '#C9C9C9'">
            {{ t.name }}
          </el-timeline-item>
        </el-timeline>

        <div class="od-book">
          <img class="cover" :src="cur.coverImage" alt="" />
          <div>
            <div class="bk-name">{{ cur.bookName }}</div>
            <div class="bk-sub">{{ cur.author }} · {{ cur.publisher }}</div>
            <div class="bk-sub">单价 ¥{{ fmt(cur.unitPrice) }} × {{ cur.itemCount }} 件</div>
          </div>
        </div>

        <el-descriptions :column="1" size="small" border style="margin-top: 12px">
          <el-descriptions-item label="图书金额">¥{{ fmt(cur.bookAmount) }}</el-descriptions-item>
          <el-descriptions-item label="运费">¥{{ fmt(cur.freight) }}</el-descriptions-item>
          <el-descriptions-item label="优惠">-¥{{ fmt(cur.discountAmount) }}</el-descriptions-item>
          <el-descriptions-item label="实付">¥{{ fmt(cur.payAmount) }}</el-descriptions-item>
          <el-descriptions-item label="收货信息">
            {{ cur.receiverName }} {{ cur.receiverPhone }}<br />{{ cur.addressDesc }}
          </el-descriptions-item>
          <el-descriptions-item label="买家">{{ cur.customerName }}</el-descriptions-item>
          <el-descriptions-item v-if="cur.cancelReason" label="取消原因">{{ cur.cancelReason }}</el-descriptions-item>
        </el-descriptions>

        <template v-if="(cur.returns || []).length">
          <h4 class="card-title" style="margin-top: 14px">退换记录</h4>
          <div v-for="r in cur.returns" :key="r.id" class="ret">
            <div class="ret-head">
              <b>{{ r.returnNo }}</b>
              <span class="st" :class="retCls(r.status)">{{ r.status }}</span>
              <span class="muted">{{ r.returnType }} · {{ r.applyQty }} 件 · 退款 ¥{{ fmt(r.refundAmount) }}</span>
            </div>
            <p class="ret-txt">{{ r.description }}</p>
          </div>
        </template>

        <div class="od-ops">
          <el-button v-if="cur.status === '待发货' && canDeliver" type="primary" size="small"
                     @click="$router.push('/order/deliver')">去发货</el-button>
          <el-button v-if="cur.status === '已发货'" size="small" @click="confirm">确认收货（模拟买家）</el-button>
          <el-button v-if="['待支付', '待发货'].includes(cur.status)" size="small" @click="cancel">取消订单</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime, fmtNum } from '@/utils/format'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const opts = ref({})
const drawer = ref(false)
const cur = ref({})
const q = reactive({ page: 1, size: 10, keyword: '', status: '', bookId: null })

const canDeliver = computed(() => ['ADMIN', 'CLERK'].includes(store.user.roleCode))
const waitDeliver = computed(() => rows.value.filter((r) => r.status === '待发货').length)
const returning = computed(() => rows.value.filter((r) => r.status === '退换中').length)
const fmt = (v) => fmtNum(v, 2)
const stCls = (s) => ({
  待支付: 'st-gray', 待发货: 'st-amber', 已发货: 'st-blue', 已签收: 'st-blue',
  已完成: 'st-green', 退换中: 'st-red', 已取消: 'st-gray'
}[s] || 'st-gray')
const retCls = (s) => ({ 待审核: 'st-amber', 待寄回: 'st-blue', 已寄回: 'st-blue', 已退款: 'st-green', 已驳回: 'st-red', 已关闭: 'st-gray' }[s] || 'st-gray')

async function load() {
  loading.value = true
  try {
    const r = await api.orderPage({
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
function reset() {
  Object.assign(q, { page: 1, keyword: '', status: '', bookId: null })
  load()
}
async function open(row) {
  cur.value = await api.orderDetail(row.id)
  drawer.value = true
}
async function confirm() {
  await api.orderConfirm(cur.value.id)
  ElMessage.success('已确认收货')
  cur.value = await api.orderDetail(cur.value.id)
  load()
}
async function cancel() {
  const r = await ElMessageBox.prompt('请选择或填写取消原因', '取消订单', {
    inputValue: '买家主动取消',
    inputValidator: (v) => (v && v.trim().length >= 4 ? true : '原因不少于 4 个字')
  }).catch(() => null)
  if (!r) return
  await api.orderCancel(cur.value.id, { reason: r.value })
  ElMessage.success('订单已取消，库存已回补')
  cur.value = await api.orderDetail(cur.value.id)
  load()
}
function exportCsv() {
  const head = ['订单号', '图书', '收件人', '件数', '应付', '状态', '快递公司', '快递单号', '下单时间']
  const lines = rows.value.map((r) => [r.orderNo, r.bookName, r.receiverName, r.itemCount, r.payAmount, r.status, r.expressCompany, r.expressNo, fmtTime(r.createTime)])
  const csv = '\ufeff' + [head, ...lines].map((l) => l.map((x) => `"${x ?? ''}"`).join(',')).join('\n')
  const a = document.createElement('a')
  a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
  a.download = `订单台账_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  ElMessage.success('已导出当前页')
}

onMounted(async () => {
  opts.value = await api.options()
  load()
})
</script>

<style scoped>
.bk { display: flex; gap: 8px; align-items: center; }
.bk-txt { min-width: 0; }
.bk-name { font-size: 12.5px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bk-sub { color: var(--text-3); font-size: 11px; }
.amt { color: var(--primary); }
.exp-no { color: var(--text-3); font-size: 10.5px; font-family: ui-monospace, Menlo, monospace; }
.muted { color: var(--text-3); font-size: 11.5px; }
.od-head { display: flex; align-items: center; gap: 10px; }
.od-head .spacer { flex: 1; }
.od-book { display: flex; gap: 12px; background: var(--paper); padding: 10px; border-radius: 4px; margin-top: 12px; }
.ret { border-top: 1px dashed var(--line); padding: 8px 0; }
.ret-head { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.ret-txt { color: var(--text-2); font-size: 12px; margin: 4px 0 0; line-height: 1.7; }
.od-ops { margin-top: 16px; display: flex; gap: 8px; }
</style>
