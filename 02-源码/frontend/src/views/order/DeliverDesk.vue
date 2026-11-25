<template>
  <div>
    <div class="page-head">
      <h2>发货工作台</h2>
      <span class="sub">待发货 {{ rows.length }} 单 · 按下单时间升序，越早越先处理</span>
      <span class="spacer"></span>
      <el-button size="small" @click="load">刷新队列</el-button>
    </div>

    <div class="desk">
      <div class="card">
        <h3 class="card-title">待发货队列</h3>
        <div v-for="r in rows" :key="r.id" class="d-item" :class="{ on: cur.id === r.id }" @click="pick(r)">
          <div class="d-top">
            <b>{{ r.orderNo }}</b>
            <span class="st st-amber">待发货</span>
          </div>
          <div class="d-book">
            <img class="cover" :src="r.coverImage" alt="" />
            <div class="d-txt">
              <div class="d-name">{{ r.bookName }}</div>
              <div class="d-sub">{{ r.receiverName }} · {{ r.receiverPhone }}</div>
              <div class="d-sub">{{ r.addressDesc }}</div>
            </div>
          </div>
          <div class="d-foot">
            <span>{{ r.itemCount }} 件 · ¥{{ fmt(r.payAmount) }}</span>
            <span class="d-time">{{ gap(r.payTime) }}</span>
          </div>
        </div>
        <p v-if="!rows.length" class="empty">暂无待发货订单</p>
      </div>

      <div class="card">
        <h3 class="card-title">发货登记 <span class="tag">{{ cur.orderNo || '请选择左侧订单' }}</span></h3>
        <template v-if="cur.id">
          <div class="d-detail">
            <img class="cover-lg" :src="cur.coverImage" alt="" />
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="图书">{{ cur.bookName }}</el-descriptions-item>
              <el-descriptions-item label="件数">{{ cur.itemCount }} 件</el-descriptions-item>
              <el-descriptions-item label="实付">¥{{ fmt(cur.payAmount) }}</el-descriptions-item>
              <el-descriptions-item label="下单时间">{{ fmtTime(cur.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="支付时间">{{ fmtTime(cur.payTime) }}</el-descriptions-item>
              <el-descriptions-item label="收件信息">
                {{ cur.receiverName }} {{ cur.receiverPhone }}<br />{{ cur.addressDesc }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <el-alert v-if="cur.stockQty < cur.itemCount" type="error" :closable="false" show-icon
                    :title="`当前库存 ${cur.stockQty} 件，不足本单 ${cur.itemCount} 件，请先补货或登记缺货原因`"
                    style="margin: 12px 0" />

          <el-form label-width="92px" style="margin-top: 6px">
            <el-form-item label="快递公司">
              <el-select v-model="form.company" placeholder="请选择" style="width: 100%" @change="genNo">
                <el-option v-for="c in opts.express" :key="c.name" :label="c.name" :value="c.name" />
              </el-select>
            </el-form-item>
            <el-form-item label="快递单号">
              <el-input v-model="form.expressNo" placeholder="扫描或手工输入">
                <template #append><el-button @click="genNo">生成</el-button></template>
              </el-input>
            </el-form-item>
            <el-form-item v-if="cur.stockQty < cur.itemCount" label="缺货说明">
              <el-input v-model="form.shortageReason" type="textarea" :rows="2"
                        placeholder="说明缺货件数与补发计划，不少于 5 个字" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="!canDo" :loading="saving" @click="submit">确认发货</el-button>
              <el-button @click="pick(cur)">重置</el-button>
              <span v-if="!canDo" class="muted">当前角色无发货权限</span>
            </el-form-item>
          </el-form>

          <el-alert type="info" :closable="false" show-icon
                    title="发货后系统记录首个物流节点；48 小时内无新节点将进入异常列表" />
        </template>
        <p v-else class="empty">请选择左侧订单</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime, fmtNum, fmtGap } from '@/utils/format'

const rows = ref([])
const cur = ref({})
const opts = ref({})
const saving = ref(false)
const form = reactive({ company: '', expressNo: '', shortageReason: '' })

const canDo = computed(() => ['ADMIN', 'CLERK'].includes(store.user.roleCode))
const fmt = (v) => fmtNum(v, 2)
const gap = (t) => fmtGap(t, new Date())
const PREFIX = { 顺丰速运: 'SF', 中通快递: 'ZT', 圆通速递: 'YT', 京东物流: 'JD' }

async function load() {
  const r = await api.orderPage({ page: 1, size: 50, status: '待发货' })
  rows.value = r.rows || []
  if (rows.value.length) pick(rows.value[0])
  else cur.value = {}
}
async function pick(r) {
  cur.value = await api.orderDetail(r.id)
  form.company = ''
  form.expressNo = ''
  form.shortageReason = ''
  genNo()
}
function genNo() {
  const p = PREFIX[form.company] || 'SF'
  form.expressNo = p + String(Date.now()).slice(-12)
}
async function submit() {
  if (!form.company) {
    ElMessage.warning('请选择快递公司')
    return
  }
  if (!form.expressNo || form.expressNo.length < 8) {
    ElMessage.warning('快递单号至少 8 位')
    return
  }
  if (cur.value.stockQty < cur.value.itemCount && (!form.shortageReason || form.shortageReason.trim().length < 5)) {
    ElMessage.warning('库存不足时必须填写不少于 5 个字的缺货说明')
    return
  }
  saving.value = true
  try {
    await api.orderDeliver(cur.value.id, { ...form })
    ElMessage.success('发货成功，物流节点已记录')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  opts.value = await api.options()
  load()
})
</script>

<style scoped>
.desk { display: grid; grid-template-columns: 400px 1fr; gap: 12px; align-items: start; }
.d-item { border: 1px solid var(--line); border-radius: 5px; padding: 9px 10px; margin-bottom: 8px; cursor: pointer; }
.d-item:hover { border-color: var(--primary-light); }
.d-item.on { border-color: var(--primary); background: #F4F8FD; }
.d-top { display: flex; justify-content: space-between; align-items: center; font-size: 12px; margin-bottom: 7px; }
.d-book { display: flex; gap: 9px; }
.d-txt { min-width: 0; flex: 1; }
.d-name { font-size: 12.5px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.d-sub { color: var(--text-3); font-size: 11px; line-height: 1.6; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.d-foot { display: flex; justify-content: space-between; margin-top: 7px; font-size: 11.5px; color: var(--text-2); }
.d-time { color: var(--warn); }
.d-detail { display: flex; gap: 16px; }
.empty { color: var(--text-3); font-size: 12px; text-align: center; padding: 30px 0; }
.muted { color: var(--text-3); font-size: 11.5px; margin-left: 8px; }
@media (max-width: 1100px) { .desk { grid-template-columns: 1fr; } }
</style>
