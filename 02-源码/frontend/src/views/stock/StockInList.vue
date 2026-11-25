<template>
  <div>
    <div class="page-head">
      <h2>入库登记</h2>
      <span class="sub">登记后进入待质检，质检合格才写库存与流水，不合格整批退回</span>
      <span class="spacer"></span>
      <el-button size="small" :icon="Refresh" @click="load">刷新</el-button>
      <el-button v-if="canAdd" type="primary" size="small" :icon="Plus" @click="open">登记入库单</el-button>
    </div>

    <div class="kpis">
      <div class="kpi"><div class="label">本月入库单</div><div class="value">{{ stat.total }}</div></div>
      <div class="kpi"><div class="label">待质检</div><div class="value" style="color:#B4761E">{{ stat.pending }}</div></div>
      <div class="kpi"><div class="label">本月入库册数</div><div class="value">{{ stat.qty }}</div></div>
      <div class="kpi"><div class="label">质检不合格率</div><div class="value" style="color:#B02A37">{{ stat.badRate }}<span class="unit">%</span></div></div>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="入库单号 / 图书名称" clearable @keyup.enter="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="q.status" placeholder="全部状态" clearable @change="search">
        <el-option label="待质检" value="待质检" />
        <el-option label="已入库" value="已入库" />
        <el-option label="已退回" value="已退回" />
        <el-option label="已作废" value="已作废" />
      </el-select>
      <span class="spacer"></span>
      <el-button type="primary" size="small" @click="search">查询</el-button>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column prop="stockInNo" label="入库单号" width="150" />
        <el-table-column label="图书" min-width="200">
          <template #default="{ row }">
            <div class="bk">
              <img class="cover" :src="row.coverImage" alt="" />
              <div>
                <div class="bk-name">{{ row.bookName }}</div>
                <div class="bk-no">{{ row.bookNo }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="supplierName" label="供货书商" width="130" />
        <el-table-column prop="inQty" label="入库数量" width="90" align="right" />
        <el-table-column label="采购价" width="94" align="right">
          <template #default="{ row }">¥{{ fmt(row.purchasePrice) }}</template>
        </el-table-column>
        <el-table-column label="采购金额" width="100" align="right">
          <template #default="{ row }">¥{{ fmt(row.inQty * row.purchasePrice) }}</template>
        </el-table-column>
        <el-table-column prop="qualityResult" label="质检结果" width="94" />
        <el-table-column label="状态" width="94">
          <template #default="{ row }">
            <span class="st" :class="stCls(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="登记时间" width="140">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="92" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '待质检' && canInspect" link type="primary" size="small"
                       @click="$router.push('/stock-in/inspect')">去质检</el-button>
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

    <el-dialog v-model="dlg" title="登记入库单" width="480px">
      <el-form ref="f" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="图书" prop="bookId">
          <el-select v-model="form.bookId" filterable placeholder="搜索书名" style="width: 100%">
            <el-option v-for="b in opts.books" :key="b.id" :label="`${b.bookName}（库存 ${b.stockQty}）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="供货书商" prop="supplierName">
          <el-select v-model="form.supplierName" placeholder="请选择" style="width: 100%">
            <el-option v-for="s in opts.suppliers" :key="s.name" :label="s.name" :value="s.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量" prop="inQty">
          <el-input-number v-model="form.inQty" :min="1" :max="5000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="采购价" prop="purchasePrice">
          <el-input-number v-model="form.purchasePrice" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="采购金额">
          <el-input :model-value="`¥${fmt(form.inQty * form.purchasePrice)}`" readonly />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">提交登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime, fmtNum } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const opts = ref({})
const dlg = ref(false)
const f = ref()
const q = reactive({ page: 1, size: 10, keyword: '', status: '' })
const form = reactive({ bookId: null, supplierName: '', inQty: 20, purchasePrice: 40, remark: '' })

const canAdd = computed(() => ['ADMIN', 'KEEPER', 'BUYER'].includes(store.user.roleCode))
const canInspect = computed(() => ['ADMIN', 'KEEPER'].includes(store.user.roleCode))
const fmt = (v) => fmtNum(v, 2)
const stCls = (s) => ({ 待质检: 'st-amber', 已入库: 'st-green', 已退回: 'st-red' }[s] || 'st-gray')

const stat = computed(() => {
  const all = rows.value
  const qty = all.filter((r) => r.status === '已入库').reduce((s, r) => s + r.inQty, 0)
  const bad = all.filter((r) => r.status === '已退回').length
  return {
    total: total.value,
    pending: all.filter((r) => r.status === '待质检').length,
    qty,
    badRate: all.length ? ((bad / all.length) * 100).toFixed(1) : '0.0'
  }
})

const rules = {
  bookId: [{ required: true, message: '请选择图书', trigger: 'change' }],
  supplierName: [{ required: true, message: '请选择供货书商', trigger: 'change' }],
  inQty: [{ required: true, message: '请输入入库数量', trigger: 'blur' }],
  purchasePrice: [{ required: true, message: '请输入采购价', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const r = await api.stockInPage({ ...q, keyword: q.keyword || undefined, status: q.status || undefined })
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
function open() {
  Object.assign(form, { bookId: null, supplierName: '', inQty: 20, purchasePrice: 40, remark: '' })
  dlg.value = true
}
async function save() {
  const ok = await f.value.validate().catch(() => false)
  if (!ok) return
  saving.value = true
  try {
    await api.stockInSave({ ...form })
    ElMessage.success('入库单已登记，等待质检')
    dlg.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  opts.value = await api.options()
  load()
})
</script>

<style scoped>
.bk { display: flex; gap: 9px; align-items: center; }
.bk-name { font-size: 12.5px; font-weight: 500; }
.bk-no { color: var(--text-3); font-size: 11px; font-family: ui-monospace, Menlo, monospace; }
.muted { color: var(--text-3); }
</style>
