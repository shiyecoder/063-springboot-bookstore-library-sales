<template>
  <div>
    <div class="page-head">
      <h2>库存流水</h2>
      <span class="sub">每一条流水满足「结存 = 变动前 + 变动」，且最后一条与图书当前库存一致</span>
      <span class="spacer"></span>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="流水号 / 图书 / 业务单号" clearable @keyup.enter="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="q.changeType" placeholder="全部类型" clearable @change="search" style="width: 150px">
        <el-option label="入库" value="入库" />
        <el-option label="销售出库" value="销售出库" />
        <el-option label="退货入库" value="退货入库" />
        <el-option label="盘盈" value="盘盈" />
        <el-option label="盘亏" value="盘亏" />
        <el-option label="取消回补" value="取消回补" />
      </el-select>
      <el-select v-model="q.bookId" placeholder="全部图书" clearable filterable @change="search" style="width: 220px">
        <el-option v-for="b in opts.books" :key="b.id" :label="b.bookName" :value="b.id" />
      </el-select>
      <span class="spacer"></span>
      <el-tag v-if="badRows" type="danger" effect="plain" size="small">本页发现 {{ badRows }} 条账实不符</el-tag>
      <el-tag v-else type="success" effect="plain" size="small">本页账实校验通过</el-tag>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column prop="flowNo" label="流水号" width="150" />
        <el-table-column label="图书" min-width="190">
          <template #default="{ row }">
            <div class="bk">
              <img class="cover" :src="row.coverImage" alt="" />
              <span class="bk-name">{{ row.bookName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="94">
          <template #default="{ row }">
            <span class="st" :class="typeCls(row.changeType)">{{ row.changeType }}</span>
          </template>
        </el-table-column>
        <el-table-column label="变动前" width="80" align="right" prop="beforeQty" />
        <el-table-column label="变动" width="78" align="right">
          <template #default="{ row }">
            <b :style="{ color: row.changeQty > 0 ? '#2B7955' : '#B02A37' }">
              {{ row.changeQty > 0 ? '+' : '' }}{{ row.changeQty }}
            </b>
          </template>
        </el-table-column>
        <el-table-column label="结存" width="76" align="right" prop="afterQty" />
        <el-table-column label="校验" width="64" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.afterQty === row.beforeQty + row.changeQty" color="#2B7955"><CircleCheck /></el-icon>
            <el-icon v-else color="#B02A37"><CircleClose /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="bizNo" label="业务单号" width="150">
          <template #default="{ row }">{{ row.bizNo || '—' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作人" width="88" prop="operatorName" />
        <el-table-column label="时间" width="140">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
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
import { Search, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const opts = ref({})
const q = reactive({ page: 1, size: 10, keyword: '', changeType: '', bookId: null })

const badRows = computed(() => rows.value.filter((r) => r.afterQty !== r.beforeQty + r.changeQty).length)

const typeCls = (t) => ({ 入库: 'st-green', 退货入库: 'st-blue', 取消回补: 'st-blue', 销售出库: 'st-amber', 盘盈: 'st-green', 盘亏: 'st-red' }[t] || 'st-gray')

async function load() {
  loading.value = true
  try {
    const r = await api.flowPage({
      ...q,
      keyword: q.keyword || undefined,
      changeType: q.changeType || undefined,
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

onMounted(async () => {
  opts.value = await api.options()
  load()
})
</script>

<style scoped>
.bk { display: flex; gap: 8px; align-items: center; }
.bk-name { font-size: 12.5px; }
</style>
