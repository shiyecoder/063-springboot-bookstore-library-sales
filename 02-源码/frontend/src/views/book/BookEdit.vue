<template>
  <div>
    <div class="page-head">
      <h2>{{ isEdit ? '编辑图书' : '新增图书' }}</h2>
      <span class="sub">{{ isEdit ? `图书编号 ${form.bookNo}` : '带 * 为必填项，售价规则由后端二次校验' }}</span>
      <span class="spacer"></span>
      <el-button size="small" @click="$router.back()">返回</el-button>
      <el-button type="primary" size="small" :loading="saving" @click="save">保存</el-button>
    </div>

    <div class="edit-wrap">
      <div class="card">
        <h3 class="card-title">基础信息</h3>
        <el-form ref="f" :model="form" :rules="rules" label-width="96px" size="default">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="书名" prop="bookName">
                <el-input v-model="form.bookName" placeholder="请输入书名" maxlength="60" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="副标题">
                <el-input v-model="form.subTitle" placeholder="选填" maxlength="80" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="作者" prop="author">
                <el-input v-model="form.author" placeholder="请输入作者" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="出版社" prop="publisher">
                <el-select v-model="form.publisher" filterable allow-create placeholder="选择或输入" style="width: 100%">
                  <el-option v-for="p in opts.publishers" :key="p.name" :label="p.name" :value="p.name" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="ISBN" prop="isbn">
                <el-input v-model="form.isbn" placeholder="13 位数字" maxlength="13" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="分类" prop="categoryId">
                <el-select v-model="form.categoryId" placeholder="请选择二级分类" style="width: 100%">
                  <el-option-group v-for="c in opts.categories" :key="c.id" :label="c.categoryName">
                    <el-option v-for="s in c.children || []" :key="s.id" :label="s.categoryName" :value="s.id" />
                  </el-option-group>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="定价" prop="listPrice">
                <el-input-number v-model="form.listPrice" :min="1" :precision="2" :step="1" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="售价" prop="salePrice">
                <el-input-number v-model="form.salePrice" :min="0.01" :precision="2" :step="1" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="折扣">
                <el-input :model-value="discountText" readonly />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="出版日期">
                <el-date-picker v-model="form.publishDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="版次">
                <el-input v-model="form.edition" placeholder="第 1 版" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="上架状态">
                <el-select v-model="form.status" style="width: 100%">
                  <el-option label="在售" value="在售" />
                  <el-option label="待上架" value="待上架" />
                  <el-option label="停售" value="停售" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="内容简介">
                <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="300" show-word-limit
                          placeholder="一句话说明这本书讲什么，用于前台图书详情页" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <el-alert v-if="priceWarn" :title="priceWarn" :type="priceWarnType" show-icon :closable="false" style="margin-bottom: 8px" />
      </div>

      <div class="side">
        <div class="card">
          <h3 class="card-title">封面</h3>
          <div class="cover-box">
            <img v-if="form.coverImage" :src="form.coverImage" class="cover-lg" alt="封面" />
            <div v-else class="cover-empty">暂无封面</div>
          </div>
          <el-select v-model="form.coverImage" placeholder="从素材库选择" size="small" style="width: 100%; margin-top: 10px">
            <el-option v-for="n in 8" :key="n" :label="`封面素材 ${String(n).padStart(2, '0')}`"
                       :value="`/uploads/seed/book/${String(n).padStart(2, '0')}.jpg`" />
          </el-select>
          <p class="hint">演示环境使用 uploads/seed/book 下的本地实拍图，正式环境走文件上传接口。</p>
        </div>

        <div class="card" v-if="isEdit">
          <h3 class="card-title">库存概览</h3>
          <div class="mini">
            <div><span>当前库存</span><b>{{ detail.stockQty ?? '—' }}</b></div>
            <div><span>锁定数量</span><b>{{ detail.lockedQty ?? '—' }}</b></div>
            <div><span>累计销量</span><b>{{ detail.totalSales ?? '—' }}</b></div>
            <div><span>缺货次数</span><b>{{ detail.shortageCount ?? '—' }}</b></div>
          </div>
          <el-button size="small" style="width: 100%; margin-top: 10px" @click="$router.push('/stock/flow')">
            查看库存流水
          </el-button>
        </div>

        <div class="card">
          <h3 class="card-title">售价规则</h3>
          <ul class="rules">
            <li>售价 ≤ 定价 × 1.2（超出直接拒绝）</li>
            <li>售价 &lt; 定价 × 70% 需运营管理员审批</li>
            <li>上架校验：分类需启用、库存 &gt; 0</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import { store } from '@/store/user'

