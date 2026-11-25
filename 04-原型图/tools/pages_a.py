#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""页面组 A：登录 / 运营工作台 / 图书 / 分类 / 入库"""
from base import *


# ---------------------------------------------------------------- 01 登录
def p_login():
    body = '''
<div class="login-wrap">
  <div class="login-left">
    <div class="logo"><span class="mk">书</span><span>纸间书店 · 图书销售管理平台</span></div>
    <h2>让每一本书<br>都在对的时间被翻开</h2>
    <p>从入库质检到拣货发货、书评晒单与退换验收，一本图书的全链路都留在系统里；
       补货不再凭经验，按近 30 天销量与周转天数自动给出建议量。</p>
    <div class="chain">
      <div class="row2"><span class="dot">01</span><span style="width:72px">前台检索</span><span class="bar2"></span><span class="lbl">关键词 / 分类 / 价格区间</span></div>
      <div class="row2"><span class="dot">02</span><span style="width:72px">加购下单</span><span class="bar2"></span><span class="lbl">满 99 免运费，锁库存</span></div>
      <div class="row2"><span class="dot">03</span><span style="width:72px">入库质检</span><span class="bar2"></span><span class="lbl">合格才写库存与流水</span></div>
      <div class="row2"><span class="dot">04</span><span style="width:72px">拣货发货</span><span class="bar2"></span><span class="lbl">手工登记物流单号</span></div>
      <div class="row2"><span class="dot">05</span><span style="width:72px">签收书评</span><span class="bar2"></span><span class="lbl">1–5 星 + 晒图 9 张</span></div>
      <div class="row2"><span class="dot">06</span><span style="width:72px">销量分级</span><span class="bar2"></span><span class="lbl">畅销 / 平销 / 滞销</span></div>
    </div>
    <div style="margin-top:auto;padding-top:26px;max-width:432px">
      <div style="border-left:2px solid #C9A227;padding:2px 0 2px 14px">
        <div style="font-size:13px;line-height:24px;color:#E4EBF5;font-family:'Songti SC',Georgia,serif">
          「书店不是卖书的地方，是把人带到书前面的地方。」</div>
        <div style="font-size:11.5px;color:#8FA5C4;margin-top:6px">今日经营简报 · 11 月 2 日</div>
      </div>
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-top:16px">
        <div style="background:rgba(255,255,255,.07);border:1px solid rgba(255,255,255,.12);border-radius:4px;padding:9px 12px">
          <div style="font-size:17px;font-weight:600">46</div><div style="font-size:11px;color:#9CAFCA">待发货订单</div></div>
        <div style="background:rgba(255,255,255,.07);border:1px solid rgba(255,255,255,.12);border-radius:4px;padding:9px 12px">
          <div style="font-size:17px;font-weight:600">3</div><div style="font-size:11px;color:#9CAFCA">48h 物流异常</div></div>
        <div style="background:rgba(255,255,255,.07);border:1px solid rgba(255,255,255,.12);border-radius:4px;padding:9px 12px">
          <div style="font-size:17px;font-weight:600;color:#E8C86A">12</div><div style="font-size:11px;color:#9CAFCA">待处理补货建议</div></div>
      </div>
    </div>
    <div class="stats" style="margin-top:20px">
      <div><b>1,286</b><span>在售品种</span></div>
      <div><b>128</b><span>今日订单</span></div>
      <div><b>8,412</b><span>注册读者</span></div>
      <div><b>96.4%</b><span>48h 发货率</span></div>
    </div>
    <svg class="shelf-deco" viewBox="0 0 196 150" fill="none">
      <path d="M4 128h188M4 74h188" stroke="#7E9AC0" stroke-width="1.4"/>
      <rect x="16" y="88" width="13" height="40" rx="1.5" fill="#C9A227" opacity=".62"/>
      <rect x="33" y="80" width="10" height="48" rx="1.5" fill="#DCE5F2" opacity=".5"/>
      <rect x="47" y="92" width="15" height="36" rx="1.5" fill="#9DB4D2" opacity=".55"/>
      <rect x="66" y="76" width="9" height="52" rx="1.5" fill="#E3E9F2" opacity=".42"/>
      <rect x="80" y="86" width="14" height="42" rx="1.5" fill="#B9C9DE" opacity=".5"/>
      <rect x="104" y="82" width="11" height="46" rx="1.5" fill="#C9A227" opacity=".38"/>
      <rect x="120" y="90" width="16" height="38" rx="1.5" fill="#DCE5F2" opacity=".46"/>
      <rect x="143" y="78" width="10" height="50" rx="1.5" fill="#9DB4D2" opacity=".52"/>
      <rect x="158" y="88" width="13" height="40" rx="1.5" fill="#C3D2E4" opacity=".44"/>
      <rect x="20" y="42" width="12" height="32" rx="1.5" fill="#DCE5F2" opacity=".34" transform="rotate(-4 20 42)"/>
      <rect x="38" y="36" width="9" height="38" rx="1.5" fill="#C9A227" opacity=".3"/>
      <rect x="54" y="44" width="14" height="30" rx="1.5" fill="#9DB4D2" opacity=".36"/>
      <rect x="76" y="34" width="10" height="40" rx="1.5" fill="#E3E9F2" opacity=".3"/>
      <rect x="94" y="46" width="12" height="28" rx="1.5" fill="#C3D2E4" opacity=".34"/>
    </svg>
  </div>
  <div class="login-right">
    <form class="login-form" onsubmit="return false">
      <h1>登录管理后台</h1>
      <div class="tip">员工与读者共用入口，登录后按角色进入对应工作台</div>
      <div class="fi"><label>账号</label><input value="admin" autocomplete="off"></div>
      <div class="fi"><label>密码</label><input type="password" value="123456"></div>
      <div class="fi" style="display:flex;justify-content:space-between;align-items:center;font-size:12.5px;color:#5B6770">
        <label style="display:flex;align-items:center;gap:6px;margin:0"><input type="checkbox" checked style="width:14px;height:14px">记住账号</label>
        <a href="#">忘记密码</a>
      </div>
      <button class="btn primary">登 录</button>
      <div class="demo">
        <div>演示账号（密码均为 <b>123456</b>）</div>
        <div>运营 <b>operator</b>　店员 <b>clerk01</b>　库管 <b>keeper01</b></div>
        <div>采购 <b>buyer01</b>　读者 <b>user</b>　管理员 <b>admin</b></div>
      </div>
    </form>
  </div>
</div>'''
    head = 'body{background:#fff}.login-left .stats{margin-top:20px}'
    return rawpage('登录', body, head)


