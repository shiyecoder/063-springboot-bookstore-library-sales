<template>
  <div v-loading="loading">
    <div class="page-head">
      <h2>补货建议详情</h2>
      <span class="sub">{{ p.planNo }} · 规则版本 {{ p.ruleVersion }}</span>
      <span class="st" :class="lvCls(p.salesLevel)" style="margin-left: 4px">{{ p.salesLevel }}</span>
      <span class="spacer"></span>
      <el-button size="small" @click="$router.push('/replenish')">返回看板</el-button>
    </div>

    <div class="dt-wrap">
      <div class="col">
        <div class="card">
          <div class="book">
            <img class="cover-lg" :src="p.coverImage" alt="" />
            <div class="book-txt">
              <h3>{{ p.bookName }}</h3>
              <p class="sub">{{ p.author }} · {{ p.publisher }} · {{ p.categoryName }}</p>
              <div class="nums">
                <div><span>近30天销量</span><b>{{ p.sales30d }}</b></div>
                <div><span>可售库存</span><b>{{ p.stockQty }}</b></div>
                <div><span>在途数量</span><b>{{ p.onwayQty }}</b></div>
                <div><span>周转天数</span><b>{{ p.turnoverDays }}</b></div>
                <div><span>季节系数</span><b>{{ p.seasonFactor }}</b></div>
                <div><span>日均销量</span><b>{{ p.avgDaily }}</b></div>
              </div>
            </div>
          </div>
          <p class="desc">{{ p.levelDesc }}</p>
        </div>

        <div class="card">
          <h3 class="card-title">五因子拆解 <span class="tag">加权合计 = 优先级分 {{ p.totalScore }}</span></h3>
          <el-table :data="factors" size="small" show-summary :summary-method="summary">
            <el-table-column prop="name" label="因子" width="120" />
            <el-table-column prop="raw" label="原始值" min-width="150" />
            <el-table-column prop="score" label="归一化得分" width="98" align="right" />
            <el-table-column label="权重" width="72" align="right">
              <template #default="{ row }">{{ row.weight }}%</template>
            </el-table-column>
            <el-table-column label="加权贡献" width="92" align="right">
              <template #default="{ row }">
                <b style="color: var(--primary)">{{ row.contribution }}</b>
              </template>
            </el-table-column>
            <el-table-column label="占比" min-width="110">
              <template #default="{ row }">
                <el-progress :percentage="Number(row.contribution)" :show-text="false" :stroke-width="5"
                             :color="row.color" />
              </template>
            </el-table-column>
          </el-table>
          <div class="formula">
            建议补货量 = 日均销量 × (30 + 安全天数) × 季节系数 − 可售库存 − 在途
            <span class="calc">
              = {{ p.avgDaily }} × (30 + {{ safety }}) × {{ p.seasonFactor }} − {{ p.stockQty }} − {{ p.onwayQty }}
              ≈ <b>{{ p.suggestQty }}</b> 册
            </span>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">最近库存流水 <span class="tag">用于核对可售库存来源</span></h3>
          <el-table :data="p.stockFlows || []" size="small">
            <el-table-column label="时间" width="140">
              <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column prop="changeType" label="类型" width="92" />
            <el-table-column label="变动" width="76" align="right">
              <template #default="{ row }">
                <span :style="{ color: row.changeQty > 0 ? '#2B7955' : '#B02A37' }">
                  {{ row.changeQty > 0 ? '+' : '' }}{{ row.changeQty }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="afterQty" label="结存" width="70" align="right" />
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
          </el-table>
        </div>
      </div>

      <div class="col side">
        <div class="card">
          <h3 class="card-title">采纳处理</h3>
          <div class="sug">
            <span>系统建议</span>
            <b>{{ p.suggestQty }} <em>册</em></b>
          </div>
          <el-form label-position="top" style="margin-top: 10px">
            <el-form-item label="实际下单量">
              <el-input-number v-model="form.qty" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
            <el-form-item label="调整理由" :required="Number(form.qty) < Number(p.suggestQty)">
              <el-input v-model="form.note" type="textarea" :rows="3" maxlength="120" show-word-limit
                        placeholder="下调必须说明原因，如：该品类月底有促销活动，先小批试销" />
            </el-form-item>
            <el-alert v-if="warn" :title="warn" type="warning" :closable="false" show-icon style="margin-bottom: 10px" />
            <el-form-item>
              <el-button type="primary" :disabled="!canDo || p.adoptStatus !== '未采纳'" :loading="saving"
                         style="width: 100%" @click="adopt('已采纳')">采纳并生成采购单</el-button>
              <el-button :disabled="!canDo || p.adoptStatus !== '未采纳'" style="width: 100%; margin: 8px 0 0"
                         @click="adopt('已忽略')">忽略本条建议</el-button>
              <p v-if="p.adoptStatus !== '未采纳'" class="muted">
                该建议已{{ p.adoptStatus }}<span v-if="p.manualQty">（人工修正 {{ p.manualQty }} 册）</span>
              </p>
            </el-form-item>
          </el-form>
        </div>

        <div class="card">
          <h3 class="card-title">同一图书历史建议</h3>
          <div v-for="(h, i) in p.history || []" :key="i" class="his">
            <div class="his-top">
              <b>{{ h.planNo }}</b>
              <span class="st st-gray">{{ h.ruleVersion }}</span>
            </div>
            <div class="his-sub">
              近30天销量 {{ h.sales30d }} · 分数 {{ h.totalScore }} · {{ h.salesLevel }}
            </div>
            <div class="his-time">{{ fmtTime(h.createTime) }}</div>
          </div>
          <p v-if="!(p.history || []).length" class="empty">暂无历史记录</p>
        </div>

        <div class="card">
          <h3 class="card-title">当前规则</h3>
          <ul class="rules">
            <li>销量 {{ w.wSales }}% · 周转 {{ w.wTurnover }}% · 在途 {{ w.wOnway }}%</li>
            <li>季节 {{ w.wSeason }}% · 缺货 {{ w.wStockout }}%</li>
            <li>畅销线 ≥ {{ w.hotLine }}，平销线 ≥ {{ w.normalLine }}</li>
            <li>安全天数：畅销 {{ w.safetyHot }} 天 / 平销 {{ w.safetyNormal }} 天 / 滞销 0 天</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime } from '@/utils/format'

