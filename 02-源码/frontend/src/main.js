import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/theme.css'
import { fmtTime, fmtDate, fmtNum, fmtGap, statusTag } from './utils/format'

const app = createApp(App)

// 图标全量注册，页面里直接 <el-icon><Edit /></el-icon>
for (const [key, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, comp)
}

app.use(ElementPlus, { locale: zhCn, size: 'default' })
app.use(router)

// 全局格式化：模板里可直接 {{ $fmt.time(row.createTime) }}，禁止各页面自行拼时间串
app.config.globalProperties.$fmt = {
  time: fmtTime,
  date: fmtDate,
  num: fmtNum,
  gap: fmtGap,
  tag: statusTag
}

app.mount('#app')