# ---------------------------------------------------------------- 02 运营工作台
def p_dash():
    days = ['10-03', '10-06', '10-09', '10-12', '10-15', '10-18', '10-21', '10-24', '10-27', '10-30', '11-01']
    sales = [6820, 7410, 5980, 8240, 9130, 7760, 10240, 8640, 9310, 11280, 8642]
    orders = [96, 104, 88, 118, 132, 109, 146, 121, 133, 158, 128]
    body = page_head('运营工作台', '数据截至 2026-11-02 10:30 · 默认展示最近 30 天，口径已剔除已退款订单',
                     '<button class="btn">切换本月</button><button class="btn ghost">导出报表</button>'
                     '<button class="btn primary">' + ic('i-refresh', 14) + ' 重算补货建议</button>')
    body += kpis([
        ('今日订单数', '128', '笔', '较昨日 +12', '', ''),
        ('待发货订单', '46', '笔', '其中超 24h 3 笔', 'mut', 'gold'),
        ('在售品种', '1,286', '种', '较上周 +18', '', ''),
        ('今日销售额', '8,642.50', '元', '较昨日 +8.4%', '', ''),
        ('缺货品种', '23', '种', '已开放到货提醒', 'mut', ''),
        ('平均客单价', '67.52', '元', '近 30 天口径', 'mut', ''),
        ('退换率', '2.8', '%', '环比 -0.3pt', '', ''),
        ('待补货品种', '37', '种', '畅销 12 / 平销 16 / 滞销 9', 'mut', 'gold'),
    ], 'cols8')
    body += '<div class="dash">'
    body += card('近 30 天销售额与订单量', chart_line(days, [('销售额（元）', '#3A5A8C', sales), ('订单量（笔）', '#C9A227', orders)], h=250, w=620, dual=True),
                 '双轴 · 按签收时间归属', 'span2')
    body += card('分类销售占比', chart_donut([('文学', 34, '#3A5A8C'), ('计算机', 26, '#6B8CBE'), ('历史', 16, '#C9A227'),
                                          ('社科', 14, '#94AACB'), ('少儿', 10, '#D8C48A')], '34%', '文学类', 116), '近 30 天')
    body += card('出版社销量 TOP10', chart_hbars([('人民文学', 328, '328'), ('机械工业', 296, '296'), ('中信', 254, '254'),
                                              ('南海出版', 221, '221'), ('中华书局', 168, '168'), ('三联书店', 142, '142'),
                                              ('作家出版', 128, '128'), ('邮电出版', 96, '96')], 70), '单位：册')
    body += card('下单 → 完成漏斗', chart_funnel([('下单', 1286, '100%'), ('支付', 1198, '93.2%'),
                                             ('发货', 1140, '88.6%'), ('签收', 1092, '84.9%'), ('完成', 1046, '81.3%')]), '近 30 天')
    body += card('库存周转天数分布', chart_hist([('<15', 186), ('15-30', 342), ('30-60', 418), ('60-90', 204), ('90-180', 96), ('>180', 40)]), '1,286 种在售')
    body += card('退换原因占比', chart_donut([('质量问题', 38, '#B02A37'), ('物流破损', 26, '#6B8CBE'),
                                          ('错发漏发', 18, '#C9A227'), ('不喜欢', 18, '#94AACB')], '72', '本月退换单', 108), '本月')
    body += card('补货等级与采纳情况', chart_stack([
        ('畅销 12', [('已采纳', 66, '#3A5A8C'), ('未采纳', 34, '#D5DEEA')], '66%'),
        ('平销 16', [('已采纳', 44, '#6B8CBE'), ('未采纳', 56, '#D5DEEA')], '44%'),
        ('滞销 9', [('已采纳', 22, '#C9A227'), ('未采纳', 78, '#E8E3D4')], '22%'),
    ]), '采纳率随等级下降')
    body += '</div>'
    head = '.dash{grid-auto-rows:303px}'
    return shell('ops', '运营工作台', '<b>经营概览</b> / 运营工作台', body, head)


