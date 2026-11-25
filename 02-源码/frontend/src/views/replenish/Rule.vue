<template>
  <div>
    <div class="page-head">
      <h2>规则配置</h2>
      <span class="sub">五项权重合计必须为 100%；每次保存生成新版本，历史建议仍按当时的版本留痕</span>
      <span class="spacer"></span>
      <el-button size="small" @click="load">还原</el-button>
      <el-button v-if="canEdit" type="primary" size="small" :loading="saving" @click="save">
        保存为新版本 {{ nextVersion }}
      </el-button>
    </div>

    <div class="rule-wrap">
      <div class="card">
        <h3 class="card-title">因子权重 <span class="tag">拖动或直接输入，合计需等于 100%</span></h3>
        <div class="w-list">
          <div v-for="f in fields" :key="f.key" class="w-item">
            <div class="w-head">
              <span class="w-name">{{ f.label }}</span>
              <span class="w-desc">{{ f.desc }}</span>
              <b class="w-val" :style="{ color: f.color }">{{ form[f.key] }}%</b>
            </div>
            <el-slider v-model="form[f.key]" :min="0" :max="100" :step="1" show-stops :format-tooltip="(v) => v + '%'"
                       :disabled="!canEdit" :styles="{ rail: { background: '#EDF0F3' } }" />
          </div>
        </div>
        <div class="sum" :class="{ bad: !sumOk }">
          权重合计 <b>{{ sum }}%</b>
          <span v-if="!sumOk">（需为 100%，当前 {{ sum > 100 ? '超出' : '不足' }} {{ Math.abs(100 - sum) }}%）</span>
          <span v-else>（校验通过）</span>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">分级与安全天数</h3>
        <el-form label-width="112px">
          <el-form-item label="畅销线（≥）">
            <el-input-number v-model="form.hotLine" :min="50" :max="100" :disabled="!canEdit" />
            <span class="muted">优先级分达到该值判为畅销</span>
          </el-form-item>
          <el-form-item label="平销线（≥）">
            <el-input-number v-model="form.normalLine" :min="20" :max="90" :disabled="!canEdit" />
            <span class="muted">低于平销线且无销量判为滞销 / 断销</span>
          </el-form-item>
          <el-form-item label="畅销安全天数">
            <el-input-number v-model="form.safetyHot" :min="0" :max="45" :disabled="!canEdit" />
            <span class="muted">天</span>
          </el-form-item>
          <el-form-item label="平销安全天数">
            <el-input-number v-model="form.safetyNormal" :min="0" :max="30" :disabled="!canEdit" />
            <span class="muted">天</span>
          </el-form-item>
          <el-form-item label="规则名称">
            <el-input v-model="form.ruleName" :disabled="!canEdit" maxlength="30" />
          </el-form-item>
          <el-form-item label="变更说明">
            <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="120" show-word-limit
                      :disabled="!canEdit" placeholder="说明本次调整的原因，便于回溯" />
          </el-form-item>
        </el-form>
        <el-alert type="info" :closable="false" show-icon
                  title="公式：建议量 = 日均销量 × (30 + 安全天数) × 季节系数 − 可售库存 − 在途" />
      </div>

      <div class="card">
        <h3 class="card-title">版本历史 <span class="tag">共 {{ list.length }} 个版本，最新在用</span></h3>
        <el-table :data="list" size="small">
          <el-table-column prop="ruleVersion" label="版本" width="70">
            <template #default="{ row }">
              <span class="st" :class="row.status === 1 ? 'st-green' : 'st-gray'">{{ row.ruleVersion }}</span>
            </template>
          </el-table-column>
          <el-table-column label="权重（销量/周转/在途/季节/缺货）" min-width="220">
            <template #default="{ row }">
              {{ row.wSales }} / {{ row.wTurnover }} / {{ row.wOnway }} / {{ row.wSeason }} / {{ row.wStockout }}
            </template>
          </el-table-column>
          <el-table-column label="畅销/平销线" width="106">
            <template #default="{ row }">{{ row.hotLine }} / {{ row.normalLine }}</template>
          </el-table-column>
          <el-table-column label="安全天数" width="96">
            <template #default="{ row }">{{ row.safetyHot }} / {{ row.safetyNormal }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="变更说明" min-width="200" show-overflow-tooltip />
          <el-table-column label="生效时间" width="140">
            <template #default="{ row }">{{ fmtTime(row.effectiveTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime } from '@/utils/format'

