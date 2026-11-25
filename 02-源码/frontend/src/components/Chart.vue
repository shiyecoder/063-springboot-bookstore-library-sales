<template>
  <div ref="el" class="chart-body"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, default: () => ({}) },
  theme: { type: String, default: '' }
})
const el = ref(null)
let chart = null
let ro = null

function render() {
  if (!el.value) return
  if (!props.option || !props.option.series || !props.option.series.length) return
  if (!chart) chart = echarts.init(el.value, props.theme, { renderer: 'canvas' })
  chart.setOption(props.option, true)
  chart.resize()
}

onMounted(async () => {
  await nextTick()
  render()
  // 容器尺寸变化（侧栏折叠、窗口缩放）时保持图表自适应
  if (window.ResizeObserver) {
    ro = new ResizeObserver(() => chart && chart.resize())
    ro.observe(el.value)
  }
})

watch(() => props.option, () => render(), { deep: true })

onBeforeUnmount(() => {
  if (ro) ro.disconnect()
  if (chart) { chart.dispose(); chart = null }
})
</script>
