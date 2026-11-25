<template>
  <div>
    <div class="page-head">
      <h2>菜单与字典</h2>
      <span class="sub">菜单决定侧栏层级与接口权限标识，字典用于各页面下拉选项，均无需改代码即可调整</span>
      <span class="spacer"></span>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <div class="md-wrap">
      <div class="card">
        <h3 class="card-title">菜单树 <span class="tag">共 {{ flatMenus.length }} 项</span></h3>
        <el-tree :data="menus" node-key="id" default-expand-all :expand-on-click-node="false">
          <template #default="{ data }">
            <div class="m-node">
              <span class="m-name">{{ data.menuName }}</span>
              <span v-if="data.menuType === 'M'" class="st st-blue">目录</span>
              <span v-else class="st st-gray">页面</span>
              <em v-if="data.path">{{ data.path }}</em>
              <span v-if="data.perm" class="perm">{{ data.perm }}</span>
            </div>
          </template>
        </el-tree>
      </div>

      <div class="card">
        <h3 class="card-title">数据字典 <span class="tag">{{ dictTotal }} 条</span></h3>
        <div class="filters" style="margin-bottom: 10px; border: none; padding: 0">
          <el-select v-model="q.dictType" placeholder="全部字典类型" clearable style="width: 200px" @change="loadDict">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
          <span class="spacer"></span>
          <el-tag type="info" effect="plain" size="small">只读：字典由初始化脚本维护</el-tag>
        </div>
        <el-table :data="dictRows" size="small" v-loading="loading">
          <el-table-column prop="dictType" label="类型" width="120" />
          <el-table-column prop="dictLabel" label="显示名" width="120" />
          <el-table-column prop="dictValue" label="值" width="110" />
          <el-table-column prop="sortNo" label="排序" width="66" align="right" />
          <el-table-column label="状态" width="76">
            <template #default="{ row }">
              <span class="st" :class="row.status === 1 ? 'st-green' : 'st-gray'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        </el-table>
        <div class="pager">
          <el-pagination small background layout="total, prev, pager, next" :total="dictTotal"
                         v-model:current-page="q.page" :page-size="q.size" @current-change="loadDict" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { api } from '@/api'

const menus = ref([])
const dictRows = ref([])
const dictTotal = ref(0)
const loading = ref(false)
const q = reactive({ page: 1, size: 10, dictType: '' })

const flatMenus = computed(() => {
  const out = []
  const walk = (arr) => arr.forEach((n) => { out.push(n); walk(n.children || []) })
  walk(menus.value)
  return out
})
const types = computed(() => [...new Set(dictRows.value.map((d) => d.dictType))])

async function loadDict() {
  loading.value = true
  try {
    const r = await api.dictPage({ ...q, dictType: q.dictType || undefined })
    dictRows.value = r.rows || []
    dictTotal.value = r.total || 0
  } finally {
    loading.value = false
  }
}
async function load() {
  menus.value = (await api.menuTree()) || []
  await loadDict()
}

onMounted(load)
</script>

<style scoped>
.md-wrap { display: grid; grid-template-columns: 420px 1fr; gap: 12px; align-items: start; }
.m-node { display: flex; align-items: center; gap: 7px; font-size: 12.5px; }
.m-name { font-weight: 500; }
.m-node em { font-style: normal; color: var(--text-3); font-size: 11px; font-family: ui-monospace, Menlo, monospace; }
.perm { color: var(--primary-light); font-size: 10.5px; font-family: ui-monospace, Menlo, monospace; }
@media (max-width: 1100px) { .md-wrap { grid-template-columns: 1fr; } }
</style>
