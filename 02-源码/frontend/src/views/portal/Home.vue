<template>
  <div>
    <section class="hero">
      <div class="hero-bg" :style="{ backgroundImage: `url(/uploads/seed/bg/01.jpg)` }"></div>
      <div class="hero-inner">
        <div class="hero-txt">
          <h1>拾页书店</h1>
          <p class="hslogan">37 种在售图书 · 6 大分类 · 当日下单次日发货</p>
          <div class="hstat">
            <div><b>{{ home.stat.books || 0 }}</b><span>在售图书</span></div>
            <div><b>{{ home.stat.orders || 0 }}</b><span>累计订单</span></div>
            <div><b>{{ home.stat.reviews || 0 }}</b><span>读者书评</span></div>
          </div>
          <el-button type="primary" size="large" @click="$router.push('/portal/books')">开始选书</el-button>
        </div>
        <div class="hero-card">
          <h3>购物车</h3>
          <p v-if="!cart.items.length" class="hint">还没有选书，从左侧挑一本开始吧</p>
          <template v-else>
            <div v-for="i in cart.items.slice(0, 3)" :key="i.id" class="hc-item">
              <img class="cover" :src="i.coverImage" alt="" />
              <div class="hc-txt">
                <div class="hc-name">{{ i.bookName }}</div>
                <div class="hc-sub">× {{ i.qty }}</div>
              </div>
              <b>¥{{ fmt(i.qty * i.salePrice) }}</b>
            </div>
            <div class="hc-foot">
              <span>共 {{ cart.count }} 件</span>
              <el-button type="primary" size="small" @click="$router.push('/portal/cart')">去结算</el-button>
            </div>
          </template>
        </div>
      </div>
    </section>

    <section class="cats">
      <div v-for="c in home.categories || []" :key="c.id" class="cat" @click="$router.push(`/portal/books?categoryId=${c.id}`)">
        <span class="cat-name">{{ c.categoryName }}</span>
        <span class="cat-cnt">{{ c.bookCount }} 种</span>
      </div>
    </section>

    <section class="block">
      <div class="block-head">
        <h2>新书上架</h2>
        <span class="spacer"></span>
        <el-button link type="primary" @click="$router.push('/portal/books?sort=new')">查看全部</el-button>
      </div>
      <div class="grid">
        <div v-for="b in home.newBooks || []" :key="b.id" class="book" @click="go(b)">
          <img class="book-cover" :src="b.coverImage" :alt="b.bookName" />
          <div class="book-name">{{ b.bookName }}</div>
          <div class="book-author">{{ b.author }}</div>
          <div class="book-foot">
            <b>¥{{ fmt(b.salePrice) }}</b>
            <s>¥{{ fmt(b.listPrice) }}</s>
            <span v-if="b.stockQty <= 0" class="st st-red">缺货</span>
          </div>
        </div>
      </div>
    </section>

    <section class="block">
      <div class="block-head">
        <h2>热销榜</h2>
        <span class="sub">按累计销量排序</span>
        <span class="spacer"></span>
        <el-button link type="primary" @click="$router.push('/portal/books?sort=sales')">查看全部</el-button>
      </div>
      <div class="grid">
        <div v-for="(b, i) in home.hotBooks || []" :key="b.id" class="book" @click="go(b)">
          <span class="rank" :class="{ top: i < 3 }">{{ i + 1 }}</span>
          <img class="book-cover" :src="b.coverImage" :alt="b.bookName" />
          <div class="book-name">{{ b.bookName }}</div>
          <div class="book-author">{{ b.author }} · 已售 {{ b.totalSales }}</div>
          <div class="book-foot"><b>¥{{ fmt(b.salePrice) }}</b><s>¥{{ fmt(b.listPrice) }}</s></div>
        </div>
      </div>
    </section>

    <section class="block two">
      <div class="card">
        <h3 class="card-title">分类专题</h3>
        <div class="topics">
          <div v-for="t in home.topics || []" :key="t.id" class="topic"
               @click="$router.push(`/portal/books?categoryId=${t.id}`)">
            <span>{{ t.name }}</span>
            <em>{{ t.cnt }} 种</em>
          </div>
        </div>
      </div>
      <div class="card">
        <h3 class="card-title">书店公告</h3>
        <div v-for="n in home.notices || []" :key="n.id" class="notice">
          <span class="st st-blue">{{ n.noticeType }}</span>
          <span class="notice-title">{{ n.title }}</span>
          <span class="notice-time">{{ fmtDate(n.publishTime) }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api'
import { cart } from '@/store/cart'
import { fmtNum, fmtDate } from '@/utils/format'

const router = useRouter()
const home = ref({ stat: {} })
const fmt = (v) => fmtNum(v, 2)

function go(b) {
  router.push(`/portal/book/${b.id}`)
}

onMounted(async () => {
  home.value = await api.portalHome()
})
</script>

<style scoped>
.hero { position: relative; border-radius: 8px; overflow: hidden; margin-bottom: 14px; }
.hero-bg { position: absolute; inset: 0; background-size: cover; background-position: center; }
.hero-inner {
  position: relative; display: flex; gap: 18px; padding: 30px 32px; align-items: center;
  background: linear-gradient(100deg, rgba(34,49,74,.94) 0%, rgba(44,69,109,.88) 55%, rgba(44,69,109,.62) 100%);
  color: #F2F5F9;
}
.hero-txt { flex: 1; }
.hero-txt h1 { margin: 0 0 8px; font-size: 30px; letter-spacing: 3px; }
.hslogan { color: rgba(242,245,249,.72); font-size: 13px; margin: 0 0 16px; }
.hstat { display: flex; gap: 28px; margin-bottom: 18px; }
.hstat div { display: flex; flex-direction: column; }
.hstat b { font-size: 22px; color: var(--accent); }
.hstat span { font-size: 11.5px; color: rgba(242,245,249,.6); }
.hero-card { width: 300px; background: rgba(255,255,255,.96); border-radius: 6px; padding: 14px; color: var(--text); }
.hero-card h3 { margin: 0 0 10px; font-size: 13px; }
.hint { color: var(--text-3); font-size: 12px; text-align: center; padding: 12px 0; margin: 0; }
.hc-item { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.hc-txt { flex: 1; min-width: 0; }
.hc-name { font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hc-sub { color: var(--text-3); font-size: 11px; }
.hc-item b { font-size: 12px; color: var(--primary); }
.hc-foot { display: flex; justify-content: space-between; align-items: center; border-top: 1px dashed var(--line); padding-top: 10px; font-size: 12px; }

.cats { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; margin-bottom: 18px; }
.cat { background: var(--card); border: 1px solid var(--line); border-radius: 6px; padding: 11px 14px;
  display: flex; justify-content: space-between; align-items: center; cursor: pointer; }
.cat:hover { border-color: var(--primary-light); background: #F6F9FD; }
.cat-name { font-size: 13px; font-weight: 600; }
.cat-cnt { color: var(--text-3); font-size: 11.5px; }

.block { margin-bottom: 18px; }
.block-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 10px; }
.block-head h2 { font-size: 16px; margin: 0; }
.block-head .sub { color: var(--text-3); font-size: 11.5px; }
.block-head .spacer { flex: 1; }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.book { position: relative; background: var(--card); border: 1px solid var(--line); border-radius: 6px;
  padding: 12px; cursor: pointer; }
.book:hover { border-color: var(--primary-light); box-shadow: var(--shadow); }
.book-cover { width: 100%; height: 168px; object-fit: cover; border-radius: 4px; background: var(--paper); }
.book-name { font-size: 13px; font-weight: 600; margin-top: 9px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.book-author { color: var(--text-3); font-size: 11.5px; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.book-foot { display: flex; align-items: baseline; gap: 7px; margin-top: 7px; }
.book-foot b { color: var(--danger); font-size: 15px; }
.book-foot s { color: var(--text-3); font-size: 11px; }
.rank { position: absolute; top: 8px; left: 8px; width: 20px; height: 20px; border-radius: 4px;
  background: rgba(31,41,51,.55); color: #fff; font-size: 11.5px; display: flex; align-items: center; justify-content: center; }
.rank.top { background: var(--accent); color: #2C456D; font-weight: 700; }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.topics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 9px; }
.topic { background: var(--paper); border-radius: 4px; padding: 9px 12px; cursor: pointer; display: flex;
  justify-content: space-between; align-items: center; }
.topic:hover { background: #F0F4FA; }
.topic span { font-size: 12.5px; }
.topic em { font-style: normal; color: var(--text-3); font-size: 11px; }
.notice { display: flex; align-items: center; gap: 9px; padding: 7px 0; border-bottom: 1px dashed var(--line); font-size: 12.5px; }
.notice:last-child { border-bottom: none; }
.notice-title { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.notice-time { color: var(--text-3); font-size: 11px; }
@media (max-width: 1000px) {
  .hero-inner { flex-direction: column; align-items: stretch; }
  .hero-card { width: auto; }
  .cats { grid-template-columns: repeat(2, 1fr); }
  .grid { grid-template-columns: repeat(2, 1fr); }
  .two { grid-template-columns: 1fr; }
}
</style>
