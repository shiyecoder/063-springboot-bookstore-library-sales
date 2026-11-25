#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""页面组 B：盘点 / 流水 / 订单 / 发货 / 物流 / 售后 / 书评"""
from base import *


# ---------------------------------------------------------------- 08 库存盘点
def p_stocktake():
    rows = [
        ['BK2610310001', '三体（全三册）', '文学 / 小说', 'A-03-02', 142, '<input value="140" style="width:74px">', '−2', stag('盘亏')],
        ['BK2610310002', '活着', '文学 / 小说', 'A-03-05', 268, '<input value="268" style="width:74px">', '0', '<span class="tag t-green">一致</span>'],
        ['BK2610300015', '百年孤独', '文学 / 小说', 'A-04-01', 86, '<input value="88" style="width:74px">', '+2', stag('盘盈')],
        ['BK2610300016', '人类简史', '社科 / 社会学', 'B-01-04', 74, '<input value="74" style="width:74px">', '0', '<span class="tag t-green">一致</span>'],
        ['BK2610290008', '深入理解计算机系统', '计算机 / 系统与架构', 'C-02-01', 42, '<input value="41" style="width:74px">', '−1', stag('盘亏')],
        ['BK2610290009', '算法导论（第3版）', '计算机 / 系统与架构', 'C-02-04', 0, '<input value="0" style="width:74px">', '0', '<span class="tag t-green">一致</span>'],
        ['BK2610280021', '房思琪的初恋乐园', '文学 / 小说', 'A-04-06', 31, '<input value="33" style="width:74px">', '+2', stag('盘盈')],
        ['BK2610280022', '万历十五年', '历史 / 中国史', 'D-01-02', 58, '<input value="57" style="width:74px">', '−1', stag('盘亏')],
    ]
    body = page_head('库存盘点', '盘点任务 PD2611010002 · A 区文学货架 · 共 12 种，已盘 8 种',
                     '<button class="btn">生成盘点任务</button><button class="btn ghost">' + ic('i-download', 14) + ' 导出差异</button>'
                     '<button class="btn primary">提交盘点结果</button>')
    body += '<div class="toolbar"><select><option>A 区 · 文学货架</option><option>B 区 · 社科货架</option></select>'
    body += '<select><option>全部分类</option><option>文学 / 小说</option></select>'
    body += '<input class="search" placeholder="扫描货位码或输入图书编号">'
    body += '<button class="btn ghost">按分类生成</button><button class="btn">按货架生成</button>'
    body += '<div style="flex:1"></div><span style="font-size:12px;color:#5B6770">盘盈盘亏必须填写原因才可提交</span></div>'
    body += '<div class="pcard" style="padding:0">' + table(['图书编号', '书名', '分类', '货位', '账面数', '实盘数', '差异', '判定'], rows) + '</div>'
    body += '<div style="display:grid;grid-template-columns:1fr 372px;gap:14px;margin-top:12px;align-items:start">'
    body += '<div class="pcard"><h3>差异汇总</h3>'
    body += '<div class="summary" style="grid-template-columns:repeat(4,1fr);margin-bottom:0">' \
            '<div><div class="k">账面合计</div><div class="v">901 册</div></div>' \
            '<div><div class="k">实盘合计</div><div class="v">901 册</div></div>' \
            '<div><div class="k">盘盈</div><div class="v" style="color:#2B7955">+4 册 / 2 种</div></div>' \
            '<div><div class="k">盘亏</div><div class="v" style="color:#B02A37">−4 册 / 3 种</div></div></div>'
    body += '<div style="margin-top:12px">' + table(['差异图书', '差异', '原因（必填，不少于 5 字）'], [
        ['三体（全三册）', '−2', '<input value="前台试读借出未登记归还" style="width:320px">'],
        ['深入理解计算机系统', '−1', '<input value="破损下架未及时报损" style="width:320px">'],
        ['万历十五年', '−1', '<input value="盘点扫漏，复核货位 D-01-03" style="width:320px">'],
        ['百年孤独', '＋2', '<input value="上批验收入库漏记两册" style="width:320px">'],
        ['房思琪的初恋乐园', '＋2', '<input value="退货入库未回写账面" style="width:320px">'],
    ]) + '</div></div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>提交校验</h3>' + note('3 种盘亏、2 种盘盈均已填写原因，累计差异 4 册，差异率 0.44%，低于 1% 阈值可直接提交。', 'ok')
    body += '<div style="height:8px"></div>' + note('提交后将生成 5 条 stock_flow 流水（盘盈 / 盘亏），并同步调整 book 可售库存。', 'info') + '</div>'
    body += '<div class="pcard"><h3>近 3 次盘点</h3>' + kvrows([
        ('PD2610150001', 'B 区社科 · 差异 6 册 · 已提交'), ('PD2610010001', 'C 区计算机 · 差异 2 册 · 已提交'),
        ('PD2609180002', 'A 区文学 · 差异 11 册 · 已提交'),
    ], 116) + '</div>'
    body += '</div></div>'
    return shell('keeper', '库存盘点', '<b>库存</b> / 库存盘点', body)


