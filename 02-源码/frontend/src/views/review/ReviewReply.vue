<template>
  <div>
    <div class="page-head">
      <h2>书评回复</h2>
      <span class="sub">优先回复低分与差评，回复内容会展示在图书详情页</span>
      <span class="spacer"></span>
      <el-select v-model="q.status" size="small" style="width: 150px" @change="search">
        <el-option label="已展示（全部）" value="已展示" />
        <el-option label="待审核" value="待审核" />
        <el-option label="已隐藏" value="已隐藏" />
      </el-select>
      <el-button size="small" @click="onlyLow = !onlyLow">
        {{ onlyLow ? '显示全部' : '只看低分（≤3）' }}
      </el-button>
    </div>

    <div class="rep-wrap">
      <div class="card">
        <h3 class="card-title">书评列表 <span class="tag">共 {{ total }} 条</span></h3>
        <div v-for="r in list" :key="r.id" class="rp-item" :class="{ on: cur.id === r.id }" @click="pick(r)">
          <div class="rp-head">
            <img class="avatar" :src="r.avatar" alt="" />
            <b>{{ r.customerName }}</b>
            <el-rate :model-value="r.score" disabled size="small" />
            <span class="spacer"></span>
            <span class="st" :class="r.replyContent ? 'st-green' : 'st-amber'">
              {{ r.replyContent ? '已回复' : '待回复' }}
            </span>
          </div>
          <p class="rp-txt">{{ r.title }}｜{{ r.content }}</p>
          <div class="rp-meta">
            <span>{{ r.bookName }}</span>
            <span>{{ fmtTime(r.createTime) }}</span>
          </div>
        </div>
        <p v-if="!list.length" class="empty">暂无书评</p>
        <div class="pager">
          <el-pagination small background layout="prev, pager, next" :total="total"
                         v-model:current-page="q.page" :page-size="q.size" @current-change="load" />
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">回复编辑 <span class="tag">{{ cur.id ? `书评 ${cur.reviewNo}` : '请选择左侧书评' }}</span></h3>
        <template v-if="cur.id">
          <div class="rp-detail">
            <div class="rp-dhead">
              <img class="avatar" :src="cur.avatar" alt="" />
              <b>{{ cur.customerName }}</b>
              <el-rate :model-value="cur.score" disabled size="small" />
              <span class="st" :class="stCls(cur.status)">{{ cur.status }}</span>
            </div>
            <div class="rp-book">
              <img class="cover" :src="cur.coverImage" alt="" />
              <div>
                <div class="rp-bname">{{ cur.bookName }}</div>
                <div class="rp-bsub">{{ cur.bookNo }}</div>
              </div>
            </div>
            <p class="rp-content">{{ cur.content }}</p>
          </div>

          <el-form label-width="76px" style="margin-top: 12px">
            <el-form-item label="回复内容">
              <el-input v-model="content" type="textarea" :rows="5" maxlength="200" show-word-limit
                        placeholder="致谢 + 具体说明，避免模板化套话；涉及质量问题的请说明补发或退换方案" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="!canReply" :loading="saving" @click="submit">提交回复</el-button>
              <el-button v-if="cur.replyContent" @click="content = cur.replyContent">载入上次回复</el-button>
              <span v-if="!canReply" class="muted">当前角色无回复权限</span>
            </el-form-item>
          </el-form>

          <div v-if="cur.replyContent" class="last-reply">
            <div class="lr-head">历史回复 · {{ fmtTime(cur.replyTime) }}</div>
            <p>{{ cur.replyContent }}</p>
          </div>

          <el-alert type="info" :closable="false" show-icon style="margin-top: 12px"
                    title="回复会写入操作日志；同一书评可多次回复，以最后一次为准" />
        </template>
        <p v-else class="empty">请选择左侧书评</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime } from '@/utils/format'

const list = ref([])
const total = ref(0)
const cur = ref({})
const content = ref('')
const saving = ref(false)
const onlyLow = ref(false)
const all = ref([])
const q = reactive({ page: 1, size: 10, status: '已展示' })

const canReply = computed(() => ['ADMIN', 'OPS', 'CLERK'].includes(store.user.roleCode))
const stCls = (s) => ({ 待审核: 'st-amber', 已展示: 'st-green', 已隐藏: 'st-gray', 已屏蔽: 'st-red' }[s] || 'st-gray')

watch(onlyLow, () => {
  const base = all.value
  list.value = onlyLow.value ? base.filter((x) => x.score <= 3) : base
  total.value = list.value.length
})

async function load() {
  const r = await api.reviewPage({ ...q, status: q.status || undefined })
  all.value = r.rows || []
  total.value = r.total || 0
  list.value = onlyLow.value ? all.value.filter((x) => x.score <= 3) : all.value
  if (list.value.length && !list.value.some((x) => x.id === cur.value.id)) pick(list.value[0])
}
function search() {
  q.page = 1
  load()
}
async function pick(r) {
  cur.value = r
  content.value = r.replyContent || ''
}
async function submit() {
  if (!content.value || content.value.trim().length < 8) {
    ElMessage.warning('回复内容不少于 8 个字')
    return
  }
  saving.value = true
  try {
    await api.reviewReply(cur.value.id, { content: content.value.trim() })
    ElMessage.success('回复已提交')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.rep-wrap { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; align-items: start; }
.rp-item { border: 1px solid var(--line); border-radius: 5px; padding: 9px 10px; margin-bottom: 8px; cursor: pointer; }
.rp-item:hover { border-color: var(--primary-light); }
.rp-item.on { border-color: var(--primary); background: #F4F8FD; }
.rp-head { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.rp-head .spacer { flex: 1; }
.rp-txt { color: var(--text-2); font-size: 12px; line-height: 1.8; margin: 6px 0 0;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.rp-meta { display: flex; justify-content: space-between; color: var(--text-3); font-size: 11px; margin-top: 6px; }
.rp-detail { background: var(--paper); border-radius: 5px; padding: 11px; }
.rp-dhead { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.rp-book { display: flex; gap: 9px; align-items: center; margin-top: 9px; }
.rp-bname { font-size: 12.5px; font-weight: 600; }
.rp-bsub { color: var(--text-3); font-size: 11px; }
.rp-content { color: var(--text-2); font-size: 12.5px; line-height: 1.9; margin: 9px 0 0; }
.last-reply { background: #F2F6FC; border-left: 3px solid var(--primary); padding: 8px 11px; border-radius: 3px; margin-top: 10px; }
.lr-head { color: var(--primary); font-size: 11.5px; margin-bottom: 4px; }
.last-reply p { margin: 0; color: var(--text-2); font-size: 12px; line-height: 1.8; }
.empty { color: var(--text-3); font-size: 12px; text-align: center; padding: 30px 0; }
.muted { color: var(--text-3); font-size: 11.5px; margin-left: 8px; }
@media (max-width: 1100px) { .rep-wrap { grid-template-columns: 1fr; } }
</style>
