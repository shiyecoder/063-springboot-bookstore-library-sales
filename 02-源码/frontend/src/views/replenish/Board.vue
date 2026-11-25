<template>
  <div>
    <div class="page-head">
      <h2>补货看板</h2>
      <span class="sub">五因子加权打分（销量35 / 周转25 / 在途20 / 季节10 / 缺货10），分数越高越该补</span>
      <span class="spacer"></span>
      <el-button size="small" @click="load">刷新</el-button>
      <el-button v-if="canGen" type="primary" size="small" :loading="genning" @click="generate">
        重新计算建议
      </el-button>
    </div>

    <div class="kpis">
      <div class="kpi"><div class="label">建议总数</div><div class="value">{{ stat.total }}<span class="unit">种</span></div></div>
      <div class="kpi"><div class="label">已采纳</div><div class="value" style="color:#2B7955">{{ stat.adopted }}<span class="unit">种</span></div></div>
      <div class="kpi"><div class="label">采纳率</div><div class="value">{{ stat.adoptRate }}<span class="unit">%</span></div></div>
      <div class="kpi"><div class="label">人工调整率</div><div class="value" style="color:#B4761E">{{ stat.manualRate }}<span class="unit">%</span></div></div>
    </div>

    <div class="dash" style="grid-auto-rows: 250px; margin-bottom: 12px">
      <div class="card">
        <h3 class="card-title">销量分级分布 <span class="tag">畅销 / 平销 / 滞销 / 断销</span></h3>
        <Chart :option="levelOpt" />
      </div>
      <div class="card span2">
        <h3 class="card-title">建议补货量 TOP8 <span class="tag">按建议数量降序</span></h3>
        <Chart :option="topOpt" />
      </div>
      <div class="card">
        <h3 class="card-title">权重构成 <span class="tag">当前启用版本</span></h3>
        <Chart :option="weightOpt" />
      </div>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="书名 / 建议单号" clearable style="width: 220px" @keyup.enter="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="q.level" placeholder="全部等级" clearable @change="search">
        <el-option label="畅销" value="畅销" />
        <el-option label="平销" value="平销" />
        <el-option label="滞销" value="滞销" />
        <el-option label="断销" value="断销" />
      </el-select>
      <el-select v-model="q.adopt" placeholder="全部采纳状态" clearable @change="search">
        <el-option label="未采纳" value="未采纳" />
        <el-option label="已采纳" value="已采纳" />
        <el-option label="已忽略" value="已忽略" />
      </el-select>
      <span class="spacer"></span>
      <el-button size="small" @click="$router.push('/replenish/rule')">规则配置</el-button>
      <el-button size="small" @click="$router.push('/replenish/trace')">计算留痕</el-button>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column label="图书" min-width="200">
          <template #default="{ row }">
            <div class="bk">
              <img class="cover" :src="row.coverImage" alt="" />
              <div>
                <div class="bk-name">{{ row.bookName }}</div>
                <div class="bk-sub">{{ row.author }} · {{ row.categoryName }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="76">
          <template #default="{ row }">
            <span class="st" :class="lvCls(row.salesLevel)">{{ row.salesLevel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="优先级分" width="96">
          <template #default="{ row }">
            <div class="score">
              <b>{{ score(row.totalScore) }}</b>
              <el-progress :percentage="score(row.totalScore)" :show-text="false" :stroke-width="4"
                           :color="scoreColor(row.totalScore)" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="近30天销量" width="94" align="right" prop="sales30d" />
        <el-table-column label="可售库存" width="86" align="right" prop="stockQty" />
        <el-table-column label="在途" width="66" align="right" prop="onwayQty" />
        <el-table-column label="周转天数" width="88" align="right">
          <template #default="{ row }">{{ row.turnoverDays }}</template>
        </el-table-column>
        <el-table-column label="建议量" width="80" align="right">
          <template #default="{ row }">
            <b :style="{ color: row.suggestQty > 0 ? '#B02A37' : '#8A939B' }">{{ row.suggestQty }}</b>
          </template>
        </el-table-column>
        <el-table-column label="采纳状态" width="88">
          <template #default="{ row }">
            <span class="st" :class="adoptCls(row.adoptStatus)">{{ row.adoptStatus }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="82" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/replenish/detail/${row.id}`)">
              详情
            </el-button>
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
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import Chart from '@/components/Chart.vue'
import { api } from '@/api'
import { store } from '@/store/user'

const rows = ref([])
const total = ref(0)
const stat = ref({})
const rule = ref({})
const loading = ref(false)
const genning = ref(false)
const q = reactive({ page: 1, size: 10, keyword: '', level: '', adopt: '' })
const C = ['#2B7955', '#3A5A8C', '#B4761E', '#B02A37']

const canGen = computed(() => ['ADMIN', 'OPS', 'BUYER'].includes(store.user.roleCode))
const score = (v) => Math.round(Number(v || 0))
const scoreColor = (v) => (v >= 80 ? '#B02A37' : v >= 55 ? '#B4761E' : '#3A5A8C')
const lvCls = (l) => ({ 畅销: 'st-red', 平销: 'st-blue', 滞销: 'st-amber', 断销: 'st-gray' }[l] || 'st-gray')
const adoptCls = (a) => ({ 已采纳: 'st-green', 未采纳: 'st-gray', 已忽略: 'st-amber' }[a] || 'st-gray')

const levelOpt = computed(() => {
  const s = stat.value.levelDist || []
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 种（{d}%）' },
    legend: { bottom: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 11 } },
    color: C,
    series: [{
      type: 'pie', radius: ['44%', '66%'], center: ['50%', '44%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}', fontSize: 11, color: '#5B6770' },
      data: s.map((i) => ({ name: i.name, value: i.value }))
    }]
  }
})

const topOpt = computed(() => {
  const s = [...(stat.value.topSuggest || [])].reverse()
  return {
    grid: { left: 110, right: 40, top: 12, bottom: 20 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#F0F2F4' } }, axisLabel: { color: '#8A939B', fontSize: 10.5 } },
    yAxis: { type: 'category', data: s.map((i) => i.name), axisTick: { show: false },
      axisLine: { lineStyle: { color: '#E3E6E9' } }, axisLabel: { color: '#5B6770', fontSize: 11, width: 96, overflow: 'truncate' } },
    series: [{
      type: 'bar', barWidth: '54%',
      data: s.map((i) => ({
        value: i.value,
        itemStyle: { color: i.level === '畅销' ? '#B02A37' : i.level === '平销' ? '#3A5A8C' : '#B4761E', borderRadius: [0, 2, 2, 0] }
      })),
      label: { show: true, position: 'right', fontSize: 10.5, color: '#5B6770' }
    }]
  }
})

const weightOpt = computed(() => {
  const r = rule.value
  const data = [
    { name: '近30天销量', value: r.wSales || 35 },
    { name: '库存周转', value: r.wTurnover || 25 },
    { name: '在途数量', value: r.wOnway || 20 },
    { name: '季节系数', value: r.wSeason || 10 },
    { name: '近90天缺货', value: r.wStockout || 10 }
  ]
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c}%' },
    legend: { bottom: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10.5 } },
    color: ['#3A5A8C', '#6B8CBE', '#C9A227', '#2B7955', '#8A6FA8'],
    series: [{
      type: 'pie', radius: ['40%', '62%'], center: ['50%', '42%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false }, labelLine: { show: false },
      data
    }]
  }
})

async function load() {
  loading.value = true
  try {
    const [p, s, rl] = await Promise.all([
      api.replenishPage({ ...q, keyword: q.keyword || undefined, level: q.level || undefined, adopt: q.adopt || undefined }),
      api.replenishStat(),
      api.ruleList()
    ])
    rows.value = p.rows || []
    total.value = p.total || 0
    stat.value = s || {}
    rule.value = (rl || [])[0] || {}
  } finally {
    loading.value = false
  }
}
function search() {
  q.page = 1
  load()
}
async function generate() {
  genning.value = true
  try {
    const r = await api.replenishGenerate()
    ElMessage.success(`已按规则 ${r.ruleVersion || '当前版本'} 重新计算 ${r.count ?? ''} 条建议`)
    await load()
  } finally {
    genning.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.bk { display: flex; gap: 8px; align-items: center; }
.bk-name { font-size: 12.5px; font-weight: 500; }
.bk-sub { color: var(--text-3); font-size: 11px; }
.score b { font-size: 13px; color: var(--primary); }
.score :deep(.el-progress) { margin-top: 1px; }
</style>
