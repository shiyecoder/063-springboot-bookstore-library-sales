#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""页面组 C：补货看板 / 建议详情 / 规则配置 / 计算留痕 / 销售报表"""
from base import *

W = [('近 30 天销量', 35, 'r'), ('库存周转天数', 25, 'r'), ('在途数量', 20, 'r'),
     ('季节与开学系数', 10, 'r'), ('近 90 天缺货次数', 10, 'r')]

# (编号, 书名, 近30天销量, 可售库存, 在途, 季节系数, 近90天缺货次数)
POOL = [
    ('解忧杂货店', 'BH2611010001', 132, 18, 0, 1.06, 4),
    ('人类简史', 'BH2611010002', 118, 74, 0, 1.08, 5),
    ('白夜行', 'BH2611010003', 96, 30, 0, 1.02, 3),
    ('活着', 'BH2611010004', 120, 268, 0, 1.05, 1),
    ('三体（全三册）', 'BH2611010005', 86, 142, 0, 1.12, 2),
    ('算法导论（第3版）', 'BH2611010006', 42, 0, 20, 1.10, 6),
    ('数学之美（第3版）', 'BH2611010007', 38, 0, 60, 1.10, 4),
    ('深入理解计算机系统', 'BH2611010008', 31, 42, 0, 1.10, 2),
    ('房思琪的初恋乐园', 'BH2611010009', 18, 31, 0, 0.95, 0),
    ('万历十五年', 'BH2611010010', 12, 58, 0, 0.90, 0),
    ('围城', 'BH2611010011', 9, 96, 0, 0.95, 0),
    ('时间简史', 'BH2611010012', 0, 0, 0, 1.00, 0),
]


def calc(sales30, stock, onway, season, stockout):
    """按需求文档第 8 节的五因子口径计算，返回 (各因子得分 dict, 总分, 等级, 周转天数, 建议量)"""
    day = sales30 / 30.0
    turn = 999.0 if day == 0 else stock / day
    f = {}
    f['sales'] = min(100.0, sales30 / 120.0 * 100)
    f['turn'] = 100.0 if stock == 0 else max(0.0, 100 - turn / 180.0 * 100)
    f['onway'] = 100 - min(100.0, onway / 200.0 * 100)
    f['season'] = max(0.0, min(100.0, (season - 0.8) / 0.7 * 100))
    f['stockout'] = min(100.0, 20 + stockout * 16)
    total = sum(f[k] * w / 100.0 for (k, w) in [('sales', 35), ('turn', 25), ('onway', 20), ('season', 10), ('stockout', 10)])
    if sales30 == 0 and stock == 0:
        level = '断销'
    elif total >= 80:
        level = '畅销'
    elif total >= 55:
        level = '平销'
    else:
        level = '滞销'
    safe = 15 if level == '畅销' else (7 if level == '平销' else 0)
    sug = max(0, int(round(day * (30 + safe) * season - stock - onway)))
    return f, total, level, turn, sug


DATA = []
for nm, no, s, st, ow, sf, so in POOL:
    f, total, level, turn, sug = calc(s, st, ow, sf, so)
    DATA.append(dict(name=nm, no=no, s=s, st=st, ow=ow, sf=sf, so=so, f=f, total=total,
                     level=level, turn=turn, sug=sug))
BY = {d['name']: d for d in DATA}
HOT = [d for d in DATA if d['level'] == '畅销']
MID = [d for d in DATA if d['level'] == '平销']
SLOW = [d for d in DATA if d['level'] in ('滞销', '断销')]


# ---------------------------------------------------------------- 17 补货看板
def _rcard(d):
    lv = {'畅销': 'lv-hot', '平销': 'lv-mid', '滞销': 'lv-slow', '断销': 'lv-dead'}[d['level']]
    turn = '—' if d['turn'] >= 999 else fmt1(d['turn']) + ' 天'
    return ('<div class="rcard"><div class="h"><span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">'
            + d['name'] + '</span><span class="' + lv + '">' + fmt1(d['total']) + '</span></div>'
            '<div class="m"><span>销量 <b>' + str(d['s']) + '</b> 册</span><span>周转 <b>' + turn + '</b></span>'
            '<span>库存 <b>' + str(d['st']) + '</b> 册</span><span>在途 <b>' + str(d['ow']) + '</b> 册</span></div>'
            '<div class="f"><span style="font-size:12px;color:#5B6770">建议补货 <b style="color:#B02A37;font-size:14px">'
            + str(d['sug']) + '</b> 册</span><a href="18-replenish-detail.html" class="btn sm ghost">依据</a></div></div>')