# ---------------------------------------------------------------- 09 库存流水
def p_flow():
    data = [
        ('KC261101000018', '三体（全三册）', '入库', '+117', 142, 259, 'RK2611010006', '周慧敏', '2026-11-01 10:12'),
        ('KC261101000017', '活着', '入库', '+300', 268, 568, 'RK2611010007', '周慧敏', '2026-11-01 09:48'),
        ('KC261101000016', '百年孤独', '销售出库', '−3', 89, 86, 'BO2611010031', '系统', '2026-11-01 09:12'),
        ('KC261101000015', '人类简史', '销售出库', '−2', 76, 74, 'BO2611010028', '系统', '2026-11-01 08:44'),
        ('KC261031000014', '算法导论（第3版）', '销售出库', '−1', 1, 0, 'BO2610310096', '系统', '2026-10-31 21:20'),
        ('KC261031000013', '房思琪的初恋乐园', '退货入库', '+2', 29, 31, 'TH2610310004', '周慧敏', '2026-10-31 16:05'),
        ('KC261031000012', '万历十五年', '盘亏', '−1', 59, 58, 'PD2610310009', '周慧敏', '2026-10-31 11:32'),
        ('KC261030000011', '数学之美（第3版）', '报损', '−4', 164, 160, 'BS2610300002', '周慧敏', '2026-10-30 18:10'),
        ('KC261030000010', '深入理解计算机系统', '盘盈', '+2', 40, 42, 'PD2610300008', '周慧敏', '2026-10-30 15:47'),
    ]
    rows = []
    for no, bk, tp, chg, before, after, biz, op, tm in data:
        col = '#2B7955' if chg.startswith('+') else '#B02A37'
        ok = ''
        if tp == '盘亏':
            ok = '前台试读借出未登记归还'
        rows.append(['<span class="mono">' + no + '</span>', bk, stag(tp),
                     '<span class="mono" style="color:' + col + ';font-weight:600">' + chg + '</span>',
                     '<span class="mono">' + str(before) + '</span>', '<span class="mono">' + str(after) + '</span>',
                     '<span class="mono">' + biz + '</span>', op,
                     '<span class="mono" style="font-size:12px">' + tm + '</span>', ok or '—'])
    body = page_head('库存流水', '每次库存变化必须写流水：变动后库存 = 变动前库存 + 变动数量，不一致时拒绝提交',
                     '<button class="btn">对账检查</button><button class="btn gold">' + ic('i-download', 14) + ' 导出流水</button>')
    body += toolbar(['input', 'sel:全部变动类型', 'sel:全部图书', 'date:2026-10-26 ~ 2026-11-02'])
    body += '<div class="summary"><div><div class="k">本期流水</div><div class="v">1,842 条</div></div>' \
            '<div><div class="k">入库合计</div><div class="v" style="color:#2B7955">+6,280 册</div></div>' \
            '<div><div class="k">出库合计</div><div class="v" style="color:#B02A37">−5,116 册</div></div>' \
            '<div><div class="k">账实不符</div><div class="v" style="color:#2B7955">0 条</div></div></div>'
    body += '<div class="pcard" style="padding:0">' + table(['流水号', '图书', '变动类型', '变动数量', '变动前', '变动后',
                                                          '业务单号', '操作人', '时间', '备注'], rows) + '</div>'
    body += pager(1842, 1, 9)
    body += '<div style="margin-top:12px">' + note('对账结果：42,186 册账面库存与 1,842 条流水累计值一致；盘亏与报损流水均已填写不少于 5 字的备注。', 'ok') + '</div>'
    return shell('keeper', '库存流水', '<b>库存</b> / 库存流水', body)


