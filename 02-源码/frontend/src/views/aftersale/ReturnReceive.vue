<template>
  <div>
    <div class="page-head">
      <h2>退换验收</h2>
      <span class="sub">质检完好 → 回补可售库存；缺页污损 → 按折扣退款后入库；非本店图书 → 不入库</span>
      <span class="spacer"></span>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <div class="wrap">
      <div class="card">
        <h3 class="card-title">待验收单据 <span class="tag">待寄回 / 已寄回</span></h3>
        <div v-for="r in queue" :key="r.id" class="v-item" :class="{ on: cur.id === r.id }" @click="pick(r)">
          <div class="v-top">
            <b>{{ r.returnNo }}</b>
            <span class="st" :class="r.status === '已寄回' ? 'st-blue' : 'st-amber'">{{ r.status }}</span>
          </div>
          <div class="v-book">
            <img class="cover" :src="r.coverImage" alt="" />
            <div>
              <div class="v-name">{{ r.bookName }}</div>
              <div class="v-sub">{{ r.returnType }} · {{ r.applyQty }} 件 · ¥{{ fmt(r.refundAmount) }}</div>
              <div class="v-sub">{{ r.customerName }} · {{ fmtTime(r.createTime) }}</div>
            </div>
          </div>
        </div>
        <p v-if="!queue.length" class="empty">没有待验收的退换单</p>
      </div>

      <div class="card">
        <h3 class="card-title">验收登记 <span class="tag">{{ cur.returnNo || '请选择左侧单据' }}</span></h3>
        <template v-if="cur.id">
          <div class="dt">
            <img class="cover-lg" :src="cur.coverImage" alt="" />
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="图书">{{ cur.bookName }}</el-descriptions-item>
              <el-descriptions-item label="退换类型">{{ cur.returnType }}</el-descriptions-item>
              <el-descriptions-item label="申请件数">{{ cur.applyQty }} 件</el-descriptions-item>
              <el-descriptions-item label="申请退款">¥{{ fmt(cur.refundAmount) }}</el-descriptions-item>
              <el-descriptions-item label="退回快递">{{ cur.backExpressNo || '读者尚未填写' }}</el-descriptions-item>
              <el-descriptions-item label="审核意见">{{ cur.auditNote || '—' }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <el-form label-width="92px" style="margin-top: 14px">
            <el-form-item label="质检结果">
              <el-radio-group v-model="form.inspectResult">
                <el-radio-button label="完好" />
                <el-radio-button label="缺页污损" />
                <el-radio-button label="非本店图书" />
              </el-radio-group>
            </el-form-item>
            <el-form-item label="退款金额">
              <el-input-number v-model="form.refundAmount" :min="0" :precision="2" :step="1" style="width: 200px" />
              <span class="muted">不得超过订单实付金额</span>
            </el-form-item>
            <el-form-item label="验收备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="150" show-word-limit
                        placeholder="记录外观、塑封、随书附件是否齐全" />
            </el-form-item>
            <el-form-item>
              <el-alert :type="form.inspectResult === '非本店图书' ? 'error' : 'info'" :closable="false" show-icon
                        :title="form.inspectResult === '非本店图书'
                          ? '非本店图书不入库，仅退款，避免污染库存'
                          : `验收后库存 +${cur.applyQty}（退货入库），并生成一条库存流水`" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="!canDo" :loading="saving" @click="submit">确认验收并退款</el-button>
              <span v-if="!canDo" class="muted">当前角色无验收权限</span>
            </el-form-item>
          </el-form>
        </template>
        <p v-else class="empty">请选择左侧退换单</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime, fmtNum } from '@/utils/format'

const queue = ref([])
const cur = ref({})
const saving = ref(false)
const form = reactive({ inspectResult: '完好', refundAmount: 0, remark: '' })

const canDo = computed(() => ['ADMIN', 'KEEPER'].includes(store.user.roleCode))
const fmt = (v) => fmtNum(v, 2)

async function load() {
  const a = await api.returnPage({ page: 1, size: 50, status: '已寄回' })
  const b = await api.returnPage({ page: 1, size: 50, status: '待寄回' })
  queue.value = [...(a.rows || []), ...(b.rows || [])]
  if (queue.value.length) pick(queue.value[0])
  else cur.value = {}
}
function pick(r) {
  cur.value = r
  form.inspectResult = '完好'
  form.refundAmount = Number(r.refundAmount || 0)
  form.remark = ''
}
async function submit() {
  saving.value = true
  try {
    await api.returnReceive(cur.value.id, { ...form })
    ElMessage.success('验收完成，已退款并回补库存')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.wrap { display: grid; grid-template-columns: 372px 1fr; gap: 12px; align-items: start; }
.v-item { border: 1px solid var(--line); border-radius: 5px; padding: 9px 10px; margin-bottom: 8px; cursor: pointer; }
.v-item:hover { border-color: var(--primary-light); }
.v-item.on { border-color: var(--primary); background: #F4F8FD; }
.v-top { display: flex; justify-content: space-between; align-items: center; font-size: 12px; margin-bottom: 7px; }
.v-book { display: flex; gap: 9px; }
.v-name { font-size: 12.5px; font-weight: 500; }
.v-sub { color: var(--text-3); font-size: 11px; line-height: 1.6; }
.dt { display: flex; gap: 16px; }
.empty { color: var(--text-3); font-size: 12px; text-align: center; padding: 30px 0; }
.muted { color: var(--text-3); font-size: 11.5px; margin-left: 8px; }
@media (max-width: 1100px) { .wrap { grid-template-columns: 1fr; } }
</style>
