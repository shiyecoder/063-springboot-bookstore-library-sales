#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""网上书店与图书销售系统 — 设计稿公共库（design 阶段）
只提供 shell / 组件 / 图表，不写页面。
所有 CSS 一律裸字符串拼接，绝不经过 % 格式化（CSS 里的 % 会被当格式符）。
"""
import os, re, html

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SYMS = re.findall(r'<symbol.*?</symbol>', open(os.path.join(BASE, 'icons.svg'), encoding='utf-8').read(), re.S)

TITLE = '网上书店与图书销售系统'
BRAND = '纸间书店'


def ic(name, size=18):
    return ('<svg class="ic" width="' + str(size) + '" height="' + str(size) + '" viewBox="0 0 24 24" fill="none" '
            'stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">'
            '<use href="#' + name + '"/></svg>')


def tag(text, cls='t-gray'):
    return '<span class="tag ' + cls + '">' + text + '</span>'


ST = {
    # 图书
    '待上架': 't-gray', '在售': 't-green', '缺货': 't-amber', '已下架': 't-gray', '停售': 't-red',
    # 入库
    '待质检': 't-amber', '已入库': 't-green', '已退回': 't-red', '已作废': 't-gray',
    '合格': 't-green', '破损': 't-red', '缺页': 't-red', '错版': 't-red', '待检': 't-gray',
    # 订单
    '待支付': 't-amber', '待发货': 't-blue', '已发货': 't-blue', '已签收': 't-green',
    '已完成': 't-green', '退换中': 't-amber', '已取消': 't-gray',
    # 退换
    '待审核': 't-amber', '待寄回': 't-blue', '已寄回': 't-blue', '已退款': 't-green',
    '已驳回': 't-red', '已关闭': 't-gray',
    '退货退款': 't-blue', '换货': 't-amber', '仅退款': 't-blue',
    '完好': 't-green', '缺页污损': 't-amber', '非本店图书': 't-red',
    # 书评
    '已展示': 't-green', '已隐藏': 't-gray', '已屏蔽': 't-red',
    # 补货
    '畅销': 't-red', '平销': 't-blue', '滞销': 't-amber', '断销': 't-gray',
    '未采纳': 't-gray', '已采纳': 't-green', '已忽略': 't-amber',
    # 通用
    '启用': 't-green', '停用': 't-gray', '正常': 't-green', '禁用': 't-red',
    '盘盈': 't-green', '盘亏': 't-red', '报损': 't-red', '入库': 't-green', '销售出库': 't-blue', '退货入库': 't-amber',
    '待处理': 't-amber', '处理中': 't-blue', '已完成 ': 't-green',
}


def stag(s):
    return tag(s, ST.get(s, 't-gray'))


# ---------------------------------------------------------------- 角色与导航
NAV = {
 'ops': [
   ('经营概览', [('i-dashboard', '运营工作台', '02-dash.html', ''), ('i-chart', '销售报表', '21-report.html', '')]),
   ('图书与分类', [('i-book', '图书管理', '03-book.html', '4'), ('i-tag', '分类维护', '05-category.html', '')]),
   ('交易', [('i-receipt', '订单管理', '10-order.html', '18'), ('i-return', '售后处理台', '14-return.html', '3')]),
   ('内容', [('i-star', '书评审核', '16-review-audit.html', '6')]),
   ('补货', [('i-box', '补货看板', '17-replenish.html', ''), ('i-rule', '分级规则配置', '19-rule.html', ''),
            ('i-trace', '计算留痕', '20-trace.html', '')]),
   ('个人', [('i-message', '站内消息', '25-message.html', '4'), ('i-user', '个人中心', '26-profile.html', '')]),
 ],
 'clerk': [
   ('门店运营', [('i-dashboard', '门店工作台', '02-dash.html', '')]),
   ('订单', [('i-receipt', '订单管理', '10-order.html', '12'), ('i-truck', '发货作业', '12-deliver.html', '8'),
            ('i-target', '物流跟踪', '13-logistics.html', '2')]),
   ('售后', [('i-return', '售后处理台', '14-return.html', '5'), ('i-check', '退换验收', '15-return-detail.html', '')]),
   ('内容', [('i-star', '书评回复', '16-review-audit.html', '6')]),
   ('个人', [('i-message', '站内消息', '25-message.html', '3'), ('i-user', '个人中心', '26-profile.html', '')]),
 ],
 'keeper': [
   ('库存作业', [('i-dashboard', '库房工作台', '02-dash.html', '')]),
   ('入库', [('i-upload', '入库登记', '06-stock-in.html', ''), ('i-check', '入库质检', '07-inspect.html', '7')]),
   ('库存', [('i-shelf', '库存盘点', '08-stocktake.html', ''), ('i-list', '库存流水', '09-flow.html', '')]),
   ('退货', [('i-return', '退回验收', '15-return-detail.html', '2')]),
   ('个人', [('i-message', '站内消息', '25-message.html', '2'), ('i-user', '个人中心', '26-profile.html', '')]),
 ],
 'buyer': [
   ('补货决策', [('i-box', '补货看板', '17-replenish.html', '12'), ('i-target', '建议详情', '18-replenish-detail.html', ''),
              ('i-list', '采纳与修正记录', '17-replenish.html#adopt', '')]),
   ('规则', [('i-rule', '分级规则配置', '19-rule.html', ''), ('i-trace', '计算留痕', '20-trace.html', '')]),
   ('经营', [('i-chart', '销售报表', '21-report.html', ''), ('i-book', '图书管理', '03-book.html', '')]),
   ('个人', [('i-message', '站内消息', '25-message.html', '5'), ('i-user', '个人中心', '26-profile.html', '')]),
 ],
 'sys': [
   ('系统设置', [('i-user', '用户管理', '22-user.html', ''), ('i-role', '角色权限', '23-role.html', ''),
              ('i-menu', '菜单与字典', '24-system.html', ''), ('i-log', '操作日志', '24-system.html#log', '')]),
   ('经营概览', [('i-dashboard', '运营工作台', '02-dash.html', '')]),
   ('个人', [('i-message', '站内消息', '25-message.html', '2'), ('i-user', '个人中心', '26-profile.html', '')]),
 ],
}
ROLE_USER = {
 'ops':    ('沈知远', '运营管理员 · 图书事业部', '沈'),
 'clerk':  ('许立言', '书店店员 · 中山路门店', '许'),
 'keeper': ('周慧敏', '库管员 · 中央仓', '周'),
 'buyer':  ('孟凡舟', '采购员 · 采购部', '孟'),
 'reader': ('林书禾', '读者 · 银卡会员', '林'),
 'sys':    ('管理员', '系统管理员', '管'),
}


def _nav(role, crumb):
    out = []
    for group, items in NAV[role]:
        out.append('<div class="group">' + group + '</div>')
        for icon, label, href, badge in items:
            on = ' on' if (label in crumb) else ''
            b = ('<span class="badge">' + badge + '</span>') if badge else ''
            out.append('<a class="' + on.strip() + '" href="' + href + '">'
                       + ic(icon, 16) + '<span>' + label + '</span>' + b + '</a>')
    return ''.join(out)


def shell(role, title, crumb, body, head='', cls=''):
    name, rlabel, initial = ROLE_USER[role]
    doc = ['<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">',
           '<title>' + title + ' · ' + TITLE + '</title>',
           '<link rel="stylesheet" href="mock.css"><style>' + head + '</style></head>',
           '<body class="' + cls + '">',
           '<svg xmlns="http://www.w3.org/2000/svg" style="display:none">' + ''.join(SYMS) + '</svg>',
           '<div class="shell"><aside class="side">',
           '<div class="brand"><span class="mark">书</span><span>' + BRAND + '</span></div>',
           '<nav class="nav">' + _nav(role, crumb) + '</nav></aside>',
           '<div class="main"><header class="top"><div class="crumb">' + crumb + '</div><div class="spacer"></div>',
           '<span class="icon-btn">' + ic('i-search') + '</span>',
           '<span class="icon-btn">' + ic('i-bell')
           + '<span class="badge" style="position:absolute;top:-5px;right:-5px;background:#C9A227;'
             'color:#3B2E05;border-radius:9px;padding:0 5px;font-size:10px;line-height:15px;font-weight:600">4</span></span>',
           '<span class="icon-btn">' + ic('i-help') + '</span>',
           '<div style="display:flex;align-items:center;gap:8px;font-size:13px">',
           '<span class="avatar">' + initial + '</span>',
           '<span><b style="font-weight:500">' + name + '</b>'
           '<span style="display:block;font-size:11px;color:#5B6770">' + rlabel + '</span></span></div>',
           '</header><main class="content">' + body + '</main></div></div></body></html>']
    return ''.join(doc)


def rawpage(title, body, head='', cls=''):
    return ('<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8"><title>' + title + ' · ' + TITLE
            + '</title><link rel="stylesheet" href="mock.css"><style>' + head + '</style></head><body class="' + cls + '">'
            + '<svg xmlns="http://www.w3.org/2000/svg" style="display:none">' + ''.join(SYMS) + '</svg>'
            + body + '</body></html>')


def page_head(title, sub, acts=''):
    return ('<div class="page-head"><div><h1>' + title + '</h1><div class="sub" style="margin-bottom:0">'
            + sub + '</div></div><div class="acts">' + acts + '</div></div>')


# ---------------------------------------------------------------- 组件
def kpis(items, cls='compact'):
    h = ['<div class="kpis ' + cls + '">']
    for it in items:
        label, val, unit = it[0], it[1], it[2]
        delta = it[3] if len(it) > 3 else ''
        dcls = it[4] if len(it) > 4 else ''
        gcls = it[5] if len(it) > 5 else ''
        d = ('<div class="delta ' + dcls + '">' + delta + '</div>') if delta else '<div class="delta" style="visibility:hidden">-</div>'
        h.append('<div class="kpi"><div class="label">' + label + '</div><div class="value ' + gcls + '">'
                 + val + '<small>' + unit + '</small></div>' + d + '</div>')
    h.append('</div>')
    return ''.join(h)


def card(title, body, sub='', cls='', bodycls=''):
    return ('<div class="c ' + cls + '"><div class="ct">' + title + '<span>' + sub
            + '</span></div><div class="cbody ' + bodycls + '">' + body + '</div></div>')


def table(head, rows, cls=''):
    h = ['<div class="tbl-wrap"><table class="' + cls + '"><thead><tr>']
    for c in head:
        h.append('<th>' + c + '</th>')
    h.append('</tr></thead><tbody>')
    for r in rows:
        h.append('<tr>' + ''.join('<td>' + str(c) + '</td>' for c in r) + '</tr>')
    h.append('</tbody></table></div>')
    return ''.join(h)


def pager(total, cur=1, size=10):
    pages = max(1, (total + size - 1) // size)
    h = ['<div class="pager"><span>共 ' + str(total) + ' 条 · 每页 ' + str(size) + ' 条</span>']
    for p in range(1, min(pages, 5) + 1):
        h.append('<span class="pg ' + ('on' if p == cur else '') + '">' + str(p) + '</span>')
    h.append('<span class="pg">下一页</span></div>')
    return ''.join(h)


def toolbar(filters, btns=''):
    h = ['<div class="toolbar">']
    for f in filters:
        if f == 'input':
            h.append('<input class="search" placeholder="搜索编号 / 名称">')
        elif f.startswith('sel:'):
            h.append('<select><option>' + f[4:] + '</option></select>')
        elif f.startswith('date:'):
            h.append('<input style="width:150px" value="' + f[5:] + '">')
        elif f.startswith('txt:'):
            h.append('<span style="font-size:13px;color:#5B6770">' + f[4:] + '</span>')
    h.append('<button class="btn ghost">查询</button><button class="btn">重置</button>'
             '<div style="flex:1"></div>' + btns + '</div>')
    return ''.join(h)


def kvrows(items, w=96):
    h = ['<div style="font-size:12.5px">']
    for k, v in items:
        h.append('<div style="display:flex;gap:10px;padding:5px 0;border-bottom:1px dashed #E6EAEF">'
                 '<span style="flex:0 0 %dpx;color:#5B6770">' % w + k + '</span>'
                 '<span style="flex:1;color:#1F2933">' + v + '</span></div>')
    return ''.join(h) + '</div>'


def flow(cur, items=None):
    items = items or ['待支付', '待发货', '已发货', '已签收', '已完成']
    idx = items.index(cur) if cur in items else -1
    h = ['<div class="flow">']
    for i, s in enumerate(items):
        c = 'done' if (idx >= 0 and i < idx) else ('on' if i == idx else '')
        h.append('<span class="st ' + c + '">' + s + '</span>')
        if i < len(items) - 1:
            h.append('<span class="arw">›</span>')
    h.append('</div>')
    return ''.join(h)


def timeline(events):
    h = ['<div class="timeline">']
    for st, title, who, when in events:
        h.append('<div class="ev ' + st + '"><div class="h">' + title + '</div><div class="d">'
                 + who + ' · ' + when + '</div></div>')
    return ''.join(h) + '</div>'


def note(text, kind='info'):
    color = {'info': '#3A5A8C', 'warn': '#C9A227', 'ok': '#2B7955', 'danger': '#B02A37'}[kind]
    return ('<div style="background:#F6F8FB;border:1px solid #E2E5E8;border-left:3px solid ' + color
            + ';padding:8px 12px;font-size:12px;color:#5B6770;border-radius:4px;line-height:1.75">' + text + '</div>')


# ---------------------------------------------------------------- 图表
def chart_line(labels, series, h=180, w=560, ymax=None, dual=False):
    """单轴折线；dual=True 时第二条序列用右侧独立坐标轴（销售额 + 订单量这类量级差大的组合）
    右轴上限取「第二序列最大值 × 2.3」，避免两条折线完全重合看不清。"""
    pad_l, pad_b, pad_t = 36, 22, 8
    pad_r = 34 if dual else 6
    maxv = ymax or (max(max(s[2]) for s in series) * 1.15)
    if dual:
        maxv = max(series[0][2]) * 1.18
    iw, ih = w - pad_l - pad_r, h - pad_b - pad_t
    rmax = max(series[1][2]) * 2.3 if (dual and len(series) > 1) else 0

    def X(i):
        return pad_l + iw * i / (len(labels) - 1)

    def Y(v):
        return pad_t + ih - ih * v / maxv

    g = []
    for k in range(5):
        gy = pad_t + ih * k / 4
        gv = maxv * (1 - k / 4)
        g.append('<line x1="' + str(pad_l) + '" y1="%.1f" x2="%.1f" y2="%.1f" stroke="#EDF0F3"/>' % (gy, w - pad_r, gy))
        g.append('<text x="' + str(pad_l - 6) + '" y="%.1f" font-size="10" fill="#9AA5B1" text-anchor="end">%.0f</text>' % (gy + 3, gv))
        if dual and rmax:
            g.append('<text x="%.1f" y="%.1f" font-size="10" fill="#B08A18" text-anchor="start">%.0f</text>'
                     % (w - pad_r + 7, gy + 3, rmax * (1 - k / 4)))
    for i, lb in enumerate(labels):
        if i == len(labels) - 1:
            g.append('<text x="%.1f" y="%d" font-size="10" fill="#9AA5B1" text-anchor="end">%s</text>' % (w - pad_r, h - 5, lb))
        elif i % 2 == 0:
            g.append('<text x="%.1f" y="%d" font-size="10" fill="#9AA5B1" text-anchor="middle">%s</text>' % (X(i), h - 5, lb))
    for si, (name, color, vals) in enumerate(series):
        k = (rmax and maxv / rmax) if (dual and si == 1) else 1.0
        pts = ' '.join('%.1f,%.1f' % (X(i), Y(v * k)) for i, v in enumerate(vals))
        g.append('<polyline points="' + pts + '" fill="none" stroke="' + color + '" stroke-width="2" stroke-linejoin="round"/>')
        for i, v in enumerate(vals):
            g.append('<circle cx="%.1f" cy="%.1f" r="2.3" fill="#fff" stroke="%s" stroke-width="1.5"/>' % (X(i), Y(v * k), color))
    leg = ''.join('<span style="display:inline-flex;align-items:center;gap:5px;margin-right:14px"><i style="width:9px;height:3px;border-radius:2px;background:'
                  + c + ';display:inline-block"></i>' + n + '</span>' for n, c, _ in series)
    return ('<div style="height:100%;display:flex;flex-direction:column"><div style="font-size:11px;color:#5B6770;margin-bottom:2px">'
            + leg + '</div><div style="flex:1;min-height:0"><svg class="chart-line" viewBox="0 0 ' + str(w) + ' ' + str(h)
            + '" preserveAspectRatio="none">' + ''.join(g) + '</svg></div></div>')


def chart_hbars(items, w=86):
    h = []
    mx = max(v for _, v, _ in items)
    for name, val, label in items:
        pct = max(2, val / mx * 100)
        h.append('<div class="hbar"><span class="n" style="width:' + str(w) + 'px;flex:0 0 ' + str(w) + 'px">'
                 + name + '</span><span class="t"><i style="width:%.1f%%"></i></span><span class="v">' % pct
                 + label + '</span></div>')
    return ''.join(h)


def chart_vbars(items, alt_from=4):
    h = ['<div class="vbars">']
    mx = max(v for _, v in items)
    for i, (lb, v) in enumerate(items):
        h.append('<div><span class="num">' + str(v) + '</span><span class="b '
                 + ('alt' if i >= alt_from else '') + '" style="height:%.1f%%"></span><span class="lb">' % (v / mx * 86)
                 + lb + '</span></div>')
    return ''.join(h) + '</div>'


def chart_donut(parts, mid_v, mid_l, size=124):
    r = size / 2 - 9
    c = 2 * 3.141592653589793 * r
    off, segs = 0.0, []
    for lab, pct, col in parts:
        d = c * pct / 100.0
        segs.append('<circle cx="%.1f" cy="%.1f" r="%.1f" fill="none" stroke="%s" stroke-width="13" '
                    'stroke-dasharray="%.2f %.2f" stroke-dashoffset="%.2f" transform="rotate(-90 %.1f %.1f)"/>'
                    % (size / 2, size / 2, r, col, d, c - d, -off, size / 2, size / 2))
        off += d
    leg = ''.join('<div><i style="background:' + c2 + ';display:inline-block"></i>' + l2
                  + '<b>' + str(p2) + '%</b></div>' for l2, p2, c2 in parts)
    return ('<div class="donut-wrap"><div style="position:relative;width:' + str(size) + 'px;height:' + str(size)
            + 'px;flex:0 0 ' + str(size) + 'px"><svg width="' + str(size) + '" height="' + str(size) + '">'
            + ''.join(segs) + '</svg><div style="position:absolute;inset:0;display:grid;place-items:center;text-align:center;line-height:1.2">'
            + '<div><b style="font-size:16px">' + mid_v + '</b><span style="font-size:11px;color:#5B6770;display:block">'
            + mid_l + '</span></div></div></div><div class="lg">' + leg + '</div></div>')


def chart_funnel(steps):
    """steps: [(label, value, pct_text)] 自下而上收窄的漏斗"""
    h = ['<div style="height:100%;display:flex;flex-direction:column;justify-content:center;gap:6px">']
    mx = steps[0][1]
    for i, (lb, v, pct) in enumerate(steps):
        wd = 34 + 62 * (v / mx)
        h.append('<div style="display:flex;align-items:center;gap:9px;font-size:12px">'
                 '<span style="flex:0 0 62px;text-align:right;color:#5B6770">' + lb + '</span>'
                 '<span style="flex:1;display:flex;justify-content:center"><i style="display:block;height:22px;width:'
                 + fmt1(wd) + '%;background:' + ['#3A5A8C', '#5570A2', '#6B8CBE', '#94AACB', '#B7C6DC'][i % 5]
                 + ';border-radius:3px"></i></span>'
                 '<span style="flex:0 0 76px;color:#1F2933;font-variant-numeric:tabular-nums">' + str(v)
                 + '<span style="color:#5B6770;font-size:11px"> · ' + pct + '</span></span></div>')
    return ''.join(h) + '</div>'


def chart_scatter(points, h=150, w=258, xlabel='', ylabel=''):
    pad_l, pad_b, pad_t = 30, 18, 6
    iw, ih = w - pad_l - 8, h - pad_b - pad_t
    mx = max(p[1] for p in points) * 1.12
    my = max(p[2] for p in points) * 1.1

    def X(v):
        return pad_l + iw * v / mx

    def Y(v):
        return pad_t + ih - ih * v / my

    g = []
    for k in range(4):
        gy = pad_t + ih * k / 3
        g.append('<line x1="' + str(pad_l) + '" y1="%.1f" x2="%d" y2="%.1f" stroke="#EDF0F3"/>' % (gy, w - 8, gy))
        g.append('<text x="' + str(pad_l - 4) + '" y="%.1f" font-size="8" fill="#9AA5B1" text-anchor="end">%.0f</text>'
                 % (gy + 3, my * (1 - k / 3)))
    for k in range(4):
        gx = pad_l + iw * k / 3
        g.append('<text x="%.1f" y="%d" font-size="8" fill="#9AA5B1" text-anchor="middle">%.0f</text>' % (gx, h - 5, mx * k / 3))
    for lb, x, y in points:
        g.append('<circle cx="%.1f" cy="%.1f" r="3.4" fill="#6B8CBE" fill-opacity=".7" stroke="#3A5A8C" stroke-width=".9"/>' % (X(x), Y(y)))
    return '<svg class="chart-line" viewBox="0 0 %d %d" preserveAspectRatio="none">%s</svg>' % (w, h, ''.join(g))


def fmt1(v):
    return ('%.1f' % v)


def chart_group2(labels, s1, s2, n1='销售额', n2='订单量', c1='#3A5A8C', c2='#C9A227', unit=''):
    mx = max(max(s1), max(s2)) * 1.16
    h = ['<div style="height:100%;display:flex;flex-direction:column">',
         '<div style="font-size:11px;color:#5B6770;margin-bottom:2px">',
         '<span style="display:inline-flex;align-items:center;gap:5px;margin-right:12px"><i style="width:9px;height:9px;border-radius:2px;background:'
         + c1 + ';display:inline-block"></i>' + n1 + '</span>',
         '<span style="display:inline-flex;align-items:center;gap:5px"><i style="width:9px;height:9px;border-radius:2px;background:'
         + c2 + ';display:inline-block"></i>' + n2 + '</span></div>',
         '<div style="flex:1;min-height:0;display:flex;align-items:flex-end;gap:12px;padding:0 2px 2px">']
    for i, lb in enumerate(labels):
        h1 = max(2, s1[i] / mx * 100)
        h2 = max(2, s2[i] / mx * 100)
        h.append('<div style="flex:1;min-width:0;height:100%;display:flex;flex-direction:column;justify-content:flex-end;align-items:center;gap:3px">'
                 '<div style="display:flex;align-items:flex-end;gap:3px;height:100%;width:100%;justify-content:center">'
                 '<div style="width:11px;height:' + fmt1(h1) + '%;background:' + c1 + ';border-radius:2px 2px 0 0"></div>'
                 '<div style="width:11px;height:' + fmt1(h2) + '%;background:' + c2 + ';border-radius:2px 2px 0 0"></div>'
                 '</div><span style="font-size:10px;color:#5B6770;white-space:nowrap">' + lb + '</span></div>')
    return ''.join(h) + '</div></div>'


def chart_stack(rows):
    h = ['<div class="stack">']
    for name, parts, right in rows:
        segs = ''.join('<i style="width:' + ('%.1f' % p) + '%;background:' + c + '"></i>' for _, p, c in parts)
        h.append('<div class="srow"><span class="sname">' + name + '</span><span class="sbar">' + segs
                 + '</span><span class="sv">' + right + '</span></div>')
    return ''.join(h) + '</div>'


def chart_hist(items, unit='本'):
    """直方图：区间 + 数量，用于库存周转天数分布"""
    mx = max(v for _, v in items)
    h = ['<div style="height:100%;display:flex;align-items:flex-end;gap:6px;padding-top:6px">']
    for lb, v in items:
        hh = max(3, v / mx * 100)
        h.append('<div style="flex:1;min-width:0;height:100%;display:flex;flex-direction:column;justify-content:flex-end;align-items:center;gap:4px">'
                 '<span style="font-size:10px;color:#1F2933;font-variant-numeric:tabular-nums">' + str(v) + '</span>'
                 '<div style="width:100%;height:' + fmt1(hh) + '%;background:#6B8CBE;border-radius:2px 2px 0 0"></div>'
                 '<span style="font-size:9.5px;color:#5B6770;white-space:nowrap">' + lb + '</span></div>')
    return ''.join(h) + '</div>'


def score_rows(items):
    h = []
    for name, weight, score, note_ in items:
        col = '#B02A37' if score < 55 else ('#C9A227' if score < 75 else '#3A5A8C')
        h.append('<div class="score-row"><span>' + name + ' <span style="color:#9AA5B1">' + weight + '</span></span>'
                 '<span class="bar"><i style="width:%.1f%%;background:%s"></i></span><span class="v">%.1f</span></div>'
                 % (score, col, score))
    return ''.join(h)


def book_cover(name, h=148):
    """纸感封面占位（design 阶段不含真实图，build 阶段替换为 uploads/seed 实拍）"""
    return ('<div class="cv" style="height:' + str(h) + 'px">'
            '<span style="padding:0 8px;text-align:center;line-height:1.6">' + name + '</span></div>')