def p_replenish():
    body = page_head('补货看板', '每日 00:30 按 5 因子重算销量等级与建议补货量 · 数据快照 2026-11-02 00:30（规则 v2.0）',
                     '<button class="btn">切换到平销视图</button>'
                     '<button class="btn gold">' + ic('i-play', 14) + ' 立即重算</button>'
                     '<button class="btn primary">' + ic('i-plus', 14) + ' 生成采购单（已采纳 5 项）</button>')
    body += ('<div class="summary" style="margin-bottom:12px">'
             '<div><div class="k">待处理建议</div><div class="v">12 项 / 3 档</div></div>'
             '<div><div class="k">畅销补货量</div><div class="v" style="color:#B02A37">%d 册</div></div>'
             '<div><div class="k">建议采购金额（按 6.2 折进价）</div><div class="v">约 ¥{:,}</div></div>'
             '<div><div class="k">本月采纳率</div><div class="v">58.3%</div></div></div>').format(
        sum(d['sug'] for d in HOT), int(sum(d['sug'] * 26 for d in HOT)))
    body += '<div class="kanban">'
    for title, tagc, items, sub in [('畅销 · 足量补货', 't-red', HOT, '安全天数 15 · 3 个工作日内下单'),
                                    ('平销 · 按 80% 执行', 't-blue', MID, '安全天数 7 · 两周后重评'),
                                    ('滞销 · 暂停补货', 't-amber', SLOW, '转促销清单 · 周转 >180 天评估退货')]:
        body += ('<div class="kcol"><div class="kh"><span>' + title + '　<span class="tag ' + tagc + '">'
                 + str(len(items)) + ' 种</span></span><span class="cnt">' + sub + '</span></div><div class="kb">')
        for d in items:
            body += _rcard(d)
        body += '</div></div>'
    body += '</div>'
    body += '<div style="margin-top:12px;display:grid;grid-template-columns:1fr 1fr;gap:14px">'
    body += '<div class="pcard"><h3>采纳与修正记录</h3>' + table(['时间', '图书', '原建议', '人工修正', '原因', '处理人'], [
        ['11-01 16:20', '人类简史', '117 册', '140 册（+19.7%）', '开学季团购预订', '孟凡舟'],
        ['11-01 15:02', '围城', '0 册', '0 册（忽略）', '已列入双十一促销清单', '孟凡舟'],
        ['10-31 10:41', '数学之美（第3版）', '9 册', '60 册（+566%）', '出版社 11-05 前统一发货，凑整', '孟凡舟'],
    ]) + '<div style="margin-top:10px">' + note('人工上浮不得超过 20%；下调不限但必须填写不少于 5 字的原因，原计算结果保留不覆盖。', 'warn') + '</div></div>'
    body += '<div class="pcard"><h3>等级分布与采纳率</h3>' + chart_stack([
        ('畅销 3', [('已采纳', 66, '#3A5A8C'), ('未采纳', 34, '#D5DEEA')], '66%'),
        ('平销 5', [('已采纳', 44, '#6B8CBE'), ('未采纳', 56, '#D5DEEA')], '44%'),
        ('滞销 4', [('已采纳', 22, '#C9A227'), ('未采纳', 78, '#E8E3D4')], '22%'),
    ]) + '</div></div>'
    return shell('buyer', '补货看板', '<b>补货决策</b> / 补货看板', body)


