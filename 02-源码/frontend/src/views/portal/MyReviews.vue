<template>
  <div>
    <div class="page-head">
      <h2>我的书评</h2>
      <span class="sub">共 {{ list.length }} 条，仅本人可见全部状态；30 天内可追评 1 次</span>
      <span class="spacer"></span>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <div v-loading="loading" class="list">
      <div v-for="r in list" :key="r.id" class="card rv">
        <div class="rv-head">
          <img class="cover" :src="r.coverImage" alt="" />
          <div class="rv-book">
            <div class="rv-name">{{ r.bookName }}</div>
            <div class="rv-sub">{{ r.bookNo }} · {{ fmtTime(r.createTime) }}</div>
          </div>
          <span class="st" :class="stCls(r.status)">{{ r.status }}</span>
          <span class="spacer"></span>
          <el-rate :model-value="r.score" disabled size="small" />
        </div>
        <div class="rv-title">{{ r.title }}</div>
        <p class="rv-txt">{{ r.content }}</p>
        <div v-if="r.imageUrls" class="rv-imgs">
          <el-image v-for="(u, i) in r.imageUrls.split(',')" :key="i" :src="u" fit="cover"
                    :preview-src-list="r.imageUrls.split(',')" :initial-index="i" preview-teleported class="rv-img" />
        </div>
        <div v-if="r.replyContent" class="reply">商家回复：{{ r.replyContent }}</div>
        <div class="rv-foot">
          <span class="muted">有用 {{ r.usefulCount }}</span>
          <span v-if="r.status === '已屏蔽'" class="muted" style="color: var(--danger)">该内容因违规被屏蔽，不对外展示</span>
          <el-button link type="primary" size="small" @click="$router.push(`/portal/book/${r.bookId}`)">查看图书</el-button>
        </div>
      </div>
      <p v-if="!list.length && !loading" class="empty">还没有写过书评，去已完成订单里挑一本评一评</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime } from '@/utils/format'

const list = ref([])
const loading = ref(false)
const stCls = (s) => ({ 待审核: 'st-amber', 已展示: 'st-green', 已隐藏: 'st-gray', 已屏蔽: 'st-red' }[s] || 'st-gray')

async function load() {
  loading.value = true
  try {
    // 后端书评分页未按用户过滤，这里取较大页再按当前登录人过滤，保证「我的书评」语义准确
    const r = await api.reviewPage({ page: 1, size: 200 })
    list.value = (r.rows || []).filter((x) => Number(x.customerId) === Number(store.user.id))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.list { display: flex; flex-direction: column; gap: 10px; }
.rv-head { display: flex; align-items: center; gap: 11px; }
.rv-book { flex: 1; min-width: 0; }
.rv-name { font-size: 13px; font-weight: 600; }
.rv-sub { color: var(--text-3); font-size: 11.5px; }
.rv-head .spacer { flex: 1; }
.rv-title { font-weight: 600; font-size: 13px; margin: 10px 0 4px; }
.rv-txt { color: var(--text-2); font-size: 12.5px; line-height: 1.9; margin: 0; }
.rv-imgs { display: flex; gap: 6px; margin-top: 8px; }
.rv-img { width: 78px; height: 58px; border-radius: 3px; border: 1px solid var(--line); }
.reply { background: #F2F6FC; border-left: 3px solid var(--primary); padding: 7px 10px; border-radius: 3px;
  color: var(--text-2); font-size: 12px; margin-top: 9px; line-height: 1.8; }
.rv-foot { display: flex; align-items: center; gap: 14px; margin-top: 9px; }
.muted { color: var(--text-3); font-size: 11.5px; }
.empty { text-align: center; color: var(--text-3); font-size: 12.5px; padding: 46px 0; }
</style>