const route = useRoute()
const p = ref({})
const loading = ref(true)
const saving = ref(false)
const form = reactive({ qty: 0, note: '' })

const canDo = computed(() => ['ADMIN', 'OPS', 'BUYER'].includes(store.user.roleCode))
const w = computed(() => p.value.weights || {})
const lvCls = (l) => ({ 畅销: 'st-red', 平销: 'st-blue', 滞销: 'st-amber', 断销: 'st-gray' }[l] || 'st-gray')
const safety = computed(() => (p.value.salesLevel === '畅销' ? w.value.safetyHot ?? 15 : p.value.salesLevel === '平销' ? w.value.safetyNormal ?? 7 : 0))

/** 五因子：把后端的归一化得分与权重还原成可读的加权明细 */
const factors = computed(() => {
  const v = p.value
  const ww = w.value
  const pct = (s, wt) => Math.round((Number(s || 0) * Number(wt || 0)) / 100 * 10) / 10
  return [
    { name: '近30天销量', raw: `${v.sales30d} 件`, score: v.sSales, weight: ww.wSales, contribution: pct(v.sSales, ww.wSales), color: '#3A5A8C' },
    { name: '库存周转', raw: `周转 ${v.turnoverDays} 天`, score: v.sTurnover, weight: ww.wTurnover, contribution: pct(v.sTurnover, ww.wTurnover), color: '#6B8CBE' },
    { name: '在途数量', raw: `${v.onwayQty} 件在途`, score: v.sOnway, weight: ww.wOnway, contribution: pct(v.sOnway, ww.wOnway), color: '#C9A227' },
    { name: '季节系数', raw: `系数 ${v.seasonFactor}`, score: v.sSeason, weight: ww.wSeason, contribution: pct(v.sSeason, ww.wSeason), color: '#2B7955' },
    { name: '近90天缺货', raw: `缺货计分 ${v.sStockout}`, score: v.sStockout, weight: ww.wStockout, contribution: pct(v.sStockout, ww.wStockout), color: '#8A6FA8' }
  ]
})

