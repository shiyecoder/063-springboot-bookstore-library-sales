<template>
  <div>
    <div class="page-head">
      <h2>操作日志</h2>
      <span class="sub">所有写操作由后端统一留痕，只读不可改，用于答辩演示「谁在什么时候改了什么」</span>
      <span class="spacer"></span>
      <el-date-picker v-model="range" type="daterange" size="small" value-format="YYYY-MM-DD"
                      start-placeholder="开始日期" end-placeholder="结束日期" style="width: 250px"
                      @change="search" />
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="操作人 / 动作 / 业务单号 / 内容 / IP" clearable
                style="width: 300px" @keyup.enter="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="q.module" placeholder="全部模块" clearable @change="search">
        <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
      </el-select>
      <span class="spacer"></span>
      <el-button size="small" @click="reset">重置</el-button>
      <el-button type="primary" size="small" @click="search">查询</el-button>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.operTime, true) }}</template>
        </el-table-column>
        <el-table-column prop="operName" label="操作人" width="94" />
        <el-table-column label="模块" width="106">
          <template #default="{ row }"><span class="st st-gray">{{ row.module }}</span></template>
        </el-table-column>
        <el-table-column label="动作" width="86">
          <template #default="{ row }">
            <span class="st" :class="actCls(row.operType)">{{ row.operType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bizNo" label="业务单号" width="140">
          <template #default="{ row }">{{ row.bizNo || '—' }}</template>
        </el-table-column>
        <el-table-column prop="content" label="操作内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="120" />
        <el-table-column label="结果" width="76">
          <template #default="{ row }">
            <span class="st" :class="row.status === 1 ? 'st-green' : 'st-red'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </span>
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
import { fmtTime } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const range = ref([])
const q = reactive({ page: 1, size: 10, keyword: '', module: '' })

const modules = computed(() => [...new Set(rows.value.map((r) => r.module).filter(Boolean))])
const actCls = (t) => ({ 新增: 'st-green', 修改: 'st-blue', 删除: 'st-red', 审核: 'st-amber', 登录: 'st-gray' }[t] || 'st-gray')

async function load() {
  loading.value = true
  try {
    const r = await api.logPage({
      ...q,
      keyword: q.keyword || undefined,
      module: q.module || undefined,
      start: range.value && range.value[0] ? range.value[0] : undefined,
      end: range.value && range.value[1] ? range.value[1] : undefined
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
  Object.assign(q, { page: 1, keyword: '', module: '' })
  range.value = []
  load()
}

onMounted(load)
</script>
