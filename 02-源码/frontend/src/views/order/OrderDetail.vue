<template>
  <div v-loading="loading">
    <div class="page-head">
      <h2>订单详情</h2>
      <span class="sub">{{ o.orderNo }}</span>
      <span class="st" :class="stCls(o.status)" style="margin-left: 4px">{{ o.status }}</span>
      <span class="spacer"></span>
      <el-button size="small" @click="$router.push('/order')">返回台账</el-button>
      <el-button v-if="o.status === '待发货' && canDeliver" type="primary" size="small"
                 @click="$router.push('/order/deliver')">去发货</el-button>
    </div>

    <div class="od-wrap">
      <div class="col">
        <div class="card">
          <h3 class="card-title">商品信息</h3>
          <div class="goods">
            <img class="cover-lg" :src="o.coverImage" alt="" />
            <div class="goods-txt">
              <h3>{{ o.bookName }}</h3>
              <p class="sub">{{ o.author }} · {{ o.publisher }}</p>
              <el-descriptions :column="2" size="small" border style="margin-top: 10px">
                <el-descriptions-item label="单价">¥{{ fmt(o.unitPrice) }}</el-descriptions-item>
                <el-descriptions-item label="件数">{{ o.itemCount }}</el-descriptions-item>
                <el-descriptions-item label="图书金额">¥{{ fmt(o.bookAmount) }}</el-descriptions-item>
                <el-descriptions-item label="运费">¥{{ fmt(o.freight) }}</el-descriptions-item>
                <el-descriptions-item label="优惠">-¥{{ fmt(o.discountAmount) }}</el-descriptions-item>
                <el-descriptions-item label="实付">
                  <b style="color: var(--primary); font-size: 15px">¥{{ fmt(o.payAmount) }}</b>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">收货与物流</h3>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="收件人">{{ o.receiverName }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ o.receiverPhone }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">{{ o.addressDesc }}</el-descriptions-item>
            <el-descriptions-item label="快递公司">{{ o.expressCompany || '—' }}</el-descriptions-item>
            <el-descriptions-item label="快递单号">{{ o.expressNo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ fmtTime(o.payTime) }}</el-descriptions-item>
            <el-descriptions-item label="发货时间">{{ fmtTime(o.deliverTime) }}</el-descriptions-item>
            <el-descriptions-item label="签收时间">{{ fmtTime(o.signTime) }}</el-descriptions-item>
            <el-descriptions-item label="买家">{{ o.customerName }}</el-descriptions-item>
          </el-descriptions>
          <el-alert v-if="o.cancelReason" type="error" :closable="false" show-icon style="margin-top: 12px"
                    :title="`取消原因：${o.cancelReason}`" />
        </div>

        <div class="card" v-if="(o.returns || []).length">
          <h3 class="card-title">退换记录</h3>
          <div v-for="r in o.returns" :key="r.id" class="ret">
            <div class="ret-head">
              <b>{{ r.returnNo }}</b>
              <span class="st" :class="retCls(r.status)">{{ r.status }}</span>
              <span class="muted">{{ r.returnType }} · {{ r.applyQty }} 件 · 退款 ¥{{ fmt(r.refundAmount) }}</span>
            </div>
            <p class="ret-txt">原因：{{ r.reasonType }}｜{{ r.description }}</p>
            <p v-if="r.auditNote" class="ret-txt">处理说明：{{ r.auditNote }}</p>
          </div>
        </div>

        <div class="card" v-if="o.review">
          <h3 class="card-title">买家书评 <span class="tag">{{ o.review.score }} 分</span></h3>
          <p class="ret-txt">{{ o.review.title }}｜{{ o.review.content }}</p>
          <p v-if="o.review.replyContent" class="ret-txt" style="color: var(--primary)">
            商家回复：{{ o.review.replyContent }}
          </p>
        </div>
      </div>

      <div class="col side">
        <div class="card">
          <h3 class="card-title">订单进度</h3>
          <el-timeline>
            <el-timeline-item v-for="(t, i) in o.timeline || []" :key="i" :timestamp="fmtTime(t.time)"
                              :color="i === (o.timeline || []).length - 1 ? '#3A5A8C' : '#C9C9C9'">
              {{ t.name }}
            </el-timeline-item>
          </el-timeline>
        </div>

        <div class="card">
          <h3 class="card-title">时效</h3>
          <div class="mini">
            <div><span>下单 → 支付</span><b>{{ gap(o.createTime, o.payTime) }}</b></div>
            <div><span>支付 → 发货</span><b>{{ gap(o.payTime, o.deliverTime) }}</b></div>
            <div><span>发货 → 签收</span><b>{{ gap(o.deliverTime, o.signTime) }}</b></div>
            <div><span>下单至今</span><b>{{ gap(o.createTime, new Date()) }}</b></div>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">可执行操作</h3>
          <div class="ops">
            <el-button v-if="o.status === '待发货' && canDeliver" type="primary" size="small" style="width: 100%"
                       @click="$router.push('/order/deliver')">发货</el-button>
            <el-button v-if="o.status === '已发货'" size="small" style="width: 100%" @click="confirm">确认收货</el-button>
            <el-button v-if="['待支付', '待发货'].includes(o.status)" size="small" style="width: 100%" @click="cancel">
              取消订单
            </el-button>
            <el-button size="small" style="width: 100%" @click="$router.push('/review')">查看相关书评</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime, fmtNum, fmtGap } from '@/utils/format'

