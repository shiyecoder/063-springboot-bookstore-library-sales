<template>
  <div>
    <div class="page-head">
      <h2>全部图书</h2>
      <span class="sub">共 {{ total }} 种{{ q.keyword ? `，关键词「${q.keyword}」` : '' }}</span>
      <span class="spacer"></span>
      <el-select v-model="q.sort" size="small" style="width: 130px" @change="search">
        <el-option label="综合排序" value="new" />
        <el-option label="销量优先" value="sales" />
        <el-option label="价格从低到高" value="priceAsc" />
        <el-option label="价格从高到低" value="priceDesc" />
      </el-select>
    </div>

    <div class="side-wrap">
      <div class="card aside">
        <h3 class="card-title">分类</h3>
        <div class="c-item" :class="{ on: !q.categoryId }" @click="pickCat(null)">全部图书</div>
        <template v-for="c in cats" :key="c.id">
          <div class="c-item" :class="{ on: q.categoryId === c.id }" @click="pickCat(c.id)">
            {{ c.categoryName }}<em>{{ c.bookCount }}</em>
          </div>
          <div v-for="s in c.children || []" :key="s.id" class="c-item sub" :class="{ on: q.categoryId === s.id }"
               @click="pickCat(s.id)">
            {{ s.categoryName }}
          </div>
        </template>
      </div>

      <div class="main">
        <div class="filters" style="margin-bottom: 12px">
          <el-input v-model="q.keyword" placeholder="搜书名 / 作者 / ISBN" clearable style="width: 280px"
                    @keyup.enter="search">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" size="small" @click="search">搜索</el-button>
          <span class="spacer"></span>
          <el-tag v-if="q.categoryId" closable size="small" @close="pickCat(null)">
            {{ catName }}
          </el-tag>
        </div>

        <div class="grid" v-loading="loading">
          <div v-for="b in rows" :key="b.id" class="book" @click="$router.push(`/portal/book/${b.id}`)">
            <img class="book-cover" :src="b.coverImage" :alt="b.bookName" />
            <div class="book-name">{{ b.bookName }}</div>
            <div class="book-author">{{ b.author }} · {{ b.publisher }}</div>
            <div class="book-foot">
              <b>¥{{ fmt(b.salePrice) }}</b>
              <s>¥{{ fmt(b.listPrice) }}</s>
              <span class="stock" :class="{ low: b.stockQty <= 5 }">库存 {{ b.stockQty }}</span>
            </div>
            <el-button size="small" class="add" :disabled="b.stockQty <= 0" @click.stop="add(b)">
              {{ b.stockQty > 0 ? '加入购物车' : '暂时缺货' }}
            </el-button>
          </div>
          <p v-if="!rows.length && !loading" class="empty">没有找到符合条件的图书</p>
        </div>

        <div class="pager">
          <el-pagination background layout="total, prev, pager, next" :total="total"
                         v-model:current-page="q.page" :page-size="q.size" @current-change="load" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { api } from '@/api'
import { cart } from '@/store/cart'
import { fmtNum } from '@/utils/format'

const route = useRoute()
const rows = ref([])
const total = ref(0)
const cats = ref([])
const loading = ref(false)
const q = reactive({ page: 1, size: 12, keyword: '', categoryId: null, sort: 'new' })
const fmt = (v) => fmtNum(v, 2)

const catName = computed(() => {
  for (const c of cats.value) {
    if (c.id === q.categoryId) return c.categoryName
    for (const s of c.children || []) if (s.id === q.categoryId) return s.categoryName
  }
  return ''
})

async function load() {
  loading.value = true
  try {
    const r = await api.portalBooks({
      page: q.page,
      size: q.size,
      keyword: q.keyword || undefined,
      categoryId: q.categoryId || undefined,
      sort: q.sort
    })
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
function pickCat(id) {
  q.categoryId = id
  search()
}
function add(b) {
  cart.add(b, 1)
  ElMessage.success(`《${b.bookName}》已加入购物车`)
}

watch(() => route.query, (v) => {
  if (v.keyword !== undefined) q.keyword = v.keyword || ''
  if (v.categoryId !== undefined) q.categoryId = Number(v.categoryId) || null
  if (v.sort) q.sort = v.sort
  search()
})

onMounted(async () => {
  const o = await api.options()
  cats.value = o.categories || []
  q.keyword = route.query.keyword || ''
  q.categoryId = Number(route.query.categoryId) || null
  if (route.query.sort) q.sort = route.query.sort
  load()
})
</script>

<style scoped>
.side-wrap { display: grid; grid-template-columns: 190px 1fr; gap: 12px; align-items: start; }
.aside { position: sticky; top: 74px; }
.c-item { display: flex; justify-content: space-between; padding: 6px 9px; border-radius: 4px;
  font-size: 12.5px; cursor: pointer; color: var(--text-2); }
.c-item:hover { background: var(--paper); }
.c-item.on { background: #EDF3FB; color: var(--primary); font-weight: 600; }
.c-item.sub { padding-left: 20px; font-size: 12px; }
.c-item em { font-style: normal; color: var(--text-3); font-size: 11px; }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; min-height: 240px; }
.book { background: var(--card); border: 1px solid var(--line); border-radius: 6px; padding: 11px; cursor: pointer; }
.book:hover { border-color: var(--primary-light); box-shadow: var(--shadow); }
.book-cover { width: 100%; height: 158px; object-fit: cover; border-radius: 4px; background: var(--paper); }
.book-name { font-size: 13px; font-weight: 600; margin-top: 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.book-author { color: var(--text-3); font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.book-foot { display: flex; align-items: baseline; gap: 6px; margin-top: 6px; }
.book-foot b { color: var(--danger); font-size: 15px; }
.book-foot s { color: var(--text-3); font-size: 11px; }
.stock { margin-left: auto; color: var(--text-3); font-size: 10.5px; }
.stock.low { color: var(--danger); }
.add { width: 100%; margin-top: 8px; }
.empty { grid-column: span 4; text-align: center; color: var(--text-3); font-size: 12.5px; padding: 50px 0; }
@media (max-width: 1000px) {
  .side-wrap { grid-template-columns: 1fr; }
  .grid { grid-template-columns: repeat(2, 1fr); }
  .empty { grid-column: span 2; }
}
</style>