const route = useRoute()
const router = useRouter()
const f = ref()
const saving = ref(false)
const opts = ref({})
const detail = ref({})
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  id: null,
  bookName: '',
  subTitle: '',
  author: '',
  publisher: '',
  isbn: '',
  categoryId: null,
  listPrice: 68,
  salePrice: 54.4,
  publishDate: '',
  edition: '第 1 版',
  status: '待上架',
  summary: '',
  coverImage: '/uploads/seed/book/01.jpg'
})

const rules = {
  bookName: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  publisher: [{ required: true, message: '请选择出版社', trigger: 'change' }],
  isbn: [{ pattern: /^\d{13}$/, message: 'ISBN 为 13 位数字', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  listPrice: [{ required: true, message: '请输入定价', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

const discountText = computed(() => {
  if (!form.listPrice || !form.salePrice) return '—'
  return (form.salePrice / form.listPrice * 10).toFixed(1) + ' 折'
})

/** 前端预判，给出即时反馈；真正的拦截在后端 bookSave */
const priceWarn = computed(() => {
  if (!form.listPrice || !form.salePrice) return ''
  if (form.salePrice > form.listPrice * 1.2) return '售价已超过定价的 1.2 倍，保存会被拒绝，请先调整'
  if (form.salePrice < form.listPrice * 0.7) return '售价低于定价的 70%，保存后将进入运营管理员审批流程'
  return ''
})
const priceWarnType = computed(() => (form.salePrice > form.listPrice * 1.2 ? 'error' : 'warning'))

async function save() {
  const ok = await f.value.validate().catch(() => false)
  if (!ok) return
  saving.value = true
  try {
    await api.bookSave({ ...form })
    ElMessage.success(isEdit.value ? '已保存' : '新增成功')
    router.push('/book')
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

watch(() => form.categoryId, () => {})

onMounted(async () => {
  opts.value = await api.options()
  if (isEdit.value) {
    detail.value = await api.bookDetail(route.params.id)
    Object.assign(form, detail.value)
  } else if (store.user.roleCode === 'OPS') {
    form.status = '待上架'
  }
})
</script>

<style scoped>
.edit-wrap { display: grid; grid-template-columns: 1fr 292px; gap: 12px; align-items: start; }
.side { display: flex; flex-direction: column; gap: 12px; }
.cover-box { display: flex; justify-content: center; }
.cover-empty { width: 190px; height: 250px; border: 1px dashed var(--line); border-radius: 4px;
  display: flex; align-items: center; justify-content: center; color: var(--text-3); font-size: 12px; background: var(--paper); }
.hint { color: var(--text-3); font-size: 11px; line-height: 1.7; margin: 8px 0 0; }
.mini { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.mini div { background: var(--paper); border-radius: 4px; padding: 7px 9px; display: flex; flex-direction: column; }
.mini span { color: var(--text-3); font-size: 11px; }
.mini b { font-size: 15px; color: var(--primary); }
.rules { margin: 0; padding-left: 16px; color: var(--text-2); font-size: 11.5px; line-height: 2; }
@media (max-width: 1100px) { .edit-wrap { grid-template-columns: 1fr; } }
</style>