# ---------------------------------------------------------------- 10 订单管理
def p_order():
    data = [
        ('BO2611010031', '林书禾', 3, 186.00, 96.00, 282.00, '已签收', '2026-11-01 08:52'),
        ('BO2611010030', '陈砚青', 1, 96.00, 6.00, 102.00, '待发货', '2026-11-01 08:31'),
        ('BO2611010029', '何以宁', 2, 74.80, 6.00, 80.80, '待支付', '2026-11-01 08:12'),
        ('BO2610310096', '苏晚', 1, 109.00, 6.00, 115.00, '已发货', '2026-10-31 21:14'),
        ('BO2610310095', '顾清和', 4, 243.50, 0.00, 243.50, '退换中', '2026-10-31 19:02'),
        ('BO2610310094', '林书禾', 2, 88.00, 6.00, 94.00, '已完成', '2026-10-31 16:38'),
        ('BO2610310093', '白露', 1, 41.00, 6.00, 47.00, '已取消', '2026-10-31 15:20'),
        ('BO2610300088', '周砚', 5, 318.00, 0.00, 318.00, '已完成', '2026-10-30 20:41'),
        ('BO2610300087', '许清欢', 2, 129.00, 0.00, 129.00, '已签收', '2026-10-30 18:05'),
    ]
    rows = []
    for no, cu, cnt, book_amt, fr, pay, st, tm in data:
        act = '<a href="11-order-detail.html">详情</a>'
        if st == '待发货':
            act += ' <a href="12-deliver.html">拣货</a>'
        if st == '已发货':
            act += ' <a href="13-logistics.html">物流</a>'
        rows.append(['<span class="mono">' + no + '</span>', cu, '<span class="mono">' + str(cnt) + '</span>',
                     '<span class="mono">' + ('%.2f' % book_amt) + '</span>',
                     '<span class="mono">' + ('%.2f' % fr) + '</span>',
                     '<span class="mono" style="font-weight:600">' + ('%.2f' % pay) + '</span>',
                     stag(st), '<span class="mono" style="font-size:12px">' + tm + '</span>', act])
    body = page_head('订单管理', '共 1,286 笔 · 待支付订单 30 分钟未支付自动取消并回补库存',
                     '<button class="btn">批量打印拣货单</button><button class="btn gold">' + ic('i-export', 14) + ' 导出</button>'
                     '<button class="btn primary" href="12-deliver.html">' + ic('i-truck', 14) + ' 去发货</button>')
    body += toolbar(['input', 'sel:全部状态', 'sel:全部收货城市', 'date:2026-10-26 ~ 2026-11-02'])
    body += '<div class="tabs"><a class="on" href="#">全部<span class="c">1,286</span></a><a href="#">待支付<span class="c">23</span></a>' \
            '<a href="#">待发货<span class="c">46</span></a><a href="#">已发货<span class="c">38</span></a>' \
            '<a href="#">已签收<span class="c">64</span></a><a href="#">退换中<span class="c">9</span></a>' \
            '<a href="#">已取消<span class="c">31</span></a></div>'
    body += '<div class="pcard" style="padding:0">' + table(['订单编号', '读者', '件数', '图书金额', '运费', '实付金额', '状态', '下单时间', '操作'], rows) + '</div>'
    body += pager(1286, 1, 9)
    return shell('clerk', '订单管理', '<b>订单</b> / 订单管理', body)


