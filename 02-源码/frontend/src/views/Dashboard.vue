<template>
  <div>
    <div class="page-head">
      <h2>数据看板</h2>
      <span class="sub">数据截至 {{ now }}，指标口径与需求文档一致</span>
      <span class="spacer"></span>
      <el-button :icon="Refresh" size="small" @click="load">刷新</el-button>
      <el-button type="primary" size="small" @click="$router.push('/report')">销售报表</el-button>
    </div>

    <div class="kpis cols8">
      <div v-for="k in kpis" :key="k.label" class="kpi">
        <div class="label">{{ k.label }}</div>
        <div class="value" :style="k.color ? { color: k.color } : null">
          {{ k.value }}<span v-if="k.unit" class="unit">{{ k.unit }}</span>
        </div>
      </div>
    </div>

    <div class="dash">
      <div class="card span2">
        <h3 class="card-title">近 30 天成交趋势 <span class="tag">下单量 / 成交金额</span></h3>
        <Chart :option="trendOpt" />
      </div>
      <div class="card">
        <h3 class="card-title">分类销售占比 <span class="tag">近 30 天</span></h3>
        <Chart :option="shareOpt" />
      </div>
      <div class="card">
        <h3 class="card-title">库存周转分布 <span class="tag">按图书册数</span></h3>
        <Chart :option="turnoverOpt" />
      </div>

      <div class="card span2">
        <h3 class="card-title">出版社销量 TOP10 <span class="tag">累计成交件数</span></h3>
        <Chart :option="publisherOpt" />
      </div>
      <div class="card">
        <h3 class="card-title">订单转化漏斗 <span class="tag">全量订单</span></h3>
        <Chart :option="funnelOpt" />
      </div>
      <div class="card">
        <h3 class="card-title">退换原因分布 <span class="tag">近 6 个月</span></h3>
        <Chart :option="returnOpt" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import Chart from '@/components/Chart.vue'
import { api } from '@/api'
import { fmtNum, fmtTime } from '@/utils/format'

const d = ref({})
const now = ref(fmtTime(new Date()))
const C = ['#3A5A8C', '#6B8CBE', '#C9A227', '#2B7955', '#B4761E', '#8A6FA8', '#B02A37']

const kpis = computed(() => {
  const v = d.value
  return [
    { label: '今日订单', value: fmtNum(v.todayOrders), unit: '单' },
    { label: '今日销售额', value: '¥' + fmtNum(v.todaySales, 2) },
    { label: '待发货', value: fmtNum(v.waitDeliver), unit: '单', color: '#B4761E' },
    { label: '在售图书', value: fmtNum(v.onSaleBooks), unit: '种' },
    { label: '缺货图书', value: fmtNum(v.shortageBooks), unit: '种', color: '#B02A37' },
    { label: '客单价', value: '¥' + fmtNum(v.avgOrderAmount, 2) },
    { label: '退换率', value: fmtNum(v.returnRate, 1), unit: '%' },
    { label: '待补货', value: fmtNum(v.needReplenish), unit: '种', color: '#C9A227' }
  ]
})

const trendOpt = computed(() => {
  const t = d.value.trend || []
  return {
    grid: { left: 46, right: 44, top: 28, bottom: 26 },
    tooltip: { trigger: 'axis' },
    legend: { right: 0, top: 0, itemWidth: 10, itemHeight: 8, textStyle: { fontSize: 11 } },
    xAxis: {
      type: 'category',
      data: t.map((i) => i.label),
      axisLine: { lineStyle: { color: '#E3E6E9' } },
      axisLabel: { color: '#8A939B', fontSize: 10.5, interval: 3 },
      axisTick: { show: false }
    },
    yAxis: [
      { type: 'value', name: '单', nameTextStyle: { fontSize: 10, color: '#8A939B' }, splitLine: { lineStyle: { color: '#F0F2F4' } }, axisLabel: { color: '#8A939B', fontSize: 10.5 } },
      { type: 'value', name: '元', nameTextStyle: { fontSize: 10, color: '#8A939B' }, splitLine: { show: false }, axisLabel: { color: '#8A939B', fontSize: 10.5 } }
    ],
    series: [
      { name: '成交金额', type: 'line', smooth: true, yAxisIndex: 1, symbol: 'none',
        data: t.map((i) => i.amount), lineStyle: { width: 2, color: C[2] },
        areaStyle: { color: 'rgba(201,162,39,.13)' } },
      { name: '下单量', type: 'bar', barWidth: '46%', data: t.map((i) => i.orders),
        itemStyle: { color: C[1], borderRadius: [2, 2, 0, 0] } }
    ]
  }
})