# ---------------------------------------------------------------- 18 补货建议详情
def p_replenish_detail():
    d = BY['人类简史']
    f = d['f']
    rows = [
        ('近 30 天销量', '已签收且未退款订单件数', '35%', str(d['s']) + ' 册', f['sales']),
        ('库存周转天数', '可售库存 ÷ 日均销量', '25%', fmt1(d['turn']) + ' 天', f['turn']),
        ('在途数量', '采购单未入库数量', '20%', str(d['ow']) + ' 册', f['onway']),
        ('季节与开学系数', '分类季节系数配置', '10%', '%.2f' % d['sf'], f['season']),
        ('近 90 天缺货次数', '缺货登记 + 缺货取消记录', '10%', str(d['so']) + ' 次', f['stockout']),
    ]
    body = page_head('补货建议 ' + d['no'], d['name'] + ' · 中信出版社 · 社科 / 社会学 · 快照时间 2026-11-02 00:30',
                     '<button class="btn">忽略本次建议</button><button class="btn gold">人工修正</button>'
                     '<button class="btn primary">' + ic('i-check', 14) + ' 采纳并生成采购单</button>')
    body += ('<div class="summary" style="margin-bottom:13px">'
             '<div><div class="k">综合得分</div><div class="v" style="color:#B02A37">' + fmt1(d['total']) + ' 分 / 100</div></div>'
             '<div><div class="k">销量等级</div><div class="v"><span class="tag t-red">畅销</span></div></div>'
             '<div><div class="k">建议补货量</div><div class="v" style="color:#B02A37">' + str(d['sug']) + ' 册</div></div>'
             '<div><div class="k">规则版本 / 安全天数</div><div class="v">v2.0 · 15 天</div></div></div>')
    body += '<div style="display:grid;grid-template-columns:1fr 372px;gap:14px;align-items:start">'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>五因子得分明细<span>权重合计 100%</span></h3>'
    body += '<div style="display:grid;grid-template-columns:1fr 1fr;gap:0 24px">'
    rows_a = rows[:3]
    rows_b = rows[3:]
    for group in (rows_a, rows_b):
        body += '<div>'
        for nm, src, w, raw, sc in group:
            body += ('<div style="margin-bottom:11px"><div style="display:flex;justify-content:space-between;font-size:12.5px">'
                     '<span>' + nm + ' <span style="color:#9AA5B1">' + w + '</span></span>'
                     '<span style="font-variant-numeric:tabular-nums"><b>' + fmt1(sc) + '</b>'
                     '<span style="color:#9AA5B1;font-size:11px"> / 100</span></span></div>'
                     '<div class="meter"><i style="width:' + fmt1(sc) + '%;background:'
                     + ('#B02A37' if sc < 55 else ('#C9A227' if sc < 75 else '#3A5A8C')) + '"></i></div>'
                     '<div style="font-size:11px;color:#5B6770;margin-top:3px">原始值 <b>' + raw
                     + '</b> · 来源 ' + src + '</div></div>')
        body += '</div>'
    body += '</div>'
    body += '<div style="border-top:1px dashed #E6EAEF;padding-top:10px;display:flex;justify-content:space-between;align-items:baseline">' \
            '<span style="font-size:12.5px;color:#5B6770">加权合计 = 98.3×35% + 89.6×25% + 100.0×20% + 40.0×10% + 100.0×10%</span>' \
            '<span style="font-size:18px;font-weight:600;color:#B02A37">' + fmt1(d['total']) + ' 分</span></div>'
    body += '</div>'
    body += '<div class="pcard"><h3>建议补货量计算</h3>'
    body += ('<div style="font-size:13px;line-height:2;color:#1F2933">'
             '日均销量 = ' + str(d['s']) + ' ÷ 30 = <b>3.93 册</b><br>'
             '建议补货量 = 日均销量 × (30 + 安全天数 15) × 季节系数 1.08 − 可售库存 ' + str(d['st']) + ' − 在途 ' + str(d['ow']) + '<br>'
             '= 3.93 × 45 × 1.08 − 74 − 0 = <b style="color:#B02A37">117 册</b></div>')
    body += '<div style="margin-top:10px">' + note('畅销档需在 3 个工作日内下单；结果小于 0 时取 0，无销量图书周转天数记 999 并按滞销处理。', 'info') + '</div></div>'
    body += '<div class="pcard"><h3>结果解释<span>推荐结论 / 主要影响因素 / 未满足条件 / 可改进项</span></h3>'
    body += '<div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">'
    body += ('<div>' + note('<b>推荐结论</b><br>近 30 天售出 118 册位列全店第 2，库存仅够 18.8 天，'
                            '建议立刻补 117 册，可覆盖至 12 月中旬。', 'ok') + '</div>')
    body += ('<div>' + note('<b>主要影响因素</b><br>销量得分 98.3（权重 35%）为决定性因素；'
                            '周转 18.8 天低于 30 天安全线，得分 89.6。', 'info') + '</div>')
    body += ('<div>' + note('<b>未满足条件</b><br>在途数量为 0，未登记采购单；'
                            '若实际已有在途未录入，请补登后重算，否则建议量会偏高。', 'warn') + '</div>')
    body += ('<div>' + note('<b>可改进项</b><br>该分类 10 月季节系数 1.08，若 11 月上调至 1.15，'
                            '建议量将变为 128 册。', 'info') + '</div>')
    body += '</div></div></div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>输入快照<span>不可修改</span></h3>' + kvrows([
        ('快照 ID', 'SN2611020031'), ('规则版本', 'v2.0（2026-10-01 生效）'),
        ('近 30 天销量', '118 册'), ('可售库存', '74 册'), ('在途数量', '0 册'),
        ('周转天数', '18.8 天'), ('季节系数', '1.08'), ('缺货次数', '5 次'),
        ('生成时间', '2026-11-02 00:30:12'),
    ]) + '</div>'
    body += '<div class="pcard"><h3>人工修正</h3>'
    body += '<div style="display:flex;flex-direction:column;gap:9px">' \
            '<label class="f"><span>修正后数量</span><input value="140" style="width:100%"></label>' \
            '<label class="f"><span>上浮比例</span><input value="+19.7%（上限 20%）" disabled style="width:100%;background:#F7F9FC"></label>' \
            '<label class="f"><span>修正原因</span><textarea rows="3" style="width:100%">开学季院系团购已口头预订 30 册，按上限上浮至 140 册。</textarea></label></div>'
    body += '<div style="display:flex;gap:8px;margin-top:4px"><button class="btn primary">提交修正</button><button class="btn">重置</button></div>'
    body += '<div style="margin-top:10px">' + note('修正只改最终结果，原始计算结果与快照保留，看板同时统计修正率。', 'info') + '</div></div>'
    body += '<div class="pcard"><h3>同分类建议</h3>' + kvrows([
        ('人类简史', '畅销 · 117 册 · 已采纳'), ('万历十五年', '滞销 · 0 册 · 未采纳'),
        ('乡土中国', '平销 · 24 册 · 未采纳'),
    ], 96) + '</div>'
    body += '</div></div>'
    return shell('buyer', '补货建议详情', '<b>补货决策</b> / 补货看板 / 建议详情', body)