# ---------------------------------------------------------------- 11 订单详情
def p_order_detail():
    body = page_head('订单 BO2611010031', '下单 2026-11-01 08:52 · 签收 2026-11-01 15:40 · 读者 林书禾',
                     '<button class="btn">打印</button><button class="btn">联系读者</button><button class="btn primary">申请退换</button>')
    body += '<div class="pcard" style="margin-bottom:13px">' + flow('已签收', ['待支付', '待发货', '已发货', '已签收', '已完成']) + '</div>'
    body += '<div style="display:grid;grid-template-columns:1fr 348px;gap:14px;align-items:start">'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard" style="padding:0"><div style="padding:14px 18px 6px"><h3 style="margin:0">图书明细</h3></div>'
    body += table(['图书', '售价', '数量', '小计', '状态'], [
        ['<div style="display:flex;gap:10px;align-items:center"><div style="width:34px;height:46px;background:#EEF2F8;border:1px solid #E2E5E8;border-radius:2px;display:grid;place-items:center;font-size:9px;color:#8DA1BE">封面</div><div><div style="font-weight:600">三体（全三册）</div><div style="font-size:11.5px;color:#5B6770">刘慈欣 · 重庆出版社</div></div></div>',
         '96.00', '2', '192.00', stag('已签收')],
        ['<div style="display:flex;gap:10px;align-items:center"><div style="width:34px;height:46px;background:#EEF2F8;border:1px solid #E2E5E8;border-radius:2px;display:grid;place-items:center;font-size:9px;color:#8DA1BE">封面</div><div><div style="font-weight:600">活着</div><div style="font-size:11.5px;color:#5B6770">余华 · 作家出版社</div></div></div>',
         '33.80', '1', '33.80', stag('已签收')],
    ]) + '</div>'
    body += '<div class="pcard"><h3>金额构成</h3>' + kvrows([
        ('图书金额', '¥225.80'), ('运费', '¥0.00（满 99 免运费）'), ('优惠金额', '−¥39.80（开学季满 200 减 40）'),
        ('实付金额', '<b style="color:#B02A37;font-size:14px">¥186.00</b>'), ('支付方式', '微信支付 · 尾号 3821'),
        ('退款金额', '¥0.00'),
    ]) + '</div>'
    body += '<div class="pcard"><h3>状态流水</h3>' + timeline([
        ('ok', '读者确认收货，订单转已签收', '林书禾', '2026-11-01 15:40'),
        ('', '物流节点「已签收 · 本人签收」', '许立言', '2026-11-01 14:26'),
        ('', '物流节点「派送中 · 中山路营业点」', '许立言', '2026-11-01 09:05'),
        ('', '登记物流单号 SF1388204662913 并发货', '许立言', '2026-11-01 09:02'),
        ('ok', '支付成功，订单转待发货，锁定库存 3 件', '系统', '2026-11-01 08:53'),
        ('', '提交订单，生成待支付订单（30 分钟内有效）', '林书禾', '2026-11-01 08:52'),
    ]) + '</div></div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>收货信息</h3>' + kvrows([
        ('收货人', '林书禾'), ('电话', '138****6271'), ('地址', '江苏省南京市鼓楼区中山北路 128 号 3 幢 502'),
        ('物流单号', '<span class="mono">SF1388204662913</span>'), ('承运方', '顺丰速运（手工登记）'),
    ]) + '</div>'
    body += '<div class="pcard"><h3>可执行操作</h3>' + kvrows([
        ('确认收货', '读者已确认，按钮关闭'), ('申请退换', '签收后 7 天内可申请，剩余 <b>5 天</b>'),
        ('写书评', '已签收可评价，评分 1–5 星'), ('再次购买', '支持一键加购'),
    ], 88) + '</div>'
    body += '<div class="pcard"><h3>售后关联</h3>' + note('该订单暂无退换申请；同一订单同一图书只允许一个进行中的退换单。', 'info') + '</div>'
    body += '</div></div>'
    return shell('clerk', '订单详情', '<b>订单</b> / 订单管理 / 订单详情', body)


