<template>
  <div>
    <div class="page-head">
      <h2>我的订单</h2>
      <span class="sub">共 {{ total }} 笔 · 待支付订单请在 30 分钟内完成付款</span>
      <span class="spacer"></span>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <el-radio-group v-model="q.status" size="small" style="margin-bottom: 12px" @change="search">
      <el-radio-button label="">全部</el-radio-button>
      <el-radio-button label="待支付">待支付</el-radio-button>
      <el-radio-button label="待发货">待发货</el-radio-button>
      <el-radio-button label="已发货">已发货</el-radio-button>
      <el-radio-button label="已完成">已完成</el-radio-button>
      <el-radio-button label="退换中">退换中</el-radio-button>
      <el-radio-button label="已取消">已取消</el-radio-button>
    </el-radio-group>

    <div v-loading="loading" class="list">
      <div v-for="o in rows" :key="o.id" class="card order">
        <div class="o-head">
          <span class="o-no">{{ o.orderNo }}</span>
          <span class="o-time">{{ fmtTime(o.createTime) }}</span>
          <span class="spacer"></span>
          <span class="st" :class="stCls(o.status)">{{ o.status }}</span>
        </div>
        <div class="o-body">
          <img class="cover" :src="o.coverImage" alt="" />
          <div class="o-txt">
            <div class="o-name">{{ o.bookName }}</div>
            <div class="o-sub">{{ o.author }} · {{ o.publisher }}</div>
            <div class="o-sub">{{ o.itemCount }} 件 · ¥{{ fmt(o.unitPrice) }}/件</div>
          </div>
          <div class="o-amt">
            <span>实付</span>
            <b>¥{{ fmt(o.payAmount) }}</b>
          </div>
          <div class="o-ops">
            <template v-if="o.status === '待支付'">
              <el-button type="primary" size="small" @click="pay(o)">立即支付</el-button>
              <el-button size="small" @click="cancel(o)">取消订单</el-button>
            </template>
            <template v-else-if="o.status === '已发货'">
              <el-button type="primary" size="small" @click="confirm(o)">确认收货</el-button>
            </template>
            <template v-else-if="['已签收', '已完成'].includes(o.status)">
              <el-button size="small" @click="applyReturn(o)">申请退换</el-button>
              <el-button size="small" @click="$router.push(`/portal/book/${o.bookId}`)">再买一次</el-button>
            </template>
            <template v-else-if="o.status === '待发货'">
              <el-button size="small" @click="cancel(o)">取消订单</el-button>
            </template>
            <el-button link type="primary" size="small" @click="detail(o)">订单详情</el-button>
          </div>
        </div>
        <div v-if="o.expressNo" class="o-express">
          物流：{{ o.expressCompany }} {{ o.expressNo }}
        </div>
      </div>
      <p v-if="!rows.length && !loading" class="empty">还没有相关订单</p>
    </div>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     v-model:current-page="q.page" :page-size="q.size" @current-change="load" />
    </div>

    <el-dialog v-model="dlg" title="订单详情" width="560px">
      <template v-if="cur.id">
        <el-timeline>
          <el-timeline-item v-for="(t, i) in cur.timeline || []" :key="i" :timestamp="fmtTime(t.time)">
            {{ t.name }}
          </el-timeline-item>
        </el-timeline>
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="订单号">{{ cur.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="图书">{{ cur.bookName }} × {{ cur.itemCount }}</el-descriptions-item>
          <el-descriptions-item label="金额">
            ¥{{ fmt(cur.payAmount) }}（图书 ¥{{ fmt(cur.bookAmount) }} + 运费 ¥{{ fmt(cur.freight) }} − 优惠 ¥{{ fmt(cur.discountAmount) }}）
          </el-descriptions-item>
          <el-descriptions-item label="收货信息">{{ cur.receiverName }} {{ cur.receiverPhone }} {{ cur.addressDesc }}</el-descriptions-item>
          <el-descriptions-item label="快递">{{ cur.expressCompany || '—' }} {{ cur.expressNo || '' }}</el-descriptions-item>
        </el-descriptions>
        <div v-for="r in cur.returns || []" :key="r.id" class="ret">
          <div class="ret-head">
            <b>{{ r.returnNo }}</b>
            <span class="st" :class="retCls(r.status)">{{ r.status }}</span>
          </div>
          <p>{{ r.reasonType }}｜{{ r.description }}</p>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="retDlg" title="申请退换" width="480px">
      <el-form label-width="86px">
        <el-form-item label="退换类型">
          <el-radio-group v-model="retForm.returnType">
            <el-radio-button label="退货退款" />
            <el-radio-button label="换货" />
            <el-radio-button label="仅退款" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因类型">
          <el-select v-model="retForm.reasonType" style="width: 100%">
            <el-option label="质量问题" value="质量问题" />
            <el-option label="发错货" value="发错货" />
            <el-option label="与描述不符" value="与描述不符" />
            <el-option label="7天无理由" value="7天无理由" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请件数">
          <el-input-number v-model="retForm.applyQty" :min="1" :max="retMax" />
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="retForm.description" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="描述具体问题，不少于 10 个字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="retDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReturn">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { fmtTime, fmtNum } from '@/utils/format'

const route = useRoute()
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const retDlg = ref(false)
const saving = ref(false)
const cur = ref({})
const retOrder = ref({})
const q = reactive({ page: 1, size: 6, status: '' })
const retForm = reactive({ returnType: '退货退款', reasonType: '质量问题', applyQty: 1, description: '' })

const fmt = (v) => fmtNum(v, 2)
const retMax = computed(() => Math.max(1, Number(retOrder.value.itemCount || 1)))
const stCls = (s) => ({
  待支付: 'st-gray', 待发货: 'st-amber', 已发货: 'st-blue', 已签收: 'st-blue',
  已完成: 'st-green', 退换中: 'st-red', 已取消: 'st-gray'
}[s] || 'st-gray')
const retCls = (s) => ({ 待审核: 'st-amber', 待寄回: 'st-blue', 已寄回: 'st-blue', 已退款: 'st-green', 已驳回: 'st-red', 已关闭: 'st-gray' }[s] || 'st-gray')

async function load() {
  loading.value = true
  try {
    const r = await api.myOrders({ ...q, status: q.status || undefined })
    rows.value = r.rows || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}
function search() {
  q.page = 1
  load()
}
async function detail(o) {
  cur.value = await api.orderDetail(o.id)
  dlg.value = true
}
async function pay(o) {
  await api.orderPay(o.id)
  ElMessage.success('支付成功，等待商家发货')
  load()
}
async function confirm(o) {
  await api.orderConfirm(o.id)
  ElMessage.success('已确认收货')
  load()
}
async function cancel(o) {
  const r = await ElMessageBox.prompt('请填写取消原因', '取消订单', {
    inputValue: '不想买了',
    inputValidator: (v) => (v && v.trim().length >= 4 ? true : '原因不少于 4 个字')
  }).catch(() => null)
  if (!r) return
  await api.orderCancel(o.id, { reason: r.value })
  ElMessage.success('已取消，库存已释放')
  load()
}
function applyReturn(o) {
  retOrder.value = o
  Object.assign(retForm, { returnType: '退货退款', reasonType: '质量问题', applyQty: 1, description: '' })
  retDlg.value = true
}
async function submitReturn() {
  if (!retForm.description || retForm.description.trim().length < 10) {
    ElMessage.warning('问题描述不少于 10 个字')
    return
  }
  saving.value = true
  try {
    await api.returnApply({ orderId: retOrder.value.id, bookId: retOrder.value.bookId, ...retForm })
    ElMessage.success('退换申请已提交，等待商家审核')
    retDlg.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (route.query.status) q.status = route.query.status
  load()
})
</script>

<style scoped>
.list { display: flex; flex-direction: column; gap: 10px; }
.order { padding: 12px 16px; }
.o-head { display: flex; align-items: center; gap: 14px; font-size: 12px; border-bottom: 1px dashed var(--line); padding-bottom: 9px; }
.o-no { font-family: ui-monospace, Menlo, monospace; font-weight: 600; }
.o-time { color: var(--text-3); font-size: 11.5px; }
.o-head .spacer { flex: 1; }
.o-body { display: flex; align-items: center; gap: 16px; padding-top: 10px; }
.o-txt { flex: 1; min-width: 0; }
.o-name { font-size: 13.5px; font-weight: 600; }
.o-sub { color: var(--text-3); font-size: 11.5px; line-height: 1.6; }
.o-amt { text-align: right; }
.o-amt span { display: block; color: var(--text-3); font-size: 11px; }
.o-amt b { color: var(--danger); font-size: 17px; }
.o-ops { display: flex; flex-direction: column; gap: 6px; width: 108px; }
.o-express { margin-top: 8px; color: var(--text-3); font-size: 11.5px; }
.ret { border-top: 1px dashed var(--line); padding-top: 8px; margin-top: 8px; }
.ret-head { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.ret p { margin: 4px 0 0; font-size: 12px; color: var(--text-2); line-height: 1.8; }
.empty { text-align: center; color: var(--text-3); font-size: 12.5px; padding: 46px 0; }
</style>
