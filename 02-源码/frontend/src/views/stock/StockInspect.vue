<template>
  <div>
    <div class="page-head">
      <h2>入库质检</h2>
      <span class="sub">合格 → 写库存与流水；破损/缺页/错版 → 整批退回，库存不增加</span>
      <span class="spacer"></span>
      <el-radio-group v-model="q.status" size="small" @change="search">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="待质检">待质检</el-radio-button>
        <el-radio-button label="已入库">已入库</el-radio-button>
        <el-radio-button label="已退回">已退回</el-radio-button>
      </el-radio-group>
    </div>

    <div class="qc-wrap">
      <div class="card list-card">
        <h3 class="card-title">待处理批次 <span class="tag">共 {{ total }} 条</span></h3>
        <div v-for="r in rows" :key="r.id" class="qc-item" :class="{ on: cur.id === r.id }" @click="pick(r)">
          <img class="cover" :src="r.coverImage" alt="" />
          <div class="qc-txt">
            <div class="qc-name">{{ r.bookName }}</div>
            <div class="qc-sub">{{ r.stockInNo }} · {{ r.supplierName }}</div>
            <div class="qc-sub">入库 {{ r.inQty }} 册 · 采购价 ¥{{ fmt(r.purchasePrice) }}</div>
          </div>
          <div class="qc-right">
            <span class="st" :class="stCls(r.status)">{{ r.status }}</span>
            <span class="qc-time">{{ fmtTime(r.createTime) }}</span>
          </div>
        </div>
        <p v-if="!rows.length" class="empty">暂无批次</p>
        <div class="pager">
          <el-pagination small background layout="prev, pager, next" :total="total"
                         v-model:current-page="q.page" :page-size="q.size" @current-change="load" />
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">质检单 <span class="tag">{{ cur.stockInNo || '请选择左侧批次' }}</span></h3>
        <template v-if="cur.id">
          <div class="dt-top">
            <img class="cover-lg" :src="cur.coverImage" alt="" />
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="图书">{{ cur.bookName }}</el-descriptions-item>
              <el-descriptions-item label="入库单号">{{ cur.stockInNo }}</el-descriptions-item>
              <el-descriptions-item label="供货书商">{{ cur.supplierName }}</el-descriptions-item>
              <el-descriptions-item label="入库数量">{{ cur.inQty }} 册</el-descriptions-item>
              <el-descriptions-item label="采购价 / 金额">¥{{ fmt(cur.purchasePrice) }} / ¥{{ fmt(cur.inQty * cur.purchasePrice) }}</el-descriptions-item>
              <el-descriptions-item label="当前状态">
                <span class="st" :class="stCls(cur.status)">{{ cur.status }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <template v-if="cur.status === '待质检' && canDo">
            <h4 class="card-title" style="margin-top: 18px">质检结论</h4>
            <el-form label-width="92px">
              <el-form-item label="质检结果">
                <el-radio-group v-model="form.qualityResult">
                  <el-radio-button label="合格" />
                  <el-radio-button label="破损" />
                  <el-radio-button label="缺页" />
                  <el-radio-button label="错版" />
                </el-radio-group>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="150" show-word-limit
                          :placeholder="form.qualityResult === '合格' ? '选填，如：外包装完好' : '必填，说明具体问题与处理方式'" />
              </el-form-item>
              <el-form-item>
                <el-alert :type="form.qualityResult === '合格' ? 'success' : 'warning'" :closable="false" show-icon
                          :title="form.qualityResult === '合格'
                            ? `提交后库存 +${cur.inQty}，并生成一条「入库」类型的库存流水`
                            : '提交后整批退回供应商，库存不变，也不生成库存流水'" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" @click="submit">提交质检结果</el-button>
                <el-button @click="pick(cur)">重置</el-button>
              </el-form-item>
            </el-form>
          </template>
          <el-result v-else-if="cur.status !== '待质检'" icon="info"
                     :title="`该批次已${cur.status}`"
                     :sub-title="`质检结果：${cur.qualityResult || '—'} · 处理时间 ${fmtTime(cur.updateTime)}`" />
          <el-alert v-else type="warning" :closable="false" show-icon style="margin-top: 16px"
                    title="当前角色无质检权限，仅库管员与系统管理员可提交质检结论" />
        </template>
        <p v-else class="empty">请选择左侧批次查看详情</p>
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

const rows = ref([])
const total = ref(0)
const cur = ref({})
const saving = ref(false)
const q = reactive({ page: 1, size: 10, status: '待质检' })
const form = reactive({ qualityResult: '合格', remark: '' })

const canDo = computed(() => ['ADMIN', 'KEEPER'].includes(store.user.roleCode))
const fmt = (v) => fmtNum(v, 2)
const stCls = (s) => ({ 待质检: 'st-amber', 已入库: 'st-green', 已退回: 'st-red' }[s] || 'st-gray')

async function load() {
  const r = await api.stockInPage({ ...q, status: q.status || undefined })
  rows.value = r.rows || []
  total.value = r.total || 0
  if (rows.value.length && !rows.value.some((x) => x.id === cur.value.id)) pick(rows.value[0])
}
function search() {
  q.page = 1
  load()
}
function pick(r) {
  cur.value = r
  form.qualityResult = '合格'
  form.remark = ''
}
async function submit() {
  if (form.qualityResult !== '合格' && (!form.remark || form.remark.trim().length < 4)) {
    ElMessage.warning('不合格需填写至少 4 个字的备注，说明问题与处理方式')
    return
  }
  saving.value = true
  try {
    const r = await api.stockInInspect(cur.value.id, { ...form })
    if (r && r.firstResult) ElMessage.info('该批次已处理过，显示首次结果')
    else ElMessage.success(form.qualityResult === '合格' ? '质检合格，库存已增加' : '已整批退回，库存未变动')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.qc-wrap { display: grid; grid-template-columns: 372px 1fr; gap: 12px; align-items: start; }
.qc-item { display: flex; gap: 10px; padding: 9px 8px; border-radius: 4px; cursor: pointer; border: 1px solid transparent; }
.qc-item:hover { background: var(--paper); }
.qc-item.on { background: #F2F6FC; border-color: #CFDCEC; }
.qc-txt { flex: 1; min-width: 0; }
.qc-name { font-size: 12.5px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.qc-sub { color: var(--text-3); font-size: 11px; line-height: 1.6; }
.qc-right { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
.qc-time { color: var(--text-3); font-size: 10.5px; }
.dt-top { display: flex; gap: 18px; }
.empty { color: var(--text-3); font-size: 12px; text-align: center; padding: 26px 0; }
@media (max-width: 1100px) { .qc-wrap { grid-template-columns: 1fr; } }
</style>
