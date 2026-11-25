<template>
  <div>
    <div class="page-head">
      <h2>分类维护</h2>
      <span class="sub">一级分类决定首页导航，二级分类用于图书归属，删除前需先清空子分类</span>
      <span class="spacer"></span>
      <el-button v-if="canEdit" type="primary" size="small" :icon="Plus" @click="open(null)">新增一级分类</el-button>
    </div>

    <div class="cat-wrap">
      <div class="card">
        <h3 class="card-title">分类树 <span class="tag">共 {{ tree.length }} 个一级 / {{ childCount }} 个二级</span></h3>
        <el-tree :data="tree" node-key="id" default-expand-all highlight-current
                 :expand-on-click-node="false" @node-click="pick">
          <template #default="{ data }">
            <div class="node">
              <span class="node-name">{{ data.categoryName }}</span>
              <span class="st st-gray">{{ data.categoryCode }}</span>
              <span class="node-meta">{{ data.bookCount || 0 }} 种</span>
              <span v-if="canEdit" class="node-ops" @click.stop>
                <el-button link type="primary" size="small" @click="open(data)">编辑</el-button>
                <el-button link type="primary" size="small" @click="open(data, true)">加子类</el-button>
                <el-button link type="danger" size="small" @click="del(data)">删除</el-button>
              </span>
            </div>
          </template>
        </el-tree>
      </div>

      <div class="card">
        <h3 class="card-title">{{ cur.categoryName || '全部分类' }} <span class="tag">{{ cur.id ? '二级分类明细' : '点击左侧节点查看' }}</span></h3>
        <el-table :data="list" size="small" v-loading="loading">
          <el-table-column prop="categoryName" label="分类名称" min-width="120" />
          <el-table-column prop="categoryCode" label="编码" width="90" />
          <el-table-column prop="bookCount" label="图书数" width="80" align="right" />
          <el-table-column prop="sortNo" label="排序" width="64" align="right" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <span class="st" :class="row.status === 1 ? 'st-green' : 'st-gray'">{{ row.status === 1 ? '启用' : '停用' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="描述" min-width="180" prop="description" show-overflow-tooltip />
          <el-table-column v-if="canEdit" label="操作" width="110">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="open(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="del(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <p v-if="!list.length" class="empty">当前节点暂无数据，可点击左侧一级分类查看其下二级分类</p>
      </div>
    </div>

    <el-dialog v-model="dlg" :title="dialogTitle" width="460px">
      <el-form ref="f" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="上级分类">
          <el-input :model-value="parentName" readonly />
        </el-form-item>
        <el-form-item label="名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="如：长篇小说" maxlength="20" />
        </el-form-item>
        <el-form-item label="编码" prop="categoryCode">
          <el-input v-model="form.categoryCode" placeholder="大写字母，如 LIT" maxlength="10" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '@/api'
import { store } from '@/store/user'

const tree = ref([])
const list = ref([])
const cur = ref({})
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const f = ref()
const isChild = ref(false)

const canEdit = computed(() => ['ADMIN', 'OPS'].includes(store.user.roleCode))
const childCount = computed(() => tree.value.reduce((s, c) => s + (c.children || []).length, 0))
const dialogTitle = computed(() => (form.id ? '编辑分类' : isChild.value ? '新增二级分类' : '新增一级分类'))
const parentName = computed(() => (isChild.value ? form.parentName || '—' : '顶级分类'))

const form = reactive({ id: null, categoryName: '', categoryCode: '', parentId: 0, parentName: '', sortNo: 10, status: 1, description: '' })
const rules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  categoryCode: [
    { required: true, message: '请输入编码', trigger: 'blur' },
    { pattern: /^[A-Z0-9]{2,10}$/, message: '编码为 2-10 位大写字母或数字', trigger: 'blur' }
  ]
}

async function loadTree() {
  tree.value = await api.categoryTree()
  if (!cur.value.id && tree.value.length) pick(tree.value[0])
}
async function loadList() {
  loading.value = true
  try {
    const r = await api.categoryPage({ page: 1, size: 50 })
    const all = r.rows || []
    list.value = cur.value.id ? all.filter((x) => x.parentId === cur.value.id) : all
  } finally {
    loading.value = false
  }
}
function pick(node) {
  cur.value = node
  loadList()
}
function open(node, asChild = false) {
  isChild.value = asChild
  Object.assign(form, {
    id: node && !asChild ? node.id : null,
    categoryName: node && !asChild ? node.categoryName : '',
    categoryCode: node && !asChild ? node.categoryCode : '',
    parentId: asChild ? node.id : node ? node.parentId || 0 : 0,
    parentName: asChild ? node.categoryName : node && node.parentId ? node.parentName : '',
    sortNo: node && !asChild ? node.sortNo : 10,
    status: node && !asChild ? node.status : 1,
    description: node && !asChild ? node.description : ''
  })
  dlg.value = true
}
async function save() {
  const ok = await f.value.validate().catch(() => false)
  if (!ok) return
  saving.value = true
  try {
    await api.categorySave({ ...form })
    ElMessage.success('已保存')
    dlg.value = false
    await loadTree()
    await loadList()
  } finally {
    saving.value = false
  }
}
async function del(node) {
  await ElMessageBox.confirm(`确认删除分类「${node.categoryName}」？该分类下仍有图书时不允许删除。`, '删除确认', { type: 'warning' })
  try {
    await api.categoryDelete(node.id)
    ElMessage.success('已删除')
    await loadTree()
    await loadList()
  } catch (e) {
    /* 后端会提示「分类下仍有图书」 */
  }
}

onMounted(loadTree)
</script>

<style scoped>
.cat-wrap { display: grid; grid-template-columns: 380px 1fr; gap: 12px; align-items: start; }
.node { display: flex; align-items: center; gap: 8px; width: 100%; font-size: 12.5px; }
.node-name { font-weight: 500; }
.node-meta { color: var(--text-3); font-size: 11px; }
.node-ops { margin-left: auto; display: none; }
.node:hover .node-ops { display: block; }
.empty { color: var(--text-3); font-size: 12px; text-align: center; padding: 24px 0; }
@media (max-width: 1100px) { .cat-wrap { grid-template-columns: 1fr; } }
</style>