# ---------------------------------------------------------------- 03 图书管理
BOOKS = [
    ('BK2610310001', '9787536692930', '三体（全三册）', '刘慈欣', '重庆出版社', '文学 / 小说', 128.00, 96.00, 142, '在售'),
    ('BK2610310002', '9787506365437', '活着', '余华', '作家出版社', '文学 / 小说', 45.00, 33.80, 268, '在售'),
    ('BK2610300015', '9787544253994', '百年孤独', '加西亚·马尔克斯', '南海出版公司', '文学 / 小说', 55.00, 41.00, 86, '在售'),
    ('BK2610300016', '9787508647357', '人类简史', '尤瓦尔·赫拉利', '中信出版社', '社科 / 社会学', 68.00, 51.00, 74, '在售'),
    ('BK2610290008', '9787111544937', '深入理解计算机系统', 'Randal E. Bryant', '机械工业出版社', '计算机 / 系统与架构', 139.00, 118.00, 42, '在售'),
    ('BK2610290009', '9787111407010', '算法导论（第3版）', 'Thomas H. Cormen', '机械工业出版社', '计算机 / 系统与架构', 128.00, 109.00, 0, '缺货'),
    ('BK2610280021', '9787559614626', '房思琪的初恋乐园', '林奕含', '北京联合出版公司', '文学 / 小说', 45.00, 38.00, 31, '在售'),
    ('BK2610270011', '9787020042494', '围城', '钱锺书', '人民文学出版社', '文学 / 小说', 39.00, 29.50, 96, '在售'),
]


