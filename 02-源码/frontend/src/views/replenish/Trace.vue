<template>
  <div>
    <div class="page-head">
      <h2>计算留痕</h2>
      <span class="sub">每条建议都固化当时的输入与权重版本，规则调整后仍可复算，便于答辩时说明「为什么是这个数」</span>
      <span class="spacer"></span>
      <el-select v-model="q.version" size="small" style="width: 140px" placeholder="全部版本" clearable @change="search">
        <el-option v-for="v in versions" :key="v" :label="v" :value="v" />
      </el-select>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <div class="kpis">
      <div class="kpi"><div class="label">建议总数</div><div class="value">{{ total }}<span class="unit">条</span></div></div>
      <div class="kpi"><div class="label">覆盖规则版本</div><div class="value">{{ versions.length }}<span class="unit">个</span></div></div>
      <div class="kpi"><div class="label">本页平均优先级分</div><div class="value">{{ avgScore }}</div></div>
      <div class="kpi"><div class="label">人工修正</div><div class="value" style="color:#B4761E">{{ fixed }}<span class="unit">条</span></div></div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" size="small" row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand">
              <div class="ex-col">
                <h4>输入</h4>
                <p>近30天销量 <b>{{ row.sales30d }}</b> 件 · 日均 <b>{{ row.avgDaily }}</b> 件/天</p>
                <p>可售库存 <b>{{ row.stockQty }}</b> · 在途 <b>{{ row.onwayQty }}</b> · 周转 <b>{{ row.turnoverDays }}</b> 天</p>
                <p>季节系数 <b>{{ row.seasonFactor }}</b> · 原等级 <b>{{ row.originLevel }}</b></p>
              </div>
              <div class="ex-col">
                <h4>归一化得分 × 权重</h4>
                <p>
                  销量 {{ row.sSales }} × {{ (row.weights || {}).wSales }}% +
                  周转 {{ row.sTurnover }} × {{ (row.weights || {}).wTurnover }}% +
                  在途 {{ row.sOnway }} × {{ (row.weights || {}).wOnway }}% +
                  季节 {{ row.sSeason }} × {{ (row.weights || {}).wSeason }}% +
                  缺货 {{ row.sStockout }} × {{ (row.weights || {}).wStockout }}%
                  = <b style="color: var(--primary)">{{ row.totalScore }}</b>
                </p>
                <p class="ex-desc">{{ row.levelDesc }}</p>
              </div>
              <div class="ex-col narrow">
                <h4>结论</h4>
                <p>建议量 <b style="color: var(--danger); font-size: 16px">{{ row.suggestQty }}</b> 册</p>
                <p>采纳状态 <span class="st" :class="adoptCls(row.adoptStatus)">{{ row.adoptStatus }}</span></p>
                <p v-if="row.manualQty">人工修正为 {{ row.manualQty }} 册<br />原因：{{ row.manualNote }}</p>
                <el-button link type="primary" size="small" @click="$router.push(`/replenish/detail/${row.id}`)">
                  查看完整计算过程
                </el-button>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="planNo" label="建议单号" width="140" />
        <el-table-column label="图书" min-width="180">
          <template #default="{ row }">
            <div class="bk">
              <img class="cover" :src="row.coverImage" alt="" />
              <span class="bk-name">{{ row.bookName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="规则版本" width="86">
          <template #default="{ row }"><span class="st st-gray">{{ row.ruleVersion }}</span></template>
        </el-table-column>
        <el-table-column label="五因子得分" width="214">
          <template #default="{ row }">
            <span class="sc">{{ row.sSales }}</span> /
            <span class="sc">{{ row.sTurnover }}</span> /
            <span class="sc">{{ row.sOnway }}</span> /
            <span class="sc">{{ row.sSeason }}</span> /
            <span class="sc">{{ row.sStockout }}</span>
          </template>
        </el-table-column>
        <el-table-column label="优先级分" width="90" align="right">
          <template #default="{ row }">
            <b :style="{ color: row.totalScore >= 80 ? '#B02A37' : row.totalScore >= 55 ? '#B4761E' : '#3A5A8C' }">
              {{ row.totalScore }}
            </b>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="76">
          <template #default="{ row }">
            <span class="st" :class="lvCls(row.salesLevel)">{{ row.salesLevel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="建议量" width="78" align="right" prop="suggestQty" />
        <el-table-column label="采纳" width="84">
          <template #default="{ row }">
            <span class="st" :class="adoptCls(row.adoptStatus)">{{ row.adoptStatus }}</span>
          </template>
        </el-table-column>
        <el-table-column label="计算时间" width="140">
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
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const weights = ref({})
const q = reactive({ page: 1, size: 10, version: '' })

const versions = computed(() => [...new Set(rows.value.map((r) => r.ruleVersion))].sort().reverse())
const avgScore = computed(() => {
  if (!rows.value.length) return '—'
  return (rows.value.reduce((s, r) => s + Number(r.totalScore || 0), 0) / rows.value.length).toFixed(1)
})
const fixed = computed(() => rows.value.filter((r) => r.manualQty !== null && r.manualQty !== undefined).length)

const lvCls = (l) => ({ 畅销: 'st-red', 平销: 'st-blue', 滞销: 'st-amber', 断销: 'st-gray' }[l] || 'st-gray')
const adoptCls = (a) => ({ 已采纳: 'st-green', 未采纳: 'st-gray', 已忽略: 'st-amber' }[a] || 'st-gray')

async function load() {
  loading.value = true
  try {
    const r = await api.replenishPage({ page: q.page, size: q.size })
    rows.value = r.rows || []
    total.value = r.total || 0
    if (!weights.value.wSales) weights.value = await api.ruleList().then((x) => (x || [])[0] || {})
  } finally {
    loading.value = false
  }
}
function search() {
  q.page = 1
  load()
}

onMounted(load)
</script>

<style scoped>
.bk { display: flex; gap: 8px; align-items: center; }
.bk-name { font-size: 12.5px; }
.sc { color: var(--text-2); font-family: ui-monospace, Menlo, monospace; font-size: 11.5px; }
.expand { display: grid; grid-template-columns: 1fr 1.4fr 220px; gap: 18px; padding: 6px 12px 10px 44px; }
.ex-col h4 { margin: 0 0 6px; font-size: 12px; color: var(--primary); }
.ex-col p { margin: 0; font-size: 11.5px; color: var(--text-2); line-height: 2; }
.ex-desc { background: var(--paper); padding: 7px 9px; border-radius: 3px; margin-top: 6px !important; }
.ex-col.narrow { border-left: 1px dashed var(--line); padding-left: 16px; }
</style>