# ---------------------------------------------------------------- 19 规则配置
def p_rule():
    body = page_head('销量分级规则配置', '权重合计必须等于 100% · 修改后生成新版本号并记录生效时间，历史计算不受影响',
                     '<button class="btn">版本对比</button><button class="btn">恢复默认</button>'
                     '<button class="btn primary">' + ic('i-check', 14) + ' 保存并发布 v2.1</button>')
    body += '<div style="display:grid;grid-template-columns:1fr 380px;gap:14px;align-items:start">'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>因子权重<span>当前版本 v2.0 · 合计 100%</span></h3>'
    sliders = [('近 30 天销量', '已签收且未退款订单件数', 35), ('库存周转天数', '可售库存 ÷ 日均销量', 25),
               ('在途数量', '采购单未入库数量', 20), ('季节与开学系数', '分类季节系数配置', 10),
               ('近 90 天缺货次数', '缺货登记 + 缺货取消记录', 10)]
    for nm, sub, v in sliders:
        body += ('<div class="slider-row"><span class="nm">' + nm + '<small>' + sub + '</small></span>'
                 '<span class="track"><i style="width:' + str(v) + '%"></i><b style="left:calc(' + str(v) + '% - 8px)"></b></span>'
                 '<span class="vv">' + str(v) + '<small>%</small></span></div>')
    body += '<div style="border-top:1px solid #E2E5E8;padding-top:10px;display:flex;justify-content:space-between;font-size:13px">' \
            '<span style="color:#5B6770">权重合计</span><span style="font-weight:600;color:#2B7955">100%　校验通过</span></div></div>'
    body += '<div class="pcard"><h3>分级阈值与安全天数</h3>' + table(['等级', '分数区间', '安全天数', '补货策略', '复核周期'], [
        ['<span class="tag t-red">畅销</span>', '≥ 80 分', '15 天', '按公式足量补货，3 个工作日内下单', '每周'],
        ['<span class="tag t-blue">平销</span>', '55 – 79 分', '7 天', '按建议值 80% 执行', '两周'],
        ['<span class="tag t-amber">滞销</span>', '< 55 分', '0 天', '暂停补货并列入促销清单', '每月'],
        ['<span class="tag t-gray">断销</span>', '无销量且零库存', '—', '转退货或报损评估', '每月'],
    ]) + '</div>'
    body += '<div class="pcard"><h3>季节与开学系数<span>按分类与月份配置，缺省 1.00</span></h3>'
    body += table(['分类', '8 月', '9 月', '10 月', '11 月', '12 月'], [
        ['文学 / 小说', '1.05', '1.12', '1.05', '1.08', '1.15'],
        ['计算机 / 系统与架构', '1.02', '1.18', '1.10', '1.05', '0.98'],
        ['社科 / 社会学', '1.00', '1.10', '1.08', '1.02', '1.00'],
        ['少儿 / 绘本', '1.08', '1.22', '0.95', '1.05', '1.25'],
        ['历史 / 中国史', '0.95', '1.02', '0.90', '0.95', '1.02'],
    ]) + '</div></div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>版本记录</h3>'
    body += '<div class="timeline">' \
            '<div class="ev ok"><div class="h">v2.0 生效</div><div class="d">销量 35 / 周转 25 / 在途 20 / 季节 10 / 缺货 10 · 沈知远 · 2026-10-01 09:00</div></div>' \
            '<div class="ev"><div class="h">v1.2 生效</div><div class="d">在途权重 15% → 20%，季节 15% → 10% · 沈知远 · 2026-08-15 14:20</div></div>' \
            '<div class="ev"><div class="h">v1.1 生效</div><div class="d">首版分级阈值调整（80/55） · 沈知远 · 2026-06-02 10:05</div></div>' \
            '<div class="ev"><div class="h">v1.0 初始</div><div class="d">规则初版上线 · 系统 · 2026-04-01 00:00</div></div></div></div>'
    body += '<div class="pcard"><h3>修改影响评估</h3>' + kvrows([
        ('受影响图书', '1,286 种（下次重算时全部生效）'), ('等级可能变化', '约 42 种（预估）'),
        ('历史快照', '保留，按各自规则版本复算'), ('生效方式', '发布后次日 00:30 重算'),
        ('是否需要重算历史', '否'),
    ], 104) + '<div style="height:9px"></div>' + note('权重之和不等于 100% 时保存按钮禁用，接口返回 4000 并提示当前合计值。', 'warn') + '</div>'
    body += '<div class="pcard"><h3>阈值试算</h3>'
    body += '<div style="display:flex;flex-direction:column;gap:9px">' \
            '<label class="f"><span>销量</span><input value="118" style="width:100%"></label>' \
            '<label class="f"><span>库存</span><input value="74" style="width:100%"></label>' \
            '<label class="f"><span>在途</span><input value="0" style="width:100%"></label>' \
            '<label class="f"><span>季节系数</span><input value="1.08" style="width:100%"></label>' \
            '<label class="f"><span>缺货次数</span><input value="5" style="width:100%"></label></div>'
    body += '<button class="btn ghost" style="width:100%;margin-top:2px">试算</button>'
    body += '<div style="margin-top:9px;font-size:12.5px;color:#5B6770">结果：<b style="color:#B02A37">90.8 分</b> · 畅销 · 建议 117 册</div></div>'
    body += '</div></div>'
    return shell('buyer', '销量分级规则配置', '<b>规则</b> / 分级规则配置', body)