const route = useRoute()
const o = ref({})
const loading = ref(true)
const canDeliver = computed(() => ['ADMIN', 'CLERK'].includes(store.user.roleCode))
const fmt = (v) => fmtNum(v, 2)
const gap = (a, b) => (a && b ? fmtGap(a, b) : '—')
const stCls = (s) => ({
  待支付: 'st-gray', 待发货: 'st-amber', 已发货: 'st-blue', 已签收: 'st-blue',
  已完成: 'st-green', 退换中: 'st-red', 已取消: 'st-gray'
}[s] || 'st-gray')
const retCls = (s) => ({ 待审核: 'st-amber', 待寄回: 'st-blue', 已寄回: 'st-blue', 已退款: 'st-green', 已驳回: 'st-red', 已关闭: 'st-gray' }[s] || 'st-gray')

async function load() {
  loading.value = true
  try {
    o.value = await api.orderDetail(route.params.id)
  } finally {
    loading.value = false
  }
}
async function confirm() {
  await api.orderConfirm(o.value.id)
  ElMessage.success('已确认收货')
  load()
}
async function cancel() {
  const r = await ElMessageBox.prompt('请填写取消原因', '取消订单', {
    inputValidator: (v) => (v && v.trim().length >= 4 ? true : '原因不少于 4 个字')
  }).catch(() => null)
  if (!r) return
  await api.orderCancel(o.value.id, { reason: r.value })
  ElMessage.success('已取消，库存已回补')
  load()
}

onMounted(load)
</script>

<style scoped>
.od-wrap { display: grid; grid-template-columns: 1fr 300px; gap: 12px; align-items: start; }
.col { display: flex; flex-direction: column; gap: 12px; }
.goods { display: flex; gap: 18px; }
.goods-txt { flex: 1; min-width: 0; }
.goods-txt h3 { margin: 0 0 4px; font-size: 15px; }
.sub { color: var(--text-3); font-size: 12px; margin: 0; }
.ret { border-top: 1px dashed var(--line); padding: 8px 0; }
.ret-head { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.ret-txt { color: var(--text-2); font-size: 12px; margin: 5px 0 0; line-height: 1.8; }
.muted { color: var(--text-3); font-size: 11.5px; }
.mini { display: grid; gap: 7px; }
.mini div { display: flex; justify-content: space-between; background: var(--paper); padding: 6px 9px; border-radius: 4px; }
.mini span { color: var(--text-3); font-size: 11.5px; }
.mini b { font-size: 12px; }
.ops { display: flex; flex-direction: column; gap: 8px; }
@media (max-width: 1100px) { .od-wrap { grid-template-columns: 1fr; } }
</style>
