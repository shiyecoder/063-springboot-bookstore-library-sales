<template>
  <div>
    <div class="page-head">
      <h2>图书列表</h2>
      <span class="sub">共 {{ total }} 种图书，当前有库存 {{ onSale }} 种</span>
      <span class="spacer"></span>
      <el-button size="small" :icon="Download" @click="exportCsv">导出</el-button>
      <el-button v-if="canEdit" type="primary" size="small" :icon="Plus" @click="$router.push('/book/new')">
        新增图书
      </el-button>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="书名 / 作者 / ISBN" clearable @keyup.enter="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="q.categoryId" placeholder="全部分类" clearable @change="search">
        <el-option v-for="c in flatCats" :key="c.id" :label="c.label" :value="c.id" />
      </el-select>
      <el-select v-model="q.status" placeholder="全部状态" clearable @change="search">
        <el-option label="在售" value="在售" />
        <el-option label="待上架" value="待上架" />
        <el-option label="停售" value="停售" />
      </el-select>
      <el-select v-model="q.publisher" placeholder="全部出版社" clearable @change="search">
        <el-option v-for="p in opts.publishers" :key="p.name" :label="p.name" :value="p.name" />
      </el-select>
      <el-select v-model="q.sort" style="width: 150px" @change="search">
        <el-option label="按上架时间" value="new" />
        <el-option label="按销量" value="sales" />
        <el-option label="按库存" value="stock" />
        <el-option label="按售价" value="price" />
      </el-select>
      <span class="spacer"></span>
      <el-button size="small" @click="reset">重置</el-button>
      <el-button type="primary" size="small" @click="search">查询</el-button>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" style="width: 100%" size="small">
        <el-table-column label="图书" min-width="240">
          <template #default="{ row }">
            <div class="bk">
              <img class="cover" :src="row.coverImage" :alt="row.bookName" />
              <div class="bk-txt">
                <div class="bk-name">{{ row.bookName }}</div>
                <div class="bk-sub">{{ row.author }} · {{ row.publisher }}</div>
                <div class="bk-no">{{ row.bookNo }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120" prop="categoryName" />
        <el-table-column label="定价 / 售价" width="126" align="right">
          <template #default="{ row }">
            <div class="price">¥{{ fmt(row.salePrice) }}</div>
            <div class="list-price">定价 ¥{{ fmt(row.listPrice) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="86" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.stockQty <= 5 ? '#B02A37' : 'inherit', fontWeight: row.stockQty <= 5 ? 600 : 400 }">
              {{ row.stockQty }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="累计销量" width="92" align="right" prop="totalSales" />
        <el-table-column label="缺货次数" width="92" align="right" prop="shortageCount" />
        <el-table-column label="状态" width="94">
          <template #default="{ row }">
            <span class="st" :class="stCls(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="detail(row)">详情</el-button>
            <template v-if="canEdit">
              <el-button link type="primary" size="small" @click="$router.push(`/book/${row.id}/edit`)">编辑</el-button>
              <el-button link :type="row.status === '在售' ? 'warning' : 'success'" size="small"
                         @click="shelf(row)">{{ row.status === '在售' ? '下架' : '上架' }}</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size"
                       :page-sizes="[10, 20, 50]" @current-change="load" @size-change="search" />
      </div>
    </div>

    <el-drawer v-model="drawer" title="图书详情" size="520px">
      <div v-if="cur.id" class="dt">
        <div class="dt-top">
          <img class="cover-lg" :src="cur.coverImage" :alt="cur.bookName" />
          <div class="dt-info">
            <h3>{{ cur.bookName }}</h3>
            <p class="dt-sub">{{ cur.subTitle }}</p>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="作者">{{ cur.author }}</el-descriptions-item>
              <el-descriptions-item label="出版社">{{ cur.publisher }}</el-descriptions-item>
              <el-descriptions-item label="分类">{{ cur.parentCategory }} / {{ cur.categoryName }}</el-descriptions-item>
              <el-descriptions-item label="ISBN">{{ cur.isbn }}</el-descriptions-item>
              <el-descriptions-item label="定价 / 售价">¥{{ fmt(cur.listPrice) }} / ¥{{ fmt(cur.salePrice) }}</el-descriptions-item>
              <el-descriptions-item label="库存 / 锁定">{{ cur.stockQty }} / {{ cur.lockedQty }}</el-descriptions-item>
              <el-descriptions-item label="累计销量">{{ cur.totalSales }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
        <p class="dt-sum">{{ cur.summary }}</p>
        <h4 class="card-title" style="margin-top: 16px">最近库存流水</h4>
        <el-table :data="cur.stockFlows || []" size="small">
          <el-table-column label="时间" width="130">
            <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column prop="changeType" label="类型" width="80" />
          <el-table-column label="变动" width="70" align="right">
            <template #default="{ row }">
              <span :style="{ color: row.changeQty > 0 ? '#2B7955' : '#B02A37' }">
                {{ row.changeQty > 0 ? '+' : '' }}{{ row.changeQty }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="afterQty" label="结存" width="64" align="right" />
        </el-table>
        <h4 class="card-title" style="margin-top: 16px">读者书评（{{ (cur.reviewStat || {}).count || 0 }} 条，均分 {{ (cur.reviewStat || {}).avgScore || '—' }}）</h4>
        <div v-for="r in cur.reviews || []" :key="r.id" class="rv">
          <div class="rv-head">
            <img class="avatar" :src="r.avatar" alt="" />
            <b>{{ r.customerName }}</b>
            <el-rate :model-value="r.score" disabled size="small" />
            <span class="st st-green">{{ r.status }}</span>
          </div>
          <p class="rv-txt">{{ r.content }}</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Download } from '@element-plus/icons-vue'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime, fmtNum } from '@/utils/format'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const opts = ref({})
const drawer = ref(false)
const cur = ref({})
const q = reactive({ page: 1, size: 10, keyword: '', categoryId: null, status: '', publisher: '', sort: 'new' })

const canEdit = computed(() => ['ADMIN', 'OPS'].includes(store.user.roleCode))
const onSale = computed(() => Number((opts.value.books || []).filter((b) => b.stockQty > 0).length))

const flatCats = computed(() => {
  const out = []
  for (const c of opts.value.categories || []) {
    out.push({ id: c.id, label: c.categoryName })
    for (const s of c.children || []) out.push({ id: s.id, label: `${c.categoryName} / ${s.categoryName}` })
  }
  return out
})

const fmt = (v) => fmtNum(v, 2)
const stCls = (s) => (s === '在售' ? 'st-green' : s === '待上架' ? 'st-amber' : 'st-gray')

async function load() {
  loading.value = true
  try {
    const r = await api.bookPage({ ...q, keyword: q.keyword || undefined, status: q.status || undefined, publisher: q.publisher || undefined, categoryId: q.categoryId || undefined })
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
  Object.assign(q, { page: 1, keyword: '', categoryId: null, status: '', publisher: '', sort: 'new' })
  load()
}
async function detail(row) {
  cur.value = await api.bookDetail(row.id)
  drawer.value = true
}
async function shelf(row) {
  const target = row.status === '在售' ? '停售' : '在售'
  let reason = ''
  if (target === '停售') {
    const r = await ElMessageBox.prompt('下架后读者前台不再展示，请填写下架原因', '下架图书', {
      inputPlaceholder: '例如：供应商缺货，暂停销售',
      inputValidator: (v) => (v && v.trim().length >= 2 ? true : '请至少填写 2 个字')
    }).catch(() => null)
    if (!r) return
    reason = r.value
  }
  await api.onShelf(row.id, { targetStatus: target, reason })
  ElMessage.success(`已${target === '在售' ? '上架' : '下架'}`)
  load()
}
function exportCsv() {
  const head = ['图书编号', '书名', '作者', '出版社', '分类', '定价', '售价', '库存', '累计销量', '状态']
  const lines = rows.value.map((r) => [r.bookNo, r.bookName, r.author, r.publisher, r.categoryName, r.listPrice, r.salePrice, r.stockQty, r.totalSales, r.status])
  const csv = '\ufeff' + [head, ...lines].map((l) => l.map((x) => `"${x ?? ''}"`).join(',')).join('\n')
  const a = document.createElement('a')
  a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
  a.download = `图书列表_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  ElMessage.success('已导出当前页数据')
}

onMounted(async () => {
  opts.value = await api.options()
  load()
})
</script>

<style scoped>
.bk { display: flex; gap: 10px; align-items: center; }
.bk-txt { min-width: 0; }
.bk-name { font-weight: 600; font-size: 12.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bk-sub { color: var(--text-3); font-size: 11.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bk-no { color: var(--text-3); font-size: 11px; font-family: ui-monospace, Menlo, monospace; }
.price { font-weight: 600; color: var(--primary); }
.list-price { color: var(--text-3); font-size: 11px; }
.dt-top { display: flex; gap: 16px; }
.dt-info { flex: 1; min-width: 0; }
.dt-info h3 { margin: 0 0 2px; font-size: 15px; }
.dt-sub { color: var(--text-3); font-size: 12px; margin: 0 0 8px; }
.dt-sum { color: var(--text-2); line-height: 1.9; font-size: 12.5px; margin-top: 14px; background: var(--paper); padding: 10px 12px; border-radius: 4px; }
.rv { border-top: 1px dashed var(--line); padding: 8px 0; }
.rv-head { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.rv-txt { color: var(--text-2); font-size: 12px; margin: 4px 0 0; line-height: 1.8; }
</style>