# ---------------------------------------------------------------- 20 计算留痕
def p_trace():
    snap = '''{
  "snapshotId": "SN2611020031",
  "bookId": 16, "bookName": "人类简史",
  "ruleVersion": "v2.0", "ruleEffectiveAt": "2026-10-01 09:00:00",
  "weights": { "sales": 35, "turnover": 25, "onway": 20, "season": 10, "stockout": 10 },
  "inputs": {
    "sales30d": 118, "stockQty": 74, "onwayQty": 0,
    "turnoverDays": 18.8, "seasonFactor": 1.08, "stockout90d": 5
  },
  "factorScores": {
    "sales": 98.3, "turnover": 89.6, "onway": 100.0, "season": 40.0, "stockout": 100.0
  },
  "totalScore": 90.8,
  "salesLevel": "畅销",
  "safetyDays": 15,
  "suggestQty": 117,
  "formula": "3.93 * 45 * 1.08 - 74 - 0 = 117",
  "generatedAt": "2026-11-02 00:30:12"
}'''
    body = page_head('计算留痕', '每次计算保存输入快照、规则版本、各因素得分、总分、等级与生成时间，结果可追溯',
                     '<button class="btn">按图书筛选</button><button class="btn ghost">' + ic('i-download', 14) + ' 导出快照</button>')
    body += toolbar(['input', 'sel:全部规则版本', 'sel:全部等级', 'date:2026-10-01 ~ 2026-11-02'])
    body += '<div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;align-items:start">'
    body += '<div class="pcard"><h3>快照记录<span>共 1,286 条 · 按生成时间倒序</span></h3>'
    body += table(['快照 ID', '图书', '版本', '总分', '等级', '建议量', '生成时间'], [
        ['<span class="mono">SN2611020031</span>', '人类简史', 'v2.0', '<b>90.8</b>', stag('畅销'), '117', '11-02 00:30'],
        ['<span class="mono">SN2611020030</span>', '解忧杂货店', 'v2.0', '<b>91.5</b>', stag('畅销'), '192', '11-02 00:30'],
        ['<span class="mono">SN2611020029</span>', '白夜行', 'v2.0', '<b>81.6</b>', stag('畅销'), '104', '11-02 00:30'],
        ['<span class="mono">SN2611020028</span>', '活着', 'v2.0', '77.9', stag('平销'), '33', '11-02 00:30'],
        ['<span class="mono">SN2611020027</span>', '算法导论（第3版）', 'v2.0', '69.6', stag('平销'), '0', '11-02 00:30'],
        ['<span class="mono">SN2611020026</span>', '房思琪的初恋乐园', 'v2.0', '47.2', stag('滞销'), '0', '11-02 00:30'],
        ['<span class="mono">SN2611020025</span>', '围城', 'v2.0', '26.7', stag('滞销'), '0', '11-02 00:30'],
        ['<span class="mono">SN2611020024</span>', '时间简史', 'v2.0', '49.9', stag('断销'), '0', '11-02 00:30'],
    ]) + pager(1286, 1, 8) + '</div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>输入快照 JSON<span>SN2611020031</span></h3>'
    body += '<div style="background:#F8FAFC;border:1px solid #E2E5E8;border-radius:6px;padding:10px 12px;font-family:ui-monospace,Menlo,monospace;font-size:11.5px;line-height:18px;white-space:pre;overflow:auto;max-height:392px">' + snap.replace('<', '&lt;') + '</div></div>'
    body += '<div class="pcard"><h3>逐项复算</h3>' + table(['因子', '原始值', '归一化', '×权重', '贡献分'], [
        ['近 30 天销量', '118 册', '98.3', '35%', '<b>34.4</b>'],
        ['库存周转天数', '18.8 天', '89.6', '25%', '<b>22.4</b>'],
        ['在途数量', '0 册', '100.0', '20%', '<b>20.0</b>'],
        ['季节与开学系数', '1.08', '40.0', '10%', '<b>4.0</b>'],
        ['近 90 天缺货次数', '5 次', '100.0', '10%', '<b>10.0</b>'],
        ['<b>合计</b>', '—', '—', '100%', '<b style="color:#B02A37">90.8</b>'],
    ]) + '<div style="margin-top:9px">' + note('相同输入 + 相同规则版本必须得到相同结果；快照生成后不可修改，人工修正另存修正记录。', 'info') + '</div></div>'
    body += '</div></div>'
    return shell('buyer', '计算留痕', '<b>规则</b> / 计算留痕', body)