def p_book():
    rows = []
    for no, isbn, name, au, pub, cat, lp, sp, qty, st in BOOKS:
        img = '<div style="width:34px;height:46px;border-radius:2px;border:1px solid #E2E5E8;background:#EEF2F8;display:grid;place-items:center;font-size:9px;color:#8DA1BE">封面</div>'
        rows.append([
            '<div style="display:flex;gap:10px;align-items:center">' + img
            + '<div><div style="font-weight:600">' + name + '</div>'
            + '<div style="font-size:11.5px;color:#5B6770">' + no + ' · ' + isbn + '</div></div></div>',
            au, pub, cat,
            '<span class="mono">' + ('%.2f' % lp) + '</span>',
            '<span class="mono" style="color:#B02A37;font-weight:600">' + ('%.2f' % sp) + '</span>',
            '<span class="mono">' + str(qty) + '</span>',
            stag(st),
            '<span class="nowrap"><a href="#">编辑</a> <a href="#" style="color:#5B6770">调价</a> '
            '<a href="#" style="color:#B02A37">下架</a></span>',
        ])
    body = page_head('图书管理', '共 1,286 种图书 · 售价不得高于定价 1.2 倍，低于定价 70% 需运营审批',
                     '<button class="btn">批量导入</button><button class="btn gold">' + ic('i-download', 14) + ' 导出</button>'
                     '<button class="btn primary">' + ic('i-plus', 14) + ' 新增图书</button>')
    body += toolbar(['input', 'sel:全部分类', 'sel:全部状态', 'sel:全部出版社', 'txt:出版日期'],
                    '<span style="font-size:12px;color:#5B6770">缺货 23 种</span>')
    body += '<div class="summary"><div><div class="k">在售品种</div><div class="v">1,286</div></div>' \
            '<div><div class="k">库存总量</div><div class="v">42,186 册</div></div>' \
            '<div><div class="k">库存金额（按定价）</div><div class="v">¥2,146,830</div></div>' \
            '<div><div class="k">低于 5 折品种</div><div class="v">17 种 · 待审 3</div></div></div>'
    body += '<div class="pcard" style="padding:0">' + table(
        ['图书', '作者', '出版社', '分类', '定价', '售价', '可售库存', '状态', '操作'], rows) + '</div>'
    body += pager(1286, 1, 8)
    return shell('ops', '图书管理', '<b>图书与分类</b> / 图书管理', body)


# ---------------------------------------------------------------- 04 新增图书
def p_book_new():
    body = page_head('新增图书', 'ISBN 全局唯一，重复提交直接拒绝；售价低于定价 70% 将转入待审批',
                     '<button class="btn">取消</button><button class="btn primary">保存并提交审核</button>')
    body += '<div style="display:grid;grid-template-columns:1fr 342px;gap:14px;align-items:start">'
    body += '<div class="pcard"><h3>基础信息</h3>'
    body += '<div class="fgrid">' \
            '<div class="it"><span>ISBN</span><input value="9787536692930" placeholder="13 位数字，可带连字符"></div>' \
            '<div class="it"><span>图书编号</span><input value="保存后自动生成" disabled style="background:#F7F9FC;color:#8B96A1"></div>' \
            '<div class="it"><span>书名</span><input value="三体（全三册）"></div>' \
            '<div class="it"><span>副标题</span><input value="地球往事三部曲"></div>' \
            '<div class="it"><span>作者</span><input value="刘慈欣"></div>' \
            '<div class="it"><span>出版社</span><select style="width:100%"><option>重庆出版社</option><option>人民文学出版社</option><option>机械工业出版社</option></select></div>' \
            '<div class="it"><span>分类</span><select style="width:100%"><option>文学 / 小说</option><option>文学 / 散文</option><option>计算机 / 系统与架构</option></select></div>' \
            '<div class="it"><span>版次</span><input value="第 1 版"></div>' \
            '<div class="it"><span>定价</span><input value="128.00"></div>' \
            '<div class="it"><span>售价</span><input value="96.00"></div>' \
            '<div class="it"><span>出版日期</span><input value="2008-01-01"></div>' \
            '<div class="it"><span>上架状态</span><select style="width:100%"><option>待上架</option><option>在售</option></select></div>' \
            '<div class="it full"><span>内容简介</span><textarea rows="3">文化大革命如火如荼进行的同时，军方探寻外星文明的绝秘计划「红岸工程」取得了突破性进展……</textarea></div>' \
            '</div></div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>封面与详情图</h3>'
    body += '<div class="imgs"><div class="img-box" style="width:104px;height:140px">封面 3:4</div>' \
            '<div class="upload" style="height:140px">' + ic('i-upload', 18) + '<span>上传封面<br>≤5MB</span></div></div>' \
            '<div style="font-size:11.5px;color:#5B6770;margin-top:8px;line-height:1.7">仅支持 jpg/png/webp，上传后存 uploads/book/yyyyMM/，页面不出现手填 URL 的输入框。</div></div>'
    body += '<div class="pcard"><h3>校验提示</h3>' + note('售价 96.00 为定价的 <b>75.0%</b>，高于 70% 阈值，可直接生效。', 'ok')
    body += '<div style="height:8px"></div>' + note('ISBN 9787536692930 已存在于「BK2610310001 三体（全三册）」，保存时将被拒绝并返回该编号。', 'warn') + '</div>'
    body += '<div class="pcard"><h3>入库衔接</h3>' + kvrows([('建议首单量', '120 册'), ('默认供应商', '重庆出版社（直发）'),
                                                          ('安全天数', '15 天'), ('季节系数', '1.12（开学季）')]) + '</div>'
    body += '</div></div>'
    return shell('ops', '新增图书', '<b>图书与分类</b> / 图书管理 / 新增图书', body)


