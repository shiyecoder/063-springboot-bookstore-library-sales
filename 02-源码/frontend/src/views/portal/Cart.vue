<template>
  <div>
    <div class="page-head">
      <h2>购物车</h2>
      <span class="sub">共 {{ cart.items.length }} 种 / {{ cart.count }} 件，满 99 元免运费</span>
      <span class="spacer"></span>
      <el-button size="small" @click="$router.push('/portal/books')">继续选书</el-button>
    </div>

    <div v-if="!cart.items.length" class="card empty-box">
      <p>购物车还是空的</p>
      <el-button type="primary" @click="$router.push('/portal/books')">去挑一本</el-button>
    </div>

    <div v-else class="cart-wrap">
      <div class="card">
        <el-table :data="cart.items" size="small">
          <el-table-column label="图书" min-width="240">
            <template #default="{ row }">
              <div class="bk">
                <img class="cover" :src="row.coverImage" alt="" />
                <div>
                  <div class="bk-name">{{ row.bookName }}</div>
                  <div class="bk-sub">{{ row.author }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="98" align="right">
            <template #default="{ row }">¥{{ fmt(row.salePrice) }}</template>
          </el-table-column>
          <el-table-column label="数量" width="140">
            <template #default="{ row }">
              <el-input-number :model-value="row.qty" :min="1" :max="Math.max(1, row.stockQty)" size="small"
                               @change="(v) => cart.setQty(row.id, v)" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="106" align="right">
            <template #default="{ row }"><b style="color: var(--danger)">¥{{ fmt(row.qty * row.salePrice) }}</b></template>
          </el-table-column>
          <el-table-column label="库存" width="80" align="right">
            <template #default="{ row }">
              <span :style="{ color: row.stockQty <= 5 ? '#B02A37' : 'inherit' }">{{ row.stockQty }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click="cart.remove(row.id)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="clr">
          <el-button link type="danger" size="small" @click="clearAll">清空购物车</el-button>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">结算</h3>
        <el-form label-position="top">
          <el-form-item label="收件人">
            <el-input v-model="form.receiverName" placeholder="请输入收件人姓名" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.receiverPhone" placeholder="11 位手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="收货地址">
            <el-input v-model="form.addressDesc" type="textarea" :rows="2" placeholder="省市区 + 详细门牌" />
          </el-form-item>
        </el-form>

        <div class="sum">
          <div><span>图书金额</span><b>¥{{ fmt(amount) }}</b></div>
          <div><span>运费</span><b>{{ freight > 0 ? '¥' + fmt(freight) : '免运费' }}</b></div>
          <div class="total"><span>应付</span><b>¥{{ fmt(amount + freight) }}</b></div>
        </div>

        <el-alert v-if="amount > 0 && amount < 99" type="info" :closable="false" show-icon
                  :title="`再买 ¥${fmt(99 - amount)} 可免运费`" style="margin: 10px 0" />

        <el-button type="primary" size="large" style="width: 100%" :loading="saving" @click="submit">
          提交订单（{{ cart.count }} 件）
        </el-button>
        <p class="hint">提交后订单进入待支付，30 分钟内未支付将自动取消并释放库存</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { cart, cartAmount, cartCount } from '@/store/cart'
import { store } from '@/store/user'
import { fmtNum } from '@/utils/format'

const router = useRouter()
const saving = ref(false)
const form = reactive({ receiverName: '', receiverPhone: '', addressDesc: '' })

const fmt = (v) => fmtNum(v, 2)
const amount = computed(() => cartAmount())
const freight = computed(() => (amount.value >= 99 || amount.value === 0 ? 0 : 6))

async function submit() {
  if (!cart.items.length) return
  if (!form.receiverName || form.receiverName.trim().length < 2) {
    ElMessage.warning('请填写收件人姓名')
    return
  }
  if (!/^1\d{10}$/.test(form.receiverPhone)) {
    ElMessage.warning('请填写 11 位手机号')
    return
  }
  if (!form.addressDesc || form.addressDesc.trim().length < 8) {
    ElMessage.warning('收货地址不少于 8 个字')
    return
  }
  await ElMessageBox.confirm(
    `共 ${cart.items.length} 种 ${cartCount()} 件，应付 ¥${fmt(amount.value + freight.value)}。每本书会生成一笔独立订单，确认提交？`,
    '提交订单',
    { type: 'info' }
  )
  saving.value = true
  const created = []
  try {
    for (const i of cart.items) {
      const r = await api.orderCreate({ bookId: i.id, itemCount: i.qty, ...form })
      created.push(r)
    }
    cart.clear()
    ElMessage.success(`已生成 ${created.length} 笔订单，正在跳转支付`)
    router.push('/portal/orders?status=待支付')
  } catch (e) {
    if (created.length) {
      ElMessage.warning(`部分订单已创建（${created.length} 笔），请到「我的订单」查看`)
      router.push('/portal/orders')
    }
  } finally {
    saving.value = false
  }
}
async function clearAll() {
  await ElMessageBox.confirm('确认清空购物车？', '提示', { type: 'warning' })
  cart.clear()
}

onMounted(() => {
  form.receiverName = store.user.realName || ''
  form.receiverPhone = store.user.phone || ''
})
</script>

<style scoped>
.cart-wrap { display: grid; grid-template-columns: 1fr 316px; gap: 12px; align-items: start; }
.bk { display: flex; gap: 9px; align-items: center; }
.bk-name { font-size: 12.5px; font-weight: 500; }
.bk-sub { color: var(--text-3); font-size: 11px; }
.clr { text-align: right; padding-top: 8px; }
.sum { border-top: 1px dashed var(--line); padding-top: 10px; }
.sum div { display: flex; justify-content: space-between; font-size: 12.5px; padding: 4px 0; }
.sum b { color: var(--text); }
.sum .total { border-top: 1px dashed var(--line); margin-top: 6px; padding-top: 9px; }
.sum .total b { color: var(--danger); font-size: 20px; }
.hint { color: var(--text-3); font-size: 11px; line-height: 1.7; margin: 8px 0 0; }
.empty-box { text-align: center; padding: 60px 0; color: var(--text-3); }
.empty-box p { margin: 0 0 14px; }
@media (max-width: 1000px) { .cart-wrap { grid-template-columns: 1fr; } }
</style>