# ---------------------------------------------------------------- 21 销售报表
def p_report():
    days = ['10-04', '10-07', '10-10', '10-13', '10-16', '10-19', '10-22', '10-25', '10-28', '10-31']
    body = page_head('销售报表', '按分类 / 出版社 / 图书三个维度统计 · 口径剔除已退款订单，跨月订单按签收时间归属',
                     '<button class="btn">本月</button><button class="btn">本季度</button><button class="btn">本年</button>'
                     '<button class="btn gold">' + ic('i-export', 14) + ' 导出 Excel</button>')
    body += ('<div class="summary" style="margin-bottom:13px">'
             '<div><div class="k">销售额</div><div class="v">¥236,842.60</div></div>'
             '<div><div class="k">销量</div><div class="v">3,286 册</div></div>'
             '<div><div class="k">订单数</div><div class="v">1,286 笔</div></div>'
             '<div><div class="k">客单价 / 件单价</div><div class="v">¥184.2 / ¥72.1</div></div></div>')
    body += '<div class="dash" style="grid-template-columns:1fr 1fr;grid-auto-rows:262px">'
    body += card('销售额趋势（最近 7 天 / 30 天切换）', chart_line(days, [('销售额（元）', '#3A5A8C',
                                                                      [7820, 8410, 6980, 9240, 8130, 10760, 8640, 9310, 11280, 8642])], h=210, w=560), '近 30 天')
    body += card('分类销售额占比', chart_donut([('文学', 34, '#3A5A8C'), ('计算机', 26, '#6B8CBE'), ('历史', 16, '#C9A227'),
                                          ('社科', 14, '#94AACB'), ('少儿', 10, '#D8C48A')], '¥236.8k', '总销售额', 112), '')
    body += '</div>'
    body += '<div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-top:13px">'
    body += '<div class="pcard"><h3>分类维度<span>按销售额排序</span></h3>' + table(['分类', '销量（册）', '销售额', '占比', '毛利率'], [
        ['文学 / 小说', '1,118', '¥80,532', '34.0%', '31.2%'],
        ['计算机 / 系统与架构', '486', '¥61,578', '26.0%', '26.8%'],
        ['历史 / 中国史', '428', '¥37,894', '16.0%', '29.4%'],
        ['社科 / 社会学', '316', '¥33,158', '14.0%', '28.1%'],
        ['少儿 / 绘本', '284', '¥23,684', '10.0%', '34.6%'],
    ]) + '</div>'
    body += '<div class="pcard"><h3>出版社维度<span>销量 TOP6</span></h3>' + table(['出版社', '销量', '销售额', '退货率', '在售品种'], [
        ['人民文学出版社', '598', '¥32,486', '1.8%', '86'],
        ['机械工业出版社', '412', '¥52,184', '3.2%', '64'],
        ['中信出版社', '386', '¥45,920', '2.4%', '112'],
        ['南海出版公司', '342', '¥38,616', '2.1%', '48'],
        ['中华书局', '268', '¥21,440', '1.2%', '36'],
        ['人民邮电出版社', '186', '¥22,318', '2.8%', '42'],
    ]) + '</div>'
    body += '</div>'
    body += '<div class="pcard" style="margin-top:13px"><h3>图书维度<span>按销量 TOP8 · 点击行可跳转到带筛选条件的图书列表</span></h3>'
    body += table(['图书', '分类', '销量', '销售额', '退货量', '退货率', '销量等级'], [
        ['解忧杂货店', '文学 / 小说', '132', '¥5,214', '1', '0.8%', stag('畅销')],
        ['活着', '文学 / 小说', '120', '¥4,056', '2', '1.7%', stag('平销')],
        ['人类简史', '社科 / 社会学', '118', '¥6,018', '3', '2.5%', stag('畅销')],
        ['白夜行', '文学 / 小说', '96', '¥4,464', '0', '0.0%', stag('畅销')],
        ['三体（全三册）', '文学 / 小说', '86', '¥8,256', '2', '2.3%', stag('平销')],
        ['算法导论（第3版）', '计算机 / 系统与架构', '42', '¥4,578', '1', '2.4%', stag('平销')],
        ['深入理解计算机系统', '计算机 / 系统与架构', '31', '¥3,658', '0', '0.0%', stag('平销')],
        ['万历十五年', '历史 / 中国史', '12', '¥384', '0', '0.0%', stag('滞销')],
    ]) + pager(1286, 1, 8) + '</div>'
    return shell('ops', '销售报表', '<b>经营概览</b> / 销售报表', body)
