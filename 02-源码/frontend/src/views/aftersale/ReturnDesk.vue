<template>
  <div>
    <div class="page-head">
      <h2>售后处理台</h2>
      <span class="sub">审核通过 → 待寄回（订单冻结为「退换中」）；驳回需写明依据并通知读者</span>
      <span class="spacer"></span>
      <el-tag type="warning" effect="plain" size="small">待审核 {{ queue.length }} 单</el-tag>
    </div>

    <div class="desk">
      <div class="card">
        <h3 class="card-title">待审核队列</h3>
        <div v-for="r in queue" :key="r.id" class="q-item" :class="{ on: cur.id === r.id }" @click="pick(r)">
          <div class="q-top">
            <b>{{ r.returnNo }}</b>
            <span class="st st-amber">待审核</span>
          </div>
          <div class="q-book">
            <img class="cover" :src="r.coverImage" alt="" />
            <div class="q-txt">
              <div class="q-name">{{ r.bookName }}</div>
              <div class="q-sub">{{ r.returnType }} · {{ r.reasonType }} · {{ r.applyQty }} 件</div>
              <div class="q-sub">退款 ¥{{ fmt(r.refundAmount) }}</div>
            </div>
          </div>
          <div class="q-foot">{{ r.customerName }} · {{ fmtTime(r.createTime) }}</div>
        </div>
        <p v-if="!queue.length" class="empty">没有待审核的退换单</p>
      </div>

      <div class="card">
        <h3 class="card-title">审核处理 <span class="tag">{{ cur.returnNo || '请选择左侧单据' }}</span></h3>
        <template v-if="cur.id">
          <div class="dt">
            <img class="cover-lg" :src="cur.coverImage" alt="" />
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="图书">{{ cur.bookName }}</el-descriptions-item>
              <el-descriptions-item label="订单号">{{ cur.orderNo }}</el-descriptions-item>
              <el-descriptions-item label="读者">{{ cur.customerName }}</el-descriptions-item>
              <el-descriptions-item label="退换类型">{{ cur.returnType }}</el-descriptions-item>
              <el-descriptions-item label="申请件数">{{ cur.applyQty }} 件</el-descriptions-item>
              <el-descriptions-item label="申请金额">¥{{ fmt(cur.refundAmount) }}</el-descriptions-item>
              <el-descriptions-item label="签收时间">{{ fmtTime(cur.signTime) }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="reason">
            <div class="rz-head">申请原因 · {{ cur.reasonType }}</div>
            <p>{{ cur.description }}</p>
            <el-tag :type="within7 ? 'success' : 'danger'" effect="plain" size="small">
              {{ within7 ? '在 7 天无理由期内' : '已超过 7 天，仅质量问题可受理' }}
            </el-tag>
          </div>

          <el-form label-width="92px" style="margin-top: 12px">
            <el-form-item label="退款金额">
              <el-input-number v-model="form.refundAmount" :min="0" :max="Number(cur.refundAmount)"
                               :precision="2" :step="1" style="width: 200px" />
              <span class="muted">不得超过申请金额 ¥{{ fmt(cur.refundAmount) }}</span>
            </el-form-item>
            <el-form-item label="审核意见">
              <el-input v-model="form.note" type="textarea" :rows="3" maxlength="150" show-word-limit
                        placeholder="通过可填「符合 7 天无理由，同意退回」；驳回必填且不少于 5 个字" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="!canDo" :loading="saving" @click="audit('待寄回')">
                通过，等待寄回
              </el-button>
              <el-button type="danger" plain :disabled="!canDo" :loading="saving" @click="audit('已驳回')">
                驳回申请
              </el-button>
              <span v-if="!canDo" class="muted">当前角色无审核权限</span>
            </el-form-item>
          </el-form>

          <el-alert type="info" :closable="false" show-icon
                    title="通过后系统把订单状态改为「退换中」，关闭评价入口，并给读者发送站内消息" />
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
const form = reactive({ refundAmount: 0, note: '' })

const canDo = computed(() => ['ADMIN', 'CLERK'].includes(store.user.roleCode))
const fmt = (v) => fmtNum(v, 2)
const within7 = computed(() => {
  if (!cur.value.signTime) return false
  return (Date.now() - new Date(String(cur.value.signTime).replace(/-/g, '/')).getTime()) / 86400000 <= 7
})

async function load() {
  const r = await api.returnPage({ page: 1, size: 50, status: '待审核' })
  queue.value = r.rows || []
  if (queue.value.length) pick(queue.value[0])
  else cur.value = {}
}
function pick(r) {
  cur.value = r
  form.refundAmount = Number(r.refundAmount || 0)
  form.note = ''
}
async function audit(target) {
  if (target === '已驳回' && (!form.note || form.note.trim().length < 5)) {
    ElMessage.warning('驳回必须填写不少于 5 个字的说明')
    return
  }
  saving.value = true
  try {
    await api.returnAudit(cur.value.id, { targetStatus: target, note: form.note, refundAmount: form.refundAmount })
    ElMessage.success(target === '待寄回' ? '已通过，等待读者寄回' : '已驳回并通知读者')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.desk { display: grid; grid-template-columns: 380px 1fr; gap: 12px; align-items: start; }
.q-item { border: 1px solid var(--line); border-radius: 5px; padding: 9px 10px; margin-bottom: 8px; cursor: pointer; }
.q-item:hover { border-color: var(--primary-light); }
.q-item.on { border-color: var(--primary); background: #F4F8FD; }
.q-top { display: flex; justify-content: space-between; align-items: center; font-size: 12px; margin-bottom: 7px; }
.q-book { display: flex; gap: 9px; }
.q-txt { min-width: 0; }
.q-name { font-size: 12.5px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.q-sub { color: var(--text-3); font-size: 11px; line-height: 1.6; }
.q-foot { color: var(--text-3); font-size: 11px; margin-top: 7px; }
.dt { display: flex; gap: 16px; }
.reason { background: var(--paper); border-radius: 5px; padding: 10px 12px; margin-top: 14px; }
.rz-head { color: var(--text-3); font-size: 11.5px; margin-bottom: 4px; }
.reason p { margin: 0 0 8px; color: var(--text-2); font-size: 12.5px; line-height: 1.85; }
.muted { color: var(--text-3); font-size: 11.5px; margin-left: 8px; }
.empty { color: var(--text-3); font-size: 12px; text-align: center; padding: 30px 0; }
@media (max-width: 1100px) { .desk { grid-template-columns: 1fr; } }
</style>