# ---------------------------------------------------------------- 12 发货页
def p_deliver():
    body = page_head('发货作业', '待发货 46 笔 · 拣货校验实际库存，缺货可拆单或取消缺货明细并部分退款',
                     '<button class="btn">打印面单</button><button class="btn primary">' + ic('i-truck', 14) + ' 确认发货（2 单）</button>')
    body += '<div style="display:grid;grid-template-columns:392px 1fr;gap:14px;align-items:start">'
    body += '<div class="pcard" style="padding:0">'
    body += '<div style="padding:12px 14px;border-bottom:1px solid #E2E5E8;display:flex;align-items:center;gap:8px">' \
            '<input type="checkbox" checked style="width:15px;height:15px"><span style="font-size:12.5px;color:#5B6770">全选本页 8 笔</span>' \
            '<div style="flex:1"></div><span class="tag t-amber">超 24h 3 笔</span></div>'
    body += '<div style="padding:11px">'
    items = [
        ('BO2611010030', '陈砚青', '三体（全三册）×1', '102.00', True, ''),
        ('BO2611010027', '祝小满', '人类简史 ×2、万历十五年 ×1', '141.80', True, ''),
        ('BO2611010025', '陆知微', '算法导论（第3版）×1', '115.00', True, '缺货 1 件'),
        ('BO2611010024', '闻笛', '活着 ×3', '107.40', True, ''),
        ('BO2611010022', '沈砚秋', '百年孤独 ×1、围城 ×1', '76.50', False, ''),
        ('BO2611010021', '江照', '数学之美（第3版）×2', '116.00', False, '缺货 2 件'),
    ]
    for no, cu, detail, amt, on, warn in items:
        badge = ('<span class="tag t-red">' + warn + '</span>') if warn else ''
        body += '<div class="qitem ' + ('on' if on else '') + '" style="align-items:flex-start">' \
                '<div style="display:flex;gap:9px;align-items:flex-start"><input type="checkbox" ' + ('checked' if on else '') + ' style="width:14px;height:14px;margin-top:4px">' \
                '<div><div style="font-weight:600;font-size:12.5px">' + no + '　' + cu + '</div>' \
                '<div style="font-size:11.5px;color:#5B6770;line-height:1.7">' + detail + '</div>' \
                '<div style="margin-top:4px">' + badge + '</div></div></div>' \
                '<span class="mono" style="font-weight:600">' + amt + '</span></div>'
    body += '</div>'
    body += pager(46, 1, 6)
    body += '</div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>拣货明细<span>BO2611010030 · 陈砚青</span></h3>'
    body += table(['图书', '货位', '应拣', '实拣', '库存校验', '操作'], [
        ['三体（全三册）', '<span class="mono">A-03-02</span>', '1', '<input value="1" style="width:56px">',
         '<span class="tag t-green">充足 · 259 册</span>', '<a href="#">换货位</a>'],
    ]) + '</div>'
    body += '<div class="pcard"><h3>缺货明细处理<span>BO2611010025</span></h3>'
    body += table(['图书', '应拣', '可拣', '处理方式', '缺货原因（必填）'], [
        ['算法导论（第3版）', '1', '0', '<select style="width:150px"><option>取消该明细并部分退款</option><option>拆单发货（后补）</option></select>',
         '<input value="供应商换货在途，预计 11-04 到货" style="width:250px">'],
    ]) + '<div style="margin-top:10px">' + note('取消缺货明细后将自动生成部分退款，退款金额 ≤ 该明细实付金额；拆单发货需在备注写明预计到货日。', 'warn') + '</div></div>'
    body += '<div class="pcard"><h3>物流登记</h3>'
    body += '<div class="fgrid" style="grid-template-columns:1fr 1fr">' \
            '<div class="it"><span>承运方</span><select style="width:100%"><option>顺丰速运</option><option>中通快递</option><option>京东物流</option></select></div>' \
            '<div class="it"><span>物流单号</span><input value="SF1388204663012"></div>' \
            '<div class="it"><span>首节点</span><input value="已揽收 · 南京中山路营业点"></div>' \
            '<div class="it"><span>节点时间</span><input value="2026-11-02 10:40"></div>' \
            '<div class="it full"><span>发货备注</span><textarea rows="2" style="width:100%">读者要求工作日送达，请勿放快递柜。</textarea></div>' \
            '</div>'
    body += '<div style="display:flex;gap:8px;margin-top:10px"><button class="btn primary">提交发货</button><button class="btn">保存草稿</button>' \
            '<button class="btn danger">整单取消</button></div></div>'
    body += '</div></div>'
    return shell('clerk', '发货作业', '<b>订单</b> / 发货作业', body)


