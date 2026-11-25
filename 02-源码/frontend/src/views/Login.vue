<template>
  <div class="login">
    <!-- 左侧品牌区：背景为 uploads/seed/bg 实拍纸感书架图，避免纯色大块 -->
    <section class="brand-side">
      <div class="brand-bg" :style="{ backgroundImage: `url(${bg})` }"></div>
      <div class="brand-veil"></div>
      <div class="brand-content">
        <div class="logo">
          <span class="mark">书</span>
          <span class="name">拾页书店</span>
        </div>
        <h1>从选书到补货，<br />一条链路管好一家书店</h1>
        <p class="slogan">
          图书、库存、订单、书评、退换与智能补货，六个角色在同一套系统里协作。
        </p>
        <ul class="points">
          <li><b>库存账实一致</b>入库质检、盘点差异、流水逐条可追溯</li>
          <li><b>订单全流程</b>待支付到已完成，超时自动取消并回补库存</li>
          <li><b>补货五因子</b>销量、周转、在途、季节、缺货加权打分</li>
        </ul>
        <div class="quote">
          “一本书卖得动，靠的不是运气，是看得见的数据。”
        </div>
      </div>
    </section>

    <!-- 右侧表单区 -->
    <section class="form-side">
      <div class="form-box">
        <div class="tabbar">
          <span :class="{ on: tab === 'login' }" @click="tab = 'login'">账号登录</span>
          <span :class="{ on: tab === 'reg' }" @click="tab = 'reg'">读者注册</span>
        </div>

        <template v-if="tab === 'login'">
          <el-form ref="lf" :model="form" :rules="rules" label-position="top" @keyup.enter="doLogin">
            <el-form-item label="账号" prop="username">
              <el-input v-model="form.username" size="large" placeholder="请输入账号" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" size="large" type="password" show-password
                        placeholder="请输入密码" :prefix-icon="Lock" />
            </el-form-item>
            <div class="row">
              <el-checkbox v-model="remember">记住账号</el-checkbox>
              <span class="link" @click="tip">忘记密码？</span>
            </div>
            <el-button class="submit" type="primary" size="large" :loading="loading" @click="doLogin">
              登 录
            </el-button>
          </el-form>

          <div class="demo">
            <div class="demo-head">
              <span>演示账号（密码均为 123456）</span>
              <span class="link" @click="showAll = !showAll">{{ showAll ? '收起' : '展开全部' }}</span>
            </div>
            <div class="demo-grid">
              <div v-for="d in (showAll ? accounts : accounts.slice(0, 3))" :key="d.u"
                   class="demo-item" @click="fill(d)">
                <b>{{ d.r }}</b>
                <span>{{ d.u }}</span>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <el-form ref="rf" :model="reg" :rules="regRules" label-position="top" @keyup.enter="doRegister">
            <el-form-item label="账号" prop="username">
              <el-input v-model="reg.username" size="large" placeholder="3-20 位字母或数字" clearable />
            </el-form-item>
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="reg.realName" size="large" placeholder="请输入真实姓名" clearable />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="reg.phone" size="large" placeholder="用于接收发货与售后通知" clearable />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="reg.password" size="large" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
            <el-button class="submit" type="primary" size="large" :loading="loading" @click="doRegister">
              注册并登录
            </el-button>
            <p class="agree">注册即表示同意《用户服务协议》与《隐私政策》，仅开放读者角色。</p>
          </el-form>
        </template>
      </div>
      <div class="copyright">网上书店与图书销售系统 · 毕业设计演示环境 v1.0</div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { api } from '@/api'
import { setLogin } from '@/store/user'

const route = useRoute()
const router = useRouter()
const bg = '/uploads/seed/bg/01.jpg'

const tab = ref('login')
const loading = ref(false)
const remember = ref(true)
const showAll = ref(false)
const lf = ref()
const rf = ref()

