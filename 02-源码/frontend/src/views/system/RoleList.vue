<template>
  <div>
    <div class="page-head">
      <h2>角色与菜单</h2>
      <span class="sub">勾选结果直接决定该角色登录后可见的侧栏与可调用的接口</span>
      <span class="spacer"></span>
      <el-button v-if="canEdit" type="primary" size="small" :icon="Plus" @click="openRole(null)">新增角色</el-button>
    </div>

    <div class="role-wrap">
      <div class="card">
        <h3 class="card-title">角色列表 <span class="tag">共 {{ total }} 个</span></h3>
        <el-table :data="rows" size="small" v-loading="loading" highlight-current-row @row-click="pick">
          <el-table-column label="角色" min-width="150">
            <template #default="{ row }">
              <div class="r-name">{{ row.roleName }}</div>
              <div class="r-code">{{ row.roleCode }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="userCount" label="用户数" width="76" align="right" />
          <el-table-column label="数据范围" width="94">
            <template #default="{ row }">
              <span class="st st-gray">{{ scopeText(row.dataScope) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="72">
            <template #default="{ row }">
              <span class="st" :class="row.status === 1 ? 'st-green' : 'st-gray'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column v-if="canEdit" label="操作" width="72">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click.stop="openRole(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination small background layout="prev, pager, next" :total="total"
                         v-model:current-page="q.page" :page-size="q.size" @current-change="load" />
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">
          菜单授权 <span class="tag">{{ cur.roleName || '请选择左侧角色' }}</span>
          <span class="spacer" style="flex: 1"></span>
          <el-button v-if="canEdit && cur.id" size="small" type="primary" :loading="saving" @click="saveMenus">
            保存授权
          </el-button>
        </h3>
        <div class="tree-tools">
          <el-checkbox v-model="expandAll" @change="toggleExpand">展开全部</el-checkbox>
          <el-checkbox v-model="checkAll" @change="toggleCheck">全选</el-checkbox>
          <span class="muted">已选 {{ checked.length }} 项</span>
        </div>
        <el-tree ref="treeRef" :data="menus" node-key="id" show-checkbox :default-expand-all="expandAll"
                 :props="{ label: 'menuName', children: 'children' }" :check-strictly="false"
                 @check="onCheck">
          <template #default="{ data }">
            <span class="tn">
              {{ data.menuName }}
              <em v-if="data.path">{{ data.path }}</em>
              <span v-if="data.perm" class="st st-gray">{{ data.perm }}</span>
            </span>
          </template>
        </el-tree>
        <el-alert type="info" :closable="false" show-icon style="margin-top: 10px"
                  title="父节点与子节点联动勾选；取消父节点会同时取消其下全部子页面" />
      </div>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑角色' : '新增角色'" width="440px">
      <el-form ref="f" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="!!form.id" placeholder="大写，如 CLERK" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如 书店店员" maxlength="20" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="全部数据" value="ALL" />
            <el-option label="本组织" value="ORG" />
            <el-option label="仅本人" value="SELF" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="80" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="savingRole" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '@/api'
import { store } from '@/store/user'

const rows = ref([])
const total = ref(0)
const menus = ref([])
const checked = ref([])
const cur = ref({})
const loading = ref(false)
const saving = ref(false)
const savingRole = ref(false)
const dlg = ref(false)
const expandAll = ref(true)
const checkAll = ref(false)
const treeRef = ref()
const f = ref()
const q = reactive({ page: 1, size: 10 })

const canEdit = computed(() => store.user.roleCode === 'ADMIN')
const form = reactive({ id: null, roleCode: '', roleName: '', dataScope: 'SELF', sortNo: 10, status: 1, remark: '' })
const rules = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z_]{2,20}$/, message: '编码为 2-20 位大写字母或下划线', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const scopeText = (s) => ({ ALL: '全部', ORG: '本组织', SELF: '仅本人' }[s] || s || '—')

async function load() {
  loading.value = true
  try {
    const r = await api.rolePage(q)
    rows.value = r.rows || []
    total.value = r.total || 0
    if (!cur.value.id && rows.value.length) pick(rows.value[0])
  } finally {
    loading.value = false
  }
}
async function pick(row) {
  cur.value = row
  const ids = (await api.roleMenus(row.id)) || []
  checked.value = ids
  await nextTick()
  if (treeRef.value) {
    treeRef.value.setCheckedKeys([])
    // 只回填叶子，父节点由组件自动推导，避免父子都被勾选导致半选状态异常
    const leaf = ids.filter((id) => !hasChild(id))
    treeRef.value.setCheckedKeys(leaf)
    checked.value = treeRef.value.getCheckedKeys()
  }
}
function hasChild(id) {
  const walk = (ns) => {
    for (const n of ns) {
      if (n.id === id) return (n.children || []).length > 0
      if (walk(n.children || [])) return true
    }
    return false
  }
  return walk(menus.value)
}
function onCheck() {
  checked.value = treeRef.value.getCheckedKeys()
  checkAll.value = checked.value.length === flatten(menus.value).length
}
function flatten(ns) {
  const out = []
  const walk = (arr) => arr.forEach((n) => { out.push(n.id); walk(n.children || []) })
  walk(ns)
  return out
}
function toggleExpand(v) {
  const nodes = treeRef.value.store._getAllNodes()
  nodes.forEach((n) => (n.expanded = v))
}
function toggleCheck(v) {
  if (!treeRef.value) return
  treeRef.value.setCheckedKeys(v ? flatten(menus.value) : [])
  checked.value = treeRef.value.getCheckedKeys()
}
function openRole(row) {
  Object.assign(form, row
    ? { id: row.id, roleCode: row.roleCode, roleName: row.roleName, dataScope: row.dataScope, sortNo: row.sortNo, status: row.status, remark: row.remark }
    : { id: null, roleCode: '', roleName: '', dataScope: 'SELF', sortNo: 10, status: 1, remark: '' })
  dlg.value = true
}
async function saveRole() {
  const ok = await f.value.validate().catch(() => false)
  if (!ok) return
  savingRole.value = true
  try {
    await api.roleSave({ ...form })
    ElMessage.success('已保存')
    dlg.value = false
    load()
  } finally {
    savingRole.value = false
  }
}
async function saveMenus() {
  saving.value = true
  try {
    await api.roleSaveMenus(cur.value.id, checked.value)
    ElMessage.success(`已为「${cur.value.roleName}」保存 ${checked.value.length} 项菜单权限`)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  menus.value = (await api.menuTree()) || []
  await load()
})
</script>

<style scoped>
.role-wrap { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; align-items: start; }
.r-name { font-size: 12.5px; font-weight: 600; }
.r-code { color: var(--text-3); font-size: 11px; font-family: ui-monospace, Menlo, monospace; }
.tree-tools { display: flex; align-items: center; gap: 16px; padding: 4px 0 8px; border-bottom: 1px dashed var(--line); margin-bottom: 8px; }
.tn { display: flex; align-items: center; gap: 8px; font-size: 12.5px; }
.tn em { font-style: normal; color: var(--text-3); font-size: 11px; font-family: ui-monospace, Menlo, monospace; }
.muted { color: var(--text-3); font-size: 11.5px; }
@media (max-width: 1100px) { .role-wrap { grid-template-columns: 1fr; } }
</style>