# ---------------------------------------------------------------- 05 分类维护
def p_category():
    tree_rows = [
        ('文学', 'LIT', 0, '一级', 1, 486, '1'),
        ('&nbsp;&nbsp;├ 小说', 'LIT-FIC', 1, '二级', 1, 318, '1'),
        ('&nbsp;&nbsp;├ 散文随笔', 'LIT-PRO', 1, '二级', 1, 104, '0'),
        ('&nbsp;&nbsp;└ 诗歌', 'LIT-POE', 1, '二级', 0, 64, '0'),
        ('计算机', 'CS', 0, '一级', 1, 264, '1'),
        ('&nbsp;&nbsp;├ 系统与架构', 'CS-SYS', 1, '二级', 1, 96, '1'),
        ('&nbsp;&nbsp;├ 编程语言', 'CS-PL', 1, '二级', 1, 112, '0'),
        ('&nbsp;&nbsp;└ 人工智能', 'CS-AI', 1, '二级', 1, 56, '1'),
        ('历史', 'HIST', 0, '一级', 1, 186, '0'),
        ('&nbsp;&nbsp;└ 中国史', 'HIST-CN', 1, '二级', 1, 142, '1'),
        ('社科', 'SOC', 0, '一级', 1, 168, '0'),
        ('少儿', 'KID', 0, '一级', 0, 92, '0'),
    ]
    rows = [[r[0], '<span class="mono">' + r[1] + '</span>', r[3], str(r[4]), '<span class="mono">' + str(r[5]) + '</span>',
             '首页推荐' if r[6] == '1' else '—',
             stag('启用' if r[4] else '停用'),
             '<a href="#">编辑</a> <a href="#" style="color:#5B6770">排序</a> <a href="#" style="color:#B02A37">删除</a>']
            for r in tree_rows]
    body = page_head('分类维护', '两级分类 · 最多配置 8 个首页推荐位 · 删除前校验是否被图书引用',
                     '<button class="btn ghost">重算图书数</button><button class="btn primary">' + ic('i-plus', 14) + ' 新增分类</button>')
    body += '<div style="display:grid;grid-template-columns:1fr 320px;gap:14px;align-items:start">'
    body += '<div class="pcard" style="padding:0">' + table(['分类名称', '编码', '层级', '排序', '关联图书', '首页推荐', '状态', '操作'], rows) + '</div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>编辑分类<span>文学 / 小说</span></h3>'
    body += '<div style="display:flex;flex-direction:column;gap:10px">' \
            '<label class="f"><span>分类名称</span><input value="小说" style="width:100%"></label>' \
            '<label class="f"><span>分类编码</span><input value="LIT-FIC" style="width:100%"></label>' \
            '<label class="f"><span>上级分类</span><select style="width:100%"><option>文学</option><option>计算机</option></select></label>' \
            '<label class="f"><span>排序号</span><input value="1" style="width:100%"></label>' \
            '<label class="f" style="align-items:center"><span>首页推荐</span><input type="checkbox" checked style="width:15px;height:15px;flex:0 0 15px"></label>' \
            '<label class="f"><span>分类描述</span><textarea rows="2" style="width:100%">长篇小说、中短篇小说集与类型小说，含科幻、悬疑、现实题材。</textarea></label>' \
            '</div>'
    body += '<div style="display:flex;gap:8px;margin-top:4px"><button class="btn primary">保存</button><button class="btn">取消</button></div></div>'
    body += '<div class="pcard"><h3>删除校验</h3>' + note('「文学 / 小说」下仍有 <b>318</b> 种图书，删除将被拒绝，请先转移图书或停用分类。', 'danger')
    body += '<div style="height:8px"></div>' + note('停用分类不影响已上架图书展示，但不可再新增该分类下的图书。', 'info') + '</div>'
    body += '</div></div>'
    return shell('ops', '分类维护', '<b>图书与分类</b> / 分类维护', body)


