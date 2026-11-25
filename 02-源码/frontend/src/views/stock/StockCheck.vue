<template>
  <div>
    <div class="page-head">
      <h2>库存盘点</h2>
      <span class="sub">盘盈盘亏必须填写原因，差异会生成一条库存流水并同步图书库存</span>
      <span class="spacer"></span>
      <el-button size="small" @click="loadRecent">刷新记录</el-button>
    </div>

    <div class="chk-wrap">
      <div class="card">
        <h3 class="card-title">录入盘点结果</h3>
        <el-form ref="f" :model="form" :rules="rules" label-width="96px">
          <el-form-item label="图书" prop="bookId">
            <el-select v-model="form.bookId" filterable placeholder="搜索书名 / 编号" style="width: 100%" @change="onBook">
              <el-option v-for="b in opts.books" :key="b.id" :label="`${b.bookName}（${b.bookNo}）`" :value="b.id">
                <span>{{ b.bookName }}</span>
                <span class="opt-side">账面 {{ b.stockQty }}</span>
              </el-option>
            </el-select>
          </el-form-item>

          <template v-if="cur.id">
            <div class="chk-book">
              <img class="cover" :src="cur.coverImage" alt="" />
              <div>
                <div class="chk-name">{{ cur.bookName }}</div>
                <div class="chk-sub">{{ cur.author }} · {{ cur.publisher }}</div>
                <div class="chk-sub">{{ cur.categoryName || '' }}</div>
              </div>
            </div>

            <div class="chk-grid">
              <div class="chk-cell">
                <span>账面库存</span>
                <b>{{ cur.stockQty }}</b>
              </div>
              <div class="chk-cell">
                <span>锁定数量</span>
                <b>{{ cur.lockedQty || 0 }}</b>
              </div>
              <div class="chk-cell">
                <span>实盘数量</span>
                <b style="color: var(--primary)">{{ form.actualQty }}</b>
              </div>
              <div class="chk-cell" :class="diffCls">
                <span>差异</span>
                <b>{{ diff > 0 ? '+' : '' }}{{ diff }}</b>
              </div>
            </div>

            <el-form-item label="差异原因" :required="diff !== 0">
              <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="150" show-word-limit
                        placeholder="必填，不少于 5 个字。如：盘点发现运输破损 2 册、上架时漏登记 1 册" />
            </el-form-item>
            <el-form-item>
              <el-alert :type="diff === 0 ? 'success' : diff > 0 ? 'info' : 'warning'" :closable="false" show-icon
                        :title="diff === 0
                          ? '账实一致，无需提交：系统只接受存在差异的盘点结果'
                          : `提交后库存变为 ${form.actualQty}，并生成一条「${diff > 0 ? '盘盈' : '盘亏'}」流水（变动 ${diff > 0 ? '+' : ''}${diff}）`" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" :disabled="!canDo || diff === 0" @click="submit">
                提交盘点
              </el-button>
              <el-button @click="reset">重置</el-button>
              <span v-if="!canDo" class="muted">当前角色无盘点权限</span>
            </el-form-item>
          </template>
          <p v-else class="empty">请选择要盘点的图书</p>
        </el-form>
      </div>

      <div class="card">
        <h3 class="card-title">最近盘点记录 <span class="tag">来源：库存流水（盘盈 / 盘亏）</span></h3>
        <el-table :data="recent" size="small" v-loading="loading">
          <el-table-column label="时间" width="132">
            <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column prop="bookName" label="图书" min-width="150" show-overflow-tooltip />
          <el-table-column prop="changeType" label="类型" width="72">
            <template #default="{ row }">
              <span class="st" :class="row.changeQty > 0 ? 'st-green' : 'st-red'">{{ row.changeType }}</span>
            </template>
          </el-table-column>
          <el-table-column label="变动" width="70" align="right">
            <template #default="{ row }">
              <span :style="{ color: row.changeQty > 0 ? '#2B7955' : '#B02A37', fontWeight: 600 }">
                {{ row.changeQty > 0 ? '+' : '' }}{{ row.changeQty }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="afterQty" label="结存" width="66" align="right" />
          <el-table-column prop="remark" label="原因" min-width="170" show-overflow-tooltip />
          <el-table-column label="操作人" width="90" prop="operatorName" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime } from '@/utils/format'