const shareOpt = computed(() => {
  const s = d.value.categoryShare || []
  return {
    tooltip: { trigger: 'item', valueFormatter: (v) => '¥' + fmtNum(v, 2) },
    legend: { orient: 'vertical', right: 0, top: 'center', itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 11 } },
    color: C,
    series: [{
      type: 'pie', radius: ['46%', '70%'], center: ['36%', '52%'], avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false }, labelLine: { show: false },
      data: s.map((i) => ({ name: i.name, value: i.value }))
    }]
  }
})

const turnoverOpt = computed(() => {
  const s = d.value.turnoverDist || []
  return {
    grid: { left: 40, right: 16, top: 16, bottom: 24 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: s.map((i) => i.name), axisTick: { show: false },
      axisLine: { lineStyle: { color: '#E3E6E9' } }, axisLabel: { color: '#8A939B', fontSize: 10.5 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F0F2F4' } }, axisLabel: { color: '#8A939B', fontSize: 10.5 } },
    series: [{
      type: 'bar', barWidth: '48%', data: s.map((i, x) => ({ value: i.value, itemStyle: { color: C[x % C.length] } })),
      itemStyle: { borderRadius: [2, 2, 0, 0] }, label: { show: true, position: 'top', fontSize: 10.5, color: '#5B6770' }
    }]
  }
})

const publisherOpt = computed(() => {
  const s = [...(d.value.publisherTop || [])].reverse()
  return {
    grid: { left: 108, right: 34, top: 12, bottom: 18 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#F0F2F4' } }, axisLabel: { color: '#8A939B', fontSize: 10.5 } },
    yAxis: { type: 'category', data: s.map((i) => i.name), axisTick: { show: false },
      axisLine: { lineStyle: { color: '#E3E6E9' } }, axisLabel: { color: '#5B6770', fontSize: 11 } },
    series: [{
      type: 'bar', barWidth: '52%', data: s.map((i) => i.value),
      itemStyle: { color: C[0], borderRadius: [0, 2, 2, 0] },
      label: { show: true, position: 'right', fontSize: 10.5, color: '#5B6770' }
    }]
  }
})

const funnelOpt = computed(() => {
  const s = d.value.funnel || []
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 单' },
    color: C,
    series: [{
      type: 'funnel', left: '8%', right: '8%', top: 8, bottom: 8, minSize: '28%',
      gap: 2, sort: 'descending',
      label: { show: true, position: 'inside', fontSize: 11, color: '#fff', formatter: '{b} {c}' },
      itemStyle: { borderWidth: 0 },
      data: s.map((i) => ({ name: i.name, value: i.value }))
    }]
  }
})

const returnOpt = computed(() => {
  const s = d.value.returnReason || []
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 单（{d}%）' },
    legend: { orient: 'vertical', right: 0, top: 'center', itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 11 } },
    color: [C[6], C[4], C[0], C[1], C[3]],
    series: [{
      type: 'pie', radius: '66%', center: ['36%', '52%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false }, labelLine: { show: false },
      data: s.map((i) => ({ name: i.name, value: i.value }))
    }]
  }
})

async function load() {
  d.value = await api.dashboard()
  now.value = fmtTime(new Date())
}

onMounted(load)
</script>