# ---------------------------------------------------------------- 06 入库登记
def p_stock_in():
    rows = []
    data = [
        ('RK2611010006', '三体（全三册）', '重庆出版社（直发）', 120, 58.40, '0001-261101', '待质检', '待质检', '2026-11-01 09:24'),
        ('RK2611010007', '活着', '作家出版社（直发）', 300, 21.60, '0002-261101', '合格', '已入库', '2026-11-01 09:41'),
        ('RK2610310004', '人类简史', '中信出版社', 150, 32.80, '0016-261031', '合格', '已入库', '2026-10-31 15:02'),
        ('RK2610310005', '算法导论（第3版）', '机械工业出版社', 80, 76.50, '0009-261031', '破损', '已退回', '2026-10-31 14:18'),
        ('RK2610300003', '百年孤独', '南海出版公司', 200, 26.40, '0015-261030', '待检', '待质检', '2026-10-30 10:35'),
        ('RK2610300004', '数学之美（第3版）', '人民邮电出版社', 160, 34.20, '0012-261030', '合格', '已入库', '2026-10-30 16:50'),
        ('RK2610290002', '万历十五年', '中华书局', 240, 19.80, '0022-261029', '缺页', '已退回', '2026-10-29 11:07'),
        ('RK2610290003', '深入理解计算机系统', '机械工业出版社', 100, 82.00, '0008-261029', '合格', '已入库', '2026-10-29 17:22'),
    ]
    for no, bk, sup, qty, price, batch, qr, st, tm in data:
        rows.append(['<span class="mono">' + no + '</span>', bk, sup, '<span class="mono">' + str(qty) + '</span>',
                     '<span class="mono">' + ('%.2f' % price) + '</span>', '<span class="mono">' + batch + '</span>',
                     stag(qr), stag(st), '<span class="mono" style="font-size:12px">' + tm + '</span>',
                     ('<a href="07-inspect.html">质检</a>' if st == '待质检' else '<a href="#">查看</a>')])
    body = page_head('入库登记', '按图书 + 供应商 + 批次登记，提交后进入待质检；合格才写库存与流水',
                     '<button class="btn">导入采购单</button><button class="btn primary">' + ic('i-plus', 14) + ' 新建入库单</button>')
    body += toolbar(['input', 'sel:全部状态', 'sel:全部质检结果', 'sel:全部供应商', 'date:2026-10-01 ~ 2026-11-02'])
    body += '<div class="summary"><div><div class="k">本月入库单</div><div class="v">42 单</div></div>' \
            '<div><div class="k">入库册数</div><div class="v">6,280 册</div></div>' \
            '<div><div class="k">待质检</div><div class="v" style="color:#8A6410">2 单 / 320 册</div></div>' \
            '<div><div class="k">质检退回率</div><div class="v">6.4%</div></div></div>'
    body += '<div class="pcard" style="padding:0">' + table(['入库单号', '图书', '供应商', '入库数量', '采购单价', '批次号',
                                                          '质检结果', '状态', '创建时间', '操作'], rows) + '</div>'
    body += pager(42, 1, 8)
    return shell('keeper', '入库登记', '<b>入库</b> / 入库登记', body)


