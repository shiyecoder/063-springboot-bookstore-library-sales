<template>
  <div>
    <div class="page-head">
      <h2>移动端预览</h2>
      <span class="sub">同一套接口与数据，自适应 375px 宽度；扫码或手机浏览器可直接访问</span>
    </div>

    <div class="mobile-wrap">
      <div class="phone">
        <div class="notch"></div>
        <div class="p-screen">
          <div class="p-top">
            <span class="p-brand">拾页书店</span>
            <span class="p-cart">购物车 {{ cart.count }}</span>
          </div>
          <div class="p-search">
            <span>搜书名 / 作者</span>
          </div>
          <div class="p-banner">满 99 免运费 · 7 天无理由退换</div>

          <div class="p-sec">
            <div class="p-sec-head">新书上架</div>
            <div class="p-books">
              <div v-for="b in home.newBooks || []" :key="b.id" class="p-book" @click="$router.push(`/portal/book/${b.id}`)">
                <img :src="b.coverImage" alt="" />
                <div class="p-name">{{ b.bookName }}</div>
                <div class="p-price">¥{{ fmt(b.salePrice) }}</div>
              </div>
            </div>
          </div>

          <div class="p-sec">
            <div class="p-sec-head">热销榜</div>
            <div v-for="(b, i) in (home.hotBooks || []).slice(0, 4)" :key="b.id" class="p-row"
                 @click="$router.push(`/portal/book/${b.id}`)">
              <span class="p-rank" :class="{ top: i < 3 }">{{ i + 1 }}</span>
              <img :src="b.coverImage" alt="" />
              <div class="p-row-txt">
                <div class="p-name">{{ b.bookName }}</div>
                <div class="p-sub">{{ b.author }}</div>
              </div>
              <b class="p-price">¥{{ fmt(b.salePrice) }}</b>
            </div>
          </div>

          <div class="p-tabbar">
            <span class="on">首页</span>
            <span>分类</span>
            <span>购物车</span>
            <span>我的</span>
          </div>
        </div>
      </div>

      <div class="card notes">
        <h3 class="card-title">说明</h3>
        <p>本站所有页面在 768px 断点下会自动收起侧栏、把四列看板压成两列，手机浏览器直接访问即为上图效果。</p>
        <el-descriptions :column="1" size="small" border style="margin-top: 12px">
          <el-descriptions-item label="在售图书">{{ home.stat.books || 0 }} 种</el-descriptions-item>
          <el-descriptions-item label="累计订单">{{ home.stat.orders || 0 }} 笔</el-descriptions-item>
          <el-descriptions-item label="读者书评">{{ home.stat.reviews || 0 }} 条</el-descriptions-item>
          <el-descriptions-item label="购物车">{{ cart.count }} 件</el-descriptions-item>
        </el-descriptions>
        <el-alert type="info" :closable="false" show-icon style="margin-top: 12px"
                  title="移动端与 PC 端共用同一套接口，不做独立后端，减少论文中的重复工作" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '@/api'
import { cart } from '@/store/cart'
import { fmtNum } from '@/utils/format'

const home = ref({ stat: {} })
const fmt = (v) => fmtNum(v, 2)

onMounted(async () => {
  home.value = await api.portalHome()
})
</script>

<style scoped>
.mobile-wrap { display: flex; gap: 24px; align-items: flex-start; }
.phone { width: 340px; height: 660px; background: #1F2933; border-radius: 30px; padding: 12px; position: relative;
  box-shadow: 0 8px 28px rgba(31, 41, 51, .18); flex: 0 0 340px; }
.notch { position: absolute; top: 14px; left: 50%; transform: translateX(-50%); width: 90px; height: 6px;
  border-radius: 3px; background: #0E1520; z-index: 2; }
.p-screen { height: 100%; background: var(--bg); border-radius: 22px; overflow-y: auto; padding: 16px 12px 54px;
  position: relative; }
.p-screen::-webkit-scrollbar { display: none; }
.p-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.p-brand { font-size: 14px; font-weight: 700; color: var(--primary-dark); }
.p-cart { font-size: 11px; color: var(--text-3); }
.p-search { background: #fff; border: 1px solid var(--line); border-radius: 14px; padding: 7px 12px;
  color: var(--text-3); font-size: 11.5px; margin-bottom: 10px; }
.p-banner { background: linear-gradient(90deg, #3A5A8C, #6B8CBE); color: #fff; border-radius: 6px;
  padding: 9px 12px; font-size: 11.5px; margin-bottom: 14px; }
.p-sec { margin-bottom: 14px; }
.p-sec-head { font-size: 12.5px; font-weight: 600; margin-bottom: 8px; }
.p-books { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.p-book { background: #fff; border-radius: 5px; padding: 6px; }
.p-book img { width: 100%; height: 72px; object-fit: cover; border-radius: 3px; }
.p-name { font-size: 10.5px; line-height: 1.4; margin-top: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.p-price { color: var(--danger); font-size: 11.5px; font-weight: 600; }
.p-row { display: flex; align-items: center; gap: 8px; background: #fff; border-radius: 5px; padding: 7px 8px; margin-bottom: 7px; }
.p-row img { width: 32px; height: 42px; object-fit: cover; border-radius: 3px; }
.p-row-txt { flex: 1; min-width: 0; }
.p-sub { color: var(--text-3); font-size: 10px; }
.p-rank { width: 16px; height: 16px; border-radius: 3px; background: var(--line); color: var(--text-2);
  font-size: 10px; display: flex; align-items: center; justify-content: center; }
.p-rank.top { background: var(--accent); color: #2C456D; font-weight: 700; }
.p-tabbar { position: sticky; bottom: 0; display: flex; justify-content: space-around; background: #fff;
  border-top: 1px solid var(--line); padding: 8px 0; font-size: 10.5px; color: var(--text-3); margin: 0 -12px -54px; }
.p-tabbar .on { color: var(--primary); font-weight: 600; }
.notes { flex: 1; }
.notes p { color: var(--text-2); font-size: 12.5px; line-height: 1.9; margin: 0; }
@media (max-width: 1000px) { .mobile-wrap { flex-direction: column; } .phone { flex: none; } }
</style>
