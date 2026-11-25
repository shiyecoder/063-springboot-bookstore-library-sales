<template>
  <div v-loading="loading">
    <div class="crumb-bar">
      <span class="link" @click="$router.push('/portal')">首页</span> /
      <span class="link" @click="$router.push('/portal/books')">全部图书</span> /
      <span>{{ b.parentCategory }}</span> / <b>{{ b.bookName }}</b>
    </div>

    <div class="card detail">
      <img class="cover-lg" :src="b.coverImage" :alt="b.bookName" />
      <div class="info">
        <h1>{{ b.bookName }}</h1>
        <p class="sub">{{ b.subTitle }}</p>
        <div class="meta">
          <span>作者：{{ b.author }}</span>
          <span>出版社：{{ b.publisher }}</span>
          <span>出版日期：{{ fmtDate(b.publishDate) }}</span>
          <span>版次：{{ b.edition }}</span>
          <span>ISBN：{{ b.isbn }}</span>
        </div>

        <div class="price-box">
          <div class="price">
            <span class="cur">¥</span><b>{{ fmt(b.salePrice) }}</b>
            <s>定价 ¥{{ fmt(b.listPrice) }}</s>
            <span class="st st-amber">{{ (b.salePrice / b.listPrice * 10).toFixed(1) }} 折</span>
          </div>
          <div class="sale">累计售出 {{ b.totalSales }} 件 · 库存 {{ b.stockQty }} 件</div>
        </div>

        <div class="actions">
          <el-input-number v-model="qty" :min="1" :max="Math.max(1, b.stockQty)" :disabled="b.stockQty <= 0" />
          <el-button type="primary" size="large" :disabled="b.stockQty <= 0" @click="add">
            {{ b.stockQty > 0 ? '加入购物车' : '暂时缺货' }}
          </el-button>
          <el-button size="large" @click="buyNow" :disabled="b.stockQty <= 0">立即购买</el-button>
        </div>

        <div class="tips">
          <span>满 99 元免运费</span>
          <span>7 天无理由退换</span>
          <span>正版保障</span>
        </div>
      </div>
    </div>

    <div class="cols">
      <div class="card">
        <el-tabs v-model="tab">
          <el-tab-pane label="内容简介" name="sum">
            <p class="summary">{{ b.summary }}</p>
            <el-descriptions :column="2" size="small" border style="margin-top: 14px">
              <el-descriptions-item label="分类">{{ b.parentCategory }} / {{ b.categoryName }}</el-descriptions-item>
              <el-descriptions-item label="图书编号">{{ b.bookNo }}</el-descriptions-item>
              <el-descriptions-item label="锁定数量">{{ b.lockedQty }}</el-descriptions-item>
              <el-descriptions-item label="缺货次数">{{ b.shortageCount }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>
          <el-tab-pane :label="`读者书评（${(b.reviewStat || {}).count || 0}）`" name="rev">
            <div class="rev-head">
              <div class="rev-score">
                <b>{{ (b.reviewStat || {}).avgScore || '—' }}</b>
                <el-rate :model-value="Number((b.reviewStat || {}).avgScore || 0)" disabled />
                <span class="muted">共 {{ (b.reviewStat || {}).count || 0 }} 条已展示书评</span>
              </div>
              <el-button size="small" @click="writeReview" :disabled="!canReview">写书评</el-button>
            </div>
            <div v-for="r in b.reviews || []" :key="r.id" class="rev">
              <div class="rev-top">
                <img class="avatar" :src="r.avatar" alt="" />
                <b>{{ r.customerName }}</b>
                <el-rate :model-value="r.score" disabled size="small" />
                <span class="rev-time">{{ fmtTime(r.createTime) }}</span>
              </div>
              <div class="rev-title">{{ r.title }}</div>
              <p class="rev-txt">{{ r.content }}</p>
              <div v-if="r.imageUrls" class="rev-imgs">
                <el-image v-for="(u, i) in r.imageUrls.split(',')" :key="i" :src="u" fit="cover"
                          :preview-src-list="r.imageUrls.split(',')" :initial-index="i" preview-teleported class="rev-img" />
              </div>
              <div v-if="r.replyContent" class="reply">商家回复：{{ r.replyContent }}</div>
              <div class="rev-foot">
                <el-button link size="small" @click="useful(r)">有用（{{ r.usefulCount }}）</el-button>
              </div>
            </div>
            <p v-if="!(b.reviews || []).length" class="empty">这本书还没有书评</p>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div class="card">
        <h3 class="card-title">同类推荐</h3>
        <div v-for="s in b.sameCategory || []" :key="s.id" class="rec" @click="go(s.id)">
          <img class="cover" :src="s.coverImage" alt="" />
          <div class="rec-txt">
            <div class="rec-name">{{ s.bookName }}</div>
            <div class="rec-author">{{ s.author }}</div>
          </div>
          <b class="rec-price">¥{{ fmt(s.salePrice) }}</b>
        </div>
        <p v-if="!(b.sameCategory || []).length" class="empty">暂无推荐</p>
      </div>
    </div>

    <el-dialog v-model="dlg" title="写书评" width="520px">
      <el-form label-width="76px">
        <el-form-item label="订单号">
          <el-select v-model="form.orderId" placeholder="选择已完成的订单" style="width: 100%">
            <el-option v-for="o in myOrders" :key="o.id" :label="`${o.orderNo}（${fmtTime(o.createTime)}）`" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="form.score" show-text :texts="['很差', '较差', '一般', '满意', '推荐']" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="30" placeholder="一句话概括" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="300" show-word-limit
                    placeholder="正文不少于 15 字，可写内容、纸张、排版与物流体验" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReview">提交书评</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import { cart } from '@/store/cart'
import { store } from '@/store/user'
import { fmtNum, fmtDate, fmtTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const b = ref({})
const loading = ref(true)
const qty = ref(1)
const tab = ref('sum')
const dlg = ref(false)
const saving = ref(false)
const myOrders = ref([])
const form = reactive({ orderId: null, score: 5, title: '', content: '' })

const fmt = (v) => fmtNum(v, 2)
const canReview = computed(() => store.user.roleCode === 'READER' || !!store.user.id)

async function load() {
  loading.value = true
  try {
    b.value = await api.portalBook(route.params.id)
  } finally {
    loading.value = false
  }
}
function add() {
  cart.add(b.value, qty.value)
  ElMessage.success(`已加入购物车：${b.value.bookName} × ${qty.value}`)
}
function buyNow() {
  cart.add(b.value, qty.value)
  router.push('/portal/cart')
}
function go(id) {
  router.push(`/portal/book/${id}`)
  location.reload()
}
async function useful(r) {
  const res = await api.reviewUseful(r.id)
  r.usefulCount = res.usefulCount ?? r.usefulCount + 1
  ElMessage.success('已标记有用')
}
async function writeReview() {
  const r = await api.myOrders({ page: 1, size: 50, status: '已完成' })
  myOrders.value = (r.rows || []).filter((o) => o.bookId === b.value.id)
  if (!myOrders.value.length) {
    ElMessage.warning('只有已完成订单才能评价，且同一订单同一图书仅 1 条书评')
    return
  }
  form.orderId = myOrders.value[0].id
  dlg.value = true
}
async function submitReview() {
  if (!form.orderId) {
    ElMessage.warning('请选择订单')
    return
  }
  if (!form.content || form.content.trim().length < 15) {
    ElMessage.warning('书评正文不少于 15 个字')
    return
  }
  saving.value = true
  try {
    await api.reviewCreate({ ...form, bookId: b.value.id })
    ElMessage.success('书评已提交，等待审核后展示')
    dlg.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.crumb-bar { font-size: 12px; color: var(--text-3); margin-bottom: 10px; }
.crumb-bar .link { color: var(--primary); cursor: pointer; }
.crumb-bar b { color: var(--text); font-weight: 500; }
.detail { display: flex; gap: 26px; margin-bottom: 12px; }
.info { flex: 1; min-width: 0; }
.info h1 { margin: 0 0 4px; font-size: 22px; }
.sub { color: var(--text-3); font-size: 12.5px; margin: 0 0 12px; }
.meta { display: flex; flex-wrap: wrap; gap: 6px 22px; color: var(--text-2); font-size: 12px; }
.price-box { background: var(--paper); border-radius: 5px; padding: 12px 14px; margin: 14px 0; }
.price { display: flex; align-items: baseline; gap: 10px; }
.price .cur { color: var(--danger); font-size: 15px; }
.price b { color: var(--danger); font-size: 30px; line-height: 1; }
.price s { color: var(--text-3); font-size: 12px; }
.sale { color: var(--text-3); font-size: 11.5px; margin-top: 6px; }
.actions { display: flex; align-items: center; gap: 10px; }
.tips { display: flex; gap: 18px; margin-top: 14px; color: var(--text-3); font-size: 11.5px; }
.cols { display: grid; grid-template-columns: 1fr 300px; gap: 12px; align-items: start; }
.summary { color: var(--text-2); font-size: 13px; line-height: 2; margin: 0; }
.rev-head { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px dashed var(--line); padding-bottom: 10px; }
.rev-score { display: flex; align-items: baseline; gap: 10px; }
.rev-score b { font-size: 26px; color: var(--warn); }
.muted { color: var(--text-3); font-size: 11.5px; }
.rev { border-bottom: 1px dashed var(--line); padding: 11px 0; }
.rev-top { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.rev-time { margin-left: auto; color: var(--text-3); font-size: 11px; }
.rev-title { font-weight: 600; font-size: 13px; margin: 6px 0 3px; }
.rev-txt { color: var(--text-2); font-size: 12.5px; line-height: 1.9; margin: 0; }
.rev-imgs { display: flex; gap: 6px; margin-top: 8px; }
.rev-img { width: 74px; height: 56px; border-radius: 3px; border: 1px solid var(--line); }
.reply { background: #F2F6FC; border-left: 3px solid var(--primary); color: var(--text-2);
  font-size: 12px; padding: 7px 10px; border-radius: 3px; margin-top: 8px; line-height: 1.8; }
.rev-foot { margin-top: 6px; }
.rec { display: flex; gap: 9px; align-items: center; padding: 8px 0; border-bottom: 1px dashed var(--line); cursor: pointer; }
.rec:hover .rec-name { color: var(--primary); }
.rec-txt { flex: 1; min-width: 0; }
.rec-name { font-size: 12.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rec-author { color: var(--text-3); font-size: 11px; }
.rec-price { color: var(--danger); font-size: 12.5px; }
.empty { color: var(--text-3); font-size: 12px; text-align: center; padding: 22px 0; }
@media (max-width: 1000px) { .cols { grid-template-columns: 1fr; } .detail { flex-direction: column; } }
</style>