# ---------------------------------------------------------------- 13 物流跟踪
def p_logistics():
    body = page_head('物流跟踪', '节点由店员手工补录，不接第三方物流接口；发货后 48 小时无节点更新进入异常列表',
                     '<button class="btn">批量补录节点</button><button class="btn primary">' + ic('i-plus', 14) + ' 新增节点</button>')
    body += '<div class="summary"><div><div class="k">在途订单</div><div class="v">38 笔</div></div>' \
            '<div><div class="k">48h 无更新</div><div class="v" style="color:#B02A37">2 笔</div></div>' \
            '<div><div class="k">平均签收时长</div><div class="v">32.4 小时</div></div>' \
            '<div><div class="k">签收率</div><div class="v">94.1%</div></div></div>'
    body += '<div style="display:grid;grid-template-columns:1fr 380px;gap:14px;align-items:start">'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>异常列表<span>发货后 48h 未更新节点，需填写跟进结果</span></h3>'
    body += table(['订单号', '物流单号', '承运方', '最后节点', '停滞时长', '跟进结果'], [
        ['<span class="mono">BO2610300087</span>', '<span class="mono">ZT7720184465</span>', '中通快递', '运输中 · 合肥中转', '<span class="tag t-red">54 小时</span>',
         '<input value="已联系网点，预计今日转出" style="width:210px">'],
        ['<span class="mono">BO2610300086</span>', '<span class="mono">SF1388204551208</span>', '顺丰速运', '派送中 · 无人在家', '<span class="tag t-red">49 小时</span>',
         '<input value="读者约定次日 18 点后再派" style="width:210px">'],
    ]) + '</div>'
    body += '<div class="pcard"><h3>在途明细</h3>' + table(['订单号', '读者', '物流单号', '承运方', '最新节点', '更新时间'], [
        ['<span class="mono">BO2610310096</span>', '苏晚', '<span class="mono">SF1388204662913</span>', '顺丰速运', '派送中 · 鼓楼区', '2026-11-02 08:20'],
        ['<span class="mono">BO2610310090</span>', '顾清和', '<span class="mono">JD9920184471</span>', '京东物流', '运输中 · 南京分拨', '2026-11-02 06:12'],
        ['<span class="mono">BO2610310088</span>', '白露', '<span class="mono">ZT7720184402</span>', '中通快递', '已揽收 · 上海', '2026-11-01 21:40'],
        ['<span class="mono">BO2611010030</span>', '陈砚青', '待登记', '—', '待发货', '—'],
    ]) + '</div></div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>节点详情<span>SF1388204662913</span></h3>' + timeline([
        ('', '派送中 · 南京鼓楼区营业点（配送员 王强 138****2210）', '许立言', '11-02 08:20'),
        ('', '到达 · 南京中转场', '许立言', '11-02 03:14'),
        ('', '运输中 · 离开无锡中转场', '许立言', '11-01 22:36'),
        ('ok', '已揽收 · 南京中山路营业点', '许立言', '11-01 09:02'),
    ]) + '</div>'
    body += '<div class="pcard"><h3>补录节点</h3>'
    body += '<div style="display:flex;flex-direction:column;gap:9px">' \
            '<label class="f"><span>节点类型</span><select style="width:100%"><option>已揽收</option><option>运输中</option><option>到达</option><option>派送中</option><option>已签收</option><option>异常</option></select></label>' \
            '<label class="f"><span>节点说明</span><input value="派送中 · 鼓楼区营业点" style="width:100%"></label>' \
            '<label class="f"><span>节点时间</span><input value="2026-11-02 08:20" style="width:100%"></label>' \
            '</div><div style="display:flex;gap:8px;margin-top:6px"><button class="btn primary">保存节点</button><button class="btn">取消</button></div>' \
            '<div style="margin-top:10px">' + note('登记「已签收」节点会同步把订单状态改为已签收，并计入销量统计。', 'info') + '</div></div>'
    body += '</div></div>'
    return shell('clerk', '物流跟踪', '<b>订单</b> / 物流跟踪', body)


# ---------------------------------------------------------------- 14 售后处理台
def p_return():
    data = [
        ('TH2611010004', 'BO2610310095', '顾清和', '三体（全三册）', '退货退款', 1, 96.00, '质量问题', '待审核', '2026-11-01 09:12'),
        ('TH2611010003', 'BO2610310094', '林书禾', '活着', '仅退款', 1, 33.80, '物流破损', '待寄回', '2026-11-01 08:40'),
        ('TH2610310002', 'BO2610300088', '周砚', '人类简史', '换货', 1, 51.00, '错发漏发', '已寄回', '2026-10-31 17:26'),
        ('TH2610300001', 'BO2610300087', '许清欢', '万历十五年', '退货退款', 1, 32.00, '不喜欢', '已退款', '2026-10-30 15:04'),
        ('TH2610290001', 'BO2610290071', '何以宁', '算法导论（第3版）', '退货退款', 1, 109.00, '质量问题', '已驳回', '2026-10-29 11:18'),
    ]
    rows = []
    for no, ordno, cu, bk, tp, qty, amt, reason, st, tm in data:
        act = '<a href="15-return-detail.html">处理</a>' if st in ('待审核', '待寄回', '已寄回') else '<a href="15-return-detail.html">查看</a>'
        rows.append(['<span class="mono">' + no + '</span>', '<span class="mono">' + ordno + '</span>', cu, bk, stag(tp),
                     '<span class="mono">' + str(qty) + '</span>', '<span class="mono">' + ('%.2f' % amt) + '</span>',
                     reason, stag(st), '<span class="mono" style="font-size:12px">' + tm + '</span>', act])
    body = page_head('售后处理台', '签收后 7 天内可申请；同订单同图书只允许一个进行中的退换单',
                     '<button class="btn">超期人工受理</button><button class="btn gold">' + ic('i-export', 14) + ' 导出售后台账</button>')
    body += '<div class="tabs"><a class="on" href="#">待处理<span class="c">8</span></a><a href="#">待寄回<span class="c">3</span></a>' \
            '<a href="#">已寄回<span class="c">2</span></a><a href="#">已退款<span class="c">18</span></a>' \
            '<a href="#">已驳回<span class="c">4</span></a></div>'
    body += toolbar(['input', 'sel:全部退换类型', 'sel:全部原因', 'date:2026-10-26 ~ 2026-11-02'])
    body += '<div class="pcard" style="padding:0">' + table(['退换单号', '订单号', '读者', '图书', '类型', '数量', '退款金额',
                                                          '原因', '状态', '申请时间', '操作'], rows) + '</div>'
    body += pager(35, 1, 5)
    body += '<div style="margin-top:12px;display:grid;grid-template-columns:1fr 1fr;gap:14px">'
    body += '<div class="pcard"><h3>退换原因分布</h3>' + chart_hbars([('质量问题', 38, '38%'), ('物流破损', 26, '26%'),
                                                                ('错发漏发', 18, '18%'), ('不喜欢', 18, '18%')], 72) + '</div>'
    body += '<div class="pcard"><h3>处理时效</h3>' + kvrows([
        ('平均审核时长', '3.2 小时'), ('平均退款到账', '1.4 天'), ('超 24h 未审核', '1 单'),
        ('本月退款总额', '¥4,286.40'), ('退款率（金额）', '2.1%'),
    ]) + '</div></div>'
    return shell('clerk', '售后处理台', '<b>售后</b> / 售后处理台', body)