function summary({ columns }) {
  return columns.map((c, i) => {
    if (i === 0) return '合计'
    if (i === 4) return String(p.value.totalScore ?? '')
    return ''
  })
}

const warn = computed(() => {
  const sug = Number(p.value.suggestQty || 0)
  const q = Number(form.qty || 0)
  const cap = Math.ceil(sug * 1.2)
  if (q > cap) return `人工上浮不得超过建议量的 20%（最多 ${cap} 册）`
  if (q < sug && (!form.note || form.note.trim().length < 5)) return '低于建议量必须填写不少于 5 个字的理由'
  return ''
})

async function load() {
  loading.value = true
  try {
    p.value = await api.replenishDetail(route.params.id)
    form.qty = p.value.manualQty ?? p.value.suggestQty
    form.note = p.value.manualNote || ''
  } finally {
    loading.value = false
  }
}
async function adopt(status) {
  if (status === '已采纳') {
    const sug = Number(p.value.suggestQty || 0)
    if (Number(form.qty) > Math.ceil(sug * 1.2)) {
      ElMessage.warning(`人工上浮不得超过 ${Math.ceil(sug * 1.2)} 册`)
      return
    }
    if (Number(form.qty) < sug && (!form.note || form.note.trim().length < 5)) {
      ElMessage.warning('下调建议量必须填写不少于 5 个字的理由')
      return
    }
    await ElMessageBox.confirm(`确认按 ${form.qty} 册生成采购需求？`, '采纳建议', { type: 'info' })
  }
  saving.value = true
  try {
    await api.replenishAdopt(p.value.id, { adoptStatus: status, qty: form.qty, note: form.note })
    ElMessage.success(status === '已采纳' ? '已采纳' : '已忽略')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.dt-wrap { display: grid; grid-template-columns: 1fr 306px; gap: 12px; align-items: start; }
.col { display: flex; flex-direction: column; gap: 12px; }
.book { display: flex; gap: 18px; }
.book-txt { flex: 1; min-width: 0; }
.book-txt h3 { margin: 0 0 4px; font-size: 16px; }
.sub { color: var(--text-3); font-size: 12px; margin: 0; }
.nums { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 12px; }
.nums div { background: var(--paper); border-radius: 4px; padding: 7px 10px; }
.nums span { display: block; color: var(--text-3); font-size: 11px; }
.nums b { font-size: 16px; color: var(--primary); }
.desc { background: #F2F6FC; border-left: 3px solid var(--primary); padding: 9px 12px; border-radius: 3px;
  color: var(--text-2); font-size: 12.5px; line-height: 1.85; margin: 14px 0 0; }
.formula { background: var(--paper); border: 1px dashed var(--line); border-radius: 4px; padding: 10px 12px;
  font-size: 12px; color: var(--text-2); line-height: 1.9; margin-top: 10px; }
.formula .calc { display: block; color: var(--primary); font-family: ui-monospace, Menlo, monospace; }
.formula .calc b { font-size: 15px; }
.sug { background: var(--paper); border-radius: 5px; padding: 10px 12px; display: flex; justify-content: space-between; align-items: baseline; }
.sug span { color: var(--text-3); font-size: 12px; }
.sug b { font-size: 24px; color: var(--danger); }
.sug em { font-style: normal; font-size: 12px; color: var(--text-3); }
.his { border-top: 1px dashed var(--line); padding: 8px 0; }
.his:first-of-type { border-top: none; }
.his-top { display: flex; justify-content: space-between; align-items: center; font-size: 12px; }
.his-sub { color: var(--text-2); font-size: 11.5px; line-height: 1.7; }
.his-time { color: var(--text-3); font-size: 11px; }
.rules { margin: 0; padding-left: 16px; color: var(--text-2); font-size: 11.5px; line-height: 2; }
.empty { color: var(--text-3); font-size: 12px; text-align: center; padding: 18px 0; }
.muted { color: var(--text-3); font-size: 11.5px; margin: 8px 0 0; line-height: 1.7; }
@media (max-width: 1100px) { .dt-wrap { grid-template-columns: 1fr; } }
</style>
