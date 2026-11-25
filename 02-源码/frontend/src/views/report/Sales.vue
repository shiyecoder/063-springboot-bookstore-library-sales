<template>
  <div>
    <div class="page-head">
      <h2>销售报表</h2>
      <span class="sub">口径：剔除已取消与已退款订单，按签收时间归属，近 {{ q.days }} 天</span>
      <span class="spacer"></span>
      <el-radio-group v-model="q.dim" size="small" @change="load">
        <el-radio-button label="category">按分类</el-radio-button>
        <el-radio-button label="publisher">按出版社</el-radio-button>
        <el-radio-button label="book">按图书</el-radio-button>
      </el-radio-group>
      <el-select v-model="q.days" size="small" style="width: 112px; margin-left: 8px" @change="load">
        <el-option label="近 7 天" :value="7" />
        <el-option label="近 30 天" :value="30" />
        <el-option label="近 90 天" :value="90" />
        <el-option label="近 180 天" :value="180" />
      </el-select>
      <el-button size="small" :icon="Download" @click="exportCsv">导出</el-button>
    </div>

    <div class="kpis">
      <div class="kpi"><div class="label">销售金额</div><div class="value">¥{{ fmt(r.totalAmount) }}</div></div>
      <div class="kpi"><div class="label">销售件数</div><div class="value">{{ r.totalQty }}<span class="unit">件</span></div></div>
      <div class="kpi"><div class="label">平均单价</div><div class="value">¥{{ avgPrice }}</div></div>
      <div class="kpi"><div class="label">统计维度</div><div class="value" style="font-size: 16px">{{ dimName }}</div></div>
    </div>

    <div class="rep-top">
      <div class="card">
        <h3 class="card-title">金额分布 <span class="tag">TOP{{ (r.list || []).length }}</span></h3>
        <Chart :option="barOpt" />
      </div>
      <div class="card">
        <h3 class="card-title">月度趋势 <span class="tag">近 6 个月成交金额</span></h3>
        <Chart :option="monthOpt" />
      </div>
    </div>

    <div class="card">
      <el-table :data="r.list || []" size="small" v-loading="loading" show-summary :summary-method="summary">
        <el-table-column type="index" label="#" width="52" />
        <el-table-column :label="dimName" min-width="180" prop="name" />
        <el-table-column label="销售件数" width="110" align="right" prop="qty" />
        <el-table-column label="销售金额" width="140" align="right">
          <template #default="{ row }"><b style="color: var(--primary)">¥{{ fmt(row.amount) }}</b></template>
        </el-table-column>
        <el-table-column label="平均单价" width="120" align="right">
          <template #default="{ row }">¥{{ fmt(row.avgAmount) }}</template>
        </el-table-column>
        <el-table-column label="金额占比" min-width="190">
          <template #default="{ row }">
            <el-progress :percentage="pct(row.amount)" :stroke-width="8" :color="'#3A5A8C'"
                         :format="(p) => p + '%'" />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import Chart from '@/components/Chart.vue'
import { api } from '@/api'
import { fmtNum } from '@/utils/format'

const r = ref({})
const loading = ref(false)
const q = reactive({ dim: 'category', days: 30 })
const fmt = (v) => fmtNum(v, 2)
const dimName = computed(() => ({ category: '分类', publisher: '出版社', book: '图书' }[q.dim] || '分类'))
const avgPrice = computed(() => (r.value.totalQty ? fmt(Number(r.value.totalAmount) / Number(r.value.totalQty)) : '0.00'))

const pct = (v) => {
  const t = Number(r.value.totalAmount || 0)
  return t ? Math.round((Number(v) / t) * 1000) / 10 : 0
}

const barOpt = computed(() => {
  const s = [...(r.value.list || [])].reverse()
  return {
    grid: { left: 96, right: 54, top: 12, bottom: 18 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => '¥' + fmtNum(v, 2) },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#F0F2F4' } }, axisLabel: { color: '#8A939B', fontSize: 10.5 } },
    yAxis: { type: 'category', data: s.map((i) => i.name), axisTick: { show: false },
      axisLine: { lineStyle: { color: '#E3E6E9' } }, axisLabel: { color: '#5B6770', fontSize: 11, width: 88, overflow: 'truncate' } },
    series: [{
      type: 'bar', barWidth: '52%', data: s.map((i) => i.amount),
      itemStyle: { color: '#3A5A8C', borderRadius: [0, 2, 2, 0] },
      label: { show: true, position: 'right', fontSize: 10.5, color: '#5B6770', formatter: (p) => '¥' + fmtNum(p.value, 0) }
    }]
  }
})

const monthOpt = computed(() => {
  // 后端按月倒序返回，图表按时间正序展示，避免横轴出现「从新到旧」的错觉
  const s = [...(r.value.monthly || [])].reverse()
  return {
    grid: { left: 50, right: 44, top: 26, bottom: 26 },
    tooltip: { trigger: 'axis' },
    legend: { right: 0, top: 0, itemWidth: 10, itemHeight: 8, textStyle: { fontSize: 11 } },
    xAxis: { type: 'category', data: s.map((i) => i.name), axisTick: { show: false },
      axisLine: { lineStyle: { color: '#E3E6E9' } }, axisLabel: { color: '#8A939B', fontSize: 10.5 } },
    yAxis: [
      { type: 'value', splitLine: { lineStyle: { color: '#F0F2F4' } }, axisLabel: { color: '#8A939B', fontSize: 10.5 } },
      { type: 'value', splitLine: { show: false }, axisLabel: { color: '#8A939B', fontSize: 10.5 } }
    ],
    series: [
      { name: '成交金额', type: 'bar', barWidth: '42%', data: s.map((i) => i.amount),
        itemStyle: { color: '#C9A227', borderRadius: [2, 2, 0, 0] } },
      { name: '订单数', type: 'line', yAxisIndex: 1, smooth: true, symbol: 'circle', symbolSize: 5,
        data: s.map((i) => i.orders), lineStyle: { width: 2, color: '#3A5A8C' }, itemStyle: { color: '#3A5A8C' } }
    ]
  }
})

function summary({ columns }) {
  return columns.map((c, i) => {
    if (i === 0) return '合计'
    if (i === 2) return String(r.value.totalQty ?? '')
    if (i === 3) return '¥' + fmt(r.value.totalAmount)
    if (i === 4) return '¥' + avgPrice.value
    if (i === 5) return '100%'
    return ''
  })
}

async function load() {
  loading.value = true
  try {
    r.value = await api.report({ dim: q.dim, days: q.days })
  } finally {
    loading.value = false
  }
}
function exportCsv() {
  const head = [dimName.value, '销售件数', '销售金额', '平均单价']
  const lines = (r.value.list || []).map((x) => [x.name, x.qty, x.amount, x.avgAmount])
  const csv = '\ufeff' + [head, ...lines].map((l) => l.map((v) => `"${v ?? ''}"`).join(',')).join('\n')
  const a = document.createElement('a')
  a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
  a.download = `销售报表_${dimName.value}_近${q.days}天.csv`
  a.click()
  ElMessage.success('已导出')
}

onMounted(load)
</script>

<style scoped>
.rep-top { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px; }
.rep-top .card { height: 268px; display: flex; flex-direction: column; }
.rep-top .chart-body { flex: 1; min-height: 0; }
@media (max-width: 1100px) { .rep-top { grid-template-columns: 1fr; } }
</style>