# ---------------------------------------------------------------- 15 退换验收
def p_return_detail():
    body = page_head('退换单 TH2610310002', '换货 · 已寄回 · 等待库管验收 · 订单 BO2610300088',
                     '<button class="btn">驳回申请</button><button class="btn primary">' + ic('i-check', 14) + ' 验收并回补库存</button>')
    body += '<div class="pcard" style="margin-bottom:13px">' + flow('已寄回', ['待审核', '待寄回', '已寄回', '已退款']) + '</div>'
    body += '<div style="display:grid;grid-template-columns:1fr 356px;gap:14px;align-items:start">'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>申请信息</h3><div class="fgrid">' + kvrows([
        ('退换单号', 'TH2610310002'), ('关联订单', 'BO2610300088'), ('读者', '周砚'),
        ('图书', '人类简史'), ('退换类型', stag('换货')), ('申请数量', '1 件'),
        ('退款金额', '¥51.00（换货不退款，验收后重新发货）'), ('申请原因', '错发漏发'),
        ('问题描述', '收到的是《未来简史》，封面与订单不符，希望换成《人类简史》。'),
        ('申请时间', '2026-10-31 17:26'),
    ], 96) + '</div></div>'
    body += '<div class="pcard"><h3>退回商品验收</h3>' + table(['检查项', '标准', '实检', '结论'], [
        ['图书一致性', '人类简史（实收为未来简史）', '错版图书 1 册', stag('非本店图书')],
        ['外观完整性', '无破损、无污渍', '封面轻微折角', '让步接收'],
        ['内页完整性', '无缺页、无笔记', '完好', stag('完好')],
        ['防伪与塑封', '塑封完整', '已拆封', '—'],
    ]) + '<div style="margin-top:10px">' + note('验收结论为「非本店图书」时，不写库存流水，按错发流程重新发货并登记差异。', 'warn') + '</div></div>'
    body += '<div class="pcard"><h3>验收登记</h3><div class="fgrid">' \
            '<div class="it"><span>质检结果</span><select style="width:100%"><option>完好</option><option>缺页污损</option><option>非本店图书</option></select></div>' \
            '<div class="it"><span>验收数量</span><input value="1"></div>' \
            '<div class="it"><span>退款金额</span><input value="0.00"></div>' \
            '<div class="it"><span>入库库位</span><input value="B-01-04"></div>' \
            '<div class="it full"><span>验收备注</span><textarea rows="2">实收为《未来简史》，属错发；原件退回供应商，另发《人类简史》1 册。</textarea></div></div>'
    body += '<div style="display:flex;gap:8px;margin-top:10px"><button class="btn primary">提交验收</button><button class="btn">暂存</button></div></div>'
    body += '</div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>退回物流</h3>' + kvrows([
        ('退回单号', '<span class="mono">SF1388204511772</span>'), ('承运方', '顺丰速运'),
        ('寄回时间', '2026-10-31 20:14'), ('到达时间', '2026-11-01 14:32'),
    ]) + '</div>'
    body += '<div class="pcard"><h3>金额核对</h3>' + kvrows([
        ('明细实付', '¥51.00'), ('可退上限', '¥51.00'), ('本次退款', '¥0.00（换货）'),
        ('原订单状态', '已完成 → 退换中'), ('验收后状态', '退换中 → 已完成'),
    ]) + '<div style="height:8px"></div>' + note('退款金额不得超过该明细实付金额，超出时接口返回 4000。', 'info') + '</div>'
    body += '<div class="pcard"><h3>处理留痕</h3>' + timeline([
        ('', '读者寄回并填写单号 SF1388204511772', '周砚', '10-31 20:14'),
        ('ok', '店员审核通过，转待寄回', '许立言', '10-31 18:02'),
        ('', '读者提交换货申请', '周砚', '10-31 17:26'),
    ]) + '</div>'
    body += '</div></div>'
    return shell('keeper', '退换验收', '<b>退货</b> / 退回验收', body)