const form = reactive({ username: localStorage.getItem('last_user') || '', password: '' })
const reg = reactive({ username: '', realName: '', phone: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const regRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度 3-20 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

// 六个角色各留一个可登录账号，演示时不必记账号表
const accounts = [
  { r: '系统管理员', u: 'admin' },
  { r: '运营管理员', u: 'ops01' },
  { r: '书店店员', u: 'clerk01' },
  { r: '库管员', u: 'keeper01' },
  { r: '采购员', u: 'buyer01' },
  { r: '读者', u: 'reader01' }
]

function fill(d) {
  form.username = d.u
  form.password = '123456'
}

function tip() {
  ElMessage.info('演示环境不提供找回密码，请联系系统管理员重置为 123456')
}

function afterLogin(data) {
  setLogin(data)
  if (remember.value) localStorage.setItem('last_user', data.user.username)
  else localStorage.removeItem('last_user')
  ElMessage.success(`欢迎回来，${data.user.realName || data.user.username}`)
  const target = route.query.redirect
  const home = data.user.roleCode === 'READER' ? '/portal' : '/dashboard'
  router.replace(target || home)
}

async function doLogin() {
  const ok = await lf.value.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    afterLogin(await api.login({ username: form.username.trim(), password: form.password }))
  } catch (e) {
    /* 统一拦截器已提示 */
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  const ok = await rf.value.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    afterLogin(await api.register({ ...reg, roleCode: 'READER' }))
  } catch (e) {
    /* ignore */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login { display: flex; height: 100vh; background: var(--paper); }

.brand-side { position: relative; flex: 1.15; overflow: hidden; background: #22314A; }
.brand-bg { position: absolute; inset: 0; background-size: cover; background-position: center; transform: scale(1.04); }
.brand-veil {
  position: absolute; inset: 0;
  background: linear-gradient(120deg, rgba(34, 49, 74, .93) 0%, rgba(44, 69, 109, .86) 58%, rgba(44, 69, 109, .55) 100%);
}
.brand-content { position: relative; height: 100%; padding: 46px 54px; color: #F2F5F9; display: flex; flex-direction: column; }
.logo { display: flex; align-items: center; gap: 10px; margin-bottom: 52px; }
.logo .mark { width: 30px; height: 30px; border-radius: 6px; background: var(--accent); color: #2C456D;
  display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 16px; }
.logo .name { font-size: 17px; font-weight: 600; letter-spacing: 1px; }
.brand-content h1 { font-size: 32px; line-height: 1.45; margin: 0 0 16px; font-weight: 600; letter-spacing: .5px; }
.slogan { color: rgba(242, 245, 249, .72); font-size: 13.5px; line-height: 1.9; margin: 0 0 30px; max-width: 430px; }
.points { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 12px; }
.points li { font-size: 12.5px; color: rgba(242, 245, 249, .68); padding-left: 14px; position: relative; line-height: 1.7; }
.points li::before { content: ''; position: absolute; left: 0; top: 7px; width: 5px; height: 5px;
  border-radius: 50%; background: var(--accent); }
.points li b { color: #fff; font-weight: 600; margin-right: 8px; }
.quote { margin-top: auto; font-size: 12px; color: rgba(242, 245, 249, .5); border-left: 2px solid rgba(201, 162, 39, .6);
  padding-left: 12px; line-height: 1.8; }

.form-side { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 24px; }
.form-box { width: 100%; max-width: 372px; }
.tabbar { display: flex; gap: 24px; margin-bottom: 22px; border-bottom: 1px solid var(--line); }
.tabbar span { padding: 0 2px 10px; font-size: 14.5px; color: var(--text-3); cursor: pointer; border-bottom: 2px solid transparent; }
.tabbar span.on { color: var(--primary); font-weight: 600; border-bottom-color: var(--primary); }
.row { display: flex; align-items: center; justify-content: space-between; margin: -2px 0 16px; }
.link { color: var(--primary); font-size: 12px; cursor: pointer; }
.link:hover { text-decoration: underline; }
.submit { width: 100%; letter-spacing: 4px; }
.agree { color: var(--text-3); font-size: 11.5px; line-height: 1.7; margin: 12px 0 0; }

.demo { margin-top: 22px; border-top: 1px dashed var(--line); padding-top: 14px; }
.demo-head { display: flex; justify-content: space-between; font-size: 11.5px; color: var(--text-3); margin-bottom: 8px; }
.demo-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
.demo-item {
  border: 1px solid var(--line); border-radius: 4px; padding: 6px 8px; cursor: pointer;
  background: #fff; display: flex; flex-direction: column; gap: 1px; line-height: 1.5;
}
.demo-item:hover { border-color: var(--primary-light); background: #F6F9FD; }
.demo-item b { font-size: 11.5px; font-weight: 600; color: var(--text); }
.demo-item span { font-size: 11px; color: var(--text-3); font-family: ui-monospace, Menlo, monospace; }
.copyright { margin-top: 28px; font-size: 11px; color: var(--text-3); }

@media (max-width: 900px) {
  .brand-side { display: none; }
}
</style>
