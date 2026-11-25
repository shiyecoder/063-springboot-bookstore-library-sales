<template>
  <div>
    <div class="page-head">
      <h2>用户管理</h2>
      <span class="sub">共 {{ total }} 个账号，覆盖读者、店员、库管、采购、运营与管理员六类角色</span>
      <span class="spacer"></span>
      <el-button v-if="canEdit" type="primary" size="small" :icon="Plus" @click="open(null)">新增用户</el-button>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="账号 / 姓名 / 手机号" clearable style="width: 220px" @keyup.enter="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="q.roleId" placeholder="全部角色" clearable @change="search">
        <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
      </el-select>
      <el-select v-model="q.status" placeholder="全部状态" clearable @change="search">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <span class="spacer"></span>
      <el-button type="primary" size="small" @click="search">查询</el-button>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="u">
              <img class="avatar" :src="row.avatar || '/uploads/seed/avatar/03.jpg'" alt="" />
              <div>
                <div class="u-name">{{ row.realName }}</div>
                <div class="u-sub">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="112">
          <template #default="{ row }">
            <span class="st" :class="roleCls(row.roleCode)">{{ row.roleName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orgName" label="所属组织" width="130" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <span class="st" :class="row.status === 1 ? 'st-green' : 'st-gray'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="140">
          <template #default="{ row }">{{ fmtTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="open(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="reset(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="q.page" :page-size="q.size" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑用户' : '新增用户'" width="480px">
      <el-form ref="f" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="`${r.roleName}（${r.roleCode}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="选填" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码">
          <el-input v-model="form.password" placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" />
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
import { Plus, Search } from '@element-plus/icons-vue'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const roles = ref([])
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const f = ref()
const q = reactive({ page: 1, size: 10, keyword: '', roleId: null, status: null })
const form = reactive({ id: null, username: '', realName: '', roleId: null, phone: '', email: '', password: '', status: 1 })

const canEdit = computed(() => store.user.roleCode === 'ADMIN')
const roleCls = (c) => ({ ADMIN: 'st-red', OPS: 'st-blue', BUYER: 'st-green', KEEPER: 'st-amber', CLERK: 'st-amber', READER: 'st-gray' }[c] || 'st-gray')

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    const r = await api.userPage({ ...q, keyword: q.keyword || undefined })
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
function open(row) {
  Object.assign(form, row ? { ...row, password: '' } : { id: null, username: '', realName: '', roleId: null, phone: '', email: '', password: '', status: 1 })
  dlg.value = true
}
async function save() {
  const ok = await f.value.validate().catch(() => false)
  if (!ok) return
  saving.value = true
  try {
    await api.userSave({ ...form })
    ElMessage.success('已保存')
    dlg.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function reset(row) {
  await ElMessageBox.confirm(`将「${row.realName}」的密码重置为 123456？`, '重置密码', { type: 'warning' })
  await api.resetPwd(row.id)
  ElMessage.success('已重置为 123456')
}

onMounted(async () => {
  roles.value = (await api.roleList()) || []
  load()
})
</script>

<style scoped>
.u { display: flex; gap: 9px; align-items: center; }
.u-name { font-size: 12.5px; font-weight: 500; }
.u-sub { color: var(--text-3); font-size: 11px; font-family: ui-monospace, Menlo, monospace; }
</style>