const list = ref([])
const saving = ref(false)
const canEdit = computed(() => ['ADMIN', 'OPS', 'BUYER'].includes(store.user.roleCode))

const fields = [
  { key: 'wSales', label: '近30天销量', desc: '反映近期真实动销，单店 25 册记满分', color: '#3A5A8C' },
  { key: 'wTurnover', label: '库存周转', desc: '周转天数越短得分越高，超过 90 天记 0', color: '#6B8CBE' },
  { key: 'wOnway', label: '在途数量', desc: '在途越多越不急，避免重复下单', color: '#C9A227' },
  { key: 'wSeason', label: '季节系数', desc: '开学季、考试季等旺季加权', color: '#2B7955' },
  { key: 'wStockout', label: '近90天缺货', desc: '缺货次数越多越该优先补', color: '#8A6FA8' }
]

const form = reactive({
  id: null, ruleName: '默认补货规则', ruleVersion: 'v1',
  wSales: 35, wTurnover: 25, wOnway: 20, wSeason: 10, wStockout: 10,
  hotLine: 80, normalLine: 55, safetyHot: 15, safetyNormal: 7, remark: ''
})

const sum = computed(() => fields.reduce((s, f) => s + Number(form[f.key] || 0), 0))
const sumOk = computed(() => Math.abs(sum.value - 100) < 0.01)
const nextVersion = computed(() => {
  const nums = list.value.map((r) => Number(String(r.ruleVersion).replace(/[^0-9]/g, '')) || 0)
  return 'v' + ((nums.length ? Math.max(...nums) : 0) + 1)
})

async function load() {
  list.value = (await api.ruleList()) || []
  const cur = list.value[0]
  if (cur) {
    Object.assign(form, {
      id: cur.id,
      ruleName: cur.ruleName,
      ruleVersion: cur.ruleVersion,
      wSales: Number(cur.wSales), wTurnover: Number(cur.wTurnover), wOnway: Number(cur.wOnway),
      wSeason: Number(cur.wSeason), wStockout: Number(cur.wStockout),
      hotLine: Number(cur.hotLine), normalLine: Number(cur.normalLine),
      safetyHot: cur.safetyHot, safetyNormal: cur.safetyNormal, remark: ''
    })
  }
}

async function save() {
  if (!sumOk.value) {
    ElMessage.warning(`权重合计必须为 100%，当前 ${sum.value}%`)
    return
  }
  if (form.hotLine <= form.normalLine) {
    ElMessage.warning('畅销线必须高于平销线')
    return
  }
  if (!form.remark || form.remark.trim().length < 4) {
    ElMessage.warning('请填写不少于 4 个字的变更说明')
    return
  }
  await ElMessageBox.confirm(`保存后将生成 ${nextVersion.value} 版本，历史补货建议仍按原版本留痕。确认保存？`, '保存规则', { type: 'info' })
  saving.value = true
  try {
    const r = await api.ruleSave({ ...form })
    ElMessage.success(`已保存 ${r.ruleVersion || nextVersion.value}，可用于下一轮计算`)
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.rule-wrap { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; align-items: start; }
.rule-wrap > .card:nth-child(3) { grid-column: span 2; }
.w-item { margin-bottom: 6px; }
.w-head { display: flex; align-items: baseline; gap: 8px; }
.w-name { font-size: 12.5px; font-weight: 600; }
.w-desc { color: var(--text-3); font-size: 11px; flex: 1; }
.w-val { font-size: 13px; }
.w-item :deep(.el-slider) { --el-slider-height: 4px; margin: 0 6px; }
.sum { margin-top: 12px; padding: 9px 12px; border-radius: 4px; background: #EDF6F1; color: var(--success); font-size: 12.5px; }
.sum b { font-size: 15px; }
.sum.bad { background: #FBECEE; color: var(--danger); }
.muted { color: var(--text-3); font-size: 11.5px; margin-left: 10px; }
@media (max-width: 1100px) { .rule-wrap { grid-template-columns: 1fr; } .rule-wrap > .card:nth-child(3) { grid-column: span 1; } }
</style>