# ---------------------------------------------------------------- 07 入库质检
def p_inspect():
    body = page_head('入库质检台', 'RK2611010006 · 三体（全三册） · 重庆出版社（直发） · 待质检',
                     '<button class="btn">整批退回</button><button class="btn primary">' + ic('i-check', 14) + ' 提交质检结果</button>')
    body += '<div style="display:grid;grid-template-columns:1fr 356px;gap:14px;align-items:start">'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>待检队列<span>共 2 单 320 册</span></h3>'
    body += '<div class="qitem"><div class="l">' + ic('i-box', 16) + '<span><b>RK2611010006</b>　三体（全三册）</span></div><span class="tag t-amber">120 册</span></div>'
    body += '<div class="qitem"><div class="l">' + ic('i-box', 16) + '<span><b>RK2610300003</b>　百年孤独</span></div><span class="tag t-amber">200 册</span></div>'
    body += '</div>'
    body += '<div class="pcard"><h3>逐项核对</h3>' + table(['检查项', '标准', '本批实检', '结论'], [
        ['数量清点', '120 册', '120 册', stag('合格')],
        ['封面与装帧', '无破损、无压痕', '2 册书脊压痕', '<span class="tag t-amber">让步接收</span>'],
        ['内页完整性', '无缺页、无倒装', '1 册缺 3 页', stag('缺页')],
        ['版权页与版次', '第 1 版 2008 年', '一致', stag('合格')],
        ['ISBN 与订单', '9787536692930', '一致', stag('合格')],
    ]) + '</div>'
    body += '<div class="pcard"><h3>质检结果登记</h3>'
    body += '<div class="fgrid">' \
            '<div class="it"><span>质检结果</span><select style="width:100%"><option>合格</option><option>破损</option><option>缺页</option><option>错版</option></select></div>' \
            '<div class="it"><span>合格数量</span><input value="117"></div>' \
            '<div class="it"><span>不合格数量</span><input value="3"></div>' \
            '<div class="it"><span>处理方式</span><select style="width:100%"><option>整批退回</option><option>让步接收（按 8 折结算）</option></select></div>' \
            '<div class="it full"><span>质检说明</span><textarea rows="2">其中 2 册书脊压痕可让步接收，1 册内页缺 3 页需退回供应商换货，整批按 117 册入库。</textarea></div>' \
            '</div>'
    body += '<div style="display:flex;gap:8px;margin-top:10px"><button class="btn primary">合格入库</button><button class="btn danger">整批退回</button></div></div>'
    body += '</div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>单据摘要</h3>' + kvrows([
        ('入库单号', 'RK2611010006'), ('图书', '三体（全三册）'), ('供应商', '重庆出版社（直发）'),
        ('入库数量', '120 册'), ('采购单价', '¥58.40'), ('采购金额', '¥7,008.00'),
        ('批次号', '0001-261101'), ('入库人', '周慧敏'), ('创建时间', '2026-11-01 09:24'),
    ]) + '</div>'
    body += '<div class="pcard"><h3>入库后库存预览</h3>' + kvrows([
        ('当前可售', '142 册'), ('本次入库', '+117 册'), ('入库后', '<b>259 册</b>'),
        ('图书状态', '在售 → 在售'), ('流水类型', '入库'),
    ]) + '<div style="height:8px"></div>' + note('提交后在同一事务内写 stock_in 状态、stock_flow 流水与 book 库存，三者不一致时整体回滚。', 'info') + '</div>'
    body += '</div></div>'
    return shell('keeper', '入库质检台', '<b>入库</b> / 入库质检', body)