const f = ref()
const opts = ref({})
const cur = ref({})
const recent = ref([])
const loading = ref(false)
const saving = ref(false)
const form = reactive({ bookId: null, actualQty: 0, reason: '' })
const rules = { bookId: [{ required: true, message: '请选择图书', trigger: 'change' }] }
const canDo = computed(() => ['ADMIN', 'KEEPER'].includes(store.user.roleCode))

const diff = computed(() => Number(form.actualQty || 0) - Number(cur.value.stockQty || 0))
const diffCls = computed(() => (diff.value === 0 ? 'ok' : diff.value > 0 ? 'plus' : 'minus'))

async function onBook(id) {
  cur.value = await api.bookDetail(id)
  form.actualQty = cur.value.stockQty
  form.reason = ''
}
function reset() {
  form.bookId = null
  cur.value = {}
  form.actualQty = 0
  form.reason = ''
}
async function loadRecent() {
  loading.value = true
  try {
    const r = await api.flowPage({ page: 1, size: 10, changeType: '' })
    recent.value = (r.rows || []).filter((x) => ['盘盈', '盘亏'].includes(x.changeType))
    if (recent.value.length < 6) {
      const all = await api.flowPage({ page: 1, size: 40 })
      recent.value = (all.rows || []).filter((x) => ['盘盈', '盘亏'].includes(x.changeType)).slice(0, 10)
    }
  } finally {
    loading.value = false
  }
}
async function submit() {
  if (diff.value === 0) {
    ElMessage.info('账实一致，无需提交')
    return
  }
  if (!form.reason || form.reason.trim().length < 5) {
    ElMessage.warning('盘盈盘亏必须填写不少于 5 个字的原因')
    return
  }
  saving.value = true
  try {
    const r = await api.stockCheck({ bookId: form.bookId, actualQty: Number(form.actualQty), reason: form.reason })
    ElMessage.success(`盘点完成：${r.beforeQty} → ${r.afterQty}（${r.diff > 0 ? '盘盈' : '盘亏'} ${Math.abs(r.diff)} 册）`)
    await onBook(form.bookId)
    opts.value = await api.options()
    loadRecent()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  opts.value = await api.options()
  loadRecent()
})
</script>

<style scoped>
.chk-wrap { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; align-items: start; }
.chk-book { display: flex; gap: 12px; padding: 10px 0 14px; }
.chk-name { font-size: 14px; font-weight: 600; }
.chk-sub { color: var(--text-3); font-size: 11.5px; line-height: 1.7; }
.chk-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 14px; }
.chk-cell { background: var(--paper); border-radius: 4px; padding: 8px 10px; border: 1px solid var(--line); }
.chk-cell span { display: block; color: var(--text-3); font-size: 11px; }
.chk-cell b { font-size: 19px; }
.chk-cell.plus { background: #EDF6F1; border-color: #CFE7DA; }
.chk-cell.plus b { color: var(--success); }
.chk-cell.minus { background: #FBECEE; border-color: #F2D2D6; }
.chk-cell.minus b { color: var(--danger); }
.chk-cell.ok b { color: var(--success); }
.opt-side { float: right; color: var(--text-3); font-size: 11px; }
.empty { color: var(--text-3); font-size: 12px; text-align: center; padding: 30px 0; }
.muted { color: var(--text-3); font-size: 11.5px; margin-left: 8px; }
@media (max-width: 1100px) { .chk-wrap { grid-template-columns: 1fr; } }
</style>
