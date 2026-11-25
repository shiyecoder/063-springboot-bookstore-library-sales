<template>
  <div>
    <div class="page-head">
      <h2>个人中心</h2>
      <span class="sub">{{ me.roleName }} · 账号 {{ me.username }}</span>
      <span class="spacer"></span>
      <el-button size="small" @click="load">重新加载</el-button>
    </div>

    <div class="prof">
      <div class="card">
        <div class="head">
          <img class="avatar-lg" :src="me.avatar || '/uploads/seed/avatar/01.jpg'" alt="头像" />
          <div class="head-txt">
            <h3>{{ me.realName }}</h3>
            <p class="sub">{{ me.roleName }} · {{ me.orgName || '拾页书店' }}</p>
            <div class="chips">
              <span class="st st-blue">{{ me.username }}</span>
              <span class="st st-gray">数据范围 {{ scopeText(me.dataScope) }}</span>
              <span v-if="me.creditScore" class="st st-green">信用分 {{ me.creditScore }}</span>
            </div>
          </div>
        </div>

        <div class="stats">
          <div><span>待收货</span><b>{{ stats.shipping }}</b></div>
          <div><span>已完成</span><b>{{ stats.done }}</b></div>
          <div><span>我的书评</span><b>{{ stats.reviews }}</b></div>
          <div><span>未读消息</span><b>{{ stats.unread }}</b></div>
        </div>

        <h3 class="card-title" style="margin-top: 18px">资料编辑</h3>
        <el-form :model="form" label-width="82px" style="max-width: 460px">
          <el-form-item label="姓名">
            <el-input v-model="form.realName" maxlength="20" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" maxlength="11" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" maxlength="60" />
          </el-form-item>
          <el-form-item label="头像">
            <div class="avatar-pick">
              <img v-for="n in 8" :key="n" :src="`/uploads/seed/avatar/${String(n).padStart(2, '0')}.jpg`"
                   class="avatar-opt" :class="{ on: form.avatar.endsWith(`/${String(n).padStart(2, '0')}.jpg`) }"
                   @click="form.avatar = `/uploads/seed/avatar/${String(n).padStart(2, '0')}.jpg`" alt="" />
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="save">保存资料</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="col">
        <div class="card">
          <h3 class="card-title">修改密码</h3>
          <el-form ref="pf" :model="pwd" :rules="pwdRules" label-position="top">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="pwd.oldPassword" type="password" show-password placeholder="请输入当前密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwd.newPassword" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirm">
              <el-input v-model="pwd.confirm" type="password" show-password placeholder="再次输入新密码" />
            </el-form-item>
            <el-button type="primary" style="width: 100%" :loading="changing" @click="changePwd">确认修改</el-button>
          </el-form>
        </div>

        <div class="card">
          <h3 class="card-title">账号信息</h3>
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="账号">{{ me.username }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ me.roleName }}（{{ me.roleCode }}）</el-descriptions-item>
            <el-descriptions-item label="所属组织">{{ me.orgName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="数据范围">{{ scopeText(me.dataScope) }}</el-descriptions-item>
          </el-descriptions>
          <el-button size="small" style="width: 100%; margin-top: 10px" @click="$router.push('/portal/orders')">
            我的订单
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import { store, setLogin } from '@/store/user'
import { fmtTime } from '@/utils/format'

const me = ref({})
const saving = ref(false)
const changing = ref(false)
const pf = ref()
const orders = ref([])
const reviews = ref([])
const form = reactive({ realName: '', phone: '', email: '', avatar: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (r, v, cb) => (v === pwd.newPassword ? cb() : cb(new Error('两次输入不一致'))),
      trigger: 'blur'
    }
  ]
}

const scopeText = (s) => ({ ALL: '全部数据', ORG: '本组织', SELF: '仅本人' }[s] || s || '—')
const isReader = computed(() => store.user.roleCode === 'READER')
const stats = computed(() => ({
  shipping: orders.value.filter((o) => ['待发货', '已发货'].includes(o.status)).length,
  done: orders.value.filter((o) => o.status === '已完成').length,
  // 书评只对读者有「我的」语义，员工账号不统计
  reviews: isReader.value ? reviews.value.length : 0,
  unread: store.unread || 0
}))

async function load() {
  me.value = await api.me()
  Object.assign(form, {
    realName: me.value.realName || '',
    phone: me.value.phone || '',
    email: me.value.email || '',
    avatar: me.value.avatar || '/uploads/seed/avatar/01.jpg'
  })
  const [o, r] = await Promise.all([
    api.myOrders({ page: 1, size: 50 }),
    isReader.value ? api.reviewPage({ page: 1, size: 100 }) : Promise.resolve({ rows: [] })
  ])
  orders.value = o.rows || []
  reviews.value = r.rows || []
}
async function save() {
  if (!form.realName || form.realName.trim().length < 2) {
    ElMessage.warning('姓名不少于 2 个字')
    return
  }
  saving.value = true
  try {
    await api.saveProfile({ ...form })
    ElMessage.success('资料已保存')
    const u = { ...store.user, ...form }
    store.user = u
    localStorage.setItem('user', JSON.stringify(u))
    await load()
  } finally {
    saving.value = false
  }
}
async function changePwd() {
  const ok = await pf.value.validate().catch(() => false)
  if (!ok) return
  changing.value = true
  try {
    await api.changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    localStorage.removeItem('token')
    location.hash = '#/login'
  } finally {
    changing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.prof { display: grid; grid-template-columns: 1fr 316px; gap: 12px; align-items: start; }
.col { display: flex; flex-direction: column; gap: 12px; }
.head { display: flex; gap: 16px; align-items: center; padding-bottom: 14px; border-bottom: 1px dashed var(--line); }
.avatar-lg { width: 66px; height: 66px; border-radius: 50%; object-fit: cover; border: 2px solid var(--paper); }
.head-txt h3 { margin: 0 0 3px; font-size: 17px; }
.sub { color: var(--text-3); font-size: 12px; margin: 0 0 7px; }
.chips { display: flex; gap: 7px; flex-wrap: wrap; }
.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-top: 14px; }
.stats div { background: var(--paper); border-radius: 5px; padding: 9px 11px; }
.stats span { display: block; color: var(--text-3); font-size: 11.5px; }
.stats b { font-size: 19px; color: var(--primary); }
.avatar-pick { display: flex; gap: 7px; flex-wrap: wrap; }
.avatar-opt { width: 38px; height: 38px; border-radius: 50%; object-fit: cover; cursor: pointer;
  border: 2px solid transparent; }
.avatar-opt.on { border-color: var(--primary); }
@media (max-width: 1000px) { .prof { grid-template-columns: 1fr; } }
</style>