# ---------------------------------------------------------------- 16 书评审核
def p_review():
    data = [
        ('SP2611010006', '三体（全三册）', '林书禾', 5, '读完第三遍，仍然被「给岁月以文明」这句话击中。装帧扎实，纸张偏米黄，长时间阅读不刺眼。', '待审核', '含联系方式', '2026-11-01 10:02'),
        ('SP2611010005', '活着', '陈砚青', 4, '一口气读完，苦难写得克制。快递很快，书角有一点点压痕，不影响阅读。', '待审核', '—', '2026-11-01 09:36'),
        ('SP2611010004', '人类简史', '何以宁', 5, '视角很新，把认知革命讲得通俗。适合通勤路上读，每章都能独立看。', '已展示', '—', '2026-11-01 08:14'),
        ('SP2610310003', '百年孤独', '苏晚', 3, '译本比想象中晦涩，人名容易混。书本身没问题，期待有导读版本。', '已展示', '—', '2026-10-31 21:40'),
        ('SP2610310002', '围城', '顾清和', 5, '经典重读，方鸿渐的窘迫今天依然熟悉。加微信 wx_book2026 可以一起讨论。', '已隐藏', '命中敏感词', '2026-10-31 19:12'),
    ]
    rows = []
    for no, bk, cu, score, content, st, flag, tm in data:
        stars = '★' * score + '☆' * (5 - score)
        act = '<a href="#">通过</a> <a href="#" style="color:#B02A37">屏蔽</a> <a href="#" style="color:#5B6770">回复</a>'
        rows.append(['<span class="mono">' + no + '</span>', bk, cu,
                     '<span class="stars">' + stars + '</span>',
                     '<div style="max-width:420px;line-height:1.6">' + content + '</div>',
                     stag(st), flag, '<span class="mono" style="font-size:12px">' + tm + '</span>', act])
    body = page_head('书评审核', '只有已签收订单的读者可评价；命中敏感词或含联系方式自动隐藏并转人工，审核人不得审核自己的内容',
                     '<button class="btn">批量通过</button><button class="btn">敏感词库</button>')
    body += '<div class="tabs"><a class="on" href="#">待审核<span class="c">6</span></a><a href="#">已展示<span class="c">428</span></a>' \
            '<a href="#">已隐藏<span class="c">12</span></a><a href="#">已屏蔽<span class="c">4</span></a></div>'
    body += toolbar(['input', 'sel:全部评分', 'sel:全部图书', 'date:2026-10-26 ~ 2026-11-02'])
    body += '<div class="pcard" style="padding:0">' + table(['书评编号', '图书', '读者', '评分', '内容', '状态', '命中规则', '提交时间', '操作'], rows) + '</div>'
    body += pager(450, 1, 5)
    body += '<div style="margin-top:12px;display:grid;grid-template-columns:1fr 1fr;gap:14px">'
    body += '<div class="pcard"><h3>评分分布</h3>' + chart_hbars([('5 星', 268, '59.6%'), ('4 星', 122, '27.1%'),
                                                             ('3 星', 38, '8.4%'), ('2 星', 14, '3.1%'), ('1 星', 8, '1.8%')], 58) + '</div>'
    body += '<div class="pcard"><h3>回复与晒单</h3>' + kvrows([
        ('待回复书评', '14 条（4 星以下优先）'), ('含晒图书评', '186 条 · 平均 2.4 张'),
        ('有用数 TOP', '「三体」书评 · 128 人觉得有用'), ('审核超时', '0 条（要求 24h 内）'),
    ]) + '</div></div>'
    return shell('ops', '书评审核', '<b>内容</b> / 书评审核', body)
